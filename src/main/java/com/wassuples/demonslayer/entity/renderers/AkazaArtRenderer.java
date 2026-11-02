package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.AkazaArtEntity;
import com.wassuples.demonslayer.util.AkazaKicks;
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
 * Akaza's Destructive Death, drawn the way the anime draws it: every punch and kick throws a cyan-white shockwave
 * (layered rings of compressed air with a bright core and speed lines) and kicks leave arcs along the path the
 * leg actually takes. The compass snowflake only appears where it belongs (Compass Needle has its own renderer;
 * here only the final form, which is used after Compass Needle). One renderer for all the modes of
 * {@link AkazaArtEntity}; each mode's timeline mirrors its form's damage ticks.
 */
public class AkazaArtRenderer extends EntityRenderer<AkazaArtEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    static final int CORE = 0xF0F2FAFF;
    static final int GLOW = 0x704CB4FF;
    static final int ICE = 0xA0A8E4FF;
    static final int SILVER = 0xC0E6ECF8;

    public AkazaArtRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(AkazaArtEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 origin = entity.getPosition(partialTick);
        Camera camera = this.entityRenderDispatcher.camera;
        Vec3 cam = camera.getPosition().subtract(origin);
        Entity ownerEntity = entity.getOwner();
        LivingEntity owner = ownerEntity instanceof LivingEntity l ? l : null;
        Minecraft mc = Minecraft.getInstance();
        boolean ownerView = owner != null && owner == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        Ctx c = new Ctx(pose, buffers, cam, age, partialTick, entity, owner, origin, ownerView, camera);
        switch (entity.getMode()) {
            case AIR -> renderAir(c);
            case ANNIHILATION -> renderAnnihilation(c);
            case CROWN -> renderCrown(c);
            case LUNGE -> renderLunge(c);
            case DISORDER -> renderDisorder(c);
            case EIGHT_CORE -> renderEightCore(c);
            case FLURRY -> renderFlurry(c);
            case AFTERGLOW -> renderAfterglow(c);
            case WHEELS -> renderWheels(c);
            case WILLOW -> renderWillow(c);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** Everything a mode needs, with owner-relative helpers (all positions relative to the entity). */
    private record Ctx(PoseStack pose, MultiBufferSource buffers, Vec3 cam, float age, float partialTick,
                       AkazaArtEntity entity, LivingEntity owner, Vec3 origin, boolean ownerView, Camera camera) {
        Vec3 feet() {
            return owner == null ? Vec3.ZERO : owner.getPosition(partialTick).subtract(origin);
        }

        Vec3 eye() {
            return owner == null ? new Vec3(0, 1.6, 0) : owner.getEyePosition(partialTick).subtract(origin);
        }

        Vec3 look() {
            return owner == null ? entity.getAimDirection() : owner.getViewVector(partialTick);
        }

        float body() {
            return owner == null ? 1.0f : owner.getBbHeight() / 1.8f;
        }

        Vec3 forward() {
            if (owner == null) {
                Vec3 d = entity.getAimDirection();
                Vec3 h = new Vec3(d.x, 0, d.z);
                return h.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : h.normalize();
            }
            float yaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
            return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        }

        /** The owner's right (matches PlayerAnimator's positive leg yaw). */
        Vec3 right() {
            Vec3 f = forward();
            return new Vec3(-f.z, 0, f.x);
        }

        float fade(float in, float out) {
            return Mth.clamp(age / in, 0.0f, 1.0f) * Mth.clamp((entity.getDuration() - age) / out, 0.0f, 1.0f);
        }
    }

    // =========================================================================================== modes

    /** Air Type: the fist flies 5 blocks a tick down the aim line, then bursts where it hit (or at full range). */
    private static void renderAir(Ctx c) {
        Vec3 dir = c.entity.getAimDirection().normalize();
        int impact = c.entity.getImpactTick();
        float travel = impact >= 0 ? impact : Math.min(c.age, 5.0f);
        Vec3 front = impact >= 0 ? c.entity.getImpactOffset() : dir.scale(Math.min(c.age * 5.0, 25.0));
        boolean flying = impact < 0 ? c.age <= 5.2f : c.age < impact + 0.5f;
        float trailFade = Mth.clamp(1.0f - (c.age - travel) / 6.0f, 0.0f, 1.0f);
        double length = front.length();
        for (double d = 0.6; d < length; d += 1.4) {
            float k = (float) ((length - d) / 8.0);
            if (k > 1.0f) continue;
            ring(c, dir.scale(d), dir, 0.35f + 0.4f * k, 0.08f, GLOW, (1.0f - k) * trailFade);
        }
        if (flying) fist(c, front, dir, 1.1f, 1.0f, 3, true);
        float t = (c.age - travel) / 8.0f;
        if (t > 0.0f && t < 1.0f) burst(c, front, 0.5f + 3.0f * t, 1.0f - t);
    }

    /**
     * Annihilation Type: crouched with both arms poised at his sides, power gathering into both palms; the lunge
     * drives two great circular shockwaves from the palms; then the cataclysmic impact.
     */
    private static void renderAnnihilation(Ctx c) {
        int release = c.entity.getReleaseTick();
        int impact = c.entity.getImpactTick();
        if (impact >= 0) {
            Vec3 at = c.entity.getImpactOffset();
            float t = (c.age - impact) / 12.0f;
            if (t >= 0.0f && t < 1.0f) {
                float e = 1.0f - (1.0f - t) * (1.0f - t);
                Vec3 dir = c.entity.getAimDirection().normalize();
                // The two palm shockwaves slam through the target as twin discs, then everything detonates.
                for (int side = -1; side <= 1; side += 2) {
                    Vec3 side3 = dir.cross(new Vec3(0, 1, 0));
                    side3 = side3.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side3.normalize();
                    Vec3 p = at.add(0, 1.0, 0).add(side3.scale(side * 0.6)).add(dir.scale(e * 6.0));
                    disc(c, p, dir, 1.6f + 2.0f * e, 1.0f - t);
                }
                for (int k = 0; k < 4; k++) {
                    double tilt = Math.PI * k / 4.0;
                    ring(c, at.add(0, 1.0, 0), new Vec3(Math.cos(tilt), 0.25, Math.sin(tilt)), 1.0f + 7.0f * e, 0.5f, GLOW, 1.0f - t);
                }
                ring(c, at.add(0, 0.15, 0), new Vec3(0, 1, 0), 1.0f + 8.5f * e, 0.9f, ICE, 1.0f - t);
                ring(c, at.add(0, 0.15, 0), new Vec3(0, 1, 0), 0.5f + 6.0f * e, 0.4f, CORE, 1.0f - t);
                burst(c, at.add(0, 1.0, 0), 1.0f + 4.0f * e, 1.0f - t);
            }
            return;
        }
        if (c.owner == null) return;
        Vec3 feet = c.feet();
        Vec3 f = c.forward();
        Vec3 r = c.right();
        float body = c.body();
        if (release < 0) {
            float charge = Mth.clamp(c.age / 30.0f, 0.0f, 1.0f);
            for (int side = -1; side <= 1; side += 2) {
                Vec3 palm = feet.add(0, 0.85 * body, 0).add(r.scale(side * 0.5 * body)).subtract(f.scale(0.25 * body));
                for (int k = 0; k < 3; k++) {
                    float q = ((c.age * 0.07f) + k / 3.0f + (side > 0 ? 0.0f : 0.17f)) % 1.0f;
                    ring(c, palm, f, (1.0f - q) * 1.8f + 0.15f, 0.1f, k % 2 == 0 ? GLOW : ICE, q * (0.35f + 0.65f * charge));
                }
                ring(c, palm, f, 0.22f + 0.08f * Mth.sin(c.age * 0.8f), 0.08f, CORE, charge);
                VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
                for (int k = 0; k < 4; k++) {
                    double a = c.age * 0.35 + k * Math.PI / 2.0 + side;
                    float q = ((c.age * 0.1f) + k / 4.0f) % 1.0f;
                    double rad = 2.4 * (1.0 - q);
                    Vec3 p0 = palm.add(f.scale(Math.cos(a) * rad)).add(0, Math.sin(a) * rad * 0.6, 0).add(r.scale(side * Math.sin(a) * rad * 0.5));
                    Vec3 p1 = palm.add(f.scale(Math.cos(a + 0.4) * rad * 0.7)).add(0, Math.sin(a + 0.4) * rad * 0.42, 0);
                    ThreadRenderHelper.ribbon(c.pose, vc, c.cam, p0, p1, 0.07f, ThreadRenderHelper.scaleAlpha(CORE, charge * (1.0f - q)));
                }
            }
        } else {
            // Lunge: both palms forward, a circular shockwave spinning in front of each, a blazing wake behind.
            Vec3 dir = c.entity.getAimDirection().normalize();
            Vec3 chest = feet.add(0, 1.15 * body, 0);
            for (int side = -1; side <= 1; side += 2) {
                Vec3 palm = chest.add(r.scale(side * 0.35 * body)).add(dir.scale(1.0));
                disc(c, palm.add(dir.scale(0.3)), dir, 1.25f, 1.0f);
            }
            VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, chest, chest.subtract(dir.scale(9.0)), 2.4f, ThreadRenderHelper.scaleAlpha(GLOW, 0.7f));
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, chest, chest.subtract(dir.scale(5.0)), 0.8f, ThreadRenderHelper.scaleAlpha(ICE, 0.8f));
            for (int k = 1; k <= 5; k++) {
                ring(c, chest.subtract(dir.scale(k * 1.6)), dir, 1.1f + k * 0.25f, 0.12f, GLOW, 1.0f - k / 6.0f);
            }
        }
    }

    /**
     * Crown Splitter: a reverse, rising axe kick. Akaza spins and his heel rises from low behind him, sweeps up
     * over the top and comes down on the target's crown (tick 4); the shockwave bursts from the crown, rolls down
     * the target and spreads across the ground.
     */
    private static void renderCrown(Ctx c) {
        Vec3 dir = c.entity.getAimDirection();
        Vec3 f = new Vec3(dir.x, 0, dir.z);
        f = f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        // Arc centred between Akaza (1.5 blocks back along the aim) and the target, in their vertical plane.
        Vec3 centre = up.scale(1.2).subtract(f.scale(0.75));
        float radius = 1.9f;
        float startA = (float) Math.toRadians(205);  // low behind Akaza
        float endA = (float) Math.toRadians(25);     // down onto the target's head
        float reveal = Mth.clamp(c.age / 4.0f, 0.0f, 1.0f);
        reveal = reveal * reveal * (3 - 2 * reveal);
        float arcFade = Mth.clamp(1.0f - (c.age - 4.0f) / 6.0f, 0.0f, 1.0f);
        float head = startA + (endA - startA) * reveal;
        crescent(c, centre, f, up, radius, startA, head, 0.75f, arcFade);
        // Heel glint at the leading end of the arc.
        if (c.age < 5.0f) {
            Vec3 heel = centre.add(f.scale(Math.cos(head) * radius)).add(up.scale(Math.sin(head) * radius));
            ring(c, heel, up, 0.35f, 0.12f, CORE, 1.0f);
        }
        float t = (c.age - 4.0f) / 8.0f;
        if (t >= 0.0f && t < 1.0f) {
            Vec3 crown = up.scale(1.9);
            burst(c, crown, 0.5f + 2.6f * t, 1.0f - t);
            // Rings rolling down the target from the crown, then out over the ground.
            for (int k = 0; k < 4; k++) {
                float y = 1.9f - (t * 3.0f - k * 0.35f) * 1.9f;
                if (y < 0.0f || y > 1.9f) continue;
                ring(c, up.scale(y), up, 0.6f + 0.15f * k, 0.15f, k % 2 == 0 ? CORE : GLOW, 1.0f - t);
            }
            ring(c, new Vec3(0, 0.15, 0), up, 0.5f + 5.0f * t, 0.6f, GLOW, 1.0f - t);
            ring(c, new Vec3(0, 0.15, 0), up, 0.3f + 4.2f * t, 0.2f, CORE, 1.0f - t);
        }
    }

    /** Crown Splitter with nothing in reach: speed lines streaming off Akaza as he lunges. */
    private static void renderLunge(Ctx c) {
        if (c.owner == null || c.ownerView) return;
        Vec3 f = c.forward();
        Vec3 feet = c.feet();
        float b = c.fade(1.0f, 4.0f);
        VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
        Random rnd = new Random(c.entity.getId() * 31L + (long) c.age);
        for (int k = 0; k < 9; k++) {
            Vec3 p = feet.add(0, (0.2 + rnd.nextDouble() * 1.6) * c.body(), 0).add(c.right().scale((rnd.nextDouble() - 0.5) * 1.0));
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, p, p.subtract(f.scale(2.0 + rnd.nextDouble() * 2.0)), 0.06f, ThreadRenderHelper.scaleAlpha(CORE, b));
        }
    }

    /**
     * Disorder: a barrage of large shockwaves punched through the forward cone. Drawn as hollow rings and speed
     * lines only (no filled glow), starting a few blocks out and dimmed in Akaza's own first-person view, so the
     * player can still see what's in front of them.
     */
    private static void renderDisorder(Ctx c) {
        if (c.owner == null) return;
        float b = c.fade(4.0f, 5.0f) * (c.ownerView ? 0.4f : 0.85f);
        Vec3 eye = c.eye();
        Vec3 look = c.look();
        int now = (int) c.age;
        for (int s = now - 4; s <= now; s++) {
            if (s < 1) continue;
            float f = (c.age - s) / 4.0f;
            if (f <= 0.0f || f >= 1.0f) continue;
            int count = 2 + s / 30;
            for (int k = 0; k < count; k++) {
                Random rnd = new Random(c.entity.getId() * 73856093L ^ s * 19349663L ^ k * 83492791L);
                float yawOff = (float) Math.toRadians((rnd.nextDouble() - 0.5) * 90.0);
                float pitchOff = (float) Math.toRadians((rnd.nextDouble() - 0.5) * 70.0);
                Vec3 d = look.yRot(yawOff).xRot(pitchOff).normalize();
                double dist = 3.0 + (4.0 + rnd.nextDouble() * 8.0) * f;
                Vec3 at = eye.add(d.scale(dist));
                fist(c, at, d, 0.9f + 0.6f * f, b * (1.0f - f * 0.7f), 2, false);
            }
        }
    }

    /** Eight punches, five ticks apart: each throws a large fist with one more layer of rings, then a final push. */
    private static void renderEightCore(Ctx c) {
        if (c.owner == null) return;
        Vec3 eye = c.eye();
        Vec3 look = c.look();
        float dim = c.ownerView ? 0.6f : 1.0f;
        for (int k = 1; k <= 8; k++) {
            float s = c.age - k * 5;
            if (s < 0.0f || s > 7.0f) continue;
            float f = Mth.clamp(s / 3.0f, 0.0f, 1.0f);
            float b = (1.0f - Mth.clamp((s - 3.0f) / 4.0f, 0.0f, 1.0f)) * dim;
            Vec3 at = eye.add(look.scale(2.0 + 10.0 * f));
            fist(c, at, look, 1.2f + 0.15f * k, b, k, !c.ownerView);
            if (s < 2.0f) ring(c, eye.add(look.scale(1.6)), look, 0.6f + s * 1.2f, 0.15f, CORE, (1.0f - s / 2.0f) * dim);
        }
        float t = (c.age - 40.0f) / 8.0f;
        if (t > 0.0f && t < 1.0f) {
            burst(c, eye.add(look.scale(5.0)), 1.0f + 5.5f * t, (1.0f - t) * dim);
            for (int k = 0; k < 3; k++) {
                ring(c, eye.add(look.scale(3.0 + k * 3.0 * t)), look, 1.8f + 3.5f * t + k, 0.3f, GLOW, (1.0f - t) * dim);
            }
        }
    }

    /**
     * Explosive Flurry: five straight kicks, each swept along its own restricted random arc (the same arcs the
     * player animation moves the legs along, from the shared seed), leaving afterimages of the leg along the arc
     * and a shockwave off the foot. Then the finishing kick's burst on the target.
     */
    private static void renderFlurry(Ctx c) {
        int impact = c.entity.getImpactTick();
        if (impact >= 0) {
            float t = (c.age - impact) / 9.0f;
            if (t >= 0.0f && t < 1.0f) {
                Vec3 at = c.entity.getImpactOffset();
                burst(c, at, 0.8f + 3.6f * t, 1.0f - t);
                ring(c, at, new Vec3(0, 1, 0), 0.5f + 4.0f * t, 0.5f, GLOW, 1.0f - t);
            }
            return;
        }
        if (c.owner == null) return;
        int seed = c.entity.getSeed();
        Vec3 f = c.forward();
        Vec3 r = c.right();
        Vec3 up = new Vec3(0, 1, 0);
        float body = c.body();
        float legLen = 0.8f * body;
        VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
        for (int k = 1; k <= AkazaKicks.KICKS; k++) {
            int strike = AkazaKicks.strikeTick(k);
            float s = c.age - (strike - 1); // 0..1 = the sweep along the arc, then it fades
            if (s < 0.0f || s > 4.0f) continue;
            AkazaKicks.Kick kick = AkazaKicks.kick(seed, k);
            Vec3 hip = c.feet().add(0, 0.75 * body, 0).add(r.scale((kick.rightLeg() ? 0.12 : -0.12) * body));
            float sweep = Mth.clamp(s, 0.0f, 1.0f);
            float fade = 1.0f - Mth.clamp((s - 1.0f) / 3.0f, 0.0f, 1.0f);
            int samples = 10;
            Vec3 prevFoot = null;
            for (int i = 0; i <= samples; i++) {
                float t = sweep * i / samples;
                Vec3 foot = hip.add(legDir(kick.pitchAt(t), kick.yawAt(t), f, r, up).scale(legLen));
                if (prevFoot != null) {
                    float w = 0.12f + 0.25f * Mth.sin((float) Math.PI * i / samples);
                    ThreadRenderHelper.ribbon(c.pose, vc, c.cam, prevFoot, foot, w * 2.4f, ThreadRenderHelper.scaleAlpha(GLOW, fade * 0.8f));
                    ThreadRenderHelper.ribbon(c.pose, vc, c.cam, prevFoot, foot, w * 0.6f, ThreadRenderHelper.scaleAlpha(CORE, fade));
                }
                // Afterimages of the whole leg every few samples.
                if (i % 3 == 0 && i > 0) {
                    ThreadRenderHelper.ribbon(c.pose, vc, c.cam, hip, foot, 0.22f * body, ThreadRenderHelper.scaleAlpha(0x406AA8FF, fade * (0.4f + 0.6f * i / samples)));
                }
                prevFoot = foot;
            }
            // The shockwave off the foot as the kick lands.
            if (s >= 1.0f && prevFoot != null) {
                Vec3 kickDir = legDir(kick.endPitch(), kick.endYaw(), f, r, up);
                float q = Mth.clamp((s - 1.0f) / 2.5f, 0.0f, 1.0f);
                fist(c, prevFoot.add(kickDir.scale(0.4 + q * 2.5)), kickDir, 0.7f + 0.5f * q, 1.0f - q, 3, true);
                vc = ThreadRenderHelper.buffer(c.buffers);
            }
        }
    }

    /**
     * Direction of a leg for PlayerAnimator angles: hanging straight down at pitch 0, swinging forward/up as the
     * pitch goes negative, turned toward the body's right by positive yaw.
     */
    private static Vec3 legDir(float pitch, float yaw, Vec3 f, Vec3 r, Vec3 up) {
        Vec3 swung = up.scale(-Math.cos(pitch)).add(f.scale(-Math.sin(pitch)));
        double h = -Math.sin(pitch);
        Vec3 horizontal = f.scale(Math.cos(yaw)).add(r.scale(Math.sin(yaw))).scale(h);
        return new Vec3(horizontal.x, swung.y, horizontal.z);
    }

    /**
     * Final Form: Blue Silver Chaotic Afterglow. Compass Needle unfolds to the attack's full 16-block reach, twelve
     * red energy clones of Akaza rise on its points, and they all lash out at nothing: an ever-thickening storm of
     * thin needle-like shockwaves fired in every direction. On the release a hundred needles close on every target
     * from everywhere at once, the air detonates in blue and silver and the afterglow hangs in the air.
     */
    private static void renderAfterglow(Ctx c) {
        int impact = c.entity.getImpactTick();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 centre = impact >= 0 ? c.entity.getImpactOffset() : c.feet();
        float build = Mth.clamp(c.age / 25.0f, 0.0f, 1.0f);
        float sinceRelease = impact >= 0 ? c.age - impact : -1.0f;
        float after = sinceRelease < 0 ? 0.0f : Mth.clamp(sinceRelease / 25.0f, 0.0f, 1.0f);
        float life = 1.0f - after;
        float radius = 16.0f;

        // Compass Needle unfolding to full reach under him.
        float unfold = Mth.clamp(c.age / 8.0f, 0.0f, 1.0f);
        float pulse = sinceRelease >= 0 && sinceRelease < 6 ? 1.0f - sinceRelease / 6.0f : 0.3f * build;
        AkazaRenderHelper.compass(c.pose, c.buffers, centre, radius, unfold, (0.55f + 0.45f * build) * life, c.age * 0.01f, pulse);

        // Twelve red energy clones on the compass points, each lashing out at nothing.
        float cloneIn = Mth.clamp((c.age - 4.0f) / 8.0f, 0.0f, 1.0f) * life;
        Vec3[] cloneFeet = new Vec3[12];
        for (int i = 0; i < 12; i++) {
            double a = Math.PI / 2.0 - i * Math.PI / 6.0 + c.age * 0.01f;
            double at = (i % 2 == 0 ? 0.875 : 0.555) * radius;
            Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
            cloneFeet[i] = centre.add(out.scale(at));
            if (cloneIn > 0.0f) {
                float rise = Mth.clamp((c.age - 4.0f - i * 0.4f) / 6.0f, 0.0f, 1.0f);
                AkazaRenderHelper.energyClone(c.pose, c.buffers, cloneFeet[i].add(0, -1.8 * (1.0 - rise), 0), out.yRot((float) Math.sin(c.age * 0.3 + i) * 0.6f),
                        1.9f, c.age + i * 1.3f, cloneIn * rise);
            }
        }

        // The storm of needles from Akaza and every clone, thickening up to the release and raging after it.
        float stormRate = impact < 0 ? Mth.clamp((c.age - 6.0f) / 19.0f, 0.0f, 1.0f) : life;
        if (stormRate > 0.0f) {
            VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
            int now = (int) c.age;
            for (int s = now - 3; s <= now; s++) {
                if (s < 0) continue;
                float q = (c.age - s) / 3.0f;
                if (q <= 0.0f || q >= 1.0f) continue;
                int count = (int) (6 + 30 * stormRate);
                for (int k = 0; k < count; k++) {
                    Random rnd = new Random(c.entity.getId() * 486187739L ^ s * 2654435761L ^ k * 40503L);
                    int src = rnd.nextInt(13);
                    Vec3 from = src == 12 ? centre.add(0, 1.3 * c.body(), 0) : cloneFeet[src].add(0, 1.3, 0);
                    Vec3 d = new Vec3(rnd.nextGaussian(), rnd.nextGaussian() * 0.6, rnd.nextGaussian()).normalize();
                    double reach = 4.0 + rnd.nextDouble() * 10.0;
                    Vec3 head = from.add(d.scale(reach * q + 0.5));
                    Vec3 tail = from.add(d.scale(Math.max(0.0, reach * q - 2.5)));
                    AkazaRenderHelper.needle(c.pose, vc, c.cam, tail, head, (1.0f - q) * (c.ownerView && src == 12 ? 0.35f : 1.0f));
                }
            }
        }

        // Gathering: blue-silver rings drawn in toward him.
        if (impact < 0) {
            for (int k = 0; k < 5; k++) {
                float q = ((c.age * 0.07f) + k / 5.0f) % 1.0f;
                ring(c, centre.add(0, 0.2 + k * 0.45, 0), up, 6.0f * (1.0f - q) + 0.3f, 0.25f, k % 2 == 0 ? GLOW : SILVER, q * build);
            }
            return;
        }

        // ---- The release ----
        float blast = Mth.clamp(sinceRelease / 8.0f, 0.0f, 1.0f);
        float blastFade = 1.0f - blast;
        if (blast < 1.0f) {
            float e = 1.0f - (1.0f - blast) * (1.0f - blast);
            // Shell of light racing out to the full reach, on many axes so it reads as a sphere.
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * k / 8.0;
                Vec3 n = new Vec3(Math.cos(a), 0.0, Math.sin(a));
                ring(c, centre.add(0, 1.0, 0), n, 1.0f + (radius + 2.0f) * e, 0.9f, k % 2 == 0 ? GLOW : SILVER, blastFade);
            }
            ring(c, centre.add(0, 1.0, 0), up, 1.0f + (radius + 2.0f) * e, 1.2f, ICE, blastFade);
            ring(c, centre.add(0, 0.1, 0), up, 1.0f + (radius + 6.0f) * e, 1.6f, GLOW, blastFade);
            ring(c, centre.add(0, 0.1, 0), up, 0.5f + (radius + 2.0f) * e, 0.5f, CORE, blastFade);
            burst(c, centre.add(0, 1.2, 0), 2.0f + 6.0f * e, blastFade);
            // Column of light straight up.
            VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, centre, centre.add(0, 40.0 * e + 4.0, 0), 5.0f * blastFade + 0.5f, ThreadRenderHelper.scaleAlpha(GLOW, blastFade));
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, centre, centre.add(0, 40.0 * e + 4.0, 0), 1.5f * blastFade + 0.2f, ThreadRenderHelper.scaleAlpha(CORE, blastFade));
        }

        // A hundred needles closing on every target from everywhere almost at once, then each target detonates.
        List<Vec3> targets = c.entity.getExtraPoints();
        float converge = Mth.clamp(sinceRelease / 4.0f, 0.0f, 1.0f);
        if (sinceRelease < 12.0f) {
            VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
            for (int ti = 0; ti < targets.size(); ti++) {
                Vec3 target = targets.get(ti).add(0, 1.0, 0);
                Random rnd = new Random(c.entity.getId() * 7919L + ti * 104729L);
                int needles = targets.size() > 8 ? 40 : 100;
                for (int k = 0; k < needles; k++) {
                    Vec3 d = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize();
                    double startR = 5.0 + rnd.nextDouble() * 5.0;
                    float delay = rnd.nextFloat() * 0.25f;
                    float q = Mth.clamp((converge - delay) / 0.75f, 0.0f, 1.0f);
                    if (q <= 0.0f) continue;
                    Vec3 head = target.add(d.scale(startR * (1.0 - q)));
                    Vec3 tail = target.add(d.scale(startR * (1.0 - q) + 2.0));
                    AkazaRenderHelper.needle(c.pose, vc, c.cam, tail, head, 1.0f - Mth.clamp((sinceRelease - 6.0f) / 6.0f, 0.0f, 1.0f));
                }
            }
            for (Vec3 target : targets) {
                float bt = Mth.clamp((sinceRelease - 3.0f) / 9.0f, 0.0f, 1.0f);
                if (bt > 0.0f && bt < 1.0f) {
                    burst(c, target.add(0, 1.0, 0), 0.8f + 3.5f * bt, 1.0f - bt);
                    ring(c, target.add(0, 1.0, 0), new Vec3(0, 1, 0), 0.5f + 2.5f * bt, 0.4f, SILVER, 1.0f - bt);
                }
            }
        }

        // The afterglow: blue and silver motes drifting up over the whole area long after the blast.
        if (sinceRelease > 2.0f) {
            VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
            for (int k = 0; k < 60; k++) {
                Random rnd = new Random(c.entity.getId() * 31337L + k);
                double a = rnd.nextDouble() * Math.PI * 2.0;
                double rr = Math.sqrt(rnd.nextDouble()) * radius;
                double h = (sinceRelease - 2.0f) * (0.08 + rnd.nextDouble() * 0.15) + rnd.nextDouble() * 1.5;
                Vec3 p = centre.add(Math.cos(a) * rr, h, Math.sin(a) * rr);
                ThreadRenderHelper.ribbon(c.pose, vc, c.cam, p, p.add(0, 0.35, 0), 0.12f,
                        ThreadRenderHelper.scaleAlpha(k % 2 == 0 ? ICE : SILVER, life * 0.9f));
            }
        }

        // Blinding flash for anyone close enough to see it, right in front of the camera.
        if (sinceRelease >= 0.0f && sinceRelease < 6.0f && c.cam.distanceTo(centre) < radius * 2.0f) {
            flash(c, (1.0f - sinceRelease / 6.0f) * 0.85f);
        }
    }

    /**
     * Flying Planet Thousand Wheels: one flowing, upward straight kick. The foot rises from in front of Akaza
     * straight up into the sky (drawn along the path the attack zone travels), throwing a column of shock skyward
     * that carries the target and Akaza with it.
     */
    private static void renderWheels(Ctx c) {
        if (c.owner == null) return;
        float b = c.fade(1.0f, 6.0f);
        Vec3 look = c.look();
        Vec3 f = new Vec3(look.x, 0, look.z);
        f = f.lengthSqr() < 1.0E-4 ? c.forward() : f.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 feet = c.feet();
        float body = c.body();
        // The kick arc: foot sweeping from low in front, up through vertical (ticks 0-4).
        float reveal = Mth.clamp(c.age / 4.0f, 0.0f, 1.0f);
        reveal = 1.0f - (1.0f - reveal) * (1.0f - reveal);
        Vec3 hip = feet.add(0, 0.8 * body, 0);
        float a0 = (float) Math.toRadians(-60), a1 = (float) Math.toRadians(100);
        crescent(c, hip, f, up, 1.5f * body, a0, a0 + (a1 - a0) * reveal, 0.65f, b * Mth.clamp(1.0f - (c.age - 4.0f) / 6.0f, 0.0f, 1.0f));
        // The shock column rising with the attack zone (form: 1.5 + p ahead, up to 4 blocks high over 12 ticks).
        float p = Mth.clamp(c.age / 12.0f, 0.0f, 1.0f);
        Vec3 zone = feet.add(look.scale(1.5 + p)).add(0, 4.0 * p, 0);
        Vec3 base = feet.add(look.scale(1.5));
        for (int k = 0; k < 6; k++) {
            float q = ((c.age * 0.25f) + k / 6.0f) % 1.0f;
            Vec3 at = base.lerp(zone, q).add(0, 0.5, 0);
            ring(c, at, up, 0.6f + 1.4f * q, 0.14f, k % 2 == 0 ? CORE : GLOW, b * (1.0f - q * 0.6f));
        }
        VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
        ThreadRenderHelper.ribbon(c.pose, vc, c.cam, base, zone.add(0, 3.0, 0), 1.6f, ThreadRenderHelper.scaleAlpha(GLOW, b * 0.6f));
        ThreadRenderHelper.ribbon(c.pose, vc, c.cam, base, zone.add(0, 3.0, 0), 0.4f, ThreadRenderHelper.scaleAlpha(CORE, b * 0.8f));
        // Burst where the kick connects (top of its arc).
        float t = (c.age - 4.0f) / 7.0f;
        if (t >= 0.0f && t < 1.0f) burst(c, zone.add(0, 0.5, 0), 0.6f + 2.4f * t, (1.0f - t) * b);
    }

    /** Ten Thousand Leaves Flashing Willow: a downward punch into the ground and a ring of shattered earth. */
    private static void renderWillow(Ctx c) {
        float p = Mth.clamp(c.age / 20.0f, 0.0f, 1.0f);
        float r = 12.0f * p;
        float b = c.fade(1.0f, 5.0f);
        Vec3 up = new Vec3(0, 1, 0);
        if (c.age < 6.0f) {
            float t = c.age / 6.0f;
            burst(c, new Vec3(0, 0.3, 0), 0.6f + 2.0f * t, 1.0f - t);
            for (int k = 0; k < 4; k++) ring(c, new Vec3(0, 0.2 + k * 0.6 * t * 3, 0), up, 0.6f + 0.4f * k + t, 0.2f, CORE, 1.0f - t);
        }
        if (p < 1.0f || c.age < 24.0f) {
            float wave = p < 1.0f ? 1.0f : Mth.clamp(1.0f - (c.age - 20.0f) / 4.0f, 0.0f, 1.0f);
            ring(c, new Vec3(0, 0.2, 0), up, r, 0.9f, GLOW, wave * b);
            ring(c, new Vec3(0, 0.2, 0), up, r * 0.88f, 0.3f, CORE, wave * b);
            ring(c, new Vec3(0, 0.8, 0), up, r * 0.95f, 0.4f, ICE, wave * b * 0.6f);
        }
        cracks(c, r, 18, b);
        for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2.0 * i / 28 + (i % 3) * 0.07;
            double rr = Math.max(0.0, r - 0.4 - (i % 4) * 0.35);
            if (rr <= 0.5) continue;
            double h = 0.25 + Math.abs(Math.sin(c.age * 0.6 + i)) * 0.9 * (1.0 - p * 0.5);
            float s = 0.12f + (i % 5) * 0.04f;
            float x = (float) (Math.cos(a) * rr), z = (float) (Math.sin(a) * rr), y = (float) h;
            int grey = (i % 3 == 0) ? 0xFF6E6A66 : (i % 3 == 1 ? 0xFF5B4B3C : 0xFF8A857F);
            ToonRenderHelper.solidBox(c.pose, c.buffers, grey, 0xF000F0, x - s, y - s, z - s, x + s, y + s, z + s, 0.0f);
        }
    }

    // =========================================================================================== pieces

    private static void ring(Ctx c, Vec3 centre, Vec3 normal, float radius, float thickness, int argb, float brightness) {
        if (brightness <= 0.0f || radius <= 0.01f) return;
        HantenguRenderHelper.soundRing(c.pose, c.buffers, c.cam, centre, normal, radius, thickness, argb, brightness);
    }

    /**
     * A fist of compressed air flying along {@code dir}: {@code layers} stacked rings shrinking back from the
     * front and speed lines streaming off the rim; {@code filled} adds the bright glowing core (left out where it
     * would cover the player's view).
     */
    private static void fist(Ctx c, Vec3 at, Vec3 dir, float size, float brightness, int layers, boolean filled) {
        if (brightness <= 0.0f) return;
        Vec3 d = dir.normalize();
        for (int l = 0; l < Math.max(1, layers); l++) {
            float k = l / (float) Math.max(1, layers);
            ring(c, at.subtract(d.scale(l * 0.35 * size)), d, size * (1.0f - k * 0.45f), 0.12f * size, l == 0 ? CORE : GLOW, brightness * (1.0f - k * 0.5f));
        }
        VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
        if (filled) {
            Vec3 tail = at.subtract(d.scale(size * 1.4));
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, tail, at.add(d.scale(0.25 * size)), size * 1.1f, ThreadRenderHelper.scaleAlpha(GLOW, brightness * 0.7f));
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, at.subtract(d.scale(size * 0.8)), at.add(d.scale(0.2 * size)), size * 0.35f, ThreadRenderHelper.scaleAlpha(CORE, brightness));
        }
        Vec3 u = d.cross(Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 v = d.cross(u).normalize();
        for (int s = 0; s < 6; s++) {
            double a = Math.PI * 2.0 * s / 6 + size;
            Vec3 rim = at.add(u.scale(Math.cos(a) * size * 0.85)).add(v.scale(Math.sin(a) * size * 0.85));
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, rim, rim.subtract(d.scale(size * 1.8)), 0.05f * Math.max(1.0f, size), ThreadRenderHelper.scaleAlpha(CORE, brightness * 0.7f));
        }
    }

    /** A circular shockwave disc facing {@code dir}: concentric rings filling it, a bright rim and a spinning spoke. */
    private static void disc(Ctx c, Vec3 at, Vec3 dir, float radius, float brightness) {
        if (brightness <= 0.0f) return;
        for (int k = 0; k < 4; k++) {
            float rr = radius * (1.0f - k * 0.22f);
            ring(c, at.add(dir.normalize().scale(k * 0.05)), dir, rr, k == 0 ? 0.22f : 0.12f, k == 0 ? CORE : GLOW, brightness * (1.0f - k * 0.15f));
        }
        Vec3 d = dir.normalize();
        Vec3 u = d.cross(Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 v = d.cross(u).normalize();
        VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
        for (int s = 0; s < 3; s++) {
            double a = c.age * 0.9 + s * Math.PI * 2.0 / 3.0;
            Vec3 e = at.add(u.scale(Math.cos(a) * radius)).add(v.scale(Math.sin(a) * radius));
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, at, e, 0.1f, ThreadRenderHelper.scaleAlpha(ICE, brightness * 0.8f));
        }
    }

    private static void burst(Ctx c, Vec3 at, float radius, float brightness) {
        if (brightness <= 0.0f) return;
        ring(c, at, new Vec3(0, 1, 0), radius, 0.3f, GLOW, brightness);
        ring(c, at, new Vec3(1, 0.2, 0), radius * 0.85f, 0.25f, ICE, brightness);
        ring(c, at, new Vec3(0, 0.2, 1), radius * 0.85f, 0.25f, CORE, brightness * 0.8f);
        VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
        for (int s = 0; s < 10; s++) {
            double a = Math.PI * 2.0 * s / 10;
            double el = ((s * 37) % 7 - 3) * 0.25;
            Vec3 dir = new Vec3(Math.cos(a) * Math.cos(el), Math.sin(el), Math.sin(a) * Math.cos(el));
            ThreadRenderHelper.ribbon(c.pose, vc, c.cam, at.add(dir.scale(radius * 0.4)), at.add(dir.scale(radius * 1.15)), 0.12f,
                    ThreadRenderHelper.scaleAlpha(CORE, brightness));
        }
    }

    private static void crescent(Ctx c, Vec3 centre, Vec3 u, Vec3 v, float radius, float a0, float a1, float width, float brightness) {
        if (brightness <= 0.0f || Math.abs(a1 - a0) < 1.0E-3f) return;
        VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
        int segments = 18;
        Vec3 prev = null;
        for (int s = 0; s <= segments; s++) {
            float t = (float) s / segments;
            double a = a0 + (a1 - a0) * t;
            Vec3 p = centre.add(u.scale(Math.cos(a) * radius)).add(v.scale(Math.sin(a) * radius));
            if (prev != null) {
                float w = width * Mth.sin((float) Math.PI * Math.min(1.0f, t * 1.15f)) + 0.03f;
                ThreadRenderHelper.ribbon(c.pose, vc, c.cam, prev, p, w * 2.2f, ThreadRenderHelper.scaleAlpha(GLOW, brightness * 0.8f));
                ThreadRenderHelper.ribbon(c.pose, vc, c.cam, prev, p, w * 0.7f, ThreadRenderHelper.scaleAlpha(CORE, brightness));
            }
            prev = p;
        }
    }

    private static void cracks(Ctx c, float length, int count, float alpha) {
        if (alpha <= 0.0f || length < 0.5f) return;
        Random rnd = new Random(c.entity.getId() * 6151L);
        int color = ToonRenderHelper.withAlpha(0xD0181410, alpha);
        for (int k = 0; k < count; k++) {
            double a = Math.PI * 2.0 * (k + rnd.nextDouble() * 0.5) / count;
            Vec3 prev = new Vec3(0, 0.03, 0);
            double width = 0.12;
            for (int s = 1; s <= 6; s++) {
                double d = length * s / 6;
                double aa = a + (rnd.nextDouble() - 0.5) * 0.25;
                Vec3 p = new Vec3(Math.cos(aa) * d, 0.03, Math.sin(aa) * d);
                Vec3 dir = p.subtract(prev);
                Vec3 side = new Vec3(-dir.z, 0, dir.x);
                if (side.lengthSqr() < 1.0E-8) continue;
                side = side.normalize();
                double w2 = width * 0.8;
                ToonRenderHelper.quad3(c.pose, c.buffers, color,
                        (float) (prev.x - side.x * width), (float) prev.y, (float) (prev.z - side.z * width),
                        (float) (prev.x + side.x * width), (float) prev.y, (float) (prev.z + side.z * width),
                        (float) (p.x + side.x * w2), (float) p.y, (float) (p.z + side.z * w2),
                        (float) (p.x - side.x * w2), (float) p.y, (float) (p.z - side.z * w2),
                        0, 1, 0);
                prev = p;
                width = w2;
            }
        }
    }

    /** Blue-white flash filling the view: a big additive quad just in front of the camera. */
    private static void flash(Ctx c, float strength) {
        if (strength <= 0.0f) return;
        Vec3 look = new Vec3(c.camera.getLookVector());
        Vec3 upV = new Vec3(c.camera.getUpVector());
        Vec3 leftV = new Vec3(c.camera.getLeftVector());
        Vec3 centre = c.cam.add(look.scale(0.3));
        VertexConsumer vc = ThreadRenderHelper.buffer(c.buffers);
        int a = (int) (200 * strength);
        float w = 1.2f;
        Vec3[] q = {
                centre.add(leftV.scale(w)).add(upV.scale(w)), centre.subtract(leftV.scale(w)).add(upV.scale(w)),
                centre.subtract(leftV.scale(w)).subtract(upV.scale(w)), centre.add(leftV.scale(w)).subtract(upV.scale(w))};
        var m = c.pose.last().pose();
        for (int pass = 0; pass < 2; pass++) {
            for (int j = 0; j < 4; j++) {
                Vec3 p = q[pass == 0 ? j : 3 - j];
                vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(200, 225, 255, a).endVertex();
            }
        }
    }

    @Override
    public boolean shouldRender(AkazaArtEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        return entity.getMode() == AkazaArtEntity.Mode.AFTERGLOW || frustum.isVisible(entity.getBoundingBox().inflate(20.0));
    }

    @Override
    public ResourceLocation getTextureLocation(AkazaArtEntity entity) {
        return TEXTURE;
    }
}
