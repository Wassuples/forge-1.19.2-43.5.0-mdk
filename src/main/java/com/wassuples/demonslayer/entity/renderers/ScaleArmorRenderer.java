package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ScaleArmorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Killer Fish Scales fallback for non-humanoid owners: overlapping diamond scales (teal and gold) wrapped around
 * the body with a shimmer band sweeping up through them. Humanoid owners are skipped here because ScaleSkinLayer
 * draws the scales directly onto their model. Hidden in the owner's own first-person view.
 */
public class ScaleArmorRenderer extends EntityRenderer<ScaleArmorEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ScaleArmorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ScaleArmorEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Entity owner = entity.getOwner();
        if (!(owner instanceof LivingEntity living)) return;
        Minecraft mc = Minecraft.getInstance();
        boolean firstPerson = owner == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        // Humanoids get the scales on their actual skin from ScaleSkinLayer; this is only the fallback.
        if (mc.getEntityRenderDispatcher().getRenderer(owner) instanceof LivingEntityRenderer<?, ?> lr
                && lr.getModel() instanceof HumanoidModel<?>) {
            return;
        }

        float age = entity.tickCount + partialTick;
        float appear = Mth.clamp(age / 6.0f, 0.0f, 1.0f) * Mth.clamp((entity.getDuration() - age) / 6.0f, 0.0f, 1.0f);
        if (appear <= 0.0f) return;
        Vec3 origin = entity.getPosition(partialTick);
        Vec3 ownerPos = living.getPosition(partialTick).subtract(origin);
        float height = living.getBbHeight();
        float radius = living.getBbWidth() * 0.62f;
        float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot) * Mth.DEG_TO_RAD;

        if (!firstPerson) {
            int rows = 10;
            int cols = 14;
            float shimmerY = (age * 0.08f) % 1.4f - 0.2f;
            for (int r = 0; r < rows; r++) {
                float v = (r + 0.5f) / rows;
                float y = v * height * 0.95f;
                float bulge = radius * (1.0f + 0.12f * Mth.sin((float) Math.PI * v));
                for (int c = 0; c < cols; c++) {
                    double a = bodyYaw + Math.PI * 2.0 * (c + (r % 2) * 0.5) / cols;
                    float shimmer = Math.max(0.0f, 1.0f - Math.abs(v - shimmerY) * 6.0f);
                    int base = ((r + c) % 3 == 0) ? 0xFFE8C15A : 0xFF3FC9B6;
                    int col = ToonRenderHelper.withAlpha(ToonRenderHelper.shade(base, 0.8f + 0.6f * shimmer), 0.85f * appear);
                    scale(pose, buffers, ownerPos, a, y, bulge * appear, height / rows * 0.75f, (float) (Math.PI * 2.0 / cols) * 0.55f, col);
                }
            }
        }

        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** One diamond scale on the cylinder at angle a, height y, pointing downward and slightly outward. */
    private static void scale(PoseStack pose, MultiBufferSource buffers, Vec3 base, double a, float y, float r, float h, float w, int color) {
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        float cl = (float) Math.cos(a - w), sl = (float) Math.sin(a - w);
        float cr = (float) Math.cos(a + w), sr = (float) Math.sin(a + w);
        float bx = (float) base.x, by = (float) base.y, bz = (float) base.z;
        float out = r * 1.08f; // lower tip flares out like overlapping scales
        ToonRenderHelper.quad3(pose, buffers, color,
                bx + ca * r, by + y + h * 0.5f, bz + sa * r,
                bx + cr * r, by + y, bz + sr * r,
                bx + ca * out, by + y - h * 0.6f, bz + sa * out,
                bx + cl * r, by + y, bz + sl * r,
                ca, 0, sa);
    }

    @Override
    public ResourceLocation getTextureLocation(ScaleArmorEntity entity) {
        return TEXTURE;
    }
}
