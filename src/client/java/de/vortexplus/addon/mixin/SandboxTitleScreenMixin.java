package de.vortexplus.addon.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the Sandbox title screen a concise Vortex entry area while preserving
 * vanilla button actions. The normal launcher never ships this addon build.
 */
@Mixin(TitleScreen.class)
public abstract class SandboxTitleScreenMixin {
    private static final int VORTEX_CYAN = 0xFF39E7D4;
    private static final int VORTEX_PANEL = 0xB30A1222;
    private static final int VORTEX_PANEL_EDGE = 0xA83B7CC7;

    @Shadow protected int width;
    @Shadow protected int height;

    @Inject(method = "init", at = @At("TAIL"))
    private void vortexplus$layoutSandboxActions(CallbackInfo ci) {
        try {
            int x = width / 2 - 160;
            int y = height / 2 - 58;
            for (Element child : ((Screen) (Object) this).children()) {
                if (!(child instanceof ClickableWidget button)) continue;
                String label = button.getMessage().getString();
                if ("Singleplayer".equals(label)) {
                    styleAction(button, x, y, "SINGLEPLAYER WORLDS");
                } else if ("Multiplayer".equals(label)) {
                    styleAction(button, x, y + 40, "VORTEX SERVERS");
                } else if ("Skins".equals(label)) {
                    styleAction(button, x, y + 80, "VORTEX SKINS");
                }
            }
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("SandboxTitleMenu", error);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.BEFORE))
    private void vortexplus$drawSandboxFrame(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            int left = width / 2 - 172;
            int top = height / 2 - 88;
            int right = left + 344;
            int bottom = top + 180;

            // A restrained glass-like panel and cyan accent keep the original Minecraft view visible.
            context.fill(left, top, right, bottom, VORTEX_PANEL);
            context.fill(left, top, right, top + 1, VORTEX_PANEL_EDGE);
            context.fill(left, bottom - 1, right, bottom, VORTEX_PANEL_EDGE);
            context.fill(left, top, left + 3, bottom, VORTEX_CYAN);
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                    Text.literal("VORTEX SANDBOX"), left + 16, top + 12, VORTEX_CYAN);
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                    Text.literal("Choose where you want to play"), left + 16, top + 27, 0xFFC4D2E8);
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("SandboxTitleMenu", error);
        }
    }

    private static void styleAction(ClickableWidget button, int x, int y, String label) {
        button.setDimensionsAndPosition(320, 34, x, y);
        button.setMessage(Text.literal(label));
    }
}
