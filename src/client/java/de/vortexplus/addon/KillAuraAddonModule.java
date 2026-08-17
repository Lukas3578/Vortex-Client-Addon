package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Kill Aura: greift automatisch das naechste lebende Ziel in Reichweite an.
 * Rotation, Angriffs-Cooldown und Zielauswahl sind konfigurierbar.
 */
public final class KillAuraAddonModule extends Module {
    public final NumberSetting range = new NumberSetting("Range", 4.0, 1.0, 6.0, 0.5);
    public final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    public final BooleanSetting onlyWhileKeyHeld = new BooleanSetting("Only While Key Held", true);
    public final BooleanSetting playersOnly = new BooleanSetting("Players Only", true);

    /**
     * Ticks between attacks.
     *
     * Was hard-wired to two. The attack cooldown already paces this, so the
     * number mostly decides how hard the module leans on that -- worth having
     * as a dial rather than a decision someone else made.
     */
    public final NumberSetting delay = new NumberSetting("Delay (ticks)", 2, 0, 20, 1);

    /**
     * Attack through walls.
     *
     * Off by default, and it should stay off. The server checks the line of
     * sight and throws the hit away, so all it does is send attacks that go
     * nowhere -- while being one of the easiest things in the world to spot.
     */
    public final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);

    /** Include armour stands. Off, because hitting one is rarely the plan. */
    public final BooleanSetting hitArmorStands = new BooleanSetting("Hit Armour Stands", false);

    private int cooldown;
    private Entity target;

    public KillAuraAddonModule() {
        super("Vortex + | Kill Aura", Category.HUD);
        addSetting(range);
        addSetting(rotate);
        addSetting(onlyWhileKeyHeld);
        addSetting(playersOnly);
        addSetting(delay);
        addSetting(throughWalls);
        addSetting(hitArmorStands);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() {
        target = null;
        cooldown = 0;
    }

    private void onTick(MinecraftClient client) {
        if (!isEnabled() || client.player == null || client.world == null
                || client.interactionManager == null) return;
        ClientPlayerEntity player = client.player;
        if (onlyWhileKeyHeld.get() && !client.options.attackKey.isPressed()) {
            target = null;
            return;
        }
        if (client.currentScreen != null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        target = findTarget(client);
        if (target == null) return;
        if (rotate.get()) rotateTowards(player, target);
        if (player.getAttackCooldownProgress(0.0f) >= 1.0f) {
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
            cooldown = delay.getInt();
        }
    }

    private Entity findTarget(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        double best = range.get() * range.get();
        Entity bestEntity = null;
        double px = player.getX();
        double py = player.getEyeY();
        double pz = player.getZ();
        for (Entity entity : client.world.getEntities()) {
            if (entity == player || !entity.isAlive() || entity.isSpectator()) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            if (playersOnly.get() && !(entity instanceof PlayerEntity)) continue;
            if (!hitArmorStands.get()
                    && entity instanceof net.minecraft.entity.decoration.ArmorStandEntity) {
                continue;
            }
            // A wall in between means the server discards the hit anyway.
            if (!throughWalls.get() && !player.canSee(entity)) continue;
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

    private static void rotateTowards(ClientPlayerEntity player, Entity target) {
        Vec3d eye = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
        double dx = target.getX() - eye.x;
        double dy = target.getEyeY() + target.getHeight() * 0.3 - eye.y;
        double dz = target.getZ() - eye.z;
        double horizontal = MathHelper.sqrt((float) (dx * dx + dz * dz));
        float yaw = (float) Math.toDegrees(MathHelper.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(MathHelper.atan2(dy, horizontal));
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
    }
}
