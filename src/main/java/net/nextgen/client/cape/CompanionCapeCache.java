package net.nextgen.client.cape;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.nextgen.compat.CapeCompat;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for companion capes.
 *
 * The companion cape is independent: we only refresh the cached texture when the companion's
 * cape "version" increments (synced via entity data).
 *
 * NOTE: This implementation intentionally avoids hard dependencies on CraftHeraldry by routing
 * through {@link CapeCompat}. If CraftHeraldry isn't present, it falls back to a default cape.
 */
public final class CompanionCapeCache {

    private static final Map<UUID, ResourceLocation> CACHED_TEX = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_VER = new ConcurrentHashMap<>();

    private CompanionCapeCache() {}

    public static ResourceLocation getOrCreate(Entity companion, @Nullable UUID sourcePlayerUuid, int version) {
        if (companion == null || sourcePlayerUuid == null) return CapeCompat.getFallbackCape();

        UUID id = companion.getUUID();
        Integer last = LAST_VER.get(id);

        if (last == null || last != version) {
            LAST_VER.put(id, version);
            // Cache whatever cape texture is currently associated with the source player.
            // (If your compat layer returns a per-player dynamic texture, this will effectively
            // "snapshot" at the time the version changes.)
            CACHED_TEX.put(id, CapeCompat.getCapeTexture(sourcePlayerUuid));
        }

        return CACHED_TEX.getOrDefault(id, CapeCompat.getFallbackCape());
    }
}
