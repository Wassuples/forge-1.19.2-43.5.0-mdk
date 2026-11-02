package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonShadowAnchorEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The tossed shadow is the owner's own silhouette: the owner's actual model (player skin shape, armour-less,
 * current limb pose) squashed flat and tinted shadow-black, lying on the floor/wall/ceiling it struck.
 * It mimics the owner's movements live, like a shadow that wandered off. In flight it tumbles toward its target.
 */
public class ToonShadowAnchorRenderer extends EntityRenderer<ToonShadowAnchorEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final float SHADOW_R = 0.035f;
    private static final float SHADOW_G = 0.03f;
    private static final float SHADOW_B = 0.05f;
    private static final float SHADOW_ALPHA = 1.0f; // opaque: overlapping flattened limbs must not darken each other
    private static final float FLATTEN = 0.02f;

    public ToonShadowAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonShadowAnchorEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        Entity owner = entity.getOwnerEntity();
        float age = entity.tickCount + partialTick;

        pose.pushPose();
        if (entity.isStuck()) {
            Vec3 normal = entity.getStuckNormal();
            orientToSurface(pose, normal, entity.getTossYaw());
            // Pop out of the throw with a little cartoon wobble, then settle.
            float settle = Mth.clamp(age / 8.0f, 0.0f, 1.0f);
            float wobble = 1.0f + 0.12f * Mth.sin(age * 0.9f) * (1.0f - settle);
            pose.scale(wobble, wobble, FLATTEN);
            // Centre the body on the impact point (model origin is at the feet).
            pose.translate(0.0, -0.9, 0.0);
        } else {
            // In flight: face the camera horizontally, tumble end over end, still paper-thin.
            float camYaw = this.entityRenderDispatcher.camera.getYRot();
            pose.mulPose(Vector3f.YP.rotationDegrees(-camYaw + 180.0f));
            pose.mulPose(Vector3f.ZP.rotationDegrees(age * 38.0f));
            pose.scale(0.75f, 0.75f, FLATTEN);
            pose.translate(0.0, -0.9, 0.0);
        }

        if (!(owner instanceof LivingEntity living) || !renderOwnerSilhouette(living, partialTick, pose, buffers)) {
            renderFallbackSilhouette(pose, buffers);
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** Renders the owner's real model as a black silhouette. Returns false if the owner has no living model. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean renderOwnerSilhouette(LivingEntity owner, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        EntityRenderer<? super LivingEntity> renderer = this.entityRenderDispatcher.getRenderer(owner);
        if (!(renderer instanceof LivingEntityRenderer livingRenderer)) return false;

        EntityModel model = livingRenderer.getModel();
        ResourceLocation skin = livingRenderer.getTextureLocation(owner);

        float limbSwingAmount = Math.min(1.0f, Mth.lerp(partialTick, owner.animationSpeedOld, owner.animationSpeed));
        float limbSwing = owner.animationPosition - owner.animationSpeed * (1.0f - partialTick);
        float ageInTicks = owner.tickCount + partialTick;
        float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot) - bodyYaw;
        float headPitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());

        model.attackTime = owner.getAttackAnim(partialTick);
        model.riding = false;
        model.young = owner.isBaby();
        model.prepareMobModel(owner, limbSwing, limbSwingAmount, partialTick);
        model.setupAnim(owner, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch);

        pose.pushPose();
        // Same transform LivingEntityRenderer applies: model space is Y-down, 1.501 blocks tall to the feet.
        pose.scale(-1.0f, -1.0f, 1.0f);
        pose.translate(0.0, -1.501f, 0.0);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(skin)); // keeps the skin's cut-out pixels (hair, hat layer)
        model.renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, SHADOW_R, SHADOW_G, SHADOW_B, SHADOW_ALPHA);
        pose.popPose();
        return true;
    }

    /** Owner unknown (unloaded / not a living entity): plain humanoid ink cut-out. */
    private static void renderFallbackSilhouette(PoseStack pose, MultiBufferSource buffers) {
        int ink = 0xD108070C;
        pose.pushPose();
        pose.translate(0.0, 0.9, 0.0);
        pose.scale(1.0f, -1.0f, 1.0f);
        pose.translate(0.0, -0.9, 0.0);
        ToonRenderHelper.disc(pose, buffers, ink, 0.0f, 0.2f, 0.25f, 16, 0.0f);          // head
        ToonRenderHelper.quad(pose, buffers, ink, -0.25f, 0.45f, 0.25f, 1.2f, 0.0f);     // torso
        ToonRenderHelper.quad(pose, buffers, ink, -0.5f, 0.45f, -0.25f, 1.15f, 0.0f);    // arms
        ToonRenderHelper.quad(pose, buffers, ink, 0.25f, 0.45f, 0.5f, 1.15f, 0.0f);
        ToonRenderHelper.quad(pose, buffers, ink, -0.25f, 1.2f, -0.02f, 1.8f, 0.0f);     // legs
        ToonRenderHelper.quad(pose, buffers, ink, 0.02f, 1.2f, 0.25f, 1.8f, 0.0f);
        pose.popPose();
    }

    /**
     * Rotate so local +Y runs along the surface ("up" the silhouette) and local Z is the surface normal,
     * which the caller then squashes flat.
     */
    private static void orientToSurface(PoseStack pose, Vec3 normal, float tossYaw) {
        if (normal.y > 0.5) {
            // Floor: lie flat, head pointing the way it was thrown.
            pose.mulPose(Vector3f.YP.rotationDegrees(-tossYaw + 180.0f));
            pose.mulPose(Vector3f.XP.rotationDegrees(-90.0f));
        } else if (normal.y < -0.5) {
            pose.mulPose(Vector3f.YP.rotationDegrees(-tossYaw + 180.0f));
            pose.mulPose(Vector3f.XP.rotationDegrees(90.0f));
        } else {
            // Wall: stand upright, facing out of the wall.
            float yaw = (float) Math.toDegrees(Math.atan2(normal.x, normal.z));
            pose.mulPose(Vector3f.YP.rotationDegrees(yaw));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ToonShadowAnchorEntity entity) {
        return TEXTURE;
    }
}
