package de.vortexplus.addon.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the client's own use cooldown.
 *
 * After every use the client sets a four tick pause before it will send the
 * next one. That pause exists only here, on your machine -- the server never
 * hears about it and does not enforce it.
 *
 * WHY THIS REPLACES THE PREVIOUS APPROACH: Fast Use used to send extra "use
 * item" messages of its own. The server accepted some and dropped the rest, and
 * what came back were the ghost items -- things that appeared to leave your
 * hand and then returned. Setting the pause to zero instead means the game
 * itself performs each use, through its normal path, in the normal order.
 * Nothing is invented, so nothing has to be taken back.
 *
 * The field is private and its real name is field_1752. Looking it up by the
 * name "itemUseCooldown" -- which the previous version did -- finds nothing in
 * a finished game, which is why it never worked.
 */
@Mixin(MinecraftClient.class)
public interface ItemUseCooldownAccessor {

    @Accessor("field_1752")
    void vortexplus$setItemUseCooldown(int value);

    @Accessor("field_1752")
    int vortexplus$getItemUseCooldown();
}
