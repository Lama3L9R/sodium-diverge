package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.util.FrameTimeStatistics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayInsertMixin {
    @ModifyArg(
            method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;ZI)V",
                    ordinal = 0),
            index = 1
    )
    private List<String> sodium$insertFpsPercentiles(List<String> leftLines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.debugEntries.isCurrentlyEnabled(SodiumClientMod.SODIUM_FPS_PERCENTILES)) {
            return leftLines;
        }
        var results = FrameTimeStatistics.INSTANCE.get();
        if (results == null || results.isEmpty()) {
            return leftLines;
        }

        // splice the percentile fps display into the debug lines to make sure it's right under the fps string.
        // without this, it may be put somewhere else on the screen.
        int insertAt = 0;
        for (int i = 0; i < leftLines.size(); i++) {
            String line = leftLines.get(i);
            if (line != null && line.contains(" fps T:")) {
                insertAt = i + 1;
                break;
            }
        }

        var sb = new StringBuilder();
        for (var entry : results.reference2LongEntrySet()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            long ns = entry.getLongValue();
            sb.append(ChatFormatting.GRAY)
                    .append(entry.getKey().name()).append('=')
                    .append(ChatFormatting.RESET)
                    .append(sodium$nanosToFps(ns));
        }

        sb.append(ChatFormatting.GRAY).append(" fps");

        leftLines.add(insertAt, sb.toString());
        return leftLines;
    }

    @Unique
    private static long sodium$nanosToFps(long ns) {
        return ns > 0L ? Math.round(1.0e9 / ns) : 0L;
    }
}
