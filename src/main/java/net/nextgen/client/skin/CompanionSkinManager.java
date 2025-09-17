package net.nextgen.client.skin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

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

        SkinManager skinManager = minecraft.getSkinManager();
        if (skinManager == null) {
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


                if (profile.getId() == null) {
                    LOGGER.debug("Resolved companion skin profile '{}' has no id; skipping fetch", trimmed);
                    REQUESTED.remove(cacheKey);
                    return;
                }


                minecraft.execute(() -> registerProfileSkin(minecraft, profile, cacheKey));
            } catch (Exception exception) {
                LOGGER.error("Failed to resolve skin profile for companion name '{}': {}", trimmed, exception.getMessage());
                REQUESTED.remove(cacheKey);
            }
        });
    }


    private static void registerProfileSkin(Minecraft minecraft, GameProfile profile, String cacheKey) {

        SkinManager skinManager = minecraft.getSkinManager();
        if (skinManager == null) {
            REQUESTED.remove(cacheKey);
            return;
        }

        skinManager.registerSkins(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                SKIN_CACHE.put(cacheKey, location);
                REQUESTED.remove(cacheKey);
            }
        }, true);
    }

    private static GameProfile resolveProfile(Minecraft minecraft, String name) {
        GameProfile cached = getCachedProfile(minecraft, name);
        if (cached != null) {
            if (cached.getId() != null && hasTextures(cached)) {
                return cached;
            }
            if (cached.getId() == null && hasTextures(cached)) {
                return ensureProfileId(cached, name);
            }
        }

        GameProfile queryProfile = cached != null ? cached : new GameProfile(null, name);
        try {

            GameProfile filled = minecraft.getMinecraftSessionService().fillProfileProperties(queryProfile, true);
            if (filled != null) {
                if (filled.getId() != null) {
                    GameProfileCache cache = minecraft.getGameProfileCache();
                    if (cache != null && filled.isComplete()) {
                        cache.add(filled);
                    }
                    return filled;
                }
                return ensureProfileId(filled, name);
            }

            private static GameProfile ensureProfileId (GameProfile profile, String name){
                if (profile.getId() != null) {
                    return profile;
                }

                GameProfile withId = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), profile.getName() == null ? name : profile.getName());
                profile.getProperties().entries()
                        .forEach(entry -> withId.getProperties().put(entry.getKey(), entry.getValue()));
                return withId;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}

