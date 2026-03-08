package com.wassuples.demonslayer.client.renderer; // Or your preferred package

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side component: Mod Render Types.
 */
public class ModRenderTypes extends RenderType {

    // Dummy constructor, required by the parent class.
    public ModRenderTypes(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
        super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
    }

    public static RenderType hologram(ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setDepthTestState(NO_DEPTH_TEST)
                .setCullState(NO_CULL)
                .createCompositeState(true);

        return create("hologram", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
    }

    public static RenderType infoBoxBackground() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_SHADER)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .createCompositeState(false);

        return create("info_box_background", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    public static RenderType heatLightningCracks(net.minecraft.resources.ResourceLocation cracksTexture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(cracksTexture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .createCompositeState(true);

        return create("heat_lightning_cracks", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
    }
}