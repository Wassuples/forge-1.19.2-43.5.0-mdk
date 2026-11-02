package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CuttingThreadCageEntity;
import com.wassuples.demonslayer.entity.custom.ThreadBasketEntity;
import com.wassuples.demonslayer.entity.custom.ThreadTrapEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Renders Rui's two traps.
 * <ul>
 *   <li><b>Cutting Thread Cage</b>: a white spider-web net (spokes + polygon rings) unfurls in the air above the
 *   target. Its point stays fixed up there while the rim drops to the ground around the target, forming a cone
 *   of web, and the rim then constricts inward into a thin spire.</li>
 *   <li><b>Murderous Eye Basket</b>: a cat's-cradle star of blood threads spreads on the ground under the target,
 *   rises into a sphere of threads around them, tightens, then shrinks rapidly. While it lives, the caster
 *   holds a cat's cradle of red threads between their hands.</li>
 * </ul>
 * Threads tighten (redder, brighter) as the trap contracts and flash white on every cut.
 */
public class ThreadTrapRenderer<T extends ThreadTrapEntity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ThreadTrapRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    /** The basket also draws the cradle at the caster's hands, so keep rendering while the caster is in view. */
    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, frustum, camX, camY, camZ)) return true;
        if (entity instanceof ThreadBasketEntity) {
            Entity owner = entity.level.getEntity(entity.getOwnerEntityId());
            return owner != null && owner.distanceToSqr(camX, camY, camZ) < 64 * 64;
        }
        return false;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 origin = entity.getPosition(partialTick);
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(origin);

        float contract = entity.contractProgress(age);
        float flash = entity.cutFlash(age);
        float fadeOut = Mth.clamp((entity.totalTicks() - age + 3.0f) / 3.0f, 0.0f, 1.0f);
        float brightness = (0.75f + 0.25f * contract + 0.6f * flash) * fadeOut;

        boolean blood = entity.bloodThreads();
        int core = blood ? ThreadRenderHelper.BLOOD_CORE
                : ThreadRenderHelper.mix(ThreadRenderHelper.WHITE_CORE, ThreadRenderHelper.BLOOD_CORE, contract * 0.6f);
        int glow = blood ? ThreadRenderHelper.BLOOD_GLOW
                : ThreadRenderHelper.mix(ThreadRenderHelper.WHITE_GLOW, ThreadRenderHelper.BLOOD_GLOW, contract);
        if (flash > 0) core = ThreadRenderHelper.mix(core, 0xFFFFFFFF, flash);

        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
        if (entity instanceof ThreadBasketEntity basket) {
            renderBasket(basket, pose, vc, cam, age, core, glow, brightness);
            renderCradle(basket, pose, vc, cam, origin, age, partialTick, fadeOut);
        } else {
            renderNet(entity, pose, vc, cam, age, core, glow, brightness);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    // ------------------------------------------------------------------ Cutting Thread Cage: falling web net

    private static final int SPOKES = 14;
    private static final int RINGS = 6;

    private static void renderNet(ThreadTrapEntity entity, PoseStack pose, VertexConsumer vc, Vec3 cam, float age,
                                  int core, int glow, float brightness) {
        float open = Mth.clamp(age / CuttingThreadCageEntity.OPEN_TICKS, 0.0f, 1.0f);
        float fall = Mth.clamp((age - CuttingThreadCageEntity.OPEN_TICKS) / CuttingThreadCageEntity.FALL_TICKS, 0.0f, 1.0f);
        float contract = entity.contractProgress(age);
        float fallEased = fall * fall; // gravity

        // Edge radius: unfurls out of a point, stays wide while falling, then constricts with the trap.
        float edgeRadius = fall < 1.0f ? entity.baseRadius() * (1.0f - (1.0f - open) * (1.0f - open)) : entity.currentRadius(age);
        // Heights: the net's point stays where it opened, high above the target; only the rim falls to the ground
        // (so the web becomes a tent/cone), then the rim constricts inward around the target.
        float top = CuttingThreadCageEntity.OPEN_HEIGHT;
        float centreHeight = top;
        float edgeHeight = Mth.lerp(fallEased, top - 0.3f, 0.03f);
        float spin = age * (fall < 1.0f ? 0.04f : 0.01f);

        Vec3[][] grid = new Vec3[SPOKES][RINGS + 1];
        for (int s = 0; s < SPOKES; s++) {
            double theta = Math.PI * 2.0 * s / SPOKES + spin;
            for (int r = 0; r <= RINGS; r++) {
                float f = (float) r / RINGS;
                // Draped profile: steep near the peak, flaring onto the ground (tent / spire).
                float h = Mth.lerp((float) Math.pow(f, 0.6), centreHeight, edgeHeight);
                // While flat in the air, wobble like a net billowing.
                if (fall < 1.0f) h += Mth.sin((float) theta * 3.0f + age * 0.8f) * 0.12f * f * (1.0f - fall);
                double rad = edgeRadius * f;
                grid[s][r] = new Vec3(Math.cos(theta) * rad, h, Math.sin(theta) * rad);
            }
        }

        // Spokes (centre → edge).
        Vec3[] line = new Vec3[RINGS + 1];
        for (int s = 0; s < SPOKES; s++) {
            System.arraycopy(grid[s], 0, line, 0, RINGS + 1);
            ThreadRenderHelper.polyline(pose, vc, cam, line, RINGS + 1, core, glow, brightness);
        }
        // Rings between spokes, each span sagging inward slightly like real web silk.
        for (int r = 1; r <= RINGS; r++) {
            for (int s = 0; s < SPOKES; s++) {
                Vec3 a = grid[s][r];
                Vec3 b = grid[(s + 1) % SPOKES][r];
                Vec3 mid = a.add(b).scale(0.5);
                Vec3 sag = new Vec3(mid.x * 0.9, mid.y - 0.04 * (1.0 - contract), mid.z * 0.9);
                ThreadRenderHelper.thread(pose, vc, cam, a, sag, core, glow, brightness * 0.85f);
                ThreadRenderHelper.thread(pose, vc, cam, sag, b, core, glow, brightness * 0.85f);
            }
        }
        // Once landed, every spoke end is pinned into the ground.
        if (fall >= 1.0f) {
            for (int s = 0; s < SPOKES; s++) {
                Vec3 end = grid[s][RINGS];
                ThreadRenderHelper.thread(pose, vc, cam, end, new Vec3(end.x * 1.12, 0.0, end.z * 1.12), core, glow, brightness * 0.7f);
            }
        }
    }

    // ------------------------------------------------------------------ Murderous Eye Basket: ground circle → sphere

    private static void renderBasket(ThreadBasketEntity entity, PoseStack pose, VertexConsumer vc, Vec3 cam, float age,
                                     int core, int glow, float brightness) {
        float ring = Mth.clamp(age / ThreadBasketEntity.RING_TICKS, 0.0f, 1.0f);
        float rise = Mth.clamp((age - ThreadBasketEntity.RING_TICKS) / ThreadBasketEntity.RISE_TICKS, 0.0f, 1.0f);
        float radius = entity.currentRadius(age);
        float centre = entity.getCentreHeight();
        float spin = age * 0.05f;

        // 1. Cat's-cradle circle on the ground (fades as the sphere takes over).
        float groundRadius = (float) Math.sqrt(Math.max(0.05, entity.baseRadius() * entity.baseRadius() - centre * centre));
        float ringFade = 1.0f - rise;
        if (ringFade > 0.0f) {
            float sweep = ring * Mth.TWO_PI; // the circle draws itself around
            circleArc(pose, vc, cam, groundRadius, 0.04f, spin, sweep, core, glow, brightness * ringFade);
            circleArc(pose, vc, cam, groundRadius * 0.62f, 0.04f, -spin, sweep, core, glow, brightness * ringFade * 0.8f);
            // {8/3} star polygon inside: the cat's-cradle pattern.
            int points = 8;
            for (int i = 0; i < points; i++) {
                double a0 = spin + Math.PI * 2.0 * i / points;
                double a1 = spin + Math.PI * 2.0 * ((i + 3) % points) / points;
                if (a0 - spin > sweep) continue;
                Vec3 p0 = new Vec3(Math.cos(a0) * groundRadius, 0.05, Math.sin(a0) * groundRadius);
                Vec3 p1 = new Vec3(Math.cos(a1) * groundRadius, 0.05, Math.sin(a1) * groundRadius);
                ThreadRenderHelper.thread(pose, vc, cam, p0, p1, core, glow, brightness * ringFade);
            }
        }
        if (rise <= 0.0f) return;

        // 2. Sphere grows upward out of the circle: only geometry below yLimit is drawn while rising.
        double yLimit = Mth.lerp(rise, 0.0, centre + radius + 0.01);
        int meridians = 14;
        int latitudes = 7;
        int segments = 16;
        for (int m = 0; m < meridians; m++) {
            double theta = Math.PI * 2.0 * m / meridians + spin;
            Vec3 prev = null;
            for (int s = 0; s <= segments; s++) {
                double phi = Math.PI * s / segments; // 0 = top pole
                Vec3 p = spherePoint(radius, centre, theta, phi);
                if (prev != null && p.y <= yLimit && prev.y <= yLimit) {
                    ThreadRenderHelper.thread(pose, vc, cam, prev, p, core, glow, brightness);
                }
                prev = p;
            }
        }
        for (int l = 1; l <= latitudes; l++) {
            double phi = Math.PI * l / (latitudes + 1);
            double y = centre + Math.cos(phi) * radius;
            if (y > yLimit) continue;
            Vec3 prev = null;
            for (int s = 0; s <= 32; s++) {
                Vec3 p = spherePoint(radius, centre, Math.PI * 2.0 * s / 32 - spin, phi);
                if (prev != null) ThreadRenderHelper.thread(pose, vc, cam, prev, p, core, glow, brightness * 0.8f);
                prev = p;
            }
        }
        // Once closed, diagonal cross-weave tightens the cage (denser as it shrinks).
        if (rise >= 1.0f) {
            float weave = Mth.clamp((age - entity.closeTick()) / 6.0f, 0.0f, 1.0f);
            for (int family = -1; family <= 1; family += 2) {
                for (int m = 0; m < 10; m++) {
                    double theta0 = Math.PI * 2.0 * m / 10 - spin * family;
                    Vec3 prev = null;
                    for (int s = 0; s <= segments; s++) {
                        double t = (double) s / segments;
                        Vec3 p = spherePoint(radius, centre, theta0 + family * t * Math.PI * 0.9, Math.PI * t);
                        if (prev != null) ThreadRenderHelper.thread(pose, vc, cam, prev, p, core, glow, brightness * 0.6f * weave);
                        prev = p;
                    }
                }
            }
        }
    }

    private static Vec3 spherePoint(double r, double centre, double theta, double phi) {
        double ring = Math.sin(phi) * r;
        return new Vec3(Math.cos(theta) * ring, centre + Math.cos(phi) * r, Math.sin(theta) * ring);
    }

    private static void circleArc(PoseStack pose, VertexConsumer vc, Vec3 cam, float r, float y, float start, float sweep,
                                  int core, int glow, float brightness) {
        int segments = 40;
        Vec3 prev = null;
        for (int s = 0; s <= segments; s++) {
            double a = start + sweep * s / segments;
            Vec3 p = new Vec3(Math.cos(a) * r, y, Math.sin(a) * r);
            if (prev != null) ThreadRenderHelper.thread(pose, vc, cam, prev, p, core, glow, brightness);
            prev = p;
        }
    }

    // ------------------------------------------------------------------ caster's cat's cradle

    /**
     * Red threads strung between the caster's hands like a cat's cradle (their arms are held out by the
     * RUI_CRADLE animation). During formation, three control threads also run from the cradle to the cage.
     */
    private void renderCradle(ThreadBasketEntity basket, PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 origin,
                              float age, float partialTick, float fade) {
        Entity ownerEntity = basket.level.getEntity(basket.getOwnerEntityId());
        if (!(ownerEntity instanceof LivingEntity owner)) return;

        Vec3 left;
        Vec3 right;
        Vec3 side;
        Vec3 up;
        Minecraft mc = Minecraft.getInstance();
        Camera camera = this.entityRenderDispatcher.camera;
        if (owner == mc.getCameraEntity() && !camera.isDetached()) {
            // First person: string it across the bottom of the view, where the raised hands are.
            Vec3 eye = camera.getPosition();
            Vec3 look = new Vec3(camera.getLookVector());
            Vec3 camUp = new Vec3(camera.getUpVector());
            Vec3 camLeft = new Vec3(camera.getLeftVector());
            Vec3 centre = eye.add(look.scale(0.6)).subtract(camUp.scale(0.24));
            left = centre.add(camLeft.scale(0.3));
            right = centre.subtract(camLeft.scale(0.3));
            side = camLeft.scale(-1);
            up = camUp;
        } else {
            float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Mth.sin(bodyYaw), 0, Mth.cos(bodyYaw));
            side = new Vec3(-forward.z, 0, forward.x); // owner's right
            up = new Vec3(0, 1, 0);
            float scale = owner.getBbHeight() / 1.8f;
            Vec3 chest = owner.getPosition(partialTick).add(0, 1.42 * scale, 0).add(forward.scale(0.62 * scale));
            left = chest.subtract(side.scale(0.48 * scale));
            right = chest.add(side.scale(0.48 * scale));
        }
        // Into the renderer's local space.
        left = left.subtract(origin);
        right = right.subtract(origin);

        float appear = Mth.clamp(age / 4.0f, 0.0f, 1.0f) * fade;
        int fingers = 5;
        Vec3[] lp = new Vec3[fingers];
        Vec3[] rp = new Vec3[fingers];
        for (int i = 0; i < fingers; i++) {
            double offset = (i - (fingers - 1) / 2.0) * 0.06;
            double wiggle = Math.sin(age * 0.5 + i) * 0.012;
            lp[i] = left.add(up.scale(offset + wiggle));
            rp[i] = right.add(up.scale(offset - wiggle));
        }
        int core = ThreadRenderHelper.BLOOD_CORE;
        int glow = ThreadRenderHelper.BLOOD_GLOW;
        for (int i = 0; i < fingers; i++) {
            ThreadRenderHelper.thread(pose, vc, cam, lp[i], rp[i], core, glow, appear * 0.9f);              // straight
            ThreadRenderHelper.thread(pose, vc, cam, lp[i], rp[fingers - 1 - i], core, glow, appear * 0.7f); // crossings
            if (i + 2 < fingers) ThreadRenderHelper.thread(pose, vc, cam, lp[i], rp[i + 2], core, glow, appear * 0.5f);
        }
        // Control threads from the cradle out to the forming cage.
        if (age < basket.closeTick() + 4) {
            Vec3 mid = left.add(right).scale(0.5);
            Vec3 target = new Vec3(0, basket.getCentreHeight(), 0);
            float control = appear * (1.0f - Mth.clamp((age - basket.closeTick()) / 4.0f, 0.0f, 1.0f));
            for (int i = -1; i <= 1; i++) {
                ThreadRenderHelper.thread(pose, vc, cam, mid.add(up.scale(i * 0.05)), target.add(side.scale(i * 0.4)), core, glow, control * 0.6f);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
