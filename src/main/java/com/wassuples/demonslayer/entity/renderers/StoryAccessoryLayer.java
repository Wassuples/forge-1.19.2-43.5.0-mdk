package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Each story character's own 3D details, worn on the head of the player-shaped body (a skin alone looks flat):
 * <ul>
 *   <li>Inosuke: his boar's-head mask (snout, tusks, ears, bristling mane);</li>
 *   <li>Kanao: the butterfly ornament holding her side ponytail;</li>
 *   <li>Tanjiro: his hanafuda earrings;</li>
 *   <li>Zenitsu: his spiky blond hair, orange at the tips;</li>
 *   <li>Genya: his crest down a shaved head;</li>
 *   <li>the Ubuyashiki twins: a paper-flower hair ornament;</li>
 *   <li>Hotaru Haganezuka: the hyottoko mask, a headband, and his straw hat hung with wind chimes.</li>
 * </ul>
 * Generated from the design prototype; texture {@code textures/entity/npc/story_accessories.png} (128x128).
 */
public class StoryAccessoryLayer extends RenderLayer<StoryNpcEntity, PlayerModel<StoryNpcEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "story_accessories"), "main");
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/npc/story_accessories.png");
    private final ModelPart boar, butterfly, earrings, mohawk, zenitsuHair, kanzashi, hyottoko, kasa;

    public StoryAccessoryLayer(RenderLayerParent<StoryNpcEntity, PlayerModel<StoryNpcEntity>> parent, EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(LAYER);
        this.boar = root.getChild("boar");
        this.butterfly = root.getChild("butterfly");
        this.earrings = root.getChild("earrings");
        this.mohawk = root.getChild("mohawk");
        this.zenitsuHair = root.getChild("zen_hair");
        this.kanzashi = root.getChild("kanzashi");
        this.hyottoko = root.getChild("hyottoko");
        this.kasa = root.getChild("kasa");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p1 = root.addOrReplaceChild("boar", CubeListBuilder.create().texOffs(0, 0).addBox(-4.6F, -8.8F, -4.6F, 9.2F, 8.6F, 9.2F).texOffs(96, 0).addBox(-1F, -10.2F, -3.5F, 2F, 2F, 7F).texOffs(88, 0).addBox(-3.3F, -6.6F, -4.75F, 1F, 1F, 0.3F).addBox(2.3F, -6.6F, -4.75F, 1F, 1F, 0.3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p2 = p1.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(40, 0).addBox(-2.5F, -2.6F, -4.4F, 5F, 4.2F, 4.6F).texOffs(60, 0).addBox(-2.1F, -2.2F, -4.8F, 4.2F, 3.2F, 0.5F), PartPose.offsetAndRotation(0F, -2.6F, -4.4F, 0.12F, 0F, 0F));
        p2.addOrReplaceChild("tuskL", CubeListBuilder.create().texOffs(72, 0).addBox(-0.4F, -2.2F, -0.4F, 0.8F, 2.4F, 0.8F), PartPose.offsetAndRotation(2.1F, 1.2F, -3.6F, 0F, 0F, -0.35F));
        p2.addOrReplaceChild("tuskR", CubeListBuilder.create().texOffs(72, 0).addBox(-0.4F, -2.2F, -0.4F, 0.8F, 2.4F, 0.8F), PartPose.offsetAndRotation(-2.1F, 1.2F, -3.6F, 0F, 0F, 0.35F));
        p1.addOrReplaceChild("earL", CubeListBuilder.create().texOffs(80, 0).addBox(-0.8F, -2.8F, -0.4F, 1.6F, 2.8F, 0.8F), PartPose.offsetAndRotation(3.6F, -8.4F, 0.5F, 0.2F, 0F, 0.55F));
        p1.addOrReplaceChild("earR", CubeListBuilder.create().texOffs(80, 0).addBox(-0.8F, -2.8F, -0.4F, 1.6F, 2.8F, 0.8F), PartPose.offsetAndRotation(-3.6F, -8.4F, 0.5F, 0.2F, 0F, -0.55F));
        PartDefinition p7 = root.addOrReplaceChild("butterfly", CubeListBuilder.create(), PartPose.offsetAndRotation(4.2F, -6F, 1.2F, 0F, 0F, 0F));
        p7.addOrReplaceChild("wingA", CubeListBuilder.create().texOffs(0, 20).addBox(-3F, -2.4F, -0.15F, 3F, 2.4F, 0.3F), PartPose.offsetAndRotation(0.1F, 0F, 0F, 0F, 1.571F, 0.5F));
        p7.addOrReplaceChild("wingB", CubeListBuilder.create().texOffs(0, 20).addBox(0F, -2.4F, -0.15F, 3F, 2.4F, 0.3F), PartPose.offsetAndRotation(0.1F, 0F, 0F, 0F, 1.571F, -0.25F));
        p7.addOrReplaceChild("wingC", CubeListBuilder.create().texOffs(0, 20).addBox(-1.8F, -1.6F, -0.15F, 1.8F, 1.6F, 0.3F), PartPose.offsetAndRotation(0.1F, 0.2F, 0F, 0F, 1.571F, 2.6F));
        p7.addOrReplaceChild("wingD", CubeListBuilder.create().texOffs(0, 20).addBox(0F, -1.6F, -0.15F, 1.8F, 1.6F, 0.3F), PartPose.offsetAndRotation(0.1F, 0.2F, 0F, 0F, 1.571F, -2.6F));
        p7.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 20).addBox(-1F, 0F, -1F, 2F, 6.5F, 2F), PartPose.offsetAndRotation(-0.4F, 0.4F, 0.4F, 0.25F, 0F, -0.12F));
        PartDefinition p13 = root.addOrReplaceChild("earrings", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p13.addOrReplaceChild("cardL", CubeListBuilder.create().texOffs(56, 20).addBox(-0.8F, 0F, -0.1F, 1.6F, 2.6F, 0.2F), PartPose.offsetAndRotation(4.15F, -2.3F, 0.2F, 0F, 1.571F, 0F));
        p13.addOrReplaceChild("cardR", CubeListBuilder.create().texOffs(56, 20).addBox(-0.8F, 0F, -0.1F, 1.6F, 2.6F, 0.2F), PartPose.offsetAndRotation(-4.15F, -2.3F, 0.2F, 0F, -1.571F, 0F));
        PartDefinition p16 = root.addOrReplaceChild("mohawk", CubeListBuilder.create().texOffs(32, 20).addBox(-1.1F, -10.6F, -4.6F, 2.2F, 2.8F, 9.2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p16.addOrReplaceChild("spike0", CubeListBuilder.create().texOffs(32, 20).addBox(-0.8F, -2.2F, -0.5F, 1.6F, 2.4F, 1F), PartPose.offsetAndRotation(0F, -10.4F, -3.6F, -0.35F, 0F, 0F));
        p16.addOrReplaceChild("spike1", CubeListBuilder.create().texOffs(32, 20).addBox(-0.8F, -2.2F, -0.5F, 1.6F, 2.4F, 1F), PartPose.offsetAndRotation(0F, -10.4F, -1.3F, -0.35F, 0F, 0F));
        p16.addOrReplaceChild("spike2", CubeListBuilder.create().texOffs(32, 20).addBox(-0.8F, -2.2F, -0.5F, 1.6F, 2.4F, 1F), PartPose.offsetAndRotation(0F, -10.4F, 1F, -0.35F, 0F, 0F));
        p16.addOrReplaceChild("spike3", CubeListBuilder.create().texOffs(32, 20).addBox(-0.8F, -2.2F, -0.5F, 1.6F, 2.4F, 1F), PartPose.offsetAndRotation(0F, -10.4F, 3.3F, -0.35F, 0F, 0F));
        PartDefinition p21 = root.addOrReplaceChild("zen_hair", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p21.addOrReplaceChild("s0", CubeListBuilder.create().texOffs(64, 20).addBox(-0.9F, -2.6F, -0.9F, 1.8F, 2.8F, 1.8F), PartPose.offsetAndRotation(-3F, -8.2F, -3F, -0.6F, 0F, 0.4F));
        p21.addOrReplaceChild("s1", CubeListBuilder.create().texOffs(64, 20).addBox(-0.9F, -2.6F, -0.9F, 1.8F, 2.8F, 1.8F), PartPose.offsetAndRotation(0F, -8.2F, -3.6F, -0.8F, 0F, 0F));
        p21.addOrReplaceChild("s2", CubeListBuilder.create().texOffs(64, 20).addBox(-0.9F, -2.6F, -0.9F, 1.8F, 2.8F, 1.8F), PartPose.offsetAndRotation(3F, -8.2F, -3F, -0.6F, 0F, -0.4F));
        p21.addOrReplaceChild("s3", CubeListBuilder.create().texOffs(64, 20).addBox(-0.9F, -2.6F, -0.9F, 1.8F, 2.8F, 1.8F), PartPose.offsetAndRotation(-3.2F, -8.2F, 1F, 0F, 0F, 0.7F));
        p21.addOrReplaceChild("s4", CubeListBuilder.create().texOffs(64, 20).addBox(-0.9F, -2.6F, -0.9F, 1.8F, 2.8F, 1.8F), PartPose.offsetAndRotation(3.2F, -8.2F, 1F, 0F, 0F, -0.7F));
        p21.addOrReplaceChild("s5", CubeListBuilder.create().texOffs(64, 20).addBox(-0.9F, -2.6F, -0.9F, 1.8F, 2.8F, 1.8F), PartPose.offsetAndRotation(-1.5F, -8.2F, 3F, 0.5F, 0F, 0.3F));
        p21.addOrReplaceChild("s6", CubeListBuilder.create().texOffs(64, 20).addBox(-0.9F, -2.6F, -0.9F, 1.8F, 2.8F, 1.8F), PartPose.offsetAndRotation(1.5F, -8.2F, 3F, 0.5F, 0F, -0.3F));
        p21.addOrReplaceChild("s7", CubeListBuilder.create().texOffs(64, 20).addBox(-0.9F, -2.6F, -0.9F, 1.8F, 2.8F, 1.8F), PartPose.offsetAndRotation(0F, -8.2F, 0F, 0.1F, 0F, 0F));
        root.addOrReplaceChild("kanzashi", CubeListBuilder.create().texOffs(72, 20).addBox(-1.2F, -1.2F, -0.2F, 2.4F, 2.4F, 0.4F).texOffs(80, 20).addBox(-0.15F, 0F, -0.15F, 0.3F, 2.6F, 0.3F), PartPose.offsetAndRotation(3.9F, -7F, 1.5F, 0.2F, 1.571F, 0.3F));
        PartDefinition p31 = root.addOrReplaceChild("hyottoko", CubeListBuilder.create().texOffs(0, 40).addBox(-4.3F, -8.4F, -5.1F, 8.6F, 8.4F, 0.8F).texOffs(24, 40).addBox(-1.4F, -3.4F, -7F, 2.2F, 2.2F, 2F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p31.addOrReplaceChild("band", CubeListBuilder.create().texOffs(40, 40).addBox(-4.5F, -0.6F, -4.5F, 9F, 1.2F, 9F), PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0F));
        PartDefinition p33 = root.addOrReplaceChild("kasa", CubeListBuilder.create().texOffs(0, 56).addBox(-9F, -10.2F, -9F, 18F, 0.8F, 18F).texOffs(0, 76).addBox(-5.5F, -12F, -5.5F, 11F, 2F, 11F).texOffs(44, 76).addBox(-3F, -13.2F, -3F, 6F, 1.2F, 6F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        p33.addOrReplaceChild("chime0", CubeListBuilder.create().texOffs(76, 76).addBox(-0.1F, 0F, -0.1F, 0.2F, 1.8F, 0.2F).texOffs(68, 76).addBox(-0.6F, 1.6F, -0.6F, 1.2F, 1.4F, 1.2F), PartPose.offsetAndRotation(3.061F, -9.6F, -7.391F, 0F, 0F, 0F));
        p33.addOrReplaceChild("chime1", CubeListBuilder.create().texOffs(76, 76).addBox(-0.1F, 0F, -0.1F, 0.2F, 1.8F, 0.2F).texOffs(68, 76).addBox(-0.6F, 1.6F, -0.6F, 1.2F, 1.4F, 1.2F), PartPose.offsetAndRotation(7.391F, -9.6F, -3.061F, 0F, 0F, 0F));
        p33.addOrReplaceChild("chime2", CubeListBuilder.create().texOffs(76, 76).addBox(-0.1F, 0F, -0.1F, 0.2F, 1.8F, 0.2F).texOffs(68, 76).addBox(-0.6F, 1.6F, -0.6F, 1.2F, 1.4F, 1.2F), PartPose.offsetAndRotation(7.391F, -9.6F, 3.061F, 0F, 0F, 0F));
        p33.addOrReplaceChild("chime3", CubeListBuilder.create().texOffs(76, 76).addBox(-0.1F, 0F, -0.1F, 0.2F, 1.8F, 0.2F).texOffs(68, 76).addBox(-0.6F, 1.6F, -0.6F, 1.2F, 1.4F, 1.2F), PartPose.offsetAndRotation(3.061F, -9.6F, 7.391F, 0F, 0F, 0F));
        p33.addOrReplaceChild("chime4", CubeListBuilder.create().texOffs(76, 76).addBox(-0.1F, 0F, -0.1F, 0.2F, 1.8F, 0.2F).texOffs(68, 76).addBox(-0.6F, 1.6F, -0.6F, 1.2F, 1.4F, 1.2F), PartPose.offsetAndRotation(-3.061F, -9.6F, 7.391F, 0F, 0F, 0F));
        p33.addOrReplaceChild("chime5", CubeListBuilder.create().texOffs(76, 76).addBox(-0.1F, 0F, -0.1F, 0.2F, 1.8F, 0.2F).texOffs(68, 76).addBox(-0.6F, 1.6F, -0.6F, 1.2F, 1.4F, 1.2F), PartPose.offsetAndRotation(-7.391F, -9.6F, 3.061F, 0F, 0F, 0F));
        p33.addOrReplaceChild("chime6", CubeListBuilder.create().texOffs(76, 76).addBox(-0.1F, 0F, -0.1F, 0.2F, 1.8F, 0.2F).texOffs(68, 76).addBox(-0.6F, 1.6F, -0.6F, 1.2F, 1.4F, 1.2F), PartPose.offsetAndRotation(-7.391F, -9.6F, -3.061F, 0F, 0F, 0F));
        p33.addOrReplaceChild("chime7", CubeListBuilder.create().texOffs(76, 76).addBox(-0.1F, 0F, -0.1F, 0.2F, 1.8F, 0.2F).texOffs(68, 76).addBox(-0.6F, 1.6F, -0.6F, 1.2F, 1.4F, 1.2F), PartPose.offsetAndRotation(-3.061F, -9.6F, -7.391F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, StoryNpcEntity npc, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (npc.isInvisible()) return;
        ModelPart[] parts = switch (npc.character()) {
            case INOSUKE -> new ModelPart[]{boar};
            case KANAO -> new ModelPart[]{butterfly};
            case TANJIRO -> new ModelPart[]{earrings};
            case GENYA -> new ModelPart[]{mohawk};
            case ZENITSU -> new ModelPart[]{zenitsuHair};
            case KIRIYA, KANATA -> new ModelPart[]{kanzashi};
            case SWORDSMITH -> new ModelPart[]{hyottoko, kasa};
            default -> new ModelPart[0];
        };
        if (parts.length == 0) return;
        // the wind chimes sway as Haganezuka walks
        if (npc.character() == StoryCharacter.SWORDSMITH) {
            for (int i = 0; i < 8; i++) kasa.getChild("chime" + i).zRot = net.minecraft.util.Mth.sin(ageInTicks * 0.2F + i) * 0.25F * (0.3F + limbSwingAmount);
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(npc, 0.0F);
        pose.pushPose();
        this.getParentModel().head.translateAndRotate(pose);
        for (ModelPart p : parts) p.render(pose, vc, light, overlay);
        pose.popPose();
    }
}
