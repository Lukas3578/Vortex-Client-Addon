package de.vortexplus.addon.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the game from opening its pause menu when you click away.
 *
 * THE PROBLEM THIS SOLVES: Spawner Safer closes whatever screen is open, but
 * losing window focus makes the game open the pause menu again straight away.
 * The module closed it, the game reopened it, and around it went -- which is
 * why nothing happened until you clicked back into the window.
 *
 * Only while the module is actually working. At every other time the pause
 * menu behaves exactly as it always has: clicking away still pauses.
 */
@Mixin(MinecraftClient.class)
public abstract class FocusPauseMixin {

    @Inject(method = "method_15995", at = @At("HEAD"), cancellable = true, require = 0)
    private void vortexplus$keepFocusMenuClosed(boolean focused, CallbackInfo ci) {
        try {
            if (focused) return;   // clicking back in is none of our business

            var mod = com.vortex.client.module.ModuleManager.INSTANCE.get(
                    de.vortexplus.addon.SpawnerSaferAddonModule.class);
            if (mod != null && mod.isEnabled() && mod.isWorking()) {
                ci.cancel();
            }
        } catch (Throwable pvpErr) {
            com.vortex.client.core.Errors.report("FocusPauseMixin", pvpErr);
        }
    }
}
