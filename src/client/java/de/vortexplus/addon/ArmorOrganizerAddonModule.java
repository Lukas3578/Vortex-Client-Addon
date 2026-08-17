package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/** Equips armor into empty armor slots using ordinary inventory clicks. */
public final class ArmorOrganizerAddonModule extends Module {
    public final BooleanSetting onlyEmptySlots = new BooleanSetting("Only Empty Slots", true);
    private int cooldown;

    public ArmorOrganizerAddonModule() {
        super("Armor Organizer", Category.CHEATS);
        addSetting(onlyEmptySlots);
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
            for (int armorType = 0; armorType < 4; armorType++) {
                int armorInventoryIndex = 36 + armorType;
                if (onlyEmptySlots.get() && !player.getInventory().getStack(armorInventoryIndex).isEmpty()) continue;
                int source = findArmor(player, armorType);
                if (source < 0) continue;
                equip(client, player, source, 5 + armorType);
                cooldown = 3;
                return;
            }
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("ArmorOrganizer", error);
        }
    }

    private static int findArmor(ClientPlayerEntity player, int armorType) {
        for (int inventory = 9; inventory < 36; inventory++) {
            ItemStack stack = player.getInventory().getStack(inventory);
            if (!stack.isEmpty() && armorType(stack) == armorType) return inventory;
        }
        return -1;
    }

    private static void equip(MinecraftClient client, ClientPlayerEntity player,
                              int inventorySlot, int armorScreenSlot) {
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                inventorySlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                armorScreenSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                inventorySlot, 0, SlotActionType.PICKUP, player);
    }

    private static int armorType(ItemStack stack) {
        String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath();
        if (id.endsWith("helmet") || id.endsWith("cap") || id.endsWith("skull")) return 0;
        if (id.endsWith("chestplate") || id.endsWith("elytra")) return 1;
        if (id.endsWith("leggings")) return 2;
        if (id.endsWith("boots")) return 3;
        return -1;
    }
}
