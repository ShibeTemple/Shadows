#version 410

uniform sampler2D Sampler0;

in vec2 texCoord0;

void main() {
    // Bias Vibrancy's depth copy slightly away from the camera so light mesh faces
    // at exact scene depth always satisfy the LEQUAL test, regardless of view angle.
    // 0.0001 NDC depth ≈ 0.002 blocks at 1-block distance — too small to bleed light.
    gl_FragDepth = min(texture(Sampler0, texCoord0).r + 0.0001, 1.0);
}
