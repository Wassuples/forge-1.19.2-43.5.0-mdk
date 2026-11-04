package com.wassuples.demonslayer.entity.renderers.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The Hand Demon: its body ({@link HandDemonModel}, drawn at {@value #SCALE}x) plus the arms it throws when it attacks, drawn from
 * its synced attack state — barrage arms shooting out at each target, the grasping arm, arms bursting out of the
 * ground, the knotted giant fist, and the flailing sweep.
 */
public class HandDemonRenderer extends MobRenderer<HandDemonEntity, HandDemonModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/hand_demon_body.png");
    public static final float SCALE = 1.15F;

    public HandDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new HandDemonModel(context.bakeLayer(HandDemonModel.LAYER)), 2.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(HandDemonEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(HandDemonEntity entity, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected float getFlipDegrees(HandDemonEntity entity) {
        return 0.0F;   // it crumbles to ash instead of keeling over
    }

    @Override
    public void render(HandDemonEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (e.deathTime > 0 || !e.isAlive()) {
            renderDying(e, partialTick, pose, buffers, light);
            return;
        }
        super.render(e, yaw, partialTick, pose, buffers, light);
        int attack = e.attack();
        if (attack == HandDemonEntity.NONE) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        float tick = e.attackTick() + partialTick;
        float bodyYaw = Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot);
        Vec3 forward = Vec3.directionFromRotation(0, bodyYaw);
        Vec3 origin = new Vec3(Mth.lerp(partialTick, e.xo, e.getX()), Mth.lerp(partialTick, e.yo, e.getY()), Mth.lerp(partialTick, e.zo, e.getZ()));
        Vec3 shoulder = forward.scale(0.5).add(0, 3.3, 0);   // relative to the entity (the hunched shoulders)
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        float reach = HandDemonEntity.reach(attack, tick);

        switch (attack) {
            case HandDemonEntity.BARRAGE, HandDemonEntity.GRAB -> {
                int n = attack == HandDemonEntity.GRAB ? 1 : 4;
                for (int i = 0; i < n; i++) {
                    Entity t = e.level.getEntity(e.targetId(i));
                    Vec3 from = shoulder.add(side.scale((i - 1.5) * 0.7)).add(0, (i % 2) * 0.4, 0);
                    if (t == null) continue;
                    Vec3 tp = new Vec3(Mth.lerp(partialTick, t.xo, t.getX()), Mth.lerp(partialTick, t.yo, t.getY()) + t.getBbHeight() * 0.55,
                            Mth.lerp(partialTick, t.zo, t.getZ())).subtract(origin);
                    if (attack == HandDemonEntity.BARRAGE && i < 3 && !e.point(i).equals(BlockPos.ZERO) && e.attackTick() >= 8) {
                        BlockPos aim = e.point(i);   // barrage arms fly to where the target was when it was thrown
                        tp = new Vec3(aim.getX() + 0.5, aim.getY() + 0.5, aim.getZ() + 0.5).subtract(origin);
                    }
                    if (reach <= 0) {
                        // coiling: the arms draw back over the body before they are thrown
                        Vec3 coil = from.add(forward.scale(-1.2)).add(0, 1.0 + Math.sin(tick * 0.6 + i) * 0.3, 0);
                        arm(pose, vc, light, from, coil, 1.6F, true);
                    } else {
                        arm(pose, vc, light, from, from.add(tp.subtract(from).scale(reach)), 1.6F, true);
                    }
                }
            }
            case HandDemonEntity.BURROW -> {
                for (int i = 0; i < 3; i++) {
                    BlockPos p = e.point(i);
                    if (p.equals(BlockPos.ZERO)) continue;
                    if (tick < 15) {   // plunging into the earth around itself
                        Vec3 dig = forward.scale(1.5).add(side.scale((i - 1) * 1.2));
                        arm(pose, vc, light, shoulder.add(side.scale((i - 1) * 0.8)), dig.add(0, -0.2 + Math.min(1, tick / 15) * -1.5, 0), 1.6F, false);
                    }
                    if (reach > 0) {
                        Vec3 base = new Vec3(p.getX() + 0.5, p.getY() - 0.3, p.getZ() + 0.5).subtract(origin);
                        for (int k = 0; k < 3; k++) {
                            double a = k * 2.1 + i;
                            Vec3 b = base.add(Math.cos(a) * 0.6, 0, Math.sin(a) * 0.6);
                            arm(pose, vc, light, b, b.add(Math.cos(a) * 0.5, 3.2 * reach, Math.sin(a) * 0.5), 1.4F, true);
                        }
                    }
                }
            }
            case HandDemonEntity.FIST -> {
                // many arms knot into one: raised over the body, then brought down on the line ahead
                double pitch;
                if (tick < HandDemonEntity.FIST_SLAM - 4) pitch = Math.toRadians(Mth.lerp(Math.min(1, tick / 14F), 10F, 70F));
                else if (tick < HandDemonEntity.FIST_SLAM) pitch = Math.toRadians(Mth.lerp((tick - (HandDemonEntity.FIST_SLAM - 4)) / 4F, 70F, -20F));
                else pitch = Math.toRadians(-20F);
                double len = 7.0;
                Vec3 dir = forward.scale(Math.cos(pitch)).add(0, Math.sin(pitch), 0);
                for (int k = 0; k < 5; k++) {
                    Vec3 off = side.scale((k - 2) * 0.35).add(0, (k % 2) * 0.35, 0);
                    arm(pose, vc, light, shoulder.add(off), shoulder.add(off).add(dir.scale(len)), 3.0F, k == 2);
                }
            }
            case HandDemonEntity.SWEEP -> {
                float spin = tick * 0.55F;
                for (int k = 0; k < 6; k++) {
                    double a = spin + k * Math.PI / 3;
                    Vec3 from = new Vec3(0, 1.4, 0);
                    Vec3 to = from.add(Math.cos(a) * 4.8, Math.sin(tick * 0.3 + k) * 0.4, Math.sin(a) * 4.8);
                    arm(pose, vc, light, from, to, 1.5F, true);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Beheaded at last: no red flash and no keeling over. The body freezes and chars from the extremities while its
     * music collapses, then sags and slowly sinks through the whole reprise. When the reprise ends one arm stretches out
     * toward whoever is near (Tanjiro, if he is there) and waits; taken, it stays; not taken, it sinks back. In the last
     * seconds everything crumbles into the ash.
     */
    private void renderDying(HandDemonEntity e, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float time = e.deathTime + partialTick;
        float len = e.deathLength();
        float t = Mth.clamp(time / len, 0.0F, 1.0F);
        float crumbleFrom = len - 120.0F;                       // the last six seconds: it falls apart
        float fade = Mth.clamp((time - crumbleFrom) / 120.0F, 0.0F, 1.0F);
        float alpha = 1.0F - fade;
        if (alpha <= 0.01F) return;
        float c = Mth.lerp(Mth.clamp(time / 40.0F, 0.0F, 1.0F), 1.0F, 0.55F) - 0.3F * t;   // charring
        c = Mth.clamp(c, 0.15F, 1.0F);
        float sink = 0.35F * t + 0.9F * fade * fade;
        float sag = 0.12F * t + 0.3F * fade * fade;

        float bodyYaw = Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot);
        pose.pushPose();
        pose.mulPose(Vector3f.YP.rotationDegrees(180.0F - bodyYaw));
        pose.scale(SCALE, SCALE, SCALE);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0D, -1.501D + sink, 0.0D);
        pose.scale(1.0F + 0.08F * t, 1.0F - sag, 1.0F + 0.08F * t);
        this.model.setupAnim(e, 0, 0, e.tickCount - e.deathTime, 0, 0);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, c, c, c, alpha);
        pose.popPose();

        // the outstretched hand
        if (time < HandDemonEntity.REACH_AT) return;
        Entity target = e.level.getEntity(e.reachTarget());
        Vec3 origin = new Vec3(Mth.lerp(partialTick, e.xo, e.getX()), Mth.lerp(partialTick, e.yo, e.getY()), Mth.lerp(partialTick, e.zo, e.getZ()));
        Vec3 forward = Vec3.directionFromRotation(0, bodyYaw);
        Vec3 from = forward.scale(0.9).add(0, 1.6 - sink * 1.2, 0);
        Vec3 aim;
        if (target != null) {
            aim = new Vec3(Mth.lerp(partialTick, target.xo, target.getX()), Mth.lerp(partialTick, target.yo, target.getY()) + target.getBbHeight() * 0.45,
                    Mth.lerp(partialTick, target.zo, target.getZ())).subtract(origin);
        } else {
            aim = forward.scale(4.0).add(0, 1.0, 0);
        }
        Vec3 d = aim.subtract(from);
        double dist = d.length();
        double maxLen = Math.min(dist - 0.4, 5.0);
        float out = Mth.clamp((time - HandDemonEntity.REACH_AT) / 30.0F, 0.0F, 1.0F);   // it stretches out, slowly
        out = out * out * (3 - 2 * out);
        if (e.outcome() == HandDemonEntity.OUTCOME_UNHELD) {
            float drop = Mth.clamp((time - e.outcomeAt()) / 60.0F, 0.0F, 1.0F);   // nobody took it: it sinks back
            d = d.add(0, -dist * 0.8 * drop, 0);
            out *= 1.0F - 0.5F * drop;
        }
        if (maxLen <= 0.3) return;
        Vec3 to = from.add(d.normalize().scale(maxLen * out));
        VertexConsumer avc = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        arm(pose, avc, light, from, to, 1.3F, true, c, alpha);
    }

    /** Draws an arm from {@code from} to {@code to} (both relative to the entity), with a hand at the end. */
    private void arm(PoseStack pose, VertexConsumer vc, int light, Vec3 from, Vec3 to, float thickness, boolean withHand) {
        arm(pose, vc, light, from, to, thickness, withHand, 1.0F, 1.0F);
    }

    private void arm(PoseStack pose, VertexConsumer vc, int light, Vec3 from, Vec3 to, float thickness, boolean withHand, float shade, float alpha) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.05) return;
        float yaw = (float) Math.atan2(d.x, d.z);
        float pitch = (float) -Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        pose.mulPose(Vector3f.YP.rotation(yaw));
        pose.mulPose(Vector3f.XP.rotation(pitch));
        pose.pushPose();
        pose.scale(thickness, thickness, (float) len);
        this.model.reachArm.render(pose, vc, light, OverlayTexture.NO_OVERLAY, shade, shade, shade, alpha);
        pose.popPose();
        if (withHand) {
            pose.translate(0, 0, len);
            pose.scale(thickness * 0.8F, thickness * 0.8F, thickness * 0.8F);
            this.model.reachHand.render(pose, vc, light, OverlayTexture.NO_OVERLAY, shade, shade, shade, alpha);
        }
        pose.popPose();
    }
}
