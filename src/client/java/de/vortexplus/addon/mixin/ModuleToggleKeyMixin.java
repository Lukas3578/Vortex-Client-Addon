package de.vortexplus.addon.mixin;

import com.vortex.client.module.Module;
import com.vortex.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Makes every module's configurable Toggle Key perform a real one-shot toggle. */
@Mixin(MinecraftClient.class)
public abstract class ModuleToggleKeyMixin {
    private static final Set<Module> VORTEXPLUS_HELD =
            Collections.newSetFromMap(new IdentityHashMap<>());

    @Inject(method = "tick", at = @At("HEAD"))
    private void vortexplus$handleModuleToggleKeys(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            int key = module.getToggleKey().getKeyCode();
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;
            boolean pressed = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
            if (pressed) {
                if (VORTEXPLUS_HELD.add(module)) module.toggle();
            } else {
                VORTEXPLUS_HELD.remove(module);
            }
        }
    }
}
