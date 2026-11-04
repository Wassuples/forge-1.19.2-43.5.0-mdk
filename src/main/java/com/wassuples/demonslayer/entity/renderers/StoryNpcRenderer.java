package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.client.dialogue.SpeechBubbleRenderer;
import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;


/**
 * Story NPCs: player-shaped (wide or slim arms per character, scaled per character — the twins are children), with
 * the mod's own name plate in the character's colour and speech bubbles for what they say out loud.
 */
public class StoryNpcRenderer extends HumanoidMobRenderer<StoryNpcEntity, PlayerModel<StoryNpcEntity>> {
    private final PlayerModel<StoryNpcEntity> wide, slim;

    public StoryNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wide = this.model;
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new StoryAccessoryLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(StoryNpcEntity entity) {
        StoryCharacter c = entity.character();
        return c == StoryCharacter.CANDIDATE ? StoryCharacter.candidateTexture(entity.variant()) : c.texture();
    }

    @Override
    protected void scale(StoryNpcEntity entity, PoseStack pose, float partialTick) {
        float s = 0.9375F * entity.character().scale();
        pose.scale(s, s, s);
    }

    @Override
    protected boolean shouldShowName(StoryNpcEntity entity) {
        return false;
    }

    @Override
    public void render(StoryNpcEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        StoryCharacter c = entity.character();
        boolean isSlim = c == StoryCharacter.CANDIDATE ? entity.variant() % 2 == 1 : c.slim();
        this.model = isSlim ? slim : wide;
        super.render(entity, yaw, partialTick, pose, buffers, light);
        drawOverhead(entity, partialTick, pose, buffers);
    }

    private void drawOverhead(StoryNpcEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        SpeechBubbleRenderer.draw(entity, entity.storyName(), entity.character().color(), partialTick, pose, buffers,
                this.entityRenderDispatcher, this.getFont());
    }
}
