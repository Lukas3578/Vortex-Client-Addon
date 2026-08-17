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

/**
 * A conservative, client-side inventory quality-of-life module.
 *
 * It only operates in the player's own inventory screen. It never opens a
 * screen, moves the cursor for the player, touches the hotbar, or interacts
 * with containers. Every movement is sent through the normal screen-handler
 * click path, just like a regular inventory click.
 */
public final class InventorySorterAddonModule extends Module {
    public final BooleanSetting sortByName =
            new BooleanSetting("Sort By Name", true);
    public final NumberSetting delay =
            new NumberSetting("Delay (ticks)", 2, 1, 10, 1);

    private int cooldown;
    private boolean sortedThisOpen;
    private int lastSyncId = -1;

    public InventorySorterAddonModule() {
        super("Inventory Sorter", Category.CHEATS);
        addSetting(sortByName);
        addSetting(delay);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() {
        cooldown = 0;
        sortedThisOpen = false;
        lastSyncId = -1;
    }

    private void onTick(MinecraftClient client) {
        try {
            if (!isEnabled() || client.player == null || client.interactionManager == null) return;
            if (!(client.currentScreen instanceof InventoryScreen)) {
                sortedThisOpen = false;
                lastSyncId = -1;
                return;
            }

            ClientPlayerEntity player = client.player;
            int syncId = player.playerScreenHandler.syncId;
            if (syncId != lastSyncId) {
                lastSyncId = syncId;
                sortedThisOpen = false;
                cooldown = 0;
            }
            if (sortedThisOpen) return;
            if (cooldown-- > 0) return;

            int[] order = buildOrder(player);
            int firstMismatch = -1;
            for (int i = 0; i < order.length; i++) {
                if (order[i] != i) {
                    firstMismatch = i;
                    break;
                }
            }
            if (firstMismatch < 0) {
                sortedThisOpen = true;
                return;
            }

            int source = order[firstMismatch];
            swapInventorySlots(client, player, firstMismatch, source);
            cooldown = Math.max(1, delay.getInt());
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("InventorySorter", error);
        }
    }

    /** Returns source inventory indices for the desired order of slots 9..35. */
    private int[] buildOrder(ClientPlayerEntity player) {
        ItemStack[] stacks = new ItemStack[27];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = player.getInventory().getStack(9 + i);
        }
        Integer[] indices = new Integer[stacks.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        java.util.Arrays.sort(indices, (a, b) -> compare(stacks[a], stacks[b]));
        int[] result = new int[indices.length];
        for (int i = 0; i < result.length; i++) result[i] = indices[i];
        return result;
    }

    private int compare(ItemStack a, ItemStack b) {
        boolean ae = a == null || a.isEmpty();
        boolean be = b == null || b.isEmpty();
        if (ae != be) return ae ? 1 : -1;
        if (ae) return 0;
        int category = Integer.compare(category(a), category(b));
        if (category != 0) return category;
        if (!sortByName.get()) return 0;
        String aid = net.minecraft.registry.Registries.ITEM.getId(a.getItem()).toString();
        String bid = net.minecraft.registry.Registries.ITEM.getId(b.getItem()).toString();
        return aid.compareTo(bid);
    }

    private static int category(ItemStack stack) {
        String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath();
        if (id.contains("sword") || id.contains("axe") || id.contains("pickaxe")
                || id.contains("shovel") || id.contains("hoe")) return 10;
        if (id.contains("helmet") || id.contains("chestplate") || id.contains("leggings")
                || id.contains("boots")) return 20;
        if (id.contains("food") || id.contains("apple") || id.contains("bread")
                || id.contains("beef") || id.contains("pork") || id.contains("carrot")) return 30;
        if (id.contains("potion") || id.contains("bucket") || id.contains("pearl")) return 40;
        if (id.contains("block") || id.contains("stone") || id.contains("plank")) return 50;
        return 100;
    }

    private static void swapInventorySlots(MinecraftClient client, ClientPlayerEntity player,
                                            int first, int second) {
        if (first == second) return;
        // Player inventory indices 9..35 map to player-screen slots 9..35.
        int firstSlot = 9 + first;
        int secondSlot = 9 + second;
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                firstSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                secondSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                firstSlot, 0, SlotActionType.PICKUP, player);
    }
}

So wird nur der eigene Inventarbereich (27 Slots) sortiert; Hotbar und Container bleiben unangetastet.
