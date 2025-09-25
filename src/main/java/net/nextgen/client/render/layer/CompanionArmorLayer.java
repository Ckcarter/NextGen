package net.nextgen.client.render.layer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.nextgen.client.model.CompanionModel;
import net.nextgen.entity.custom.CompanionEntity;

/**
 * Armor layer that mirrors the player renderer logic so companions display
 * their equipped armor pieces.
 */
public class CompanionArmorLayer extends HumanoidArmorLayer<CompanionEntity, CompanionModel, HumanoidModel<CompanionEntity>> {

    public CompanionArmorLayer(RenderLayerParent<CompanionEntity, CompanionModel> parent,
                               EntityModelSet modelSet) {
        super(parent,
                new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)));
    }
}