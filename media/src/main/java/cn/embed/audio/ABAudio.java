package cn.embed.audio;

import cn.embed.base.IAudio;
import cn.embed.media.manager.StreamingProfile;

public abstract class ABAudio implements IAudio {
    StreamingProfile.AudioProfile audioProfile;

    boolean isRunning = false;

    public void setConfig(StreamingProfile.AudioProfile audioProfile) {
        this.audioProfile = audioProfile;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
