package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Intentionally draws nothing: the illusion is the missing blocks themselves, which each client hides
 * locally (see ToonTunnelClient). Any frame or decal here would give the lie away.
 */
public class ToonFakeTunnelRenderer extends EntityRenderer<ToonFakeTunnelEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonFakeTunnelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonFakeTunnelEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        // no-op by design
    }

    @Override
    public ResourceLocation getTextureLocation(ToonFakeTunnelEntity entity) {
        return TEXTURE;
    }
}
