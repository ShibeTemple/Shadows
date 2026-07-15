#version 410

#include "vibrancy:fragment"
#include "vibrancy:rays"

uniform usamplerBuffer ShadowQuadBuffer;
uniform usamplerBuffer GridBuffer;
uniform ivec3 GridMin;
uniform ivec3 GridSize;
uniform int GridCellCount;

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;

uniform vec3 LightPos;
uniform int ShadowRadius;

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

bool isInGrid(ivec3 voxel, ivec3 gridMin, ivec3 gridMax) {
    return all(greaterThanEqual(voxel, gridMin)) && all(lessThan(voxel, gridMax));
}

ComplexQuad fetchComplexQuad(int j) {
    int b = j * 32;
    ComplexQuad q;
    q.vert1    = vec3(uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 0).r),
                      uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 1).r),
                      uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 2).r));
    q.u1       = uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 3).r);
    q.v1       = uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 4).r);
    q.overlay1 = texelFetch(ShadowQuadBuffer, b + 5).r;
    q.color1   = texelFetch(ShadowQuadBuffer, b + 6).r;
    q.normal1  = texelFetch(ShadowQuadBuffer, b + 7).r;
    q.vert2    = vec3(uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 8).r),
                      uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 9).r),
                      uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 10).r));
    q.u2       = uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 11).r);
    q.v2       = uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 12).r);
    q.overlay2 = texelFetch(ShadowQuadBuffer, b + 13).r;
    q.color2   = texelFetch(ShadowQuadBuffer, b + 14).r;
    q.normal2  = texelFetch(ShadowQuadBuffer, b + 15).r;
    q.vert3    = vec3(uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 16).r),
                      uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 17).r),
                      uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 18).r));
    q.u3       = uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 19).r);
    q.v3       = uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 20).r);
    q.overlay3 = texelFetch(ShadowQuadBuffer, b + 21).r;
    q.color3   = texelFetch(ShadowQuadBuffer, b + 22).r;
    q.normal3  = texelFetch(ShadowQuadBuffer, b + 23).r;
    q.vert4    = vec3(uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 24).r),
                      uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 25).r),
                      uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 26).r));
    q.u4       = uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 27).r);
    q.v4       = uintBitsToFloat(texelFetch(ShadowQuadBuffer, b + 28).r);
    q.overlay4 = texelFetch(ShadowQuadBuffer, b + 29).r;
    q.color4   = texelFetch(ShadowQuadBuffer, b + 30).r;
    q.normal4  = texelFetch(ShadowQuadBuffer, b + 31).r;
    return q;
}

vec3 castShadow(Ray ray) {
    ivec3 gridMax = GridMin + GridSize;
    ivec3 voxel = ivec3(floor(ray.pos));
    ivec3 step = ivec3(sign(ray.dir));
    ivec3 lightVoxel = ivec3(floor(LightPos));

    vec3 nextPos;
    nextPos.x = ray.dir.x > 0 ? float(voxel.x + 1) : float(voxel.x);
    nextPos.y = ray.dir.y > 0 ? float(voxel.y + 1) : float(voxel.y);
    nextPos.z = ray.dir.z > 0 ? float(voxel.z + 1) : float(voxel.z);

    vec3 tMax = (nextPos - ray.pos) * ray.invDir;
    vec3 tDelta = abs(ray.invDir);
    float t = 0.0;

    vec3 tint = vec3(0.0);
    float denom = 0.0;

    // Traverse the full path from the face to the light.
    // Only test faces when inside the shadow grid — lets faces far from the
    // torch still receive shadows from occluders close to the torch.
    while (t <= ray.len) {
        if (voxel != lightVoxel && all(greaterThanEqual(voxel, GridMin)) && all(lessThan(voxel, gridMax))) {
            ivec3 local = voxel - GridMin;
            int cellIdx = local.z * GridSize.x * GridSize.y + local.y * GridSize.x + local.x;
            uint cellRange = texelFetch(GridBuffer, 8 + cellIdx).r;
            uint from = cellRange & 0xFFFFu;
            uint to   = cellRange >> 16;

            for (uint j = from; j < to; j++) {
                float dist;
                vec4 outColor;
                ComplexQuad quad = fetchComplexQuad(int(j));
                if (sampleComplexQuad(false, Sampler0, Sampler0Size, ray.pos, ray.dir, ray.len, 1e-3, quad, dist, outColor)) {
                    if (outColor.a == 1.0) {
                        return vec3(0.0);
                    } else if (outColor.a != 0.0) {
                        tint += outColor.rgb * outColor.a;
                        denom += outColor.a;
                    }
                }
            }
        }

        vec3 tCand = tMax;
        if (step.x == 0) tCand.x = 1e30;
        if (step.y == 0) tCand.y = 1e30;
        if (step.z == 0) tCand.z = 1e30;

        ivec3 oldVoxel = voxel;
        if (tCand.x < tCand.y) {
            if (tCand.x < tCand.z) { t = tCand.x; voxel.x += step.x; tMax.x += tDelta.x; }
            else                    { t = tCand.z; voxel.z += step.z; tMax.z += tDelta.z; }
        } else {
            if (tCand.y < tCand.z) { t = tCand.y; voxel.y += step.y; tMax.y += tDelta.y; }
            else                   { t = tCand.z; voxel.z += step.z; tMax.z += tDelta.z; }
        }
        if (oldVoxel == voxel) break;
    }

    if (denom > 0.0) {
        return tint / denom;
    }
    return vec3(1.0);
}

void main() {
    fragColor = castShadow(ray(vertexPos));
}
