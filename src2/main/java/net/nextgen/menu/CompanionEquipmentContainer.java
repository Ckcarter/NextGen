package net.nextgen.menu;

import java.util.Arrays;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.nextgen.entity.custom.CompanionEntity;

/**
 * Container that exposes a {@link CompanionEntity}'s equipped items so they can be
 * manipulated from menus.
 */
public class CompanionEquipmentContainer implements Container {

    private final CompanionEntity companion;
    private final EquipmentSlot[] slots;

    public CompanionEquipmentContainer(CompanionEntity companion, EquipmentSlot... slots) {
        this.companion = companion;
        this.slots = Arrays.copyOf(slots, slots.length);
    }

    @Override
    public int getContainerSize() {
        return this.slots.length;
    }

    @Override
    public boolean isEmpty() {
        for (EquipmentSlot slot : this.slots) {
            if (!this.companion.getItemBySlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        EquipmentSlot slot = this.slots[index];
        return this.companion.getItemBySlot(slot);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack current = this.getItem(index);
        if (current.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        EquipmentSlot slot = this.slots[index];
        if (count >= current.getCount()) {
            ItemStack removed = current.copy();
            this.companion.setItemSlot(slot, ItemStack.EMPTY);
            return removed;
        }
        ItemStack removed = current.copy();
        removed.setCount(count);
        ItemStack remaining = current.copy();
        remaining.shrink(count);
        this.companion.setItemSlot(slot, remaining);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack current = this.getItem(index);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        EquipmentSlot slot = this.slots[index];
        ItemStack removed = current.copy();
        this.companion.setItemSlot(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        EquipmentSlot slot = this.slots[index];
        ItemStack copy = stack.copy();
        if (!copy.isEmpty()) {
            copy.setCount(1);
        }
        this.companion.setItemSlot(slot, copy);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return this.companion.isAlive() && this.companion.isOwnedBy(player)
                && player.distanceToSqr(this.companion) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        EquipmentSlot slot = this.slots[index];
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            return stack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == slot;
        }
        return false;
    }

    @Override
    public void startOpen(Player player) {
    }

    @Override
    public void stopOpen(Player player) {
    }

    @Override
    public void clearContent() {
        // The companion keeps equipment items even when the menu closes.
    }
}