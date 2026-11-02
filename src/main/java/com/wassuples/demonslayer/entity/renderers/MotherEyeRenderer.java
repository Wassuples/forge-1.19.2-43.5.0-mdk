package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.MotherEyeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Eyes of Forced Unconscious Sleep.
 * <ul>
 *   <li><b>Mother Eye</b>: a veined mound of flesh that Enmu stands on, with fleshy roots spreading over the
 *   ground and a huge eye bulging from its front. The eye opens when summoned, blinks, and turns to stare at the
 *   nearest player.</li>
 *   <li><b>Lesser eyes</b>: almond-shaped eyes that open in the ground in front of every target; meeting one's
 *   gaze is what puts you to sleep. They open together each time the Mother Eye re-places them, glance around,
 *   and lock onto the nearest player.</li>
 * </ul>
 */
public class MotherEyeRenderer extends EntityRenderer<MotherEyeEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int FLESH = 0xFFD58C8E;
    private static final int FLESH_DARK = 0xFF8E3F4C;
    private static final int VEIN = 0xFF6A1E33;
    private static final int SCLERA = 0xFFF4EAE2;
    private static final int IRIS = 0xFFE58AC8;
    private static final int IRIS_DARK = 0xFF8A2C70;
    private static final int PUPIL = 0xFF12050D;

    public MotherEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.8f;
    }

    /** Keep drawing while any lesser eye is on screen, even if the mound itself isn't. */
    @Override
    public boolean shouldRender(MotherEyeEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return super.shouldRender(entity, frustum, camX, camY, camZ) || entity.distanceToSqr(camX, camY, camZ) < 48 * 48;
    }

    @Override
    public void render(MotherEyeEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 origin = entity.getPosition(partialTick);
        Player watched = nearestOtherPlayer(entity, origin, 25.0);

        renderMound(entity, pose, buffers, light, age);
        renderBigEye(entity, pose, buffers, age, origin, watched);
        renderLesserEyes(entity, pose, buffers, age, origin);
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    // ------------------------------------------------------------------ mound

    private static void renderMound(MotherEyeEntity entity, PoseStack pose, MultiBufferSource buffers, int light, float age) {
        float rise = Mth.clamp(age / 16.0f, 0.0f, 1.0f);
        float breathe = 1.0f + 0.03f * Mth.sin(age * 0.12f);
        pose.pushPose();
        pose.scale(1.7f * breathe, 0.95f * rise / breathe, 1.7f * breathe);
        ToonRenderHelper.solidSphere(pose, buffers, light, 1.0f, 16, 24, MotherEyeRenderer::fleshColor, false);
        pose.popPose();

        // Fleshy roots gripping the ground.
        for (int i = 0; i < 9; i++) {
            double a = i * (Math.PI * 2.0 / 9.0) + 0.3 * Math.sin(i * 2.1);
            double len = (1.6 + 0.6 * Math.sin(i * 3.7)) * rise;
            float px = 0;
            float pz = 0;
            for (int s = 1; s <= 6; s++) {
                double t = s / 6.0;
                double r = 1.5 + len * t;
                double wav = 0.15 * Math.sin(t * 6.0 + i + age * 0.05);
                float x = (float) (Math.cos(a + wav * 0.3) * r);
                float z = (float) (Math.sin(a + wav * 0.3) * r);
                if (s > 1) {
                    float w = (float) (0.13 * (1.0 - t) + 0.03);
                    float nx = -(z - pz);
                    float nz = x - px;
                    float nl = Mth.sqrt(nx * nx + nz * nz);
                    if (nl > 1.0E-4) {
                        nx = nx / nl * w;
                        nz = nz / nl * w;
                        ToonRenderHelper.quad3(pose, buffers, s % 2 == 0 ? FLESH_DARK : VEIN,
                                px - nx, 0.02f, pz - nz, px + nx, 0.02f, pz + nz, x + nx, 0.02f, z + nz, x - nx, 0.02f, z - nz, 0, 1, 0);
                    }
                }
                px = x;
                pz = z;
            }
        }
    }

    private static int fleshColor(float x, float y, float z) {
        double a = Math.atan2(z, x);
        double vein = Math.abs(Math.sin(a * 7.0 + y * 6.0) * Math.cos(a * 3.0 - y * 4.0));
        if (vein < 0.08) return VEIN;
        if (vein < 0.2) return FLESH_DARK;
        return y > 0.75f ? 0xFFE3A3A4 : FLESH;
    }

    // ------------------------------------------------------------------ big eye

    private void renderBigEye(MotherEyeEntity entity, PoseStack pose, MultiBufferSource buffers, float age, Vec3 origin, Player watched) {
        float open = eyeOpenness(age, 6.0f, entity.getId());
        float bodyYaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 eyeLocal = new Vec3(-Mth.sin(bodyYaw) * 1.05, 0.65, Mth.cos(bodyYaw) * 1.05);
        Vec3 lookDir = watched != null
                ? watched.getEyePosition().subtract(origin.add(eyeLocal)).normalize()
                : new Vec3(-Mth.sin(bodyYaw + Mth.sin(age * 0.03f) * 0.6f), -0.1, Mth.cos(bodyYaw + Mth.sin(age * 0.03f) * 0.6f)).normalize();

        pose.pushPose();
        pose.translate(eyeLocal.x, eyeLocal.y, eyeLocal.z);
        float lookYaw = (float) (Mth.atan2(-lookDir.x, lookDir.z) * Mth.RAD_TO_DEG);
        float lookPitch = (float) (Math.asin(Mth.clamp(-lookDir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);
        pose.mulPose(Vector3f.YP.rotationDegrees(-lookYaw));
        pose.mulPose(Vector3f.XP.rotationDegrees(lookPitch));
        float size = 0.85f * Mth.clamp(age / 12.0f, 0.0f, 1.0f);
        ToonRenderHelper.solidSphere(pose, buffers, 0xF000F0, size * 1.05f, 12, 18, (x, y, z) -> FLESH_DARK, true);
        ToonRenderHelper.solidSphere(pose, buffers, 0xF000F0, size, 22, 32, (x, y, z) -> bigEyeColor(x, y, z, open), false);
        pose.popPose();
    }

    /** Eye colours with eyelids: vertices outside the open slit are skin. Pupil faces local +Z. */
    private static int bigEyeColor(float x, float y, float z, float open) {
        // Lids close from top and bottom toward the horizontal centre line.
        if (Math.abs(y) > open * 0.92f && z > -0.2f) {
            return Math.abs(Math.abs(y) - open * 0.92f) < 0.05f ? FLESH_DARK : FLESH;
        }
        if (z > 0.96f) return PUPIL;
        if (z > 0.86f) {
            // Enmu-style iris: concentric pink rings.
            double ring = Math.sin(z * 90.0);
            return ring > 0.4 ? IRIS_DARK : IRIS;
        }
        if (z > 0.82f) return IRIS_DARK;
        double a = Math.atan2(y, x);
        if (z < 0.6f && z > -0.3f && Math.abs(Math.sin(a * 8.0 + z * 10.0)) < 0.12) return 0xFFC74A62;
        return SCLERA;
    }

    // ------------------------------------------------------------------ lesser eyes

    private static void renderLesserEyes(MotherEyeEntity entity, PoseStack pose, MultiBufferSource buffers, float age, Vec3 origin) {
        List<BlockPos> eyes = entity.getLesserEyePositions();
        if (eyes.isEmpty()) return;
        float sinceOpened = age - entity.getEyesOpenedTick();
        Minecraft mc = Minecraft.getInstance();

        for (BlockPos pos : eyes) {
            Vec3 world = new Vec3(pos.getX() + 0.5, pos.getY() + 0.02, pos.getZ() + 0.5);
            // Each eye opens a little after the last, then blinks on its own rhythm.
            int seed = (int) pos.asLong();
            float open = Mth.clamp((sinceOpened - (seed & 7)) / 6.0f, 0.0f, 1.0f) * eyeOpenness(age, 0.0f, seed);
            if (open <= 0.02f) continue;

            Player watched = nearestOtherPlayer(entity, world, 16.0);
            // Rotate the almond so it lies across the watcher's line of sight; iris slides toward them.
            Vec3 toWatcher = watched != null ? watched.getEyePosition().subtract(world) : new Vec3(Mth.sin(age * 0.05f + seed), 0, Mth.cos(age * 0.05f + seed));
            float faceYaw = (float) Math.atan2(toWatcher.x, toWatcher.z);
            float irisShift = watched != null ? 0.12f : 0.06f * Mth.sin(age * 0.2f + seed);

            Vec3 rel = world.subtract(origin);
            pose.pushPose();
            pose.translate(rel.x, rel.y, rel.z);
            pose.mulPose(Vector3f.YP.rotation(faceYaw));
            pose.mulPose(Vector3f.XP.rotationDegrees(90.0f)); // lay flat: local XY = ground plane, -Z = up
            float scale = 0.85f;
            pose.scale(scale, scale, scale);

            almond(pose, buffers, FLESH_DARK, 0.62f, 0.34f * Math.max(open, 0.25f), 0.0f);   // fleshy socket
            almond(pose, buffers, SCLERA, 0.5f, 0.24f * open, -0.003f);
            // iris + pupil, clipped to the opening by shrinking with it
            float irisR = 0.17f * Math.min(1.0f, open * 1.4f);
            ToonRenderHelper.disc(pose, buffers, IRIS_DARK, 0, irisShift, irisR, 20, -0.006f);
            ToonRenderHelper.disc(pose, buffers, IRIS, 0, irisShift, irisR * 0.8f, 20, -0.008f);
            ToonRenderHelper.disc(pose, buffers, PUPIL, 0, irisShift, irisR * 0.42f, 16, -0.010f);
            ToonRenderHelper.disc(pose, buffers, 0xFFFFFFFF, -irisR * 0.3f, irisShift - irisR * 0.3f, irisR * 0.15f, 10, -0.012f);
            // lash line
            almondOutline(pose, buffers, 0xFF2A0A16, 0.5f, 0.24f * open, -0.013f);
            pose.popPose();
        }
    }

    /** Filled almond (pointed ellipse) in the XY plane. */
    private static void almond(PoseStack pose, MultiBufferSource buffers, int color, float halfWidth, float halfHeight, float z) {
        int n = 24;
        float[] xs = new float[n * 2];
        float[] ys = new float[n * 2];
        for (int i = 0; i < n; i++) {
            float t = -1.0f + 2.0f * i / (n - 1);
            float h = halfHeight * (float) Math.pow(Math.max(0.0, 1.0 - t * t), 0.75);
            xs[i] = t * halfWidth;
            ys[i] = h;
            xs[n * 2 - 1 - i] = t * halfWidth;
            ys[n * 2 - 1 - i] = -h;
        }
        ToonRenderHelper.polygon(pose, buffers, color, xs, ys, z);
    }

    private static void almondOutline(PoseStack pose, MultiBufferSource buffers, int color, float halfWidth, float halfHeight, float z) {
        int n = 16;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < n; i++) {
                float t0 = -1.0f + 2.0f * i / n;
                float t1 = -1.0f + 2.0f * (i + 1) / n;
                float h0 = side * halfHeight * (float) Math.pow(Math.max(0.0, 1.0 - t0 * t0), 0.75);
                float h1 = side * halfHeight * (float) Math.pow(Math.max(0.0, 1.0 - t1 * t1), 0.75);
                float w = 0.018f;
                ToonRenderHelper.quad3(pose, buffers, color,
                        t0 * halfWidth, h0 - w, z, t1 * halfWidth, h1 - w, z, t1 * halfWidth, h1 + w, z, t0 * halfWidth, h0 + w, z,
                        0, 0, -1);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** 0 closed .. 1 open: opens over {@code openDelay}+8 ticks after summon, then blinks every ~4 s. */
    private static float eyeOpenness(float age, float openDelay, int seed) {
        float opening = Mth.clamp((age - openDelay) / 8.0f, 0.0f, 1.0f);
        float period = 80.0f + (seed & 31);
        float phase = (age + (seed & 63)) % period;
        float blink = phase < 4.0f ? Math.abs(phase - 2.0f) / 2.0f : 1.0f;
        return opening * blink;
    }

    private static Player nearestOtherPlayer(MotherEyeEntity entity, Vec3 from, double range) {
        Player best = null;
        double bestD = range * range;
        for (Player p : entity.level.players()) {
            if (p.getId() == entity.getCasterEntityId() || p.isSpectator()) continue;
            double d = p.distanceToSqr(from);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    public ResourceLocation getTextureLocation(MotherEyeEntity entity) {
        return TEXTURE;
    }
}
