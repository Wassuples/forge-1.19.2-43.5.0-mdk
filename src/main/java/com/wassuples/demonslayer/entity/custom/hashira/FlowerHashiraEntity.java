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

/**
 * NPC Hashira entity: Flower Hashira.
 *
 * @see com.wassuples.demonslayer.entity.custom.HashiraEntity
 */
public class FlowerHashiraEntity extends HashiraEntity {
    public FlowerHashiraEntity(EntityType<? extends HashiraEntity> pEntityType, Level level) {
        super(pEntityType, level);
        this.setBreathingStyle(BreathingStyles.FLOWER);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        this.setItemSlot(EquipmentSlot.MAINHAND, NichirinSwordItem.createSword(SwordModel.FLOWER));
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }
}
