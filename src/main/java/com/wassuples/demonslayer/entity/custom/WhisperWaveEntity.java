package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.effects.ForcedSleepEffect;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.StartSleepQTES2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Enmu's Whispers of Forced Unconscious Hypnosis, made visible: a mouth opens on the back of Enmu's raised hand
 * and its whisper ("sleep...") rolls toward the target as dreamy pink-violet ripples. When the whisper reaches
 * the target, the Forced Sleep effect is applied exactly as before (same duration, same QTE for players).
 * Visual-only otherwise; never saved.
 */
public class WhisperWaveEntity extends Entity {
    /** Blocks per tick; fast enough that it still feels like an instant spell (≤ 8 ticks at max range). */
    public static final double SPEED = 2.2;

    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(WhisperWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(WhisperWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ARRIVED_TICK = SynchedEntityData.defineId(WhisperWaveEntity.class, EntityDataSerializers.INT);

    private int sleepDuration;

    public WhisperWaveEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public WhisperWaveEntity(Level level, LivingEntity caster, LivingEntity target, int sleepDuration) {
        this(ModEntityTypes.WHISPER_WAVE.get(), level);
        this.entityData.set(CASTER_ID, caster.getId());
        this.entityData.set(TARGET_ID, target.getId());
        this.sleepDuration = sleepDuration;
        this.setPos(caster.getX(), caster.getEyeY(), caster.getZ());
    }

    @Nullable
    public Entity getCaster() {
        return this.level.getEntity(this.entityData.get(CASTER_ID));
    }

    @Nullable
    public Entity getTarget() {
        return this.level.getEntity(this.entityData.get(TARGET_ID));
    }

    /** Tick the whisper reached its target (-1 while still travelling). */
    public int getArrivedTick() {
        return this.entityData.get(ARRIVED_TICK);
    }

    /** Distance the whisper front has travelled at {@code age}. */
    public double travelled(float age) {
        return Math.max(0.0, (age - 3.0) * SPEED); // 3-tick wind-up while the hand-mouth opens
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;

        Entity caster = getCaster();
        Entity target = getTarget();
        if (caster != null) {
            this.setPos(caster.getX(), caster.getEyeY(), caster.getZ()); // keeps tracking/culling near the action
        }
        if (this.level.isClientSide) return;

        if (caster == null || target == null || !target.isAlive()) {
            this.discard();
            return;
        }
        if (getArrivedTick() < 0) {
            double distance = caster.getEyePosition().distanceTo(target.getEyePosition());
            if (travelled(this.tickCount) >= distance) {
                this.entityData.set(ARRIVED_TICK, this.tickCount);
                if (target instanceof LivingEntity living && caster instanceof LivingEntity livingCaster) {
                    lullToSleep((ServerLevel) this.level, livingCaster, living);
                }
            }
        } else if (this.tickCount - getArrivedTick() > 12) {
            this.discard();
        }
    }

    /** Same effect the form always applied, just at the moment the whisper lands. */
    private void lullToSleep(ServerLevel level, LivingEntity caster, LivingEntity target) {
        target.addEffect(new MobEffectInstance(ModEffects.FORCED_SLEEP.get(), this.sleepDuration, 0));
        ForcedSleepEffect.setCaster(target, caster);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.6f);
        if (target instanceof ServerPlayer sleepingPlayer) {
            List<Integer> keys = new ArrayList<>(List.of(0, 1, 2, 3)); // 0=up, 1=left, 2=down, 3=right
            Collections.shuffle(keys);
            ModMessages.sendToPlayer(new StartSleepQTES2CPacket(keys), sleepingPlayer);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 80 * 80;
    }

    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        Entity target = getTarget();
        return target == null ? this.getBoundingBox().inflate(2) : this.getBoundingBox().minmax(target.getBoundingBox()).inflate(1.5);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CASTER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(ARRIVED_TICK, -1);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /** Helper for renderers: where the whisper starts (the caster's raised right hand). */
    public static Vec3 handPosition(Entity caster, float partialTick) {
        float yaw = (caster instanceof LivingEntity l
                ? net.minecraft.util.Mth.rotLerp(partialTick, l.yBodyRotO, l.yBodyRot) : caster.getYRot()) * net.minecraft.util.Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        float scale = caster.getBbHeight() / 1.8f;
        return caster.getPosition(partialTick).add(0, 1.5 * scale, 0).add(forward.scale(0.7 * scale)).add(right.scale(0.36 * scale));
    }
}
