# Vibrancy (1.20.1 Downport)

A **Minecraft 1.20.1** Fabric port of [Vibrancy](https://github.com/TheTypholorian/vibrancy) and [Big Shot Lib](https://github.com/TheTypholorian/big_shot_lib), originally targeting 1.21.11. This fork brings the mod to older Minecraft versions with a focus on **macOS OpenGL compatibility** (older OpenGL versions that macOS is restricted to).

> [!NOTE]
> This repository contains two independent mod projects:
> - **`vibrancy/`** — The main visuals mod
> - **`big_shot_lib/`** — The rendering library dependency

---

## What is Vibrancy?

Vibrancy reworks Minecraft's lighting system while keeping the feel of the game. Features include:

- **Ray-traced dynamic shadows** from blocks, items, and the sun
- **Colored block lights** (e.g., redstone emits red light, lapis emits blue)
- **Sky lighting** with configurable shadow distance and resolution
- **Specular reflections** for shiny surfaces
- **Entity shadows** cast from block light sources
- **Bundled resource pack** with glowing ore variants
- **Extensive configuration** via YACL (Yet Another Config Lib) with automatic potato GPU detection
- **ModMenu integration** for in-game config access

## What is Big Shot Lib?

Big Shot Rendering Library abstracts Minecraft's rendering internals into a stable API, providing:

- Custom **shader compilation and preprocessing** pipeline
- **Mesh building** utilities
- OpenGL resource management (textures, framebuffers, buffers, programs, samplers)
- GL state management and stacking
- Shader mixins that survive Minecraft version updates
- An `api` submodule with a full OpenGL abstraction layer

---

## Porting Notes

**Goal:** Port Vibrancy (raytraced point-light shadows) from MC 1.21 / GLSL 4.30 to MC 1.20.1 / Fabric / Sodium 0.5.13 / GLSL 4.10.

The original mods target Minecraft 1.21+ and assume modern OpenGL availability. This fork required deep changes to the shader pipeline and rendering logic.

### Dependency Versions

| | Original (1.21+) | This Fork (1.20.1) |
|---|---|---|
| **Minecraft** | 1.21.0–1.21.11 | 1.20.1 |
| **Sodium** | 0.6.x / 0.8.x | 0.5.13 |
| **OpenGL** | Modern (4.x+) | Older (macOS-compatible) |
| **LWJGL** | Latest bundled | Forced 3.3.1 (Sodium 0.5.x compat) |
| **Fabric API** | 0.116+ | 0.92.9 |
| **YACL** | 3.8+ | 3.6.6 |
| **ModMenu** | 11.0.3 | 7.2.2 |
| **Java** | 21 | 17 |

### What Was Ported / Fixed

#### 1. SSBO → TBO (the core API downgrade)

1.21 used `layout(std430) readonly buffer` (GLSL 4.30 SSBOs). 1.20 only supports GLSL 4.10, so both `ShadowQuadBuffer` (face geometry) and `GridBuffer` (voxel grid) were rewritten to use `usamplerBuffer` + `texelFetch()`. The header of `GridBuffer` stores `GridMin` (`ivec3`) and `GridSize` (`ivec3`) as the first 8 `uint32` texels; cell data follows at offset 8. `fetchComplexQuad(int j)` reads face `j` as 32 consecutive `uint32` texels (4 verts × 8 fields each).

#### 2. ModelViewMat bug (root cause of invisible Vibrancy light)

The 1.20 port added an extra translation `translate(lightPos − cameraPos)` to `ModelViewMat`, but the vertex shader already subtracts `CameraPos`. The geometry was displaced by 2×(lightPos−cameraPos), putting it off-screen. Fixed by restoring `set(data.modelViewMat)` with no translation, matching the original.

#### 3. Self-shadowing — light's own voxel

The torch's shaft geometry sits in voxel `(0,0,0)` (same as `floor(LightPos)`). Every floor face starts in that voxel and immediately hits the torch's shaft faces. Fixed by skipping `voxel == ivec3(floor(LightPos))` in the DDA loop.

#### 4. NaN in raycastQuad when ray is parallel to face

`denom = 0` caused `tt = 0/0 = NaN`, which passed both margin checks and produced false hits. Fixed by adding `if (abs(denom) < 1e-6) return false` before the `tt` computation (in `rays.glsl`).

#### 5. Shadow receiver range (the "bounding box" artifact)

The original's DDA traverses the full ray path from the face to the light and only tests faces when the current voxel is inside the shadow grid. Our initial port stopped the DDA as soon as the starting voxel left the grid, so faces far from the torch could never receive shadows from nearby occluders. Fixed by switching the loop from `while (isInGrid(voxel, ...))` to `while (t <= ray.len)` with an inner grid-bounds check.

#### 6. Shadow softness — semi-transparent alpha accumulation

The original returns `vec3(0)` only for `alpha == 1.0` hits; for `0 < alpha < 1` it accumulates a weighted colour tint and returns `tint/denom` at the end (coloured light through leaves/glass). Our initial port hard-cut at `alpha > 0.1`. Fixed by matching the original's accumulation logic exactly.

#### 7. `margin * sign(denom)` in raycastQuad

The original uses an asymmetric margin: back-face hits reject `tt < +margin`, front-face hits reject `tt < -margin` (allowing grazing-angle hits that ours would discard). Updated `rays.glsl` to match.

### Current State

Shadows work: point lights cast raytraced shadows through fences, blocks, and transparent geometry. Semi-transparent hits (leaves, glass) produce tinted rather than fully dark shadows. Shadow occluders are limited to within `rayLightShadowRadius` (default now 16 to match torch radius) but receivers are not limited.

### Known Remaining Gaps vs. Original

- No specular reflections (uniforms exist in `mesh.fsh` but the 1.20 port doesn't supply `Sampler3`)
- No entity/dynamic shadows (`dynamicBlit` shader not ported)
- No BVH for the dynamic shadow pass
- Performance: 32 `texelFetch` calls per quad (TBO) vs one SSBO struct read — acceptable but slower than 1.21

---

## Building

### Prerequisites

- **Java 17** (or higher)
- **Git**

### Build

First, build Big Shot Lib (Vibrancy depends on it):

```bash
cd big_shot_lib
git checkout mc1_20_fabric
./gradlew build
cd ..
```

Then build Vibrancy:

```bash
cd vibrancy
git checkout mc1_20_fabric
./gradlew build
```

The built JARs will be in:
- `vibrancy/build/libs/`
- `big_shot_lib/build/libs/`

> **Note:** The Stonecutter plugin manages multi-version builds. The active version for this repository is `mc1_20_fabric`. Switching versions may require running `./gradlew chiseledBuild` from the respective project directory.

---

## Project Structure

```
.
├── vibrancy/                    # Vibrancy mod (main mod)
│   ├── src/main/java/           # Java sources (mixins)
│   ├── src/main/kotlin/         # Kotlin sources (core logic)
│   ├── src/main/resources/      # Mod metadata, assets, lang files
│   ├── versions/                # Per-version build properties (20 targets)
│   ├── resource_packs/          # Bundled glowing ores resource pack
│   └── build.fabric.gradle.kts  # Fabric build script
│
├── big_shot_lib/                # Big Shot Rendering Library
│   ├── api/                     # API submodule (GL abstraction layer)
│   ├── src/main/java/           # Java sources (mixins)
│   ├── src/main/kotlin/         # Kotlin sources (implementation)
│   ├── src/main/resources/      # Mod metadata, access wideners
│   ├── versions/                # Per-version build properties (20 targets)
│   └── build.fabric.gradle.kts  # Fabric build script
│
└── README.md
```

---

## Requirements

Both mods are **client-side only**.

| Dependency | Required Version |
|---|---|
| Minecraft | 1.20.1 |
| Fabric Loader | ≥ 0.19.2 |
| Fabric API | 0.92.9+1.20.1 |
| Sodium | mc1.20.1-0.5.13-fabric |
| Fabric Language Kotlin | 1.13.4+kotlin.2.2.0 |
| YACL (Yet Another Config Lib) | 3.6.6+1.20.1-fabric |
| ModMenu | 7.2.2 |

> [!WARNING]
> **Iris is incompatible** with Vibrancy. Do not run both together.

---

## Configuration

Vibrancy provides a config screen via ModMenu (press the mod's settings button in the mods list) or by editing `vibrancy.json` in your Minecraft config directory.

Key settings:
- **General** — Enable/disable mod, multithreading, async thread count, light brightness limits, flicker strength
- **Block Lights (Ray Traced)** — Enable, max rendered lights, brightness, shadow radius, high-quality shadow count
- **Block Lights (Subtle)** — Enable, render distance, brightness, culling mode
- **Sky Lights** — Enable, shadow distance, brightness, shadow map resolution/power, translucent support
- **Specular Reflections** — Enable, strength, exponent
- **Entity Shadows** — Enable, block entity shadows, distance, max block lights

The mod auto-detects low-end hardware (Intel/AMD integrated GPUs) and adjusts defaults accordingly.

---

## License

This repository contains code from two upstream projects, both licensed under the **MIT License**:

- **Vibrancy** — Copyright (c) 2026 The Typhothanian
- **Big Shot Lib** — Copyright (c) 2026 The Typhothanian

The MIT License permits use, modification, and redistribution with attribution. The full MIT License text is preserved in the respective project directories:

- [`vibrancy/LICENSE`](vibrancy/LICENSE)
- [`big_shot_lib/LICENSE`](big_shot_lib/LICENSE)

### This Fork

The 1.20.1 downport and macOS OpenGL compatibility modifications are **Copyright (c) 2026 ShibeTemple. All rights reserved.**

You may not use, modify, distribute, or sublicense any portions of this repository that are original to this fork (the 1.20.1 port, macOS OpenGL fixes, and any other changes not present in the upstream projects) without explicit written permission from the copyright holder.

> If you are a developer interested in distributing or modifying this work, ShibeTemple is very open to discussions. Please reach out before redistributing or forking — permission is likely to be granted for reasonable requests, but communication must come first.

The original upstream files retain their MIT License and original copyright:

```
Vibrancy (c) 2026 The Typhothanian  — MIT License (see vibrancy/LICENSE)
Big Shot Lib (c) 2026 The Typhothanian  — MIT License (see big_shot_lib/LICENSE)
1.20.1 Port and macOS OpenGL compatibility modifications (c) 2026 ShibeTemple — All Rights Reserved
```

All original copyright notices must be retained in any distribution that includes upstream code. See the [MIT License](https://opensource.org/licenses/MIT) for the terms governing the upstream portions.

---

## Credits

- **The Typhothanian** — Original author of [Vibrancy](https://github.com/TheTypholorian/vibrancy) and [Big Shot Lib](https://github.com/TheTypholorian/big_shot_lib)
- Sodium — Performance rendering library by [CaffeineMC](https://github.com/CaffeineMC/sodium-fabric)
- YACL — Configuration library by [ismaeljake](https://github.com/ismaeljake/yacl)
- Fabric — Mod loader and API by [FabricMC](https://fabricmc.net/)
- Stonecutter — Multi-version build tool by [kikugie](https://github.com/kikugie/stonecutter)
