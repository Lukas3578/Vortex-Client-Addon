package de.vortexplus.addon.mixin;

import com.vortex.client.module.ModuleManager;
import de.vortexplus.addon.InventoryTweakAddonModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Moves a stack once when the mouse hovers over it while Sneak/Shift is held. */
@Mixin(HandledScreen.class)
public abstract class InventoryTweakMixin {
    @Shadow protected abstract Slot getSlotAt(double x, double y);

    private int vortexplus$lastSlot = -1;
    private int vortexplus$lastSyncId = -1;
    private long vortexplus$lastMoveAt;

    @Inject(method = "renderMain", at = @At("HEAD"))
    private void vortexplus$moveOnHover(DrawContext context, int mouseX, int mouseY,
                                         float delta, CallbackInfo ci) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            InventoryTweakAddonModule module = ModuleManager.INSTANCE.get(InventoryTweakAddonModule.class);
            if (module == null || !module.isEnabled() || client.player == null
                    || client.interactionManager == null || !client.options.sneakKey.isPressed()) return;

            Slot slot = getSlotAt(mouseX, mouseY);
            if (slot == null || !slot.hasStack()) {
                vortexplus$lastSlot = -1;
                return;
            }
            ScreenHandler handler = ((HandledScreen<?>)(Object)this).getScreenHandler();
            if (slot.id == vortexplus$lastSlot && handler.syncId == vortexplus$lastSyncId) return;
            long now = System.currentTimeMillis();
            if (now - vortexplus$lastMoveAt < Math.max(1, module.delay.getInt()) * 10L) return;

            vortexplus$lastSlot = slot.id;
            vortexplus$lastSyncId = handler.syncId;
            vortexplus$lastMoveAt = now;
            client.interactionManager.clickSlot(handler.syncId, slot.id, 0,
                    SlotActionType.QUICK_MOVE, client.player);
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("InventoryTweak", error);
        }
    }
}
