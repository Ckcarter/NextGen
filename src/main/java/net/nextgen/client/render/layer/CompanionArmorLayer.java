package net.nextgen.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.nextgen.client.model.CompanionModel;
import net.nextgen.client.skin.CompanionSkinManager;
import net.nextgen.entity.custom.CompanionEntity;

@OnlyIn(Dist.CLIENT)
public class CompanionArmorLayer extends RenderLayer<CompanionEntity, CompanionModel> {

    private final HumanoidArmorLayer<CompanionEntity, CompanionModel, PlayerModel<CompanionEntity>> defaultLayer;
    private final HumanoidArmorLayer<CompanionEntity, CompanionModel, PlayerModel<CompanionEntity>> slimLayer;

    public CompanionArmorLayer(RenderLayerParent<CompanionEntity, CompanionModel> parent, EntityModelSet modelSet) {
        super(parent);
        PlayerModel<CompanionEntity> defaultInner = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR), false);
        PlayerModel<CompanionEntity> defaultOuter = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR), false);
        PlayerModel<CompanionEntity> slimInner = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR), true);
        PlayerModel<CompanionEntity> slimOuter = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR), true);
        this.defaultLayer = new HumanoidArmorLayer<>(parent, defaultInner, defaultOuter);
        this.slimLayer = new HumanoidArmorLayer<>(parent, slimInner, slimOuter);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, CompanionEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (CompanionSkinManager.usesSlimModel(entity)) {
            this.slimLayer.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTick,
                    ageInTicks, netHeadYaw, headPitch);
        } else {
            this.defaultLayer.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTick,
                    ageInTicks, netHeadYaw, headPitch);
        }
    }
}
