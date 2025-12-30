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
import net.nextgen.client.cape.CompanionCapeCache;
import net.nextgen.client.model.CompanionModel;
import net.nextgen.entity.custom.CompanionEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Mojang-style cape for the Companion entity.
 *
 * Key points for smooth "Mojang cape" motion:
 *  - Uses a cloak-chasing position (companion.capeX/Y/Z) updated client-side each tick
 *  - Uses partialTick interpolation for all moving values
 *  - Uses vanilla-like clamping and rotation math
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

        ResourceLocation capeTex = CompanionCapeCache.getOrCreate(entity, entity.getCapeSourceUuid(), entity.getCapeVersion());
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entitySolid(capeTex));

        poseStack.pushPose();

        // Attach to body (same as player cape layer does)
        this.getParentModel().body.translateAndRotate(poseStack);

        // Move cape slightly toward the back to avoid z-fighting with body
        // (negative Z brings it closer to the model's back in this local space)
        poseStack.translate(0.0D, 0.0D, 0.15D);

        // === Vanilla-ish cape physics (smooth) ===
        float pt = partialTick;

        // Interpolated "chasing cloak" position vs real position
        double chaseX = Mth.lerp(pt, entity.capeXO, entity.capeX);
        double chaseY = Mth.lerp(pt, entity.capeYO, entity.capeY);
        double chaseZ = Mth.lerp(pt, entity.capeZO, entity.capeZ);

        double x = Mth.lerp(pt, entity.xo, entity.getX());
        double y = Mth.lerp(pt, entity.yo, entity.getY());
        double z = Mth.lerp(pt, entity.zo, entity.getZ());

        double dx = chaseX - x;
        double dy = chaseY - y;
        double dz = chaseZ - z;

        float bodyRot = Mth.lerp(pt, entity.yBodyRotO, entity.yBodyRot);
        float sin = Mth.sin(bodyRot * ((float)Math.PI / 180F));
        float cos = -Mth.cos(bodyRot * ((float)Math.PI / 180F));

        float lift = (float)dy * 10.0F;
        lift = Mth.clamp(lift, -6.0F, 32.0F);

        float swingZ = (float)(dx * sin + dz * cos) * 100.0F;
        swingZ = Mth.clamp(swingZ, 0.0F, 150.0F);

        float swingX = (float)(dx * cos - dz * sin) * 100.0F;

        // Add walk bob (LivingEntity has walkDist)
        float walk = Mth.lerp(pt, entity.walkDistO, entity.walkDist);
        float bob = Mth.sin(walk * 6.0F) * 32.0F * limbSwingAmount;
        swingZ += bob;

        // Crouch adjustment like vanilla
        if (entity.isCrouching()) {
            lift += 25.0F;
        }

        // Apply rotations (vanilla order feel)
        poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + (swingZ / 2.0F) + lift));
        poseStack.mulPose(Axis.ZP.rotationDegrees(swingX / 2.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // === Render a thin cape box (Mojang-like thickness) ===
        // Local cape dimensions (tune if needed)
        float w = 10.0F / 16.0F;   // ~0.625 blocks wide in model space
        float h = 16.0F / 16.0F;   // 1.0
        float d = 1.0F / 16.0F;    // 1px thickness

        float halfW = w * 0.5F;

        // Vanilla cape cloth UV region: top-left 10x16 of a 64x32 cape texture.
        float U0 = 0.0F;
        float V0 = 0.0F;
        float U1 = 10.0F / 64.0F;
        float V1 = 16.0F / 32.0F;

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // Front face (visible outer cloth)
        quad(vc, pose, normal,
                -halfW, 0.0F, 0.0F,
                 halfW, 0.0F, 0.0F,
                 halfW, h,    0.0F,
                -halfW, h,    0.0F,
                U0, V0, U1, V1,
                packedLight,
                0.0F, 0.0F, 1.0F);

        // Back face (inner)
        quad(vc, pose, normal,
                -halfW, 0.0F, -d,
                -halfW, h,    -d,
                 halfW, h,    -d,
                 halfW, 0.0F, -d,
                U1, V0, U0, V1,
                packedLight,
                0.0F, 0.0F, -1.0F);

        // Left side
        quad(vc, pose, normal,
                -halfW, 0.0F, -d,
                -halfW, 0.0F, 0.0F,
                -halfW, h,    0.0F,
                -halfW, h,    -d,
                U0, V0, U1, V1,
                packedLight,
                -1.0F, 0.0F, 0.0F);

        // Right side
        quad(vc, pose, normal,
                 halfW, 0.0F, 0.0F,
                 halfW, 0.0F, -d,
                 halfW, h,    -d,
                 halfW, h,    0.0F,
                U0, V0, U1, V1,
                packedLight,
                1.0F, 0.0F, 0.0F);

        // Top
        quad(vc, pose, normal,
                -halfW, 0.0F, -d,
                 halfW, 0.0F, -d,
                 halfW, 0.0F, 0.0F,
                -halfW, 0.0F, 0.0F,
                U0, V0, U1, V1,
                packedLight,
                0.0F, -1.0F, 0.0F);

        // Bottom
        quad(vc, pose, normal,
                -halfW, h,    0.0F,
                 halfW, h,    0.0F,
                 halfW, h,    -d,
                -halfW, h,    -d,
                U0, V0, U1, V1,
                packedLight,
                0.0F, 1.0F, 0.0F);

        poseStack.popPose();
    }

    private static void quad(VertexConsumer vc,
                             Matrix4f pose, Matrix3f normalMat,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float u0, float v0, float u1, float v1,
                             int light,
                             float nx, float ny, float nz) {

        // NOTE: the winding order here is consistent for our normals
        vertex(vc, pose, normalMat, x0, y0, z0, u0, v0, light, nx, ny, nz);
        vertex(vc, pose, normalMat, x1, y1, z1, u1, v0, light, nx, ny, nz);
        vertex(vc, pose, normalMat, x2, y2, z2, u1, v1, light, nx, ny, nz);
        vertex(vc, pose, normalMat, x3, y3, z3, u0, v1, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc,
                               Matrix4f pose, Matrix3f normalMat,
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
