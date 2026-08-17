package de.vortexplus.addon;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

/**
 * Hotbar handling, in one place.
 *
 * THE BUG THIS FIXES: two of the modules changed the slot locally and never
 * told the server. The server therefore still believed you were holding the
 * previous item, and every placement made with the "new" one was rejected --
 * which is why Crystal Aura appeared to do nothing at all. A third module did
 * it correctly, so the same code existed twice in two versions, one of them
 * broken.
 *
 * Now there is one version, and it is the working one.
 */
public final class Slots {

    private Slots() {}

    /**
     * Selects a hotbar slot and tells the server about it.
     *
     * @return the slot that was selected before, or -1 if nothing changed
     */
    public static int select(ClientPlayerEntity player, int slot) {
        if (player == null || slot < 0 || slot > 8) return -1;
        int current = player.getInventory().getSelectedSlot();
        if (current == slot) return -1;

        player.getInventory().setSelectedSlot(slot);
        // Without this the server keeps the old item in your hand.
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        return current;
    }

    /** Goes back to a slot remembered earlier. */
    public static void restore(ClientPlayerEntity player, int previous) {
        if (player == null || previous < 0 || previous > 8) return;
        player.getInventory().setSelectedSlot(previous);
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previous));
    }

    /** Hotbar slot holding this item, or -1. */
    public static int find(ClientPlayerEntity player, Item item) {
        if (player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}
