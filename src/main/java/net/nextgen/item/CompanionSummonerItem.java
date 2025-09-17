package net.nextgen.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.nextgen.entity.ModEntityTypes;
import net.nextgen.entity.custom.CompanionEntity;

public class CompanionSummonerItem extends Item {

    private static final String SKIN_TAG = "CompanionSkin";

    public CompanionSummonerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 spawnPos = player.position().add(look.scale(2.0D));
            return spawnCompanion(serverLevel, player, stack, spawnPos);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        if (level instanceof ServerLevel serverLevel) {
            BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace());
            Vec3 spawnPos = Vec3.atBottomCenterOf(targetPos);
            return spawnCompanion(serverLevel, player, stack, spawnPos).getResult();
        }

        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    private InteractionResultHolder<ItemStack> spawnCompanion(ServerLevel serverLevel, Player player, ItemStack stack, Vec3 position) {
        CompanionEntity entity = ModEntityTypes.COMPANION.get().create(serverLevel);
        if (entity == null) {
            return InteractionResultHolder.fail(stack);
        }

        entity.moveTo(position.x(), position.y(), position.z(), player.getYRot(), 0.0F);
        if (!serverLevel.noCollision(entity)) {
            return InteractionResultHolder.fail(stack);
        }
        entity.tame(player);
        String skinName = resolveSkin(stack, player).trim();
        entity.setSkinName(skinName);
        if (!skinName.isEmpty()) {
            entity.setCustomName(Component.literal(skinName));
        }
        entity.setPersistenceRequired();
        serverLevel.addFreshEntity(entity);
        serverLevel.gameEvent(GameEvent.ENTITY_PLACE, BlockPos.containing(position), GameEvent.Context.of(player));

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, serverLevel.isClientSide());
    }

    private String resolveSkin(ItemStack stack, Player player) {
        return getStoredSkin(stack).filter(name -> !name.isBlank())
                .orElseGet(() -> stack.hasCustomHoverName() ? stack.getHoverName().getString() : player.getGameProfile().getName());
    }

    public static Optional<String> getStoredSkin(ItemStack stack) {
        if (!(stack.getItem() instanceof CompanionSummonerItem)) {
            return Optional.empty();
        }

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SKIN_TAG)) {
            return Optional.of(tag.getString(SKIN_TAG));
        }
        if (stack.hasCustomHoverName()) {
            return Optional.of(stack.getHoverName().getString());
        }
        return Optional.empty();
    }

    public static void storeSkin(ItemStack stack, String skinName) {
        if (skinName == null || skinName.isBlank()) {
            return;
        }
        stack.getOrCreateTag().putString(SKIN_TAG, skinName);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        getStoredSkin(stack).ifPresent(name -> tooltip.add(Component.translatable("item.nextgen.companion_summoner.skin", name)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
