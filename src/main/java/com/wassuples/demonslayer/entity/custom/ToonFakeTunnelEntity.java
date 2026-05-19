package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

public class ToonFakeTunnelEntity extends Entity {
    private static final int MAX_LIFE_TICKS = 20 * 12;
    private int lifeTicks = MAX_LIFE_TICKS;
    @Nullable
    private UUID ownerUUID;
    private Vec3 facing = Vec3.ZERO;

    public ToonFakeTunnelEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ToonFakeTunnelEntity(Level level, Entity owner, Vec3 facing) {
        this(ModEntityTypes.TOON_FAKE_TUNNEL.get(), level);
        this.ownerUUID = owner.getUUID();
        this.facing = facing.normalize();
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide) {
            if (--this.lifeTicks <= 0) {
                this.discard();
                return;
            }

            AABB box = this.getBoundingBox().inflate(1.0, 1.5, 1.0);
            for (LivingEntity target : this.level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (this.ownerUUID != null && this.ownerUUID.equals(target.getUUID())) {
                    continue;
                }

                Vec3 push = this.facing.scale(-0.8).add(0, 0.2, 0);
                target.setDeltaMovement(push);
                target.hurtMarked = true;
                target.hurt(net.minecraft.world.damagesource.DamageSource.GENERIC, 3.0f);
                ToonForceHelper.tryApplyDizzy(target, null, 0.25f);
                this.level.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
        } else if (this.tickCount % 3 == 0) {
            this.level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    this.getX() + (this.random.nextDouble() - 0.5) * 1.2,
                    this.getY() + this.random.nextDouble() * 1.4,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 1.2,
                    0, 0.02, 0);
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        this.facing = new Vec3(tag.getDouble("FacingX"), tag.getDouble("FacingY"), tag.getDouble("FacingZ"));
        this.lifeTicks = tag.getInt("LifeTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putDouble("FacingX", this.facing.x);
        tag.putDouble("FacingY", this.facing.y);
        tag.putDouble("FacingZ", this.facing.z);
        tag.putInt("LifeTicks", this.lifeTicks);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

