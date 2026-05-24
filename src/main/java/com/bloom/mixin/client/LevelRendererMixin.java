package com.bloom.mixin.client;

import com.bloom.client.render.BloomPostProcessor;
import com.bloom.core.IMainTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enables MRT (draw to bloom attachment) during opaque/cutout terrain rendering
 * so Embeddium's injected shader can write per-pixel bloom data.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    private static final int[] BLOOM_BUFFERS = {GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1};

    @Inject(
        method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
        at = @At("HEAD")
    )
    private void shine$enableBloomBuffers(RenderType type, PoseStack stack, double x, double y, double z,
                                          Matrix4f projMatrix, CallbackInfo ci) {
        if (!BloomPostProcessor.isCaptureEnabled()) return;
        if (!isBloomTerrainType(type)) return;
        if (!(Minecraft.getInstance().getMainRenderTarget() instanceof IMainTarget mainTarget)) return;
        if (mainTarget.shine$getColorBloomTextureId() < 0) return;
        GL20.glDrawBuffers(BLOOM_BUFFERS);
    }

    @Inject(
        method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
        at = @At("RETURN")
    )
    private void shine$disableBloomBuffers(RenderType type, PoseStack stack, double x, double y, double z,
                                           Matrix4f projMatrix, CallbackInfo ci) {
        if (!isBloomTerrainType(type)) return;
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
    }

    private static boolean isBloomTerrainType(RenderType type) {
        return type == RenderType.solid() || type == RenderType.cutout() || type == RenderType.cutoutMipped();
    }
}
