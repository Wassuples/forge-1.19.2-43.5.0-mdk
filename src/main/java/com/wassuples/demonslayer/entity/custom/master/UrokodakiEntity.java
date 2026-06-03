package com.wassuples.demonslayer.entity.custom.master;

import com.wassuples.demonslayer.entity.ModEntityTypes;
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
 * NPC entity: Sakonji Urokodaki — non-Hashira Water Breathing master.
 *
 * <p>Fixed at Mt. Sagiri. No rank requirement; Tanjiro affinity &ge; 100 grants a
 * warm-up dialogue waiver. Requires Mission 0 completion (checked in event handler).
 */
public class UrokodakiEntity extends StyleMasterNpcEntity {

    public UrokodakiEntity(EntityType<? extends StyleMasterNpcEntity> type, Level level) {
        super(type, level);
        this.setMasterId("sakonji_urokodaki");
        this.setCustomName(Component.literal("Sakonji Urokodaki"));
    }

    @Override
    public BreathingStyles getMasterStyle() { return BreathingStyles.WATER; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }
}
