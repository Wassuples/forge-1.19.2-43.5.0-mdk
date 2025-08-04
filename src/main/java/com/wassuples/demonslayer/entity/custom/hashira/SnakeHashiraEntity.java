package com.wassuples.demonslayer.entity.custom.hashira;

import com.wassuples.demonslayer.entity.custom.HashiraEntity;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.item.custom.SwordModel;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class SnakeHashiraEntity extends HashiraEntity {
    public SnakeHashiraEntity(EntityType<? extends HashiraEntity> pEntityType, Level level) {
        super(pEntityType, level);
        this.setBreathingStyle(BreathingStyles.SERPENT);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        this.setItemSlot(EquipmentSlot.MAINHAND, NichirinSwordItem.createSword(SwordModel.SNAKE));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }
}
