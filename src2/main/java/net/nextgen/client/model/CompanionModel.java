package net.nextgen.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.nextgen.entity.custom.CompanionEntity;

/**
 * Player-like model used for rendering {@link CompanionEntity} instances.
 */
public class CompanionModel extends HumanoidModel<CompanionEntity> {

    private final ModelPart leftSleeve;
    private final ModelPart rightSleeve;
    private final ModelPart leftPants;
    private final ModelPart rightPants;
   // private final ModelPart jacket;
    private final boolean slim;

    public CompanionModel(ModelPart root, boolean slim) {
        super(root);
        this.slim = slim;
        this.leftSleeve = root.getChild("left_arm");
        this.rightSleeve = root.getChild("right_arm");
        this.leftPants = root.getChild("left_leg");
        this.rightPants = root.getChild("right_leg");
        //this.jacket = root.getChild("jacket");
    }

    @Override
    public void setupAnim(CompanionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.copyLayerTransforms();
    }

    @Override
    public void prepareMobModel(CompanionEntity entity, float limbSwing, float limbSwingAmount,
                                float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        this.copyLayerTransforms();
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.leftSleeve.visible = visible;
        this.rightSleeve.visible = visible;
        this.leftPants.visible = visible;
        this.rightPants.visible = visible;
        //this.jacket.visible = visible;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftSleeve.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightSleeve.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftPants.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightPants.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        //this.jacket.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        ModelPart armPart = this.getArm(arm);
        if (this.slim) {
            float offset = 0.5F / 16.0F;
            poseStack.translate(arm == HumanoidArm.RIGHT ? offset : -offset, 0.0D, 0.0D);
        }
        armPart.translateAndRotate(poseStack);
    }

    private void copyLayerTransforms() {
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
       // this.jacket.copyFrom(this.body);
    }
}