package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.DomaArtEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Doma's Cryokinesis: crystal lotuses, weeping ice tendrils, ice vines, plunging icicles and billowing freezing
 * mist (his frozen blood, finely powdered). One renderer for every mode of {@link DomaArtEntity}; each mode's
 * timeline mirrors its form's damage ticks.
 */
public class DomaArtRenderer extends EntityRenderer<DomaArtEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int FULL = 0xF000F0;

    public DomaArtRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(DomaArtEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 origin = entity.getPosition(partialTick);
        Camera camera = this.entityRenderDispatcher.camera;
        Vec3 cam = camera.getPosition().subtract(origin);
        Entity o = entity.getOwner();
        LivingEntity owner = o instanceof LivingEntity l ? l : null;
        Minecraft mc = Minecraft.getInstance();
        boolean ownerView = owner != null && owner == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        Ctx c = new Ctx(pose, buffers, cam, camera, age, partialTick, entity, owner, origin, light, ownerView);
        switch (entity.getMode()) {
            case FROZEN_LOTUS -> renderFrozenLotus(c);
            case HANGING_GARDEN -> renderHangingGarden(c);
            case FREEZING_CLOUDS -> renderFreezingClouds(c);
            case LOTUS_VINES -> renderLotusVines(c);
            case WINTRY_ICICLES -> renderWintryIcicles(c);
            case CRYSTAL_CHARGE -> renderCrystalCharge(c);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private record Ctx(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Camera camera, float age, float partialTick,
                       DomaArtEntity entity, LivingEntity owner, Vec3 origin, int light, boolean ownerView) {
        Vec3 feet() {
            return owner == null ? Vec3.ZERO : owner.getPosition(partialTick).subtract(origin);
        }

        Vec3 look() {
            return owner == null ? entity.getAimDirection() : owner.getViewVector(partialTick);
        }

        float fade(float in, float out) {
            return Mth.clamp(age / in, 0.0f, 1.0f) * Mth.clamp((entity.getDuration() - age) / out, 0.0f, 1.0f);
        }
    }

    // =========================================================================================== modes

    /**
     * Frozen Lotus: one sweep of Doma's fan sends a crescent of freezing mist across the 120° cone (7 blocks), and
     * crystal lotuses burst open where it passes, then crumble.
     */
    private static void renderFrozenLotus(Ctx c) {
        Vec3 dir = c.entity.getAimDirection();
        Vec3 f = new Vec3(dir.x, 0, dir.z);
        f = f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
        float b = c.fade(1.0f, 6.0f);
        // The sweep: mist and a frost arc travelling across the cone, left to right.
        float sweep = Mth.clamp(c.age / 5.0f, 0.0f, 1.0f);
        Random rnd = new Random(c.entity.getSeed());
        for (int i = 0; i < 26; i++) {
            float t = rnd.nextFloat();
            if (t > sweep) continue;
            double a = Math.toRadians(-60 + 120 * t);
            double dist = 1.5 + rnd.nextFloat() * 5.5;
            Vec3 p = f.yRot((float) a).scale(dist).add(0, 1.0 + rnd.nextFloat() * 0.8 + c.age * 0.03, 0);
            float life = Mth.clamp(1.0f - (c.age - t * 5.0f) / 18.0f, 0.0f, 1.0f);
            DomaRenderHelper.puff(c.pose, c.buffers, c.camera, p, 1.6f + 1.4f * (1.0f - life), rnd.nextFloat() * 6.28f + c.age * 0.02f, 0.55f * life * b);
        }
        if (c.age < 8.0f) {
            VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
            Vec3 prev = null;
            float r = 1.0f + 6.0f * sweep;
            for (int s = 0; s <= 16; s++) {
                double a = Math.toRadians(-60 + 120 * sweep * s / 16.0);
                Vec3 p = f.yRot((float) a).scale(r).add(0, 1.1, 0);
                if (prev != null) ThreadRenderHelper.ribbon(c.pose, vc, c.cam, prev, p, 0.35f, ThreadRenderHelper.scaleAlpha(0x90CFF4FF, 1.0f - c.age / 8.0f));
                prev = p;
            }
        }
        List<Vec3> spots = c.entity.getExtraPoints();
        for (int i = 0; i < spots.size(); i++) {
            float bloom = Mth.clamp((c.age - 1.5f - i * 0.6f) / 4.0f, 0.0f, 1.0f);
            if (bloom <= 0.0f) continue;
            float crumble = Mth.clamp((c.age - 16.0f) / 6.0f, 0.0f, 1.0f);
            float size = (0.6f + 1.4f * bloom) * (1.0f - crumble * 0.6f);
            DomaRenderHelper.lotus(c.pose, c.buffers, spots.get(i), size, bloom * (1.0f - crumble * 0.3f), i * 0.7f, FULL, c.cam);
        }
    }

    /**
     * Barren Hanging Garden: with each of the eight waves a ring of weeping ice tendrils lashes down and out
     * around Doma to the 6-block edge, turning a little each time, with a frost ring racing outward.
     */
    private static void renderHangingGarden(Ctx c) {
        if (c.owner == null && !c.entity.isFrozen()) return;
        Vec3 feet = c.feet();
        float b = c.fade(1.0f, 5.0f);
        int tendrils = 10;
        for (int w = 1; w <= 8; w++) {
            float s = c.age - w * 2;
            if (s < -1.0f || s > 5.0f) continue;
            float lash = Mth.clamp((s + 1.0f) / 2.0f, 0.0f, 1.0f);
            float fade = 1.0f - Mth.clamp((s - 1.0f) / 4.0f, 0.0f, 1.0f);
            double turn = w * 0.31;
            for (int t = 0; t < tendrils; t++) {
                double a = turn + Math.PI * 2.0 * t / tendrils;
                Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
                Vec3 top = feet.add(out.scale(0.8)).add(0, 3.2, 0);
                Vec3 end = feet.add(out.scale(6.0 * lash)).add(0, 0.4 + (1.0 - lash) * 2.4, 0);
                Vec3 ctrl = feet.add(out.scale(3.4 * lash)).add(0, 3.6, 0);
                Vec3 prev = top;
                for (int k = 1; k <= 4; k++) {
                    double q = k / 4.0;
                    Vec3 p = HantenguRenderHelper.bezier(top, ctrl, end, q);
                    DomaRenderHelper.iceLimb(c.pose, c.buffers, prev, p, 0.11 * (1.0 - (k - 1) * 0.2), 0.11 * (1.0 - k * 0.2) + 0.015,
                            ThreadRenderHelper.scaleAlpha(DomaRenderHelper.ICE, fade), FULL, fade > 0.5f ? c.cam : null);
                    prev = p;
                }
            }
            HantenguRenderHelper.soundRing(c.pose, c.buffers, c.cam, feet.add(0, 0.15, 0), new Vec3(0, 1, 0), 6.0f * lash, 0.35f, 0x80CFF4FF, fade * b);
        }
        // Low mist pooling around him.
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8 + c.age * 0.05;
            Vec3 p = feet.add(Math.cos(a) * 3.0, 0.5, Math.sin(a) * 3.0);
            DomaRenderHelper.puff(c.pose, c.buffers, c.camera, p, 2.4f, i + c.age * 0.03f, 0.35f * b);
        }
    }

    /**
     * Freezing Clouds: a wall of freezing mist 20 blocks wide rolling 30 blocks forward over two seconds (the
     * band the form chills), glittering with ice crystals, leaving a thinning haze behind it.
     */
    private static void renderFreezingClouds(Ctx c) {
        if (c.owner == null) return;
        float b = c.fade(2.0f, 8.0f);
        Vec3 look = c.look();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        float p = Mth.clamp(c.age / 40.0f, 0.0f, 1.0f);
        Vec3 centre = c.feet().add(look.scale(30.0 * p)).add(0, 1.0, 0);
        Random rnd = new Random(c.entity.getId() * 7L);
        int puffs = c.ownerView ? 22 : 34;
        for (int i = 0; i < puffs; i++) {
            double across = (rnd.nextDouble() - 0.5) * 20.0;
            double h = rnd.nextDouble() * 4.0 - 1.0;
            double lag = rnd.nextDouble() * 3.0;
            Vec3 at = centre.add(side.scale(across)).add(0, h, 0).subtract(flat.scale(lag));
            at = at.add(Math.sin(c.age * 0.1 + i) * 0.5, Math.sin(c.age * 0.13 + i * 2) * 0.3, 0);
            // keep it out of Doma's own face as it leaves him
            float near = (float) Mth.clamp((at.subtract(c.feet().add(0, 1.6, 0)).length() - 2.0) / 3.0, 0.0, 1.0);
            DomaRenderHelper.puff(c.pose, c.buffers, c.camera, at, 3.2f + (float) lag, rnd.nextFloat() * 6.28f + c.age * 0.015f, 0.5f * b * near);
        }
        // Thinning haze left behind.
        for (int i = 0; i < 12; i++) {
            double q = rnd.nextDouble() * p;
            Vec3 at = c.feet().add(look.scale(30.0 * q)).add(side.scale((rnd.nextDouble() - 0.5) * 18.0)).add(0, 0.6 + rnd.nextDouble(), 0);
            DomaRenderHelper.puff(c.pose, c.buffers, c.camera, at, 3.5f, i, 0.18f * b);
        }
        // Glittering ice crystals in the wall.
        VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
        for (int i = 0; i < 40; i++) {
            Random g = new Random(c.entity.getId() * 131L + i * 7919L + (long) (c.age / 3));
            Vec3 at = centre.add(side.scale((g.nextDouble() - 0.5) * 20.0)).add(0, g.nextDouble() * 4.0 - 1.0, 0);
            float tw = 0.5f + 0.5f * Mth.sin(c.age * 1.3f + i);
            float s = 0.12f + 0.1f * tw;
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, at.add(0, -s, 0), at.add(0, s, 0), 0.04f, ThreadRenderHelper.scaleAlpha(0xE0F0FBFF, tw * b));
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, at.add(side.scale(-s)), at.add(side.scale(s)), 0.04f, ThreadRenderHelper.scaleAlpha(0xE0F0FBFF, tw * b));
        }
    }

    /**
     * Lotus Vines: three ice vines snake along the ground from Doma to the target point (15 ticks), budding as
     * they go; then a garden of crystal lotuses blooms there in freezing mist for five seconds.
     */
    private static void renderLotusVines(Ctx c) {
        Vec3 target = c.entity.getImpactOffset();
        Vec3 path = target.subtract(Vec3.ZERO);
        Vec3 flat = new Vec3(path.x, 0, path.z);
        Vec3 side = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(-flat.z, 0, flat.x).normalize();
        float grow = Mth.clamp(c.age / 15.0f, 0.0f, 1.0f);
        float b = c.fade(1.0f, 10.0f);
        int segs = 18;
        for (int v = 0; v < 3; v++) {
            Vec3 prev = null;
            int shown = Math.max(1, (int) (segs * grow));
            for (int s = 0; s <= shown; s++) {
                double t = (double) s / segs;
                double weave = Math.sin(t * Math.PI * 2.0 + v * 2.1) * 1.2 * Math.sin(Math.PI * t);
                Vec3 p = path.scale(t).add(side.scale(weave)).add(0, 0.15 + 0.1 * Math.sin(t * 9 + v), 0);
                if (prev != null) {
                    DomaRenderHelper.iceLimb(c.pose, c.buffers, prev, p, 0.09, 0.08, ThreadRenderHelper.scaleAlpha(DomaRenderHelper.ICE, b), FULL, null);
                    if (s % 5 == 0) {
                        DomaRenderHelper.lotus(c.pose, c.buffers, p, 0.45f, 0.2f + 0.3f * grow, s + v, FULL, null);
                    }
                }
                prev = p;
            }
            // Leading tip glint while growing.
            if (grow < 1.0f && prev != null) {
                VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
                ThreadRenderHelper.ribbon(c.pose, vc, c.cam, prev, prev.add(0, 0.35, 0), 0.3f, 0xB0E0F6FF);
            }
        }
        if (c.age < 15.0f) return;
        // The garden.
        List<Vec3> spots = c.entity.getExtraPoints();
        for (int i = 0; i < spots.size(); i++) {
            float bloom = Mth.clamp((c.age - 15.0f - i * 1.5f) / 6.0f, 0.0f, 1.0f);
            DomaRenderHelper.lotus(c.pose, c.buffers, spots.get(i), (0.8f + 1.0f * bloom) * b + 0.01f, bloom, i * 1.1f + c.age * 0.004f, FULL, c.cam);
        }
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2.0 * i / 10 + c.age * 0.02;
            Vec3 p = target.add(Math.cos(a) * 2.8, 0.5 + 0.3 * Math.sin(c.age * 0.07 + i), Math.sin(a) * 2.8);
            DomaRenderHelper.puff(c.pose, c.buffers, c.camera, p, 2.6f, i + c.age * 0.02f, 0.3f * b);
        }
        HantenguRenderHelper.soundRing(c.pose, c.buffers, c.cam, target.add(0, 0.08, 0), new Vec3(0, 1, 0), 4.0f, 0.2f, 0x60CFF4FF, b * 0.7f);
    }

    /**
     * Wintry Icicles: frost spreads on the ground at the target while a cluster of great icicles grows in the air
     * above it; on tick 15 (the damage tick) they plunge and impale the ground, then crack apart.
     */
    private static void renderWintryIcicles(Ctx c) {
        Random rnd = new Random(c.entity.getSeed());
        float form = Mth.clamp(c.age / 13.0f, 0.0f, 1.0f);
        float drop = Mth.clamp((c.age - 13.0f) / 2.0f, 0.0f, 1.0f);
        drop = drop * drop;
        float shatter = Mth.clamp((c.age - 22.0f) / 6.0f, 0.0f, 1.0f);
        Vec3 up = new Vec3(0, 1, 0);
        // Frost spreading and pulsing on the ground (the telegraph).
        float ring = c.age < 15.0f ? 0.6f + 0.4f * Mth.sin(c.age * 0.8f) : 1.0f - shatter;
        HantenguRenderHelper.soundRing(c.pose, c.buffers, c.cam, new Vec3(0, 0.08, 0), up, 2.5f * (0.4f + 0.6f * form), 0.25f, 0x90CFF4FF, ring);
        for (int i = 0; i < 7; i++) {
            double x = (rnd.nextDouble() - 0.5) * 2.5, z = (rnd.nextDouble() - 0.5) * 2.5;
            double len = 1.8 + rnd.nextDouble() * 1.2;
            double r = 0.18 + rnd.nextDouble() * 0.1;
            float delay = rnd.nextFloat() * 0.25f;
            float g = Mth.clamp((form - delay) / (1.0f - delay), 0.0f, 1.0f);
            if (g <= 0.0f) continue;
            double hang = 6.5 + rnd.nextDouble() * 1.5;
            // tip ends embedded slightly in the ground after the drop
            double tipY = Mth.lerp(drop, hang - len * g, -0.4);
            Vec3 base = new Vec3(x, tipY + len * g, z);
            double sink = shatter * 0.6;
            DomaRenderHelper.iceSpike(c.pose, c.buffers, base.add(0, -sink, 0), new Vec3(0, -1, 0), len * g * (1.0 - shatter * 0.5), r * (1.0 - shatter * 0.4),
                    ThreadRenderHelper.scaleAlpha(DomaRenderHelper.ICE, 1.0f - shatter), FULL, c.cam);
        }
        // Impact burst.
        float t = (c.age - 15.0f) / 6.0f;
        if (t >= 0.0f && t < 1.0f) {
            HantenguRenderHelper.soundRing(c.pose, c.buffers, c.cam, new Vec3(0, 0.3, 0), up, 0.5f + 3.0f * t, 0.4f, 0xC0E6F8FF, 1.0f - t);
            for (int i = 0; i < 5; i++) {
                double a = Math.PI * 2.0 * i / 5;
                DomaRenderHelper.puff(c.pose, c.buffers, c.camera, new Vec3(Math.cos(a) * 1.5 * (0.5 + t), 0.6 + t, Math.sin(a) * 1.5 * (0.5 + t)), 2.0f + 2.0f * t, i, 0.5f * (1.0f - t));
            }
        }
    }

    /** Crystalline Divine Child charge: one crystal lotus per clone gathered so far, orbiting Doma. */
    private static void renderCrystalCharge(Ctx c) {
        if (c.owner == null) return;
        int clones = Math.min((int) (c.age / 20.0f), 6);
        float newest = (c.age % 20.0f) / 20.0f;
        Vec3 feet = c.feet();
        for (int i = 0; i < Math.max(clones, 0); i++) {
            double a = c.age * 0.05 + i * (Math.PI * 2.0 / Math.max(1, clones));
            Vec3 p = feet.add(Math.cos(a) * 2.5, 0.6 + 0.15 * Math.sin(c.age * 0.1 + i), Math.sin(a) * 2.5);
            float open = i == clones - 1 ? Mth.clamp(newest * 3.0f, 0.3f, 1.0f) : 1.0f;
            DomaRenderHelper.lotus(c.pose, c.buffers, p, 0.9f, open, (float) a, FULL, c.cam);
        }
        // Next clone forming: a bud rising out of the frost at Doma's feet.
        if (clones < 6) {
            DomaRenderHelper.lotus(c.pose, c.buffers, feet.add(0, 0.1, 0), 0.4f + 0.5f * newest, newest * 0.4f, c.age * 0.1f, FULL, null);
        }
        DomaRenderHelper.puff(c.pose, c.buffers, c.camera, feet.add(0, 0.4, 0), 3.0f, c.age * 0.02f, 0.25f);
    }

    @Override
    public boolean shouldRender(DomaArtEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
        return frustum.isVisible(entity.getBoundingBox().inflate(entity.getMode() == DomaArtEntity.Mode.FREEZING_CLOUDS ? 40.0 : 24.0));
    }

    @Override
    public ResourceLocation getTextureLocation(DomaArtEntity entity) {
        return TEXTURE;
    }
}
