package net.nextgen.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.nextgen.client.model.CompanionModel;
import net.nextgen.compat.CapeCompat;              // ✅ add this
import net.nextgen.entity.custom.CompanionEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Simple, vanilla-style cape rendered on the Companion entity.
 *
 * Renders a 1px-thick cape box (Mojang-like thickness) attached to the body.
 */
public class CompanionCapeLayer extends RenderLayer<CompanionEntity, CompanionModel> {

    public CompanionCapeLayer(RenderLayerParent<CompanionEntity, CompanionModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       CompanionEntity entity,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTick,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {

        if (entity.isInvisible()) return;

        // ✅ Use compat: CraftHeraldry cape if present, otherwise fallback cape texture
        ResourceLocation capeTex = CapeCompat.getCompanionCapeTexture(entity);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entitySolid(capeTex));

        poseStack.pushPose();

        // Follow body rotations so it stays attached to the back.
        this.getParentModel().body.translateAndRotate(poseStack);

        // Push the cape slightly backward so it doesn't z-fight the body.
        poseStack.translate(0.0D, 0.0D, 0.125D);

        // A small default tilt backward, plus a tiny walk sway.
        float sway = Mth.sin(ageInTicks * 0.1F) * 2.0F;
        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F) * 10.0F;
        poseStack.mulPose(Axis.XP.rotationDegrees(10.0F + sway + walk));

        // Cape size (model units; 1 unit = 1/16 block)
        float halfW = (10.0F / 16.0F) / 2.0F; // 10px wide
        float h = 16.0F / 16.0F;             // 16px tall
        float d = 1.0F / 16.0F;              // 1px thick

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // UVs: whole texture (simple)
        float u0 = 0.0F, v0 = 0.0F, u1 = 1.0F, v1 = 1.0F;

        // Thickness: front at z=0, back at z=-d
        float zFront = 0.0F;
        float zBack = -d;

        // ✅ NOTE: every quad call must include packedLight

        // Front face (+Z)
        quad(vc, pose, normal,
                -halfW, 0.0F,  zFront,
                halfW, 0.0F,  zFront,
                halfW, h,     zFront,
                -halfW, h,     zFront,
                u0, v0, u1, v1,
                packedLight,
                0.0F, 0.0F, 1.0F);

        // Back face (-Z) (reverse winding)
        quad(vc, pose, normal,
                -halfW, h,     zBack,
                halfW, h,     zBack,
                halfW, 0.0F,  zBack,
                -halfW, 0.0F,  zBack,
                u0, v0, u1, v1,
                packedLight,
                0.0F, 0.0F, -1.0F);

        // Left face (-X)
        quad(vc, pose, normal,
                -halfW, 0.0F,  zBack,
                -halfW, 0.0F,  zFront,
                -halfW, h,     zFront,
                -halfW, h,     zBack,
                u0, v0, u1, v1,
                packedLight,
                -1.0F, 0.0F, 0.0F);

        // Right face (+X)
        quad(vc, pose, normal,
                halfW, 0.0F,  zFront,
                halfW, 0.0F,  zBack,
                halfW, h,     zBack,
                halfW, h,     zFront,
                u0, v0, u1, v1,
                packedLight,
                1.0F, 0.0F, 0.0F);

        // Top face (-Y)
        quad(vc, pose, normal,
                -halfW, 0.0F,  zBack,
                halfW, 0.0F,  zBack,
                halfW, 0.0F,  zFront,
                -halfW, 0.0F,  zFront,
                u0, v0, u1, v1,
                packedLight,
                0.0F, -1.0F, 0.0F);

        // Bottom face (+Y)
        quad(vc, pose, normal,
                -halfW, h,     zFront,
                halfW, h,     zFront,
                halfW, h,     zBack,
                -halfW, h,     zBack,
                u0, v0, u1, v1,
                packedLight,
                0.0F, 1.0F, 0.0F);

        // ❌ Removed the extra flat "back face" at z=0 (it caused z-fighting)

        poseStack.popPose();
    }

    private static void quad(VertexConsumer vc,
                             Matrix4f pose,
                             Matrix3f normalMat,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float u0, float v0, float u1, float v1,
                             int light,
                             float nx, float ny, float nz) {

        vertex(vc, pose, normalMat, x0, y0, z0, u0, v0, light, nx, ny, nz);
        vertex(vc, pose, normalMat, x1, y1, z1, u1, v0, light, nx, ny, nz);
        vertex(vc, pose, normalMat, x2, y2, z2, u1, v1, light, nx, ny, nz);
        vertex(vc, pose, normalMat, x3, y3, z3, u0, v1, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc,
                               Matrix4f pose,
                               Matrix3f normalMat,
                               float x, float y, float z,
                               float u, float v,
                               int light,
                               float nx, float ny, float nz) {

        vc.vertex(pose, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMat, nx, ny, nz)
                .endVertex();
    }
}
