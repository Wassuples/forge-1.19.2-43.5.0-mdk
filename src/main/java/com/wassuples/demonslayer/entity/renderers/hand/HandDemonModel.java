package com.wassuples.demonslayer.entity.renderers.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Hand Demon's body (drawn at 2x by {@link HandDemonRenderer}): a bloated olive torso wrapped in arms, a head with
 * a wide jagged mouth and small yellow eyes, eight arms underneath that it crawls on, and four more arms writhing on
 * its back. Long reaching arms for attacks are drawn separately by the renderer ({@link #reachArm}, {@link #reachHand}).
 *
 * <p>Texture regions (128x128): skin 0,0-128,64; head 0,64; jaw 0,84; eyes 64,64; teeth 72,64; nails 96,64;
 * mouth 64,96; palms 64,72; fingers 64,80.
 */
public class HandDemonModel extends EntityModel<HandDemonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "hand_demon"), "main");
    private static final int LEGS = 8, BACK_ARMS = 4, WRAPS = 10;

    private final ModelPart root, body, head, jaw;
    private final ModelPart[] legUpper = new ModelPart[LEGS], legLower = new ModelPart[LEGS];
    private final ModelPart[] backUpper = new ModelPart[BACK_ARMS], backLower = new ModelPart[BACK_ARMS];
    private final ModelPart[] wraps = new ModelPart[WRAPS];
    /** A one-block arm segment along +Z (scaled by the renderer to any length) and the hand that ends it. */
    public final ModelPart reachArm, reachHand;
    private final float[] legBaseZRot = new float[LEGS];

    public HandDemonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.jaw = head.getChild("jaw");
        for (int i = 0; i < LEGS; i++) {
            legUpper[i] = root.getChild("leg" + i);
            legLower[i] = legUpper[i].getChild("fore");
            legBaseZRot[i] = legUpper[i].zRot;
        }
        for (int i = 0; i < BACK_ARMS; i++) {
            backUpper[i] = body.getChild("back" + i);
            backLower[i] = backUpper[i].getChild("fore");
        }
        for (int i = 0; i < WRAPS; i++) wraps[i] = body.getChild("wrap" + i);
        this.reachArm = root.getChild("reach_arm");
        this.reachHand = root.getChild("reach_hand");
    }

    /** A hand: palm, four clawed fingers with blood-red nails, a thumb. Built hanging down (+Y). */
    private static void hand(PartDefinition parent, String name, float y, float scale) {
        CubeListBuilder palm = CubeListBuilder.create().texOffs(64, 72).addBox(-2.0F * scale, 0, -2.0F * scale, 4 * scale, 1.5F * scale, 4 * scale);
        PartDefinition h = parent.addOrReplaceChild(name, palm, PartPose.offset(0, y, 0));
        for (int f = 0; f < 4; f++) {
            float x = (-1.5F + f) * scale;
            h.addOrReplaceChild("f" + f, CubeListBuilder.create()
                            .texOffs(64, 80).addBox(-0.45F * scale, 0, -0.45F * scale, 0.9F * scale, 2.2F * scale, 0.9F * scale)
                            .texOffs(96, 64).addBox(-0.5F * scale, 2.1F * scale, -0.6F * scale, 1.0F * scale, 0.9F * scale, 1.0F * scale),
                    PartPose.offsetAndRotation(x, 1.4F * scale, -1.6F * scale, -0.35F, 0, 0));
        }
        h.addOrReplaceChild("thumb", CubeListBuilder.create()
                        .texOffs(64, 80).addBox(-0.45F * scale, 0, -0.45F * scale, 0.9F * scale, 1.8F * scale, 0.9F * scale)
                        .texOffs(96, 64).addBox(-0.5F * scale, 1.7F * scale, -0.5F * scale, 1.0F * scale, 0.8F * scale, 1.0F * scale),
                PartPose.offsetAndRotation(2.0F * scale, 1.0F * scale, 0, 0, 0, -0.6F));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---- torso, a bloated mass
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-9.0F, -8.0F, -11.0F, 18, 14, 22)
                        .texOffs(0, 0).addBox(-7.0F, -11.0F, -8.0F, 14, 4, 16, new CubeDeformation(0.2F))
                        .texOffs(0, 0).addBox(-8.0F, 5.0F, -9.0F, 16, 4, 18),   // sagging belly
                PartPose.offset(0, 3.0F, 1.0F));

        // ---- head jutting forward and low, with the wide mouth
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-6.0F, -6.0F, -9.0F, 12, 9, 10)
                        .texOffs(64, 64).addBox(-4.5F, -4.0F, -9.3F, 2, 2, 1)     // eyes
                        .texOffs(64, 64).addBox(2.5F, -4.0F, -9.3F, 2, 2, 1)
                        .texOffs(72, 64).addBox(-5.5F, 2.0F, -9.2F, 11, 1, 1)     // upper teeth
                        .texOffs(64, 96).addBox(-5.0F, 1.0F, -8.6F, 10, 2, 8),   // mouth roof
                PartPose.offset(0, -1.0F, -10.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(0, 84).addBox(-5.5F, 0, -9.0F, 11, 4, 9)
                        .texOffs(72, 67).addBox(-5.0F, -1.0F, -8.8F, 10, 1, 1)    // lower teeth
                        .texOffs(64, 96).addBox(-4.5F, -0.2F, -8.3F, 9, 1, 7),
                PartPose.offset(0, 3.0F, 0.5F));

        // ---- arms wrapped around the torso like clothing
        final float Q = (float) Math.PI / 2;
        float[][] wraps = {   // x, y, z, xRot, yRot, zRot — each lies along the torso's surface
                {-9.3F, -4, 0, Q + 0.25F, 0, 0}, {-9.3F, 2, 1, Q - 0.3F, 0, 0}, {9.3F, -3, -1, Q - 0.25F, 0, 0}, {9.3F, 3, 0, Q + 0.3F, 0, 0},
                {0, -11.3F, -5, 0, 0.5F, Q}, {0, -11.3F, 3, 0, -0.4F, Q}, {2, -11.3F, 0, Q, 0, 0},
                {0, 4, -11.3F, 0, 0, Q}, {0, -2, 11.3F, 0, 0, 1.2F}, {0, 6.3F, 0, 0, 0.3F, Q}};
        for (int i = 0; i < wraps.length; i++) {
            float[] w = wraps[i];
            PartDefinition wrap = body.addOrReplaceChild("wrap" + i, CubeListBuilder.create().texOffs(0, 36).addBox(-1.3F, -10.0F, -1.3F, 2.6F, 20, 2.6F),
                    PartPose.offsetAndRotation(w[0], w[1], w[2], w[3], w[4], w[5]));
            hand(wrap, "hand", 9.5F, 0.8F);
        }

        // ---- eight arms underneath: it crawls on its hands
        float[] zs = {-9, -3.5F, 2.5F, 8};
        for (int i = 0; i < LEGS; i++) {
            boolean left = i < 4;
            float z = zs[i % 4];
            PartDefinition upper = root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 36).addBox(-1.6F, 0, -1.6F, 3.2F, 10, 3.2F),
                    PartPose.offsetAndRotation(left ? 7.5F : -7.5F, 7.0F, z, 0, 0, left ? -0.75F : 0.75F));
            PartDefinition fore = upper.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(16, 36).addBox(-1.3F, 0, -1.3F, 2.6F, 9.5F, 2.6F),
                    PartPose.offsetAndRotation(0, 9.6F, 0, 0, 0, left ? 1.05F : -1.05F));
            hand(fore, "hand", 9.3F, 1.0F);
        }

        // ---- arms writhing on its back
        float[][] backs = {{-5, -10, 3}, {5, -10, 3}, {-2.5F, -10, 8}, {2.5F, -10, 8}};
        for (int i = 0; i < BACK_ARMS; i++) {
            float[] b = backs[i];
            PartDefinition upper = body.addOrReplaceChild("back" + i, CubeListBuilder.create().texOffs(0, 36).addBox(-1.3F, -10.0F, -1.3F, 2.6F, 10, 2.6F),
                    PartPose.offsetAndRotation(b[0], b[1], b[2], -0.3F, 0, b[0] < 0 ? -0.35F : 0.35F));
            PartDefinition fore = upper.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(16, 36).addBox(-1.1F, -9.0F, -1.1F, 2.2F, 9, 2.2F),
                    PartPose.offsetAndRotation(0, -9.8F, 0, 0.6F, 0, 0));
            PartDefinition hand = fore.addOrReplaceChild("hand_root", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -9.0F, 0, (float) Math.PI, 0, 0));
            hand(hand, "hand", 0, 0.9F);
        }

        // ---- reaching arm (1 block long along +Z) and its hand, for the renderer
        root.addOrReplaceChild("reach_arm", CubeListBuilder.create().texOffs(0, 36).addBox(-1.5F, -1.5F, 0, 3, 3, 16), PartPose.ZERO);
        PartDefinition reachHand = root.addOrReplaceChild("reach_hand", CubeListBuilder.create(), PartPose.rotation((float) Math.PI / 2, 0, 0));
        hand(reachHand, "hand", 0, 1.3F);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(HandDemonEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        int attack = e.attack();
        float at = e.attackTick();
        // breathing / heaving
        body.y = 3.0F + Mth.sin(ageInTicks * 0.08F) * 0.6F;
        body.xRot = Mth.sin(ageInTicks * 0.05F) * 0.03F;
        // crawl: alternate hands in two groups, like a centipede of arms
        float walk = Math.min(1.0F, limbSwingAmount * 1.6F);
        for (int i = 0; i < LEGS; i++) {
            float phase = (i % 2 == 0 ? 0 : (float) Math.PI) + (i % 4) * 0.7F;
            float s = Mth.sin(limbSwing * 0.55F + phase);
            legUpper[i].xRot = s * 0.55F * walk;
            legUpper[i].zRot = legBaseZRot[i] + Math.max(0, -s) * 0.25F * walk * (i < 4 ? -1 : 1);
            legLower[i].xRot = -s * 0.25F * walk;
        }
        // back arms writhe; for the cluster fist they rise together
        for (int i = 0; i < BACK_ARMS; i++) {
            float sway = Mth.sin(ageInTicks * 0.12F + i * 1.7F);
            backUpper[i].xRot = -0.3F + sway * 0.35F;
            backLower[i].xRot = 0.6F + Mth.sin(ageInTicks * 0.17F + i) * 0.4F;
            if (attack == HandDemonEntity.FIST) {
                float raise = at < HandDemonEntity.FIST_SLAM ? Math.min(1, at / 12F) : Math.max(0, 1 - (at - HandDemonEntity.FIST_SLAM) / 4F);
                backUpper[i].xRot = Mth.lerp(raise, backUpper[i].xRot, -1.4F);
            } else if (attack == HandDemonEntity.SWEEP) {
                backUpper[i].zRot = (i % 2 == 0 ? -1 : 1) * (0.35F + Mth.sin(at * 0.9F) * 1.1F);
            }
        }
        // wrapped arms squirm a little
        for (int i = 0; i < WRAPS; i++) wraps[i].yRot = Mth.sin(ageInTicks * 0.07F + i * 2.1F) * 0.06F;
        // head follows its prey; the jaw gapes when it attacks (and grinds idly)
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
        float gape = 0.12F + Mth.sin(ageInTicks * 0.1F) * 0.08F;
        if (attack != HandDemonEntity.NONE) gape = 0.55F + Mth.sin(at * 0.5F) * 0.15F;
        if (attack == HandDemonEntity.GRAB && at > 50) gape = 0.9F;
        jaw.xRot = gape;
        // burrow: the front hands dig in
        if (attack == HandDemonEntity.BURROW && at < HandDemonEntity.BURROW_ERUPT + 6) {
            float dig = Math.min(1, at / 10F);
            for (int i : new int[]{0, 4}) legUpper[i].xRot = Mth.lerp(dig, legUpper[i].xRot, -0.9F);
            body.xRot += dig * 0.2F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        body.render(pose, buffer, light, overlay, r, g, b, a);
        for (ModelPart leg : legUpper) leg.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
