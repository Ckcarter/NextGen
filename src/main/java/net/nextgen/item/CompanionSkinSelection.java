package net.nextgen.item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nextgen.NextGen;

@Mod.EventBusSubscriber(modid = NextGen.MOD_ID)
public final class CompanionSkinSelection {

    private static final Map<UUID, InteractionHand> ACTIVE_SELECTIONS = new ConcurrentHashMap<>();

    private CompanionSkinSelection() {}

    public static void beginEditing(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CompanionSummonerItem)) {
            return;
        }

        ACTIVE_SELECTIONS.put(player.getUUID(), hand);
        CompanionSummonerItem.getStoredSkin(stack)
                .filter(name -> !name.isBlank())
                .ifPresent(name -> player.sendSystemMessage(
                        Component.translatable("message.nextgen.companion_summoner.current", name)));
        player.sendSystemMessage(Component.translatable("message.nextgen.companion_summoner.enter_skin"));
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        InteractionHand hand = ACTIVE_SELECTIONS.remove(player.getUUID());
        if (hand == null) {
            return;
        }

        event.setCanceled(true);

        String rawInput = event.getMessage().getSignedContent().trim();
        if (rawInput.isEmpty() || "cancel".equalsIgnoreCase(rawInput)) {
            player.sendSystemMessage(Component.translatable("message.nextgen.companion_summoner.cancelled"));
            return;
        }

        ItemStack stack = resolveTargetStack(player, hand);
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.nextgen.companion_summoner.not_found"));
            return;
        }

        if ("clear".equalsIgnoreCase(rawInput)) {
            CompanionSummonerItem.clearStoredSkin(stack);
            stack.resetHoverName();
            player.getInventory().setChanged();
            player.sendSystemMessage(Component.translatable("message.nextgen.companion_summoner.cleared"));
            return;
        }

        CompanionSummonerItem.storeSkin(stack, rawInput);
        stack.setHoverName(Component.literal(rawInput.trim()));
        player.getInventory().setChanged();
        player.sendSystemMessage(Component.translatable("message.nextgen.companion_summoner.updated", rawInput.trim()));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE_SELECTIONS.remove(event.getEntity().getUUID());
    }

    private static ItemStack resolveTargetStack(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof CompanionSummonerItem) {
            return stack;
        }

        for (ItemStack inventoryStack : player.getInventory().items) {
            if (inventoryStack.getItem() instanceof CompanionSummonerItem) {
                return inventoryStack;
            }
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof CompanionSummonerItem) {
            return offhand;
        }

        return ItemStack.EMPTY;
    }
}