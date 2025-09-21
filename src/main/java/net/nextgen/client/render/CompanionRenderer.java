package net.nextgen.client.render;


import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.nextgen.client.model.CompanionModel;
import net.nextgen.client.skin.CompanionSkinManager;
import net.nextgen.entity.custom.CompanionEntity;

@OnlyIn(Dist.CLIENT)
public class CompanionRenderer extends HumanoidMobRenderer<CompanionEntity, CompanionModel> {

    public CompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new CompanionModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        HumanoidModel<CompanionEntity> innerArmorModel =
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        HumanoidModel<CompanionEntity> outerArmorModel =
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
       // this.addLayer(new HumanoidArmorLayer<>(this, innerArmorModel, outerArmorModel));
    }

    @Override
    public ResourceLocation getTextureLocation(CompanionEntity entity) {
        return CompanionSkinManager.getSkinLocation(entity);
    }
}
