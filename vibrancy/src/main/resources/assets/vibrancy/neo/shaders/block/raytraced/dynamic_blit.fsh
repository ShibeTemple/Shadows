#version 410

#include "vibrancy:fragment"
#include "vibrancy:rays"

struct BVH {
    vec3 min;
    uint start;
    vec3 max;
    uint end;
};

uniform usamplerBuffer ShadowQuadBuffer;
uniform usamplerBuffer BVHBuffer;
uniform usamplerBuffer TextureInfoBuffer;
uniform int BVHCount;

uniform sampler2D Samplers[8];
uniform ivec2 SamplersSizes[8];

uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform float LightBrightness;

in vec3 vertexPos;

out vec3 fragColor;

struct Ray {
    vec3 pos;
    vec3 dir;
    vec3 invDir;
    float len;
};

Ray ray(vec3 pos) {
    vec3 delta = LightPos - pos;
    vec3 dir = normalize(delta);
    float len = length(delta);
    return Ray(pos, dir, 1 / dir, len);
}

BVH fetchBVH(int i) {
    int b = i * 8;
    BVH bvh;
    bvh.min   = vec3(uintBitsToFloat(texelFetch(BVHBuffer, b + 0).r),
                     uintBitsToFloat(texelFetch(BVHBuffer, b + 1).r),
                     uintBitsToFloat(texelFetch(BVHBuffer, b + 2).r));
    bvh.start = texelFetch(BVHBuffer, b + 3).r;
    bvh.max   = vec3(uintBitsToFloat(texelFetch(BVHBuffer, b + 4).r),
                     uintBitsToFloat(texelFetch(BVHBuffer, b + 5).r),
                     uintBitsToFloat(texelFetch(BVHBuffer, b + 6).r));
    bvh.end   = texelFetch(BVHBuffer, b + 7).r;
    return bvh;
}

Quad fetchQuad(int j) {
    int b = j * 16;
    Quad q;
    q.vert1 = vec3(uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 0).r),
                   uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 1).r),
                   uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 2).r));
    q.uv1   = texelFetch(ShadowQuadBuffer, b + 3).r;
    q.vert2 = vec3(uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 4).r),
                   uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 5).r),
                   uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 6).r));
    q.uv2   = texelFetch(ShadowQuadBuffer, b + 7).r;
    q.vert3 = vec3(uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 8).r),
                   uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 9).r),
                   uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 10).r));
    q.uv3   = texelFetch(ShadowQuadBuffer, b + 11).r;
    q.vert4 = vec3(uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 12).r),
                   uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 13).r),
                   uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 14).r));
    q.uv4   = texelFetch(ShadowQuadBuffer, b + 15).r;
    return q;
}

void main() {
    Ray ray = ray(vertexPos);

    fragColor = vec3(1);
    vec3 tint = vec3(0);
    float denom = 0;

    for (int i = 0; i < BVHCount; i++) {
        BVH bvh = fetchBVH(i);

        if (raycastAABB(ray.pos, ray.invDir, ray.len, AABB(bvh.min, bvh.max))) {
            for (uint j = bvh.start; j < bvh.end; j++) {
                float dist;
                vec4 outColor;
                Quad quad = fetchQuad(int(j));
                uint texture = texelFetch(TextureInfoBuffer, int(j)).r;

                if (sampleQuad(false, Samplers[texture], SamplersSizes[texture], ray.pos, ray.dir, ray.len, 1e-3, quad, dist, outColor)) {
                    if (outColor.a == 1) {
                        fragColor = vec3(0);
                        break;
                    } else if (outColor.a != 0) {
                        tint += outColor.rgb * outColor.a;
                        denom += outColor.a;
                    }
                }
            }
        }
    }

    if (denom > 0) {
        fragColor *= tint / denom;
    }
}
