package net.caffeinemc.mods.sodium.client.util;

import org.lwjgl.sdl.SDLTimer;

public class PreciseTime {
    private static final long PERF_FREQ = SDLTimer.SDL_GetPerformanceFrequency();
    private static final long START_TICK = SDLTimer.SDL_GetPerformanceCounter();

    public static double getTime() {
        return (double) (SDLTimer.SDL_GetPerformanceCounter() - START_TICK) / PERF_FREQ;
    }
}
