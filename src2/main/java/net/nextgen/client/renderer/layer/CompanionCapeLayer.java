package net.nextgen.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.nextgen.client.cape.CompanionCapeCache;
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
        ResourceLocation capeTex = CompanionCapeCache.getOrCreate(entity, entity.getCapeSourceUuid(), entity.getCapeVersion());
VertexConsumer vc = bufferSource.getBuffer(RenderType.entitySolid(capeTex));

        poseStack.pushPose();

        // Follow body rotations so it stays attached to the back.
        this.getParentModel().body.translateAndRotate(poseStack);

        // Vanilla crouch lift so cape doesn't clip
        if (entity.isCrouching()) {
            poseStack.translate(0.0F, 0.2F, 0.0F);
        }


        // Push the cape slightly backward so it doesn't z-fight the body.
        poseStack.translate(0.0D, 0.0D, 0.2D);

        // Vanilla-like cape physics:
// We keep a small client-side "cloak" position that chases the companion's position,
// then use the difference (cloak - body) to produce tilt and side-sway like the player cape.
CapePhysics.State s = CapePhysics.get(entity);
s.tick(entity);

double cloakX = Mth.lerp(partialTick, s.xo, s.x);
double cloakY = Mth.lerp(partialTick, s.yo, s.y);
double cloakZ = Mth.lerp(partialTick, s.zo, s.z);

double bodyX  = Mth.lerp(partialTick, entity.xo, entity.getX());
double bodyY  = Mth.lerp(partialTick, entity.yo, entity.getY());
double bodyZ  = Mth.lerp(partialTick, entity.zo, entity.getZ());

double dx = cloakX - bodyX;
double dy = cloakY - bodyY;
double dz = cloakZ - bodyZ;

float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
float sin = Mth.sin(bodyYaw * ((float)Math.PI / 180F));
float cos = -Mth.cos(bodyYaw * ((float)Math.PI / 180F));

float f1 = (float)(dx * sin + dz * cos) * 100.0F; // forward/back
float f2 = (float)(dx * cos - dz * sin) * 100.0F; // left/right
float f3 = (float)dy * 10.0F;                     // vertical

f1 = Mth.clamp(f1, -6.0F, 32.0F);
f2 = Mth.clamp(f2, -20.0F, 20.0F);
f3 = Mth.clamp(f3, -6.0F, 32.0F);

// Walking bob (uses the same limbSwing/amount values the renderer already gives us)
float walkBob = Mth.sin(limbSwing * 1.0F) * 32.0F * limbSwingAmount * 0.5F;

if (entity.isCrouching()) {
    f1 += 25.0F;
}

poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + f1 / 2.0F + f3 + walkBob));
poseStack.mulPose(Axis.ZP.rotationDegrees(f2 / 2.0F));
poseStack.mulPose(Axis.YP.rotationDegrees(0.0F - f2 / 2.0F));
// Cape size (model units; 1 unit = 1/16 block)
        float halfW = (10.0F / 16.0F) / 2.0F; // 10px wide
        float h = 16.0F / 16.0F;             // 16px tall
        float d = 1.0F / 16.0F;              // 1px thick

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        
// === VANILLA CAPE UV REGION (CraftHeraldry builds a 64x32 cape texture like Mojang) ===
// The actual cape cloth lives in the TOP-LEFT 10x16 pixels of the 64x32 texture.
float U0 = 0.0F;
float V0 = 0.0F;
float U1 = 10.0F / 64.0F; // 0.15625
float V1 = 16.0F / 32.0F; // 0.5

// === TUNING KNOBS ===
// ZOOM > 1.0 samples a smaller window of the cloth region, making the crest appear larger.
final float ZOOM = 1.00F; // final tuned size // slightly smaller crest
// Offset the sampling window inside the cloth region to center the crest on your companion cape.
// +X moves RIGHT, +Y moves DOWN (small values like +/-0.02)
final float OFF_X = 0.10F; // +1px right
final float OFF_Y = -1.955F;

float du = (U1 - U0) / ZOOM;
float dv = (V1 - V0) / ZOOM;

float uMid = (U0 + U1) * 0.5F + OFF_X * (U1 - U0);
float vMid = (V0 + V1) * 0.5F + OFF_Y * (V1 - V0);

float u0 = uMid - du * 0.5F;
float u1 = uMid + du * 0.5F;

// Flip horizontally (mirror crest)
float tmpU = u0;
u0 = u1;
u1 = tmpU;
float v0 = vMid - dv * 0.5F;
float v1 = vMid + dv * 0.5F;
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

/**
 * Minimal client-side "cloak" position cache to emulate AbstractClientPlayer's cloak/chasing behavior.
 * This lets non-player entities get vanilla-like cape motion without needing player-only fields.
 */
private static final class CapePhysics {
    private static final java.util.Map<java.util.UUID, State> STATES = new java.util.concurrent.ConcurrentHashMap<>();

    static State get(CompanionEntity e) {
        return STATES.computeIfAbsent(e.getUUID(), id -> new State(e.getX(), e.getY(), e.getZ()));
    }

    static final class State {
        double x, y, z;
        double xo, yo, zo;

        State(double x, double y, double z) {
            this.x = this.xo = x;
            this.y = this.yo = y;
            this.z = this.zo = z;
        }

        void tick(CompanionEntity e) {
            // store previous
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;

            // chase current position with smoothing (similar feel to vanilla)
            double tx = e.getX();
            double ty = e.getY();
            double tz = e.getZ();

            double chase = 0.08D; // smoother (lower = more floaty, higher = snappier)
            this.x += (tx - this.x) * chase;
            this.y += (ty - this.y) * chase;
            this.z += (tz - this.z) * chase;
        }
    }
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
