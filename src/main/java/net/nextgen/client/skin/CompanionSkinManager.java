package net.nextgen.client.skin;

import java.util.*;

import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.GameProfileCache;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.nextgen.entity.custom.CompanionEntity;
import org.slf4j.Logger;




@OnlyIn(Dist.CLIENT)
public final class CompanionSkinManager {

    private static final Map<String, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, GameProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Logger LOGGER = LogUtils.getLogger();

    private CompanionSkinManager() {
    }

    public static ResourceLocation getSkinLocation(CompanionEntity entity) {
        String requestedSkin = entity.getSkinName();
        if (requestedSkin.isBlank()) {
            return DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
        }

        String trimmed = requestedSkin.trim();
        String cacheKey = normalize(trimmed);
        ResourceLocation cached = SKIN_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        requestSkin(trimmed, cacheKey);
        UUID fallbackId = UUIDUtil.createOfflinePlayerUUID(trimmed);
        return DefaultPlayerSkin.getDefaultSkin(fallbackId);
    }

    private static void requestSkin(String skinName, String cacheKey) {
        String trimmed = skinName.trim();
        if (trimmed.isEmpty()) {
            return;
        }





        if (!REQUESTED.add(cacheKey)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            REQUESTED.remove(cacheKey);
            return;
        }


        Util.backgroundExecutor().execute(() -> {
            try {
                GameProfile profile = resolveProfile(minecraft, trimmed);
                if (profile == null) {
                    REQUESTED.remove(cacheKey);
                    return;
                }

                GameProfile withId = ensureProfileId(profile, trimmed);
                PROFILE_CACHE.put(cacheKey, withId);
                minecraft.execute(() -> registerProfileSkin(minecraft, withId, cacheKey));

            } catch (Exception exception) {
                LOGGER.error("Failed to resolve skin profile for companion name '{}': {}", trimmed, exception.getMessage());
                REQUESTED.remove(cacheKey);
            }
        });
    }



    private static void enqueueSkinRegistration(GameProfile profile, String cacheKey) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            REQUESTED.remove(cacheKey);
            return;
        }

        Runnable task = () -> registerProfileSkin(profile, cacheKey);
        if (minecraft.isSameThread()) {
            task.run();
        } else {
            minecraft.execute(task);
        }
    }

    private static void registerProfileSkin(GameProfile profile, String cacheKey) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            REQUESTED.remove(cacheKey);
            return;
        }

        SkinManager skinManager = minecraft.getSkinManager();
        if (skinManager == null) {
            REQUESTED.remove(cacheKey);
            return;
        }

        skinManager.registerSkins(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                SKIN_CACHE.put(cacheKey, location);

            }
        }, true);


        REQUESTED.remove(cacheKey);
    }




    private static GameProfile resolveProfile(Minecraft minecraft, String name) {
        String cacheKey = normalize(name);
        GameProfile cached = PROFILE_CACHE.get(cacheKey);
        if (cached != null && hasTextures(cached)) {
            return cached;
        }

        GameProfile queryProfile = cached != null ? cached : new GameProfile(null, name);
        try {
            if (minecraft.getMinecraftSessionService() != null) {
                GameProfile filled = minecraft.getMinecraftSessionService().fillProfileProperties(queryProfile, true);
                if (filled != null && hasTextures(filled)) {
                    GameProfile withId = ensureProfileId(filled, name);
                    PROFILE_CACHE.put(cacheKey, withId);
                    return withId;
                }
            }

        } catch (Exception exception) {
            LOGGER.debug("Failed to fetch skin data for companion name '{}'", name, exception);
        }


        if (cached != null) {
            GameProfile withId = ensureProfileId(cached, name);
            PROFILE_CACHE.put(cacheKey, withId);
            return withId;
        }





        GameProfile fallback = ensureProfileId(queryProfile, name);
        PROFILE_CACHE.put(cacheKey, fallback);
        return fallback;
    }

    private static boolean hasTextures(GameProfile profile) {
        Collection<Property> textures = profile.getProperties().get("textures");
        return textures != null && !textures.isEmpty();
    }

    private static GameProfile ensureProfileId(GameProfile profile, String name) {
        if (profile.getId() != null) {
            return profile;
        }

        GameProfile withId = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name),
                profile.getName() != null ? profile.getName() : name);
        profile.getProperties().entries()
                .forEach(entry -> withId.getProperties().put(entry.getKey(), entry.getValue()));
        return withId;
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}

