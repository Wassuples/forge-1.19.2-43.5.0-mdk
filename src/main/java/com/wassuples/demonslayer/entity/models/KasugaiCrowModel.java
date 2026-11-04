package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CrowEntity;
import net.minecraft.client.model.HierarchicalModel;
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
 * The Kasugai crow, a jungle crow: a heavy arched beak (upper and lower mandible), a rounded head with a ruff of
 * throat feathers and bright eyes, a body tapering to a wedge tail, layered wings whose long primaries cross over the
 * tail when folded and spread and beat in flight, and short scaly legs with toes. Texture 64x64
 * ({@code kasugai_crow_v2.png}).
 */
public class KasugaiCrowModel extends HierarchicalModel<CrowEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "kasugai_crow"), "main");

    private final ModelPart root, body, head, tail, leftWing, rightWing, leftOuter, rightOuter, leftLeg, rightLeg;

    public KasugaiCrowModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.leftOuter = leftWing.getChild("outer");
        this.rightOuter = rightWing.getChild("outer");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.2F, -2.2F, -3.6F, 4.4F, 4.2F, 4.6F).addBox(-1.9F, -2F, 0.8F, 3.8F, 3.6F, 3.4F), PartPose.offsetAndRotation(0F, 18.6F, 0F, -0.22F, 0F, 0F));
        PartDefinition p2 = p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-1.6F, -3.4F, -2.4F, 3.2F, 3.2F, 3.4F).texOffs(40, 16).addBox(-1.75F, -2.6F, -1.6F, 3.5F, 0.9F, 0.9F), PartPose.offsetAndRotation(0F, 16.4F, -3.6F, 0F, 0F, 0F));
        PartDefinition p3 = p2.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(26, 16).addBox(-0.75F, -0.6F, -2.2F, 1.5F, 1.3F, 2.4F).addBox(-0.5F, -0.3F, -3.6F, 1F, 0.9F, 1.6F), PartPose.offsetAndRotation(0F, -1.6F, -2.4F, 0.08F, 0F, 0F));
        p3.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(26, 16).addBox(-0.6F, -0.1F, -2.6F, 1.2F, 0.5F, 2.6F), PartPose.offsetAndRotation(0F, 0.7F, -0.4F, 0F, 0F, 0F));
        p2.addOrReplaceChild("ruff", CubeListBuilder.create().texOffs(0, 16).addBox(-1.8F, -0.8F, -1.6F, 3.6F, 2.2F, 3F), PartPose.offsetAndRotation(0F, -0.4F, 0.2F, 0.2F, 0F, 0F));
        p.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 46).addBox(-1.6F, -0.5F, 0F, 3.2F, 0.9F, 5F).addBox(-2F, -0.3F, 2.6F, 4F, 0.6F, 2.6F), PartPose.offsetAndRotation(0F, 18.8F, 3.4F, 0.3F, 0F, 0F));
        PartDefinition p7 = p.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 30).addBox(-0.5F, 0F, -1.6F, 1F, 3.4F, 5F), PartPose.offsetAndRotation(2.25F, 16.8F, -1.6F, -0.18F, 0F, -0.06F));
        p7.addOrReplaceChild("outer", CubeListBuilder.create().texOffs(24, 30).addBox(-0.35F, -0.2F, 0F, 0.6F, 2.4F, 5.2F), PartPose.offsetAndRotation(0F, 0.6F, 2.4F, 0F, 0F, 0F));
        PartDefinition p9 = p.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 30).addBox(-0.5F, 0F, -1.6F, 1F, 3.4F, 5F), PartPose.offsetAndRotation(-2.25F, 16.8F, -1.6F, -0.18F, 0F, 0.06F));
        p9.addOrReplaceChild("outer", CubeListBuilder.create().texOffs(24, 30).addBox(-0.25F, -0.2F, 0F, 0.6F, 2.4F, 5.2F), PartPose.offsetAndRotation(0F, 0.6F, 2.4F, 0F, 0F, 0F));
        PartDefinition p11 = p.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(44, 0).addBox(-0.4F, 0F, -0.4F, 0.8F, 3F, 0.8F), PartPose.offsetAndRotation(0.9F, 20.6F, 0.6F, 0F, 0F, 0F));
        p11.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(44, 0).addBox(-0.4F, 0F, -1.8F, 0.8F, 0.4F, 1.8F).addBox(-1F, 0F, -1.4F, 0.6F, 0.4F, 1.2F).addBox(0.4F, 0F, -1.4F, 0.6F, 0.4F, 1.2F).addBox(-0.25F, 0F, 0F, 0.5F, 0.4F, 1F), PartPose.offsetAndRotation(0F, 3F, 0F, 0F, 0F, 0F));
        PartDefinition p13 = p.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(44, 0).addBox(-0.4F, 0F, -0.4F, 0.8F, 3F, 0.8F), PartPose.offsetAndRotation(-0.9F, 20.6F, 0.6F, 0F, 0F, 0F));
        p13.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(44, 0).addBox(-0.4F, 0F, -1.8F, 0.8F, 0.4F, 1.8F).addBox(-1F, 0F, -1.4F, 0.6F, 0.4F, 1.2F).addBox(0.4F, 0F, -1.4F, 0.6F, 0.4F, 1.2F).addBox(-0.25F, 0F, 0F, 0.5F, 0.4F, 1F), PartPose.offsetAndRotation(0F, 3F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CrowEntity crow, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        if (crow.isFlying()) {
            // wings spread out to the sides and beat, the primaries flexing behind the stroke; legs tucked; body level
            float beat = Mth.sin(age * 0.9F);
            leftWing.zRot = -1.35F + beat * 0.9F;
            rightWing.zRot = 1.35F - beat * 0.9F;
            leftWing.xRot = rightWing.xRot = 0.0F;
            leftOuter.zRot = -0.35F * Mth.sin(age * 0.9F - 0.6F);
            rightOuter.zRot = 0.35F * Mth.sin(age * 0.9F - 0.6F);
            leftOuter.yRot = -0.5F;
            rightOuter.yRot = 0.5F;
            body.xRot = 0.0F;
            tail.xRot = 0.05F;
            leftLeg.xRot = rightLeg.xRot = 1.2F;
        } else {
            // folded along the body, the primaries crossing over the tail; a little hop when walking
            leftWing.zRot = -0.06F;
            rightWing.zRot = 0.06F;
            leftWing.xRot = rightWing.xRot = -0.18F;
            leftOuter.zRot = rightOuter.zRot = 0.0F;
            leftOuter.yRot = -0.12F;
            rightOuter.yRot = 0.12F;
            body.xRot = -0.22F;
            tail.xRot = 0.3F + Mth.sin(age * 0.08F) * 0.05F;
            leftLeg.xRot = Mth.cos(limbSwing * 1.3F) * 1.2F * limbSwingAmount;
            rightLeg.xRot = Mth.cos(limbSwing * 1.3F + Mth.PI) * 1.2F * limbSwingAmount;
            if (crow.isPerched()) head.xRot += Mth.sin(age * 0.05F) * 0.1F;   // looks around while it waits
        }
    }
}
