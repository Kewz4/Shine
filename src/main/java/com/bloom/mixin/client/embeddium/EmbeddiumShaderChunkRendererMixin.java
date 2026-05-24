package com.bloom.mixin.client.embeddium;

import com.bloom.client.render.BloomPostProcessor;
import com.bloom.core.IMainTarget;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enables MRT rendering (bloom attachment) before Embeddium draws opaque chunks
 * and restores single-output mode afterwards.
 *
 * The method `begin` takes a TerrainRenderPass and optional extra parameters
 * (Fog, sampler) — all injectors use require=0 so version mismatches silently skip.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer", remap = false)
public abstract class EmbeddiumShaderChunkRendererMixin {

    private static final int[] BLOOM_DRAW_BUFFERS = {GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1};

    @Inject(method = "begin", at = @At("HEAD"), require = 0)
    private void shine$enableBloomMRT(Object pass, CallbackInfo ci) {
        if (!BloomPostProcessor.isCaptureEnabled()) return;
        if (!isTranslucent(pass)) {
            if (!(Minecraft.getInstance().getMainRenderTarget() instanceof IMainTarget t)) return;
            if (t.shine$getColorBloomTextureId() < 0) return;
            GL20.glDrawBuffers(BLOOM_DRAW_BUFFERS);
        }
    }

    @Inject(method = "end", at = @At("RETURN"), require = 0)
    private void shine$disableBloomMRT(Object pass, CallbackInfo ci) {
        if (!isTranslucent(pass)) {
            GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        }
    }

    private static boolean isTranslucent(Object pass) {
        if (pass == null) return false;
        try {
            return (boolean) pass.getClass().getMethod("isTranslucent").invoke(pass);
        } catch (Exception e) {
            return false;
        }
    }
}
