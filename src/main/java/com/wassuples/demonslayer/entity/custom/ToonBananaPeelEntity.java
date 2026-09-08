package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.util.FormAnimations;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.ArrayList;
import java.util.List;

public class ToonBananaPeelEntity extends ThrowableItemProjectile implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private static final int MAX_LIFE_TICKS = 20 * 60; // 1 minute
    private static final int OWNER_GRACE_TICKS = 20;
    private int lifeTicks = MAX_LIFE_TICKS;
    private UUID ownerUUID;

    public ToonBananaPeelEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public ToonBananaPeelEntity(Level level, LivingEntity owner) {
        super(ModEntityTypes.TOON_BANANA_PEEL.get(), owner, level);
        this.ownerUUID = owner.getUUID();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.YELLOW_DYE;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide) {
            this.setYRot(this.getYRot() + 25.0f);
            if (this.tickCount % 3 == 0) {
                this.level.addParticle(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
                        this.getX(), this.getY() + 0.04, this.getZ(), 0, 0, 0);
            }
        }

        if (!this.level.isClientSide) {
            if (--this.lifeTicks <= 0) {
                this.discard();
                return;
            }

            for (LivingEntity target : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.3))) {
                if (target.isPassenger()) {
                    continue;
                }

                if (target.getUUID().equals(this.ownerUUID) && this.tickCount < OWNER_GRACE_TICKS) {
                    continue;
                }

                if (target.isAlive()) {
                    List<ToonBananaPeelEntity> stackedPeels = new ArrayList<>(this.level.getEntitiesOfClass(ToonBananaPeelEntity.class, target.getBoundingBox().inflate(0.9)));
                    int stackCount = 1;
                    for (ToonBananaPeelEntity peel : stackedPeels) {
                        if (peel == this || !peel.isAlive()) {
                            continue;
                        }
                        stackCount++;
                        peel.discard();
                    }

                    float launchPower = 0.65f + (stackCount - 1) * 0.18f;
                    ToonForceHelper.applyBananaSlip(target, this.getOwner() instanceof LivingEntity owner ? owner : null, launchPower);
                    ToonForceHelper.spawnToonText((net.minecraft.server.level.ServerLevel) this.level, target.position().add(0, target.getBbHeight(), 0),
                            Component.literal(stackCount > 1 ? "WHIFF x" + stackCount + "!" : "WHIFF!").withStyle(ChatFormatting.YELLOW), 24);
                    this.level.playSound(null, blockPosition(), ModSounds.TOON_SLIP.get(), SoundSource.PLAYERS, 0.9f, 1.1f);
                    this.discard();
                    break;
                }
            }
        }
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult hitResult) {
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            this.setNoGravity(true);
            return;
        }
        super.onHit(hitResult);
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> getAddEntityPacket() {
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
