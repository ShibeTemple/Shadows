package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.math.rect.AbstractRect3
import net.typho.big_shot_lib.api.math.rect.AbstractRect3.Companion.sizeInclusive
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glDeleteTextures
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL31.GL_R32UI
import org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER
import org.lwjgl.opengl.GL31.glTexBuffer

open class VoxelGridBuffer(
    @JvmField
    val usage: GlBufferUsage
) : NeoGlBuffer() {
    val tboTexId: Int = glGenTextures()
    var gridMinX: Int = 0; private set
    var gridMinY: Int = 0; private set
    var gridMinZ: Int = 0; private set
    var gridSizeX: Int = 0; private set
    var gridSizeY: Int = 0; private set
    var gridSizeZ: Int = 0; private set
    var gridCellCount: Int = 0; private set

    open fun lazyUpload(texWidth: Int, texHeight: Int, numFaces: Int, bounds: AbstractRect3<Int>, faces: List<Pair<IVec3<Int>, List<PrimitiveQuad>>>): Pair<AutoCloseable, () -> Unit> {
        val minX = bounds.min.x; val minY = bounds.min.y; val minZ = bounds.min.z
        val szX = bounds.sizeInclusive.x; val szY = bounds.sizeInclusive.y; val szZ = bounds.sizeInclusive.z

        // 3D array: header (8 uint32) + szX*szY*szZ cell slots (each 1 uint32 = from(16)|to(16))
        val cellSlots = szX * szY * szZ
        val gridBuffer = NeoBuffer.GCNative((8L + cellSlots) * 4)

        val cellData = IntArray(cellSlots) // 0 means empty (from==to==0)
        var quadIndex = 0

        for ((pos, faceList) in faces) {
            if (faceList.isNotEmpty() && bounds.contains(pos)) {
                val lx = pos.x - minX; val ly = pos.y - minY; val lz = pos.z - minZ
                val idx = lz * szX * szY + ly * szX + lx
                val from = quadIndex
                val to = quadIndex + faceList.size
                cellData[idx] = (from and 0xFFFF) or (to shl 16)
            }
            quadIndex += faceList.size
        }

        gridBuffer.write().run {
            writeInt(minX); writeInt(minY); writeInt(minZ); writeInt(0)
            writeInt(szX);  writeInt(szY);  writeInt(szZ);  writeInt(0)
            for (cell in cellData) writeInt(cell)
        }

        return AutoCloseable {
            gridBuffer.free()
        } to {
            gridMinX = minX; gridMinY = minY; gridMinZ = minZ
            gridSizeX = szX; gridSizeY = szY; gridSizeZ = szZ
            gridCellCount = cellSlots

            bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(gridBuffer, usage) }

            glBindTexture(GL_TEXTURE_BUFFER, tboTexId)
            glTexBuffer(GL_TEXTURE_BUFFER, GL_R32UI, glId)
            glBindTexture(GL_TEXTURE_BUFFER, 0)
        }
    }

    override fun free() {
        glDeleteTextures(tboTexId)
        super.free()
    }
}
