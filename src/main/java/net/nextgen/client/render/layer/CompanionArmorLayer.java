package net.nextgen.client.render.layer;

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
public class CompanionArmorLayer extends HumanoidArmorLayer<CompanionEntity, PlayerModel<CompanionEntity>, CompanionModel> {

    private static final ModelLayerLocation SLIM_INNER_ARMOR =
            new ModelLayerLocation(new ResourceLocation("minecraft", "player_slim_inner_armor"), "main");
    private static final ModelLayerLocation SLIM_OUTER_ARMOR =
            new ModelLayerLocation(new ResourceLocation("minecraft", "player_slim_outer_armor"), "main");

    private final PlayerModel<CompanionEntity> slimInnerModel;
    private final PlayerModel<CompanionEntity> slimOuterModel;

    public CompanionArmorLayer(RenderLayerParent<CompanionEntity, CompanionModel> parent,
                               EntityModelSet modelSet) {
        super(parent,
                new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR), false),
                new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR), false));
        this.slimInnerModel = bakeSlimModel(modelSet, SLIM_INNER_ARMOR, ModelLayers.PLAYER_INNER_ARMOR);
        this.slimOuterModel = bakeSlimModel(modelSet, SLIM_OUTER_ARMOR, ModelLayers.PLAYER_OUTER_ARMOR);
    }

    @Override
    protected PlayerModel<CompanionEntity> getArmorModelHook(CompanionEntity entity, ItemStack stack,
                                                             EquipmentSlot slot, PlayerModel<CompanionEntity> original) {
        if (!CompanionSkinManager.usesSlimModel(entity)) {
            return super.getArmorModelHook(entity, stack, slot, original);
        }
        return slot == EquipmentSlot.LEGS ? this.slimInnerModel : this.slimOuterModel;
    }

    private static PlayerModel<CompanionEntity> bakeSlimModel(EntityModelSet modelSet,
                                                              ModelLayerLocation location,
                                                              ModelLayerLocation fallback) {
        ModelPart part;
        boolean slim = true;
        try {
            part = modelSet.bakeLayer(location);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            part = modelSet.bakeLayer(fallback);
            slim = false;
        }
        return new PlayerModel<>(part, slim);
    }
}