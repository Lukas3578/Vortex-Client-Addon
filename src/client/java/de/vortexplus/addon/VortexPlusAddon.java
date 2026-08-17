package de.vortexplus.addon;

import com.vortex.client.gui.ModuleInfo;
import com.vortex.client.module.Module;
import com.vortex.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;

/**
 * Vortex Plus -- extra combat modules for Vortex Client.
 *
 * WHAT CHANGED FROM THE PREVIOUS VERSION, AND WHY:
 *
 * The modules used to be pushed straight into the client's internal list, and a
 * category was forced into its enum at runtime using sun.misc.Unsafe -- roughly
 * a hundred lines of the most dangerous technique Java has, where a mistake
 * gives you memory corruption rather than an error message.
 *
 * None of that is needed. The client has had a Cheats category since 2.5.0 and
 * a public way to add modules, so this now asks properly instead of reaching
 * inside. A hundred lines of risk replaced by one word.
 */
public final class VortexPlusAddon implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        register(new KillAuraAddonModule(),
                "Attacks whatever you are looking at, within range. Very high ban risk.");
        register(new CrystalAuraAddonModule(),
                "Places end crystals under opponents and breaks them, without catching you. Extreme ban risk.");
        register(new FastAnchorAddonModule(),
                "Charges and triggers respawn anchors in one go. Extreme ban risk.");
        register(new AutoToolAddonModule(),
                "Switches to the best tool for the block you are breaking.");
        register(new ChestStealerAddonModule(),
                "Empties an open chest into your inventory.");
        register(new FastUseAddonModule(),
                "Removes the client-side pause between throws. Does not affect eating. High ban risk.");
        register(new SpawnerSaferAddonModule(),
                "Spots an approaching player, packs up every spawner nearby and logs out.");
        register(new ArmorOrganizerAddonModule(),
                "Equips the best available armor from the player's own inventory, even without opening the inventory screen.");
        register(new InventoryTweakAddonModule(),
                "Moves an item once when hovering over it while Sneak/Shift is held.");
        register(new LocalFeaturesAddonModule(),
                "Local session tracker, server profiles, PvP notifications, ping alerts and hit sounds.");
    }

    /**
     * Adds one module and the line shown under its name.
     *
     * Both in one place, so a module cannot end up in the list without a
     * description -- which is exactly what happened before, and made the addon
     * modules look unfinished next to the built-in ones.
     */
    private static void register(Module module, String description) {
        try {
            ModuleManager.INSTANCE.register(module);
            ModuleInfo.register(module.getName(), description);
        } catch (Throwable pvpErr) {
            com.vortex.client.core.Errors.report("VortexPlus.register", pvpErr);
        }
    }
}
