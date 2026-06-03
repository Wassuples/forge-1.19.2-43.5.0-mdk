package com.wassuples.demonslayer.entity.custom.master;

import com.wassuples.demonslayer.entity.custom.StyleMasterNpcEntity;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;

/**
 * NPC entity: Shinjuro Rengoku — non-Hashira Flame Breathing master.
 *
 * <p>Located in the inner room of the Flame Hashira Estate. Requires having met Rengoku
 * ({@link com.wassuples.demonslayer.reputation.HardlineFlag#MET_RENGOKU}) and Corps Rep &ge; 1.
 * He abandoned the style and can accelerate only the first three forms.
 */
public class ShinjuroEntity extends StyleMasterNpcEntity {

    public ShinjuroEntity(EntityType<? extends StyleMasterNpcEntity> type, Level level) {
        super(type, level);
        this.setMasterId("shinjuro_rengoku");
        this.setCustomName(Component.literal("Shinjuro Rengoku"));
    }

    @Override
    public BreathingStyles getMasterStyle() { return BreathingStyles.FLAME; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }
}
