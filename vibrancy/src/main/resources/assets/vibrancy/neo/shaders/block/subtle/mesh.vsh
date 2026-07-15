#version 410

struct Light {
    vec3 pos;
    uint shape;
    vec3 color;
    float flicker;
};

uniform usamplerBuffer LightBuffer;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec2 UV0;
in uint LightIndex;
in vec4 Color;
in vec3 Normal;

out vec2 texCoord0;
flat out Light light;
out vec4 vertexColor;
out vec3 vertexPosition;
out vec3 fogPosition;
out vec3 vertexNormal;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1);
    gl_Position = ProjMat * pos;
    texCoord0 = UV0;

    int base = int(LightIndex) * 8;
    light.pos    = vec3(uintBitsToFloat(texelFetch(LightBuffer, base + 0).r),
                        uintBitsToFloat(texelFetch(LightBuffer, base + 1).r),
                        uintBitsToFloat(texelFetch(LightBuffer, base + 2).r));
    light.shape  = texelFetch(LightBuffer, base + 3).r;
    light.color  = vec3(uintBitsToFloat(texelFetch(LightBuffer, base + 4).r),
                        uintBitsToFloat(texelFetch(LightBuffer, base + 5).r),
                        uintBitsToFloat(texelFetch(LightBuffer, base + 6).r));
    light.flicker = uintBitsToFloat(texelFetch(LightBuffer, base + 7).r);

    vertexColor = Color;
    fogPosition = pos.xyz;
    vertexPosition = Position;
    vertexNormal = Normal;
}
