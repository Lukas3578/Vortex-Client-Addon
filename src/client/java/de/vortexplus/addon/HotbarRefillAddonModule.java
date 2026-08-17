package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/** Refills compatible hotbar stacks through normal inventory clicks. */
public final class HotbarRefillAddonModule extends Module {
    public final NumberSetting minimum =
            new NumberSetting("Refill Below", 8, 1, 32, 1);
    public final BooleanSetting emptySlots =
            new BooleanSetting("Fill Empty Slots", false);

    private int cooldown;

    public HotbarRefillAddonModule() {
        super("Hotbar Refill", Category.CHEATS);
        addSetting(minimum);
        addSetting(emptySlots);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() {
        cooldown = 0;
    }

    private void onTick(MinecraftClient client) {
        try {
            if (!isEnabled() || client.player == null || client.interactionManager == null) return;
            if (!(client.currentScreen instanceof InventoryScreen)) return;
            if (cooldown-- > 0) return;

            ClientPlayerEntity player = client.player;
            for (int hotbar = 0; hotbar < 9; hotbar++) {
                ItemStack target = player.getInventory().getStack(hotbar);
                if (target.isEmpty()) {
                    if (!emptySlots.get()) continue;
                } else if (target.getCount() >= minimum.getInt()
                        || !target.isStackable()) {
                    continue;
                }

                int source = findCompatibleSource(player, target);
                if (source < 0) continue;
                moveIntoHotbar(client, player, hotbar, source);
                cooldown = 3;
                return;
            }
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("HotbarRefill", error);
        }
    }

    private static int findCompatibleSource(ClientPlayerEntity player, ItemStack target) {
        for (int inventory = 9; inventory < 36; inventory++) {
            ItemStack candidate = player.getInventory().getStack(inventory);
            if (candidate.isEmpty()) continue;
            if (target.isEmpty() || ItemStack.areItemsAndComponentsEqual(target, candidate)) {
                return inventory;
            }
        }
        return -1;
    }

    private static void moveIntoHotbar(MinecraftClient client, ClientPlayerEntity player,
                                       int hotbar, int inventory) {
        int hotbarScreenSlot = 36 + hotbar;
        int inventoryScreenSlot = inventory;
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                hotbarScreenSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                inventoryScreenSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                hotbarScreenSlot, 0, SlotActionType.PICKUP, player);
    }
}
