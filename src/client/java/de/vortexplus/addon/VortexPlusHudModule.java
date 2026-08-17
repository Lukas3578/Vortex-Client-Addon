package de.vortexplus.addon;

import com.vortex.client.core.setting.BooleanSetting;
import com.vortex.client.core.setting.ColorSetting;
import com.vortex.client.core.setting.NumberSetting;
import com.vortex.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/** Add-on-owned HUD features that do not require changes to the main client. */
public final class VortexPlusHudModule extends Module {
    public final NumberSetting x = new NumberSetting("X", 6, 0, 1920, 1);
    public final NumberSetting y = new NumberSetting("Y", 6, 0, 1080, 1);
    public final ColorSetting color = new ColorSetting("Text Color", 0xFFFFFFFF);
    public final BooleanSetting sessionTracker = new BooleanSetting("Session Tracker", true);
    public final BooleanSetting networkMonitor = new BooleanSetting("Network Monitor", true);
    public final BooleanSetting eventNotifications = new BooleanSetting("PvP Notifications", true);
    public final BooleanSetting perServerProfile = new BooleanSetting("Per-Server Settings", true);
    public final BooleanSetting replayHighlights = new BooleanSetting("Replay Highlights", true);
    public final BooleanSetting dynamicCrosshair = new BooleanSetting("Dynamic Crosshair", true);
    public final BooleanSetting automaticPerformance = new BooleanSetting("Performance Hint", true);
    public final NumberSetting pvpProfile = new NumberSetting("PvP Profile", 0, 0, 5, 1);

    private static VortexPlusHudModule instance;
    private long sessionStarted;
    private int deaths;
    private boolean wasDead;
    private boolean lowHpNotified;
    private String serverKey = "singleplayer";
    private String lastEvent = "Ready";
    private int ping = -1;

    public static void observeChat(String message) {
        VortexPlusHudModule module = instance;
        if (module == null || message == null || !module.isEnabled()) return;
        String text = message.toLowerCase(Locale.ROOT);
        if (text.contains("killed") || text.contains("was slain") || text.contains("you killed")) {
            module.lastEvent = "Kill";
            module.recordHighlight("Kill");
        } else if (text.contains("win") || text.contains("victory") || text.contains("winner")) {
            module.lastEvent = "Win";
            module.recordHighlight("Win");
        } else if (text.contains("combo") || text.contains("hit")) {
            module.lastEvent = "PvP event";
            module.recordHighlight("PvP event");
        }
    }

    public VortexPlusHudModule() {
        super("Vortex + | HUD", Category.HUD);
        instance = this;
        addSetting(x);
        addSetting(y);
        addSetting(color);
        addSetting(sessionTracker);
        addSetting(networkMonitor);
        addSetting(eventNotifications);
        addSetting(perServerProfile);
        addSetting(replayHighlights);
        addSetting(dynamicCrosshair);
        addSetting(automaticPerformance);
        addSetting(pvpProfile);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("vortex_plus_addon", "vortex_plus_hud"),
                (context, tickCounter) -> render(context));
    }

    private void onTick(MinecraftClient client) {
        try {
            if (!isEnabled() || client.player == null) return;
            if (sessionStarted == 0L) sessionStarted = System.currentTimeMillis();
            updateServerKey(client);
            if (client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerList().stream()
                    .anyMatch(entry -> entry.getProfile() != null && entry.getProfile().id().equals(client.player.getUuid()))) {
                var own = client.getNetworkHandler().getPlayerList().stream()
                        .filter(entry -> entry.getProfile() != null && entry.getProfile().id().equals(client.player.getUuid()))
                        .findFirst().orElse(null);
                if (own != null) ping = own.getLatency();
            }
            boolean dead = client.player.isDead() || client.player.getHealth() <= 0.0F;
            if (dead && !wasDead) {
                deaths++;
                notifyEvent(client, "Death");
                recordHighlight("Death");
            }
            wasDead = dead;
            if (client.player.getHealth() > 0.0F && client.player.getHealth() <= 4.0F && !lowHpNotified) {
                lowHpNotified = true;
                notifyEvent(client, "Low HP");
                recordHighlight("Low HP");
            } else if (client.player.getHealth() > 6.0F) {
                lowHpNotified = false;
            }
            if (perServerProfile.get()) saveServerMarker();
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("VortexPlusHud", error);
        }
    }

    private void updateServerKey(MinecraftClient client) {
        String next = "singleplayer";
        if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null) {
            next = client.getNetworkHandler().getConnection().getAddress().toString();
        }
        if (!next.equals(serverKey)) {
            serverKey = next;
            sessionStarted = System.currentTimeMillis();
            deaths = 0;
            lastEvent = "Server joined";
        }
    }

    private void notifyEvent(MinecraftClient client, String event) {
        lastEvent = event;
        if (eventNotifications.get() && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal("§b[Vortex +] §f" + event));
        }
    }

    private void saveServerMarker() {
        try {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("vortex-plus");
            Files.createDirectories(dir);
            String safe = serverKey.replaceAll("[^a-zA-Z0-9._-]", "_");
            Files.writeString(dir.resolve("server-" + safe + ".profile"),
                    "lastEvent=" + lastEvent + "\ndeaths=" + deaths + "\npvpProfile=" + pvpProfile.getInt() + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private void render(DrawContext context) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!isEnabled() || client.player == null) return;
            int px = x.getInt();
            int py = y.getInt();
            int c = color.get();
            int line = 0;
            context.drawTextWithShadow(client.textRenderer, Text.literal("Vortex +"), px, py + line++ * 10, c);
            if (sessionTracker.get()) {
                long seconds = Math.max(0, (System.currentTimeMillis() - sessionStarted) / 1000L);
                context.drawTextWithShadow(client.textRenderer,
                        Text.literal(String.format(Locale.ROOT, "Session %02d:%02d  Deaths %d", seconds / 60, seconds % 60, deaths)),
                        px, py + line++ * 10, c);
            }
            if (networkMonitor.get()) {
                context.drawTextWithShadow(client.textRenderer,
                        Text.literal("Ping " + (ping < 0 ? "?" : ping + " ms") + "  " + serverKey),
                        px, py + line++ * 10, c);
            }
            if (eventNotifications.get()) {
                context.drawTextWithShadow(client.textRenderer, Text.literal("Event: " + lastEvent),
                        px, py + line++ * 10, c);
            }
            if (automaticPerformance.get()) {
                String mode = client.world != null && client.world.getPlayers().size() > 20 ? "Large fight" : "Normal";
                context.drawTextWithShadow(client.textRenderer, Text.literal("Performance: " + mode),
                        px, py + line++ * 10, c);
            }
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Profile: " + profileName(pvpProfile.getInt())), px, py + line * 10, c);
            if (dynamicCrosshair.get()) renderCrosshair(context, client);
        } catch (Throwable error) {
            com.vortex.client.core.Errors.report("VortexPlusHud.render", error);
        }
    }

    private String profileName(int index) {
        return switch (index) {
            case 1 -> "Sword";
            case 2 -> "Crystal";
            case 3 -> "Mace";
            case 4 -> "Spear";
            case 5 -> "UHC";
            default -> "Default";
        };
    }

    private void renderCrosshair(DrawContext context, MinecraftClient client) {
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int gap = client.player.isSprinting() ? 6 : 4;
        if (client.player.getAttackCooldownProgress(0.0F) < 1.0F) gap += 3;
            int hudColor = this.color.get();
            context.drawHorizontalLine(cx - gap - 3, cx - gap, cy, hudColor);
            context.drawHorizontalLine(cx + gap, cx + gap + 3, cy, hudColor);
            context.drawVerticalLine(cx, cy - gap - 3, cy - gap, hudColor);
            context.drawVerticalLine(cx, cy + gap, cy + gap + 3, hudColor);
    }

    private void recordHighlight(String event) {
        if (!replayHighlights.get()) return;
        try {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("vortex-plus");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("replay-highlights.log"),
                    System.currentTimeMillis() + "\t" + serverKey + "\t" + event + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
