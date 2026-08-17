package de.vortexplus.addon.mixin;

import de.vortexplus.addon.FfmpegReplayRecorder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures finished frames only while the local FFmpeg recorder is active. */
@Mixin(GameRenderer.class)
public abstract class GameRendererCaptureMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void vortexplus$capture(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        FfmpegReplayRecorder.captureFrame();
    }
}
