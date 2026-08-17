package de.vortexplus.addon.mixin;

import com.vortex.client.module.ModuleManager;
import de.vortexplus.addon.LocalFeaturesAddonModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Passively forwards already visible chat text to local feature tracking. */
@Mixin(ChatHud.class)
public abstract class LocalFeaturesChatMixin {
    @ModifyVariable(method = "method_1812", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text vortexplus$observe(Text message) {
        try {
            LocalFeaturesAddonModule module = ModuleManager.INSTANCE.get(LocalFeaturesAddonModule.class);
            if (module != null && message != null) {
                module.observeChat(MinecraftClient.getInstance(), message.getString());
            }
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("LocalFeaturesChat", error);
        }
        return message;
    }
}
