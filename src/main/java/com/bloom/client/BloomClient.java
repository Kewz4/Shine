package com.bloom.client;

import com.bloom.BloomMod;
import com.bloom.client.config.BloomConfig;
import com.bloom.client.render.BloomPostProcessor;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;

@Mod.EventBusSubscriber(modid = BloomMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BloomClient {

    public static final KeyMapping TOGGLE_BLOOM_KEY = new KeyMapping(
        "key.shine.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_B,
        "key.categories.shine.main"
    );

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(BloomClient::registerKeys);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_BLOOM_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (TOGGLE_BLOOM_KEY.consumeClick()) {
            boolean enabled = BloomPostProcessor.toggleFromKeybind();
            BloomMod.LOGGER.info("Shine bloom {}", enabled ? "enabled" : "disabled");
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            BloomPostProcessor.prepareSourceIfEnabled(event);
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            BloomPostProcessor.renderIfEnabled(event);
        }
    }
}
