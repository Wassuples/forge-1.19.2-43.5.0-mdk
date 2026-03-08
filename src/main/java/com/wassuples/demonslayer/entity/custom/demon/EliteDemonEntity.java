package com.wassuples.demonslayer.entity.custom.demon;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.entity.custom.DemonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * NPC entity: Elite Demon.
 *
 * @see com.wassuples.demonslayer.entity.custom.DemonEntity
 */
public class EliteDemonEntity extends DemonEntity {
    public EliteDemonEntity(EntityType<? extends DemonEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setDemonRank(DemonRank.ELITE_DEMON);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DemonEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D);
    }
}