package com.bloom.mixin.client.embeddium;

import com.bloom.client.selection.BloomSelection;
import com.bloom.client.selection.BloomSelectionState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class EmbeddiumBlockRendererMixin {

    @Unique private double bloom$prevBlockStrength;
    @Unique private double bloom$prevFluidStrength;

    @Inject(method = "renderModel", at = @At("HEAD"), require = 0)
    private void bloom$push(BakedModel model, BlockState state, BlockPos pos, BlockPos offset, CallbackInfo ci) {
        bloom$prevBlockStrength = BloomSelectionState.pushBlockStrength(BloomSelection.getBlockSourceStrength(state));
        bloom$prevFluidStrength = BloomSelectionState.pushFluidStrength(
            BloomSelection.getFluidSourceStrength(state.getFluidState()));
    }

    @Inject(method = "renderModel", at = @At("RETURN"), require = 0)
    private void bloom$pop(BakedModel model, BlockState state, BlockPos pos, BlockPos offset, CallbackInfo ci) {
        BloomSelectionState.popFluidStrength(bloom$prevFluidStrength);
        BloomSelectionState.popBlockStrength(bloom$prevBlockStrength);
    }
}
