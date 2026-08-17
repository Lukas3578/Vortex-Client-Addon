package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Fast Anchor v3: es wird IMMER zuerst wirklich auf den Glowstone gewechselt
 * (serverseitig sichtbar per UpdateSelectedSlotC2SPacket) und ein Glowstone in
 * den anvisierten Anchor gesetzt - ist der Anchor voll, explodiert er dadurch
 * (Overworld/Ende). Danach wird wirklich auf den Anchor gewechselt.
 * Pro Tastendruck wird GENAU EIN Glowstone benutzt, egal wie lange die Taste
 * gehalten wird. Die Charge-Once-Einstellung entfaellt (Verhalten ist fix).
 */
public final class FastAnchorAddonModule extends Module {
    public final NumberSetting delay = new NumberSetting("Delay", 1, 0, 6, 1);
    public final NumberSetting switchDelay = new NumberSetting("Switch Delay", 3, 0, 10, 1);
    public final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);

    /**
     * Refuse anchors that would catch you as well.
     *
     * There was no such check at all. An anchor going off next to you does the
     * same damage as one going off next to anyone else, and the module was
     * perfectly willing to set one at your feet.
     */
    public final BooleanSetting selfProtect = new BooleanSetting("Do Not Hit Myself", true);

    /** How far the anchor has to be before it may be triggered. */
    public final NumberSetting minSelfDistance =
            new NumberSetting("Min Distance to Me", 4.0, 1.0, 10.0, 0.5);

    private static final int IDLE = 0;
    private static final int GLOWSTONE_SELECTED = 1;
    private static final int ANCHOR_SELECTED = 2;
    private static final int SWITCH_BACK = 3;

    private int state = IDLE;
    private int timer;
    private int cooldown;
    private int previousSlot = -1;
    private boolean placedThisPress;
    private BlockHitResult pendingHit;

    public FastAnchorAddonModule() {
        super("Vortex + | Fast Anchor", Category.HUD);
        addSetting(delay);
        addSetting(switchDelay);
        addSetting(switchBack);
        addSetting(selfProtect);
        addSetting(minSelfDistance);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void onDisable() {
        reset(MinecraftClient.getInstance());
    }

    private void onTick(MinecraftClient client) {
        if (!isEnabled() || client.player == null || client.world == null
                || client.interactionManager == null) return;
        if (!client.options.useKey.isPressed()) {
            reset(client);
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        switch (state) {
            case IDLE -> startSwap(client);
            case GLOWSTONE_SELECTED -> finishGlowstoneUse(client);
            case ANCHOR_SELECTED -> finishAnchorUse(client);
            case SWITCH_BACK -> finishSwitchBack(client);
        }
    }

    /** Sequenz starten: IMMER zuerst auf den Glowstone wechseln (auch wenn der Anchor voll ist). */
    private void startSwap(MinecraftClient client) {
        if (placedThisPress) return; // nur EIN Glowstone pro Tastendruck
        BlockHitResult hit = getAnchorHit(client);
        if (hit == null) return;
        int slot = findGlowstone(client.player);
        if (slot < 0) {
            // kein Glowstone im Hotbar -> trotzdem sichtbar auf den Anchor wechseln
            int anchorSlot = findAnchor(client.player);
            if (anchorSlot < 0) return;
            selectSlot(client.player, anchorSlot);
            pendingHit = hit;
            state = ANCHOR_SELECTED;
            timer = switchDelay.getInt();
            return;
        }
        selectSlot(client.player, slot);
        pendingHit = hit;
        state = GLOWSTONE_SELECTED;
        timer = switchDelay.getInt();
    }

    /** Nach dem Halt auf dem Glowstone: einsetzen (laedt auf ODER explodiert), dann zum Anchor. */
    private void finishGlowstoneUse(MinecraftClient client) {
        if (timer > 0) {
            timer--;
            return;
        }
        if (client.player == null || pendingHit == null) {
            reset(client);
            return;
        }
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, pendingHit);
        client.player.swingHand(Hand.MAIN_HAND);
        placedThisPress = true;

        int slot = findAnchor(client.player);
        if (slot < 0) {
            // kein Anchor im Hotbar -> Sequenz beenden, Glowstone bleibt aktiv
            finishSequence(client);
            return;
        }
        selectSlot(client.player, slot);
        state = ANCHOR_SELECTED;
        timer = switchDelay.getInt();
    }

    /** Nach dem Halt auf dem Anchor: Interaktion ausfuehren und Sequenz abschliessen. */
    private void finishAnchorUse(MinecraftClient client) {
        if (timer > 0) {
            timer--;
            return;
        }
        if (client.player == null || pendingHit == null) {
            reset(client);
            return;
        }
        // Last check before it goes off: is it far enough from us?
        if (selfProtect.get() && tooCloseToMe(client, pendingHit.getBlockPos())) {
            pendingHit = null;
            finishSequence(client);
            return;
        }

        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, pendingHit);
        client.player.swingHand(Hand.MAIN_HAND);
        pendingHit = null;
        finishSequence(client);
    }

    /** Abschluss: je nach Einstellung zurueckwechseln oder auf dem Anchor bleiben. */
    private void finishSequence(MinecraftClient client) {
        if (switchBack.get()) {
            state = SWITCH_BACK;
            timer = 1;
        } else {
            previousSlot = -1; // auf dem Anchor bleiben
            cooldown = delay.getInt();
            state = IDLE;
        }
    }

    /** Zurueck auf den vorherigen Hotbar-Slot wechseln (serverseitig gesynct). */
    private void finishSwitchBack(MinecraftClient client) {
        if (timer > 0) {
            timer--;
            return;
        }
        restoreSlot(client);
        cooldown = delay.getInt();
        state = IDLE;
    }

    /** Zustand zuruecksetzen und ggf. Slot zurueckschalten. */
    private void reset(MinecraftClient client) {
        restoreSlot(client);
        state = IDLE;
        timer = 0;
        cooldown = 0;
        placedThisPress = false;
        pendingHit = null;
    }

    private static BlockHitResult getAnchorHit(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || client.world == null
                || !client.world.getBlockState(hit.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
            return null;
        }
        return hit;
    }

    private static int findGlowstone(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            var stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.isOf(Items.GLOWSTONE)) return slot;
        }
        return -1;
    }

    private static int findAnchor(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            var stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.isOf(Items.RESPAWN_ANCHOR)) return slot;
        }
        return -1;
    }

    /** Slot lokal wechseln UND per UpdateSelectedSlotC2SPacket an den Server melden. */
    /** Is this block close enough that the blast would reach us? */
    private boolean tooCloseToMe(MinecraftClient client, BlockPos pos) {
        if (client.player == null) return false;
        double dx = pos.getX() + 0.5 - client.player.getX();
        double dy = pos.getY() + 0.5 - client.player.getY();
        double dz = pos.getZ() + 0.5 - client.player.getZ();
        double min = minSelfDistance.get();
        return (dx * dx + dy * dy + dz * dz) < (min * min);
    }

    private void selectSlot(ClientPlayerEntity player, int slot) {
        // This module already did it right; now it shares the one version with
        // the others, so the correct handling cannot drift apart again.
        int before = Slots.select(player, slot);
        if (before >= 0 && previousSlot < 0) previousSlot = before;
    }

    /** Vorherigen Slot wiederherstellen und serverseitig synchronisieren. */
    private void restoreSlot(MinecraftClient client) {
        if (previousSlot < 0 || client.player == null) return;
        Slots.restore(client.player, previousSlot);
        previousSlot = -1;
    }
}
