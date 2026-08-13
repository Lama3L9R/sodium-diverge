package net.caffeinemc.mods.sodium.mixin.features.gui;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.Options.class)
public interface OptionsAccessor {
    @Accessor("exclusiveFullscreen")
    OptionInstance<Boolean> sodium$exclusiveFullscreen();
}
