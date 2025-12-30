package net.nextgen.entity.custom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.nextgen.NextGen;
import net.nextgen.item.CompanionSummonerItem;
import net.nextgen.menu.CompanionInventoryMenu;
import net.nextgen.menu.CompanionSkinMenu;
import net.minecraft.world.item.ItemStack;
import net.nextgen.compat.CraftHeraldryCompat;



public class CompanionEntity extends TamableAnimal implements RangedAttackMob {

    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    // --- Independent cape data (synced to clients, saved to NBT) ---
    private static final EntityDataAccessor<String> DATA_CAPE_SOURCE_UUID =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_CAPE_VERSION =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);


    private static final String INVENTORY_TAG = "Inventory";
    private static final String STAY_POS_TAG = "StayPos";
    public static final int INVENTORY_SIZE = 54;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    private boolean hasDroppedSummonerToken;

    private final MeleeAttackGoal meleeAttackGoal;
    private final RangedBowAttackGoal<CompanionEntity> bowAttackGoal;

    private static final int STAY_PATROL_RADIUS = 6;
    @Nullable
    private BlockPos stayPos;

    private static final int CONSUMABLE_COOLDOWN_TICKS = 100;
    private int consumableCooldown;

    public CompanionEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
        this.meleeAttackGoal = new MeleeAttackGoal(this, 1.2D, true);
        this.bowAttackGoal = new RangedBowAttackGoal<>(this, 1.15D, 20, 15.0F);


    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));

//        this.goalSelector.addGoal(2, this.meleeAttackGoal);

        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15D, 5.0F, 2.0F, false));
        this.goalSelector.addGoal(4, new CompanionRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, true));

//        this.updateAttackGoal();

    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SKIN, "");
        this.entityData.define(DATA_CAPE_SOURCE_UUID, "");
        this.entityData.define(DATA_CAPE_VERSION, 0);
    }





    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Skin", this.getSkinName());
        tag.putString("CapeSourceUuid", this.entityData.get(DATA_CAPE_SOURCE_UUID));
        tag.putInt("CapeVersion", this.entityData.get(DATA_CAPE_VERSION));

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


        if (this.stayPos != null) {
            tag.putLong(STAY_POS_TAG, this.stayPos.asLong());
        }


    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Skin")) {
            this.setSkinName(tag.getString("Skin"));
        if (tag.contains("CapeSourceUuid")) this.entityData.set(DATA_CAPE_SOURCE_UUID, tag.getString("CapeSourceUuid"));
        if (tag.contains("CapeVersion")) this.entityData.set(DATA_CAPE_VERSION, tag.getInt("CapeVersion"));
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


        if (!this.level().isClientSide) {
            this.updateAttackGoal();
        }

        if (tag.contains(STAY_POS_TAG, Tag.TAG_LONG)) {
            this.stayPos = BlockPos.of(tag.getLong(STAY_POS_TAG));
            if (this.isOrderedToSit()) {
                this.restrictTo(this.stayPos, STAY_PATROL_RADIUS);
            }
        } else {
            this.stayPos = null;
        }

    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);


// CraftHeraldry scroll interaction: apply/clear an INDEPENDENT companion cape snapshot.
if (!this.level().isClientSide && CraftHeraldryCompat.isScroll(stack) && this.isOwnedBy(player)) {
    if (player.isCrouching()) {
        this.clearCape();
        player.displayClientMessage(Component.literal("Companion cape cleared."), true);
    } else {
        this.applyCapeFromPlayer(player.getUUID());
        player.displayClientMessage(Component.literal("Companion cape applied."), true);
    }
    return InteractionResult.CONSUME;
}


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


        if (!stack.isEmpty() && this.isOwnedBy(player)) {
            if (this.level().isClientSide) {
                return this.hasInventorySpaceFor(stack)
                        ? InteractionResult.SUCCESS
                        : InteractionResult.PASS;
            }
            int inserted = this.storeItemInInventory(stack);
            if (inserted > 0) {
                if (!player.isCreative()) {
                    stack.shrink(inserted);
                }
                return InteractionResult.CONSUME;
            }
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
    public void setStay(boolean stay) {
        this.setOrderedToSit(stay);
        if (stay) {
            BlockPos currentPos = this.blockPosition();
            this.stayPos = currentPos;
            this.restrictTo(currentPos, STAY_PATROL_RADIUS);
            this.getNavigation().stop();
            this.setTarget(null);
        } else {
            this.stayPos = null;
            this.clearRestriction();
        }
    }
    @Nullable
    public BlockPos getStayPos() {
        return this.stayPos;
    }

    private boolean canAcceptWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof TieredItem
                || item instanceof TridentItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
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


public @Nullable UUID getCapeSourceUuid() {
    String s = this.entityData.get(DATA_CAPE_SOURCE_UUID);
    if (s == null || s.isEmpty()) return null;
    try { return UUID.fromString(s); } catch (Exception e) { return null; }
}

public int getCapeVersion() {
    return this.entityData.get(DATA_CAPE_VERSION);
}

/** Apply (or re-apply) the cape from a player UUID. Increments version to force clients to re-snapshot. */
public void applyCapeFromPlayer(UUID playerUuid) {
    if (playerUuid == null) return;
    this.entityData.set(DATA_CAPE_SOURCE_UUID, playerUuid.toString());
    this.entityData.set(DATA_CAPE_VERSION, this.entityData.get(DATA_CAPE_VERSION) + 1);
}

/** Clear the companion cape. */
public void clearCape() {
    this.entityData.set(DATA_CAPE_SOURCE_UUID, "");
    this.entityData.set(DATA_CAPE_VERSION, this.entityData.get(DATA_CAPE_VERSION) + 1);
}



    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            if (this.consumableCooldown > 0) {
                this.consumableCooldown--;
            }
            this.tryDrinkPotion();
            this.tryEatFood();
            this.tryDrinkPotion();
            this.updateWeaponChoice();
            this.collectNearbyItems();
            this.updateStayBehavior();
        }
    }


    @Override
    protected void dropEquipment() {

        this.dropEquipmentItems();
        super.dropEquipment();
        this.dropStoredItems();

        this.dropSummonerTokenIfNeeded();

    }


    private void collectNearbyItems() {
        if (this.isOrderedToSit() || this.isDeadOrDying()) {
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


    private void updateStayBehavior() {
        if (!this.isOrderedToSit()) {
            return;
        }
        if (this.stayPos == null) {
            this.stayPos = this.blockPosition();
            this.restrictTo(this.stayPos, STAY_PATROL_RADIUS);
        }
        if (this.stayPos == null) {
            return;
        }
        Vec3 stayCenter = Vec3.atCenterOf(this.stayPos);
        double maxDistance = (double) (STAY_PATROL_RADIUS + 2);
        if (this.distanceToSqr(stayCenter) > maxDistance * maxDistance && !this.getNavigation().isInProgress()) {
            this.getNavigation().moveTo(stayCenter.x, stayCenter.y, stayCenter.z, 1.0D);
        }
    }



    @Override
    public void die(DamageSource damageSource) {
        this.setCanPickUpLoot(false);
        this.dropAllCompanionItems(false);
        this.hasDroppedSummonerToken = true;
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

    private void dropSummonerTokenIfNeeded() {
        if (this.isTame() && !this.hasDroppedSummonerToken) {
            this.dropItemWithoutDespawn(this.createSummonerToken());
            this.hasDroppedSummonerToken = true;
        }
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

    private boolean hasInventorySpaceFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack current = this.inventory.getItem(slot);
            if (current.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameTags(current, stack)) {
                int maxStackSize = Math.min(current.getMaxStackSize(), this.inventory.getMaxStackSize());
                if (current.getCount() < maxStackSize) {
                    return true;
                }
            }
        }
        return false;
    }

    private int storeItemInInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ItemStack copy = stack.copy();
        int originalCount = copy.getCount();
        ItemStack remainder = this.inventory.addItem(copy);
        int remaining = remainder.isEmpty() ? 0 : remainder.getCount();
        return originalCount - remaining;
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




    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!this.level().isClientSide && slot == EquipmentSlot.MAINHAND) {
            this.updateAttackGoal();
        }
    }

    private void updateAttackGoal() {
        this.goalSelector.removeGoal(this.meleeAttackGoal);
        this.goalSelector.removeGoal(this.bowAttackGoal);

        ItemStack mainHand = this.getMainHandItem();
        if (mainHand.getItem() instanceof BowItem) {
            this.goalSelector.addGoal(2, this.bowAttackGoal);
        } else {
            this.goalSelector.addGoal(2, this.meleeAttackGoal);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack weapon = this.getItemInHand(
                ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));


        if (!(weapon.getItem() instanceof ProjectileWeaponItem projectileWeaponItem)) {
            return;
        }

        int projectileSlot = this.findProjectileSlot(projectileWeaponItem);
        if (projectileSlot < 0) {
            return;
        }

        ItemStack projectileStack = this.inventory.getItem(projectileSlot);
        if (projectileStack.isEmpty()) {
            return;
        }

        ItemStack projectileCopy = projectileStack.copy();
        projectileCopy.setCount(1);

        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectileCopy, distanceFactor);

        if (arrow == null) {
            return;
        }
        if (weapon.getItem() instanceof BowItem bowItem) {
            arrow = bowItem.customArrow(arrow);
        }

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontalDistance * 0.2D, dz, 1.6F,
                (float) (14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F,
                1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);


        this.consumeProjectileFromSlot(projectileSlot);
    }

    @Override
    public ItemStack getProjectile(ItemStack weapon) {
        if (!(weapon.getItem() instanceof ProjectileWeaponItem projectileWeaponItem)) {
            return ItemStack.EMPTY;
        }

        int slot = this.findProjectileSlot(projectileWeaponItem);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = this.inventory.getItem(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private int findProjectileSlot(ProjectileWeaponItem weaponItem) {
        Predicate<ItemStack> predicate = weaponItem.getSupportedHeldProjectiles();
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private void consumeProjectileFromSlot(int slot) {
        if (slot < 0) {
            return;
        }
        ItemStack stack = this.inventory.getItem(slot);
        if (!stack.isEmpty()) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                this.inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

    }



    private void updateWeaponChoice() {
        LivingEntity target = this.getTarget();
        ItemStack mainHand = this.getMainHandItem();

        if (this.shouldUseRangedWeapon(target)) {
            if (!this.isRangedWeapon(mainHand)) {
                int slot = this.findRangedWeaponSlot();
                if (slot >= 0) {
                    this.swapMainHandWithInventorySlot(slot);
                    mainHand = this.getMainHandItem();
                }
            }

            if (this.isRangedWeapon(mainHand)
                    && mainHand.getItem() instanceof ProjectileWeaponItem projectileWeaponItem
                    && !this.hasProjectileFor(projectileWeaponItem)) {
                int meleeSlot = this.findMeleeWeaponSlot();
                if (meleeSlot >= 0) {
                    this.swapMainHandWithInventorySlot(meleeSlot);
                }
            }
        } else {
            if (this.isRangedWeapon(mainHand)) {
                int meleeSlot = this.findMeleeWeaponSlot();
                if (meleeSlot >= 0) {
                    this.swapMainHandWithInventorySlot(meleeSlot);
                }
            } else if (mainHand.isEmpty()) {
                int meleeSlot = this.findMeleeWeaponSlot();
                if (meleeSlot >= 0) {
                    this.swapMainHandWithInventorySlot(meleeSlot);
                }
            }
        }
    }

    private boolean shouldUseRangedWeapon(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        double distance = this.distanceToSqr(target);
        if (distance <= 16.0D) {
            return false;
        }

        ItemStack mainHand = this.getMainHandItem();
        if (this.isRangedWeapon(mainHand)
                && mainHand.getItem() instanceof ProjectileWeaponItem projectileWeaponItem
                && this.hasProjectileFor(projectileWeaponItem)) {
            return true;
        }

        int slot = this.findRangedWeaponSlot();
        if (slot < 0) {
            return false;
        }

        ItemStack stack = this.inventory.getItem(slot);
        if (!(stack.getItem() instanceof ProjectileWeaponItem projectileWeaponItem)) {
            return false;
        }

        return this.hasProjectileFor(projectileWeaponItem);
    }

    private boolean hasProjectileFor(ProjectileWeaponItem weaponItem) {
        if (weaponItem.getSupportedHeldProjectiles().test(this.getOffhandItem())) {
            return true;
        }
        return this.findProjectileSlot(weaponItem) >= 0;
    }

    private boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    private int findRangedWeaponSlot() {
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof BowItem) {
                return slot;
            }
        }
        return -1;
    }

    private int findMeleeWeaponSlot() {
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (!stack.isEmpty() && (stack.getItem() instanceof TieredItem
                    || stack.getItem() instanceof TridentItem)) {
                return slot;
            }
        }
        return -1;
    }

    private void swapMainHandWithInventorySlot(int slot) {
        if (slot < 0 || slot >= this.inventory.getContainerSize()) {
            return;
        }

        ItemStack inventoryStack = this.inventory.getItem(slot);
        if (inventoryStack.isEmpty()) {
            return;
        }

        ItemStack mainHand = this.getMainHandItem();
        this.inventory.setItem(slot, mainHand);
        this.setItemSlot(EquipmentSlot.MAINHAND, inventoryStack);
    }

    private void tryDrinkPotion() {
        if (this.consumableCooldown > 0) {
            return;
        }

        if (this.isDeadOrDying() || this.getHealth() >= this.getMaxHealth()) {
            return;
        }

        int slot = this.findHealingPotionSlot();
        if (slot < 0) {
            return;
        }

        ItemStack potionStack = this.inventory.getItem(slot);
        if (potionStack.isEmpty() || !(potionStack.getItem() instanceof PotionItem)) {
            return;
        }

        ItemStack singlePotion = potionStack.copy();
        singlePotion.setCount(1);

        this.swing(InteractionHand.MAIN_HAND);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_DRINK, this.getSoundSource(), 1.0F, 1.0F);

        this.applyPotionEffects(singlePotion);

        potionStack.shrink(1);
        if (potionStack.isEmpty()) {
            this.inventory.setItem(slot, ItemStack.EMPTY);
        } else {
            this.inventory.setItem(slot, potionStack);
        }

        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        ItemStack remainder = this.inventory.addItem(bottle);
        if (!remainder.isEmpty()) {
            this.dropItemWithoutDespawn(remainder);
        }

        this.consumableCooldown = CONSUMABLE_COOLDOWN_TICKS;
    }

    private void tryEatFood() {
        if (this.consumableCooldown > 0) {
            return;
        }

        if (this.isDeadOrDying() || this.getHealth() >= this.getMaxHealth()) {
            return;
        }

        int slot = this.findHealingFoodSlot();
        if (slot < 0) {
            return;
        }

        ItemStack foodStack = this.inventory.getItem(slot);
        FoodProperties properties = foodStack.getFoodProperties(this);
        if (foodStack.isEmpty() || properties == null) {
            return;
        }

        ItemStack singleFood = foodStack.copy();
        singleFood.setCount(1);

        this.swing(InteractionHand.MAIN_HAND);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EAT, this.getSoundSource(), 1.0F, 1.0F);
        this.gameEvent(GameEvent.EAT);

        ItemStack container = singleFood.finishUsingItem(this.level(), this);

        foodStack.shrink(1);
        if (foodStack.isEmpty()) {
            this.inventory.setItem(slot, ItemStack.EMPTY);
        } else {
            this.inventory.setItem(slot, foodStack);
        }

        if (!container.isEmpty() && !container.isEdible()) {
            ItemStack remainder = this.inventory.addItem(container);
            if (!remainder.isEmpty()) {
                this.dropItemWithoutDespawn(remainder);
            }
        }

        this.heal(properties.getNutrition());
        this.consumableCooldown = CONSUMABLE_COOLDOWN_TICKS;
    }

    private void applyPotionEffects(ItemStack potionStack) {
        if (!(potionStack.getItem() instanceof PotionItem)) {
            return;
        }

        for (MobEffectInstance instance : PotionUtils.getMobEffects(potionStack)) {
            MobEffect effect = instance.getEffect();
            if (effect == null) {
                continue;
            }

            if (effect.isInstantenous()) {
                effect.applyInstantenousEffect(null, null, this, instance.getAmplifier(), 1.0D);
            } else {
                this.addEffect(new MobEffectInstance(instance));
            }
        }
    }

    private int findHealingPotionSlot() {
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (this.isHealingPotion(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private int findHealingFoodSlot() {
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (this.isHealingFood(stack)) {
                return slot;
            }
        }
        return -1;
    }




    private boolean isHealingPotion(ItemStack stack) {
        if (!(stack.getItem() instanceof PotionItem)) {
            return false;
        }

        for (MobEffectInstance instance : PotionUtils.getMobEffects(stack)) {
            MobEffect effect = instance.getEffect();
            if (effect != null && this.isHealingEffect(effect)) {
                return true;
            }
        }

        return false;
    }

    private boolean isHealingEffect(MobEffect effect) {
        return effect == MobEffects.HEAL
                || effect == MobEffects.REGENERATION
                || effect == MobEffects.HEALTH_BOOST
                || effect == MobEffects.ABSORPTION;
    }
    private boolean isHealingFood(ItemStack stack) {
        if (!stack.isEdible()) {
            return false;
        }

        FoodProperties properties = stack.getFoodProperties(this);
        return properties != null && properties.getNutrition() > 0;
    }


    private static class CompanionRandomStrollGoal extends WaterAvoidingRandomStrollGoal {

        private final CompanionEntity companion;

        CompanionRandomStrollGoal(CompanionEntity companion, double speedModifier) {
            super(companion, speedModifier);
            this.companion = companion;
        }

        @Override
        public boolean canUse() {
            if (this.companion.isOrderedToSit() && this.companion.getStayPos() == null) {
                return false;
            }
            return super.canUse();
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            if (this.companion.isOrderedToSit()) {
                BlockPos stayPos = this.companion.getStayPos();
                if (stayPos != null) {
                    for (int attempt = 0; attempt < 8; attempt++) {
                        int offsetX = this.companion.getRandom().nextIntBetweenInclusive(-STAY_PATROL_RADIUS, STAY_PATROL_RADIUS);
                        int offsetZ = this.companion.getRandom().nextIntBetweenInclusive(-STAY_PATROL_RADIUS, STAY_PATROL_RADIUS);
                        int offsetY = this.companion.getRandom().nextIntBetweenInclusive(-1, 1);
                        BlockPos targetPos = stayPos.offset(offsetX, offsetY, offsetZ);
                        double distanceSq = targetPos.distSqr(stayPos);
                        if (distanceSq <= (double) (STAY_PATROL_RADIUS * STAY_PATROL_RADIUS)) {
                            return Vec3.atCenterOf(targetPos);
                        }
                    }
                    return Vec3.atCenterOf(stayPos);
                }
            }
            return super.getPosition();
        }
    }


}
