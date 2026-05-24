package com.bloom.client.compat;

import com.bloom.BloomMod;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class IrisCompat {
    private static final boolean IRIS_LOADED = isModLoaded("oculus") || isModLoaded("iris");
    private static final String ACTIVE_MESSAGE = "Shine bloom disabled: shader pack is active.";
    private static final String INSTALLED_MESSAGE = "Shine bloom disabled: Iris/Oculus is installed but shader state could not be verified.";

    private static boolean reflectionInitialized;
    private static boolean reflectionAvailable;
    private static boolean loggedReflectionFailure;
    private static Method irisGetInstanceMethod;
    private static Method irisIsShaderPackInUseMethod;
    private static String disableMessage;

    private IrisCompat() {}

    public static boolean shouldDisableBloom() {
        disableMessage = null;
        if (!IRIS_LOADED) return false;
        if (!initReflection()) { disableMessage = INSTALLED_MESSAGE; return true; }
        try {
            Object irisApi = irisGetInstanceMethod.invoke(null);
            Object result = irisIsShaderPackInUseMethod.invoke(irisApi);
            boolean active = result instanceof Boolean b && b;
            disableMessage = active ? ACTIVE_MESSAGE : null;
            return active;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!loggedReflectionFailure) {
                BloomMod.LOGGER.warn("Shine Iris/Oculus compatibility check failed.", e);
                loggedReflectionFailure = true;
            }
            disableMessage = INSTALLED_MESSAGE;
            return true;
        }
    }

    public static String disableMessage() { return disableMessage; }

    private static boolean initReflection() {
        if (reflectionInitialized) return reflectionAvailable;
        reflectionInitialized = true;
        try {
            Class<?> api = tryClass("net.irisshaders.iris.api.v0.IrisApi");
            if (api == null) api = tryClass("net.coderbot.iris.api.v0.IrisApi");
            if (api == null) { reflectionAvailable = false; return false; }
            irisGetInstanceMethod = api.getMethod("getInstance");
            irisIsShaderPackInUseMethod = api.getMethod("isShaderPackInUse");
            reflectionAvailable = true;
            return true;
        } catch (NoSuchMethodException e) {
            if (!loggedReflectionFailure) {
                BloomMod.LOGGER.warn("Shine could not access Iris/Oculus API.");
                loggedReflectionFailure = true;
            }
            reflectionAvailable = false;
            return false;
        }
    }

    private static Class<?> tryClass(String name) {
        try { return Class.forName(name); } catch (ClassNotFoundException e) { return null; }
    }

    private static boolean isModLoaded(String id) {
        try { return ModList.get().isLoaded(id); } catch (Exception e) { return false; }
    }
}
