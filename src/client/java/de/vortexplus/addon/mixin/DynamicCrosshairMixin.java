package de.vortexplus.addon.mixin;

import de.vortexplus.addon.LocalFeaturesAddonModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only the vanilla crosshair while Dynamic Crosshair is enabled. */
@Mixin(InGameHud.class)
public abstract class DynamicCrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void vortexplus$dynamic(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!LocalFeaturesAddonModule.dynamicCrosshairEnabled()) return;
        LocalFeaturesAddonModule.drawDynamicCrosshair(context);
        ci.cancel();
    }
}
