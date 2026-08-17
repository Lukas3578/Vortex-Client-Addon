package de.vortexplus.addon;

import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/** Combines compatible partial stacks in the player's own inventory. */
public final class InventoryCompactorAddonModule extends Module {
    public final NumberSetting delay = new NumberSetting("Delay (ticks)", 2, 1, 10, 1);
    private int cooldown;

    public InventoryCompactorAddonModule() {
        super("Inventory Compactor", Category.CHEATS);
        addSetting(delay);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() { cooldown = 0; }

    private void onTick(MinecraftClient client) {
        try {
            if (!isEnabled() || client.player == null || client.interactionManager == null
                    || !(client.currentScreen instanceof InventoryScreen)) return;
            if (cooldown-- > 0) return;
            ClientPlayerEntity player = client.player;
            for (int a = 9; a < 36; a++) {
                ItemStack first = player.getInventory().getStack(a);
                if (first.isEmpty() || first.getCount() >= first.getMaxCount()) continue;
                for (int b = a + 1; b < 36; b++) {
                    ItemStack second = player.getInventory().getStack(b);
                    if (second.isEmpty() || !ItemStack.areItemsAndComponentsEqual(first, second)) continue;
                    merge(client, player, a, b);
                    cooldown = Math.max(1, delay.getInt());
                    return;
                }
            }
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("InventoryCompactor", error);
        }
    }

    private static void merge(MinecraftClient client, ClientPlayerEntity player, int first, int second) {
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, first, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, second, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, first, 0, SlotActionType.PICKUP, player);
    }
}
