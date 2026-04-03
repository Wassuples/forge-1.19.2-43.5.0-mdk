package com.wassuples.demonslayer.entity.renderers.demon;

import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.blueprint.NemesisSkinRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side renderer for Nemesis Demon entities.
 *
 * <p>Canonical demons (those with a source blueprint ID) use their own unique skin textures.
 * Procedurally generated demons fall back to a default texture.
 */
public class NemesisDemonRenderer extends MobRenderer<NemesisDemonEntity, PlayerModel<NemesisDemonEntity>> {

    public NemesisDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(NemesisDemonEntity entity) {
        ResourceLocation explicit = NemesisSkinRegistry.parseSkinLocation(entity.getSkinLocation());
        if (explicit != null) {
            return explicit;
        }

        String blueprintId = entity.getBlueprintId();
        ResourceLocation canonical = NemesisSkinRegistry.getCanonicalSkinForBlueprint(blueprintId);
        if (canonical != null) {
            return canonical;
        }

        return NemesisSkinRegistry.pickDeterministicNonCanonSkin(entity.getUUID());
    }
}
