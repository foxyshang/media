package cn.embed.codec;

import android.view.Surface;

public interface IVideoEncoder {
    public Surface getInputSurface();

    public void release();

    public void drainEncoder(boolean endOfStream);
}
