package com.bloom.client.gui;

import com.bloom.client.config.BloomConfig;
import com.bloom.client.render.BloomPostProcessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class BloomConfigScreen extends Screen {

    private final Screen parent;
    private final BloomConfig.Data working;
    private Button enabledBtn;

    public BloomConfigScreen(Screen parent) {
        super(Component.literal("Shine Bloom Settings"));
        this.parent = parent;
        this.working = BloomConfig.copy();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y  = 40;
        int w  = 300;
        int h  = 20;
        int gap = 24;

        enabledBtn = addRenderableWidget(Button.builder(enabledLabel(),
                btn -> { working.enabled = !working.enabled; btn.setMessage(enabledLabel()); })
                .bounds(cx - w / 2, y, w, h).build());
        y += gap;

        addSlider(cx, y, w, h, "Strength: %.1f",
                working.strength, 0.0, BloomConfig.MAX_STRENGTH,
                v -> working.strength = v);
        y += gap;

        addSlider(cx, y, w, h, "Threshold: %.2f",
                working.threshold, 0.0, 1.0,
                v -> working.threshold = v);
        y += gap;

        addSlider(cx, y, w, h, "Soft Knee: %.2f",
                working.softKnee, BloomConfig.MIN_SOFT_KNEE, BloomConfig.MAX_SOFT_KNEE,
                v -> working.softKnee = v);
        y += gap;

        addSlider(cx, y, w, h, "Highlight Clamp: %.2f",
                working.highlightClamp, BloomConfig.MIN_HIGHLIGHT_CLAMP, BloomConfig.MAX_HIGHLIGHT_CLAMP,
                v -> working.highlightClamp = v);
        y += gap;

        addSlider(cx, y, w, h, "Blur Passes: %.0f",
                (double) working.blurPassCount, 1.0, BloomConfig.MAX_BLUR_PASSES,
                v -> working.blurPassCount = (int) Math.round(v));
        y += gap;

        addSlider(cx, y, w, h, "Bloom Distance: %.0f",
                working.bloomDistance, BloomConfig.MIN_BLOOM_DISTANCE, BloomConfig.MAX_BLOOM_DISTANCE,
                v -> working.bloomDistance = v);
        y += gap + 4;

        int half = w / 2 - 2;
        addRenderableWidget(Button.builder(Component.literal("Done"),   btn -> save())
                .bounds(cx - w / 2, y, half, h).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> onClose())
                .bounds(cx + 2,     y, half, h).build());
    }

    private Component enabledLabel() {
        return Component.literal("Bloom: " + (working.enabled ? "§aON" : "§cOFF"));
    }

    @FunctionalInterface
    private interface DoubleSetter { void set(double v); }

    private void addSlider(int cx, int y, int w, int h,
                           String fmt, double current, double min, double max,
                           DoubleSetter setter) {
        double norm = (max > min) ? Mth.clamp((current - min) / (max - min), 0.0, 1.0) : 0.0;
        addRenderableWidget(new AbstractSliderButton(cx - w / 2, y, w, h, Component.empty(), norm) {
            {
                // instance initializer: set initial label after value is set by super()
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                double v = min + this.value * (max - min);
                setMessage(Component.literal(String.format(fmt, v)));
            }

            @Override
            protected void applyValue() {
                setter.set(min + this.value * (max - min));
            }
        });
    }

    private void save() {
        BloomConfig.set(working);
        BloomConfig.save();
        BloomPostProcessor.onConfigSaved();
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
