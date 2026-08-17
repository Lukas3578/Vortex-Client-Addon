package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Empties an open container into your inventory.
 *
 * WHAT WAS WRONG BEFORE: it moved exactly one stack per tick and then waited
 * two more, so a double chest took about eight seconds -- slower than doing it
 * by hand. It now moves a whole batch per tick, and the batch size is yours to
 * pick.
 *
 * The old "Steal All" switch checked whether an item showed a durability bar,
 * which has nothing to do with being worth taking. It is gone; what replaced it
 * are two switches that mean what they say.
 */
public final class ChestStealerAddonModule extends Module {

    /**
     * Stacks moved per tick.
     *
     * A double chest holds 54. At 54 it empties in a single tick, which is as
     * fast as the protocol allows -- and about as subtle as it sounds.
     */
    public final NumberSetting perTick = new NumberSetting("Stacks Per Tick", 54, 1, 54, 1);

    /** Ticks to wait between batches. Zero means every tick. */
    public final NumberSetting delay = new NumberSetting("Delay (ticks)", 0, 0, 10, 1);

    /** Only run while the use key is held. */
    public final BooleanSetting onlyWhileKeyHeld = new BooleanSetting("Only While Key Held", true);

    /** Close the container once nothing is left. */
    public final BooleanSetting closeWhenEmpty = new BooleanSetting("Close When Empty", true);

    /**
     * Stop when your own inventory is full.
     *
     * Otherwise the module keeps clicking at slots that cannot move anywhere,
     * which achieves nothing and looks exactly like what it is.
     */
    public final BooleanSetting stopWhenFull = new BooleanSetting("Stop When Full", true);

    private int cooldown;
    /**
     * Ticks with items still visible but nothing moved.
     *
     * Happens when the inventory fills up mid-batch: QUICK_MOVE silently does
     * nothing for a slot with nowhere to go, so "moved" stays zero while
     * "anyLeft" stays true forever -- the module used to sit there clicking
     * nothing every tick instead of noticing it was stuck.
     */
    private int stuckTicks;
    private static final int STUCK_LIMIT = 5;

    public ChestStealerAddonModule() {
        super("Vortex + | Chest Stealer", Category.HUD);
        addSetting(perTick);
        addSetting(delay);
        addSetting(onlyWhileKeyHeld);
        addSetting(closeWhenEmpty);
        addSetting(stopWhenFull);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() {
        cooldown = 0;
        stuckTicks = 0;
    }

    private void onTick(MinecraftClient client) {
        try {
            if (!isEnabled() || client.player == null || client.interactionManager == null) return;
            if (!(client.currentScreen instanceof GenericContainerScreen screen)) return;

            ClientPlayerEntity player = client.player;
            if (onlyWhileKeyHeld.get()
                    && (client.options.useKey == null || !client.options.useKey.isPressed())) {
                return;
            }
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (stopWhenFull.get() && inventoryFull(player)) return;

            GenericContainerScreenHandler handler = screen.getScreenHandler();
            int containerSlots = handler.getRows() * 9;
            int budget = perTick.getInt();
            boolean anyLeft = false;
            int moved = 0;

            for (Slot slot : handler.slots) {
                // Only the container half; past that come your own slots, and
                // moving those would push everything straight back in.
                if (slot.id >= containerSlots) break;
                if (!slot.hasStack()) continue;

                ItemStack stack = slot.getStack();
                if (stack == null || stack.isEmpty()) continue;

                anyLeft = true;
                if (moved >= budget) break;

                client.interactionManager.clickSlot(handler.syncId, slot.id, 0,
                        SlotActionType.QUICK_MOVE, player);
                moved++;
            }

            if (moved > 0) {
                cooldown = delay.getInt();
                stuckTicks = 0;
                // Something was taken, so there may be more next tick.
                return;
            }
            if (!anyLeft) {
                stuckTicks = 0;
                if (closeWhenEmpty.get()) player.closeHandledScreen();
                return;
            }
            // Items remain but nothing could be moved this tick -- almost
            // always a full inventory mid-batch. Give it a few ticks in case
            // something frees up on its own, then stop clicking uselessly.
            if (++stuckTicks >= STUCK_LIMIT) {
                stuckTicks = 0;
                setEnabled(false);
            }
        } catch (Throwable pvpErr) {
            com.vortex.client.core.Errors.report("ChestStealer", pvpErr);
        }
    }

    /** Is there any room left in the main inventory? */
    private static boolean inventoryFull(ClientPlayerEntity player) {
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack == null || stack.isEmpty()) return false;
        }
        return true;
    }
}
