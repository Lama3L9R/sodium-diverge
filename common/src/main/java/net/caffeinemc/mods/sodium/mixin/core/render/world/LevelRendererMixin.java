package net.caffeinemc.mods.sodium.mixin.core.render.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.util.GameRendererStorage;
import net.caffeinemc.mods.sodium.client.util.IgnoringSectionRenderDispatcher;
import net.caffeinemc.mods.sodium.client.util.IgnoringViewArea;
import net.caffeinemc.mods.sodium.client.util.SodiumChunkSection;
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension;
import net.caffeinemc.mods.sodium.mixin.core.render.texture.TextureAtlasAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.LeavesBlock;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererExtension {
    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private LevelRenderState levelRenderState;
    @Shadow
    @Final
    private CloudRenderer cloudRenderer;

    @Shadow
    public abstract void clearVisibleSections();

    @Shadow
    private @Nullable ViewArea viewArea;
    @Shadow
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;
    @Shadow
    @Final
    private SectionOcclusionGraph sectionOcclusionGraph;
    @Unique
    private SodiumWorldRenderer renderer;

    @Unique
    private ChunkRenderMatrices matrices;

    @Override
    public SodiumWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Redirect(
            method = "invalidateCompiledGeometry",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getEffectiveRenderDistance()I",
                    ordinal = 0))
    private int nullifyBuiltChunkStorage(Options options) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityRenderDispatcher entityRenderDispatcher,
                      BlockEntityRenderDispatcher blockEntityRenderDispatcher,
                      ModelManager modelManager,
                      TextureManager textureManager,
                      AtlasManager atlasManager,
                      ShaderManager shaderManager,
                      GameRenderer gameRenderer,
                      int width,
                      int height,
                      CallbackInfo ci) {
        this.renderer = new SodiumWorldRenderer(Minecraft.getInstance());
    }

    /**
     * @reason Redirect the check to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean hasRenderedAllSections() {
        return this.renderer.isTerrainRenderComplete();
    }

    @Inject(method = "resetLevelRenderData", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.renderer.scheduleTerrainUpdate();
    }

    @Inject(method = "endFrame", at = @At("RETURN"))
    private void sodium$endFrame(CallbackInfo ci) {
        this.renderer.endFrame();
    }

    /**
     * @reason Redirect to our renderer
     * @author IMS
     */
    @Overwrite
    public ChunkSectionsToRender prepareChunkRenders(Matrix4fc matrix4fc, boolean sortTranslucentSections) {
        return this.sodium$createChunkRenderState(matrix4fc);
    }

    @Overwrite
    public ChunkSectionsToRender prepareChunkRendersIndirect(Matrix4fc matrix4fc, boolean sortTranslucentSections) {
        return this.sodium$createChunkRenderState(matrix4fc);
    }

    @Unique
    private ChunkSectionsToRender sodium$createChunkRenderState(Matrix4fc modelViewMatrix) {
        var atlas = Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);
        var atlasAccess = (TextureAtlasAccessor) atlas;
        var terrainTransform = RenderSystem.getDynamicUniforms().writeTerrainTransform(modelViewMatrix,
                atlasAccess.sodium$getWidth(),
                atlasAccess.sodium$getHeight());

        Map<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> map = new EnumMap<>(ChunkSectionLayer.class);

        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            map.put(layer, List.of());
        }

        return new ChunkSectionsToRender.DrawSeparate(terrainTransform, map, 0, new GpuBufferSlice[0]);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareChunkRenders(Lorg/joml/Matrix4fc;Z)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"))
    private ChunkSectionsToRender sodium$wrapPrepareChunkRenders(LevelRenderer instance,
                                                                  Matrix4fc modelViewMatrix,
                                                                  boolean sortTranslucentSections,
                                                                  Operation<ChunkSectionsToRender> original,
                                                                  @Local Vector4f fogColor) {
        return this.sodium$setRenderState(instance, modelViewMatrix, sortTranslucentSections, original, fogColor);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareChunkRendersIndirect(Lorg/joml/Matrix4fc;Z)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"))
    private ChunkSectionsToRender sodium$wrapPrepareChunkRendersIndirect(LevelRenderer instance,
                                                                          Matrix4fc modelViewMatrix,
                                                                          boolean sortTranslucentSections,
                                                                          Operation<ChunkSectionsToRender> original,
                                                                          @Local Vector4f fogColor) {
        return this.sodium$setRenderState(instance, modelViewMatrix, sortTranslucentSections, original, fogColor);
    }

    @Unique
    private ChunkSectionsToRender sodium$setRenderState(LevelRenderer instance,
                                                        Matrix4fc modelViewMatrix,
                                                        boolean sortTranslucentSections,
                                                        Operation<ChunkSectionsToRender> original,
                                                        Vector4f fogColor) {

        var projectionMatrix = ((GameRendererStorage) Minecraft.getInstance().gameRenderer)
                .sodium$getProjectionMatrix();

        this.matrices = new ChunkRenderMatrices(projectionMatrix, modelViewMatrix);
        var pos = this.levelRenderState.cameraRenderState.pos;

        var chunkSectionsToRender = original.call(instance, modelViewMatrix, sortTranslucentSections);
        ((SodiumChunkSection) (Object) chunkSectionsToRender)
                .sodium$setRendering(this.renderer,
                        this.matrices,
                        pos.x,
                        pos.y,
                        pos.z);

        // update the fog color here with the actual fog color being used to render the sky, since the fog color that
        // SodiumWorldRenderer still has stored from FogRendererMixin is outdated.
        this.renderer.updateFogColor(fogColor);
        return chunkSectionsToRender;
    }

    @ModifyExpressionValue(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;useImprovedTransparency()Z"))
    private boolean sodium$useClassicTransparency(boolean improvedTransparency) {
        // Sodium's terrain pipeline currently implements the classic alpha-blended pass, not vanilla's OIT stages.
        return false;
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean isSectionCompiledAndVisible(BlockPos pos) {
        return this.renderer.isSectionReady(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    @Inject(method = "invalidateCompiledGeometry", at = @At("HEAD"), cancellable = true)
    private void sodium$replace(ClientLevel level,
                                Options options,
                                Camera camera,
                                BlockColors blockColors,
                                CallbackInfo ci) {
        ci.cancel();

        this.cloudRenderer.markForRebuild();
        LeavesBlock.setCutoutLeaves(options.cutoutLeaves().get());

        this.renderer.reload();

        this.sectionRenderDispatcher = new IgnoringSectionRenderDispatcher(Util.backgroundExecutor(),
                this.renderBuffers,
                null,
                this.sectionOcclusionGraph::schedulePropagationFrom);
        this.viewArea = new IgnoringViewArea(this.sectionRenderDispatcher);
        this.sectionOcclusionGraph .waitAndReset(this.viewArea);

        this.clearVisibleSections();
    }

    /**
     * @reason Allow control of the texture filtering mode
     * @author pajic
     */
    @Redirect(
            method = "lambda$addMainPass$0",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/mojang/renderpearl/api/textures/FilterMode;LINEAR:Lcom/mojang/renderpearl/api/textures/FilterMode;",
                    opcode = Opcodes.GETSTATIC))
    private FilterMode setFilterMode() {
        return SodiumClientMod.options().quality.pixelFilteringMode;
    }

    @Override
    public void sodium$setMatrices(ChunkRenderMatrices matrices) {
        this.matrices = matrices;
    }

    @Override
    public ChunkRenderMatrices sodium$getMatrices() {
        return this.matrices;
    }
}
