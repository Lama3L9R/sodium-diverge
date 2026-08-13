package net.caffeinemc.mods.sodium.client.gpu.device.context;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.caffeinemc.mods.sodium.client.gpu.device.backend.DrawBackend;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public abstract class DrawContext {
    protected RenderPass pass;

    public static final int PUSH_CONSTANT_RANGE = 32;

    public static DrawContext create() {
        if (DrawBackend.BACKEND == DrawBackend.OPENGL) {
            return new GLDrawContext();
        } else if (DrawBackend.BACKEND == DrawBackend.VK_MULTIDRAW) {
            return new VKMultiDrawContext();
        } else if (DrawBackend.BACKEND == DrawBackend.VK_INDIRECT) {
            return new VKIndirectContext();
        }

        throw new IllegalStateException("Unknown backend");
    }

    protected static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }

    public RenderPass getPass() {
        return this.pass;
    }

    public void updateData(RenderRegion region, CameraTransform camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.intX, camera.fracX);
        float y = getCameraTranslation(region.getOriginY(), camera.intY, camera.fracY);
        float z = getCameraTranslation(region.getOriginZ(), camera.intZ, camera.fracZ);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer constants = stack.calloc(PUSH_CONSTANT_RANGE);
            constants.putFloat(0, x);
            constants.putFloat(4, y);
            constants.putFloat(8, z);
            constants.putInt(16, Math.toIntExact(System.currentTimeMillis() - region.getCreationTime()));
            constants.putInt(20, region.getId());

            this.pass.pushConstants(constants);
        }
    }

    public void setContext(RenderPass pass, RenderPipeline pipeline) {
        this.pass = pass;
    }

    public abstract void rotate();

    public abstract void delete();

    public abstract void endDraw();
}
