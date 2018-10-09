package cn.embed.base;

import cn.embed.media.manager.StreamingProfile;

public interface IAudio {


    public void setConfig(StreamingProfile.AudioProfile audioProfile);

    public boolean start();

    public void end();
}
