package net.typho.vibrancy.block.impl

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.blockPos
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.HashMapBlockLightStorage

class RayPointLightStorage : HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>(RayPointLightType) {
    override fun shouldCollectMeshGeometry(pos: SectionPos): Boolean {
        synchronized(map) {
            return map.values.any { it.sections.contains(pos) }
        }
    }

    override fun onLightAdded(manager: LightManager, pos: IVec3<Int>) {
        val light = synchronized(map) { map[pos] } ?: return
        val renderer = SodiumWorldRenderer.instanceNullable() ?: return
        val bounds = light.boundingBox
        renderer.scheduleRebuildForBlockArea(
            bounds.min.x, bounds.min.y, bounds.min.z,
            bounds.max.x, bounds.max.y, bounds.max.z,
            false
        )
    }

    override fun createLight(
        manager: LightManager,
        level: Level,
        state: BlockState,
        pos: IVec3<Int>,
        info: RayPointLightInfo
    ) = if (info.enabled(state)) RayPointLight(level, info, state, pos) else null

    override fun reload(manager: LightManager, chunk: ChunkPos?) {
        synchronized(map) {
            if (chunk == null) {
                map.values.forEach { it.reload() }
            } else {
                map.values.filter { ChunkPos(it.pos.blockPos) == chunk }
                    .forEach { it.reload() }
            }
        }
    }
}