package de.vortexplus.addon.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the legacy core hat renderer from drawing its detached translucent atlas piece. */
@Mixin(targets = "com.vortex.client.cosmetics.VortexHatFeatureRenderer")
public abstract class DisableCoreHatRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void vortexplus$disableLegacyHat(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                             PlayerEntityRenderState state, float limbAngle, float limbDistance,
                                             CallbackInfo ci) {
        ci.cancel();
    }
}
