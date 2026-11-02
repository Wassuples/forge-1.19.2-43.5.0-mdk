package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.entity.custom.ColdWhitePrincessEntity;
import com.wassuples.demonslayer.entity.models.ColdWhitePrincessModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/**
 * Cold White Princess: Doma's ice doll (GeckoLib model), plus the freezing breath she exhales at her target, drawn
 * as a widening cone of mist (her breath had no visible effect before: its particles were spawned on the server).
 */
public class ColdWhitePrincessRenderer extends GeoEntityRenderer<ColdWhitePrincessEntity> {
    public ColdWhitePrincessRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ColdWhitePrincessModel());
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(ColdWhitePrincessEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
        int targetId = entity.getBreathTargetId();
        if (targetId < 0) return;
        Entity target = entity.level.getEntity(targetId);
        if (target == null) return;
        Vec3 origin = entity.getPosition(partialTicks);
        Vec3 mouth = entity.getEyePosition(partialTicks).subtract(0, 0.2, 0).subtract(origin);
        Vec3 aim = target.getEyePosition(partialTicks).subtract(origin);
        Vec3 dir = aim.subtract(mouth);
        if (dir.lengthSqr() < 1.0E-4) return;
        dir = dir.normalize();
        float age = entity.tickCount + partialTicks;
        for (int i = 0; i < 18; i++) {
            float q = ((age * 0.09f) + i / 18.0f) % 1.0f;
            double d = 0.6 + q * 15.0;
            java.util.Random rnd = new java.util.Random(entity.getId() * 131L + i);
            Vec3 jitter = new Vec3(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5).scale(d * 0.4);
            Vec3 p = mouth.add(dir.scale(d)).add(jitter);
            DomaRenderHelper.puff(stack, bufferIn, this.entityRenderDispatcher.camera, p, (float) (0.4 + d * 0.3), i + age * 0.05f, 0.55f * (1.0f - q));
        }
    }
}
