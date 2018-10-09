package cn.embed.media.surfaceEncoder.filter;

import java.nio.FloatBuffer;

public interface IFilter {

    /**
     * 获取纹理目标
     *
     * @return
     */
    int getTextureTarget();

    /**
     * 设置纹理大小
     *
     * @param width
     * @param height
     */
    void setTextureSize(int width, int height);

    /**
     * 使用opengl渲染图像
     *
     * @param mvpMatrix       //模型，视图，投影矩阵
     * @param vertexBuffer    //顶点数组
     * @param firstVertex     //起始索引
     * @param vertexCount     //顶点数量
     * @param coordsPerVertex //每个顶点的坐标
     * @param vertexStride    //顶点坐标的长度
     * @param texMatrix       //纹理矩阵
     * @param texBuffer       //纹理缓存
     * @param textureId       //纹理id
     * @param texStride       //纹理长度
     */
    void onDraw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex, int vertexCount,
                int coordsPerVertex, int vertexStride, float[] texMatrix, FloatBuffer texBuffer,
                int textureId, int texStride);

    void releaseProgram();

    //滤镜初始化
    void init();
}
