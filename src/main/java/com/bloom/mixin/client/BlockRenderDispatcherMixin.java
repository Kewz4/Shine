package com.bloom.mixin.client;

import com.bloom.client.selection.BloomSelection;
import com.bloom.client.selection.BloomSelectionState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pushes bloom source strength state before each block/fluid render call in the chunk compiler.
 * Replaces the 1.21.x SectionCompilerMixin. Targets BlockRenderDispatcher which is called
 * directly by SectionCompiler in 1.20.1.
 */
@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherMixin {

    @Unique private double shine$prevBlockStrength = 0.0;
    @Unique private double shine$prevFluidStrength = 0.0;

    @Inject(
        method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;)V",
        at = @At("HEAD")
    )
    private void shine$pushBlockStrength(
        BlockState state, BlockPos pos, BlockAndTintGetter level,
        PoseStack stack, VertexConsumer consumer, boolean checkSides,
        RandomSource random, CallbackInfo ci
    ) {
        shine$prevBlockStrength = BloomSelectionState.pushBlockStrength(
            BloomSelection.getBlockSourceStrength(state));
    }

    @Inject(
        method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;)V",
        at = @At("RETURN")
    )
    private void shine$popBlockStrength(
        BlockState state, BlockPos pos, BlockAndTintGetter level,
        PoseStack stack, VertexConsumer consumer, boolean checkSides,
        RandomSource random, CallbackInfo ci
    ) {
        BloomSelectionState.popBlockStrength(shine$prevBlockStrength);
    }

    @Inject(
        method = "renderLiquid(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V",
        at = @At("HEAD")
    )
    private void shine$pushFluidStrength(
        BlockPos pos, BlockAndTintGetter level, VertexConsumer consumer,
        BlockState blockState, FluidState fluidState, CallbackInfo ci
    ) {
        shine$prevFluidStrength = BloomSelectionState.pushFluidStrength(
            BloomSelection.getFluidSourceStrength(fluidState));
    }

    @Inject(
        method = "renderLiquid(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V",
        at = @At("RETURN")
    )
    private void shine$popFluidStrength(
        BlockPos pos, BlockAndTintGetter level, VertexConsumer consumer,
        BlockState blockState, FluidState fluidState, CallbackInfo ci
    ) {
        BloomSelectionState.popFluidStrength(shine$prevFluidStrength);
    }
}
