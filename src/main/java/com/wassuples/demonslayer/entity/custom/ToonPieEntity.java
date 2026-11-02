package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PieSplatS2CPacket;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.util.ToonSfx;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Pie Splat's cream pie. Purely visual and fully scripted from the server: it flies from the thrower's hand
 * into the attacker's face ({@link #FLIGHT_TICKS}), sticks there dripping ({@link #STUCK_TICKS}), then slides
 * off and drops. Everyone around sees it; a splatted player additionally gets the first-person overlay.
 */
public class ToonPieEntity extends Entity {
    public static final int FLIGHT_TICKS = 5;
    public static final int STUCK_TICKS = 60;
    public static final int SLIDE_TICKS = 12;
    public static final int TOTAL_TICKS = FLIGHT_TICKS + STUCK_TICKS + SLIDE_TICKS;

    private static final EntityDataAccessor<Integer> THROWER_ID = SynchedEntityData.defineId(ToonPieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(ToonPieEntity.class, EntityDataSerializers.INT);

    public ToonPieEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ToonPieEntity(Level level, LivingEntity thrower, LivingEntity target) {
        this(ModEntityTypes.TOON_PIE.get(), level);
        this.entityData.set(THROWER_ID, thrower.getId());
        this.entityData.set(TARGET_ID, target.getId());
        this.setPos(target.getX(), target.getEyeY(), target.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(THROWER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;

        Entity target = getTarget();
        if (target != null) {
            // Keep the entity (and so its tracking/culling box) on the victim's face.
            this.setPos(target.getX(), target.getEyeY() - 0.25, target.getZ());
        }

        if (this.level.isClientSide) return;

        if (target == null || !target.isAlive()) {
            this.discard();
            return;
        }
        if (this.tickCount == FLIGHT_TICKS && target instanceof LivingEntity living) {
            onSplat((ServerLevel) this.level, living);
        }
        if (this.tickCount >= TOTAL_TICKS) {
            this.discard();
        }
    }

    private void onSplat(ServerLevel level, LivingEntity target) {
        level.playSound(null, target.blockPosition(), ModSounds.TOON_SPLAT.get(), SoundSource.PLAYERS, 1.0f, 0.95f + level.random.nextFloat() * 0.15f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CAKE)),
                target.getX(), target.getEyeY(), target.getZ(), 22, 0.3, 0.25, 0.3, 0.12);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getEyeY(), target.getZ(), 16, 0.3, 0.3, 0.3, 0.08);
        ToonSfx.spawn(level, target.getEyePosition().add(0, 0.7, 0), ToonSfx.Kind.SPLAT, 1.15f);

        if (target instanceof ServerPlayer player) {
            ModMessages.sendToPlayer(new PieSplatS2CPacket(STUCK_TICKS + SLIDE_TICKS), player);
        } else {
            // Mobs can't see an overlay; a face full of cream still blinds them for a moment.
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, STUCK_TICKS, 0, true, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STUCK_TICKS / 2, 1, true, false, false));
        }
    }

    @Nullable
    public Entity getTarget() {
        return this.level.getEntity(this.entityData.get(TARGET_ID));
    }

    @Nullable
    public Entity getThrower() {
        return this.level.getEntity(this.entityData.get(THROWER_ID));
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
}
