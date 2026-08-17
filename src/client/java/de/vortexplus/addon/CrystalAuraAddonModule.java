package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Crystal Aura: zerstoert Endkristalle in der Naehe des Ziels und platziert
 * automatisch neue. Fehlt ein Spot, wird selbst Obsidian gesetzt - besonders
 * wenn der Gegner oberhalb steht (Block direkt unter das Ziel legen).
 */
public final class CrystalAuraAddonModule extends Module {
    public final NumberSetting targetRange = new NumberSetting("Target Range", 10.0, 1.0, 20.0, 1.0);
    public final NumberSetting placeRange = new NumberSetting("Place Range", 4.5, 1.0, 8.0, 0.5);
    public final NumberSetting breakRange = new NumberSetting("Break Range", 4.5, 1.0, 8.0, 0.5);
    public final NumberSetting delay = new NumberSetting("Delay", 1, 0, 10, 1);
    public final BooleanSetting rotate = new BooleanSetting("Rotate", true);

    /**
     * Only use spots that can actually reach the opponent.
     *
     * MY EARLIER ATTEMPT AT THIS WAS IMPOSSIBLE: it demanded the obsidian sit
     * exactly one block under the opponent's feet. A crystal placed there fills
     * the two blocks above -- the space the opponent is standing in -- and the
     * game refuses to place anything into an entity. The rule could never be
     * satisfied, so nothing was ever placed.
     *
     * What matters is not where the spot sits relative to their feet, but
     * whether the blast reaches them: close by, and not far below.
     */
    public final BooleanSetting onlyUseful = new BooleanSetting("Only If It Can Hit", true);

    /** How far the crystal may sit from the opponent, horizontally. */
    public final NumberSetting maxSpread = new NumberSetting("Max Spread", 2.0, 1.0, 6.0, 0.5);

    /**
     * Refuse anything that would catch you as well.
     *
     * Without this the module happily lights a crystal at your feet. The blast
     * does not care who set it off, and blowing yourself up is a strange way to
     * win a fight.
     */
    public final BooleanSetting selfProtect = new BooleanSetting("Do Not Hit Myself", true);

    /** How far a crystal has to be from you before it may be used. */
    public final NumberSetting minSelfDistance =
            new NumberSetting("Min Distance to Me", 3.5, 1.0, 8.0, 0.5);
    public final BooleanSetting breakInstantly = new BooleanSetting("Break Instantly", true);
    public final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);
    public final BooleanSetting onlyWhileKeyHeld = new BooleanSetting("Only While Key Held", true);
    public final BooleanSetting includeBedrock = new BooleanSetting("Include Bedrock", true);
    public final BooleanSetting playersOnly = new BooleanSetting("Players Only", true);
    public final BooleanSetting placeObsidian = new BooleanSetting("Place Obsidian", true);
    public final BooleanSetting obsidianOnlyAbove = new BooleanSetting("Obsidian Only Above", true);

    private int cooldown;
    private int previousSlot = -1;
    private BlockPos pendingSpot;
    private int pendingTicks;

    public CrystalAuraAddonModule() {
        super("Vortex + | Crystal Aura", Category.HUD);
        addSetting(targetRange);
        addSetting(placeRange);
        addSetting(breakRange);
        addSetting(delay);
        addSetting(rotate);
        addSetting(onlyUseful);
        addSetting(maxSpread);
        addSetting(selfProtect);
        addSetting(minSelfDistance);
        addSetting(breakInstantly);
        addSetting(switchBack);
        addSetting(onlyWhileKeyHeld);
        addSetting(includeBedrock);
        addSetting(playersOnly);
        addSetting(placeObsidian);
        addSetting(obsidianOnlyAbove);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() {
        restoreSlot(MinecraftClient.getInstance());
        pendingSpot = null;
        cooldown = 0;
    }

    private void onTick(MinecraftClient client) {
        if (!isEnabled() || client.player == null || client.world == null
                || client.interactionManager == null) return;
        ClientPlayerEntity player = client.player;
        if (onlyWhileKeyHeld.get() && !client.options.attackKey.isPressed()) return;
        if (client.currentScreen != null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        Entity target = findTarget(client);
        if (target == null) {
            pendingSpot = null;
            return;
        }

        // 1) Kristall in der Naehe des Ziels zuerst zerstoeren.
        EndCrystalEntity crystal = findCrystal(client, player, target);
        if (crystal != null) {
            if (rotate.get()) rotateTowards(player, crystal);
            if (breakInstantly.get() || player.getAttackCooldownProgress(0.0f) >= 1.0f) {
                client.interactionManager.attackEntity(player, crystal);
                player.swingHand(Hand.MAIN_HAND);
                cooldown = delay.getInt();
            }
            return;
        }

        // 2) Obsidian wurde gerade gelegt -> jetzt Kristall darauf setzen.
        if (pendingSpot != null) {
            if (isCrystalSpot(client, pendingSpot)) {
                placeCrystal(client, player, pendingSpot);
                cooldown = delay.getInt();
                // Done with this spot -- without clearing it the module kept
                // retrying the same place for ten more ticks.
                pendingSpot = null;
                pendingTicks = 0;
                return;
            }
            // Platzierung abwarten (bis zu 10 Ticks), dann aufgeben.
            if (++pendingTicks > 10) pendingSpot = null;
            return;
        }

        // 3) Bestehenden Kristall-Spot in der Naehe des Ziels nutzen.
        BlockPos spot = findSpot(client, player, target);
        if (spot != null) {
            placeCrystal(client, player, spot);
            cooldown = delay.getInt();
            return;
        }

        // 4) Gegner oberhalb: eigenen Obsidian direkt unter das Ziel legen.
        if (placeObsidian.get() && target.getY() - player.getY() > 1.5) {
            if (placeObsidianBelow(client, player, target)) {
                cooldown = delay.getInt();
            }
            return;
        }

        // 5) Optional auch abseits neue Spots bauen (nur wenn erlaubt).
        if (placeObsidian.get() && !obsidianOnlyAbove.get()) {
            BlockPos make = findBuildSpot(client, player, target);
            if (make != null && placeBlockWithSide(client, player, make, Items.OBSIDIAN)) {
                pendingSpot = make;
                pendingTicks = 0;
                cooldown = delay.getInt();
            }
        }
    }

    private Entity findTarget(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        double best = targetRange.get() * targetRange.get();
        Entity bestEntity = null;
        double px = player.getX();
        double py = player.getEyeY();
        double pz = player.getZ();
        for (Entity entity : client.world.getEntities()) {
            if (entity == player || !entity.isAlive() || entity.isSpectator()) continue;
            if (playersOnly.get() && !(entity instanceof PlayerEntity)) continue;
            double dx = entity.getX() - px;
            double dy = entity.getY() - py;
            double dz = entity.getZ() - pz;
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < best) {
                best = dist;
                bestEntity = entity;
            }
        }
        return bestEntity;
    }

    /**
     * A crystal worth breaking, near the target and away from us.
     *
     * The distance to us used to go unchecked entirely: any crystal close to
     * the opponent was fair game, including one standing right beside you. The
     * blast makes no distinction, so that was a good way to kill yourself with
     * your own module.
     */
    private EndCrystalEntity findCrystal(MinecraftClient client,
                                         ClientPlayerEntity player, Entity target) {
        double max = breakRange.get();
        double maxSq = max * max;
        double tx = target.getX();
        double ty = target.getY();
        double tz = target.getZ();

        double safe = minSelfDistance.get();
        double safeSq = safe * safe;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;

            double dx = crystal.getX() - tx;
            double dy = crystal.getY() - ty;
            double dz = crystal.getZ() - tz;
            if (dx * dx + dy * dy + dz * dz > maxSq) continue;

            if (selfProtect.get()) {
                double sx = crystal.getX() - player.getX();
                double sy = crystal.getY() - player.getY();
                double sz = crystal.getZ() - player.getZ();
                if (sx * sx + sy * sy + sz * sz < safeSq) continue;
            }

            // Same three conditions as for placing.
            if (onlyUseful.get()) {
                double fx = crystal.getX() - tx;
                double fz = crystal.getZ() - tz;
                if (Math.sqrt(fx * fx + fz * fz) > maxSpread.get()) continue;

                // Opponent above the crystal, and the block it stands on at
                // your level or higher -- so that block shields you.
                double base = crystal.getY();
                if (target.getY() < base - 0.1) continue;
                if (base - 1.0 < player.getY() - 0.1) continue;
                if (target.getY() > base + 4.0) continue;
            }
            return crystal;
        }
        return null;
    }

    private BlockPos findSpot(MinecraftClient client, ClientPlayerEntity player, Entity target) {
        // The block the target is standing on is deliberately NOT preferred.
        //
        // A crystal placed there would fill the two blocks above it -- the
        // space the opponent occupies -- and the game refuses to put anything
        // inside an entity. Trying it wastes the attempt every single time.
        // The search below skips it and takes the nearest usable neighbour.

        int radius = Math.min(6, (int) Math.ceil(placeRange.get()));
        double maxSq = placeRange.get() * placeRange.get();
        BlockPos targetPos = target.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = targetPos.add(dx, dy, dz);
                    if (!isCrystalSpot(client, pos)) continue;
                    if (!spotIsUseful(client, player, target, pos)) continue;
                    double pdx = pos.getX() + 0.5 - player.getX();
                    double pdy = pos.getY() + 0.5 - player.getY();
                    double pdz = pos.getZ() + 0.5 - player.getZ();
                    if (pdx * pdx + pdy * pdy + pdz * pdz > maxSq) continue;
                    double tdx = pos.getX() + 0.5 - target.getX();
                    double tdy = pos.getY() + 0.5 - target.getY();
                    double tdz = pos.getZ() + 0.5 - target.getZ();
                    double dist = tdx * tdx + tdy * tdy + tdz * tdz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private boolean placeObsidianBelow(MinecraftClient client, ClientPlayerEntity player, Entity target) {
        BlockPos spot = target.getBlockPos();
        // Liegt schon ein Kristall-Spot unter dem Ziel? Dann direkt Kristall.
        if (isCrystalSpot(client, spot)) {
            placeCrystal(client, player, spot);
            return true;
        }
        if (!client.world.getBlockState(spot).isAir()) return false;
        return placeBlockWithSide(client, player, spot, Items.OBSIDIAN);
    }

    private BlockPos findBuildSpot(MinecraftClient client, ClientPlayerEntity player, Entity target) {
        int radius = Math.min(6, (int) Math.ceil(placeRange.get()));
        double maxSq = placeRange.get() * placeRange.get();
        BlockPos targetPos = target.getBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = targetPos.add(dx, dy, dz);
                    if (!client.world.getBlockState(pos).isAir()) continue;
                    if (!client.world.getBlockState(pos.up()).isAir()) continue;
                    if (findPlaceSide(client, pos) == null) continue;
                    if (withinRange(player, pos, placeRange.get())) return pos;
                }
            }
        }
        return null;
    }

    private boolean placeBlockWithSide(MinecraftClient client, ClientPlayerEntity player,
                                       BlockPos pos, Item item) {
        Direction side = findPlaceSide(client, pos);
        if (side == null) return false;
        int slot = findSlot(player, item);
        if (slot < 0) return false;
        if (rotate.get()) rotateTowards(player, Vec3d.ofCenter(pos));
        selectSlot(player, slot);
        placeBlock(client, player, pos, side);
        if (switchBack.get()) restoreSlot(client);
        return true;
    }

    private void placeCrystal(MinecraftClient client, ClientPlayerEntity player, BlockPos spot) {
        int slot = findSlot(player, Items.END_CRYSTAL);
        if (slot < 0) return;
        if (rotate.get()) rotateTowards(player, Vec3d.ofCenter(spot));
        selectSlot(player, slot);
        useOn(client, player, spot, Direction.UP);
        if (switchBack.get()) restoreSlot(client);
    }

    private static Direction findPlaceSide(MinecraftClient client, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (client.world.getBlockState(neighbor).isFullCube(client.world, neighbor)) {
                return dir.getOpposite();
            }
        }
        return null;
    }

    private static void placeBlock(MinecraftClient client, ClientPlayerEntity player,
                                   BlockPos pos, Direction side) {
        BlockPos neighbor = pos.offset(side.getOpposite());
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(neighbor), side, neighbor, false);
        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        player.swingHand(Hand.MAIN_HAND);
    }

    /**
     * Is this spot worth using against that target, and safe for us?
     *
     * Two separate questions, both answered here because a spot that fails
     * either one must not be used at all -- not for placing, and not for
     * breaking a crystal that is already there.
     */
    private boolean spotIsUseful(MinecraftClient client, ClientPlayerEntity player,
                                 Entity target, BlockPos pos) {
        // Your rule, in three parts.
        //
        // 1) The opponent must stand ABOVE the obsidian -- at least one block.
        //    A crystal below them catches their legs, where the damage lands.
        // 2) The obsidian must NOT be above your own feet. An explosion above
        //    you rains down on you; one at your level or lower is largely
        //    blocked by whatever you are standing on.
        // 3) Close enough that the blast reaches at all.
        // Never the block they are standing on: no room for the crystal.
        if (pos.equals(target.getBlockPos().down())) return false;

        if (onlyUseful.get()) {
            double crystalBase = pos.getY() + 1.0;   // the crystal sits on top

            // The opponent is on it or above it.
            if (target.getY() < crystalBase - 0.1) return false;

            // The obsidian must be at your level or ABOVE it -- never below.
            //
            // I had this the wrong way round. The block itself is what shields
            // you: the crystal sits on top of it, so with the obsidian above
            // you the block stands between you and the blast and swallows most
            // of it. With the obsidian below you the crystal ends up level with
            // you, nothing in between, and you take it full.
            if (pos.getY() < player.getY() - 0.1) return false;

            double dx = pos.getX() + 0.5 - target.getX();
            double dz = pos.getZ() + 0.5 - target.getZ();
            if (Math.sqrt(dx * dx + dz * dz) > maxSpread.get()) return false;

            // And within reach of the blast at all.
            if (target.getY() > crystalBase + 4.0) return false;
        }

        // And it must not be close enough to catch us.
        if (selfProtect.get()) {
            double dx = pos.getX() + 0.5 - player.getX();
            double dy = pos.getY() + 1.0 - player.getY();
            double dz = pos.getZ() + 0.5 - player.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < minSelfDistance.get()) return false;

            // Standing on the block itself is the worst case of all -- the
            // crystal goes off directly under your feet.
            if (player.getBlockPos().down().equals(pos)
                    || player.getBlockPos().equals(pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCrystalSpot(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        if (!state.isOf(Blocks.OBSIDIAN)
                && !(includeBedrock.get() && state.isOf(Blocks.BEDROCK))) {
            return false;
        }
        return client.world.getBlockState(pos.up()).isAir()
                && client.world.getBlockState(pos.up(2)).isAir();
    }

    private static boolean withinRange(ClientPlayerEntity player, BlockPos pos, double range) {
        double dx = pos.getX() + 0.5 - player.getX();
        double dy = pos.getY() + 0.5 - player.getY();
        double dz = pos.getZ() + 0.5 - player.getZ();
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    private static int findSlot(ClientPlayerEntity player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            var stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.isOf(item)) return slot;
        }
        return -1;
    }

    private void selectSlot(ClientPlayerEntity player, int slot) {
        // Through Slots, which tells the server about the change.
        //
        // This method used to set the slot locally only. The server kept the
        // old item in your hand, so every crystal placed with the "new" one was
        // rejected -- the module looked completely dead while doing everything
        // else right.
        int before = Slots.select(player, slot);
        if (before >= 0 && previousSlot < 0) previousSlot = before;
    }

    private void restoreSlot(MinecraftClient client) {
        if (previousSlot < 0 || client.player == null) return;
        Slots.restore(client.player, previousSlot);
        previousSlot = -1;
    }

    private static void useOn(MinecraftClient client, ClientPlayerEntity player,
                              BlockPos pos, Direction side) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), side, pos, false);
        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        player.swingHand(Hand.MAIN_HAND);
    }

    private static void rotateTowards(ClientPlayerEntity player, Entity entity) {
        rotateTowards(player, new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ()));
    }

    private static void rotateTowards(ClientPlayerEntity player, Vec3d pos) {
        Vec3d eye = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
        double dx = pos.x - eye.x;
        double dy = pos.y - eye.y;
        double dz = pos.z - eye.z;
        double horizontal = MathHelper.sqrt((float) (dx * dx + dz * dz));
        float yaw = (float) Math.toDegrees(MathHelper.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(MathHelper.atan2(dy, horizontal));
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
    }
}
