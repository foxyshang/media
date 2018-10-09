precision mediump float;//fragment中没有默认的浮点数精度修饰符。因此，对于浮点数，浮点数向量和矩阵变量声明，必须声明包含一个精度修饰符。
 attribute vec4 position;
 attribute vec4 inputTextureCoordinate;
 const int GAUSSIAN_SAMPLES = 9;
 uniform float texelWidthOffset;
 uniform float texelHeightOffset;
 varying vec2 textureCoordinate;
 varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];

 void main()
 {
 	gl_Position = position;
 	textureCoordinate = inputTextureCoordinate.xy;

 	// Calculate the positions for the blur
 	int multiplier = 0;
 	vec2 blurStep;
    vec2 singleStepOffset = vec2(texelHeightOffset, texelWidthOffset);

 	for (int i = 0; i < GAUSSIAN_SAMPLES; i++)
    {
 		multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));
        // Blur in x (horizontal)
        blurStep = float(multiplier) * singleStepOffset;
 		blurCoordinates[i] = inputTextureCoordinate.xy + blurStep;
 	}
                    }