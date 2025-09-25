
package net.nextgen.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.nextgen.client.model.CompanionModel;
import net.nextgen.client.skin.CompanionSkinManager;
import net.nextgen.entity.custom.CompanionEntity;

/**
 * Armor layer that mirrors the player renderer logic while respecting
 * whether the companion uses the slim player model. The base
 * {@link HumanoidArmorLayer} is re-used but gated so that only one of the
 * slim or classic models will render for a given entity.
 */
public class CompanionArmorLayer extends HumanoidArmorLayer<CompanionEntity, CompanionModel, CompanionModel> {

    private final boolean slimModel;

    public CompanionArmorLayer(RenderLayerParent<CompanionEntity, CompanionModel> parent,
                               EntityModelSet modelSet,
                               boolean slimModel) {
        super(parent,
                createModel(modelSet, slimModel, true),
                createModel(modelSet, slimModel, false));
        this.slimModel = slimModel;
    }

    private static CompanionModel createModel(EntityModelSet modelSet, boolean slim, boolean inner) {
        return new CompanionModel(modelSet.bakeLayer(inner
                ? (slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)
                : (slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)), slim);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, CompanionEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (CompanionSkinManager.usesSlimModel(entity) != this.slimModel) {
            return;
        }
        super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks,
                netHeadYaw, headPitch);
    }
}