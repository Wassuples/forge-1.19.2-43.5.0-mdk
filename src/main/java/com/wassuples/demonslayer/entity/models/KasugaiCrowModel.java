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
 * The Kasugai crow: a sleek black crow with a heavy beak, a wedge tail and long wings that fold along its sides when
 * it stands or perches and beat when it flies.
 */
public class KasugaiCrowModel extends HierarchicalModel<CrowEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "kasugai_crow"), "main");

    private final ModelPart root, body, head, tail, leftWing, rightWing, leftLeg, rightLeg;

    public KasugaiCrowModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -3.5F, 4, 4, 7),
                PartPose.offsetAndRotation(0.0F, 17.5F, 0.0F, -0.2F, 0.0F, 0.0F));
        PartDefinition head = p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 11).addBox(-1.5F, -3.0F, -2.0F, 3, 3, 3),
                PartPose.offset(0.0F, 16.6F, -3.2F));
        // a crow's beak is long and heavy: upper mandible plus a slimmer hooked tip
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(12, 11).addBox(-0.5F, -1.8F, -4.0F, 1, 1, 2)
                        .texOffs(18, 11).addBox(-0.5F, -1.0F, -3.5F, 1, 1, 1, new CubeDeformation(-0.1F)),
                PartPose.ZERO);
        p.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 17).addBox(-1.5F, -0.5F, 0.0F, 3, 1, 4),
                PartPose.offsetAndRotation(0.0F, 18.0F, 3.0F, 0.35F, 0.0F, 0.0F));
        p.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(0.0F, 0.0F, -2.5F, 1, 4, 6),
                PartPose.offset(2.0F, 15.8F, -0.5F));
        p.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 22).addBox(-1.0F, 0.0F, -2.5F, 1, 4, 6),
                PartPose.offset(-2.0F, 15.8F, -0.5F));
        p.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(22, 0).addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1),
                PartPose.offset(1.0F, 20.0F, 0.5F));
        p.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(22, 0).addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1),
                PartPose.offset(-1.0F, 20.0F, 0.5F));
        return LayerDefinition.create(mesh, 32, 32);
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
            // wings spread out to the sides and beat; legs tucked back; body level
            float beat = Mth.sin(age * 0.9F);
            leftWing.zRot = -1.35F + beat * 0.9F;
            rightWing.zRot = 1.35F - beat * 0.9F;
            leftWing.xRot = rightWing.xRot = 0.0F;
            body.xRot = 0.0F;
            tail.xRot = 0.05F;
            leftLeg.xRot = rightLeg.xRot = 1.2F;
        } else {
            // folded along the body, tips crossing over the tail; a little hop when walking
            leftWing.zRot = -0.08F;
            rightWing.zRot = 0.08F;
            leftWing.xRot = rightWing.xRot = -0.15F;
            body.xRot = -0.2F;
            tail.xRot = 0.35F + Mth.sin(age * 0.08F) * 0.05F;
            leftLeg.xRot = Mth.cos(limbSwing * 1.3F) * 1.2F * limbSwingAmount;
            rightLeg.xRot = Mth.cos(limbSwing * 1.3F + Mth.PI) * 1.2F * limbSwingAmount;
            if (crow.isPerched()) head.xRot += Mth.sin(age * 0.05F) * 0.1F;   // looks around while it waits
        }
    }
}
