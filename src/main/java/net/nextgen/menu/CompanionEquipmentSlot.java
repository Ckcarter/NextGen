package net.nextgen.menu;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Slot implementation that renders the appropriate empty armor icon for
 * a {@link CompanionEquipmentContainer} entry.
 */
public class CompanionEquipmentSlot extends Slot {

    private static final ResourceLocation EMPTY_HELMET_ICON =
            new ResourceLocation("item/empty_armor_slot_helmet");
    private static final ResourceLocation EMPTY_CHEST_ICON =
            new ResourceLocation("item/empty_armor_slot_chestplate");
    private static final ResourceLocation EMPTY_LEGGINGS_ICON =
            new ResourceLocation("item/empty_armor_slot_leggings");
    private static final ResourceLocation EMPTY_BOOTS_ICON =
            new ResourceLocation("item/empty_armor_slot_boots");

    private final EquipmentSlot equipmentSlot;

    public CompanionEquipmentSlot(Container container, int index, int x, int y, EquipmentSlot equipmentSlot) {
        super(container, index, x, y);
        this.equipmentSlot = equipmentSlot;
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        ResourceLocation icon = switch (this.equipmentSlot) {
            case HEAD -> EMPTY_HELMET_ICON;
            case CHEST -> EMPTY_CHEST_ICON;
            case LEGS -> EMPTY_LEGGINGS_ICON;
            case FEET -> EMPTY_BOOTS_ICON;
            default -> null;
        };
        return icon == null ? null : Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
    }
}
