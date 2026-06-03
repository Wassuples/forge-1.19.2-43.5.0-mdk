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
 * NPC entity: Jigoro Kuwajima — non-Hashira Thunder Breathing master.
 *
 * <p>Located in a remote mountain village. Requires rank Kanoto; no Corps Rep.
 * Travel distance is the primary gate.
 */
public class JigoroEntity extends StyleMasterNpcEntity {

    public JigoroEntity(EntityType<? extends StyleMasterNpcEntity> type, Level level) {
        super(type, level);
        this.setMasterId("jigoro_kuwajima");
        this.setCustomName(Component.literal("Jigoro Kuwajima"));
    }

    @Override
    public BreathingStyles getMasterStyle() { return BreathingStyles.THUNDER; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }
}
