uniform mat4 uMVPMatrix;  // MVP 的变换矩阵（整体变形）
uniform mat4 uTexMatrix;  // Texture 的变换矩阵 （只对texture变形）

attribute vec4 aPosition;
attribute vec4 aTextureCoord;

const int GAUSSIAN_SAMPLES = 49;

uniform vec2 singleStepOffset;

varying vec2 vTextureCoord;
varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];

void main()
{
	 gl_Position =  uMVPMatrix*aPosition;
	vTextureCoord = (uTexMatrix * aTextureCoord).xy;


	int multiplier = 0;
	vec2 blurStep;

	for (int i = 0; i < GAUSSIAN_SAMPLES; i++)
	{
		multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));

		blurStep = float(multiplier) * singleStepOffset;
		blurCoordinates[i] = (uTexMatrix * aTextureCoord).xy + blurStep;
	}
}