package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ToonShadowAnchorEntity extends Entity implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private static final int MAX_LIFE_TICKS = 20 * 30;
    private int lifeTicks = MAX_LIFE_TICKS;
    private boolean stuck = false;
    private Vec3 stuckNormal = Vec3.ZERO;
    @Nullable
    private BlockPos stuckBlockPos;
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
                    this.stuckBlockPos = pos;
                    this.stuckNormal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
                    Vec3 hitPos = hit.getLocation().add(this.stuckNormal.scale(0.08));
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
    public Vec3 findSafeTeleportPosition(Entity teleported) {
        if (!this.stuck) {
            return null;
        }

        List<Vec3> candidates = new ArrayList<>();
        Vec3 normal = this.stuckNormal.lengthSqr() > 0.001 ? this.stuckNormal.normalize() : Vec3.ZERO;

        if (this.stuckBlockPos != null && normal.y > 0.5) {
            candidates.add(Vec3.atBottomCenterOf(this.stuckBlockPos.above()));
        }

        if (normal.lengthSqr() > 0.001) {
            double offset = teleported.getBbWidth() * 0.5 + 0.42;
            Vec3 outsideFace = this.position().add(normal.scale(offset));
            addVerticalCandidates(candidates, outsideFace);
        }

        if (this.stuckBlockPos != null) {
            candidates.add(Vec3.atBottomCenterOf(this.stuckBlockPos.above()));
            addVerticalCandidates(candidates, Vec3.atBottomCenterOf(this.stuckBlockPos).add(normal.scale(0.9)));
        }

        for (Vec3 candidate : candidates) {
            if (isSafeTeleportPosition(teleported, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static void addVerticalCandidates(List<Vec3> candidates, Vec3 base) {
        int baseY = net.minecraft.util.Mth.floor(base.y);
        for (int y = baseY + 1; y >= baseY - 4; y--) {
            candidates.add(new Vec3(base.x, y, base.z));
        }
        for (int y = baseY + 2; y <= baseY + 4; y++) {
            candidates.add(new Vec3(base.x, y, base.z));
        }
    }

    private boolean isSafeTeleportPosition(Entity teleported, Vec3 candidate) {
        return this.level.noCollision(teleported, teleported.getBoundingBox().move(candidate.subtract(teleported.position())));
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
        this.stuckNormal = new Vec3(tag.getDouble("NormalX"), tag.getDouble("NormalY"), tag.getDouble("NormalZ"));
        if (tag.contains("StuckBlockX")) {
            this.stuckBlockPos = new BlockPos(tag.getInt("StuckBlockX"), tag.getInt("StuckBlockY"), tag.getInt("StuckBlockZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putBoolean("Stuck", this.stuck);
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putDouble("NormalX", this.stuckNormal.x);
        tag.putDouble("NormalY", this.stuckNormal.y);
        tag.putDouble("NormalZ", this.stuckNormal.z);
        if (this.stuckBlockPos != null) {
            tag.putInt("StuckBlockX", this.stuckBlockPos.getX());
            tag.putInt("StuckBlockY", this.stuckBlockPos.getY());
            tag.putInt("StuckBlockZ", this.stuckBlockPos.getZ());
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));
        return PlayState.CONTINUE;
    }
    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
