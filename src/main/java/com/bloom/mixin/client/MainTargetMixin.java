package com.bloom.mixin.client;

import com.bloom.core.IMainTarget;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extends the main RenderTarget (MainTarget in 1.20.1) with an extra
 * color attachment for capturing per-pixel bloom source data via MRT.
 * Attachment index 1 (GL_COLOR_ATTACHMENT1) holds bloom data.
 *
 * MainTarget uses createFrameBuffer(II) on first creation and
 * createBuffers(IIZ) on resize (via inherited RenderTarget.resize).
 */
@Mixin(MainTarget.class)
public abstract class MainTargetMixin extends RenderTarget implements IMainTarget {

    @Unique private int shine$bloomTextureId = -1;

    protected MainTargetMixin(boolean useDepth) {
        super(useDepth);
    }

    @Override
    public int shine$getColorBloomTextureId() {
        return shine$bloomTextureId;
    }

    @Override
    public void shine$clearBloomTexture() {
        if (shine$bloomTextureId < 0) return;
        int fbo = this.frameBufferId;
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL20.glDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void shine$destroyBloomTexture() {
        if (shine$bloomTextureId >= 0) {
            GL11.glDeleteTextures(shine$bloomTextureId);
            shine$bloomTextureId = -1;
        }
    }

    // Called on initial creation (MainTarget uses its own createFrameBuffer path)
    // Use the method parameters directly — avoids accessing this.width/this.height
    // whose field names would need separate @Shadow declarations to be refmap-remapped.
    @Inject(method = "createFrameBuffer(II)V", at = @At("RETURN"))
    private void shine$attachBloomOnFirstCreate(int width, int height, CallbackInfo ci) {
        shine$attachBloomTexture(width, height);
    }

    @Override
    public void shine$attachBloomTexture(int width, int height) {
        shine$destroyBloomTexture();
        int tex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1,
                GL11.GL_TEXTURE_2D, tex, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        shine$bloomTextureId = tex;
    }
}
