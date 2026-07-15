// fogPosition passed in is already in eye space (ModelViewMat * (Position - CameraPos)),
// so fog_distance does not need a model-view matrix.
uniform float FogStart;
uniform float FogEnd;
uniform int FogShape;

float fogFade(vec3 pos) {
    float d = (FogShape == 0) ? length(pos) : max(length(pos.xz), abs(pos.y));
    if (d <= FogStart) return 1.0;
    if (d >= FogEnd) return 0.0;
    return smoothstep(FogEnd, FogStart, d);
}
