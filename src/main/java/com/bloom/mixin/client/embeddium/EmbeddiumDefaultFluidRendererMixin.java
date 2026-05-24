package com.bloom.mixin.client.embeddium;

import com.bloom.client.selection.BloomSelection;
import com.bloom.client.selection.BloomSelectionState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public abstract class EmbeddiumDefaultFluidRendererMixin {

    @Unique private double bloom$prevFluidStrength;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void bloom$push(Object level, BlockState blockState, FluidState fluidState,
                            BlockPos blockPos, BlockPos modelOffset, CallbackInfo ci) {
        bloom$prevFluidStrength = BloomSelectionState.pushFluidStrength(
            BloomSelection.getFluidSourceStrength(fluidState));
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void bloom$pop(Object level, BlockState blockState, FluidState fluidState,
                           BlockPos blockPos, BlockPos modelOffset, CallbackInfo ci) {
        BloomSelectionState.popFluidStrength(bloom$prevFluidStrength);
    }
}
