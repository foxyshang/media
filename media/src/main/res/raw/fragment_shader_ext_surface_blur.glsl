#extension GL_OES_EGL_image_external : require
 uniform samplerExternalOES uTexture;
const lowp int GAUSSIAN_SAMPLES = 49;
const lowp float threshold = 20.0;

varying highp vec2 vTextureCoord;
varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];


void main()
{
	
    lowp vec4 sampleColor;
    lowp vec4 firstColor;
    firstColor= texture2D(uTexture, blurCoordinates[0]);
    int r,g,b;
    for(int i=0;i<GAUSSIAN_SAMPLES;i++){
		 lowp vec4 upColor;
		 lowp vec4 downColor;
		 lowp vec4 tempColor;
		 tempColor=texture2D(uTexture, blurCoordinates[0]);
		 int x=tempColor.r-firstColor.r;
		 if(x<0){x=-x}
		 int upx=(1-(float)x/(2.5*threshold))tempColor.r;
		 int downx=(1-(float)x/(2.5*threshold))
    }
    r=upx/downx;
    g=upy/downy;
    z=upz/downz;
    gl_FragColor=vec4(r,g,b,vTextureCoord.a);
    

    centralColor = texture2D(uTexture, blurCoordinates[4]);
    gaussianWeightTotal = 0.18;
    sum = centralColor * 0.18;

    sampleColor = texture2D(uTexture, blurCoordinates[0]);
    distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);
    gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);
    gaussianWeightTotal += gaussianWeight;
    sum += sampleColor * gaussianWeight;

    sampleColor = texture2D(uTexture, blurCoordinates[1]);
    distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);
    gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
    gaussianWeightTotal += gaussianWeight;
    sum += sampleColor * gaussianWeight;

    sampleColor = texture2D(uTexture, blurCoordinates[2]);
    distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);
    gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);
    gaussianWeightTotal += gaussianWeight;
    sum += sampleColor * gaussianWeight;

    sampleColor = texture2D(uTexture, blurCoordinates[3]);
    distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);
    gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);
    gaussianWeightTotal += gaussianWeight;
    sum += sampleColor * gaussianWeight;

    sampleColor = texture2D(uTexture, blurCoordinates[5]);
    distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);
    gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);
    gaussianWeightTotal += gaussianWeight;
    sum += sampleColor * gaussianWeight;

    sampleColor = texture2D(uTexture, blurCoordinates[6]);
    distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);
    gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);
    gaussianWeightTotal += gaussianWeight;
    sum += sampleColor * gaussianWeight;

    sampleColor = texture2D(uTexture, blurCoordinates[7]);
    distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);
    gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);
    gaussianWeightTotal += gaussianWeight;
    sum += sampleColor * gaussianWeight;

    sampleColor = texture2D(uTexture, blurCoordinates[8]);
    distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);
    gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);
    gaussianWeightTotal += gaussianWeight;
    sum += sampleColor * gaussianWeight;
    gl_FragColor = sum / gaussianWeightTotal;
}