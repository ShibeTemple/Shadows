package net.typho.big_shot_lib.impl.client.rendering.util

import com.mojang.blaze3d.vertex.VertexConsumer
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexConsumer
import net.typho.big_shot_lib.api.util.NeoColor
import com.mojang.blaze3d.vertex.PoseStack
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

class VertexConsumerWrapper(
    @JvmField
    val inner: NeoVertexConsumer
) : VertexConsumer, VertexBufferWriter {
    //? if >=1.21.11 {
    /*override fun setLineWidth(f: Float): VertexConsumer {
        // TODO implement once all support pre-1.21.11 is dropped
        return this
    }
    *///? }

    //? if >=1.21 {
    override fun addVertex(
        f: Float,
        g: Float,
        h: Float
    ): VertexConsumer {
        inner.vertex(f, g, h)
        return this
    }

    override fun setColor(
        i: Int,
        j: Int,
        k: Int,
        l: Int
    ): VertexConsumer {
        inner.color(i, j, k, l)
        return this
    }

    override fun setUv(f: Float, g: Float): VertexConsumer {
        inner.textureUV(f, g)
        return this
    }

    override fun setUv1(i: Int, j: Int): VertexConsumer {
        inner.overlayUV(i, j)
        return this
    }

    override fun setUv2(i: Int, j: Int): VertexConsumer {
        inner.lightUV(i, j)
        return this
    }

    override fun setNormal(
        f: Float,
        g: Float,
        h: Float
    ): VertexConsumer {
        inner.normal(f, g, h)
        return this
    }

    override fun addVertex(
        pose: PoseStack.Pose,
        f: Float,
        g: Float,
        h: Float
    ): VertexConsumer {
        inner.vertex(pose, f, g, h)
        return this
    }

    override fun addVertex(
        //? if <1.21.11 {
        matrix4f: Matrix4f,
        //? } else {
        /*matrix4f: Matrix4fc,
        *///? }
        f: Float,
        g: Float,
        h: Float
    ): VertexConsumer {
        inner.vertex(matrix4f, f, g, h)
        return this
    }

    override fun setColor(
        f: Float,
        g: Float,
        h: Float,
        i: Float
    ): VertexConsumer {
        inner.color(f, g, h, i)
        return this
    }

    override fun setColor(i: Int): VertexConsumer {
        inner.color(i)
        return this
    }

    override fun setNormal(
        pose: PoseStack.Pose,
        f: Float,
        g: Float,
        h: Float
    ): VertexConsumer {
        inner.normal(pose, f, g, h)
        return this
    }

    override fun setLight(i: Int): VertexConsumer {
        inner.lightUV(i)
        return this
    }

    override fun setOverlay(i: Int): VertexConsumer {
        inner.overlayUV(i)
        return this
    }
    //? } else {
    /*var defaultColor: NeoColor? = null

    override fun vertex(
        d: Double,
        e: Double,
        f: Double
    ): VertexConsumer {
        inner.vertex(d.toFloat(), e.toFloat(), f.toFloat())
        defaultColor?.let { inner.color(it) }
        return this
    }

    override fun color(
        i: Int,
        j: Int,
        k: Int,
        l: Int
    ): VertexConsumer {
        inner.color(i, j, k, l)
        return this
    }

    override fun uv(f: Float, g: Float): VertexConsumer {
        inner.textureUV(f, g)
        return this
    }

    override fun overlayCoords(i: Int, j: Int): VertexConsumer {
        inner.overlayUV(i, j)
        return this
    }

    override fun uv2(i: Int, j: Int): VertexConsumer {
        inner.lightUV(i, j)
        return this
    }

    override fun normal(
        f: Float,
        g: Float,
        h: Float
    ): VertexConsumer {
        inner.normal(f, g, h)
        return this
    }

    override fun endVertex() {
        inner.endVertex()
    }

    override fun defaultColor(i: Int, j: Int, k: Int, l: Int) {
        defaultColor = NeoColor.RGBA(i, j, k, l)
    }

    override fun unsetDefaultColor() {
        defaultColor = null
    }
    *///? }

    override fun push(stack: MemoryStack, ptr: Long, count: Int, format: VertexFormatDescription) {
        val stride = format.stride().toLong()

        val hasPos     = format.containsElement(CommonVertexAttribute.POSITION)
        val hasColor   = format.containsElement(CommonVertexAttribute.COLOR)
        val hasTex     = format.containsElement(CommonVertexAttribute.TEXTURE)
        val hasOverlay = format.containsElement(CommonVertexAttribute.OVERLAY)
        val hasLight   = format.containsElement(CommonVertexAttribute.LIGHT)
        val hasNormal  = format.containsElement(CommonVertexAttribute.NORMAL)

        val posOff     = if (hasPos)     format.getElementOffset(CommonVertexAttribute.POSITION).toLong() else 0L
        val colorOff   = if (hasColor)   format.getElementOffset(CommonVertexAttribute.COLOR).toLong()    else 0L
        val texOff     = if (hasTex)     format.getElementOffset(CommonVertexAttribute.TEXTURE).toLong()  else 0L
        val overlayOff = if (hasOverlay) format.getElementOffset(CommonVertexAttribute.OVERLAY).toLong()  else 0L
        val lightOff   = if (hasLight)   format.getElementOffset(CommonVertexAttribute.LIGHT).toLong()    else 0L
        val normalOff  = if (hasNormal)  format.getElementOffset(CommonVertexAttribute.NORMAL).toLong()   else 0L

        for (i in 0 until count) {
            val base = ptr + i * stride

            if (hasPos) {
                inner.vertex(
                    MemoryUtil.memGetFloat(base + posOff),
                    MemoryUtil.memGetFloat(base + posOff + 4),
                    MemoryUtil.memGetFloat(base + posOff + 8)
                )
            }
            if (hasColor) {
                val c = MemoryUtil.memGetInt(base + colorOff)
                inner.color(c and 0xFF, (c ushr 8) and 0xFF, (c ushr 16) and 0xFF, (c ushr 24) and 0xFF)
            }
            if (hasTex) {
                inner.textureUV(
                    MemoryUtil.memGetFloat(base + texOff),
                    MemoryUtil.memGetFloat(base + texOff + 4)
                )
            }
            if (hasOverlay) {
                val o = MemoryUtil.memGetInt(base + overlayOff)
                inner.overlayUV(o and 0xFFFF, (o ushr 16) and 0xFFFF)
            }
            if (hasLight) {
                val l = MemoryUtil.memGetInt(base + lightOff)
                inner.lightUV(l and 0xFFFF, (l ushr 16) and 0xFFFF)
            }
            if (hasNormal) {
                val n = MemoryUtil.memGetInt(base + normalOff)
                inner.normal(
                    (n and 0xFF).toByte().toFloat() / 127f,
                    ((n ushr 8) and 0xFF).toByte().toFloat() / 127f,
                    ((n ushr 16) and 0xFF).toByte().toFloat() / 127f
                )
            }
            inner.endVertex()
        }
    }
}