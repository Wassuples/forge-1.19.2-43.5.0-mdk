package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamageExcluding;

public class BodhisattvaEntity extends Monster implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    private static final EntityDataAccessor<Optional<UUID>> PILOT_UUID = SynchedEntityData.defineId(BodhisattvaEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> ANIMATION_STATE = SynchedEntityData.defineId(BodhisattvaEntity.class, EntityDataSerializers.INT);

    private int lifeTicks = 1200;
    public int attackCooldown = 0;

    public BodhisattvaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.maxUpStep = 2.5f;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PILOT_UUID, Optional.empty());
        this.entityData.define(ANIMATION_STATE, 0);
    }

    @Override
    public @NotNull Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void setPilot(@Nullable LivingEntity pilot) {
        if (pilot != null) {
            this.entityData.set(PILOT_UUID, Optional.of(pilot.getUUID()));
        } else {
            this.entityData.set(PILOT_UUID, Optional.empty());
        }
    }

    @Nullable
    public LivingEntity getPilot() {
        if (!this.level.isClientSide) {
            return (LivingEntity) this.entityData.get(PILOT_UUID)
                    .map(uuid -> ((ServerLevel) this.level).getEntity(uuid))
                    .filter(entity -> entity instanceof LivingEntity).orElse(null);
        }
        return null;
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource pSource) {
        return pSource == DamageSource.IN_WALL || pSource == DamageSource.FALL || (pSource.getEntity() != null && pSource.getEntity().getUUID().equals(this.entityData.get(PILOT_UUID).orElse(null))) || super.isInvulnerableTo(pSource);
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return !this.isRemoved(); }

    @Override
    public void tick() {
        super.tick();
        LivingEntity pilot = getPilot();

        if (!this.level.isClientSide) {
            if (this.tickCount > 5 && (pilot == null || !pilot.isAlive())) {
                this.shatter();
                return;
            }
            if (this.lifeTicks-- <= 0) {
                this.shatter();
                return;
            }

            if (attackCooldown > 0) {
                attackCooldown--;
                if (getAnimationState() == 3 && attackCooldown == 60) {
                    handleSwipeDamage(pilot);
                }
                if (attackCooldown == 0) {
                    setAnimationState(0);
                }
            }

            if (getAnimationState() == 2) {
                handleBreathDamageAndParticles(pilot);
            }

            if (pilot != null) {
                this.setYRot(pilot.getYRot());
                this.setXRot(pilot.getXRot());
            }
        }
    }

    @Override
    public void positionRider(@NotNull Entity pPassenger) {
        if (this.hasPassenger(pPassenger)) {
            // This calculates a point on the right shoulder.
            // Adjust the numbers to move the seat exactly where you want it on your model.
            Vec3 rightShoulderOffset = new Vec3(1.5D, this.getBbHeight() * 0.85D, 0.0D)
                    .yRot(-this.getYRot() * ((float)Math.PI / 180F)); // Rotate the offset with the body

            // Set the passenger's position to our custom seat location.
            pPassenger.setPos(this.getX() + rightShoulderOffset.x, this.getY() + rightShoulderOffset.y, this.getZ() + rightShoulderOffset.z);
        }
    }


    public void moveWithInput(Vec3 input) {
        Vec3 move = input.yRot(-this.getYRot() * ((float)Math.PI / 180F));
        // Preserve current vertical movement (gravity) while applying new horizontal controls
        double currentY = this.getDeltaMovement().y;
        Vec3 scaledMove = move.scale(getAttributeValue(Attributes.MOVEMENT_SPEED));
        this.setDeltaMovement(scaledMove.x, currentY, scaledMove.z);

        if (input.lengthSqr() > 0 && this.attackCooldown == 0) {
            setAnimationState(1);
        } else if (this.attackCooldown == 0) {
            setAnimationState(0);
        }
    }

    @Override
    public void die(@NotNull DamageSource pDamageSource) {
        this.shatter();
        super.die(pDamageSource);
    }

    public void shatter() {
        if (this.getPilot() != null) {
            ActiveFormManager.stop(this.getPilot());
        }
        if (!this.level.isClientSide) {
            ((ServerLevel)this.level).sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                    this.getX(), this.getY(), this.getZ(), 500, this.getBbWidth() / 2.0, this.getBbHeight() / 2.0, this.getBbWidth() / 2.0, 0.5);
        }
        this.level.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.NEUTRAL, 2.0f, 1.0f);
        this.discard();
    }

    public void performBreathAttack() {
        if (this.attackCooldown > 0) return;
        setAnimationState(2);
        this.attackCooldown = 40;
        this.level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 1.5f);
    }

    public void performSwipeAttack() {
        if (this.attackCooldown > 0) return;
        setAnimationState(3);
        this.attackCooldown = 80;
        this.level.playSound(null, this.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 2.0f, 0.8f);
    }

    private void handleBreathDamageAndParticles(LivingEntity pilot) {
        if (pilot == null || this.level.isClientSide) return;
        for (int d = 1; d < 25; d++) {
            Vec3 lookDir = this.getLookAngle();
            Vec3 centerOfCone = this.getEyePosition().add(lookDir.scale(d));
            double widthOfCone = d * 0.7;
            // Attribute damage to the pilot but exclude this summon.
            dealAOEDamageExcluding(pilot, centerOfCone, widthOfCone, 2.0f, 0.2f, this);

            for (int i = 0; i < 15; i++) {
                Vec3 sideDir = new Vec3(-lookDir.z, 0, lookDir.x).normalize();
                Vec3 particlePos = centerOfCone.add(sideDir.scale((this.random.nextDouble() - 0.5) * (widthOfCone * 2)));
                particlePos = particlePos.add(0, (this.random.nextDouble() - 0.5) * 4.0, 0);
                ((ServerLevel) this.level).sendParticles(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 1, 0,0,0,0);
            }
        }
    }

    private void handleSwipeDamage(LivingEntity pilot) {
        if (pilot == null) return;

        this.level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.5f);
        this.level.playSound(null, this.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 2.0f, 0.8f);

        // --- NEW: Added Particle Effect for the Swipe ---
        if (this.level instanceof ServerLevel serverLevel) {
            Vec3 arcOrigin = this.getEyePosition().add(0, -this.getBbHeight() * 0.25, 0); // Start the arc from the statue's chest area
            float startYaw = this.getYRot() - 75; // Center a 150-degree arc in front

            // Loop through the angle of the arc
            for (int i = 0; i <= 150; i += 4) {
                float currentYaw = startYaw + i;
                double angleRad = Math.toRadians(-currentYaw); // Negative to match Minecraft's yaw system
                Vec3 direction = new Vec3(Math.sin(angleRad), 0, Math.cos(angleRad));

                // Spawn particles along the length of the swipe
                Vec3 particlePos = arcOrigin.add(direction.scale(8.0));
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, particlePos.x, particlePos.y, particlePos.z, 2, 0.5, 0.5, 0.5, 0.0);
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 5, 1.0, 1.0, 1.0, 0.1);
            }
        }

        // Damage logic remains the same
        Vec3 centerOfSwipe = this.getEyePosition().add(this.getLookAngle().scale(8.0));
        // Attribute damage to the pilot but exclude this summon.
        dealAOEDamageExcluding(pilot, centerOfSwipe, 8.0, 50.0f, 2.5f, this);
    }

    @Override
    public void removePassenger(@NotNull Entity pPassenger) {
        // First, call the super method to handle the default dismounting logic.
        super.removePassenger(pPassenger);

        // Now, add our custom logic.
        // If the entity that dismounted was our pilot, the statue shatters.
        if (!this.level.isClientSide && pPassenger.getUUID().equals(this.entityData.get(PILOT_UUID).orElse(null))) {
            this.shatter();
        }
    }

    @Override protected void registerGoals() {}

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        int state = getAnimationState();
        AnimationBuilder builder = new AnimationBuilder();
        switch (state) {
            case 1: builder.addAnimation("animation.bodhisattva.walk", true); break;
            case 2: builder.addAnimation("animation.bodhisattva.breath_attack", false); break;
            case 3: builder.addAnimation("animation.bodhisattva.swipe_attack", false); break;
            default: builder.addAnimation("animation.bodhisattva.idle", true); break;
        }
        event.getController().setAnimation(builder);
        return PlayState.CONTINUE;
    }

    @Override public void registerControllers(AnimationData data) { data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate)); }
    @Override public AnimationFactory getFactory() { return this.factory; }

    public int getAnimationState() { return this.entityData.get(ANIMATION_STATE); }
    public void setAnimationState(int state) { this.entityData.set(ANIMATION_STATE, state); }
}
