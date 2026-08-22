package de.vortexplus.addon;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the launcher-owned cosmetic selection without linking the addon against optional
 * internal classes of the Vortex cosmetics core. This keeps local and CI builds stable.
 */
@Environment(EnvType.CLIENT)
public final class LocalCosmeticsSelection {
    private static final Set<String> HATS = Set.of(
            "vortex-cap", "neon-halo", "void-crown", "cyber-headphones", "slime-antenna"
    );
    private static final Pattern HAT = Pattern.compile("\\\"hat\\\"\\s*:\\s*\\\"([a-z0-9-]+)\\\"", Pattern.CASE_INSENSITIVE);

    private LocalCosmeticsSelection() {}

    public static boolean isLocalPlayer(PlayerEntityRenderState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && state.id == client.player.getId();
    }

    public static String activeHat() {
        try {
            Path profile = FabricLoader.getInstance().getConfigDir()
                    .resolve("vortexclient")
                    .resolve("launcher-cosmetics.json");
            if (!Files.isRegularFile(profile)) return "vortex-cap";
            Matcher matcher = HAT.matcher(Files.readString(profile, StandardCharsets.UTF_8));
            if (!matcher.find()) return "vortex-cap";
            String hat = matcher.group(1).toLowerCase(java.util.Locale.ROOT);
            return HATS.contains(hat) ? hat : "vortex-cap";
        } catch (Throwable ignored) {
            return "vortex-cap";
        }
    }
}
