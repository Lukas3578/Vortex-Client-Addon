package de.vortexplus.addon;

import com.vortex.client.module.Module;
import com.vortex.client.module.ModuleManager;
import com.vortex.client.module.modules.AutoTotemModule;

/** Exposes Vortex's Auto Totem feature as a dedicated addon module. */
public final class SmartAutoTotemAddonModule extends Module {
    private final AutoTotemModule delegate;

    public SmartAutoTotemAddonModule() {
        super("Smart Auto Totem", Category.CHEATS);
        delegate = ModuleManager.INSTANCE.get(AutoTotemModule.class);
        if (delegate != null) {
            addSetting(delegate.delay);
            addSetting(delegate.jitter);
            addSetting(delegate.onlyWithWeapon);
            addSetting(delegate.healthBelow);
            addSetting(delegate.warnEmpty);
        }
    }

    @Override
    protected void onEnable() {
        setDelegateEnabled(true);
    }

    @Override
    protected void onDisable() {
        setDelegateEnabled(false);
    }

    private void setDelegateEnabled(boolean enabled) {
        if (delegate != null && delegate.isEnabled() != enabled) {
            delegate.setEnabled(enabled);
        }
    }
}
