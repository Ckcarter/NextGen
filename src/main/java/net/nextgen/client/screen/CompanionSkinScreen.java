package net.nextgen.client.screen;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.nextgen.entity.custom.CompanionEntity;
import net.nextgen.menu.CompanionSkinMenu;
import net.nextgen.network.ModMessages;
import net.nextgen.network.SetCompanionSkinC2SPacket;
import net.nextgen.network.UnsummonCompanionC2SPacket;

public class CompanionSkinScreen extends AbstractContainerScreen<CompanionSkinMenu> {

    private EditBox skinInput;
    private Button saveButton;
    private Button clearButton;
    private Button unsummonButton;

    public CompanionSkinScreen(CompanionSkinMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 140;
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) {
            this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        }
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 8;
        this.inventoryLabelX = 0;
        this.inventoryLabelY = 0;

        int inputX = this.leftPos + 20;
        int inputY = this.topPos + 42;
        this.skinInput = new EditBox(this.font, inputX, inputY, 136, 20,
                Component.translatable("screen.nextgen.companion_skin.input"));
        CompanionEntity entity = this.getEntity();
        if (entity != null) {
            this.skinInput.setValue(entity.getSkinName());
        }
        this.skinInput.setResponder(value -> this.updateButtonStates());
        this.skinInput.setMaxLength(64);
        this.addRenderableWidget(this.skinInput);
        this.setInitialFocus(this.skinInput);

        int leftButtonX = this.leftPos + 20;
        int rightButtonX = this.leftPos + this.imageWidth - 20 - 64;
        int topRowY = this.topPos + 72;
        int bottomRowY = this.topPos + 100;

        this.saveButton = Button.builder(Component.translatable("screen.nextgen.companion_skin.save"), button -> this.saveSkin())
                .bounds(leftButtonX, topRowY, 64, 20)
                .build();
        this.addRenderableWidget(this.saveButton);

        Button cancelButton = Button.builder(Component.translatable("screen.nextgen.companion_skin.cancel"), button -> this.onClose())
                .bounds(rightButtonX, topRowY, 64, 20)
                .build();
        this.addRenderableWidget(cancelButton);

        this.clearButton = Button.builder(Component.translatable("screen.nextgen.companion_skin.clear"), button -> this.clearSkin())
                .bounds(leftButtonX, bottomRowY, 64, 20)
                .build();
        this.addRenderableWidget(this.clearButton);

        this.unsummonButton = Button.builder(Component.translatable("screen.nextgen.companion_skin.unsummon"), button -> this.unsummon())
                .bounds(rightButtonX, bottomRowY, 64, 20)
                .build();
        this.addRenderableWidget(this.unsummonButton);

        this.updateButtonStates();
    }

    @Override
    public void removed() {
        super.removed();
        if (this.minecraft != null) {
            this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.skinInput != null) {
            this.skinInput.tick();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.skinInput != null && (this.skinInput.keyPressed(keyCode, scanCode, modifiers)
                || this.skinInput.canConsumeInput())) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.saveSkin();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xC0101010);
        guiGraphics.fill(x + 10, y + 28, x + this.imageWidth - 10, y + this.imageHeight - 10, 0xFF202020);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, this.titleLabelY, 0xFFFFFF);
        CompanionEntity entity = this.getEntity();
        if (entity != null) {
            guiGraphics.drawCenteredString(this.font, entity.getDisplayName(), this.imageWidth / 2, 24, 0xFFD0D0);
            String current = entity.getSkinName().isBlank() ? "-" : entity.getSkinName();
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("screen.nextgen.companion_skin.current", current), this.imageWidth / 2, 56,
                    0xFFA0A0);
        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.nextgen.companion_skin.missing"),
                    this.imageWidth / 2, 24, 0xFFD0D0);
        }
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("screen.nextgen.companion_skin.instructions"), this.imageWidth / 2, 32, 0xFFA0A0);
    }

    private CompanionEntity getEntity() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        return this.minecraft.level.getEntity(this.menu.getEntityId()) instanceof CompanionEntity companion ? companion : null;
    }

    private void saveSkin() {
        if (this.minecraft == null) {
            return;
        }
        ModMessages.sendToServer(new SetCompanionSkinC2SPacket(this.menu.getEntityId(), this.skinInput.getValue()));
        if (this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
    }

    private void clearSkin() {
        if (this.minecraft == null) {
            return;
        }
        ModMessages.sendToServer(new SetCompanionSkinC2SPacket(this.menu.getEntityId(), ""));
        if (this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
    }

    private void unsummon() {
        if (this.minecraft == null) {
            return;
        }
        ModMessages.sendToServer(new UnsummonCompanionC2SPacket(this.menu.getEntityId()));
        if (this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
    }

    private void updateButtonStates() {
        boolean hasEntity = this.getEntity() != null;
        String value = this.skinInput != null ? this.skinInput.getValue().trim() : "";
        if (this.saveButton != null) {
            this.saveButton.active = !value.isEmpty();
        }
        if (this.clearButton != null) {
            this.clearButton.active = !value.isEmpty();
        }
        if (this.unsummonButton != null) {
            this.unsummonButton.active = hasEntity;
        }
    }
}