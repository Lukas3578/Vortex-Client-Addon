package de.vortexplus.addon;

import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;

/** Enables the add-on's normal shift/quick-move inventory interaction. */
public final class InventoryTweakAddonModule extends Module {
    public final NumberSetting delay = new NumberSetting("Hover Delay (ticks)", 0, 0, 5, 1);

    public InventoryTweakAddonModule() {
        super("Inventory Tweak", Category.MISC);
        addSetting(delay);
    }
}
