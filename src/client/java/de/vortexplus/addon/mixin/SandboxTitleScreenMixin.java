package de.vortexplus.addon.mixin;

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
 * Keeps the Sandbox entry points visually close to Minecraft's title screen.
 * The three Vortex actions are aligned beneath the logo without covering the
 * Mojang splash text, panorama, options row, or normal button behavior.
 */
@Mixin(TitleScreen.class)
public abstract class SandboxTitleScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void vortexplus$layoutSandboxActions(CallbackInfo ci) {
        try {
            Screen screen = (Screen) (Object) this;
            int width = 230;
            int height = 20;
            int x = screen.width / 2 - width / 2;
            // The logo occupies the upper third. Start just below it and keep
            // a compact, familiar Minecraft 4-pixel rhythm between actions.
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

    private static void styleAction(ClickableWidget button, int x, int y, int width, int height, String label) {
        button.setDimensionsAndPosition(width, height, x, y);
        button.setMessage(Text.literal(label));
    }
}
