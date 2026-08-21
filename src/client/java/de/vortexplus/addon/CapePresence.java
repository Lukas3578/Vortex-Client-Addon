package de.vortexplus.addon;

import com.vortex.client.cosmetics.ActiveCape;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Synchronizes selected Vortex capes and resolves the fixed renderer's self-contained textures. */
@Environment(EnvType.CLIENT)
public final class CapePresence {
    private static final String API = "https://vortex-client.onrender.com/api/capes/presence";
    private static final Pattern PRESENCE = Pattern.compile("\\\"uuid\\\"\\s*:\\s*\\\"([0-9a-f-]{36})\\\"[\\s\\S]*?\\\"cape\\\"\\s*:\\s*(?:\\\"([a-z0-9_-]+)\\\"|null)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCAL_CAPE = Pattern.compile("\\\"cape\\\"\\s*:\\s*\\\"([a-z0-9_-]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "vortex-cape-presence");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<UUID, String> CAPES = new ConcurrentHashMap<>();
    private static volatile boolean started;

    private CapePresence() {}

    public static void init() {
        if (started) return;
        started = true;
        EXECUTOR.scheduleWithFixedDelay(CapePresence::sync, 3, 8, TimeUnit.SECONDS);
    }

    /**
     * Resolves a resource that is embedded in this addon, not in an external or downloaded cache.
     * This keeps rendering reliable even when the core cosmetic texture registration is unavailable.
     */
    public static Identifier textureFor(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player.getId() != entityId) continue;
            if (player == client.player) return localTexture();
            return embeddedTexture(CAPES.get(player.getUuid()));
        }
        return null;
    }

    public static Identifier localTexture() {
        Identifier configured = embeddedTexture(readConfiguredCape());
        if (configured != null) return configured;
        // Compatibility fallback for profiles written by older Vortex core versions.
        try {
            Identifier active = ActiveCape.textureId();
            String path = active == null ? "" : active.getPath();
            if (path.contains("vortex-crest")) return embeddedTexture("vortex-crest");
            if (path.contains("nebula-mark")) return embeddedTexture("nebula-mark");
            if (path.contains("void-rune")) return embeddedTexture("void-rune");
        } catch (Throwable ignored) {}
        return null;
    }

    private static Identifier embeddedTexture(String cape) {
        if (cape == null) return null;
        return switch (cape) {
            case "nebula-mark", "void-rune", "vortex-crest" -> Identifier.of("vortexplus", "textures/capes/" + cape + ".png");
            default -> null;
        };
    }

    private static String readConfiguredCape() {
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("vortex-client").resolve("cosmetics.json");
            if (!Files.isRegularFile(file)) return null;
            Matcher matcher = LOCAL_CAPE.matcher(Files.readString(file, StandardCharsets.UTF_8));
            if (!matcher.find()) return null;
            String cape = matcher.group(1).toLowerCase(Locale.ROOT);
            return embeddedTexture(cape) == null ? null : cape;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void sync() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.player == null || client.world == null) return;
                String localUuid = client.player.getUuid().toString();
                String localName = client.player.getName().getString();
                String localCape = readLocalCape();
                CAPES.put(client.player.getUuid(), localCape);
                postPresence(localUuid, localName, localCape);
                StringBuilder uuids = new StringBuilder();
                for (PlayerEntity player : client.world.getPlayers()) {
                    if (uuids.length() > 0) uuids.append(',');
                    uuids.append(player.getUuid());
                }
                fetchPresence(uuids.toString());
            });
        } catch (Throwable ignored) {
            // Cosmetics must never affect gameplay or prevent the client from starting.
        }
    }

    private static String readLocalCape() {
        String configured = readConfiguredCape();
        if (configured != null) return configured;
        try {
            Identifier active = ActiveCape.textureId();
            if (active == null) return null;
            String path = active.getPath();
            if (path.contains("nebula-mark")) return "nebula-mark";
            if (path.contains("void-rune")) return "void-rune";
            if (path.contains("vortex-crest")) return "vortex-crest";
        } catch (Throwable ignored) {}
        return null;
    }

    private static void postPresence(String uuid, String name, String cape) {
        String json = "{\"uuid\":\"" + uuid + "\",\"name\":\"" + escape(name) + "\",\"cape\":" + (cape == null ? "null" : "\"" + cape + "\"") + "}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(API))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("User-Agent", "VortexClient/2.29.10")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    private static void fetchPresence(String uuids) {
        if (uuids.isBlank()) return;
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + "?uuids=" + uuids))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "VortexClient/2.29.10")
                .GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() / 100 != 2) return;
            Matcher matcher = PRESENCE.matcher(response.body());
            while (matcher.find()) {
                try {
                    UUID uuid = UUID.fromString(matcher.group(1));
                    CAPES.put(uuid, matcher.group(2));
                } catch (IllegalArgumentException ignored) {}
            }
        });
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
