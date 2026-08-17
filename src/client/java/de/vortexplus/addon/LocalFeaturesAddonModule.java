package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.core.setting.ModeSetting;
import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import com.vortex.client.module.ModuleManager;
import com.vortex.client.module.modules.PotatoModeModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/** Local, non-HUD session and PvP utilities for the add-on. */
public final class LocalFeaturesAddonModule extends Module {
    public final BooleanSetting sessionTracker = new BooleanSetting("Session Tracker", true);
    public final BooleanSetting networkMonitor = new BooleanSetting("Network Monitor", true);
    public final BooleanSetting notifications = new BooleanSetting("PvP Notifications", true);
    public final BooleanSetting hitSounds = new BooleanSetting("Hit Sounds", true);
    public final BooleanSetting perServerSettings = new BooleanSetting("Per-Server Settings", true);
    public final BooleanSetting replayHighlights = new BooleanSetting("Replay Highlights", true);
    public final BooleanSetting dynamicCrosshair = new BooleanSetting("Dynamic Crosshair", true);
    public final BooleanSetting automaticPerformance = new BooleanSetting("Automatic Performance Profiles", true);
    public final ModeSetting pvpProfile = new ModeSetting("PvP Profile", 0,
            "Default", "Crystal", "Sword", "Mace", "Spear", "UHC");
    public final NumberSetting soundVolume = new NumberSetting("Sound Volume", 1.0, 0.0, 2.0, 0.05);
    public final NumberSetting soundPitch = new NumberSetting("Sound Pitch", 1.0, 0.5, 2.0, 0.05);
    public final NumberSetting pingAlert = new NumberSetting("Ping Alert (ms)", 120, 0, 1000, 10);
    public final NumberSetting clipSeconds = new NumberSetting("Highlight Seconds", 8, 2, 30, 1);

    private long sessionStarted;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int lastPing = -1;
    private String server = "singleplayer";
    private long lastNetworkNotice;
    private boolean autoPerformanceOwned;

    public LocalFeaturesAddonModule() {
        super("Local PvP Features", Category.MISC);
        addSetting(sessionTracker);
        addSetting(networkMonitor);
        addSetting(notifications);
        addSetting(hitSounds);
        addSetting(perServerSettings);
        addSetting(replayHighlights);
        addSetting(dynamicCrosshair);
        addSetting(automaticPerformance);
        addSetting(pvpProfile);
        addSetting(soundVolume);
        addSetting(soundPitch);
        addSetting(pingAlert);
        addSetting(clipSeconds);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public static boolean dynamicCrosshairEnabled() {
        LocalFeaturesAddonModule module = ModuleManager.INSTANCE.get(LocalFeaturesAddonModule.class);
        return module != null && module.isEnabled() && module.dynamicCrosshair.get();
    }

    public static void drawDynamicCrosshair(net.minecraft.client.gui.DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int gap = client.player.isSprinting() ? 6 : 4;
        if (client.player.getAttackCooldownProgress(0.0F) < 1.0F) gap += 3;
        if (client.player.isUsingItem()) gap += 2;
        int color = 0xFFFFFFFF;
        context.drawHorizontalLine(cx - gap - 3, cx - gap, cy, color);
        context.drawHorizontalLine(cx + gap, cx + gap + 3, cy, color);
        context.drawVerticalLine(cx, cy - gap - 3, cy - gap, color);
        context.drawVerticalLine(cx, cy + gap, cy + gap + 3, color);
    }

    private void tick(MinecraftClient client) {
        try {
            if (!isEnabled() || client.player == null) return;
            if (sessionStarted == 0L) sessionStarted = System.currentTimeMillis();
            String next = currentServer(client);
            if (!next.equals(server)) {
                server = next;
                sessionStarted = System.currentTimeMillis();
                wins = losses = kills = deaths = 0;
                notify(client, "Server gewechselt: " + server);
            }
            if (client.getNetworkHandler() != null) {
                var own = client.getNetworkHandler().getPlayerList().stream()
                        .filter(e -> e.getProfile() != null && e.getProfile().id().equals(client.player.getUuid()))
                        .findFirst().orElse(null);
                if (own != null) {
                    int ping = own.getLatency();
                    if (networkMonitor.get() && lastPing >= 0
                            && Math.abs(ping - lastPing) >= 50
                            && System.currentTimeMillis() - lastNetworkNotice > 3000L) {
                        notify(client, "Ping: " + lastPing + " -> " + ping + " ms");
                        lastNetworkNotice = System.currentTimeMillis();
                    }
                    lastPing = ping;
                }
            }
            if (automaticPerformance.get()) updatePerformanceProfile(client);
            if (perServerSettings.get()) saveProfile();
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("LocalPvPFeatures", error);
        }
    }

    private void updatePerformanceProfile(MinecraftClient client) {
        PotatoModeModule potato = ModuleManager.INSTANCE.get(PotatoModeModule.class);
        if (potato == null || client.world == null) return;
        boolean largeFight = client.world.getPlayers().size() > 20;
        boolean lobby = client.player.age < 100 && client.world.getPlayers().size() <= 2;
        boolean shouldUse = largeFight || lobby;
        if (shouldUse && !potato.isEnabled()) {
            potato.setEnabled(true);
            autoPerformanceOwned = true;
        } else if (!shouldUse && autoPerformanceOwned && potato.isEnabled()) {
            potato.setEnabled(false);
            autoPerformanceOwned = false;
        }
    }

    public void observeChat(MinecraftClient client, String raw) {
        if (!isEnabled() || raw == null) return;
        String text = raw.toLowerCase(Locale.ROOT);
        if (text.contains("you killed") || text.contains("killed by you") || text.contains("was slain by")) {
            kills++;
            notify(client, "Kill");
            markHighlight("kill");
            playHitSound(client);
        } else if (text.contains("victory") || text.contains("winner") || text.contains("you win")) {
            wins++;
            notify(client, "Win");
            markHighlight("win");
        } else if (text.contains("defeat") || text.contains("you lost") || text.contains("loss")) {
            losses++;
            notify(client, "Loss");
        } else if (text.contains("died") || text.contains("death") || text.contains("was slain")) {
            deaths++;
            notify(client, "Death");
        } else if (text.contains("combo") || text.contains("clutch") || text.contains("opponent")) {
            notify(client, "PvP event");
            markHighlight("pvp-event");
        }
    }

    private void markHighlight(String label) {
        if (replayHighlights.get()) {
            FfmpegReplayRecorder.markHighlight(label, clipSeconds.getInt());
        }
    }

    private void playHitSound(MinecraftClient client) {
        if (!hitSounds.get() || client.player == null) return;
        client.player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                soundVolume.getFloat(), soundPitch.getFloat());
    }

    private void notify(MinecraftClient client, String message) {
        if (notifications.get() && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal("§b[Local PvP] §f" + message));
        }
    }

    private String currentServer(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.getNetworkHandler().getConnection() == null) return "singleplayer";
        return client.getNetworkHandler().getConnection().getAddress().toString();
    }

    private void saveProfile() {
        try {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("vortex-plus/server-profiles");
            Files.createDirectories(dir);
            String safe = server.replaceAll("[^a-zA-Z0-9._-]", "_");
            long seconds = Math.max(0L, (System.currentTimeMillis() - sessionStarted) / 1000L);
            String data = "server=" + server + "\nprofile=" + pvpProfile.getIndex()
                    + "\nwins=" + wins + "\nlosses=" + losses + "\nkills=" + kills
                    + "\ndeaths=" + deaths + "\nplaytimeSeconds=" + seconds + "\n";
            Files.writeString(dir.resolve(safe + ".properties"), data,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }
}
