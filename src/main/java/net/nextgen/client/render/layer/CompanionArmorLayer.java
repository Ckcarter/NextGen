package net.nextgen.client.render.layer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.nextgen.client.model.CompanionModel;
import net.nextgen.client.skin.CompanionSkinManager;
import net.nextgen.entity.custom.CompanionEntity;

/**
 * Armor layer that mirrors the vanilla player implementation but swaps the
 * baked armor models when the companion is using a slim skin.
 */
@OnlyIn(Dist.CLIENT)
public class CompanionArmorLayer extends HumanoidArmorLayer<CompanionEntity, CompanionModel, HumanoidModel<CompanionEntity>> {

    private final HumanoidModel<CompanionEntity> slimInnerModel;
    private final HumanoidModel<CompanionEntity> slimOuterModel;

    public CompanionArmorLayer(RenderLayerParent<CompanionEntity, CompanionModel> parent,
                               EntityModelSet modelSet) {
        this(parent,
                new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                bakeSlimModel(modelSet, ModelLayers.PLAYER_SLIM_INNER_ARMOR, ModelLayers.PLAYER_INNER_ARMOR),
                bakeSlimModel(modelSet, ModelLayers.PLAYER_SLIM_OUTER_ARMOR, ModelLayers.PLAYER_OUTER_ARMOR));
    }

    private CompanionArmorLayer(RenderLayerParent<CompanionEntity, CompanionModel> parent,
                                HumanoidModel<CompanionEntity> defaultInnerModel,
                                HumanoidModel<CompanionEntity> defaultOuterModel,
                                HumanoidModel<CompanionEntity> slimInnerModel,
                                HumanoidModel<CompanionEntity> slimOuterModel) {
        super(parent, defaultInnerModel, defaultOuterModel);
        this.slimInnerModel = slimInnerModel;
        this.slimOuterModel = slimOuterModel;
    }

    @Override
    protected HumanoidModel<CompanionEntity> getArmorModelHook(CompanionEntity entity, ItemStack stack,
                                                               EquipmentSlot slot, HumanoidModel<CompanionEntity> original) {
        HumanoidModel<CompanionEntity> baseModel = (HumanoidModel<CompanionEntity>) super.getArmorModelHook(entity, stack, slot, original);
        if (!CompanionSkinManager.usesSlimModel(entity)) {
            return baseModel;
        }

        HumanoidModel<CompanionEntity> slimModel = slot == EquipmentSlot.LEGS ? this.slimInnerModel : this.slimOuterModel;
        baseModel.copyPropertiesTo(slimModel);
        return slimModel;
    }

    private static HumanoidModel<CompanionEntity> bakeSlimModel(EntityModelSet modelSet,
                                                                ModelLayerLocation location,
                                                                ModelLayerLocation fallback) {
        ModelPart part;
        try {
            part = modelSet.bakeLayer(location);
        } catch (IllegalArgumentException ignored) {
            part = modelSet.bakeLayer(fallback);
        }
        return new HumanoidModel<>(part);
    }
}