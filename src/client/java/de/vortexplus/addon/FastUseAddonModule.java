package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import de.vortexplus.addon.mixin.ItemUseCooldownAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/**
 * Throws faster by removing the client's own pause.
 *
 * HOW THIS WORKS NOW, AND WHY THE LAST VERSION MISBEHAVED:
 *
 * After each use the client waits four ticks before sending another. That pause
 * lives on your machine alone; the server neither knows nor enforces it.
 *
 * The previous version tried to get around it by sending extra "use item"
 * messages itself. The server took some and discarded the rest, and what you
 * saw were ghost items -- a handful thrown, then most of them back in your
 * hand. Nothing was really thrown; the client had simply told itself a story
 * the server never agreed to.
 *
 * This version sends nothing of its own. It sets the pause to zero and lets the
 * game do the using, exactly as it would if you were clicking. Every throw goes
 * through the normal path in the normal order, so there is nothing to take back.
 *
 * ON FOOD: eating is not repeated tapping, it is one long hold. There is no
 * pause to remove, and hurrying it is not something the client can decide --
 * the server times the eating and hands you the effect at the end. That is why
 * golden apples never sped up, and why this no longer pretends otherwise.
 */
public final class FastUseAddonModule extends Module {

    /**
     * How much of the pause to remove, in ticks.
     *
     * Vanilla waits four. Zero means no pause at all, which is one use per tick
     * -- twenty a second against the usual five.
     */
    public final NumberSetting cooldown = new NumberSetting("Cooldown (ticks)", 0, 0, 4, 1);

    /** Only while the use key is held. */
    public final BooleanSetting onlyWhileHeld = new BooleanSetting("Only While Held", true);

    /**
     * Only for things that are thrown.
     *
     * Bottles, pearls, snowballs, eggs and potions. Everything else keeps its
     * normal timing -- for food and bows there is nothing here to speed up
     * anyway, and touching their timing only makes the rest look wrong too.
     */
    public final BooleanSetting throwablesOnly = new BooleanSetting("Throwables Only", true);

    public FastUseAddonModule() {
        super("Vortex + | Fast Use", Category.HUD);
        addSetting(cooldown);
        addSetting(onlyWhileHeld);
        addSetting(throwablesOnly);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        try {
            if (!isEnabled()) return;
            if (client.player == null || client.interactionManager == null) return;
            if (client.currentScreen != null) return;

            if (onlyWhileHeld.get()
                    && (client.options.useKey == null || !client.options.useKey.isPressed())) {
                return;
            }

            ItemStack held = client.player.getMainHandStack();
            if (held == null || held.isEmpty()) return;
            if (throwablesOnly.get() && !isThrowable(held)) return;

            ItemUseCooldownAccessor acc = (ItemUseCooldownAccessor) client;
            int want = cooldown.getInt();
            if (acc.vortexplus$getItemUseCooldown() > want) {
                acc.vortexplus$setItemUseCooldown(want);
            }
        } catch (Throwable pvpErr) {
            com.vortex.client.core.Errors.report("FastUse", pvpErr);
        }
    }

    /**
     * Is this something you throw?
     *
     * Matched by item id rather than by class: the item classes were merged in
     * this version, so no single type covers them any more.
     */
    private static boolean isThrowable(ItemStack stack) {
        try {
            var id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
            if (id == null) return false;
            String path = id.getPath();
            return path.equals("experience_bottle")
                    || path.equals("ender_pearl")
                    || path.equals("snowball")
                    || path.equals("egg")
                    || path.equals("splash_potion")
                    || path.equals("lingering_potion");
        } catch (Throwable pvpErr) {
            return false;
        }
    }
}
