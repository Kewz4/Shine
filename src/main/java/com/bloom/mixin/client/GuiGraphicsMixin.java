package com.bloom.mixin.client;

import com.bloom.client.render.BloomPostProcessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores single-color draw buffer before HUD rendering so UI elements don't
 * accidentally write into the bloom MRT attachment.
 */
@Mixin(Gui.class)
public abstract class GuiGraphicsMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At("HEAD"))
    private void shine$restoreDrawBuffersPreHud(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (BloomPostProcessor.isCaptureEnabled()) {
            GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        }
    }
}
