package net.nextgen.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.nextgen.entity.custom.CompanionEntity;

public class CompanionSkinMenu extends AbstractContainerMenu {

    private final int entityId;

    public CompanionSkinMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        this(windowId, inventory, data.readVarInt());
    }

    public CompanionSkinMenu(int windowId, Inventory inventory, int entityId) {
        super(ModMenuTypes.COMPANION_SKIN.get(), windowId);
        this.entityId = entityId;
    }

    public int getEntityId() {
        return this.entityId;
    }

    @Override
    public boolean stillValid(Player player) {
        Level level = player.level();
        if (level == null) {
            return false;
        }
        return level.getEntity(this.entityId) instanceof CompanionEntity companion && companion.isAlive()
                && companion.isOwnedBy(player);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}