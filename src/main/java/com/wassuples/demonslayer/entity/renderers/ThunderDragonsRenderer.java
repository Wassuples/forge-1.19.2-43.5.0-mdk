package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ThunderDragonsEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Crazed Cry of Thunderdeath: two colossal serpentine wooden dragons (heads ~11 blocks long, bodies ~7 blocks
 * thick and ~60 long) chase each other around the arena overhead. The thunder dragon (Sekido's electricity) has
 * sparks crawling over its jaws and spits every bolt the form calls down as a forked lightning arc. The sound
 * dragon (Urogi's sound waves) hums with pulsing rings during the wind-up, then rears and cries out: rings burst
 * from its jaws and walls of sound roll out across the arena.
 */
public class ThunderDragonsRenderer extends EntityRenderer<ThunderDragonsEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int BODY_POINTS = 44;
    private static final float TRAIL_STEP = 1.15f; // ticks of path between body points
    private static final float HEAD_SCALE = 6.5f;
    /** Which dragon carries Sekido's lightning; the other carries Urogi's sound waves. */
    private static final int THUNDER = 0;
    private static final int SOUND = 1;

    public ThunderDragonsRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ThunderDragonsEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        float emerge = Mth.clamp(age / 15.0f, 0.0f, 1.0f);
        float charge = Mth.clamp(age / ThunderDragonsEntity.WIND_UP, 0.0f, 1.0f);
        float roar = Mth.clamp((age - (ThunderDragonsEntity.WIND_UP - 4)) / 4.0f, 0.0f, 1.0f)
                * Mth.clamp((ThunderDragonsEntity.WIND_UP + 18 - age) / 6.0f, 0.0f, 1.0f);
        float sinceBolt = age - entity.getBoltTick();
        boolean bolting = sinceBolt >= 0 && sinceBolt < 5 && age >= ThunderDragonsEntity.LIGHTNING_START;
        Vec3 bolt = entity.getBoltOffset();
        int boltDragon = bolting ? THUNDER : -1;

        Vec3[] mouths = new Vec3[2];
        for (int d = 0; d < 2; d++) {
            // Body follows the head's own path back in time: a serpent coiling round the arena.
            Vec3[] pts = new Vec3[BODY_POINTS];
            float[] radii = new float[BODY_POINTS];
            for (int i = 0; i < BODY_POINTS; i++) {
                float back = (BODY_POINTS - 1 - i) * TRAIL_STEP;
                Vec3 p = entity.headOffset(d, age - back);
                float t = (float) i / (BODY_POINTS - 1); // 0 tail .. 1 neck
                p = p.add(0, Math.sin(age * 0.2 - i * 0.4 + d) * 2.6 * (1.0 - t * 0.5), 0);
                pts[i] = p;
                radii[i] = Mth.lerp(t, 0.5f, 3.3f) * (0.35f + 0.65f * emerge);
            }
            HantenguRenderHelper.woodTube(pose, buffers, light, pts, radii, 14, d * 3);

            Vec3 head = pts[BODY_POINTS - 1];
            Vec3 facing = head.subtract(pts[BODY_POINTS - 3]);
            float myRoar = d == SOUND ? roar : roar * 0.3f;
            if (myRoar > 0.0f) {
                // Rear up and look down into the arena while crying out.
                facing = facing.normalize().lerp(head.scale(-1).normalize().add(0, 0.6, 0).normalize(), myRoar * 0.7);
            }
            if (d == boltDragon) {
                facing = facing.normalize().lerp(bolt.subtract(head).normalize(), 0.75);
            }
            facing = facing.normalize();
            float mouth = Math.max(myRoar, d == boltDragon ? 0.8f : 0.15f + 0.1f * Mth.sin(age * 0.3f + d));
            float glow = d == THUNDER ? Math.max(charge * 0.8f, d == boltDragon ? 1.0f : 0.0f) : charge * 0.4f;
            pose.pushPose();
            pose.translate(head.x, head.y, head.z);
            HantenguRenderHelper.faceAlong(pose, facing);
            float s = HEAD_SCALE * (0.4f + 0.6f * emerge);
            pose.scale(s, s, s);
            HantenguRenderHelper.dragonHead(pose, buffers, age < ThunderDragonsEntity.WIND_UP ? light : 0xF000F0, mouth, glow);
            pose.popPose();
            mouths[d] = head.add(facing.scale(1.6 * HEAD_SCALE));
        }

        Vec3 cam = this.entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partialTick));

        // Wind-up: sparks crawling over the jaws, more as the charge builds.
        if (age > 15) {
            // Sekido's electricity crawls over the thunder dragon's jaws the whole time, more as the charge builds.
            long seed = (long) (age * 0.5f) * 31L;
            java.util.Random rnd = new java.util.Random(seed);
            int sparks = 2 + (int) (charge * 4);
            for (int k = 0; k < sparks; k++) {
                Vec3 tip = mouths[THUNDER].add((rnd.nextDouble() - 0.5) * 9, (rnd.nextDouble() - 0.5) * 9, (rnd.nextDouble() - 0.5) * 9);
                HantenguRenderHelper.lightningArc(pose, buffers, cam, mouths[THUNDER], tip, seed + k, 0.5f + 0.5f * charge, 0.12f);
            }
        }
        if (age > 15 && age < ThunderDragonsEntity.WIND_UP) {
            // Urogi's sound builds in the other dragon's throat as faint pulsing rings.
            for (int ring = 0; ring < 2; ring++) {
                float k = ((age * 0.08f) + ring * 0.5f) % 1.0f;
                HantenguRenderHelper.soundRing(pose, buffers, cam, mouths[SOUND], mouths[SOUND].scale(-1),
                        1.0f + k * 4.0f, 0.4f, 0x50FFE6B0, (1.0f - k) * charge);
            }
        }

        // The cry: walls of sound rolling out across the arena, plus rings bursting from each mouth.
        float cry = (age - ThunderDragonsEntity.WIND_UP) / 22.0f;
        if (cry > 0.0f && cry < 1.0f) {
            float r = entity.getRadius() * 1.25f;
            for (int ring = 0; ring < 3; ring++) {
                float k = Mth.clamp(cry - ring * 0.12f, 0.0f, 1.0f);
                if (k <= 0.0f) continue;
                for (int layer = 0; layer < 3; layer++) {
                    HantenguRenderHelper.soundRing(pose, buffers, cam, new Vec3(0, 0.6 + layer * 1.6, 0), new Vec3(0, 1, 0),
                            0.5f + r * k, 0.9f, 0x60E8D0FF, (1.0f - k) * (1.0f - layer * 0.25f));
                }
            }
            Vec3 dir = mouths[SOUND].scale(-1).add(0, -mouths[SOUND].y * 0.5, 0);
            for (int ring = 0; ring < 4; ring++) {
                float k = (cry * 3.0f + ring * 0.25f) % 1.0f;
                HantenguRenderHelper.soundRing(pose, buffers, cam, mouths[SOUND].add(dir.normalize().scale(k * 16.0)), dir,
                        2.0f + k * 9.0f, 0.8f, 0x80FFE6B0, 1.0f - k);
            }
        }

        // Each bolt leaps from the nearer dragon's jaws.
        if (bolting) {
            float b = 1.0f - sinceBolt / 5.0f;
            long seed = entity.getBoltTick() * 977L + (long) (sinceBolt * 2.0f);
            HantenguRenderHelper.lightningArc(pose, buffers, cam, mouths[boltDragon], bolt, seed, b, 0.45f);
            HantenguRenderHelper.lightningArc(pose, buffers, cam, mouths[boltDragon], bolt.add(0.8, 0, -0.6), seed + 5, b * 0.6f, 0.12f);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public boolean shouldRender(ThunderDragonsEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        double r = entity.getRadius() + 20.0;
        return frustum.isVisible(entity.getBoundingBox().inflate(r, ThunderDragonsEntity.HEIGHT + 20.0, r));
    }

    @Override
    public ResourceLocation getTextureLocation(ThunderDragonsEntity entity) {
        return TEXTURE;
    }
}
