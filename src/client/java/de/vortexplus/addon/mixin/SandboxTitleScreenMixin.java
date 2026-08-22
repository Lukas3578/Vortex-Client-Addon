package de.vortexplus.addon.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refines only the Sandbox entry actions with restrained Vortex cyan accents.
 * The Minecraft logo, panorama, splash text, Realms row and utility buttons
 * remain in their vanilla positions and style.
 */
@Mixin(TitleScreen.class)
public abstract class SandboxTitleScreenMixin {
    private static final int VORTEX_CYAN = 0xFF67E9F8;
    private static final int VORTEX_BLUE = 0xFF4E7CFF;
    private static final int VORTEX_TEXT = 0xFFD7F8FF;

    @Inject(method = "init", at = @At("TAIL"))
    private void vortexplus$layoutSandboxActions(CallbackInfo ci) {
        try {
            Screen screen = (Screen) (Object) this;
            int width = 230;
            int height = 20;
            int x = screen.width / 2 - width / 2;
            // A three-button Vortex group sits below the logo and finishes
            // before the untouched Realms row begins.
            int y = screen.height / 4 + 58;
            for (Element child : screen.children()) {
                if (!(child instanceof ClickableWidget button)) continue;
                String label = button.getMessage().getString();
                if ("Singleplayer".equals(label)) {
                    styleAction(button, x, y, width, height, "Singleplayer");
                } else if ("Multiplayer".equals(label)) {
                    styleAction(button, x, y + 24, width, height, "Vortex Servers");
                } else if ("Skins".equals(label)) {
                    styleAction(button, x, y + 48, width, height, "Vortex Skins");
                }
            }
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("SandboxTitleMenu", error);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.BEFORE))
    private void vortexplus$drawVortexAccents(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            Screen screen = (Screen) (Object) this;
            int x = screen.width / 2 - 115;
            int y = screen.height / 4 + 58;
            for (int index = 0; index < 3; index++) {
                int row = y + index * 24;
                int accent = index == 1 ? VORTEX_BLUE : VORTEX_CYAN;
                // Small left rail and top trace: Vortex character, but no panel overlay.
                context.fill(x + 2, row + 2, x + 4, row + 18, accent);
                context.fill(x + 5, row + 2, x + 36, row + 3, accent);
            }
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("SandboxTitleMenu", error);
        }
    }

    private static void styleAction(ClickableWidget button, int x, int y, int width, int height, String label) {
        button.setDimensionsAndPosition(width, height, x, y);
        button.setMessage(Text.literal(label).styled(style -> style.withColor(VORTEX_TEXT)));
    }
}
