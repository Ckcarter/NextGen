package net.nextgen.client.skin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.nextgen.entity.custom.CompanionEntity;

@OnlyIn(Dist.CLIENT)
public final class CompanionSkinManager {

    private static final Map<String, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();

    private CompanionSkinManager() {}

    public static ResourceLocation getSkinLocation(CompanionEntity entity) {
        String requestedSkin = entity.getSkinName();
        if (requestedSkin.isBlank()) {
            return DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
        }

        ResourceLocation cached = SKIN_CACHE.get(requestedSkin);
        if (cached != null) {
            return cached;
        }

        requestSkin(requestedSkin);
        UUID fallbackId = UUIDUtil.createOfflinePlayerUUID(requestedSkin);
        return DefaultPlayerSkin.getDefaultSkin(fallbackId);
    }

    private static void requestSkin(String skinName) {
        if (!REQUESTED.add(skinName)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            REQUESTED.remove(skinName);
            return;
        }

        SkinManager skinManager = minecraft.getSkinManager();
        if (skinManager == null) {
            REQUESTED.remove(skinName);
            return;
        }

        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(skinName), skinName);
        skinManager.registerSkins(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                SKIN_CACHE.put(skinName, location);
            }
        }, true);
    }
}
