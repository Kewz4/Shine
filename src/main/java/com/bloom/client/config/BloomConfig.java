package com.bloom.client.config;

import com.bloom.BloomMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BloomConfig {
    public static final int MAX_BLUR_PASSES = 6;
    public static final double MAX_STRENGTH = 20.0;
    public static final double MAX_RADIUS = 500.0;
    public static final double MIN_BLOOM_DISTANCE = 1.0;
    public static final double MAX_BLOOM_DISTANCE = 256.0;
    public static final double MIN_HIGHLIGHT_CLAMP = 0.01;
    public static final double MAX_HIGHLIGHT_CLAMP = 4.0;
    public static final double MIN_SOFT_KNEE = 0.01;
    public static final double MAX_SOFT_KNEE = 1.0;
    public static final double MIN_SOURCE_STRENGTH = 0.0;
    public static final double MAX_SOURCE_STRENGTH = 500.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path CONFIG_PATH;
    private static Path LEGACY_CONFIG_PATH;

    private static Data data = Data.defaults();

    private BloomConfig() {}

    private static Path configPath() {
        if (CONFIG_PATH == null) {
            CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("shine.json");
            LEGACY_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("bloom.json");
        }
        return CONFIG_PATH;
    }

    public static Data get() { return data; }
    public static Data copy() { return data.copy(); }
    public static Data defaults() { return Data.defaults(); }

    public static void set(Data newData) {
        data = sanitize(newData);
    }

    public static void load() {
        configPath();
        Path toLoad = Files.exists(CONFIG_PATH) ? CONFIG_PATH
                    : (Files.exists(LEGACY_CONFIG_PATH) ? LEGACY_CONFIG_PATH : null);
        if (toLoad == null) {
            data = Data.defaults();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(toLoad)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            data = sanitize(loaded);
            if (!toLoad.equals(CONFIG_PATH)) {
                save();
                BloomMod.LOGGER.info("Migrated bloom.json → shine.json");
            }
        } catch (Exception e) {
            BloomMod.LOGGER.error("Failed to read Shine config, using defaults.", e);
            data = Data.defaults();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(configPath().getParent());
            try (Writer w = Files.newBufferedWriter(configPath())) {
                GSON.toJson(data, w);
            }
        } catch (IOException e) {
            BloomMod.LOGGER.error("Failed to write Shine config.", e);
        }
    }

    private static Data sanitize(Data in) {
        Data s = in == null ? Data.defaults() : in.copy();
        s.threshold = clamp(s.threshold, 0.0, 1.0);
        s.strength = clamp(s.strength, 0.0, MAX_STRENGTH);
        s.radius = clamp(s.radius, 0.0, MAX_RADIUS);
        s.blurPassCount = (int) clamp(s.blurPassCount, 1, MAX_BLUR_PASSES);
        if (s.bloomDistance <= 0.0) s.bloomDistance = Data.defaults().bloomDistance;
        s.bloomDistance = clamp(s.bloomDistance, MIN_BLOOM_DISTANCE, MAX_BLOOM_DISTANCE);
        if (s.highlightClamp <= 0.0) s.highlightClamp = Data.defaults().highlightClamp;
        s.highlightClamp = clamp(s.highlightClamp, MIN_HIGHLIGHT_CLAMP, MAX_HIGHLIGHT_CLAMP);
        if (s.softKnee <= 0.0) s.softKnee = Data.defaults().softKnee;
        s.softKnee = clamp(s.softKnee, MIN_SOFT_KNEE, MAX_SOFT_KNEE);
        s.defaultLightSourceStrength = clamp(s.defaultLightSourceStrength, MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH);
        s.defaultNonLightStrength = clamp(s.defaultNonLightStrength, MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH);
        Map<String, Double> safeOverrides = new LinkedHashMap<>();
        if (s.sourceStrengthOverrides != null) {
            for (Map.Entry<String, Double> e : s.sourceStrengthOverrides.entrySet()) {
                if (e == null || e.getKey() == null || e.getValue() == null) continue;
                safeOverrides.put(e.getKey(), clamp(e.getValue(), MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH));
            }
        }
        for (Map.Entry<String, Double> baseline : Data.defaultBlockStrengthOverrides().entrySet()) {
            safeOverrides.putIfAbsent(baseline.getKey(), clamp(baseline.getValue(), MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH));
        }
        s.sourceStrengthOverrides = safeOverrides;
        return s;
    }

    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private static long clamp(long v, long min, long max) { return Math.max(min, Math.min(max, v)); }

    public static final class Data {
        public boolean enabled = true;
        public double strength = 8.0;
        public double threshold = 0.15;
        public double radius = 400.0;
        public int blurPassCount = 2;
        public double bloomDistance = 75.0;
        public double highlightClamp = 0.28;
        public double softKnee = 0.2;
        public double defaultLightSourceStrength = 50.0;
        public double defaultNonLightStrength = 0.0;
        /** Legacy field name kept for config compatibility. */
        public Map<String, Double> blockStrengthOverrides = null;
        public Map<String, Double> sourceStrengthOverrides = defaultBlockStrengthOverrides();

        public static Data defaults() { return new Data(); }

        public static LinkedHashMap<String, Double> defaultBlockStrengthOverrides() {
            LinkedHashMap<String, Double> d = new LinkedHashMap<>();
            d.put("minecraft:water", 0.0);
            d.put("minecraft:flowing_water", 0.0);
            d.put("minecraft:sculk", 500.0);
            d.put("minecraft:sculk_vein", 150.0);
            d.put("minecraft:amethyst_cluster", 100.0);
            d.put("minecraft:large_amethyst_bud", 100.0);
            d.put("minecraft:medium_amethyst_bud", 100.0);
            d.put("minecraft:small_amethyst_bud", 100.0);
            d.put("minecraft:glow_lichen", 400.0);
            d.put("minecraft:warped_stem", 100.0);
            d.put("minecraft:warped_fungus", 300.0);
            d.put("minecraft:nether_portal", 75.0);
            d.put("minecraft:crimson_stem", 175.0);
            d.put("minecraft:twisting_vines", 25.0);
            d.put("minecraft:twisting_vines_plant", 25.0);
            d.put("minecraft:weeping_vines", 75.0);
            d.put("minecraft:weeping_vines_plant", 100.0);
            d.put("minecraft:lava", 75.0);
            d.put("minecraft:flowing_lava", 75.0);
            d.put("minecraft:crimson_fungus", 150.0);
            d.put("minecraft:nether_wart", 50.0);
            d.put("minecraft:crying_obsidian", 75.0);
            d.put("minecraft:torch", 499.0);
            d.put("minecraft:wall_torch", 499.0);
            d.put("minecraft:soul_torch", 220.0);
            d.put("minecraft:soul_wall_torch", 220.0);
            d.put("minecraft:redstone_torch", 120.0);
            d.put("minecraft:redstone_wall_torch", 120.0);
            d.put("minecraft:lantern", 200.0);
            d.put("minecraft:soul_lantern", 180.0);
            d.put("minecraft:soul_fire", 250.0);
            d.put("minecraft:powder_snow", 25.0);
            d.put("minecraft:snow", 25.0);
            d.put("minecraft:snow_block", 25.0);
            return d;
        }

        public Map<String, Double> effectiveOverrides() {
            // Support legacy config field name
            return sourceStrengthOverrides != null ? sourceStrengthOverrides
                 : blockStrengthOverrides != null ? blockStrengthOverrides
                 : defaultBlockStrengthOverrides();
        }

        public Data copy() {
            Data c = new Data();
            c.enabled = enabled;
            c.strength = strength;
            c.threshold = threshold;
            c.radius = radius;
            c.blurPassCount = blurPassCount;
            c.bloomDistance = bloomDistance;
            c.highlightClamp = highlightClamp;
            c.softKnee = softKnee;
            c.defaultLightSourceStrength = defaultLightSourceStrength;
            c.defaultNonLightStrength = defaultNonLightStrength;
            c.sourceStrengthOverrides = sourceStrengthOverrides == null ? null : new LinkedHashMap<>(sourceStrengthOverrides);
            return c;
        }
    }
}
