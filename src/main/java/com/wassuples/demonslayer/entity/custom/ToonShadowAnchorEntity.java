package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

public class ToonShadowAnchorEntity extends Entity {
    private static final int MAX_LIFE_TICKS = 20 * 30;
    private int lifeTicks = MAX_LIFE_TICKS;
    private boolean stuck = false;
    @Nullable
    private UUID ownerUUID;

    public ToonShadowAnchorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ToonShadowAnchorEntity(Level level, Entity owner) {
        this(ModEntityTypes.TOON_SHADOW_ANCHOR.get(), level);
        this.ownerUUID = owner.getUUID();
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide) {
            if (--this.lifeTicks <= 0) {
                this.discard();
                return;
            }

            if (!this.stuck) {
                Vec3 motion = this.getDeltaMovement();
                Vec3 start = this.position();
                Vec3 end = start.add(motion);
                BlockHitResult hit = this.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = hit.getBlockPos();
                    Vec3 hitPos = Vec3.atCenterOf(pos).add(hit.getDirection().getNormal().getX() * 0.2,
                            hit.getDirection().getNormal().getY() * 0.2,
                            hit.getDirection().getNormal().getZ() * 0.2);
                    this.setPos(hitPos);
                    this.setDeltaMovement(Vec3.ZERO);
                    this.stuck = true;
                } else {
                    this.move(MoverType.SELF, motion);
                }
            }
        }

        if (this.level.isClientSide && this.tickCount % 2 == 0) {
            this.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.1, this.getZ(), 0, 0, 0);
        }
    }

    public boolean isStuck() {
        return this.stuck;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        this.stuck = tag.getBoolean("Stuck");
        this.lifeTicks = tag.getInt("LifeTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putBoolean("Stuck", this.stuck);
        tag.putInt("LifeTicks", this.lifeTicks);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

