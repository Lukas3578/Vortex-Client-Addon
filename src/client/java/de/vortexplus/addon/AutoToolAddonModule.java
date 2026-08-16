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
        super("Auto Tool", Category.CHEATS);
        addSetting(onlyWhileKeyHeld);
        addSetting(switchBack);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() {
        restoreSlot(MinecraftClient.getInstance());
        previousSlot = -1;
    }

    private void onTick(MinecraftClient client) {
        if (!isEnabled() || client.player == null || client.world == null) return;
        ClientPlayerEntity player = client.player;
        boolean mining = client.options.attackKey.isPressed()
                && client.crosshairTarget instanceof BlockHitResult
                && ((BlockHitResult) client.crosshairTarget).getType() == HitResult.Type.BLOCK;

        if (onlyWhileKeyHeld.get() && !mining) {
            restoreSlot(client);
            return;
        }
        if (!mining) return;

        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        if (state.isAir()) return;

        int best = player.getInventory().getSelectedSlot();
        float bestSpeed = player.getInventory().getStack(best).getMiningSpeedMultiplier(state);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = slot;
            }
        }
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
