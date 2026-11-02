package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ScaleArmorEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Killer Fish Scales on the skin: while the owner has an active {@link ScaleArmorEntity}, a skin-tight copy of the
 * humanoid model (inflated by a hair so it never z-fights the skin) is drawn over them with a diamond fish-scale
 * texture, posed exactly like the body underneath, followed by a pearly shimmer that sweeps across the scales.
 * The face is left bare. Added to the player renderers and every humanoid entity renderer (see DemonSlayer).
 */
public class ScaleSkinLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public static final ModelLayerLocation WIDE = new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "scale_skin"), "main");
    public static final ModelLayerLocation SLIM = new ModelLayerLocation(new ResourceLocation(DemonSlayer.MOD_ID, "scale_skin_slim"), "main");

    private static final ResourceLocation SCALES = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/gyokko/scale_skin.png");
    private static final ResourceLocation SHIMMER = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/gyokko/scale_shimmer.png");

    private final PlayerModel<T> skin;

    public ScaleSkinLayer(RenderLayerParent<T, M> parent, EntityModelSet models, boolean slim) {
        super(parent);
        this.skin = new PlayerModel<>(models.bakeLayer(slim ? SLIM : WIDE), slim);
    }

    public static LayerDefinition createLayer(boolean slim) {
        return LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.07f), slim), 64, 64);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ScaleArmorEntity armor = ScaleArmorEntity.clientArmorFor(entity);
        if (armor == null || entity.isInvisible()) return;
        float appear = armor.appear(partialTick);
        if (appear <= 0.0f) return;

        M parent = this.getParentModel();
        parent.copyPropertiesTo(this.skin);
        // Only the inner (skin) layer; the outer jacket/sleeve layer would sit on top of the scales.
        this.skin.hat.visible = false;
        this.skin.jacket.visible = false;
        this.skin.leftSleeve.visible = false;
        this.skin.rightSleeve.visible = false;
        this.skin.leftPants.visible = false;
        this.skin.rightPants.visible = false;
        this.skin.head.visible = parent.head.visible;
        this.skin.body.visible = parent.body.visible;
        this.skin.leftArm.visible = parent.leftArm.visible;
        this.skin.rightArm.visible = parent.rightArm.visible;
        this.skin.leftLeg.visible = parent.leftLeg.visible;
        this.skin.rightLeg.visible = parent.rightLeg.visible;

        // Scales grow outward from the body: alpha fades in/out with the armour.
        this.skin.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(SCALES)), light,
                OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, Mth.clamp(appear * 1.1f, 0.0f, 1.0f));

        // Pearly shimmer sliding over the scales (additive, like the charged-creeper swirl).
        float t = armor.tickCount + partialTick;
        float glint = 0.35f * appear;
        this.skin.renderToBuffer(pose, buffers.getBuffer(RenderType.energySwirl(SHIMMER, (t * 0.004f) % 1.0f, (t * 0.015f) % 1.0f)),
                light, OverlayTexture.NO_OVERLAY, glint * 0.7f, glint, glint * 0.9f, 1.0f);
    }
}
