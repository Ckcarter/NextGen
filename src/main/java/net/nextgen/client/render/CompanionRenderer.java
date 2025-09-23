package net.nextgen.client.render;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
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


    private final CompanionModel defaultModel;
    private final CompanionModel slimModel;

    public CompanionRenderer(EntityRendererProvider.Context context) {

        this(context, new CompanionModel(context.bakeLayer(ModelLayers.PLAYER), false));
    }

    private CompanionRenderer(EntityRendererProvider.Context context, CompanionModel defaultModel) {
        super(context, defaultModel, 0.5F);
        this.defaultModel = defaultModel;
        this.slimModel = new CompanionModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);


//        PlayerModel<CompanionEntity> innerArmorModel =
//                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR), false);
//        PlayerModel<CompanionEntity> outerArmorModel =
//                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR), false);
      //  this.addLayer(new HumanoidArmorLayer<>(this, innerArmorModel, outerArmorModel));
    }


    @Override
    public void render(CompanionEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        this.model = CompanionSkinManager.usesSlimModel(entity) ? this.slimModel : this.defaultModel;
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }



    @Override
    public ResourceLocation getTextureLocation(CompanionEntity entity) {
        return CompanionSkinManager.getSkinLocation(entity);
    }
}
