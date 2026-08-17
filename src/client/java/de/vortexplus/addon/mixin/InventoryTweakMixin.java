package de.vortexplus.addon.mixin;

import com.vortex.client.module.ModuleManager;
import de.vortexplus.addon.InventoryTweakAddonModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses Minecraft's own quick-move action for one sneak-click. */
@Mixin(HandledScreen.class)
public abstract class InventoryTweakMixin {
    @Shadow protected abstract Slot getSlotAt(double x, double y);

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void vortexplus$quickMove(Click click, boolean doubled,
                                      CallbackInfoReturnable<Boolean> cir) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            InventoryTweakAddonModule module = ModuleManager.INSTANCE.get(InventoryTweakAddonModule.class);
            if (module == null || !module.isEnabled() || click.button() != 0
                    || client.player == null || client.interactionManager == null
                    || !client.options.sneakKey.isPressed()) return;
            Slot slot = getSlotAt(click.x(), click.y());
            if (slot == null || !slot.hasStack()) return;
            ScreenHandler handler = ((HandledScreen<?>)(Object)this).getScreenHandler();
            client.interactionManager.clickSlot(handler.syncId, slot.id, 0,
                    SlotActionType.QUICK_MOVE, client.player);
            cir.setReturnValue(true);
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("InventoryTweak", error);
        }
    }
}
