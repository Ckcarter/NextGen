package net.nextgen.client.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.nextgen.client.model.CompanionModel;
import net.nextgen.entity.custom.CompanionEntity;

/**
 * Armor layer for the {@link CompanionEntity} that reuses the vanilla player
 * armor models so equipped armor renders correctly on the companion.
 */
public class CompanionArmorLayer extends HumanoidArmorLayer<CompanionEntity, CompanionModel, HumanoidModel<CompanionEntity>> {

    public CompanionArmorLayer(CompanionRenderer renderer, EntityModelSet modelSet) {
        super(renderer,
                new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)));
    }
}