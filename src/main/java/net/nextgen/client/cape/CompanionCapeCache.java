package net.nextgen.client.cape;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.nextgen.compat.CapeCompat;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache that builds a companion-owned cape texture by copying pixels from a source cape texture.
 *
 * Server stores only the "source player UUID" + "version" on the companion. Client resolves that UUID to a cape texture
 * via CraftHeraldry (if present), then snapshots the pixels into a new DynamicTexture registered under
 * a companion-specific ResourceLocation.
 *
 * The companion cape stays independent: it only changes when version increments.
 */
public final class CompanionCapeCache {

    private static final Map<UUID, ResourceLocation> SNAPSHOT = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_VER = new ConcurrentHashMap<>();

    private CompanionCapeCache() {}

    public static ResourceLocation getOrCreate(Entity companion, @Nullable UUID sourcePlayerUuid, int version) {
        if (companion == null || sourcePlayerUuid == null) return CapeCompat.getFallbackCape();

        UUID id = companion.getUUID();
        Integer last = LAST_VER.get(id);
        if (last == null || last != version) {
            LAST_VER.put(id, version);
            SNAPSHOT.remove(id);
        }

        return SNAPSHOT.computeIfAbsent(id, k -> snapshotFrom(sourcePlayerUuid, k));
    }

    private static ResourceLocation snapshotFrom(UUID sourcePlayerUuid, UUID companionUuid) {
        try {
            ResourceLocation sourceTex = CapeCompat.getCapeTexture(sourcePlayerUuid);
            if (sourceTex == null) return CapeCompat.getFallbackCape();

            NativeImage pixels = readPixels(sourceTex);
            if (pixels == null) return CapeCompat.getFallbackCape();

            DynamicTexture dyn = new DynamicTexture(pixels);
            String clean = companionUuid.toString().replace("-", "");
            ResourceLocation out = new ResourceLocation("nextgen", "dynamic/companion_cape/" + clean);

            TextureManager tm = Minecraft.getInstance().getTextureManager();
            tm.register(out, dyn);
            return out;
        } catch (Throwable t) {
            return CapeCompat.getFallbackCape();
        }
    }

    private static @Nullable NativeImage readPixels(ResourceLocation tex) {
        try {
            TextureManager tm = Minecraft.getInstance().getTextureManager();
            AbstractTexture at = tm.getTexture(tex);

            if (at instanceof DynamicTexture dyn) {
                NativeImage img = dyn.getPixels();
                if (img != null) {
                    NativeImage copy = new NativeImage(NativeImage.Format.RGBA, img.getWidth(), img.getHeight(), false);
                    copy.copyFrom(img);
                    return copy;
                }
            }
        } catch (Throwable ignored) {}

        // Try reading from resource manager if it's a normal resource texture.
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            var opt = rm.getResource(tex);
            if (opt.isEmpty()) return null;

            Resource res = opt.get();
            try (InputStream is = res.open()) {
                return NativeImage.read(is);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static void clear() {
        SNAPSHOT.clear();
        LAST_VER.clear();
    }
}
