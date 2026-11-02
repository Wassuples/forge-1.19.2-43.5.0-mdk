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

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> OWNER_ID =
            net.minecraft.network.syncher.SynchedEntityData.defineId(ToonShadowAnchorEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> TOSS_YAW =
            net.minecraft.network.syncher.SynchedEntityData.defineId(ToonShadowAnchorEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> STUCK =
            net.minecraft.network.syncher.SynchedEntityData.defineId(ToonShadowAnchorEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> STUCK_FACE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(ToonShadowAnchorEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    public ToonShadowAnchorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ToonShadowAnchorEntity(Level level, Entity owner) {
        this(ModEntityTypes.TOON_SHADOW_ANCHOR.get(), level);
        this.ownerUUID = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(TOSS_YAW, owner.getYRot());
    }

    /** Network id of the owner, so clients can draw their real silhouette and hide their real shadow. */
    public int getOwnerEntityId() {
        return this.entityData.get(OWNER_ID);
    }

    public float getTossYaw() {
        return this.entityData.get(TOSS_YAW);
    }

    @Nullable
    public Entity getOwnerEntity() {
        return this.level.getEntity(getOwnerEntityId());
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
                    this.entityData.set(STUCK, true);
                    this.entityData.set(STUCK_FACE, hit.getDirection().get3DDataValue());
                } else {
                    this.move(MoverType.SELF, motion);
                }
            }
        }

        if (this.level.isClientSide) {
            // While the shadow is away, its owner casts none (see ToonShadowSuppressMixin).
            int ownerId = getOwnerEntityId();
            if (ownerId >= 0) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.wassuples.demonslayer.client.toon.ToonShadowClient.markTossed(ownerId));
            }
            if (!isStuck() && this.tickCount % 2 == 0) {
                this.level.addParticle(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                        this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        }
    }

    public boolean isStuck() {
        return this.level.isClientSide ? this.entityData.get(STUCK) : this.stuck;
    }

    public Vec3 getStuckNormal() {
        if (this.level.isClientSide) {
            return Vec3.atLowerCornerOf(net.minecraft.core.Direction.from3DDataValue(this.entityData.get(STUCK_FACE)).getNormal());
        }
        return this.stuckNormal;
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
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TOSS_YAW, 0.0f);
        this.entityData.define(STUCK, false);
        this.entityData.define(STUCK_FACE, net.minecraft.core.Direction.UP.get3DDataValue());
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
