package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.module.Module;
import com.vortex.client.module.ModuleManager;
import com.vortex.client.module.modules.AutoReconnectModule;
import com.vortex.client.module.modules.AutoTotemModule;
import com.vortex.client.module.modules.CrosshairModule;
import com.vortex.client.module.modules.EspModule;
import com.vortex.client.module.modules.FullbrightModule;
import com.vortex.client.module.modules.NoFallModule;
import com.vortex.client.module.modules.NoFogModule;
import com.vortex.client.module.modules.SessionStatsModule;
import com.vortex.client.module.modules.ToggleSprintModule;
import com.vortex.client.module.modules.ZoomModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Meteor-inspired presets wired to Vortex modules and this addon's Auto Tool. */
public final class MeteorUtilitiesAddonModule extends Module {
    public final BooleanSetting autoReconnect = new BooleanSetting("Auto Reconnect", false);
    public final BooleanSetting autoTool = new BooleanSetting("Smart Auto Tool", false);
    public final BooleanSetting autoTotem = new BooleanSetting("Auto Totem", false);
    public final BooleanSetting dynamicCrosshair = new BooleanSetting("Dynamic Crosshair", false);
    public final BooleanSetting entityEsp = new BooleanSetting("Entity ESP", false);
    public final BooleanSetting fullbright = new BooleanSetting("Fullbright", false);
    public final BooleanSetting noFall = new BooleanSetting("No Fall", false);
    public final BooleanSetting noFog = new BooleanSetting("No Fog", false);
    public final BooleanSetting sessionStats = new BooleanSetting("Session Stats", false);
    public final BooleanSetting toggleSprint = new BooleanSetting("Toggle Sprint", false);
    public final BooleanSetting zoom = new BooleanSetting("Zoom", false);

    private boolean lastAutoReconnect;
    private boolean lastAutoTool;
    private boolean lastAutoTotem;
    private boolean lastDynamicCrosshair;
    private boolean lastEntityEsp;
    private boolean lastFullbright;
    private boolean lastNoFall;
    private boolean lastNoFog;
    private boolean lastSessionStats;
    private boolean lastToggleSprint;
    private boolean lastZoom;

    public MeteorUtilitiesAddonModule() {
        super("Meteor Utilities Pack", Category.MISC);
        addSetting(autoReconnect); addSetting(autoTool); addSetting(autoTotem);
        addSetting(dynamicCrosshair); addSetting(entityEsp); addSetting(fullbright);
        addSetting(noFall); addSetting(noFog); addSetting(sessionStats);
        addSetting(toggleSprint); addSetting(zoom);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isEnabled()) sync();
        });
    }

    private void sync() {
        lastAutoReconnect = apply(autoReconnect, lastAutoReconnect, AutoReconnectModule.class);
        lastAutoTool = apply(autoTool, lastAutoTool, AutoToolAddonModule.class);
        lastAutoTotem = apply(autoTotem, lastAutoTotem, AutoTotemModule.class);
        lastDynamicCrosshair = apply(dynamicCrosshair, lastDynamicCrosshair, CrosshairModule.class);
        lastEntityEsp = apply(entityEsp, lastEntityEsp, EspModule.class);
        lastFullbright = apply(fullbright, lastFullbright, FullbrightModule.class);
        lastNoFall = apply(noFall, lastNoFall, NoFallModule.class);
        lastNoFog = apply(noFog, lastNoFog, NoFogModule.class);
        lastSessionStats = apply(sessionStats, lastSessionStats, SessionStatsModule.class);
        lastToggleSprint = apply(toggleSprint, lastToggleSprint, ToggleSprintModule.class);
        lastZoom = apply(zoom, lastZoom, ZoomModule.class);
    }

    private <T extends Module> boolean apply(BooleanSetting setting, boolean previous, Class<T> type) {
        boolean wanted = setting.get();
        if (wanted != previous) {
            T module = ModuleManager.INSTANCE.get(type);
            if (module != null && module.isEnabled() != wanted) module.setEnabled(wanted);
        }
        return wanted;
    }
}
