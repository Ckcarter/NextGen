package net.nextgen.client.render.layer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
//import net.minecraft.client.renderer.entity.layers.RenderLayerParent;
import net.nextgen.client.model.CompanionModel;
import net.nextgen.entity.custom.CompanionEntity;

/**
 * Armor layer that uses the player armor models so equipped armor renders on the companion.
 */
public class CompanionArmorLayer extends HumanoidArmorLayer<CompanionEntity, CompanionModel, HumanoidModel<CompanionEntity>> {

    public CompanionArmorLayer(RenderLayerParent<CompanionEntity, CompanionModel> renderer,
                               EntityRendererProvider.Context context) {
        super(renderer,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)));
    }
}