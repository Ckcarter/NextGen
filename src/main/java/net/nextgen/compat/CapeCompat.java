package net.nextgen.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

/**
 * CraftHeraldry20 integration (optional).
 *
 * This class must compile and run even when CraftHeraldry is NOT installed.
 * We therefore locate CraftHeraldry's CapeClientCache by reflection and invoke a static method that:
 *   - takes a UUID
 *   - returns ResourceLocation
 */
public final class CapeCompat {

    private static final String MODID_CRAFTHERALDRY = "craftheraldry";

    private static final ResourceLocation FALLBACK =
            new ResourceLocation("nextgen", "textures/entity/companion_cape.png");

    // Reflection cache (resolved lazily)
    private static volatile boolean triedResolve = false;
    private static volatile Method getCapeByUuid = null;

    private CapeCompat() {}

    /** Always safe to call. */
    public static ResourceLocation getFallbackCape() {
        return FALLBACK;
    }

    /** Returns true if the CraftHeraldry mod is loaded. */
    public static boolean isCraftHeraldryLoaded() {
        try {
            return ModList.get().isLoaded(MODID_CRAFTHERALDRY);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Get the CraftHeraldry cape texture for a UUID, or fallback if unavailable.
     */
    public static ResourceLocation getCapeTexture(UUID playerUuid) {
        if (playerUuid == null) return FALLBACK;
        if (!isCraftHeraldryLoaded()) return FALLBACK;

        resolveIfNeeded();
        if (getCapeByUuid == null) return FALLBACK;

        try {
            Object out = getCapeByUuid.invoke(null, playerUuid);
            return (out instanceof ResourceLocation rl) ? rl : FALLBACK;
        } catch (Throwable t) {
            return FALLBACK;
        }
    }

    /**
     * Convenience overload: tries to read entity.getUUID() reflectively and then calls getCapeTexture(UUID).
     */
    public static ResourceLocation getCapeTexture(Object entity) {
        UUID id = tryGetUuid(entity);
        return getCapeTexture(id);
    }

    private static void resolveIfNeeded() {
        if (triedResolve) return;
        synchronized (CapeCompat.class) {
            if (triedResolve) return;
            triedResolve = true;

            // Known/likely class names for CraftHeraldry20 cache.
            String[] classNames = new String[] {
                    "com.example.craftheraldry.client.cape.CapeClientCache",
                    "com.example.craftheraldry.client.cape.CapeCache",
                    "com.example.craftheraldry.client.cape.CapeClient"
            };

            for (String cn : classNames) {
                try {
                    Class<?> c = Class.forName(cn);
                    Method m = findStaticUuidToResourceLocation(c);
                    if (m != null) {
                        getCapeByUuid = m;
                        return;
                    }
                } catch (Throwable ignored) {
                    // keep trying
                }
            }

            getCapeByUuid = null;
        }
    }

    /**
     * Find a static method that takes (UUID) and returns ResourceLocation.
     * We also tolerate method names changing across versions.
     */
    private static Method findStaticUuidToResourceLocation(Class<?> c) {
        try {
            for (Method m : c.getMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != UUID.class) continue;
                if (m.getReturnType() != ResourceLocation.class) continue;

                // Prefer common names, but accept any matching signature.
                String n = m.getName();
                if (n.equals("getCapeTexture") || n.equals("getCape") || n.equals("getCapeFor") || n.equals("getCapeLocation")) {
                    return m;
                }
            }
            // If none matched preferred names, accept any signature match
            for (Method m : c.getMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != UUID.class) continue;
                if (m.getReturnType() != ResourceLocation.class) continue;
                return m;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static UUID tryGetUuid(Object entity) {
        if (entity == null) return null;
        try {
            Method m = entity.getClass().getMethod("getUUID");
            Object out = m.invoke(entity);
            return (out instanceof UUID) ? (UUID) out : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
