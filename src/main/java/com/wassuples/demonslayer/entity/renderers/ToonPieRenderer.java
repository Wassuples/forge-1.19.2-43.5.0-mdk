package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonPieEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** A tin-plate cream pie: flies from the thrower's hand, splats onto the victim's face, drips, slides off. */
public class ToonPieRenderer extends EntityRenderer<ToonPieEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int TIN = 0xFFB9BEC7;
    private static final int TIN_DARK = 0xFF7F858F;
    private static final int TIN_RIM = 0xFFE1E5EB;
    private static final int CRUST = 0xFFD9A05B;
    private static final int CRUST_DARK = 0xFFA8702F;
    private static final int CREAM = 0xFFFFFCF0;
    private static final int CREAM_SHADE = 0xFFF1E7CF;
    private static final int CHERRY = 0xFFE0242F;

    public ToonPieRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonPieEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        Entity target = entity.getTarget();
        if (!(target instanceof LivingEntity victim)) return;

        float t = entity.tickCount + partialTick;
        boolean flying = t < ToonPieEntity.FLIGHT_TICKS;
        Minecraft mc = Minecraft.getInstance();
        boolean victimFirstPerson = victim == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        if (victimFirstPerson && !flying) return; // the HUD splat takes over for the victim

        // Everything below is computed in world space, then made relative to this entity's render origin.
        Vec3 origin = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
        float headYaw = Mth.rotLerp(partialTick, victim.yHeadRotO, victim.yHeadRot);
        float headPitch = Mth.lerp(partialTick, victim.xRotO, victim.getXRot());
        Vec3 look = Vec3.directionFromRotation(headPitch, headYaw);
        Vec3 eye = new Vec3(Mth.lerp(partialTick, victim.xo, victim.getX()),
                Mth.lerp(partialTick, victim.yo, victim.getY()) + victim.getEyeHeight(),
                Mth.lerp(partialTick, victim.zo, victim.getZ()));
        Vec3 face = eye.add(look.scale(victim.getBbWidth() * 0.5 + 0.08));

        pose.pushPose();
        if (flying) {
            Entity thrower = entity.getThrower();
            Vec3 hand = thrower == null ? face.subtract(look.scale(-2.0)) : new Vec3(
                    Mth.lerp(partialTick, thrower.xo, thrower.getX()),
                    Mth.lerp(partialTick, thrower.yo, thrower.getY()) + thrower.getBbHeight() * 0.7,
                    Mth.lerp(partialTick, thrower.zo, thrower.getZ())).add(thrower.getLookAngle().scale(0.5));
            float u = t / ToonPieEntity.FLIGHT_TICKS;
            Vec3 at = hand.lerp(face, u).add(0.0, Mth.sin((float) Math.PI * u) * 0.45, 0.0);
            Vec3 rel = at.subtract(origin);
            pose.translate(rel.x, rel.y, rel.z);
            // Pie travels cream-first, wobbling.
            Vec3 travel = face.subtract(hand).normalize();
            float travelYaw = (float) (Mth.atan2(-travel.x, travel.z) * Mth.RAD_TO_DEG);
            pose.mulPose(Vector3f.YP.rotationDegrees(-travelYaw));
            pose.mulPose(Vector3f.XP.rotationDegrees(90.0f + Mth.sin(t * 2.0f) * 12.0f));
            drawPie(pose, buffers, 1.0f);
        } else {
            float stuckT = t - ToonPieEntity.FLIGHT_TICKS;
            float slide = Mth.clamp((stuckT - ToonPieEntity.STUCK_TICKS) / ToonPieEntity.SLIDE_TICKS, 0.0f, 1.0f);
            Vec3 at = face.add(0.0, -slide * slide * 1.1, 0.0).add(look.scale(slide * 0.15));
            Vec3 rel = at.subtract(origin);
            pose.translate(rel.x, rel.y, rel.z);

            // Cream side pressed into the face, then peeling forward/down as it slides off.
            pose.mulPose(Vector3f.YP.rotationDegrees(-headYaw));
            pose.mulPose(Vector3f.XP.rotationDegrees(headPitch + slide * 70.0f));
            pose.mulPose(Vector3f.XP.rotationDegrees(-90.0f));
            float squash = stuckT < 4.0f ? 1.0f + 0.25f * Mth.sin(stuckT * 1.6f) * (1.0f - stuckT / 4.0f) : 1.0f;
            pose.scale(squash, 1.0f / squash, squash);
            drawPie(pose, buffers, 1.0f - slide * 0.3f);
            pose.popPose();

            // Cream drips running down the face/chest, growing over time.
            pose.pushPose();
            Vec3 dripRel = face.subtract(origin);
            pose.translate(dripRel.x, dripRel.y - 0.12, dripRel.z);
            drawDrips(pose, buffers, entity.getId(), Math.min(stuckT, ToonPieEntity.STUCK_TICKS), slide);
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** Pie with its cream side facing local +Y, centred on the cream surface. */
    private static void drawPie(PoseStack pose, MultiBufferSource buffers, float creamAmount) {
        // tin
        ToonRenderHelper.cylinder(pose, buffers, TIN_DARK, 0.165f, 0.215f, -0.075f, -0.004f, 16);
        ToonRenderHelper.cylinder(pose, buffers, TIN, 0.16f, 0.21f, -0.07f, 0.0f, 16);
        pose.pushPose();
        pose.mulPose(Vector3f.XP.rotationDegrees(180.0f));
        ToonRenderHelper.discXZ(pose, buffers, TIN_DARK, 0.165f, 0.075f, 16); // underside
        pose.popPose();
        ToonRenderHelper.cylinder(pose, buffers, TIN_RIM, 0.21f, 0.235f, 0.0f, 0.012f, 16);
        // crust edge
        ToonRenderHelper.cylinder(pose, buffers, CRUST_DARK, 0.205f, 0.2f, 0.0f, 0.03f, 16);
        ToonRenderHelper.cylinder(pose, buffers, CRUST, 0.2f, 0.19f, 0.03f, 0.042f, 16);
        // cream: base, dome, dollops, cherry
        float c = creamAmount;
        ToonRenderHelper.discXZ(pose, buffers, CREAM_SHADE, 0.19f, 0.042f, 16);
        ToonRenderHelper.cylinder(pose, buffers, CREAM_SHADE, 0.19f, 0.12f, 0.042f, 0.042f + 0.05f * c, 16);
        ToonRenderHelper.discXZ(pose, buffers, CREAM, 0.12f, 0.042f + 0.05f * c, 16);
        for (int i = 0; i < 6; i++) {
            float a = i * (Mth.TWO_PI / 6.0f);
            pose.pushPose();
            pose.translate(Mth.cos(a) * 0.135f, 0.06f * c, Mth.sin(a) * 0.135f);
            ToonRenderHelper.cylinder(pose, buffers, CREAM, 0.045f, 0.0f, 0.0f, 0.06f * c, 8);
            pose.popPose();
        }
        pose.pushPose();
        pose.translate(0.0, 0.092f * c, 0.0);
        ToonRenderHelper.box(pose, buffers, CHERRY, -0.025f, 0.0f, -0.025f, 0.025f, 0.045f, 0.025f);
        pose.popPose();
    }

    private void drawDrips(PoseStack pose, MultiBufferSource buffers, int seed, float stuckT, float slide) {
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.scale(-1.0f, -1.0f, 1.0f); // Y down from here, like GUI space
        java.util.Random rnd = new java.util.Random(seed);
        for (int i = 0; i < 5; i++) {
            float x = (rnd.nextFloat() - 0.5f) * 0.32f;
            float speed = 0.004f + rnd.nextFloat() * 0.006f;
            float length = Math.min(0.55f, stuckT * speed) * (1.0f - slide);
            float w = 0.018f + rnd.nextFloat() * 0.014f;
            if (length <= 0.01f) continue;
            ToonRenderHelper.quad(pose, buffers, CREAM_SHADE, x - w - 0.006f, -0.01f, x + w + 0.006f, length + 0.006f, 0.002f);
            ToonRenderHelper.quad(pose, buffers, CREAM, x - w, 0.0f, x + w, length, 0.0f);
            ToonRenderHelper.disc(pose, buffers, CREAM, x, length, w * 1.5f, 10, -0.001f);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ToonPieEntity entity) {
        return TEXTURE;
    }
}
