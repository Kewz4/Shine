package com.bloom.client.rendertarget;

import com.bloom.core.IMainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class MRTTarget extends RenderTarget {
    private final IMainTarget screenTarget;

    public MRTTarget(IMainTarget screenTarget) {
        super(false); // no depth
        this.screenTarget = screenTarget;
    }

    @Override
    public int getColorTextureId() {
        return screenTarget.shine$getColorBloomTextureId();
    }

    @Override
    public void resize(int width, int height, boolean onMainThread) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void destroyBuffers() {}

    @Override
    public void createBuffers(int width, int height, boolean onMainThread) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void checkStatus() {}

    @Override
    public void bindRead() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, getColorTextureId());
    }

    @Override
    public void unbindRead() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    @Override
    public void bindWrite(boolean setViewport) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
}
