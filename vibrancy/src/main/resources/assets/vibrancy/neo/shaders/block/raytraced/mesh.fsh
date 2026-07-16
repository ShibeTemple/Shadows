#version 410

#include "vibrancy:fragment"

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;

uniform vec3 CameraPos;
uniform vec3 LightColor;
uniform vec3 LightPos;
uniform float LightRadius;

uniform bool SpecularReflectionsEnabled;
uniform float SpecularReflectionStrength;
uniform float SpecularReflectionExponent;

uniform float LightFlicker;
uniform float GLFWTime;

in vec2 texCoord0;
in vec2 texCoord1;
in vec4 vertexColor;
in vec3 vertexPosition;
in vec3 fogPosition;
in vec3 vertexNormal;

out vec3 fragColor;

void main() {
    // Read the static (block geometry) shadow map first.
    // If this fragment is fully in block shadow it contributes vec3(0) regardless
    // of block color, entity shadows, attenuation, or specular — so skip all of that.
    vec3 staticShadow = texelFetch(Sampler1, ivec2(texCoord1), 0).rgb;
    if (dot(staticShadow, vec3(1.0)) < 0.001) {
        fragColor = vec3(0.0);
        return;
    }

    vec4 block = texture(Sampler0, texCoord0) * vertexColor;

    if (block.a == 0) {
        discard;
    }

    vec3 lightColor = staticShadow
                    * texelFetch(Sampler2, ivec2(texCoord1), 0).rgb
                    * LightColor
                    * attenuateNoCusp(distance(LightPos, vertexPosition), LightRadius);

    if (SpecularReflectionsEnabled) {
        lightColor = specularReflection(lightColor, lightColor, normalize(LightPos - vertexPosition), CameraPos, vertexPosition, vertexNormal, SpecularReflectionStrength, SpecularReflectionExponent, Sampler3, texCoord0);
    }

    fragColor = applyLight(lightColor, block, fogPosition, LightPos, LightFlicker, GLFWTime);
}