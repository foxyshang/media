#extension GL_OES_EGL_image_external : require
precision mediump float; //指定默认精度
//收缩失真，凹面镜效果

varying vec2 vTextureCoord;
uniform samplerExternalOES uTexture;

uniform highp float aspectRatio;
 uniform highp vec2 center;
 uniform highp float radius;
 uniform highp float scale;

 void main()
 {
     highp vec2 textureCoordinateToUse = vec2(vTextureCoord.x, (vTextureCoord.y * aspectRatio + 0.5 - 0.5 * aspectRatio));
     highp float dist = distance(center, textureCoordinateToUse);
     textureCoordinateToUse = vTextureCoord;

     if (dist < radius)
     {
         textureCoordinateToUse -= center;
         highp float percent = 1.0 + ((0.5 - dist) / 0.5) * scale;
         textureCoordinateToUse = textureCoordinateToUse * percent;
         textureCoordinateToUse += center;

         gl_FragColor = texture2D(uTexture, textureCoordinateToUse );
     }
     else
     {
         gl_FragColor = texture2D(uTexture, vTextureCoord );
     }
}