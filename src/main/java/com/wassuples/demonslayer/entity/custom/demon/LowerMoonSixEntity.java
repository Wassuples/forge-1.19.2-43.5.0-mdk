package com.wassuples.demonslayer.entity.custom.demon;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.entity.custom.DemonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class LowerMoonSixEntity extends DemonEntity {

    public LowerMoonSixEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setDemonRank(DemonRank.LOWER_MOON_6);
    }

    // These are boss-level stats
    public static AttributeSupplier.Builder createAttributes() {
        return DemonEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D) // Very high health
                .add(Attributes.MOVEMENT_SPEED, 0.35D) // Faster than a player
                .add(Attributes.ATTACK_DAMAGE, 12.0D) // Hits very hard
                .add(Attributes.ARMOR, 15.0D) // Tough hide
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D); // Resists being knocked back
    }
}