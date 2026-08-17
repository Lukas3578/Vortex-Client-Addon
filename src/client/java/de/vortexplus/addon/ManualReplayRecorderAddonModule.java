package de.vortexplus.addon;

import com.vortex.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Manual MP4 recorder controlled by this module's configurable toggle key. */
public final class ManualReplayRecorderAddonModule extends Module {
    public ManualReplayRecorderAddonModule() {
        super("Manual Replay Recorder", Category.MISC);
    }

    @Override
    protected void onEnable() {
        if (!FfmpegReplayRecorder.startManual()) {
            setEnabled(false);
            notifyPlayer("Replay konnte nicht gestartet werden. Ist FFmpeg installiert und im PATH?");
        } else {
            notifyPlayer("MP4-Aufnahme gestartet.");
        }
    }

    @Override
    protected void onDisable() {
        boolean wasRecording = FfmpegReplayRecorder.isRecording();
        FfmpegReplayRecorder.stop();
        if (wasRecording) notifyPlayer("MP4-Aufnahme gespeichert.");
    }

    private static void notifyPlayer(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.player.sendMessage(Text.literal("[Replay] " + message), false);
    }
}
