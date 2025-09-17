package net.nextgen.entity.custom;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.nextgen.NextGen;
import net.nextgen.item.CompanionSummonerItem;

public class CompanionEntity extends TamableAnimal {

    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    public CompanionEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15D, 5.0F, 2.0F, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SKIN, "");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Skin", this.getSkinName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Skin")) {
            this.setSkinName(tag.getString("Skin"));
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching() && stack.isEmpty() && this.isOwnedBy(player)) {
            if (!this.level().isClientSide) {
                this.returnEquipment(player);
                ItemStack token = new ItemStack(NextGen.COMPANION_SUMMONER.get());
                CompanionSummonerItem.storeSkin(token, this.getSkinName());
                if (!this.getSkinName().isBlank()) {
                    token.setHoverName(Component.literal(this.getSkinName()));
                }
                if (!player.addItem(token)) {
                    this.spawnAtLocation(token);
                }
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (stack.getItem() instanceof NameTagItem && stack.hasCustomHoverName()) {
            if (!this.level().isClientSide) {
                Component hoverName = stack.getHoverName();
                String skin = hoverName.getString().trim();
                if (!skin.isEmpty()) {
                    this.setSkinName(skin);
                    this.setCustomName(hoverName.copy());
                }
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        Optional<String> storedSkin = CompanionSummonerItem.getStoredSkin(stack);
        if (storedSkin.isPresent()) {
            if (!this.level().isClientSide) {
                this.setSkinName(storedSkin.get());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!stack.isEmpty() && this.canAcceptWeapon(stack) && this.isOwnedBy(player)) {
            if (!this.level().isClientSide) {
                ItemStack previous = this.getItemBySlot(EquipmentSlot.MAINHAND);
                if (!previous.isEmpty()) {
                    if (!player.addItem(previous.copy())) {
                        this.spawnAtLocation(previous.copy());
                    }
                }
                ItemStack copy = stack.copy();
                copy.setCount(1);
                this.setItemSlot(EquipmentSlot.MAINHAND, copy);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private void returnEquipment(Player player) {
        ItemStack held = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!held.isEmpty()) {
            ItemStack copy = held.copy();
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            if (!player.addItem(copy)) {
                this.spawnAtLocation(copy);
            }
        }
    }

    private boolean canAcceptWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof TieredItem || item instanceof TridentItem;
    }

    public String getSkinName() {
        return this.entityData.get(DATA_SKIN);
    }

    public void setSkinName(@Nullable String name) {
        this.entityData.set(DATA_SKIN, name == null ? "" : name);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }
}
