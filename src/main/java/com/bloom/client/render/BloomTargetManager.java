package com.bloom.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

public final class BloomTargetManager {
    public static final int MAX_LEVELS = 6;

    private static TextureTarget sceneCopy;
    private static TextureTarget[] pyramid = new TextureTarget[MAX_LEVELS]; // downsampled levels
    private static TextureTarget composite;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private BloomTargetManager() {}

    public static boolean ensureTargets(int width, int height) {
        if (width == lastWidth && height == lastHeight && sceneCopy != null) return true;
        destroyAll();
        boolean onThread = Minecraft.getInstance().isSameThread();
        try {
            sceneCopy = new TextureTarget(width, height, false, onThread);
            sceneCopy.setClearColor(0, 0, 0, 0);
            composite = new TextureTarget(width, height, false, onThread);
            composite.setClearColor(0, 0, 0, 0);
            for (int i = 0; i < MAX_LEVELS; i++) {
                int w = Math.max(1, width >> (i + 1));
                int h = Math.max(1, height >> (i + 1));
                pyramid[i] = new TextureTarget(w, h, false, onThread);
                pyramid[i].setClearColor(0, 0, 0, 0);
            }
            lastWidth = width;
            lastHeight = height;
            return true;
        } catch (Exception e) {
            destroyAll();
            return false;
        }
    }

    public static TextureTarget getSceneCopy() { return sceneCopy; }
    public static TextureTarget getComposite() { return composite; }
    public static TextureTarget getPyramidLevel(int level) {
        return (level >= 0 && level < MAX_LEVELS) ? pyramid[level] : null;
    }
    public static int getPyramidWidth(int level) { return pyramid[level] != null ? pyramid[level].width : 1; }
    public static int getPyramidHeight(int level) { return pyramid[level] != null ? pyramid[level].height : 1; }

    public static void destroyAll() {
        if (sceneCopy != null) { sceneCopy.destroyBuffers(); sceneCopy = null; }
        if (composite != null) { composite.destroyBuffers(); composite = null; }
        for (int i = 0; i < MAX_LEVELS; i++) {
            if (pyramid[i] != null) { pyramid[i].destroyBuffers(); pyramid[i] = null; }
        }
        lastWidth = -1;
        lastHeight = -1;
    }

    public static void invalidate() {
        lastWidth = -1;
        lastHeight = -1;
    }
}
