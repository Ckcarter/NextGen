
package net.nextgen.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

public final class CapeCompat {

    private static final ResourceLocation FALLBACK =
            new ResourceLocation("nextgen", "textures/entity/companion_cape.png");

    private static final boolean HAS_CRAFTHERALDRY =
            ModList.get().isLoaded("craftheraldry");

    private static boolean lookedUp = false;
    private static Method getCapeTexMethod = null;

    private CapeCompat() {}

    public static ResourceLocation getCompanionCapeTexture(Object entity) {
        if (!HAS_CRAFTHERALDRY) return FALLBACK;

        UUID uuid = tryGetUuid(entity);
        if (uuid == null) return FALLBACK;

        try {
            ensureLookup();
            if (getCapeTexMethod == null) return FALLBACK;

            Object rl = getCapeTexMethod.invoke(null, uuid);
            return (rl instanceof ResourceLocation) ? (ResourceLocation) rl : FALLBACK;
        } catch (Throwable t) {
            return FALLBACK;
        }
    }

    private static void ensureLookup() {
        if (lookedUp) return;
        lookedUp = true;

        try {
            Class<?> cache = Class.forName(
                "com.example.craftheraldry.client.cape.CapeClientCache"
            );
            getCapeTexMethod = cache.getMethod("getCapeTexture", UUID.class);
        } catch (Throwable t) {
            getCapeTexMethod = null;
        }
    }

    private static UUID tryGetUuid(Object entity) {
        try {
            Method m = entity.getClass().getMethod("getUUID");
            Object out = m.invoke(entity);
            return (out instanceof UUID) ? (UUID) out : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
