package de.vortexplus.addon;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/** Streams rendered RGBA frames to a local ffmpeg process and creates MP4 clips. */
public final class FfmpegReplayRecorder {
    private static final DateTimeFormatter NAME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static Process process;
    private static OutputStream input;
    private static long stopAt;
    private static int width;
    private static int height;
    private static ByteBuffer pixels;
    private static long lastFrameAt;

    private FfmpegReplayRecorder() {}

    /** Starts a manual recording that ends only when the module is disabled. */
    public static synchronized boolean startManual() {
        return markHighlight("manual", Integer.MAX_VALUE);
    }

    public static synchronized boolean markHighlight(String label, int seconds) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getWindow() == null) return false;
            if (isRecording()) {
                stopAt = Math.max(stopAt, System.currentTimeMillis() + seconds * 1000L);
                return true;
            }
            width = client.getWindow().getFramebufferWidth();
            height = client.getWindow().getFramebufferHeight();
            if (width <= 0 || height <= 0) return false;
            Path dir = client.runDirectory.toPath().resolve("vortex-plus/replays");
            Files.createDirectories(dir);
            String safe = label == null ? "highlight" : label.replaceAll("[^a-zA-Z0-9_-]", "_");
            Path output = dir.resolve(safe + "-" + LocalDateTime.now().format(NAME) + ".mp4");
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-f", "rawvideo", "-pixel_format", "rgba",
                    "-video_size", width + "x" + height, "-framerate", "30", "-i", "-", "-vf", "vflip",
                    "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p", output.toString());
            pb.redirectError(ProcessBuilder.Redirect.appendTo(dir.resolve("ffmpeg.log").toFile()));
            process = pb.start();
            input = process.getOutputStream();
            stopAt = System.currentTimeMillis() + seconds * 1000L;
            pixels = BufferUtils.createByteBuffer(width * height * 4);
            lastFrameAt = 0L;
            return true;
        } catch (IOException | RuntimeException error) {
            process = null;
            input = null;
            return false;
        }
    }

    public static synchronized boolean isRecording() {
        return process != null && process.isAlive();
    }

    public static synchronized void captureFrame() {
        if (!isRecording() || input == null) return;
        try {
            long now = System.currentTimeMillis();
            if (now >= stopAt) {
                stop();
                return;
            }
            // Keep capture at the ffmpeg input rate instead of blocking the render thread.
            if (lastFrameAt != 0L && now - lastFrameAt < 33L) return;
            lastFrameAt = now;
            pixels.clear();
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            byte[] frame = new byte[pixels.remaining()];
            pixels.get(frame);
            input.write(frame);
        } catch (IOException | RuntimeException error) {
            stop();
        }
    }

    public static synchronized void stop() {
        if (input != null) {
            try { input.close(); } catch (IOException ignored) {}
        }
        input = null;
        if (process != null) {
            try {
                if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        process = null;
        pixels = null;
        lastFrameAt = 0L;
    }
}
