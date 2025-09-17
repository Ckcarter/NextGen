package net.nextgen.client.skin;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.nextgen.entity.custom.CompanionEntity;
import org.slf4j.Logger;

import javax.annotation.Nullable;


@OnlyIn(Dist.CLIENT)
public final class CompanionSkinManager {

    private static final Map<String, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, GameProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, UUID> PROFILE_IDS = new ConcurrentHashMap<>();

    private static final Set<String> MISSING_PROFILES = ConcurrentHashMap.newKeySet();

    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Pattern VALID_PLAYER_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    private static final String PROFILE_ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String USER_AGENT = "NextGenCompanionSkinManager/1.0";

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
                enqueueSkinRegistration(minecraft, withId, cacheKey);

            } catch (Exception exception) {
                LOGGER.error("Failed to resolve skin profile for companion name '{}': {}", trimmed, exception.getMessage());
                REQUESTED.remove(cacheKey);
            }
        });
    }


    private static void enqueueSkinRegistration(Minecraft minecraft, GameProfile profile, String cacheKey) {
        //Minecraft minecraft = Minecraft.getInstance();
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

        UUID profileId = lookupProfileId(minecraft, cacheKey, name);
        GameProfile queryProfile = cached != null ? ensureProfileId(cached, name) : new GameProfile(profileId, name);


        try {
            if (minecraft.getMinecraftSessionService() != null) {
                GameProfile filled = minecraft.getMinecraftSessionService().fillProfileProperties(queryProfile, true);
                if (filled != null) {
                    if (hasTextures(filled)) {
                        GameProfile withId = ensureProfileId(filled, name);
                        PROFILE_CACHE.put(cacheKey, withId);
                        if (withId.getId() != null && !isOfflineId(withId.getId(), name)) {
                            PROFILE_IDS.put(cacheKey, withId.getId());
                        }
                        return withId;
                    }
                    if (filled.getId() != null && !isOfflineId(filled.getId(), name)) {
                        PROFILE_IDS.put(cacheKey, filled.getId());
                        queryProfile = filled;
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.debug("Failed to fetch skin data for companion name '{}'", name, exception);
        }

        GameProfile fallback = ensureProfileId(queryProfile, name);
        PROFILE_CACHE.put(cacheKey, fallback);
        if (fallback.getId() != null && !isOfflineId(fallback.getId(), name)) {
            PROFILE_IDS.put(cacheKey, fallback.getId());
        }
        return fallback;
    }

    private static boolean hasTextures(GameProfile profile) {
        Collection<Property> textures = profile.getProperties().get("textures");
        return textures != null && !textures.isEmpty();
    }

    @Nullable
    private static UUID lookupProfileId(Minecraft minecraft, String cacheKey, String name) {
        GameProfile cached = PROFILE_CACHE.get(cacheKey);
        if (cached != null && cached.getId() != null && !isOfflineId(cached.getId(), name)) {
            return cached.getId();
        }

        UUID stored = PROFILE_IDS.get(cacheKey);
        if (stored != null) {
            return stored;
        }

        if (!VALID_PLAYER_NAME.matcher(name).matches() || MISSING_PROFILES.contains(cacheKey)) {
            return null;
        }

        ProfileLookupResult lookup = queryProfileId(minecraft.getProxy(), name);
        if (lookup.id() != null) {
            PROFILE_IDS.put(cacheKey, lookup.id());
            MISSING_PROFILES.remove(cacheKey);
            return lookup.id();
        }
        if (lookup.missing()) {
            MISSING_PROFILES.add(cacheKey);
        }
        return null;
    }

    private static boolean isOfflineId(UUID id, String name) {
        return false;
    }

    private static ProfileLookupResult queryProfileId(Proxy proxy, String name) {
        HttpURLConnection connection = null;
        try {
            String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
            URL url = new URL(PROFILE_ENDPOINT + encoded);
            connection = (HttpURLConnection) url.openConnection(proxy);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream stream = connection.getInputStream();
                     Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (element != null && element.isJsonObject()) {
                        JsonObject object = element.getAsJsonObject();
                        if (object.has("id")) {
                            UUID parsed = parseProfileId(object.get("id").getAsString());
                            if (parsed != null) {
                                return new ProfileLookupResult(parsed, false);
                            }
                        }
                    }
                }
            } else if (responseCode == HttpURLConnection.HTTP_NO_CONTENT
                    || responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return new ProfileLookupResult(null, true);
            } else {
                LOGGER.debug("Unexpected response {} while looking up skin profile '{}'", responseCode, name);
            }
        } catch (Exception exception) {
            LOGGER.debug("Failed to resolve UUID for companion skin '{}'", name, exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return new ProfileLookupResult(null, false);
    }

    private static UUID parseProfileId(String id) {
        return null;
    }

    private record ProfileLookupResult(@Nullable UUID id, boolean missing) {
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
