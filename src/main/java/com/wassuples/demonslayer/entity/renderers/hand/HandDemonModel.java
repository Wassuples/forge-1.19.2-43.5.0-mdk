package com.wassuples.demonslayer.entity.renderers.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Hand Demon as drawn in the anime (drawn at {@link HandDemonRenderer#SCALE}x, about five blocks tall and four
 * across): an enormous, round, bloated mass, pale olive with dark veins, bound in coils of its own arms like rolls of
 * fat, with more arms clutching across its belly. It squats on four stubby legs, each a thick bent arm planted on a
 * great hand. Its small head sits on top, wholly wrapped in its own hands (crown, brow, cheeks, nape, and two clamped
 * over the mouth): only its yellow, red-rimmed eyes peer out between the fingers. Eight arms reach up and out from its
 * upper body. Red nails on every finger.
 *
 * <p>The geometry below is generated from the design prototype (round body slabs are a box plus the same box turned
 * 45 degrees; each coil is eight arm segments round an ellipse). The long arms it throws when attacking are drawn by
 * the renderer ({@link #reachArm}, {@link #reachHand}).
 *
 * <p>Texture (128x128, {@code hand_demon_body.png}): body flesh 0,0-128,64; arm flesh 0,64; eyes 64,64; palms and
 * fingers 64,72; nails 96,72.
 */
public class HandDemonModel extends EntityModel<HandDemonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "hand_demon"), "main");
    private static final int LEGS = 4, REACH = 8, RINGS = 4;

    private final ModelPart body, head, muzzleL, muzzleR;
    private final float muzzleLZ, muzzleRZ;
    private final ModelPart[] leg = new ModelPart[LEGS], shin = new ModelPart[LEGS];
    private final ModelPart[] reach = new ModelPart[REACH], reachFore = new ModelPart[REACH], ring = new ModelPart[RINGS];
    private final float[] legX = new float[LEGS], shinX = new float[LEGS], reachX = new float[REACH], reachZ = new float[REACH], foreX = new float[REACH];
    private final float bodyY;
    /** A one-block arm segment along +Z (scaled by the renderer to any length) and the hand that ends it. */
    public final ModelPart reachArm, reachHand;

    public HandDemonModel(ModelPart root) {
        this.body = root.getChild("body");
        this.bodyY = body.y;
        this.head = body.getChild("head");
        this.muzzleL = head.getChild("muzzle").getChild("l");
        this.muzzleR = head.getChild("muzzle").getChild("r");
        this.muzzleLZ = muzzleL.zRot;
        this.muzzleRZ = muzzleR.zRot;
        for (int i = 0; i < LEGS; i++) {
            leg[i] = root.getChild("leg" + i);
            shin[i] = leg[i].getChild("shin");
            legX[i] = leg[i].xRot;
            shinX[i] = shin[i].xRot;
        }
        for (int i = 0; i < REACH; i++) {
            reach[i] = body.getChild("reach" + i);
            reachFore[i] = reach[i].getChild("fore");
            reachX[i] = reach[i].xRot;
            reachZ[i] = reach[i].zRot;
            foreX[i] = reachFore[i].xRot;
        }
        for (int i = 0; i < RINGS; i++) ring[i] = body.getChild("ring" + i);
        this.reachArm = root.getChild("reach_arm");
        this.reachHand = root.getChild("reach_hand");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 5.5F, 1F, 0.05F, 0F, 0F));
        PartDefinition p2 = p1.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-12F, -4F, -10F, 24F, 5F, 20F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p2.addOrReplaceChild("r", CubeListBuilder.create().texOffs(0, 0).addBox(-9.84F, -3.7F, -8.2F, 19.68F, 4.4F, 16.4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0.785F, 0F));
        PartDefinition p4 = p1.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(0, 0).addBox(-17F, -17F, -14F, 34F, 14F, 28F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p4.addOrReplaceChild("r", CubeListBuilder.create().texOffs(0, 0).addBox(-13.94F, -16.7F, -11.48F, 27.88F, 13.4F, 22.96F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0.785F, 0F));
        PartDefinition p6 = p1.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 0).addBox(-15F, -25F, -12.5F, 30F, 9F, 25F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p6.addOrReplaceChild("r", CubeListBuilder.create().texOffs(0, 0).addBox(-12.3F, -24.7F, -10.25F, 24.6F, 8.4F, 20.5F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0.785F, 0F));
        PartDefinition p8 = p1.addOrReplaceChild("shoulders", CubeListBuilder.create().texOffs(0, 0).addBox(-11F, -31F, -9.5F, 22F, 7F, 19F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p8.addOrReplaceChild("r", CubeListBuilder.create().texOffs(0, 0).addBox(-9.02F, -30.7F, -7.79F, 18.04F, 6.4F, 15.58F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0.785F, 0F));
        PartDefinition p10 = p1.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0).addBox(-6.5F, -34F, -6F, 13F, 4F, 12F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p10.addOrReplaceChild("r", CubeListBuilder.create().texOffs(0, 0).addBox(-5.33F, -33.7F, -4.92F, 10.66F, 3.4F, 9.84F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0.785F, 0F));
        PartDefinition p12 = p1.addOrReplaceChild("ring0", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p13 = p12.addOrReplaceChild("s0", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -5.441F, -11.5F, 0F, 0F, 0F));
        p13.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -5.608F, -2.6F, 5.2F, 11.215F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p15 = p12.addOrReplaceChild("s1", CubeListBuilder.create(), PartPose.offsetAndRotation(9.899F, -5.309F, -8.132F, 0F, -0.785F, 0F));
        PartDefinition p16 = p15.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -5.608F, -2.6F, 5.2F, 11.215F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p17 = p16.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.1F, 0F, -0.7F, 4.2F, 3.2F, 1.4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -1.571F));
        p17.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.3F, 1F).texOffs(96, 72).addBox(-0.52F, 2F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(-1.5F, 3F, 0F, 1.2F, 0F, 0F));
        p17.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.8F, 1F).texOffs(96, 72).addBox(-0.52F, 2.5F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(-0.5F, 3F, 0F, 1.2F, 0F, 0F));
        p17.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.8F, 1F).texOffs(96, 72).addBox(-0.52F, 2.5F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(0.5F, 3F, 0F, 1.2F, 0F, 0F));
        p17.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.3F, 1F).texOffs(96, 72).addBox(-0.52F, 2F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(1.5F, 3F, 0F, 1.2F, 0F, 0F));
        p17.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.2F, 1F).texOffs(96, 72).addBox(-0.52F, 1.9F, -0.3F, 1.04F, 0.75F, 0.8F), PartPose.offsetAndRotation(2.1F, 0.9F, 0F, 0.92F, 0F, -0.6F));
        PartDefinition p23 = p12.addOrReplaceChild("s2", CubeListBuilder.create(), PartPose.offsetAndRotation(14F, -5.559F, 0F, 0F, -1.571F, 0F));
        p23.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -5.608F, -2.6F, 5.2F, 11.215F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p25 = p12.addOrReplaceChild("s3", CubeListBuilder.create(), PartPose.offsetAndRotation(9.899F, -5.691F, 8.132F, 0F, -2.356F, 0F));
        p25.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -5.608F, -2.6F, 5.2F, 11.215F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p27 = p12.addOrReplaceChild("s4", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -5.441F, 11.5F, 0F, -3.142F, 0F));
        p27.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -5.608F, -2.6F, 5.2F, 11.215F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p29 = p12.addOrReplaceChild("s5", CubeListBuilder.create(), PartPose.offsetAndRotation(-9.899F, -5.309F, 8.132F, 0F, -3.927F, 0F));
        p29.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -5.608F, -2.6F, 5.2F, 11.215F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p31 = p12.addOrReplaceChild("s6", CubeListBuilder.create(), PartPose.offsetAndRotation(-14F, -5.559F, 0F, 0F, -4.712F, 0F));
        p31.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -5.608F, -2.6F, 5.2F, 11.215F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p33 = p12.addOrReplaceChild("s7", CubeListBuilder.create(), PartPose.offsetAndRotation(-9.899F, -5.691F, -8.132F, 0F, -5.498F, 0F));
        p33.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -5.608F, -2.6F, 5.2F, 11.215F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p35 = p1.addOrReplaceChild("ring1", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p36 = p35.addOrReplaceChild("s0", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -11.915F, -15F, 0F, 0F, 0F));
        p36.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.8F, -7.367F, -2.8F, 5.6F, 14.734F, 5.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p38 = p35.addOrReplaceChild("s1", CubeListBuilder.create(), PartPose.offsetAndRotation(13.081F, -11.775F, -10.607F, 0F, -0.785F, 0F));
        p38.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.8F, -7.367F, -2.8F, 5.6F, 14.734F, 5.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p40 = p35.addOrReplaceChild("s2", CubeListBuilder.create(), PartPose.offsetAndRotation(18.5F, -12.085F, 0F, 0F, -1.571F, 0F));
        p40.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.8F, -7.367F, -2.8F, 5.6F, 14.734F, 5.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p42 = p35.addOrReplaceChild("s3", CubeListBuilder.create(), PartPose.offsetAndRotation(13.081F, -12.225F, 10.607F, 0F, -2.356F, 0F));
        p42.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.8F, -7.367F, -2.8F, 5.6F, 14.734F, 5.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p44 = p35.addOrReplaceChild("s4", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -11.915F, 15F, 0F, -3.142F, 0F));
        p44.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.8F, -7.367F, -2.8F, 5.6F, 14.734F, 5.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p46 = p35.addOrReplaceChild("s5", CubeListBuilder.create(), PartPose.offsetAndRotation(-13.081F, -11.775F, 10.607F, 0F, -3.927F, 0F));
        p46.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.8F, -7.367F, -2.8F, 5.6F, 14.734F, 5.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p48 = p35.addOrReplaceChild("s6", CubeListBuilder.create(), PartPose.offsetAndRotation(-18.5F, -12.085F, 0F, 0F, -4.712F, 0F));
        PartDefinition p49 = p48.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.8F, -7.367F, -2.8F, 5.6F, 14.734F, 5.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p50 = p49.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.1F, 0F, -0.7F, 4.2F, 3.2F, 1.4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -1.571F));
        p50.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.3F, 1F).texOffs(96, 72).addBox(-0.52F, 2F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(-1.5F, 3F, 0F, 1.2F, 0F, 0F));
        p50.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.8F, 1F).texOffs(96, 72).addBox(-0.52F, 2.5F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(-0.5F, 3F, 0F, 1.2F, 0F, 0F));
        p50.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.8F, 1F).texOffs(96, 72).addBox(-0.52F, 2.5F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(0.5F, 3F, 0F, 1.2F, 0F, 0F));
        p50.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.3F, 1F).texOffs(96, 72).addBox(-0.52F, 2F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(1.5F, 3F, 0F, 1.2F, 0F, 0F));
        p50.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.2F, 1F).texOffs(96, 72).addBox(-0.52F, 1.9F, -0.3F, 1.04F, 0.75F, 0.8F), PartPose.offsetAndRotation(2.1F, 0.9F, 0F, 0.92F, 0F, -0.6F));
        PartDefinition p56 = p35.addOrReplaceChild("s7", CubeListBuilder.create(), PartPose.offsetAndRotation(-13.081F, -12.225F, -10.607F, 0F, -5.498F, 0F));
        p56.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.8F, -7.367F, -2.8F, 5.6F, 14.734F, 5.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p58 = p1.addOrReplaceChild("ring2", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p59 = p58.addOrReplaceChild("s0", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -18.87F, -14.5F, 0F, 0F, 0F));
        p59.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -7.037F, -2.6F, 5.2F, 14.074F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p61 = p58.addOrReplaceChild("s1", CubeListBuilder.create(), PartPose.offsetAndRotation(12.374F, -18.73F, -10.253F, 0F, -0.785F, 0F));
        p61.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -7.037F, -2.6F, 5.2F, 14.074F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p63 = p58.addOrReplaceChild("s2", CubeListBuilder.create(), PartPose.offsetAndRotation(17.5F, -19.13F, 0F, 0F, -1.571F, 0F));
        p63.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -7.037F, -2.6F, 5.2F, 14.074F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p65 = p58.addOrReplaceChild("s3", CubeListBuilder.create(), PartPose.offsetAndRotation(12.374F, -19.27F, 10.253F, 0F, -2.356F, 0F));
        PartDefinition p66 = p65.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -7.037F, -2.6F, 5.2F, 14.074F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p67 = p66.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.1F, 0F, -0.7F, 4.2F, 3.2F, 1.4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -1.571F));
        p67.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.3F, 1F).texOffs(96, 72).addBox(-0.52F, 2F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(-1.5F, 3F, 0F, 1.2F, 0F, 0F));
        p67.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.8F, 1F).texOffs(96, 72).addBox(-0.52F, 2.5F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(-0.5F, 3F, 0F, 1.2F, 0F, 0F));
        p67.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.8F, 1F).texOffs(96, 72).addBox(-0.52F, 2.5F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(0.5F, 3F, 0F, 1.2F, 0F, 0F));
        p67.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.3F, 1F).texOffs(96, 72).addBox(-0.52F, 2F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(1.5F, 3F, 0F, 1.2F, 0F, 0F));
        p67.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.2F, 1F).texOffs(96, 72).addBox(-0.52F, 1.9F, -0.3F, 1.04F, 0.75F, 0.8F), PartPose.offsetAndRotation(2.1F, 0.9F, 0F, 0.92F, 0F, -0.6F));
        PartDefinition p73 = p58.addOrReplaceChild("s4", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -18.87F, 14.5F, 0F, -3.142F, 0F));
        p73.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -7.037F, -2.6F, 5.2F, 14.074F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p75 = p58.addOrReplaceChild("s5", CubeListBuilder.create(), PartPose.offsetAndRotation(-12.374F, -18.73F, 10.253F, 0F, -3.927F, 0F));
        p75.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -7.037F, -2.6F, 5.2F, 14.074F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p77 = p58.addOrReplaceChild("s6", CubeListBuilder.create(), PartPose.offsetAndRotation(-17.5F, -19.13F, 0F, 0F, -4.712F, 0F));
        p77.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -7.037F, -2.6F, 5.2F, 14.074F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p79 = p58.addOrReplaceChild("s7", CubeListBuilder.create(), PartPose.offsetAndRotation(-12.374F, -19.27F, -10.253F, 0F, -5.498F, 0F));
        p79.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.6F, -7.037F, -2.6F, 5.2F, 14.074F, 5.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p81 = p1.addOrReplaceChild("ring3", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p82 = p81.addOrReplaceChild("s0", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -25.941F, -11.5F, 0F, 0F, 0F));
        p82.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.3F, -5.498F, -2.3F, 4.6F, 10.996F, 4.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p84 = p81.addOrReplaceChild("s1", CubeListBuilder.create(), PartPose.offsetAndRotation(9.546F, -25.809F, -8.132F, 0F, -0.785F, 0F));
        p84.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.3F, -5.498F, -2.3F, 4.6F, 10.996F, 4.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p86 = p81.addOrReplaceChild("s2", CubeListBuilder.create(), PartPose.offsetAndRotation(13.5F, -26.059F, 0F, 0F, -1.571F, 0F));
        p86.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.3F, -5.498F, -2.3F, 4.6F, 10.996F, 4.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p88 = p81.addOrReplaceChild("s3", CubeListBuilder.create(), PartPose.offsetAndRotation(9.546F, -26.191F, 8.132F, 0F, -2.356F, 0F));
        p88.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.3F, -5.498F, -2.3F, 4.6F, 10.996F, 4.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p90 = p81.addOrReplaceChild("s4", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -25.941F, 11.5F, 0F, -3.142F, 0F));
        p90.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.3F, -5.498F, -2.3F, 4.6F, 10.996F, 4.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p92 = p81.addOrReplaceChild("s5", CubeListBuilder.create(), PartPose.offsetAndRotation(-9.546F, -25.809F, 8.132F, 0F, -3.927F, 0F));
        p92.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.3F, -5.498F, -2.3F, 4.6F, 10.996F, 4.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p94 = p81.addOrReplaceChild("s6", CubeListBuilder.create(), PartPose.offsetAndRotation(-13.5F, -26.059F, 0F, 0F, -4.712F, 0F));
        p94.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.3F, -5.498F, -2.3F, 4.6F, 10.996F, 4.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p96 = p81.addOrReplaceChild("s7", CubeListBuilder.create(), PartPose.offsetAndRotation(-9.546F, -26.191F, -8.132F, 0F, -5.498F, 0F));
        PartDefinition p97 = p96.addOrReplaceChild("lay", CubeListBuilder.create().texOffs(0, 64).addBox(-2.3F, -5.498F, -2.3F, 4.6F, 10.996F, 4.6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 1.571F));
        PartDefinition p98 = p97.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.1F, 0F, -0.7F, 4.2F, 3.2F, 1.4F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -1.571F));
        p98.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.3F, 1F).texOffs(96, 72).addBox(-0.52F, 2F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(-1.5F, 3F, 0F, 1.2F, 0F, 0F));
        p98.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.8F, 1F).texOffs(96, 72).addBox(-0.52F, 2.5F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(-0.5F, 3F, 0F, 1.2F, 0F, 0F));
        p98.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.8F, 1F).texOffs(96, 72).addBox(-0.52F, 2.5F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(0.5F, 3F, 0F, 1.2F, 0F, 0F));
        p98.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.3F, 1F).texOffs(96, 72).addBox(-0.52F, 2F, -0.3F, 1.04F, 0.8F, 0.8F), PartPose.offsetAndRotation(1.5F, 3F, 0F, 1.2F, 0F, 0F));
        p98.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.5F, 0F, -0.5F, 1F, 2.2F, 1F).texOffs(96, 72).addBox(-0.52F, 1.9F, -0.3F, 1.04F, 0.75F, 0.8F), PartPose.offsetAndRotation(2.1F, 0.9F, 0F, 0.92F, 0F, -0.6F));
        PartDefinition p104 = p1.addOrReplaceChild("clutch0", CubeListBuilder.create().texOffs(0, 64).addBox(-2.2F, -9F, -2.2F, 4.4F, 18F, 4.4F), PartPose.offsetAndRotation(-3F, -15F, -16.5F, 0F, 0F, 1F));
        PartDefinition p105 = p104.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.625F, 0F, -0.875F, 5.25F, 4F, 1.75F), PartPose.offsetAndRotation(0F, 8F, 0F, 0.3F, 0F, 0F));
        p105.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.875F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.5F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(-1.875F, 3.75F, 0F, 1.1F, 0F, -0.225F));
        p105.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 3.5F, 1.25F).texOffs(96, 72).addBox(-0.65F, 3.125F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(-0.625F, 3.75F, 0F, 1.1F, 0F, -0.075F));
        p105.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 3.5F, 1.25F).texOffs(96, 72).addBox(-0.65F, 3.125F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(0.625F, 3.75F, 0F, 1.1F, 0F, 0.075F));
        p105.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.875F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.5F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(1.875F, 3.75F, 0F, 1.1F, 0F, 0.225F));
        p105.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.75F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.375F, -0.375F, 1.3F, 0.938F, 1F), PartPose.offsetAndRotation(2.625F, 1.125F, 0F, 0.86F, 0F, -0.6F));
        PartDefinition p111 = p1.addOrReplaceChild("clutch1", CubeListBuilder.create().texOffs(0, 64).addBox(-2.2F, -8F, -2.2F, 4.4F, 16F, 4.4F), PartPose.offsetAndRotation(4F, -22F, -15F, 0F, 0F, -1.25F));
        PartDefinition p112 = p111.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.625F, 0F, -0.875F, 5.25F, 4F, 1.75F), PartPose.offsetAndRotation(0F, 7F, 0F, 0.3F, 0F, 0F));
        p112.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.875F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.5F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(-1.875F, 3.75F, 0F, 1.1F, 0F, -0.225F));
        p112.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 3.5F, 1.25F).texOffs(96, 72).addBox(-0.65F, 3.125F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(-0.625F, 3.75F, 0F, 1.1F, 0F, -0.075F));
        p112.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 3.5F, 1.25F).texOffs(96, 72).addBox(-0.65F, 3.125F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(0.625F, 3.75F, 0F, 1.1F, 0F, 0.075F));
        p112.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.875F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.5F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(1.875F, 3.75F, 0F, 1.1F, 0F, 0.225F));
        p112.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.75F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.375F, -0.375F, 1.3F, 0.938F, 1F), PartPose.offsetAndRotation(2.625F, 1.125F, 0F, 0.86F, 0F, -0.6F));
        PartDefinition p118 = p1.addOrReplaceChild("clutch2", CubeListBuilder.create().texOffs(0, 64).addBox(-2.2F, -6F, -2.2F, 4.4F, 12F, 4.4F), PartPose.offsetAndRotation(-1F, -8F, -16F, 0F, 0F, -0.4F));
        PartDefinition p119 = p118.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.625F, 0F, -0.875F, 5.25F, 4F, 1.75F), PartPose.offsetAndRotation(0F, 5F, 0F, 0.3F, 0F, 0F));
        p119.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.875F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.5F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(-1.875F, 3.75F, 0F, 1.1F, 0F, -0.225F));
        p119.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 3.5F, 1.25F).texOffs(96, 72).addBox(-0.65F, 3.125F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(-0.625F, 3.75F, 0F, 1.1F, 0F, -0.075F));
        p119.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 3.5F, 1.25F).texOffs(96, 72).addBox(-0.65F, 3.125F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(0.625F, 3.75F, 0F, 1.1F, 0F, 0.075F));
        p119.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.875F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.5F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(1.875F, 3.75F, 0F, 1.1F, 0F, 0.225F));
        p119.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.75F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.375F, -0.375F, 1.3F, 0.938F, 1F), PartPose.offsetAndRotation(2.625F, 1.125F, 0F, 0.86F, 0F, -0.6F));
        PartDefinition p125 = p1.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -10F, -5F, 10F, 9F, 10F).texOffs(64, 64).addBox(-4.2F, -7.4F, -5.6F, 3.2F, 3.2F, 1.2F).addBox(1F, -7F, -5.6F, 3.2F, 3.2F, 1.2F), PartPose.offsetAndRotation(0F, -34F, -2F, 0.15F, 0F, 0F));
        PartDefinition p126 = p125.addOrReplaceChild("muzzle", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p127 = p126.addOrReplaceChild("l", CubeListBuilder.create(), PartPose.offsetAndRotation(-5.6F, -2.6F, -5.9F, 0F, 0F, -1.571F));
        PartDefinition p128 = p127.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.415F, 0F, -0.805F, 4.83F, 3.68F, 1.61F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p128.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.575F, 0F, -0.575F, 1.15F, 2.645F, 1.15F).texOffs(96, 72).addBox(-0.598F, 2.3F, -0.345F, 1.196F, 0.92F, 0.92F), PartPose.offsetAndRotation(-1.725F, 3.45F, 0F, 0.3F, 0F, -0.075F));
        p128.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.575F, 0F, -0.575F, 1.15F, 3.22F, 1.15F).texOffs(96, 72).addBox(-0.598F, 2.875F, -0.345F, 1.196F, 0.92F, 0.92F), PartPose.offsetAndRotation(-0.575F, 3.45F, 0F, 0.3F, 0F, -0.025F));
        p128.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.575F, 0F, -0.575F, 1.15F, 3.22F, 1.15F).texOffs(96, 72).addBox(-0.598F, 2.875F, -0.345F, 1.196F, 0.92F, 0.92F), PartPose.offsetAndRotation(0.575F, 3.45F, 0F, 0.3F, 0F, 0.025F));
        p128.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.575F, 0F, -0.575F, 1.15F, 2.645F, 1.15F).texOffs(96, 72).addBox(-0.598F, 2.3F, -0.345F, 1.196F, 0.92F, 0.92F), PartPose.offsetAndRotation(1.725F, 3.45F, 0F, 0.3F, 0F, 0.075F));
        p128.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.575F, 0F, -0.575F, 1.15F, 2.53F, 1.15F).texOffs(96, 72).addBox(-0.598F, 2.185F, -0.345F, 1.196F, 0.862F, 0.92F), PartPose.offsetAndRotation(2.415F, 1.035F, 0F, 0.38F, 0F, -0.6F));
        PartDefinition p134 = p126.addOrReplaceChild("r", CubeListBuilder.create(), PartPose.offsetAndRotation(5.6F, -1.4F, -6.5F, 0F, 0F, 1.571F));
        PartDefinition p135 = p134.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.31F, 0F, -0.77F, 4.62F, 3.52F, 1.54F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p135.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.55F, 0F, -0.55F, 1.1F, 2.53F, 1.1F).texOffs(96, 72).addBox(-0.572F, 2.2F, -0.33F, 1.144F, 0.88F, 0.88F), PartPose.offsetAndRotation(-1.65F, 3.3F, 0F, 0.3F, 0F, -0.075F));
        p135.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.55F, 0F, -0.55F, 1.1F, 3.08F, 1.1F).texOffs(96, 72).addBox(-0.572F, 2.75F, -0.33F, 1.144F, 0.88F, 0.88F), PartPose.offsetAndRotation(-0.55F, 3.3F, 0F, 0.3F, 0F, -0.025F));
        p135.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.55F, 0F, -0.55F, 1.1F, 3.08F, 1.1F).texOffs(96, 72).addBox(-0.572F, 2.75F, -0.33F, 1.144F, 0.88F, 0.88F), PartPose.offsetAndRotation(0.55F, 3.3F, 0F, 0.3F, 0F, 0.025F));
        p135.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.55F, 0F, -0.55F, 1.1F, 2.53F, 1.1F).texOffs(96, 72).addBox(-0.572F, 2.2F, -0.33F, 1.144F, 0.88F, 0.88F), PartPose.offsetAndRotation(1.65F, 3.3F, 0F, 0.3F, 0F, 0.075F));
        p135.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.55F, 0F, -0.55F, 1.1F, 2.42F, 1.1F).texOffs(96, 72).addBox(-0.572F, 2.09F, -0.33F, 1.144F, 0.825F, 0.88F), PartPose.offsetAndRotation(2.31F, 0.99F, 0F, 0.38F, 0F, -0.6F));
        PartDefinition p141 = p125.addOrReplaceChild("over", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -10.6F, 4.6F, 0F, 0F, 0F));
        PartDefinition p142 = p141.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-4.935F, 0F, -1.645F, 9.87F, 7.52F, 3.29F), PartPose.offsetAndRotation(0F, 0F, 0F, -1.451F, 0.08F, 0F));
        p142.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-1.175F, 0F, -1.175F, 2.35F, 5.405F, 2.35F).texOffs(96, 72).addBox(-1.222F, 4.7F, -0.705F, 2.444F, 1.88F, 1.88F), PartPose.offsetAndRotation(-3.525F, 7.05F, 0F, 1.45F, 0F, -0.15F));
        p142.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-1.175F, 0F, -1.175F, 2.35F, 6.58F, 2.35F).texOffs(96, 72).addBox(-1.222F, 5.875F, -0.705F, 2.444F, 1.88F, 1.88F), PartPose.offsetAndRotation(-1.175F, 7.05F, 0F, 1.45F, 0F, -0.05F));
        p142.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-1.175F, 0F, -1.175F, 2.35F, 6.58F, 2.35F).texOffs(96, 72).addBox(-1.222F, 5.875F, -0.705F, 2.444F, 1.88F, 1.88F), PartPose.offsetAndRotation(1.175F, 7.05F, 0F, 1.45F, 0F, 0.05F));
        p142.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-1.175F, 0F, -1.175F, 2.35F, 5.405F, 2.35F).texOffs(96, 72).addBox(-1.222F, 4.7F, -0.705F, 2.444F, 1.88F, 1.88F), PartPose.offsetAndRotation(3.525F, 7.05F, 0F, 1.45F, 0F, 0.15F));
        p142.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-1.175F, 0F, -1.175F, 2.35F, 5.17F, 2.35F).texOffs(96, 72).addBox(-1.222F, 4.465F, -0.705F, 2.444F, 1.763F, 1.88F), PartPose.offsetAndRotation(4.935F, 2.115F, 0F, 1.07F, 0F, -0.6F));
        PartDefinition p148 = p125.addOrReplaceChild("browL", CubeListBuilder.create(), PartPose.offsetAndRotation(-5.4F, -10.6F, 2F, 0F, 1.571F, 0F));
        PartDefinition p149 = p148.addOrReplaceChild("t", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p150 = p149.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-3.15F, 0F, -1.05F, 6.3F, 4.8F, 2.1F), PartPose.offsetAndRotation(0F, 0F, 0F, -1.371F, 0F, 0F));
        p150.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 3.45F, 1.5F).texOffs(96, 72).addBox(-0.78F, 3F, -0.45F, 1.56F, 1.2F, 1.2F), PartPose.offsetAndRotation(-2.25F, 4.5F, 0F, 1.3F, 0F, 0F));
        p150.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 4.2F, 1.5F).texOffs(96, 72).addBox(-0.78F, 3.75F, -0.45F, 1.56F, 1.2F, 1.2F), PartPose.offsetAndRotation(-0.75F, 4.5F, 0F, 1.3F, 0F, 0F));
        p150.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 4.2F, 1.5F).texOffs(96, 72).addBox(-0.78F, 3.75F, -0.45F, 1.56F, 1.2F, 1.2F), PartPose.offsetAndRotation(0.75F, 4.5F, 0F, 1.3F, 0F, 0F));
        p150.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 3.45F, 1.5F).texOffs(96, 72).addBox(-0.78F, 3F, -0.45F, 1.56F, 1.2F, 1.2F), PartPose.offsetAndRotation(2.25F, 4.5F, 0F, 1.3F, 0F, 0F));
        p150.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 3.3F, 1.5F).texOffs(96, 72).addBox(-0.78F, 2.85F, -0.45F, 1.56F, 1.125F, 1.2F), PartPose.offsetAndRotation(3.15F, 1.35F, 0F, 0.98F, 0F, -0.6F));
        PartDefinition p156 = p125.addOrReplaceChild("browR", CubeListBuilder.create(), PartPose.offsetAndRotation(5.4F, -10.4F, -1F, 0F, -1.571F, 0F));
        PartDefinition p157 = p156.addOrReplaceChild("t", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p158 = p157.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-3.15F, 0F, -1.05F, 6.3F, 4.8F, 2.1F), PartPose.offsetAndRotation(0F, 0F, 0F, -1.371F, 0F, 0F));
        p158.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 3.45F, 1.5F).texOffs(96, 72).addBox(-0.78F, 3F, -0.45F, 1.56F, 1.2F, 1.2F), PartPose.offsetAndRotation(-2.25F, 4.5F, 0F, 1.3F, 0F, 0F));
        p158.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 4.2F, 1.5F).texOffs(96, 72).addBox(-0.78F, 3.75F, -0.45F, 1.56F, 1.2F, 1.2F), PartPose.offsetAndRotation(-0.75F, 4.5F, 0F, 1.3F, 0F, 0F));
        p158.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 4.2F, 1.5F).texOffs(96, 72).addBox(-0.78F, 3.75F, -0.45F, 1.56F, 1.2F, 1.2F), PartPose.offsetAndRotation(0.75F, 4.5F, 0F, 1.3F, 0F, 0F));
        p158.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 3.45F, 1.5F).texOffs(96, 72).addBox(-0.78F, 3F, -0.45F, 1.56F, 1.2F, 1.2F), PartPose.offsetAndRotation(2.25F, 4.5F, 0F, 1.3F, 0F, 0F));
        p158.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.75F, 0F, -0.75F, 1.5F, 3.3F, 1.5F).texOffs(96, 72).addBox(-0.78F, 2.85F, -0.45F, 1.56F, 1.125F, 1.2F), PartPose.offsetAndRotation(3.15F, 1.35F, 0F, 0.98F, 0F, -0.6F));
        PartDefinition p164 = p125.addOrReplaceChild("cheekL", CubeListBuilder.create(), PartPose.offsetAndRotation(-5.6F, -9F, -1F, 0F, 1.571F, 0F));
        PartDefinition p165 = p164.addOrReplaceChild("t", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -0.2F));
        PartDefinition p166 = p165.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-3.36F, 0F, -1.12F, 6.72F, 5.12F, 2.24F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p166.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 3.68F, 1.6F).texOffs(96, 72).addBox(-0.832F, 3.2F, -0.48F, 1.664F, 1.28F, 1.28F), PartPose.offsetAndRotation(-2.4F, 4.8F, 0F, 1.1F, 0F, 0F));
        p166.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 4.48F, 1.6F).texOffs(96, 72).addBox(-0.832F, 4F, -0.48F, 1.664F, 1.28F, 1.28F), PartPose.offsetAndRotation(-0.8F, 4.8F, 0F, 1.1F, 0F, 0F));
        p166.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 4.48F, 1.6F).texOffs(96, 72).addBox(-0.832F, 4F, -0.48F, 1.664F, 1.28F, 1.28F), PartPose.offsetAndRotation(0.8F, 4.8F, 0F, 1.1F, 0F, 0F));
        p166.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 3.68F, 1.6F).texOffs(96, 72).addBox(-0.832F, 3.2F, -0.48F, 1.664F, 1.28F, 1.28F), PartPose.offsetAndRotation(2.4F, 4.8F, 0F, 1.1F, 0F, 0F));
        p166.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 3.52F, 1.6F).texOffs(96, 72).addBox(-0.832F, 3.04F, -0.48F, 1.664F, 1.2F, 1.28F), PartPose.offsetAndRotation(3.36F, 1.44F, 0F, 0.86F, 0F, -0.6F));
        PartDefinition p172 = p125.addOrReplaceChild("cheekR", CubeListBuilder.create(), PartPose.offsetAndRotation(5.6F, -8F, -1F, 0F, -1.571F, 0F));
        PartDefinition p173 = p172.addOrReplaceChild("t", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.3F));
        PartDefinition p174 = p173.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-3.36F, 0F, -1.12F, 6.72F, 5.12F, 2.24F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p174.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 3.68F, 1.6F).texOffs(96, 72).addBox(-0.832F, 3.2F, -0.48F, 1.664F, 1.28F, 1.28F), PartPose.offsetAndRotation(-2.4F, 4.8F, 0F, 1.1F, 0F, 0F));
        p174.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 4.48F, 1.6F).texOffs(96, 72).addBox(-0.832F, 4F, -0.48F, 1.664F, 1.28F, 1.28F), PartPose.offsetAndRotation(-0.8F, 4.8F, 0F, 1.1F, 0F, 0F));
        p174.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 4.48F, 1.6F).texOffs(96, 72).addBox(-0.832F, 4F, -0.48F, 1.664F, 1.28F, 1.28F), PartPose.offsetAndRotation(0.8F, 4.8F, 0F, 1.1F, 0F, 0F));
        p174.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 3.68F, 1.6F).texOffs(96, 72).addBox(-0.832F, 3.2F, -0.48F, 1.664F, 1.28F, 1.28F), PartPose.offsetAndRotation(2.4F, 4.8F, 0F, 1.1F, 0F, 0F));
        p174.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.8F, 0F, -0.8F, 1.6F, 3.52F, 1.6F).texOffs(96, 72).addBox(-0.832F, 3.04F, -0.48F, 1.664F, 1.2F, 1.28F), PartPose.offsetAndRotation(3.36F, 1.44F, 0F, 0.86F, 0F, -0.6F));
        PartDefinition p180 = p125.addOrReplaceChild("nape", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -9.5F, 5.6F, 0F, 3.142F, 0F));
        PartDefinition p181 = p180.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-3.99F, 0F, -1.33F, 7.98F, 6.08F, 2.66F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p181.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.95F, 0F, -0.95F, 1.9F, 4.37F, 1.9F).texOffs(96, 72).addBox(-0.988F, 3.8F, -0.57F, 1.976F, 1.52F, 1.52F), PartPose.offsetAndRotation(-2.85F, 5.7F, 0F, 1.1F, 0F, 0F));
        p181.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.95F, 0F, -0.95F, 1.9F, 5.32F, 1.9F).texOffs(96, 72).addBox(-0.988F, 4.75F, -0.57F, 1.976F, 1.52F, 1.52F), PartPose.offsetAndRotation(-0.95F, 5.7F, 0F, 1.1F, 0F, 0F));
        p181.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.95F, 0F, -0.95F, 1.9F, 5.32F, 1.9F).texOffs(96, 72).addBox(-0.988F, 4.75F, -0.57F, 1.976F, 1.52F, 1.52F), PartPose.offsetAndRotation(0.95F, 5.7F, 0F, 1.1F, 0F, 0F));
        p181.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.95F, 0F, -0.95F, 1.9F, 4.37F, 1.9F).texOffs(96, 72).addBox(-0.988F, 3.8F, -0.57F, 1.976F, 1.52F, 1.52F), PartPose.offsetAndRotation(2.85F, 5.7F, 0F, 1.1F, 0F, 0F));
        p181.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.95F, 0F, -0.95F, 1.9F, 4.18F, 1.9F).texOffs(96, 72).addBox(-0.988F, 3.61F, -0.57F, 1.976F, 1.425F, 1.52F), PartPose.offsetAndRotation(3.99F, 1.71F, 0F, 0.86F, 0F, -0.6F));
        PartDefinition p187 = p1.addOrReplaceChild("reach0", CubeListBuilder.create().texOffs(0, 64).addBox(-2.2F, 0F, -2.2F, 4.4F, 11F, 4.4F), PartPose.offsetAndRotation(-9F, -29F, -3F, -2.6F, 0.2F, -0.7F));
        PartDefinition p188 = p187.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 64).addBox(-1.98F, 0F, -1.98F, 3.96F, 11F, 3.96F), PartPose.offsetAndRotation(0F, 10.5F, 0F, -0.9F, 0F, 0F));
        PartDefinition p189 = p188.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.835F, 0F, -0.945F, 5.67F, 4.32F, 1.89F), PartPose.offsetAndRotation(0F, 10.5F, 0F, 0F, 0F, 0F));
        p189.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.105F, 1.35F).texOffs(96, 72).addBox(-0.702F, 2.7F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(-2.025F, 4.05F, 0F, 0.45F, 0F, -0.3F));
        p189.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.78F, 1.35F).texOffs(96, 72).addBox(-0.702F, 3.375F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(-0.675F, 4.05F, 0F, 0.45F, 0F, -0.1F));
        p189.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.78F, 1.35F).texOffs(96, 72).addBox(-0.702F, 3.375F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(0.675F, 4.05F, 0F, 0.45F, 0F, 0.1F));
        p189.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.105F, 1.35F).texOffs(96, 72).addBox(-0.702F, 2.7F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(2.025F, 4.05F, 0F, 0.45F, 0F, 0.3F));
        p189.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 2.97F, 1.35F).texOffs(96, 72).addBox(-0.702F, 2.565F, -0.405F, 1.404F, 1.013F, 1.08F), PartPose.offsetAndRotation(2.835F, 1.215F, 0F, 0.47F, 0F, -0.6F));
        PartDefinition p195 = p1.addOrReplaceChild("reach1", CubeListBuilder.create().texOffs(0, 64).addBox(-2.2F, 0F, -2.2F, 4.4F, 11F, 4.4F), PartPose.offsetAndRotation(9F, -29F, -3F, -2.502F, -0.2F, 0.6F));
        PartDefinition p196 = p195.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 64).addBox(-1.98F, 0F, -1.98F, 3.96F, 11F, 3.96F), PartPose.offsetAndRotation(0F, 10.5F, 0F, -1F, 0F, 0F));
        PartDefinition p197 = p196.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.835F, 0F, -0.945F, 5.67F, 4.32F, 1.89F), PartPose.offsetAndRotation(0F, 10.5F, 0F, 0F, 0F, 0F));
        p197.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.105F, 1.35F).texOffs(96, 72).addBox(-0.702F, 2.7F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(-2.025F, 4.05F, 0F, 0.45F, 0F, -0.3F));
        p197.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.78F, 1.35F).texOffs(96, 72).addBox(-0.702F, 3.375F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(-0.675F, 4.05F, 0F, 0.45F, 0F, -0.1F));
        p197.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.78F, 1.35F).texOffs(96, 72).addBox(-0.702F, 3.375F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(0.675F, 4.05F, 0F, 0.45F, 0F, 0.1F));
        p197.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.105F, 1.35F).texOffs(96, 72).addBox(-0.702F, 2.7F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(2.025F, 4.05F, 0F, 0.45F, 0F, 0.3F));
        p197.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 2.97F, 1.35F).texOffs(96, 72).addBox(-0.702F, 2.565F, -0.405F, 1.404F, 1.013F, 1.08F), PartPose.offsetAndRotation(2.835F, 1.215F, 0F, 0.47F, 0F, -0.6F));
        PartDefinition p203 = p1.addOrReplaceChild("reach2", CubeListBuilder.create().texOffs(0, 64).addBox(-2F, 0F, -2F, 4F, 10F, 4F), PartPose.offsetAndRotation(-5F, -32F, 3F, -2.951F, 0F, -0.25F));
        PartDefinition p204 = p203.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 64).addBox(-1.8F, 0F, -1.8F, 3.6F, 10F, 3.6F), PartPose.offsetAndRotation(0F, 9.5F, 0F, 1F, 0F, 0F));
        PartDefinition p205 = p204.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.625F, 0F, -0.875F, 5.25F, 4F, 1.75F), PartPose.offsetAndRotation(0F, 9.5F, 0F, 0F, 0F, 0F));
        p205.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.875F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.5F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(-1.875F, 3.75F, 0F, 0.45F, 0F, -0.3F));
        p205.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 3.5F, 1.25F).texOffs(96, 72).addBox(-0.65F, 3.125F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(-0.625F, 3.75F, 0F, 0.45F, 0F, -0.1F));
        p205.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 3.5F, 1.25F).texOffs(96, 72).addBox(-0.65F, 3.125F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(0.625F, 3.75F, 0F, 0.45F, 0F, 0.1F));
        p205.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.875F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.5F, -0.375F, 1.3F, 1F, 1F), PartPose.offsetAndRotation(1.875F, 3.75F, 0F, 0.45F, 0F, 0.3F));
        p205.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.625F, 0F, -0.625F, 1.25F, 2.75F, 1.25F).texOffs(96, 72).addBox(-0.65F, 2.375F, -0.375F, 1.3F, 0.938F, 1F), PartPose.offsetAndRotation(2.625F, 1.125F, 0F, 0.47F, 0F, -0.6F));
        PartDefinition p211 = p1.addOrReplaceChild("reach3", CubeListBuilder.create().texOffs(0, 64).addBox(-2F, 0F, -2F, 4F, 10F, 4F), PartPose.offsetAndRotation(6F, -31F, 3F, -2.985F, 0F, 0.35F));
        PartDefinition p212 = p211.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 64).addBox(-1.8F, 0F, -1.8F, 3.6F, 9F, 3.6F), PartPose.offsetAndRotation(0F, 9.5F, 0F, 0.9F, 0F, 0F));
        PartDefinition p213 = p212.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.52F, 0F, -0.84F, 5.04F, 3.84F, 1.68F), PartPose.offsetAndRotation(0F, 8.5F, 0F, 0F, 0F, 0F));
        p213.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.6F, 0F, -0.6F, 1.2F, 2.76F, 1.2F).texOffs(96, 72).addBox(-0.624F, 2.4F, -0.36F, 1.248F, 0.96F, 0.96F), PartPose.offsetAndRotation(-1.8F, 3.6F, 0F, 0.45F, 0F, -0.3F));
        p213.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.6F, 0F, -0.6F, 1.2F, 3.36F, 1.2F).texOffs(96, 72).addBox(-0.624F, 3F, -0.36F, 1.248F, 0.96F, 0.96F), PartPose.offsetAndRotation(-0.6F, 3.6F, 0F, 0.45F, 0F, -0.1F));
        p213.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.6F, 0F, -0.6F, 1.2F, 3.36F, 1.2F).texOffs(96, 72).addBox(-0.624F, 3F, -0.36F, 1.248F, 0.96F, 0.96F), PartPose.offsetAndRotation(0.6F, 3.6F, 0F, 0.45F, 0F, 0.1F));
        p213.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.6F, 0F, -0.6F, 1.2F, 2.76F, 1.2F).texOffs(96, 72).addBox(-0.624F, 2.4F, -0.36F, 1.248F, 0.96F, 0.96F), PartPose.offsetAndRotation(1.8F, 3.6F, 0F, 0.45F, 0F, 0.3F));
        p213.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.6F, 0F, -0.6F, 1.2F, 2.64F, 1.2F).texOffs(96, 72).addBox(-0.624F, 2.28F, -0.36F, 1.248F, 0.9F, 0.96F), PartPose.offsetAndRotation(2.52F, 1.08F, 0F, 0.47F, 0F, -0.6F));
        PartDefinition p219 = p1.addOrReplaceChild("reach4", CubeListBuilder.create().texOffs(0, 64).addBox(-2.2F, 0F, -2.2F, 4.4F, 12F, 4.4F), PartPose.offsetAndRotation(-13F, -22F, -4F, -1.501F, 0.4F, -0.9F));
        PartDefinition p220 = p219.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 64).addBox(-1.98F, 0F, -1.98F, 3.96F, 11F, 3.96F), PartPose.offsetAndRotation(0F, 11.5F, 0F, -0.7F, 0F, 0F));
        PartDefinition p221 = p220.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.835F, 0F, -0.945F, 5.67F, 4.32F, 1.89F), PartPose.offsetAndRotation(0F, 10.5F, 0F, 0F, 0F, 0F));
        p221.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.105F, 1.35F).texOffs(96, 72).addBox(-0.702F, 2.7F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(-2.025F, 4.05F, 0F, 0.45F, 0F, -0.3F));
        p221.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.78F, 1.35F).texOffs(96, 72).addBox(-0.702F, 3.375F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(-0.675F, 4.05F, 0F, 0.45F, 0F, -0.1F));
        p221.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.78F, 1.35F).texOffs(96, 72).addBox(-0.702F, 3.375F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(0.675F, 4.05F, 0F, 0.45F, 0F, 0.1F));
        p221.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 3.105F, 1.35F).texOffs(96, 72).addBox(-0.702F, 2.7F, -0.405F, 1.404F, 1.08F, 1.08F), PartPose.offsetAndRotation(2.025F, 4.05F, 0F, 0.45F, 0F, 0.3F));
        p221.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.675F, 0F, -0.675F, 1.35F, 2.97F, 1.35F).texOffs(96, 72).addBox(-0.702F, 2.565F, -0.405F, 1.404F, 1.013F, 1.08F), PartPose.offsetAndRotation(2.835F, 1.215F, 0F, 0.47F, 0F, -0.6F));
        PartDefinition p227 = p1.addOrReplaceChild("reach5", CubeListBuilder.create().texOffs(0, 64).addBox(-2.3F, 0F, -2.3F, 4.6F, 13F, 4.6F), PartPose.offsetAndRotation(14F, -20F, -3F, -0.84F, -0.3F, 1.4F));
        PartDefinition p228 = p227.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 64).addBox(-2.07F, 0F, -2.07F, 4.14F, 12F, 4.14F), PartPose.offsetAndRotation(0F, 12.5F, 0F, -0.5F, 0F, 0F));
        PartDefinition p229 = p228.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.94F, 0F, -0.98F, 5.88F, 4.48F, 1.96F), PartPose.offsetAndRotation(0F, 11.5F, 0F, 0F, 0F, 0F));
        p229.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.22F, 1.4F).texOffs(96, 72).addBox(-0.728F, 2.8F, -0.42F, 1.456F, 1.12F, 1.12F), PartPose.offsetAndRotation(-2.1F, 4.2F, 0F, 0.45F, 0F, -0.3F));
        p229.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.92F, 1.4F).texOffs(96, 72).addBox(-0.728F, 3.5F, -0.42F, 1.456F, 1.12F, 1.12F), PartPose.offsetAndRotation(-0.7F, 4.2F, 0F, 0.45F, 0F, -0.1F));
        p229.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.92F, 1.4F).texOffs(96, 72).addBox(-0.728F, 3.5F, -0.42F, 1.456F, 1.12F, 1.12F), PartPose.offsetAndRotation(0.7F, 4.2F, 0F, 0.45F, 0F, 0.1F));
        p229.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.22F, 1.4F).texOffs(96, 72).addBox(-0.728F, 2.8F, -0.42F, 1.456F, 1.12F, 1.12F), PartPose.offsetAndRotation(2.1F, 4.2F, 0F, 0.45F, 0F, 0.3F));
        p229.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.08F, 1.4F).texOffs(96, 72).addBox(-0.728F, 2.66F, -0.42F, 1.456F, 1.05F, 1.12F), PartPose.offsetAndRotation(2.94F, 1.26F, 0F, 0.47F, 0F, -0.6F));
        PartDefinition p235 = p1.addOrReplaceChild("reach6", CubeListBuilder.create().texOffs(0, 64).addBox(-2.1F, 0F, -2.1F, 4.2F, 10F, 4.2F), PartPose.offsetAndRotation(-15F, -12F, -2F, -0.74F, 0.3F, -1.2F));
        PartDefinition p236 = p235.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 64).addBox(-1.89F, 0F, -1.89F, 3.78F, 9F, 3.78F), PartPose.offsetAndRotation(0F, 9.5F, 0F, -0.9F, 0F, 0F));
        PartDefinition p237 = p236.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.73F, 0F, -0.91F, 5.46F, 4.16F, 1.82F), PartPose.offsetAndRotation(0F, 8.5F, 0F, 0F, 0F, 0F));
        p237.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 2.99F, 1.3F).texOffs(96, 72).addBox(-0.676F, 2.6F, -0.39F, 1.352F, 1.04F, 1.04F), PartPose.offsetAndRotation(-1.95F, 3.9F, 0F, 0.45F, 0F, -0.3F));
        p237.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 3.64F, 1.3F).texOffs(96, 72).addBox(-0.676F, 3.25F, -0.39F, 1.352F, 1.04F, 1.04F), PartPose.offsetAndRotation(-0.65F, 3.9F, 0F, 0.45F, 0F, -0.1F));
        p237.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 3.64F, 1.3F).texOffs(96, 72).addBox(-0.676F, 3.25F, -0.39F, 1.352F, 1.04F, 1.04F), PartPose.offsetAndRotation(0.65F, 3.9F, 0F, 0.45F, 0F, 0.1F));
        p237.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 2.99F, 1.3F).texOffs(96, 72).addBox(-0.676F, 2.6F, -0.39F, 1.352F, 1.04F, 1.04F), PartPose.offsetAndRotation(1.95F, 3.9F, 0F, 0.45F, 0F, 0.3F));
        p237.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 2.86F, 1.3F).texOffs(96, 72).addBox(-0.676F, 2.47F, -0.39F, 1.352F, 0.975F, 1.04F), PartPose.offsetAndRotation(2.73F, 1.17F, 0F, 0.47F, 0F, -0.6F));
        PartDefinition p243 = p1.addOrReplaceChild("reach7", CubeListBuilder.create().texOffs(0, 64).addBox(-2.2F, 0F, -2.2F, 4.4F, 12F, 4.4F), PartPose.offsetAndRotation(15F, -10F, 2F, -0.324F, 0F, 1.6F));
        PartDefinition p244 = p243.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 64).addBox(-1.98F, 0F, -1.98F, 3.96F, 11F, 3.96F), PartPose.offsetAndRotation(0F, 11.5F, 0F, -0.6F, 0F, 0F));
        PartDefinition p245 = p244.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.94F, 0F, -0.98F, 5.88F, 4.48F, 1.96F), PartPose.offsetAndRotation(0F, 10.5F, 0F, 0F, 0F, 0F));
        p245.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.22F, 1.4F).texOffs(96, 72).addBox(-0.728F, 2.8F, -0.42F, 1.456F, 1.12F, 1.12F), PartPose.offsetAndRotation(-2.1F, 4.2F, 0F, 0.45F, 0F, -0.3F));
        p245.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.92F, 1.4F).texOffs(96, 72).addBox(-0.728F, 3.5F, -0.42F, 1.456F, 1.12F, 1.12F), PartPose.offsetAndRotation(-0.7F, 4.2F, 0F, 0.45F, 0F, -0.1F));
        p245.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.92F, 1.4F).texOffs(96, 72).addBox(-0.728F, 3.5F, -0.42F, 1.456F, 1.12F, 1.12F), PartPose.offsetAndRotation(0.7F, 4.2F, 0F, 0.45F, 0F, 0.1F));
        p245.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.22F, 1.4F).texOffs(96, 72).addBox(-0.728F, 2.8F, -0.42F, 1.456F, 1.12F, 1.12F), PartPose.offsetAndRotation(2.1F, 4.2F, 0F, 0.45F, 0F, 0.3F));
        p245.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.7F, 0F, -0.7F, 1.4F, 3.08F, 1.4F).texOffs(96, 72).addBox(-0.728F, 2.66F, -0.42F, 1.456F, 1.05F, 1.12F), PartPose.offsetAndRotation(2.94F, 1.26F, 0F, 0.47F, 0F, -0.6F));
        PartDefinition p251 = root.addOrReplaceChild("leg0", CubeListBuilder.create().texOffs(0, 64).addBox(-4.5F, -1F, -4.5F, 9F, 9F, 9F), PartPose.offsetAndRotation(-10F, 6.5F, -7F, 0.55F, 0.5F, 0F));
        PartDefinition p252 = p251.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 64).addBox(-3.6F, 0F, -3.6F, 7.2F, 8.5F, 7.2F), PartPose.offsetAndRotation(0F, 7F, 0F, -0.75F, 0F, 0F));
        PartDefinition p253 = p252.addOrReplaceChild("foot", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 7.5F, 0F, 0F, 0F, 0F));
        PartDefinition p254 = p253.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-4.2F, 0F, -1.4F, 8.4F, 6.4F, 2.8F), PartPose.offsetAndRotation(0F, 0F, 0F, -1.371F, 0F, 0F));
        p254.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.6F, 2F).texOffs(96, 72).addBox(-1.04F, 4F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(-3F, 6F, 0F, 0.55F, 0F, -0.27F));
        p254.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 5.6F, 2F).texOffs(96, 72).addBox(-1.04F, 5F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(-1F, 6F, 0F, 0.55F, 0F, -0.09F));
        p254.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 5.6F, 2F).texOffs(96, 72).addBox(-1.04F, 5F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(1F, 6F, 0F, 0.55F, 0F, 0.09F));
        p254.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.6F, 2F).texOffs(96, 72).addBox(-1.04F, 4F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(3F, 6F, 0F, 0.55F, 0F, 0.27F));
        p254.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.4F, 2F).texOffs(96, 72).addBox(-1.04F, 3.8F, -0.6F, 2.08F, 1.5F, 1.6F), PartPose.offsetAndRotation(4.2F, 1.8F, 0F, 0.53F, 0F, -0.6F));
        PartDefinition p260 = root.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(0, 64).addBox(-4.5F, -1F, -4.5F, 9F, 9F, 9F), PartPose.offsetAndRotation(10F, 6.5F, -7F, 0.55F, -0.5F, 0F));
        PartDefinition p261 = p260.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 64).addBox(-3.6F, 0F, -3.6F, 7.2F, 8.5F, 7.2F), PartPose.offsetAndRotation(0F, 7F, 0F, -0.75F, 0F, 0F));
        PartDefinition p262 = p261.addOrReplaceChild("foot", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 7.5F, 0F, 0F, 0F, 0F));
        PartDefinition p263 = p262.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-4.2F, 0F, -1.4F, 8.4F, 6.4F, 2.8F), PartPose.offsetAndRotation(0F, 0F, 0F, -1.371F, 0F, 0F));
        p263.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.6F, 2F).texOffs(96, 72).addBox(-1.04F, 4F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(-3F, 6F, 0F, 0.55F, 0F, -0.27F));
        p263.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 5.6F, 2F).texOffs(96, 72).addBox(-1.04F, 5F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(-1F, 6F, 0F, 0.55F, 0F, -0.09F));
        p263.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 5.6F, 2F).texOffs(96, 72).addBox(-1.04F, 5F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(1F, 6F, 0F, 0.55F, 0F, 0.09F));
        p263.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.6F, 2F).texOffs(96, 72).addBox(-1.04F, 4F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(3F, 6F, 0F, 0.55F, 0F, 0.27F));
        p263.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.4F, 2F).texOffs(96, 72).addBox(-1.04F, 3.8F, -0.6F, 2.08F, 1.5F, 1.6F), PartPose.offsetAndRotation(4.2F, 1.8F, 0F, 0.53F, 0F, -0.6F));
        PartDefinition p269 = root.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(0, 64).addBox(-4.5F, -1F, -4.5F, 9F, 9F, 9F), PartPose.offsetAndRotation(-10F, 6.5F, 8F, 0.55F, 2.4F, 0F));
        PartDefinition p270 = p269.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 64).addBox(-3.6F, 0F, -3.6F, 7.2F, 8.5F, 7.2F), PartPose.offsetAndRotation(0F, 7F, 0F, -0.75F, 0F, 0F));
        PartDefinition p271 = p270.addOrReplaceChild("foot", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 7.5F, 0F, 0F, 0F, 0F));
        PartDefinition p272 = p271.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-4.2F, 0F, -1.4F, 8.4F, 6.4F, 2.8F), PartPose.offsetAndRotation(0F, 0F, 0F, -1.371F, 0F, 0F));
        p272.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.6F, 2F).texOffs(96, 72).addBox(-1.04F, 4F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(-3F, 6F, 0F, 0.55F, 0F, -0.27F));
        p272.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 5.6F, 2F).texOffs(96, 72).addBox(-1.04F, 5F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(-1F, 6F, 0F, 0.55F, 0F, -0.09F));
        p272.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 5.6F, 2F).texOffs(96, 72).addBox(-1.04F, 5F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(1F, 6F, 0F, 0.55F, 0F, 0.09F));
        p272.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.6F, 2F).texOffs(96, 72).addBox(-1.04F, 4F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(3F, 6F, 0F, 0.55F, 0F, 0.27F));
        p272.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.4F, 2F).texOffs(96, 72).addBox(-1.04F, 3.8F, -0.6F, 2.08F, 1.5F, 1.6F), PartPose.offsetAndRotation(4.2F, 1.8F, 0F, 0.53F, 0F, -0.6F));
        PartDefinition p278 = root.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(0, 64).addBox(-4.5F, -1F, -4.5F, 9F, 9F, 9F), PartPose.offsetAndRotation(10F, 6.5F, 8F, 0.55F, -2.4F, 0F));
        PartDefinition p279 = p278.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 64).addBox(-3.6F, 0F, -3.6F, 7.2F, 8.5F, 7.2F), PartPose.offsetAndRotation(0F, 7F, 0F, -0.75F, 0F, 0F));
        PartDefinition p280 = p279.addOrReplaceChild("foot", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 7.5F, 0F, 0F, 0F, 0F));
        PartDefinition p281 = p280.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-4.2F, 0F, -1.4F, 8.4F, 6.4F, 2.8F), PartPose.offsetAndRotation(0F, 0F, 0F, -1.371F, 0F, 0F));
        p281.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.6F, 2F).texOffs(96, 72).addBox(-1.04F, 4F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(-3F, 6F, 0F, 0.55F, 0F, -0.27F));
        p281.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 5.6F, 2F).texOffs(96, 72).addBox(-1.04F, 5F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(-1F, 6F, 0F, 0.55F, 0F, -0.09F));
        p281.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 5.6F, 2F).texOffs(96, 72).addBox(-1.04F, 5F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(1F, 6F, 0F, 0.55F, 0F, 0.09F));
        p281.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.6F, 2F).texOffs(96, 72).addBox(-1.04F, 4F, -0.6F, 2.08F, 1.6F, 1.6F), PartPose.offsetAndRotation(3F, 6F, 0F, 0.55F, 0F, 0.27F));
        p281.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-1F, 0F, -1F, 2F, 4.4F, 2F).texOffs(96, 72).addBox(-1.04F, 3.8F, -0.6F, 2.08F, 1.5F, 1.6F), PartPose.offsetAndRotation(4.2F, 1.8F, 0F, 0.53F, 0F, -0.6F));
        root.addOrReplaceChild("reach_arm", CubeListBuilder.create().texOffs(0, 64).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 16F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p288 = root.addOrReplaceChild("reach_hand", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 1.571F, 0F, 0F));
        PartDefinition p289 = p288.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(64, 72).addBox(-2.73F, 0F, -0.91F, 5.46F, 4.16F, 1.82F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p289.addOrReplaceChild("f0", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 2.99F, 1.3F).texOffs(96, 72).addBox(-0.676F, 2.6F, -0.39F, 1.352F, 1.04F, 1.04F), PartPose.offsetAndRotation(-1.95F, 3.9F, 0F, 0.4F, 0F, -0.225F));
        p289.addOrReplaceChild("f1", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 3.64F, 1.3F).texOffs(96, 72).addBox(-0.676F, 3.25F, -0.39F, 1.352F, 1.04F, 1.04F), PartPose.offsetAndRotation(-0.65F, 3.9F, 0F, 0.4F, 0F, -0.075F));
        p289.addOrReplaceChild("f2", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 3.64F, 1.3F).texOffs(96, 72).addBox(-0.676F, 3.25F, -0.39F, 1.352F, 1.04F, 1.04F), PartPose.offsetAndRotation(0.65F, 3.9F, 0F, 0.4F, 0F, 0.075F));
        p289.addOrReplaceChild("f3", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 2.99F, 1.3F).texOffs(96, 72).addBox(-0.676F, 2.6F, -0.39F, 1.352F, 1.04F, 1.04F), PartPose.offsetAndRotation(1.95F, 3.9F, 0F, 0.4F, 0F, 0.225F));
        p289.addOrReplaceChild("thumb", CubeListBuilder.create().texOffs(64, 72).addBox(-0.65F, 0F, -0.65F, 1.3F, 2.86F, 1.3F).texOffs(96, 72).addBox(-0.676F, 2.47F, -0.39F, 1.352F, 0.975F, 1.04F), PartPose.offsetAndRotation(2.73F, 1.17F, 0F, 0.44F, 0F, -0.6F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(HandDemonEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        int attack = e.attack();
        float at = e.attackTick();
        float walk = Math.min(1.0F, limbSwingAmount * 1.4F);
        float stride = limbSwing * 0.5F;
        // the whole mass heaves with its breath and wobbles as it waddles
        body.y = bodyY + Mth.sin(ageInTicks * 0.07F) * 0.5F + Math.abs(Mth.sin(stride)) * walk * 1.2F;
        body.zRot = Mth.sin(stride) * 0.06F * walk;
        body.xRot = 0.05F + Mth.sin(ageInTicks * 0.05F) * 0.02F;
        for (int i = 0; i < LEGS; i++) {
            float sw = Mth.sin(stride + (i == 0 || i == 3 ? 0 : (float) Math.PI)) * 0.35F * walk;
            leg[i].xRot = legX[i] + sw;
            shin[i].xRot = shinX[i] - Math.max(0, sw) * 0.5F;
        }
        // the coils of arm slide and squirm around the body
        for (int i = 0; i < RINGS; i++) ring[i].yRot = Mth.sin(ageInTicks * 0.04F + i * 1.3F) * 0.06F;
        // the reaching arms writhe and claw at the air
        for (int i = 0; i < REACH; i++) {
            float w = Mth.sin(ageInTicks * 0.11F + i * 1.7F);
            reach[i].xRot = reachX[i] + w * 0.18F;
            reach[i].zRot = reachZ[i] + Mth.cos(ageInTicks * 0.08F + i) * 0.1F;
            reachFore[i].xRot = foreX[i] - w * 0.25F;
        }
        // the head turns after its prey, peering out between its fingers; the hands clamped over its mouth
        // tighten and twitch (harder when it attacks, as if something behind them were chewing)
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        head.xRot = 0.15F + headPitch * Mth.DEG_TO_RAD * 0.4F;
        float chew = attack != HandDemonEntity.NONE ? Mth.sin(at * 0.9F) * 0.06F : Mth.sin(ageInTicks * 0.15F) * 0.015F;
        muzzleL.zRot = muzzleLZ + chew;
        muzzleR.zRot = muzzleRZ - chew;

        switch (attack) {
            case HandDemonEntity.FIST -> {
                // every arm rises and knots overhead, then comes down
                float raise = at < HandDemonEntity.FIST_SLAM ? Math.min(1, at / 12F) : Math.max(0, 1 - (at - HandDemonEntity.FIST_SLAM) / 4F);
                for (int i = 0; i < REACH; i++) {
                    reach[i].xRot = Mth.lerp(raise, reach[i].xRot, -3.0F);
                    reach[i].zRot = Mth.lerp(raise, reach[i].zRot, reachZ[i] * 0.3F);
                }
            }
            case HandDemonEntity.SWEEP -> {
                for (int i = 0; i < REACH; i++) {
                    reach[i].xRot = -1.5F;
                    reach[i].zRot = reachZ[i] + (i % 2 == 0 ? 1 : -1) * Mth.sin(at * 0.9F) * 0.9F;
                }
            }
            case HandDemonEntity.BURROW -> {
                if (at < HandDemonEntity.BURROW_ERUPT + 6) {
                    float dig = Math.min(1, at / 10F);
                    for (int i = 4; i < REACH; i++) {   // the low arms plunge into the earth
                        reach[i].xRot = Mth.lerp(dig, reach[i].xRot, 0.4F);
                        reach[i].zRot = Mth.lerp(dig, reach[i].zRot, reachZ[i] * 0.4F);
                    }
                    body.y += dig * 2.0F;
                }
            }
            case HandDemonEntity.GRAB, HandDemonEntity.BARRAGE -> {
                // it rears back as it throws its arms (the thrown arms are drawn by the renderer)
                float k = Math.min(1, at / 8F);
                body.xRot -= k * 0.12F;
                for (int i = 0; i < 4; i++) reach[i].xRot = Mth.lerp(k, reach[i].xRot, -1.8F);
            }
            default -> {
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        body.render(pose, buffer, light, overlay, r, g, b, a);
        for (ModelPart l : leg) l.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
