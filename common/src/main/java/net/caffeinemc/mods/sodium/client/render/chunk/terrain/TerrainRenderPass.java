package net.caffeinemc.mods.sodium.client.render.chunk.terrain;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class TerrainRenderPass {
    @Deprecated(forRemoval = true)
    private final ChunkSectionLayer renderType;

    private final boolean isTranslucent;
    private final boolean fragmentDiscard;

    public TerrainRenderPass(ChunkSectionLayer renderType, boolean isTranslucent, boolean allowFragmentDiscard) {
        this.renderType = renderType;

        this.isTranslucent = isTranslucent;
        this.fragmentDiscard = allowFragmentDiscard;
    }

    public boolean isTranslucent() {
        return this.isTranslucent;
    }

    public boolean supportsFragmentDiscard() {
        return this.fragmentDiscard;
    }

    public RenderPipeline getPipeline() {
        return this.renderType.pipeline(true);
    }

}
