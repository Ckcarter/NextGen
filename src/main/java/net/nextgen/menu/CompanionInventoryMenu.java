package net.nextgen.menu;

import net.minecraft.network.FriendlyByteBuf;
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
    private final CompanionEntity companion;
    private final SimpleContainer companionInventory;

    public CompanionInventoryMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, resolveCompanion(inventory, data));
    }

    public CompanionInventoryMenu(int windowId, Inventory inventory, CompanionEntity companion) {
        super(ModMenuTypes.COMPANION_INVENTORY.get(), windowId);
        this.companion = companion;
        this.companionInventory = companion.getInventory();
        checkContainerSize(this.companionInventory, CompanionEntity.INVENTORY_SIZE);
        this.companionInventory.startOpen(inventory.player);

        this.addCompanionSlots();
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
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slotIndex = column + row * 3;
                int x = 62 + column * 18;
                int y = 17 + row * 18;
                this.addSlot(new Slot(this.companionInventory, slotIndex, x, y));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = column + row * 9 + HOTBAR_SLOT_COUNT;
                int x = 8 + column * 18;
                int y = 84 + row * 18;
                this.addSlot(new Slot(playerInventory, slotIndex, x, y));
            }
        }

        for (int column = 0; column < 9; column++) {
            int x = 8 + column * 18;
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
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index < CompanionEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(stack, CompanionEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, CompanionEntity.INVENTORY_SIZE, false)) {
                return ItemStack.EMPTY;
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