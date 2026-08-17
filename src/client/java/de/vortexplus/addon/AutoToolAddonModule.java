package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * Auto Tool: wechselt beim Abbauen automatisch auf das beste Werkzeug in der
 * Hotbar (hoechste Mining-Speed fuer den anvisierten Block).
 */
public final class AutoToolAddonModule extends Module {
    public final BooleanSetting onlyWhileKeyHeld = new BooleanSetting("Only While Key Held", true);
    public final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);

    private int previousSlot = -1;

    public AutoToolAddonModule() {
        super("Auto Tool", Category.MISC);
        addSetting(onlyWhileKeyHeld);
        addSetting(switchBack);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() {
        restoreSlot(MinecraftClient.getInstance());
        previousSlot = -1;
    }

    /**
     * Minimum speed gain to justify a switch.
     *
     * Without this, two items tied at the same mining speed (e.g. both at the
     * default 1.0 because neither is right for the block) still caused a
     * pointless slot flip every tick when floating point rounding nudged one
     * a hair above the other, and the hotbar visibly flickered.
     */
    private static final float MIN_GAIN = 0.05f;

    private void onTick(MinecraftClient client) {
        if (!isEnabled() || client.player == null || client.world == null) return;
        ClientPlayerEntity player = client.player;
        boolean mining = client.options.attackKey.isPressed()
                && client.crosshairTarget instanceof BlockHitResult
                && ((BlockHitResult) client.crosshairTarget).getType() == HitResult.Type.BLOCK;

        if (!mining) {
            // Switch back whenever mining stops, not only when the setting
            // requires the key to be held -- otherwise "Switch Back" with
            // "Only While Key Held" off never actually ran, since this branch
            // used to be skipped entirely in that combination.
            if (switchBack.get()) restoreSlot(client);
            return;
        }

        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        if (state.isAir()) return;

        int current = player.getInventory().getSelectedSlot();
        float currentSpeed = player.getInventory().getStack(current).getMiningSpeedMultiplier(state);
        int best = current;
        float bestSpeed = currentSpeed;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = slot;
            }
        }
        // Only switch when the gain is actually worth it -- otherwise a tie
        // (or a rounding-level difference) triggers a slot change that helps
        // nothing and just flickers the hotbar every tick.
        if (best == current || bestSpeed - currentSpeed < MIN_GAIN) return;

        // Through Slots, so the server learns about the change too. Set only
        // locally, the server kept swinging with the old item -- the block then
        // took its full time to break and the module seemed to do nothing.
        int before = Slots.select(player, best);
        if (before >= 0 && previousSlot < 0) previousSlot = before;
    }

    private void restoreSlot(MinecraftClient client) {
        if (previousSlot < 0 || client.player == null) return;
        Slots.restore(client.player, previousSlot);
        previousSlot = -1;
    }
}
