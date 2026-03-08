package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Custom entity: Blinding Mist Cloud.
 */
public class BlindingMistCloudEntity extends AreaEffectCloud {
    public BlindingMistCloudEntity(EntityType<? extends AreaEffectCloud> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity owner = this.getOwner();
        if (owner != null && owner.hasEffect(MobEffects.BLINDNESS)) {
            owner.removeEffect(MobEffects.BLINDNESS);
        }
        List<Player> playersInCloud = this.level.getEntitiesOfClass(Player.class, this.getBoundingBox());
        for (Player playerInCloud : playersInCloud) {
            if (playerInCloud.is(owner)) continue;

            boolean isMistBreather = playerInCloud.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                    .map(style -> style.getCurrentStyles() == BreathingStyles.MIST)
                    .orElse(false);

            if (isMistBreather && playerInCloud.hasEffect(MobEffects.BLINDNESS)) {
                playerInCloud.removeEffect(MobEffects.BLINDNESS);
            }
        }
    }
}