package de.vortexplus.addon.mixin;

import de.vortexplus.addon.VortexPlusHudModule;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Passively forwards visible server chat text to the local tracker. */
@Mixin(ChatHud.class)
public abstract class VortexPlusChatMixin {
    @ModifyVariable(method = "method_1812", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text vortexplus$observe(Text message) {
        try {
            if (message != null) VortexPlusHudModule.observeChat(message.getString());
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("VortexPlusChat", error);
        }
        return message;
    }
}
