package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(RenderTarget.class)
public interface RenderTargetAccessor {
    @Accessor("depthBufferId")
    int vibrancy$getDepthBufferId();
}
