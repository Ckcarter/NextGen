package net.nextgen.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;
import net.nextgen.entity.custom.CompanionEntity;

public class CompanionInventoryMenu extends AbstractContainerMenu {

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int COMPANION_ROWS = 3;
    private static final int COMPANION_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int SLOT_SPACING = 18;
    private static final int COMPANION_SLOT_START_X = 8;
    private static final int COMPANION_SLOT_START_Y = 18;
    private static final EquipmentSlot[] EQUIPMENT_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final int EQUIPMENT_SLOT_START_X = 178;
    private static final int EQUIPMENT_SLOT_START_Y = COMPANION_SLOT_START_Y;
    private static final int COMPANION_SLOT_START = 0;
    private static final int COMPANION_SLOT_END = CompanionEntity.INVENTORY_SIZE;
    private static final int EQUIPMENT_SLOT_START = COMPANION_SLOT_END;
    private static final int EQUIPMENT_SLOT_COUNT = EQUIPMENT_SLOTS.length;
    private static final int EQUIPMENT_SLOT_END = EQUIPMENT_SLOT_START + EQUIPMENT_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_ROWS * 9;
    private static final int PLAYER_INVENTORY_START = EQUIPMENT_SLOT_END;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + HOTBAR_SLOT_COUNT;

    private final CompanionEntity companion;
    private final SimpleContainer companionInventory;
    private final CompanionEquipmentContainer equipmentContainer;

    public CompanionInventoryMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, resolveCompanion(inventory, data));
    }

    public CompanionInventoryMenu(int windowId, Inventory inventory, CompanionEntity companion) {
        super(ModMenuTypes.COMPANION_INVENTORY.get(), windowId);
        this.companion = companion;
        this.companionInventory = companion.getInventory();
        this.equipmentContainer = new CompanionEquipmentContainer(companion, EQUIPMENT_SLOTS);
        checkContainerSize(this.companionInventory, CompanionEntity.INVENTORY_SIZE);
        this.companionInventory.startOpen(inventory.player);
        this.equipmentContainer.startOpen(inventory.player);

        this.addCompanionSlots();
        this.addEquipmentSlots();
        this.addPlayerInventorySlots(inventory);
    }

    private static CompanionEntity resolveCompanion(Inventory inventory, FriendlyByteBuf data) {
        if (data == null) {
            throw new IllegalStateException("Companion inventory data was not provided");
        }
        Level level = inventory.player.level();
        if (level == null) {
            throw new IllegalStateException("Player level is not available");
        }
        int entityId = data.readVarInt();
        if (level.getEntity(entityId) instanceof CompanionEntity companion) {
            return companion;
        }
        throw new IllegalStateException("Entity " + entityId + " is not a companion");
    }

    private void addCompanionSlots() {
        for (int row = 0; row < COMPANION_ROWS; row++) {
            for (int column = 0; column < COMPANION_COLUMNS; column++) {
                int slotIndex = column + row * COMPANION_COLUMNS;
                int x = COMPANION_SLOT_START_X + column * SLOT_SPACING;
                int y = COMPANION_SLOT_START_Y + row * SLOT_SPACING;
                this.addSlot(new Slot(this.companionInventory, slotIndex, x, y));
            }
        }
    }

    private void addEquipmentSlots() {
        for (int index = 0; index < EQUIPMENT_SLOT_COUNT; index++) {
            EquipmentSlot slot = EQUIPMENT_SLOTS[index];
            int x = EQUIPMENT_SLOT_START_X;
            int y = EQUIPMENT_SLOT_START_Y + index * SLOT_SPACING;
            this.addSlot(new CompanionEquipmentSlot(this.equipmentContainer, index, x, y, slot));
        }
    }



    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = column + row * 9 + HOTBAR_SLOT_COUNT;
                int x = 8 + column * SLOT_SPACING;
                int y = 84 + row * SLOT_SPACING;
                this.addSlot(new Slot(playerInventory, slotIndex, x, y));
            }
        }

        for (int column = 0; column < 9; column++) {
            int x = 8 + column * SLOT_SPACING;
            int y = 142;
            this.addSlot(new Slot(playerInventory, column, x, y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.companion.isAlive() && this.companion.isOwnedBy(player)
                && player.distanceToSqr(this.companion) <= 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.companionInventory.stopOpen(player);
        this.equipmentContainer.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index < COMPANION_SLOT_END) {
                if (!this.moveItemStackTo(stack, EQUIPMENT_SLOT_START, EQUIPMENT_SLOT_END, false)
                        && !this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < EQUIPMENT_SLOT_END) {
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)
                        && !this.moveItemStackTo(stack, COMPANION_SLOT_START, COMPANION_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, EQUIPMENT_SLOT_START, EQUIPMENT_SLOT_END, false)) {
                    if (index < PLAYER_INVENTORY_END) {
                        if (!this.moveItemStackTo(stack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)
                                && !this.moveItemStackTo(stack, COMPANION_SLOT_START, COMPANION_SLOT_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index < PLAYER_HOTBAR_END) {
                        if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)
                                && !this.moveItemStackTo(stack, COMPANION_SLOT_START, COMPANION_SLOT_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
    }
}