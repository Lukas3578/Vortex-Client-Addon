package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;

/**
 * Spawner Safer:
 * Sobald sich ein anderer Spieler innerhalb von 100 Bloeken befindet, schleicht
 * der Spieler und baut alle Spawner in seiner Umgebung ab, um sie einzusammeln.
 * Ist alles abgebaut, wird der Server verlassen. Wird der Spieler dabei von
 * einem Spieler getroffen, wird NICHT verlassen, sondern 30 Sekunden gewartet
 * (weiter geschlichen), danach wird weitergemacht und anschliessend verlassen.
 */
public final class SpawnerSaferAddonModule extends Module {
    private static final double REACH = 4.0;
    private static final double REACH_SQ = REACH * REACH;
    private static final int SEARCH_RADIUS_Y = 4;
    private static final int WAIT_TICKS = 600;      // 30 Sekunden
    private static final int BREAK_DELAY = 2;       // Ticks zwischen zwei Abbruechen
    private static final int WALK_TIMEOUT = 100;    // ~5s ohne Fortschritt -> Ziel aufgeben
    private static final int PICKUP_TIMEOUT = 120;  // ~6s ohne Aufsammeln -> aufgeben

    /** Suchradius um den Spieler (x/z). */
    public final NumberSetting range = new NumberSetting("Range", 10, 1, 16, 1);

    /**
     * How far away a player counts as spotted.
     *
     * Render distance rather than a few blocks: by the time someone is close
     * enough to see the spawners, packing them up has already taken too long.
     */
    public final NumberSetting detectRange = new NumberSetting("Detect Range", 100, 16, 256, 8);

    /** Crouch while working. */
    public final BooleanSetting sneak = new BooleanSetting("Sneak", true);

    /**
     * Close whatever is open when someone shows up.
     *
     * Chat, inventory, a chest, the escape menu -- all of it. Being in a menu
     * is not a reason to keep standing next to the spawners.
     */
    public final BooleanSetting closeScreens = new BooleanSetting("Close Open Screens", true);

    /**
     * Hotbar slot holding the silk touch pickaxe (1 to 9).
     *
     * Without silk touch a spawner breaks into nothing at all, so this is not
     * a nicety -- it decides whether you pack the spawners up or destroy them.
     */
    public final NumberSetting silkSlot = new NumberSetting("Pickaxe Slot", 1, 1, 9, 1);

    /** Switch to that slot before breaking. */
    public final BooleanSetting useSilkTouch = new BooleanSetting("Switch to Pickaxe", true);

    /** Do nothing at all if that slot holds no pickaxe. */
    public final BooleanSetting requirePickaxe = new BooleanSetting("Only With a Pickaxe", true);

    /**
     * Throw away totems when there is no room left for the spawners.
     *
     * Spawners do not stack -- ten of them need ten free slots. With a full
     * inventory they drop on the floor instead, and the module then waits for
     * a pickup that can never happen while nothing has moved.
     *
     * Totems are what gets thrown because they are the one thing usually
     * carried by the stack, and a stack of them frees a whole slot at once.
     */
    public final BooleanSetting dropForSpace = new BooleanSetting("Drop Totems For Space", true);

    /** Log out once everything is collected. */
    public final BooleanSetting logoutWhenDone = new BooleanSetting("Log Out When Done", true);

    private boolean started;
    private boolean done;
    private boolean hit;
    private int waitTimer;
    private int breakCooldown;
    private float lastHealth = -1.0f;

    private BlockPos breakTarget;

    /** The block we have already started on, so the first hit happens once. */
    private BlockPos attackedTarget;
    private BlockPos walkTarget;
    private int walkTicks;
    private int pickupId = -1;
    private int pickupTicks;

    public SpawnerSaferAddonModule() {
        super("Spawner Safer", Category.CHEATS);
        addSetting(range);
        addSetting(detectRange);
        addSetting(sneak);
        addSetting(closeScreens);
        addSetting(silkSlot);
        addSetting(useSilkTouch);
        addSetting(requirePickaxe);
        addSetting(dropForSpace);
        addSetting(logoutWhenDone);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onEnable() {
        started = false;
        done = false;
        hit = false;
        waitTimer = 0;
        breakTarget = null;
        attackedTarget = null;
        walkTarget = null;
        walkTicks = 0;
        pickupId = -1;
        pickupTicks = 0;
        lastHealth = -1.0f;
    }

    /**
     * Is the module in the middle of its work?
     *
     * Asked by FocusPauseMixin: only then is it worth keeping the pause menu
     * from reopening. The rest of the time the game should behave normally.
     */
    public boolean isWorking() {
        return started && !done;
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        releaseAllKeys(client);
    }

    private void onTick(MinecraftClient client) {
        if (!isEnabled()) return;
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;
        // An open screen used to stop everything here.
        //
        // That is exactly backwards: standing in the escape menu, a chest or
        // the chat is when you are least likely to notice someone arriving.
        // The module now closes whatever is open and gets on with it.
        if (client.currentScreen != null) {
            if (!closeScreens.get()) return;
            // Only once someone is actually coming -- otherwise it would fight
            // you for the inventory every tick while nothing is happening.
            if (!started && !enemyWithinRange(client, player)) return;
            closeAnyScreen(client, player);
            // The screen goes this tick; the work starts on the next one.
            return;
        }

        if (!started) {
            if (!enemyWithinRange(client, player)) return;

            // Someone is in range. Nothing else matters until the pickaxe is
            // sorted: breaking a spawner without silk touch destroys it, which
            // is worse than being caught with it.
            if (requirePickaxe.get() && !pickaxeReady(player)) return;

            started = true;
            lastHealth = player.getHealth();
        }

        // Hold the pickaxe. Checked every tick rather than once: a slot can
        // change underneath you, and finding out mid-break is too late.
        if (useSilkTouch.get() && pickaxeReady(player)) {
            Slots.select(player, pickaxeSlot());
        }

        // Make room before breaking anything.
        //
        // Checked against the number of spawners still standing, not against
        // one: breaking the last of ten with nine free slots means the tenth
        // lands on the floor.
        if (dropForSpace.get()) {
            makeRoom(client, player);
        }

        // Sneak while working, if that is wanted.
        //
        // Crouching keeps you off the edge of the platform and makes you a
        // little harder to spot. Optional, because on some servers moving while
        // crouched is what draws attention in the first place.
        client.options.sneakKey.setPressed(sneak.get());

        // Wurden wir gerade von einem Spieler getroffen?
        checkHit(player);

        // Nach einem Treffer: 30 Sekunden warten, nicht abbauen
        if (waitTimer > 0) {
            waitTimer--;
            releaseMoveKeys(client);
            if (waitTimer == 0) {
                hit = false;
            }
            return;
        }

        if (done) {
            releaseMoveKeys(client);
            if (logoutWhenDone.get()) {
                leave(client);
            } else {
                // Everything collected, nothing left to do -- switch off rather
                // than keep running through the checks every tick.
                setEnabled(false);
            }
            return;
        }

        if (breakCooldown > 0) {
            breakCooldown--;
            return;
        }

        // 1) Zu einem entfernten Spawner laufen
        if (walkTarget != null) {
            if (!isSpawner(player.getEntityWorld(), walkTarget)) {
                walkTarget = null;
            } else {
                double distSq = player.squaredDistanceTo(
                        walkTarget.getX() + 0.5, walkTarget.getY() + 0.5, walkTarget.getZ() + 0.5);
                if (distSq <= REACH_SQ) {
                    walkTarget = null; // im naechsten Tick brechen
                } else if (walkTicks++ > WALK_TIMEOUT) {
                    walkTarget = null; // nicht erreichbar -> naechstes Ziel
                } else {
                    moveToward(client, walkTarget.getX() + 0.5, walkTarget.getZ() + 0.5);
                    return;
                }
            }
        }

        // 2) Fallengelassenes Spawner-Item aufsammeln
        if (pickupId != -1) {
            Entity entity = player.getEntityWorld().getEntityById(pickupId);
            if (entity instanceof ItemEntity item && item.getStack().isOf(Blocks.SPAWNER.asItem())) {
                if (player.squaredDistanceTo(entity) <= 1.5 * 1.5) {
                    pickupId = -1; // aufgehoben
                } else if (pickupTicks++ > PICKUP_TIMEOUT) {
                    pickupId = -1; // nicht erreichbar -> weiter
                } else {
                    moveToward(client, entity.getX(), entity.getZ());
                    return;
                }
            } else {
                pickupId = -1; // aufgehoben oder verschwunden
            }
        }

        // 3) Break the spawner in reach.
        //
        // WHY THIS WAS BROKEN: it called breakBlock(), which removes a block in
        // one go. That only works in creative. In survival a block has to be
        // hit and then held until it gives way, so the single call did nothing
        // -- the spawner stayed, the target was cleared anyway, and the module
        // decided it was finished and logged out. One swing, then gone, exactly
        // as you saw.
        if (breakTarget != null) {
            if (!isSpawner(player.getEntityWorld(), breakTarget)) {
                // Gone -- either broken or never there.
                breakTarget = null;
                attackedTarget = null;
                client.interactionManager.cancelBlockBreaking();
                return;
            }

            // Look at it, then keep hitting. The face does not matter much;
            // upwards is the one always reachable from beside the block.
            lookAt(player, breakTarget);

            // Whether breaking has started is remembered here rather than
            // asked of the game: isCurrentlyBreaking is private, and reaching
            // past that would need a mixin for something we already know. The
            // first hit starts it, every one after keeps it going.
            if (!breakTarget.equals(attackedTarget)) {
                client.interactionManager.attackBlock(breakTarget, Direction.UP);
                attackedTarget = breakTarget;
            }
            client.interactionManager.updateBlockBreakingProgress(breakTarget, Direction.UP);
            player.swingHand(Hand.MAIN_HAND);
            return;
        }

        // 4) Neues Ziel suchen (naechster Spawner)
        BlockPos nearest = findNearestSpawner(player);
        if (nearest != null) {
            double distSq = player.squaredDistanceTo(
                    nearest.getX() + 0.5, nearest.getY() + 0.5, nearest.getZ() + 0.5);
            if (distSq <= REACH_SQ) {
                breakTarget = nearest;
            } else {
                walkTarget = nearest;
                walkTicks = 0;
            }
            return;
        }

        // 5) Uebrig gebliebenes Spawner-Item einsammeln
        ItemEntity item = findSpawnerItem(client, player);
        if (item != null) {
            pickupId = item.getId();
            pickupTicks = 0;
            return;
        }
        // Dropped totems are deliberately NOT collected.
        //
        // They used to be, and together with dropping them for space that made
        // a loop with no way out: throw a stack to free a slot, see it lying
        // there, walk over and pick it up, inventory full again, throw another.
        // The module would never finish and never log out.
        //
        // Space for the spawners is the point here; the totems can stay where
        // they land.

        // 7) Nothing left -> done
        done = true;
    }

    /** Anderer (lebender) Spieler innerhalb von 100 Bloeken? */
    private boolean enemyWithinRange(MinecraftClient client, ClientPlayerEntity player) {
        double r = detectRange.get();
        double rSq = r * r;
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof PlayerEntity other)) continue;
            if (other == player || !other.isAlive() || other.isSpectator()) continue;
            if (other.squaredDistanceTo(player) <= rSq) return true;
        }
        return false;
    }

    /** Treffer durch einen Spieler erkennen (Schaden + Verursacher). */
    private void checkHit(ClientPlayerEntity player) {
        float health = player.getHealth();
        if (health < lastHealth) {
            DamageSource source = player.getRecentDamageSource();
            Entity attacker = source == null ? null : source.getAttacker();
            if (attacker instanceof PlayerEntity) {
                hit = true;
                waitTimer = WAIT_TICKS;
            }
        }
        lastHealth = health;
    }

    /** Naechsten Spawner in der Suchbox finden (nach Distanz sortiert). */
    private BlockPos findNearestSpawner(ClientPlayerEntity player) {
        int r = (int) range.get();
        BlockPos center = player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -SEARCH_RADIUS_Y; dy <= SEARCH_RADIUS_Y; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    if (!isSpawner(player.getEntityWorld(), pos)) continue;
                    double distSq = player.squaredDistanceTo(
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (distSq < bestDist) {
                        bestDist = distSq;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    /** Naechstes herumliegendes Spawner-Item finden. */
    private static ItemEntity findSpawnerItem(MinecraftClient client, ClientPlayerEntity player) {
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ItemEntity item)) continue;
            if (!item.getStack().isOf(Blocks.SPAWNER.asItem())) continue;
            double distSq = entity.squaredDistanceTo(player);
            if (distSq < bestDist) {
                bestDist = distSq;
                best = item;
            }
        }
        return best;
    }

    /**
     * Which hotbar slot holds the silk touch pickaxe.
     *
     * WHY A SLOT AND NOT A CHECK FOR THE ENCHANTMENT: reading enchantments in
     * this version needs a chain of registry lookups I cannot verify here, and
     * building on a guess is how a module ends up silently doing nothing --
     * which is exactly what happened to Fast Use in the previous release.
     *
     * You know which pickaxe has silk touch. Put it in this slot and the module
     * reaches for it with certainty rather than cleverness.
     */
    private int pickaxeSlot() {
        return Math.max(1, Math.min(9, silkSlot.getInt())) - 1;
    }

    /** Does that slot actually hold a pickaxe? */
    private boolean pickaxeReady(ClientPlayerEntity player) {
        try {
            ItemStack stack = player.getInventory().getStack(pickaxeSlot());
            if (stack == null || stack.isEmpty()) return false;
            var id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
            return id != null && id.getPath().endsWith("_pickaxe");
        } catch (Throwable pvpErr) {
            return false;
        }
    }

    /** How many spawners are still standing in range. */
    private int countSpawners(ClientPlayerEntity player) {
        int r = (int) range.get();
        BlockPos center = player.getBlockPos();
        int n = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -SEARCH_RADIUS_Y; dy <= SEARCH_RADIUS_Y; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (isSpawner(player.getEntityWorld(), center.add(dx, dy, dz))) n++;
                }
            }
        }
        return n;
    }

    /**
     * Empty slots that a picked-up spawner could actually land in.
     *
     * The hotbar and the main inventory, which is where items go. The off hand
     * is deliberately left out: nothing is ever picked up into it, so counting
     * it would promise a slot that does not exist -- and the last spawner would
     * end up on the floor.
     */
    private static int freeSlots(ClientPlayerEntity player) {
        int free = 0;
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack == null || stack.isEmpty()) free++;
        }
        return free;
    }

    /**
     * Throws away totem stacks until the spawners will fit.
     *
     * One stack per tick, not all at once: each throw is a message to the
     * server, and a burst of them in a single tick is both unnecessary and
     * exactly the sort of thing that stands out.
     */
    private void makeRoom(MinecraftClient client, ClientPlayerEntity player) {
        try {
            int needed = countSpawners(player);
            if (needed <= 0) return;

            // One slot of headroom.
            //
            // Without it the last spawner regularly missed: the count fits
            // exactly, then something else is picked up on the way -- a dropped
            // item, an arrow -- and the final slot is gone. One spare costs a
            // totem stack and removes the whole class of near-misses.
            needed += 1;

            if (freeSlots(player) >= needed) return;

            var inv = player.getInventory();
            for (int i = 0; i < 36; i++) {
                ItemStack stack = inv.getStack(i);
                if (stack == null || stack.isEmpty()) continue;
                var id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
                if (id == null || !id.getPath().equals("totem_of_undying")) continue;

                // The player's own screen handler numbers its slots differently
                // from the inventory: the hotbar sits at the end, not the start.
                int slot = (i < 9) ? (36 + i) : i;
                client.interactionManager.clickSlot(
                        player.playerScreenHandler.syncId, slot, 1,
                        net.minecraft.screen.slot.SlotActionType.THROW, player);
                return;   // one per tick
            }
        } catch (Throwable pvpErr) {
            com.vortex.client.core.Errors.report("SpawnerSafer.makeRoom", pvpErr);
        }
    }

    /**
     * Closes whatever screen is open.
     *
     * A container needs closeHandledScreen so the server is told as well --
     * simply dropping the screen would leave it thinking you still have the
     * chest open, and the next thing you do arrives out of order.
     */
    private static void closeAnyScreen(MinecraftClient client, ClientPlayerEntity player) {
        try {
            if (client.currentScreen instanceof
                    net.minecraft.client.gui.screen.ingame.HandledScreen) {
                player.closeHandledScreen();
            } else {
                client.setScreen(null);
            }
        } catch (Throwable pvpErr) {
            com.vortex.client.core.Errors.report("SpawnerSafer.close", pvpErr);
        }
    }

    /**
     * Turns towards a block.
     *
     * The server checks that you are looking at what you are breaking, so
     * without this the swings are discarded and the spawner never gives way.
     */
    private static void lookAt(ClientPlayerEntity player, BlockPos pos) {
        double dx = pos.getX() + 0.5 - player.getX();
        double dy = pos.getY() + 0.5 - (player.getY() + player.getEyeHeight(player.getPose()));
        double dz = pos.getZ() + 0.5 - player.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
    }

    private static boolean isSpawner(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.SPAWNER);
    }

    /** Hin zu einem Punkt laufen (Blickrichtung setzen + Vorwaerts-Taste). */
    private void moveToward(MinecraftClient client, double x, double z) {
        ClientPlayerEntity player = client.player;
        double dx = x - player.getX();
        double dz = z - player.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        player.setYaw(yaw);
        client.options.forwardKey.setPressed(true);
        client.options.jumpKey.setPressed(player.horizontalCollision);
    }

    /** Server verlassen (Disconnect). */
    private void leave(MinecraftClient client) {
        // Tell Auto Reconnect to stay out of this one.
        //
        // The whole point of leaving here is not being there. Without this the
        // client would dial straight back in, drop you next to the player you
        // just avoided, and the spawners would be in your inventory instead of
        // safely away.
        try {
            com.vortex.client.hud.AutoReconnect.suppressNext();
        } catch (Throwable pvpErr) {
            // Older client without that call -- leaving still works.
            com.vortex.client.core.Errors.report("SpawnerSafer.suppress", pvpErr);
        }

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler != null) {
            handler.getConnection().disconnect(Text.literal("Spawner Safer: Fertig"));
        }
        setEnabled(false);
    }

    private static void releaseMoveKeys(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
    }

    private static void releaseAllKeys(MinecraftClient client) {
        releaseMoveKeys(client);
        client.options.sneakKey.setPressed(false);
    }
}
