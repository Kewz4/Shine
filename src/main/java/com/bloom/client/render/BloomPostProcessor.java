package com.bloom.client.render;

import com.bloom.BloomMod;
import com.bloom.client.compat.IrisCompat;
import com.bloom.client.config.BloomConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;

public final class BloomPostProcessor {

    private static boolean irisDisabled;
    private static boolean warnedLoadFail;
    private static boolean captureEnabledThisFrame;

    private static PostPass extractPass;        // sceneCopy → pyramid[0]
    private static PostPass[] downPasses;       // pyramid[i] → pyramid[i+1]
    private static PostPass compositePass;      // sceneCopy → composite (reads pyramid levels)

    private static int builtW = -1, builtH = -1, builtLevels = -1;

    private BloomPostProcessor() {}

    public static boolean toggleFromKeybind() {
        BloomConfig.Data cfg = BloomConfig.get();
        cfg.enabled = !cfg.enabled;
        BloomConfig.save();
        return cfg.enabled;
    }

    public static void onConfigSaved() {
        warnedLoadFail = false;
        invalidatePasses();
        BloomTargetManager.invalidate();
    }

    public static void prepareSourceIfEnabled(RenderLevelStageEvent event) {
        captureEnabledThisFrame = false;
        BloomConfig.Data cfg = BloomConfig.get();
        if (!cfg.enabled || checkIris()) return;
        if (Minecraft.getInstance().level == null) return;
        captureEnabledThisFrame = true;
    }

    public static void renderIfEnabled(RenderLevelStageEvent event) {
        BloomConfig.Data cfg = BloomConfig.get();
        if (!cfg.enabled || !captureEnabledThisFrame || checkIris()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || cfg.strength <= 1e-4) return;

        RenderTarget main = mc.getMainRenderTarget();
        int w = main.width, h = main.height;
        int levels = computeLevels(cfg);
        if (!BloomTargetManager.ensureTargets(w, h)) return;
        if (!ensurePasses(w, h, levels, mc.getResourceManager())) return;

        // Step 1: copy main → sceneCopy
        applySceneCopy(main);
        // Step 2: extract bright pixels from sceneCopy → pyramid[0]
        applyExtract(cfg, main, levels);
        // Step 3: downsample pyramid
        applyDownsample(levels);
        // Step 4: composite all levels back → composite target, then blit to main
        applyComposite(cfg, levels, main, w, h);
    }

    // ─── Iris ───────────────────────────────────────────────────────────────

    private static boolean checkIris() {
        boolean skip = IrisCompat.shouldDisableBloom();
        if (skip && !irisDisabled) {
            BloomMod.LOGGER.info(IrisCompat.disableMessage());
            irisDisabled = true;
            captureEnabledThisFrame = false;
            invalidatePasses();
        } else if (!skip && irisDisabled) {
            BloomMod.LOGGER.info("Shine bloom re-enabled.");
            irisDisabled = false;
        }
        return skip;
    }

    // ─── Pass management ────────────────────────────────────────────────────

    private static boolean ensurePasses(int w, int h, int levels, ResourceManager rm) {
        if (builtW == w && builtH == h && builtLevels == levels && extractPass != null) return true;
        invalidatePasses();
        try {
            TextureTarget sceneCopy  = BloomTargetManager.getSceneCopy();
            TextureTarget composite  = BloomTargetManager.getComposite();
            TextureTarget lvl0       = BloomTargetManager.getPyramidLevel(0);

            // Extract: sceneCopy → pyramid[0]
            extractPass = new PostPass(rm, "shine_bloom_extract", sceneCopy, lvl0);

            // Downsample: pyramid[i] → pyramid[i+1]
            downPasses = new PostPass[Math.max(0, levels - 1)];
            for (int i = 0; i < levels - 1; i++) {
                downPasses[i] = new PostPass(rm, "shine_bloom_downsample",
                        BloomTargetManager.getPyramidLevel(i),
                        BloomTargetManager.getPyramidLevel(i + 1));
            }

            // Composite: sceneCopy → composite target
            compositePass = new PostPass(rm, "shine_bloom_composite", sceneCopy, composite);

            builtW = w; builtH = h; builtLevels = levels;
            warnedLoadFail = false;
            return true;
        } catch (IOException e) {
            if (!warnedLoadFail) { BloomMod.LOGGER.warn("Shine bloom shader load failed.", e); warnedLoadFail = true; }
            invalidatePasses();
            return false;
        }
    }

    private static void invalidatePasses() {
        close(extractPass); extractPass = null;
        if (downPasses != null) { for (PostPass p : downPasses) close(p); downPasses = null; }
        close(compositePass); compositePass = null;
        builtW = builtH = builtLevels = -1;
    }

    private static void close(AutoCloseable c) { if (c != null) try { c.close(); } catch (Exception ignored) {} }

    // ─── Render steps ───────────────────────────────────────────────────────

    private static void applySceneCopy(RenderTarget main) {
        // Blit main color → sceneCopy using GL framebuffer blit
        RenderSystem.assertOnRenderThread();
        TextureTarget dst = BloomTargetManager.getSceneCopy();
        blitFramebuffer(main, dst);
    }

    private static void blitFramebuffer(RenderTarget src, RenderTarget dst) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, src.width, src.height, 0, 0, dst.width, dst.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private static void applyExtract(BloomConfig.Data cfg, RenderTarget main, int levels) {
        EffectInstance e = extractPass.getEffect();
        u2(e, "OutSize", BloomTargetManager.getPyramidWidth(0), BloomTargetManager.getPyramidHeight(0));
        u1(e, "Threshold",           (float) cfg.threshold);
        u1(e, "HighlightClamp",      (float) cfg.highlightClamp);
        u1(e, "SoftKnee",            (float) cfg.softKnee);
        u1(e, "MaxDistance",         (float) cfg.bloomDistance);
        u1(e, "NearPlane",           0.05f);
        u1(e, "FarPlane",            Minecraft.getInstance().gameRenderer.getDepthFar());
        u1(e, "SourceStrengthScale", 5.0f);
        u1(e, "DistanceFadeRange",   2.0f);
        e.setSampler("DepthSampler", main::getDepthTextureId);
        e.markDirty();
        extractPass.process(0.0f);
    }

    private static void applyDownsample(int levels) {
        for (int i = 0; i < levels - 1; i++) {
            EffectInstance e = downPasses[i].getEffect();
            u2(e, "OutSize", BloomTargetManager.getPyramidWidth(i + 1), BloomTargetManager.getPyramidHeight(i + 1));
            u2(e, "InSize",  BloomTargetManager.getPyramidWidth(i),     BloomTargetManager.getPyramidHeight(i));
            e.markDirty();
            downPasses[i].process(0.0f);
        }
    }

    private static void applyComposite(BloomConfig.Data cfg, int levels, RenderTarget main, int w, int h) {
        EffectInstance e = compositePass.getEffect();
        u2(e, "OutSize", w, h);
        u1(e, "Strength", (float) cfg.strength);
        float[] weights = computeWeights(cfg, levels);
        for (int i = 0; i < BloomTargetManager.MAX_LEVELS; i++) {
            u1(e, "Weight" + i, i < levels ? weights[i] : 0f);
            int li = i;
            e.setSampler("BloomLevel" + i, () -> {
                TextureTarget t = BloomTargetManager.getPyramidLevel(Math.min(li, levels - 1));
                return t != null ? t.getColorTextureId() : 0;
            });
        }
        e.markDirty();
        compositePass.process(0.0f);
        // Blit composite result → main framebuffer
        blitFramebuffer(BloomTargetManager.getComposite(), main);
        // Restore main framebuffer binding
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, main.frameBufferId);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private static int computeLevels(BloomConfig.Data cfg) {
        return Math.max(1, Math.min(BloomTargetManager.MAX_LEVELS, cfg.blurPassCount + 1));
    }

    private static float[] computeWeights(BloomConfig.Data cfg, int levels) {
        float[] w = new float[BloomTargetManager.MAX_LEVELS];
        float[] base = {1f, 0.8f, 0.6f, 0.4f, 0.2f, 0.1f};
        float total = 0;
        for (int i = 0; i < levels; i++) { w[i] = base[Math.min(i, base.length - 1)]; total += w[i]; }
        if (total > 1e-5f) for (int i = 0; i < levels; i++) w[i] /= total;
        return w;
    }

    private static void u1(EffectInstance e, String n, float v) { var u = e.getUniform(n); if (u != null) u.set(v); }
    private static void u2(EffectInstance e, String n, float x, float y) { var u = e.getUniform(n); if (u != null) u.set(x, y); }
    private static void u2(EffectInstance e, String n, int x, int y) { u2(e, n, (float) x, (float) y); }

    public static boolean isCaptureEnabled() { return captureEnabledThisFrame; }
}
