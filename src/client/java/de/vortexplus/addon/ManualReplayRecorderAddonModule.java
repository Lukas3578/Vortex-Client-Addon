package de.vortexplus.addon;

import com.vortex.client.module.Module;

/** Manual MP4 recorder controlled by this module's configurable toggle key. */
public final class ManualReplayRecorderAddonModule extends Module {
    public ManualReplayRecorderAddonModule() {
        super("Manual Replay Recorder", Category.MISC);
    }

    @Override
    protected void onEnable() {
        if (!FfmpegReplayRecorder.startManual()) {
            setEnabled(false);
        }
    }

    @Override
    protected void onDisable() {
        FfmpegReplayRecorder.stop();
    }
}
