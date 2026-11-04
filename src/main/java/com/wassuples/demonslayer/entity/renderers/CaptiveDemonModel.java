package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CaptiveDemonEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
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
 * Captive demons' bodies, one model per build, all on the 64x64 player-skin layout (both layers) so every skin fits
 * every build:
 * <ul>
 *   <li><b>lean</b>: gaunt, narrow chest, long thin arms (slim) and legs, stooped, reaching with its claws;</li>
 *   <li><b>ordinary</b>: a starved human frame, slightly hunched;</li>
 *   <li><b>brute</b>: swollen chest and shoulders, thick arms hanging low, heavily hunched.</li>
 * </ul>
 * Shapes come from per-axis cube deformation (the UVs stay those of the player skin). Horns are optional head parts
 * textured from the skin's unused 8x8 corner at (0,0); which ones show depends on the variant ({@link #hornStyle}).
 */
public class CaptiveDemonModel extends PlayerModel<CaptiveDemonEntity> {
    public static final ModelLayerLocation[] LAYERS = {
            new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "captive_demon_lean"), "main"),
            new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "captive_demon"), "main"),
            new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "captive_demon_brute"), "main")};
    public static final int HORNS_NONE = 0, HORNS_PAIR = 1, HORNS_SINGLE = 2, HORNS_NUBS = 3, HORNS_BIG = 4;

    /** {head grow, body grow x/y/z, arm grow x/y/z, leg grow x/y/z, arm pivot x, leg pivot x, hunch (radians)}. */
    private static final float[][] SHAPES = {
            {-0.25F, -0.6F, 0.4F, -0.5F, -0.15F, 1.6F, -0.3F, -0.35F, 0.8F, -0.35F, 4.2F, 1.6F, 0.32F},
            {0.0F, 0.15F, 0.0F, 0.1F, 0.1F, 0.3F, 0.1F, 0.0F, 0.0F, 0.0F, 5.2F, 1.9F, 0.2F},
            {0.35F, 1.8F, 0.6F, 1.3F, 1.0F, 0.8F, 1.0F, 0.9F, 0.3F, 0.9F, 7.0F, 2.7F, 0.42F}};

    private final int build;
    private final float hunch, legY, armY;
    private final ModelPart hornL, hornR, hornC, nubL, nubR, tipL, tipR;

    public CaptiveDemonModel(ModelPart root, int build) {
        super(root, build == CaptiveDemonEntity.LEAN);
        this.build = build;
        this.hunch = SHAPES[build][12];
        this.legY = 12.0F - SHAPES[build][8];
        this.armY = build == CaptiveDemonEntity.LEAN ? 2.5F : 2.0F;
        this.hornL = this.head.getChild("horn_l");
        this.hornR = this.head.getChild("horn_r");
        this.hornC = this.head.getChild("horn_c");
        this.nubL = this.head.getChild("nub_l");
        this.nubR = this.head.getChild("nub_r");
        this.tipL = this.hornL.getChild("tip");
        this.tipR = this.hornR.getChild("tip");
    }

    public static LayerDefinition create(int build) {
        float[] s = SHAPES[build];
        boolean slim = build == CaptiveDemonEntity.LEAN;
        int armW = slim ? 3 : 4;
        float armY = slim ? 2.5F : 2.0F;
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        PartDefinition root = mesh.getRoot();
        CubeDeformation head = new CubeDeformation(s[0]);
        CubeDeformation body = new CubeDeformation(s[1], s[2], s[3]);
        CubeDeformation arm = new CubeDeformation(s[4], s[5], s[6]);
        CubeDeformation leg = new CubeDeformation(s[7], s[8], s[9]);
        float ax = s[10], lx = s[11], ly = 12.0F - s[8];

        PartDefinition h = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, head), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, head.extend(0.5F)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, body), PartPose.ZERO);
        root.addOrReplaceChild("jacket", CubeListBuilder.create().texOffs(16, 32).addBox(-4, 0, -2, 8, 12, 4, body.extend(0.25F)), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(slim ? -2 : -3, -2, -2, armW, 12, 4, arm), PartPose.offset(-ax, armY, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1, -2, -2, armW, 12, 4, arm), PartPose.offset(ax, armY, 0));
        root.addOrReplaceChild("right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(slim ? -2 : -3, -2, -2, armW, 12, 4, arm.extend(0.25F)), PartPose.offset(-ax, armY, 0));
        root.addOrReplaceChild("left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1, -2, -2, armW, 12, 4, arm.extend(0.25F)), PartPose.offset(ax, armY, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4, leg), PartPose.offset(-lx, ly, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2, 0, -2, 4, 12, 4, leg), PartPose.offset(lx, ly, 0));
        root.addOrReplaceChild("right_pants", CubeListBuilder.create().texOffs(0, 32).addBox(-2, 0, -2, 4, 12, 4, leg.extend(0.25F)), PartPose.offset(-lx, ly, 0));
        root.addOrReplaceChild("left_pants", CubeListBuilder.create().texOffs(0, 48).addBox(-2, 0, -2, 4, 12, 4, leg.extend(0.25F)), PartPose.offset(lx, ly, 0));

        // horns, from the skin's (0,0) corner
        float top = -8.0F - s[0];
        PartDefinition hl = h.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -4, -1, 2, 4, 2),
                PartPose.offsetAndRotation(2.4F, top + 0.5F, -1.5F, -0.35F, 0, 0.4F));
        hl.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -3, -0.5F, 1, 3, 1),
                PartPose.offsetAndRotation(0, -3.6F, 0, -0.45F, 0, -0.25F));
        PartDefinition hr = h.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1, -4, -1, 2, 4, 2),
                PartPose.offsetAndRotation(-2.4F, top + 0.5F, -1.5F, -0.35F, 0, -0.4F));
        hr.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -3, -0.5F, 1, 3, 1),
                PartPose.offsetAndRotation(0, -3.6F, 0, -0.45F, 0, 0.25F));
        h.addOrReplaceChild("horn_c", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -5, -1, 2, 5, 2),
                PartPose.offsetAndRotation(0, top + 0.5F, -2.5F, -0.3F, 0, 0));
        h.addOrReplaceChild("nub_l", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -2, -0.5F, 1, 2, 1),
                PartPose.offsetAndRotation(2.2F, top + 0.3F, -2.6F, -0.2F, 0, 0.3F));
        h.addOrReplaceChild("nub_r", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -2, -0.5F, 1, 2, 1),
                PartPose.offsetAndRotation(-2.2F, top + 0.3F, -2.6F, -0.2F, 0, -0.3F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Which horns a skin wears (about half have none). */
    public static int hornStyle(int variant) {
        if (variant == CaptiveDemonEntity.HORNED) return HORNS_BIG;
        return switch ((variant * 7) % 6) {
            case 1 -> HORNS_PAIR;
            case 3 -> HORNS_SINGLE;
            case 4 -> HORNS_NUBS;
            default -> HORNS_NONE;
        };
    }

    public void showHorns(int variant) {
        int style = hornStyle(variant);
        hornL.visible = hornR.visible = style == HORNS_PAIR || style == HORNS_BIG;
        tipL.visible = tipR.visible = style == HORNS_BIG;
        hornC.visible = style == HORNS_SINGLE;
        nubL.visible = nubR.visible = style == HORNS_NUBS;
    }

    @Override
    public void prepareMobModel(CaptiveDemonEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        showHorns(entity.variant());
    }

    @Override
    public void setupAnim(CaptiveDemonEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.crouching = false;
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // the stoop: like a crouch scaled to the build (the body tips forward from the shoulders, legs set back)
        float k = hunch / 0.5F;
        this.body.xRot = hunch;
        this.body.y = 3.2F * k;
        this.head.y = 4.2F * k;
        this.rightArm.y = armY + 3.2F * k;
        this.leftArm.y = armY + 3.2F * k;
        this.rightLeg.z = this.leftLeg.z = 4.0F * k;
        this.rightLeg.y = this.leftLeg.y = legY + 0.2F * k;
        if (entity.isClimbing()) {
            // scaling a wall: arms up, clawing
            float c = Mth.sin(ageInTicks * 0.6F) * 0.5F;
            this.rightArm.xRot = -2.6F + c;
            this.leftArm.xRot = -2.6F - c;
        } else if (entity.isAggressive()) {
            if (build == CaptiveDemonEntity.BRUTE) {
                // heavy arms swung low and wide
                this.rightArm.zRot = 0.25F + Mth.cos(ageInTicks * 0.1F) * 0.05F;
                this.leftArm.zRot = -0.25F - Mth.cos(ageInTicks * 0.1F) * 0.05F;
                this.rightArm.xRot -= 0.35F + this.attackTime * 1.6F;
                this.leftArm.xRot -= 0.35F + this.attackTime * 1.2F;
            } else {
                AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, this.attackTime, ageInTicks);
                if (build == CaptiveDemonEntity.LEAN) {
                    this.rightArm.zRot += 0.15F;
                    this.leftArm.zRot -= 0.15F;
                }
            }
        } else if (build != CaptiveDemonEntity.ORDINARY) {
            // idle: arms dangle in front of the stooped body
            this.rightArm.xRot -= hunch * 0.6F;
            this.leftArm.xRot -= hunch * 0.6F;
        }
        // a starving twitch of the head now and then
        this.head.zRot = Mth.sin(ageInTicks * 0.9F + entity.getId()) * (Mth.sin(ageInTicks * 0.05F + entity.getId()) > 0.92F ? 0.25F : 0.0F);
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
    }

    /** The same standing pose with no motion (a body frozen at the moment of death). */
    public void freeze(CaptiveDemonEntity entity) {
        this.attackTime = 0;
        this.riding = false;
        this.young = false;
        setupAnim(entity, 0, 0, 0, 0, 0);
        this.head.zRot = 0;
        this.hat.copyFrom(this.head);
    }
}
