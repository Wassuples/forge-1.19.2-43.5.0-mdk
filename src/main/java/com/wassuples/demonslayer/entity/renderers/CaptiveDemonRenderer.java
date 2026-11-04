package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Mt. Fujikasane's captive demons: one model per build ({@link CaptiveDemonModel}), one of {@value CaptiveDemonEntity#SKINS}
 * skins, sized by variant and build.
 *
 * <p>Dying is drawn here, unmistakably unlike being hurt (no red flash, no keeling over): the body freezes upright,
 * the head drops away and tumbles to the ground, and everything chars from the extremities and crumbles into ash
 * that sinks and fades. A demon caught by the sun glows like embers and burns away faster, head and all.
 */
public class CaptiveDemonRenderer extends HumanoidMobRenderer<CaptiveDemonEntity, CaptiveDemonModel> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[CaptiveDemonEntity.SKINS];
    private final CaptiveDemonModel[] models = new CaptiveDemonModel[3];

    static {
        for (int i = 0; i < CaptiveDemonEntity.SKINS; i++) {
            TEXTURES[i] = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/captive_demon/captive_demon_skin_" + i + ".png");
        }
    }

    public CaptiveDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new CaptiveDemonModel(context.bakeLayer(CaptiveDemonModel.LAYERS[CaptiveDemonEntity.ORDINARY]), CaptiveDemonEntity.ORDINARY), 0.5F);
        for (int b = 0; b < 3; b++) {
            models[b] = b == CaptiveDemonEntity.ORDINARY ? this.model : new CaptiveDemonModel(context.bakeLayer(CaptiveDemonModel.LAYERS[b]), b);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CaptiveDemonEntity entity) {
        int v = entity.variant();
        return TEXTURES[v >= 0 && v < TEXTURES.length ? v : 0];
    }

    @Override
    protected void scale(CaptiveDemonEntity entity, PoseStack pose, float partialTick) {
        float s = 0.9375F * CaptiveDemonEntity.scaleFor(entity.variant(), entity.build());
        pose.scale(s, s, s);
    }

    @Override
    protected float getFlipDegrees(CaptiveDemonEntity entity) {
        return 0.0F;
    }

    @Override
    public void render(CaptiveDemonEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        int build = Mth.clamp(entity.build(), 0, 2);
        this.model = models[build];
        if (entity.deathTime > 0 || !entity.isAlive()) {
            renderDying(entity, partialTick, pose, buffers, light);
            return;
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private void renderDying(CaptiveDemonEntity e, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        boolean sun = e.diedToSun();
        float length = sun ? CaptiveDemonEntity.SUN_DEATH_TICKS : CaptiveDemonEntity.DEATH_TICKS;
        float t = Mth.clamp((e.deathTime + partialTick) / length, 0.0F, 1.0F);
        CaptiveDemonModel m = this.model;
        m.prepareMobModel(e, 0, 0, partialTick);
        m.freeze(e);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(e)));

        pose.pushPose();
        float bodyYaw = Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot);
        pose.mulPose(Vector3f.YP.rotationDegrees(180.0F - bodyYaw));
        scale(e, pose, partialTick);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0D, -1.501D, 0.0D);

        // the body: chars (sun: glows like embers first), slumps a little and sinks as the ash falls away
        float burn = Mth.clamp(t * 2.0F, 0.0F, 1.0F);
        float r, g, b;
        if (sun) {
            float ember = Mth.clamp(1.0F - Math.abs(t - 0.2F) / 0.25F, 0.0F, 1.0F);
            r = Mth.lerp(burn, 1.0F, 0.2F) + ember * 0.6F;
            g = Mth.lerp(burn, 1.0F, 0.12F) + ember * 0.25F;
            b = Mth.lerp(burn, 1.0F, 0.1F);
        } else {
            r = Mth.lerp(burn, 1.0F, 0.22F);
            g = Mth.lerp(burn, 1.0F, 0.2F);
            b = Mth.lerp(burn, 1.0F, 0.2F);
        }
        float alpha = t < 0.5F ? 1.0F : 1.0F - (t - 0.5F) / 0.5F;
        if (alpha <= 0.01F) {
            pose.popPose();
            return;
        }
        pose.pushPose();
        float sink = t * t;
        pose.translate(0.0D, 1.5D * sink * 0.35D, 0.0D);   // model space: +Y is down
        pose.scale(1.0F + 0.12F * t, 1.0F - 0.3F * sink, 1.0F + 0.12F * t);
        boolean beheaded = !sun;
        m.head.visible = m.hat.visible = !beheaded;
        int overlay = OverlayTexture.NO_OVERLAY;
        m.renderToBuffer(pose, vc, light, overlay, Math.min(1, r), Math.min(1, g), Math.min(1, b), alpha);
        m.head.visible = m.hat.visible = true;
        pose.popPose();

        // the head drops away and rolls, crumbling a moment after the body
        if (beheaded) {
            float fall = Mth.clamp(t * 2.6F, 0.0F, 1.0F);
            float headBurn = Mth.clamp(t * 2.0F - 0.35F, 0.0F, 1.0F);
            float hc = Mth.lerp(headBurn, 1.0F, 0.22F);
            float headAlpha = t < 0.6F ? 1.0F : 1.0F - (t - 0.6F) / 0.4F;
            float side = (e.getId() & 1) == 0 ? 1.0F : -1.0F;
            pose.pushPose();
            // from the neck to the ground beside the feet (model units are 1/16 block; ground is at y 24)
            pose.translate(side * fall * 6.0F / 16.0F, (fall * fall * 19.0F) / 16.0F, -fall * 5.0F / 16.0F);
            pose.mulPose(Vector3f.XP.rotation(-fall * 1.9F));
            pose.mulPose(Vector3f.ZP.rotation(side * fall * 1.2F));
            renderHead(m.head, pose, vc, light, hc, headAlpha);
            renderHead(m.hat, pose, vc, light, hc, headAlpha);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void renderHead(ModelPart part, PoseStack pose, VertexConsumer vc, int light, float c, float alpha) {
        if (alpha <= 0.01F) return;
        float x = part.x, y = part.y, z = part.z, xr = part.xRot, yr = part.yRot, zr = part.zRot;
        part.x = 0;
        part.z = 0;
        part.xRot = part.yRot = part.zRot = 0;
        part.render(pose, vc, light, OverlayTexture.NO_OVERLAY, c, c, c, alpha);
        part.x = x;
        part.y = y;
        part.z = z;
        part.xRot = xr;
        part.yRot = yr;
        part.zRot = zr;
    }
}
