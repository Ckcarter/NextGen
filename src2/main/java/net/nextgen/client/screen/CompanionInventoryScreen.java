package net.nextgen.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.nextgen.menu.CompanionInventoryMenu;


public class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft",
            "textures/gui/container/generic_54.png");



    private static final int SLOT_SPACING = 18;
    private static final int EQUIPMENT_SLOT_COUNT = 4;
    private static final int EQUIPMENT_SLOT_START_X = 178;
    private static final int EQUIPMENT_SLOT_START_Y = 18;


    public CompanionInventoryScreen(CompanionInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 202;
        this.imageHeight = 114 + menu.getCompanionRowCount() * SLOT_SPACING;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, 176, this.imageHeight);
        this.renderEquipmentBackgrounds(guiGraphics, x, y);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    private void renderEquipmentBackgrounds(GuiGraphics guiGraphics, int left, int top) {
        for (int index = 0; index < EQUIPMENT_SLOT_COUNT; index++) {
            int slotX = left + EQUIPMENT_SLOT_START_X;
            int slotY = top + EQUIPMENT_SLOT_START_Y + index * SLOT_SPACING;
            guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF373737);
        }
    }

}