package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.util.ToonForceHelper;
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

public class ToonBananaPeelEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE_TICKS = 20 * 20;
    private int lifeTicks = MAX_LIFE_TICKS;
    private UUID ownerUUID;

    public ToonBananaPeelEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public ToonBananaPeelEntity(Level level, LivingEntity owner) {
        super(ModEntityTypes.TOON_BANANA_PEEL.get(), owner, level);
        this.ownerUUID = owner.getUUID();
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.YELLOW_DYE;
    }

    @Override
    public void tick() {
        this.baseTick();

        if (!this.level.isClientSide) {
            if (--this.lifeTicks <= 0) {
                this.discard();
                return;
            }

            for (LivingEntity target : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.3))) {
                if (target.getUUID().equals(this.ownerUUID) || target.isPassenger()) {
                    // Owner can still slip, but give them a tiny grace radius to avoid instant self-slip.
                    if (this.tickCount < 4) {
                        continue;
                    }
                }

                if (target.isAlive()) {
                    ToonForceHelper.applyBananaSlip(target, this.getOwner() instanceof LivingEntity owner ? owner : null, 0.65f);
                    this.level.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.9f, 1.2f);
                    this.discard();
                    break;
                }
            }
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

