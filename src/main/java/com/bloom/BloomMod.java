package com.bloom;

import com.bloom.client.config.BloomConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BloomMod.MOD_ID)
public class BloomMod {
    public static final String MOD_ID = "shine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public BloomMod() {
        BloomConfig.load();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup));
    }

    private void setup(FMLCommonSetupEvent event) {
    }

    private void clientSetup(FMLClientSetupEvent event) {
        com.bloom.client.BloomClient.init();
    }
}
