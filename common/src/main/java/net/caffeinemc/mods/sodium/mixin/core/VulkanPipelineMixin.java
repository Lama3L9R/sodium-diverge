package net.caffeinemc.mods.sodium.mixin.core;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.LongBuffer;

@Mixin(VulkanRenderPipeline.class)
public class VulkanPipelineMixin {
    @WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkPipelineLayoutCreateInfo;pSetLayouts(Ljava/nio/LongBuffer;)Lorg/lwjgl/vulkan/VkPipelineLayoutCreateInfo;"))
    private static VkPipelineLayoutCreateInfo sodium$fixPipelineLayout(VkPipelineLayoutCreateInfo instance,
                                                                        LongBuffer value,
                                                                        Operation<VkPipelineLayoutCreateInfo> original,
                                                                        VulkanDevice device,
                                                                        BackendRenderPipeline.CreateInfo createInfo) {
        if (createInfo.name().startsWith("sodium:")) {
            instance.pPushConstantRanges(VkPushConstantRange.calloc(1, MemoryStack.stackGet())
                    .offset(0)
                    .size(DrawContext.PUSH_CONSTANT_RANGE)
                    .stageFlags(VK13.VK_SHADER_STAGE_ALL));
        }
        return original.call(instance, value);
    }
}
