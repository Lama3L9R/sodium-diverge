package net.caffeinemc.mods.sodium.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatOffsetCache;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteCoordinateExpander.class)
public abstract class SpriteCoordinateExpanderMixin implements VertexBufferWriter {
    @Shadow
    public abstract VertexConsumer delegate();

    @Shadow
    public abstract net.minecraft.client.renderer.texture.UvMapping mapping();

    @Unique
    private boolean canUseIntrinsics;

    @Unique
    private float minU, minV;

    @Unique
    private float maxU, maxV;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(VertexConsumer delegate, net.minecraft.client.renderer.texture.UvMapping mapping, CallbackInfo ci) {
        if (mapping instanceof TextureAtlasSprite sprite) {
            SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }

        this.minU = mapping.getU(0.0F);
        this.minV = mapping.getV(0.0F);
        this.maxU = mapping.getU(1.0F);
        this.maxV = mapping.getV(1.0F);
        this.canUseIntrinsics = VertexBufferWriter.tryOf(delegate) != null;
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.canUseIntrinsics;
    }

    @Override
    public void push(MemoryStack stack, final long ptr, int count, VertexFormat format) {
        transform(ptr, count, format,
                this.minU, this.minV, this.maxU, this.maxV);

        VertexBufferWriter.of(this.delegate())
                .push(stack, ptr, count, format);
    }

    /**
     * Transforms the texture UVs for each vertex from their absolute coordinates into the sprite area specified
     * by the parameters.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param minU   The minimum X-coordinate of the sprite bounds
     * @param minV   The minimum Y-coordinate of the sprite bounds
     * @param maxU   The maximum X-coordinate of the sprite bounds
     * @param maxV   The maximum Y-coordinate of the sprite bounds
     */
    @Unique
    private static void transform(long ptr, int count, VertexFormat format,
                                  float minU, float minV, float maxU, float maxV) {
        long stride = format.getVertexSize();

        var cache = VertexFormatOffsetCache.getInstance().getCachedOffsets(format);

        var offsetUV = cache[VertexFormatOffsetCache.UV];

        // The width/height of the sprite
        float w = maxU - minU;
        float h = maxV - minV;

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            // The texture coordinates relative to the sprite bounds
            float u = TextureAttribute.getU(ptr + offsetUV);
            float v = TextureAttribute.getV(ptr + offsetUV);

            // The texture coordinates in absolute space on the sprite sheet
            float ut = minU + (w * u);
            float vt = minV + (h * v);

            TextureAttribute.put(ptr + offsetUV, ut, vt);

            ptr += stride;
        }
    }
}
