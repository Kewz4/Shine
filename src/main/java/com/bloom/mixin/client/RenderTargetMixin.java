package com.bloom.mixin.client;

import com.bloom.core.IMainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into RenderTarget (the actual declaring class) so that Mixin can find
 * createBuffers and destroyBuffers in the class that owns them. An instanceof
 * guard restricts the bloom logic to MainTarget instances only.
 */
@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

    @Inject(method = "createBuffers(IIZ)V", at = @At("TAIL"))
    private void shine$attachBloomOnResize(int width, int height, boolean onThread, CallbackInfo ci) {
        if (this instanceof IMainTarget t) {
            t.shine$attachBloomTexture(width, height);
        }
    }

    @Inject(method = "destroyBuffers()V", at = @At("HEAD"))
    private void shine$destroyBloomOnBufferDestroy(CallbackInfo ci) {
        if (this instanceof IMainTarget t) {
            t.shine$destroyBloomTexture();
        }
    }
}
