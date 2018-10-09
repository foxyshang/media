package cn.embed.renderer;

import java.io.Serializable;

/**
 * 视频纹理变换参数
 */

public class TextureTrans implements Serializable {
    private float scaleX;
    private float scaleY;
    private float transX;
    private float transY;


    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public float getTransX() {
        return transX;
    }

    public void setTransX(float transX) {
        this.transX = transX;
    }

    public float getTransY() {
        return transY;
    }

    public void setTransY(float transY) {
        this.transY = transY;
    }

    public TextureTrans(float scaleX, float scaleY, float transX, float transY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.transX = transX;
        this.transY = transY;
    }
}
