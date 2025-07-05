package com.wassuples.demonslayer.entity.custom.demon;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.entity.custom.DemonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class CommonDemonEntity extends DemonEntity {
    public CommonDemonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setDemonRank(DemonRank.DEMON);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DemonEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D);
    }
}