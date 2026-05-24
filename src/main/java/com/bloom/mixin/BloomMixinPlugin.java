package com.bloom.mixin;

import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class BloomMixinPlugin implements IMixinConfigPlugin {
    private static boolean embeddiumLoaded;
    private static boolean checkedEmbeddium;

    private static boolean isEmbeddiumLoaded() {
        if (!checkedEmbeddium) {
            try {
                embeddiumLoaded = ModList.get().isLoaded("embeddium")
                               || ModList.get().isLoaded("rubidium");
            } catch (Exception e) {
                embeddiumLoaded = false;
            }
            checkedEmbeddium = true;
        }
        return embeddiumLoaded;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".embeddium.")) return isEmbeddiumLoaded();
        return true;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String t, ClassNode c, String m, IMixinInfo i) {}
    @Override public void postApply(String t, ClassNode c, String m, IMixinInfo i) {}
}
