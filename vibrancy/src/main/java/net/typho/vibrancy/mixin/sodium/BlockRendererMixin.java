package net.typho.vibrancy.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
//? if >=1.21 {
/*import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
*///? }
//? if <1.21 {
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
//? }
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas;
import net.typho.vibrancy.shadows.LightFace;
import net.typho.vibrancy.shadows.PrimitiveVertex;
import net.typho.vibrancy.util.SectionMeshCache;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21 {
/*import org.spongepowered.asm.mixin.Shadow;
*///? }
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderer.class)
public class BlockRendererMixin {
    @Unique
    private BlockPos vibrancy$block;

    //? if >=1.21 {
    /*@Shadow
    private ChunkBuildBuffers buffers;
    @Unique
    private NeoAtlas vibrancy$atlas;

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void renderModel(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        vibrancy$block = pos;
    }

    @Inject(method = "prepare", at = @At("TAIL"))
    private void prepare(ChunkBuildBuffers buffers, LevelSlice level, TranslucentGeometryCollector collector, CallbackInfo ci) {
        vibrancy$atlas = NeoAtlas.Companion.getBlocks();
    }

    @Unique
    private PrimitiveVertex vibrancy$convertVertex(ChunkVertexEncoder.Vertex vertex, int normal, float offX, float offY, float offZ) {
        return new PrimitiveVertex(vertex.x + offX, vertex.y + offY, vertex.z + offZ, vertex.color, vertex.u, vertex.v, vertex.light, normal);
    }

    @Inject(method = "bufferQuad", at = @At("TAIL"))
    private void bufferQuad(MutableQuadViewImpl quad, float[] brightnesses, Material material, CallbackInfo ci, @Local ChunkVertexEncoder.Vertex[] vertices) {
        SectionMeshCache cache = ((SectionMeshCache.Holder) buffers).getVibrancy$sectionMeshCache();
        if (cache != null && vibrancy$block != null) {
            float offX = -SectionPos.sectionRelative(vibrancy$block.getX());
            float offY = -SectionPos.sectionRelative(vibrancy$block.getY());
            float offZ = -SectionPos.sectionRelative(vibrancy$block.getZ());
            int normal = quad.getFaceNormal();
            cache.getOrCreate(vibrancy$block).get(material).add(
                    new LightFace(
                            vibrancy$convertVertex(vertices[0], normal, offX, offY, offZ),
                            vibrancy$convertVertex(vertices[1], normal, offX, offY, offZ),
                            vibrancy$convertVertex(vertices[2], normal, offX, offY, offZ),
                            vibrancy$convertVertex(vertices[3], normal, offX, offY, offZ),
                            vibrancy$atlas
                    )
            );
        }
    }
    *///? }

    //? if <1.21 {
    @Unique
    private ChunkBuildBuffers vibrancy$buffers;

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void renderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers, CallbackInfo ci) {
        vibrancy$buffers = buffers;
        vibrancy$block = ctx.pos();
    }

    @Unique
    private PrimitiveVertex vibrancy$convertBakedVertex(BakedQuadView quad, int idx, int normal) {
        return new PrimitiveVertex(
                quad.getX(idx), quad.getY(idx), quad.getZ(idx),
                quad.getColor(idx), quad.getTexU(idx), quad.getTexV(idx),
                0, normal
        );
    }

    @Inject(method = "writeGeometry", at = @At("TAIL"))
    private void writeGeometry(
            BlockRenderContext ctx,
            ChunkModelBuilder builder,
            Vec3 offset,
            Material material,
            BakedQuadView quad,
            int[] colors,
            QuadLightData light,
            CallbackInfo ci
    ) {
        if (vibrancy$buffers == null || vibrancy$block == null) return;
        SectionMeshCache cache = ((SectionMeshCache.Holder) vibrancy$buffers).getVibrancy$sectionMeshCache();
        if (cache == null) return;

        Direction dir = quad.getLightFace();
        int normal = ((dir.getStepX() * 127) << 16) | ((dir.getStepY() * 127) << 8) | (dir.getStepZ() * 127 & 0xFF);

        LightFace face = new LightFace(
                vibrancy$convertBakedVertex(quad, 0, normal),
                vibrancy$convertBakedVertex(quad, 1, normal),
                vibrancy$convertBakedVertex(quad, 2, normal),
                vibrancy$convertBakedVertex(quad, 3, normal),
                NeoAtlas.Companion.getBlocks()
        );

        SectionMeshCache.Block block = cache.getOrCreate(vibrancy$block);
        if (material.pass.isReverseOrder()) {
            if (block.translucentFaces == null) block.translucentFaces = new java.util.ArrayList<>();
            block.translucentFaces.add(face);
        } else {
            if (block.solidFaces == null) block.solidFaces = new java.util.ArrayList<>();
            block.solidFaces.add(face);
        }
    }
    //? }
}
