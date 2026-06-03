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
 * NPC entity: Tengen Uzui (retired) — non-Hashira Sound Breathing master.
 *
 * <p>Located in the Entertainment District safe house, available post-Mission 11.
 * Requires Corps Rep &ge; 1 only; no rank requirement. Lower access bar than the active Hashira path.
 */
public class TengenRetiredEntity extends StyleMasterNpcEntity {

    public TengenRetiredEntity(EntityType<? extends StyleMasterNpcEntity> type, Level level) {
        super(type, level);
        this.setMasterId("tengen_uzui_retired");
        this.setCustomName(Component.literal("Tengen Uzui (Retired)"));
    }

    @Override
    public BreathingStyles getMasterStyle() { return BreathingStyles.SOUND; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }
}
