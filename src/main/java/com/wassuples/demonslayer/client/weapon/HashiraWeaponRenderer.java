package com.wassuples.demonslayer.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.item.custom.HashiraSwordHelper;
import com.wassuples.demonslayer.item.custom.SwordModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the eleven Hashira weapons procedurally, so they can have real curvature (sori), tapered kissaki, true tsuba
 * silhouettes, wrapped grips, and moving parts.
 *
 * <p>Moving parts follow the holder: Mitsuri's whip-blade ripples and cracks, Gyomei's chain and spiked ball hang and
 * fly out when he swings, Tengen's second cleaver dangles on its chain and whirls around like a nunchaku, and Jigoro's
 * kashira ribbons flutter. In the inventory (no holder) the flexible weapons lie in a tidy display pose.
 *
 * <p>Coordinates are model pixels with the grip on the same axis as the base nichirin blade (x 7.8, z 8.3, grip from
 * y 0 up), so the base blade's hand transforms still fit.
 */
public class HashiraWeaponRenderer extends BlockEntityWithoutLevelRenderer {
    private static HashiraWeaponRenderer instance;

    static final float CX = 7.8f, CZ = 8.3f;
    private static final Map<SwordModel, ResourceLocation> TEXTURES = new EnumMap<>(SwordModel.class);

    static {
        put(SwordModel.WATER, "giyu");
        put(SwordModel.INSECT, "shinobu");
        put(SwordModel.LOVE, "mitsuri");
        put(SwordModel.SNAKE, "obanai");
        put(SwordModel.WIND, "sanemi");
        put(SwordModel.STONE, "gyomei");
        put(SwordModel.MIST, "muichiro");
        put(SwordModel.FLAME, "rengoku");
        put(SwordModel.SOUND, "tengen");
        put(SwordModel.THUNDER, "jigoro");
        put(SwordModel.FLOWER, "kanae");
    }

    private static void put(SwordModel m, String name) {
        TEXTURES.put(m, new ResourceLocation(DemonSlayer.MOD_ID, "textures/item/hashira/" + name + ".png"));
    }

    public HashiraWeaponRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static HashiraWeaponRenderer get() {
        if (instance == null) instance = new HashiraWeaponRenderer();
        return instance;
    }

    /** Pose inputs for one draw. */
    record Anim(float time, float swing, boolean display) {
    }

    @Override
    public void renderByItem(ItemStack stack, ItemTransforms.TransformType type, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        SwordModel model = HashiraSwordHelper.getModelFromStack(stack);
        if (model == null || model == SwordModel.DEFAULT) return;
        ResourceLocation tex = TEXTURES.get(model);
        if (tex == null) return;

        Minecraft mc = Minecraft.getInstance();
        float partial = mc.getFrameTime();
        LivingEntity holder = switch (type) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> mc.player;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD -> WeaponHolderTracker.current();
            default -> null;
        };
        boolean display = type == ItemTransforms.TransformType.GUI || type == ItemTransforms.TransformType.FIXED
                || type == ItemTransforms.TransformType.GROUND || holder == null;
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + partial;
        if (holder != null) time += holder.getId() * 7.3f;
        float swing = holder != null ? holder.getAttackAnim(partial) : 0f;
        Anim anim = new Anim(time, swing, display);

        pose.pushPose();
        pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
        WeaponMesh m = new WeaponMesh(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay);
        switch (model) {
            case WATER, WIND, MIST, FLAME, FLOWER -> katana(m, KatanaSpec.standard(model));
            case THUNDER -> {
                katana(m, KatanaSpec.standard(model));
                ribbons(m, anim);
            }
            case INSECT -> shinobu(m);
            case LOVE -> mitsuri(m, anim);
            case SNAKE -> obanai(m);
            case STONE -> gyomei(m, anim);
            case SOUND -> tengen(m, anim);
            default -> { }
        }
        pose.popPose();
    }

    // ================================================================== katana parts

    record KatanaSpec(float handle, float blade, float width, float sori, float thick, float tsubaHalf, float tsubaThick) {
        static KatanaSpec standard(SwordModel m) {
            return switch (m) {
                case FLAME -> new KatanaSpec(6.4f, 18.6f, 1.3f, 0.9f, 0.34f, 1.85f, 0.45f);
                case WIND -> new KatanaSpec(6.4f, 18.5f, 1.28f, 0.95f, 0.32f, 1.95f, 0.42f);
                case MIST -> new KatanaSpec(6.2f, 18.0f, 1.22f, 0.85f, 0.30f, 1.6f, 0.42f);
                default -> new KatanaSpec(6.4f, 18.5f, 1.28f, 0.9f, 0.32f, 1.7f, 0.42f);
            };
        }
    }

    /** Kashira, wrapped grip, seppa, tsuba, seppa and habaki. Returns the y where the blade starts. */
    static float hilt(WeaponMesh m, float handle, float tsubaHalf, float tsubaThick) {
        m.box(CX - 0.62f, -0.55f, CZ - 0.5f, CX + 0.62f, 0f, CZ + 0.5f, WeaponMesh.SW_KASHIRA);
        m.wrappedBox(CX - 0.55f, 0f, CZ - 0.45f, CX + 0.55f, handle, CZ + 0.45f, 32, 0, 16, 128, WeaponMesh.SW_KASHIRA);
        float y = handle;
        m.box(CX - 0.72f, y, CZ - 0.58f, CX + 0.72f, y + 0.1f, CZ + 0.58f, WeaponMesh.SW_SEPPA);
        y += 0.1f;
        tsuba(m, y, tsubaHalf, tsubaThick);
        y += tsubaThick;
        m.box(CX - 0.72f, y, CZ - 0.58f, CX + 0.72f, y + 0.1f, CZ + 0.58f, WeaponMesh.SW_SEPPA);
        y += 0.1f;
        m.box(CX - 0.7f, y, CZ - 0.34f, CX + 0.7f, y + 0.85f, CZ + 0.34f, WeaponMesh.SW_HABAKI);
        return y;
    }

    /** Tsuba decal: the atlas art is a circle of radius 30 px centred in a 64 px square, so pad the quad to match. */
    static void tsuba(WeaponMesh m, float y, float half, float thick) {
        float h = half * 32f / 30f;
        m.decalH(CX - h, CZ - h, CX + h, CZ + h, y, thick, 5, 64, 0, 128, 64);
    }

    /** Standard curved katana blade from y0, spine on -x, edge on +x, curving back toward the spine. */
    static void katanaBlade(WeaponMesh m, float y0, float len, float width, float sori, float thick) {
        int n = 26;
        float kl = width * 1.9f;                       // kissaki length
        float[][] sp = new float[n][], ed = new float[n][], th = new float[n][];
        float[] w = new float[n], t = new float[n], v = new float[n];
        for (int i = 0; i < n; i++) {
            float s = len * i / (n - 1f);
            float q = s / len;
            float x = CX - width / 2f - sori * q * q;
            float slope = -2f * sori * q / len;        // dx/ds
            float[] tan = WeaponMesh.norm(new float[]{slope, 1, 0});
            sp[i] = new float[]{x, y0 + s, CZ};
            ed[i] = new float[]{tan[1], -tan[0], 0};
            th[i] = new float[]{0, 0, 1};
            float kissaki = s > len - kl ? (s - (len - kl)) / kl : 0f;
            w[i] = width * (1f - 0.12f * q) * (float) Math.sqrt(Math.max(0, 1 - kissaki * kissaki));
            t[i] = thick * (1f - 0.45f * q);
            v[i] = 256f * (1f - q);
        }
        m.blade(sp, ed, th, w, t, v, WeaponMesh.SW_SPINE);
    }

    static void katana(WeaponMesh m, KatanaSpec s) {
        float y = hilt(m, s.handle(), s.tsubaHalf(), s.tsubaThick());
        katanaBlade(m, y, s.blade(), s.width(), s.sori(), s.thick());
    }

    // ================================================================== Jigoro: two short white ribbons from the kashira

    static void ribbons(WeaponMesh m, Anim a) {
        for (int k = 0; k < 2; k++) {
            float x = CX + (k == 0 ? -0.3f : 0.3f), y = -0.55f, z = CZ + (k == 0 ? 0.2f : -0.2f);
            float ang = (float) Math.PI;               // hanging straight down the grip axis
            float seg = 0.8f;
            for (int i = 0; i < 6; i++) {
                float sway = 0.22f * Mth.sin(a.time() * 0.18f + i * 0.7f + k * 1.9f) + (a.swing() > 0 ? 0.9f * Mth.sin((float) Math.PI * a.swing()) * (i / 5f) : 0f);
                float h = ang + sway + (k == 0 ? -0.18f : 0.18f);
                float nx = x - Mth.sin(h) * seg, ny = y + Mth.cos(h) * seg;
                float hw = 0.26f;
                m.swatchQuad(new float[]{x - hw, y, z}, new float[]{x + hw, y, z}, new float[]{nx + hw, ny, z}, new float[]{nx - hw, ny, z}, WeaponMesh.SW_RIBBON);
                x = nx;
                y = ny;
            }
        }
    }

    // ================================================================== Shinobu: katana up to just above the hilt, then a rod and needle

    static void shinobu(WeaponMesh m) {
        float y0 = hilt(m, 5.6f, 1.75f, 0.42f);
        float len = 15.5f, base = 3.0f, tip = 2.6f;
        int n = 44;
        float[][] sp = new float[n][], ed = new float[n][], th = new float[n][];
        float[] w = new float[n], t = new float[n], v = new float[n];
        for (int i = 0; i < n; i++) {
            float s = len * i / (n - 1f);
            float width, thick;
            if (s < base) {
                width = 1.15f;
                thick = 0.3f;
            } else if (s < base + 0.6f) {
                float k = (s - base) / 0.6f;
                width = Mth.lerp(k, 1.15f, 0.36f);
                thick = Mth.lerp(k, 0.3f, 0.36f);
            } else if (s < len - tip) {
                width = 0.36f;
                thick = 0.36f;
            } else {
                float q = (s - (len - tip)) / tip;
                width = q < 0.3f ? Mth.lerp(q / 0.3f, 0.36f, 0.82f) : 0.82f * (float) Math.sqrt(Math.max(0, 1 - Mth.square((q - 0.3f) / 0.7f)));
                thick = 0.28f;
            }
            // the rod is centred on the grip axis; the base section and the needle keep the spine on -x
            float spineX = s < base + 0.6f || s >= len - tip ? CX - 0.58f : CX - width / 2;
            sp[i] = new float[]{spineX, y0 + s, CZ};
            ed[i] = new float[]{1, 0, 0};
            th[i] = new float[]{0, 0, 1};
            w[i] = width;
            t[i] = thick;
            v[i] = 256f * (1f - s / len);
        }
        m.blade(sp, ed, th, w, t, v, WeaponMesh.SW_SPINE);
    }

    // ================================================================== Obanai: kris-like serpentine blade

    static void obanai(WeaponMesh m) {
        float y0 = hilt(m, 6.4f, 1.8f, 0.45f);
        float len = 18.5f, width = 1.3f;
        int n = 60;
        float[][] sp = new float[n][], ed = new float[n][], th = new float[n][];
        float[] w = new float[n], t = new float[n], v = new float[n];
        float[][] centre = new float[n][];
        for (int i = 0; i < n; i++) {
            float s = len * i / (n - 1f);
            float q = s / len;
            float env = Math.min(1f, s / 1.6f) * (1f - 0.35f * q);
            float wave = 0.55f * env * Mth.sin((float) (Math.PI * 2 * 6.5 * q));
            centre[i] = new float[]{CX + wave, y0 + s, CZ};
        }
        for (int i = 0; i < n; i++) {
            float[] a = centre[Math.max(0, i - 1)], b = centre[Math.min(n - 1, i + 1)];
            float[] tan = WeaponMesh.norm(new float[]{b[0] - a[0], b[1] - a[1], 0});
            float[] e = {tan[1], -tan[0], 0};
            float q = i / (n - 1f);
            float tipq = q > 0.86f ? (q - 0.86f) / 0.14f : 0f;
            float width_i = width * (1f - 0.4f * q) * (1f - tipq);
            sp[i] = WeaponMesh.add(centre[i], e, -width_i / 2);
            ed[i] = e;
            th[i] = new float[]{0, 0, 1};
            w[i] = width_i;
            t[i] = 0.3f * (1f - 0.4f * q);
            v[i] = 256f * (1f - q);
        }
        m.blade(sp, ed, th, w, t, v, WeaponMesh.SW_SPINE);
    }

    // ================================================================== Mitsuri: long, paper-thin whip blade

    static void mitsuri(WeaponMesh m, Anim a) {
        float y0 = hilt(m, 6.4f, 1.85f, 0.42f);
        float len = 30f;
        int n = 64;
        float ds = len / (n - 1f);
        float[][] sp = new float[n][], ed = new float[n][], th = new float[n][];
        float[] w = new float[n], t = new float[n], v = new float[n];
        float x = CX - 0.42f, y = y0, phi = 0f;
        for (int i = 0; i < n; i++) {
            float s = i * ds;
            float q = s / len;
            sp[i] = new float[]{x, y, CZ};
            float[] tan = {Mth.sin(phi), Mth.cos(phi), 0};
            ed[i] = new float[]{tan[1], -tan[0], 0};
            th[i] = new float[]{0, 0, 1};
            float tipq = q > 0.95f ? (q - 0.95f) / 0.05f : 0f;
            w[i] = 0.85f * (1f - 0.45f * q) * (float) Math.sqrt(Math.max(0, 1 - tipq * tipq));
            t[i] = 0.09f;
            v[i] = 256f * (1f - q);
            // curvature along the blade
            float k;
            if (a.display()) {
                k = s < 7f ? 0f : 0.07f + 0.2f * (s - 7f) / (len - 7f);        // a neat coil for the inventory
            } else {
                float sway = 0.06f * Mth.sin(q * 7.5f - a.time() * 0.16f) * q;   // idle ripple
                float droop = 0.012f * q;                                         // flexes under its own weight
                float crack = 0f;
                if (a.swing() > 0f) {
                    // a bend travels from the hilt to the tip and snaps straight behind it
                    float front = a.swing() * 1.25f;
                    float d = (q - front) / 0.12f;
                    crack = 0.55f * Mth.sin((float) Math.PI * a.swing()) * (float) Math.exp(-d * d) * (d < 0 ? 1f : 0.6f);
                }
                k = sway + droop + crack;
            }
            phi += k * ds * 2.2f;
            x += Mth.sin(phi) * ds;
            y += Mth.cos(phi) * ds;
        }
        m.blade(sp, ed, th, w, t, v, WeaponMesh.SW_SPINE);
    }

    // ================================================================== Gyomei: axe on a long chain to a spiked iron ball

    static void gyomei(WeaponMesh m, Anim a) {
        // haft with the carved kanji, end cap and ring
        m.wrappedBox(CX - 0.62f, -2.0f, CZ - 0.62f, CX + 0.62f, 12.2f, CZ + 0.62f, 32, 128, 32, 128, WeaponMesh.SW_HAFT_END);
        m.box(CX - 0.8f, -2.6f, CZ - 0.8f, CX + 0.8f, -2.0f, CZ + 0.8f, WeaponMesh.SW_HAFT_END);
        m.box(CX - 0.3f, -3.3f, CZ - 0.12f, CX + 0.3f, -2.6f, CZ + 0.12f, WeaponMesh.SW_CHAIN);
        // axe head (side-on art), blade facing -x
        m.decalV(CX - 9.4f, 6.2f, CX + 1.3f, 14.8f, CZ, 1.0f, 6, 64, 128, 128, 192);
        m.box(CX - 0.85f, 11.9f, CZ - 0.85f, CX + 0.85f, 12.6f, CZ + 0.85f, WeaponMesh.SW_HAFT_END);

        float[] anchor = {CX, -3.3f, CZ};
        int links = a.display() ? 10 : 22;
        float linkLen = 1.0f;
        float[][] pts = new float[links + 1][];
        pts[0] = anchor;
        for (int i = 1; i <= links; i++) {
            float q = i / (float) links;
            float ang;   // angle from straight down, toward +x (forward of the axe's back)
            if (a.display()) {
                // inventory: the chain curls out to the right and down, ball resting beside the axe
                ang = 0.5f + 1.9f * q;
            } else if (a.swing() > 0f) {
                // swung: the chain goes taut and the ball flies out and over in a wide arc
                float arc = 2.7f * Mth.sin((float) Math.PI * Math.min(1f, a.swing() * 1.1f));
                ang = arc * (0.55f + 0.45f * q);
            } else {
                // hanging, swaying like a pendulum
                ang = 0.22f * Mth.sin(a.time() * 0.07f) * (0.4f + 0.6f * q) + 0.05f * Mth.sin(a.time() * 0.21f + q * 4f);
            }
            float[] p = pts[i - 1];
            float dz = 0.12f * Mth.sin(a.time() * 0.05f + q * 2f) * (a.display() ? 0 : 1);
            pts[i] = new float[]{p[0] + Mth.sin(ang) * linkLen, p[1] - Mth.cos(ang) * linkLen, p[2] + dz};
        }
        m.chain(pts, 0.55f, 0.18f, WeaponMesh.SW_CHAIN);

        // spiked ball hanging off the last link
        float[] end = pts[links];
        float[] prev = pts[links - 1];
        float[] dir = WeaponMesh.norm(new float[]{end[0] - prev[0], end[1] - prev[1], end[2] - prev[2]});
        float r = 3.6f;   // a massive iron ball
        float[] c = WeaponMesh.add(end, dir, r);
        ball(m, c, r);
    }

    static void ball(WeaponMesh m, float[] c, float r) {
        PoseStack pose = m.pose();
        float h = r * 0.82f;
        m.box(c[0] - h, c[1] - h, c[2] - h, c[0] + h, c[1] + h, c[2] + h, WeaponMesh.SW_BALL);
        pose.pushPose();
        pose.translate(c[0], c[1], c[2]);
        pose.mulPose(Vector3f.YP.rotationDegrees(45));
        m.box(-h, -h * 0.9f, -h, h, h * 0.9f, h, WeaponMesh.SW_BALL);
        pose.mulPose(Vector3f.XP.rotationDegrees(45));
        m.box(-h * 0.9f, -h, -h, h * 0.9f, h, h, WeaponMesh.SW_BALL);
        pose.popPose();
        float[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
                {1, 1, 1}, {1, 1, -1}, {1, -1, 1}, {1, -1, -1}, {-1, 1, 1}, {-1, 1, -1}, {-1, -1, 1}, {-1, -1, -1}};
        for (float[] d : dirs) {
            float[] u = WeaponMesh.norm(d);
            m.spike(WeaponMesh.add(c, u, r * 0.9f), u, 1.2f, 2.0f, WeaponMesh.SW_SPIKE);
        }
    }

    // ================================================================== Tengen: twin cleavers joined at the kashira by a chain

    /** One cleaver in its own frame: kashira bottom at y -0.55 on the grip axis, blade up, edge toward +x. */
    static void cleaver(WeaponMesh m) {
        float y0 = hilt(m, 6.0f, 2.2f, 0.5f);
        // chain ring under the kashira
        m.box(CX - 0.28f, -1.05f, CZ - 0.1f, CX + 0.28f, -0.55f, CZ + 0.1f, WeaponMesh.SW_CHAIN);
        float len = 14.5f, width = 4.3f;
        int n = 34;
        float[][] sp = new float[n][], ed = new float[n][], th = new float[n][];
        float[] w = new float[n], t = new float[n], v = new float[n];
        for (int i = 0; i < n; i++) {
            float s = len * i / (n - 1f);
            float q = s / len;
            float width_i = width;
            if (s < 0.8f) width_i = Mth.lerp(s / 0.8f, 3.0f, width);                    // shoulder out of the habaki
            if (s > 2.6f && s < 4.0f) width_i -= 1.15f * (1f - Math.abs((s - 3.3f) / 0.7f)); // the notch in the edge
            if (s > len - 1.8f) width_i = Mth.lerp((s - (len - 1.8f)) / 1.8f, width, 2.4f);  // clipped corner
            sp[i] = new float[]{CX - 1.3f, y0 + s, CZ};
            ed[i] = new float[]{1, 0, 0};
            th[i] = new float[]{0, 0, 1};
            w[i] = width_i;
            t[i] = 0.5f * (1f - 0.25f * q);
            v[i] = 256f * (1f - q);
        }
        m.blade(sp, ed, th, w, t, v, WeaponMesh.SW_SPINE);
    }

    static void tengen(WeaponMesh m, Anim a) {
        cleaver(m);
        float[] anchor = {CX, -1.05f, CZ};
        int links = 9;
        float linkLen = 0.75f;
        float swingAng = 0f;
        float[][] pts = new float[links + 1][];
        pts[0] = anchor;
        for (int i = 1; i <= links; i++) {
            float q = i / (float) links;
            float ang;
            if (a.display()) {
                ang = 1.2f + 1.6f * q;                                    // chain loops round to the right
            } else if (a.swing() > 0f) {
                ang = (float) (Math.PI * 2) * a.swing() * (0.7f + 0.3f * q) + 0.4f;   // whirled once round
            } else {
                ang = 0.16f * Mth.sin(a.time() * 0.09f) * (0.5f + 0.5f * q);
            }
            float[] p = pts[i - 1];
            pts[i] = new float[]{p[0] + Mth.sin(ang) * linkLen, p[1] - Mth.cos(ang) * linkLen, p[2]};
            swingAng = ang;
        }
        m.chain(pts, 0.42f, 0.14f, WeaponMesh.SW_CHAIN);

        // second cleaver: its kashira on the end of the chain, blade continuing away along the chain
        float[] end = pts[links];
        float dx = Mth.sin(swingAng), dy = -Mth.cos(swingAng);
        float theta = (float) Math.atan2(-dx, dy);
        if (a.display()) theta = 0f;                                      // lie parallel to the first in the inventory
        PoseStack pose = m.pose();
        pose.pushPose();
        pose.translate(end[0], end[1], end[2]);
        pose.mulPose(Vector3f.ZP.rotation(theta));
        if (a.display()) pose.translate(0.6f, 0f, -0.9f);
        pose.translate(-CX, 1.05f, -CZ);
        cleaver(m);
        pose.popPose();
    }
}
