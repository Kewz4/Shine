package com.bloom.client.selection;

import com.bloom.client.config.BloomConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Map;
import java.util.Set;

public final class BloomSelection {
    private static final Set<String> SELECTED_FLUID_IDS = Set.of(
        "minecraft:lava",
        "minecraft:flowing_lava"
    );

    private BloomSelection() {}

    public static double getBlockSourceStrength(BlockState state) {
        BloomConfig.Data cfg = BloomConfig.get();
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        Map<String, Double> overrides = cfg.effectiveOverrides();
        Double ov = overrides.get(key.toString());
        if (ov != null) return clamp(ov);
        double fallback = state.getLightEmission() > 0 ? cfg.defaultLightSourceStrength : cfg.defaultNonLightStrength;
        return clamp(fallback);
    }

    public static double getFluidSourceStrength(FluidState state) {
        if (state.isEmpty()) return 0.0;
        BloomConfig.Data cfg = BloomConfig.get();
        Map<String, Double> overrides = cfg.effectiveOverrides();
        ResourceLocation fluidKey = BuiltInRegistries.FLUID.getKey(state.getType());
        Double ov = overrides.get(fluidKey.toString());
        if (ov != null) return clamp(ov);
        Block legacyBlock = state.createLegacyBlock().getBlock();
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(legacyBlock);
        if (blockKey != null) {
            Double bOv = overrides.get(blockKey.toString());
            if (bOv != null) return clamp(bOv);
        }
        double fallback = SELECTED_FLUID_IDS.contains(fluidKey.toString())
            ? cfg.defaultLightSourceStrength : cfg.defaultNonLightStrength;
        return clamp(fallback);
    }

    private static double clamp(double v) {
        return Math.max(BloomConfig.MIN_SOURCE_STRENGTH, Math.min(BloomConfig.MAX_SOURCE_STRENGTH, v));
    }
}
