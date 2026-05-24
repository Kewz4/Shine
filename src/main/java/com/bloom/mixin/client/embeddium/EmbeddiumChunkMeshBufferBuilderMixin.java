package com.bloom.mixin.client.embeddium;

import com.bloom.client.selection.BloomSelectionState;
import com.bloom.client.selection.BloomSourceEncoding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Encodes bloom source strength into the Embeddium material bits before the
 * vertex is pushed to the chunk mesh buffer. This data later drives the bloom
 * intensity written to the MRT bloom attachment via the injected shader output.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder", remap = false)
public abstract class EmbeddiumChunkMeshBufferBuilderMixin {

    @ModifyVariable(
        method = "push",
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private int bloom$encodeMaterialBits(int materialBits) {
        double strength = Math.max(
            BloomSelectionState.getBlockStrength(),
            BloomSelectionState.getFluidStrength()
        );
        return BloomSourceEncoding.encodeMaterialBits(materialBits, strength);
    }
}
