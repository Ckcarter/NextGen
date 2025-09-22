package net.nextgen.entity.custom;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import net.nextgen.NextGen;
import net.nextgen.item.CompanionSummonerItem;
import net.nextgen.menu.CompanionInventoryMenu;
import net.nextgen.menu.CompanionSkinMenu;

public class CompanionEntity extends TamableAnimal {

    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);


    private static final String INVENTORY_TAG = "Inventory";
    public static final int INVENTORY_SIZE = 27;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);


    public CompanionEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
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

        ListTag items = new ListTag();
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) slot);
                stack.save(itemTag);
                items.add(itemTag);
            }
        }
        tag.put(INVENTORY_TAG, items);

    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Skin")) {
            this.setSkinName(tag.getString("Skin"));
        }

        this.inventory.clearContent();
        ListTag items = tag.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                this.inventory.setItem(slot, ItemStack.of(itemTag));
            }
        }

        this.setOrderedToSit(false);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching() && stack.isEmpty() && this.isOwnedBy(player)) {
            if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer,
                        new SimpleMenuProvider((windowId, inventory, serverSidePlayer) ->
                                new CompanionSkinMenu(windowId, inventory, this.getId()),
                                Component.translatable("Pick a players skin by name")),
                        buffer -> buffer.writeVarInt(this.getId()));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!player.isCrouching() && stack.isEmpty() && this.isOwnedBy(player)) {
            if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                Component title = this.hasCustomName() ? this.getCustomName()
                        : Component.translatable("container.nextgen.companion");
                NetworkHooks.openScreen(serverPlayer,
                        new SimpleMenuProvider((windowId, inventory, serverSidePlayer) ->
                                new CompanionInventoryMenu(windowId, inventory, this),
                                title),
                        buffer -> buffer.writeVarInt(this.getId()));
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


        if (!stack.isEmpty() && this.canAcceptArmor(stack) && this.isOwnedBy(player)) {
            if (!this.level().isClientSide) {
                ArmorItem armor = (ArmorItem) stack.getItem();
                EquipmentSlot slot = armor.getEquipmentSlot();
                this.equipItem(player, slot, stack);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }



        if (!stack.isEmpty() && this.canAcceptWeapon(stack) && this.isOwnedBy(player)) {
            if (!this.level().isClientSide) {
                this.equipItem(player, EquipmentSlot.MAINHAND, stack);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }


    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public void unsummon(ServerPlayer player) {
        this.dropAllCompanionItems(true);
        this.discard();
    }


    @Override
    public void tame(Player player) {
        super.tame(player);
        this.setOrderedToSit(false);
    }

    private boolean canAcceptWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof TieredItem || item instanceof TridentItem;
    }
    private boolean canAcceptArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }

    private void equipItem(Player player, EquipmentSlot slot, ItemStack stack) {
        ItemStack previous = this.getItemBySlot(slot);
        if (!previous.isEmpty()) {
            ItemStack previousCopy = previous.copy();
            if (!player.addItem(previousCopy)) {
                this.dropItemWithoutDespawn(previousCopy);
            }
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        this.setItemSlot(slot, copy);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
    }

    public String getSkinName() {
        return this.entityData.get(DATA_SKIN);
    }


    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            this.collectNearbyItems();
        }
    }


    @Override
    protected void dropEquipment() {

        this.dropEquipmentItems();
        super.dropEquipment();
        this.dropStoredItems();
        if (this.isTame()) {
            this.dropItemWithoutDespawn(this.createSummonerToken());
        }
    }



    private void collectNearbyItems() {
        if (this.isOrderedToSit()) {
            return;
        }
        AABB area = this.getBoundingBox().inflate(1.5D);
        List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, area,
                item -> !item.hasPickUpDelay() && item.isAlive());
        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            int originalCount = stack.getCount();
            ItemStack remaining = this.inventory.addItem(stack);
            int pickedUp = originalCount - (remaining.isEmpty() ? 0 : remaining.getCount());
            if (pickedUp > 0) {
                if (remaining.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(remaining);
                }
            }
        }
    }


    @Override
    public void die(DamageSource damageSource) {
        this.dropAllCompanionItems(this.isTame());
        super.die(damageSource);
    }

    private void dropAllCompanionItems(boolean dropSummonerToken) {
        if (this.level().isClientSide) {
            return;
        }
        this.dropEquipmentItems();
        this.dropStoredItems();
        if (dropSummonerToken) {
            this.dropItemWithoutDespawn(this.createSummonerToken());
        }
    }


    private void dropStoredItems() {
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                this.inventory.setItem(slot, ItemStack.EMPTY);
                this.dropItemWithoutDespawn(copy);
            }
        }
    }


    private void dropEquipmentItems() {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = this.getItemBySlot(slot);
            if (!equipped.isEmpty()) {
                ItemStack copy = equipped.copy();
                this.setItemSlot(slot, ItemStack.EMPTY);
                this.dropItemWithoutDespawn(copy);
            }
        }
    }

    private ItemStack createSummonerToken() {
        ItemStack token = new ItemStack(NextGen.COMPANION_SUMMONER.get());
        String skinName = this.getSkinName();
        CompanionSummonerItem.storeSkin(token, skinName);
        if (!skinName.isBlank()) {
            token.setHoverName(Component.literal(skinName));
        }
        return token;
    }



    private void dropItemWithoutDespawn(ItemStack stack) {
        ItemEntity itemEntity = this.spawnAtLocation(stack);
        if (itemEntity != null) {
            itemEntity.setUnlimitedLifetime();
        }
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
