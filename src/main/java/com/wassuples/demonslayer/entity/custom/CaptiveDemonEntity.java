package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket;
import com.wassuples.demonslayer.particle.ModParticles;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneLayout;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneSites;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A demon caught alive by the Corps and penned on Mt. Fujikasane's mountaintop for the Final Selection. Starving,
 * vicious and weak on its own (most have eaten one or two people). Not part of the Nemesis hierarchy: these demons
 * can never leave the mountain.
 *
 * <ul>
 *   <li>Many looks ({@link #VARIANT}: 0-3 and 5-{@value #LAST_SKIN}); {@link #HORNED} is the side quest's big horned
 *       demon.</li>
 *   <li>No two quite alike: each is a lean, ordinary or brutish build ({@link #BUILD}) with its own size, plus a random
 *       spread on health, strength and speed; all of it grows with the night of the selection ({@link #setTier}).</li>
 *   <li>Wisteria: they keep away from the summit's edge (wandering, and giving up chases, well short of it), and are
 *       seared and driven back if they get too close.</li>
 *   <li>Sunlight: they crumble to ash at once, quietly.</li>
 *   <li>Death: the body breaks apart into a column of ash with a dry crumbling sound — unmistakable.</li>
 * </ul>
 */
public class CaptiveDemonEntity extends Monster {
    public static final int HORNED = 4, LAST_SKIN = 20;
    public static final int LEAN = 0, ORDINARY = 1, BRUTE = 2;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CaptiveDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BUILD = SynchedEntityData.defineId(CaptiveDemonEntity.class, EntityDataSerializers.INT);
    /** Keep this far inside the summit's edge when wandering or chasing. */
    private static final int EDGE_MARGIN = 8;
    private int tier;
    /** Set when the sun took it (so that death isn't credited to anyone). */
    private boolean burnedBySun;
    /** Mirrors the variant/build size; getDimensions can run before synced data exists. */
    private float scale;

    public CaptiveDemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
        this.entityData.define(BUILD, ORDINARY);
    }

    public boolean diedToSun() {
        return burnedBySun;
    }

    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public int build() {
        return this.entityData.get(BUILD);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, v);
        refreshScale();
    }

    /** A random ordinary look (never the horned demon's). */
    public static int randomSkin(RandomSource random) {
        int v = random.nextInt(LAST_SKIN);   // 0..19, skipping HORNED
        return v >= HORNED ? v + 1 : v;
    }

    private void refreshScale() {
        this.scale = scaleFor(variant(), build());
        this.refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (VARIANT.equals(key) || BUILD.equals(key)) {
            this.scale = scaleFor(variant(), build());
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(scale <= 0.0F ? 1.0F : scale);
    }

    public static float scaleFor(int variant, int build) {
        if (variant == HORNED) return 1.45F;
        float base = 0.94F + (variant % 5) * 0.035F;
        return base * (build == BRUTE ? 1.14F : build == LEAN ? 0.92F : 1.0F);
    }

    /**
     * Scales the demon to the night of the selection (1..7) with a random build and spread, so no two are the same;
     * the horned demon is a few nights tougher still.
     */
    public void setTier(int night) {
        this.tier = night;
        RandomSource r = this.random;
        int build = variant() == HORNED ? BRUTE : (r.nextInt(10) < 3 ? LEAN : r.nextInt(10) < 3 ? BRUTE : ORDINARY);
        this.entityData.set(BUILD, build);
        refreshScale();
        double spread = 0.85 + r.nextDouble() * 0.35;
        double hp = (18 + 4.0 * night) * spread, dmg = (2.5 + 0.5 * night) * (0.9 + r.nextDouble() * 0.25), speed = 0.27 + 0.005 * night;
        switch (build) {
            case LEAN -> {
                hp *= 0.7;
                dmg *= 0.9;
                speed *= 1.22;
            }
            case BRUTE -> {
                hp *= 1.6;
                dmg *= 1.35;
                speed *= 0.86;
            }
            default -> {
            }
        }
        if (variant() == HORNED) {
            hp = 110 + 15 * night;
            dmg = 7 + night;
            speed = 0.27;
            this.getAttribute(Attributes.ARMOR).setBaseValue(8.0D);
        }
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dmg);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        this.setHealth(this.getMaxHealth());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ShunWisteriaGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(6, new SummitStrollGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, CaptiveDemonEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                p -> !((Player) p).isCreative() && !p.isSpectator() && insideSummit(p)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, StoryNpcEntity.class, true,
                e -> ((StoryNpcEntity) e).character().fighter() && insideSummit(e)));
    }

    // ------------------------------------------------------------------ the wisteria line

    @Nullable
    private FujikasaneLayout layout() {
        if (this.level.isClientSide || this.level.dimension() != Level.OVERWORLD || this.level.getServer() == null) return null;
        return FujikasaneSites.known(this.level.getServer());
    }

    /** Distance from the mountain's centre (or -1 when there is no mountain / not on it). */
    private double radius(LivingEntity e) {
        FujikasaneLayout layout = layout();
        if (layout == null) return -1;
        double r = FujikasaneLayout.dist(layout.dx(e.blockPosition()), layout.dz(e.blockPosition()));
        return r > FujikasaneLayout.R + 16 ? -1 : r;
    }

    /** Whether {@code e} stands where a demon dares to follow: the summit, short of the wisteria. */
    private boolean insideSummit(LivingEntity e) {
        double r = radius(e);
        return r < 0 || r < FujikasaneLayout.DEMON_R - 2;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide || !this.isAlive()) return;
        // daylight: crumble to ash on the spot, quietly
        if (this.level.isDay() && this.level.canSeeSky(this.blockPosition())) {
            burnedBySun = true;
            ((ServerLevel) this.level).sendParticles(ModParticles.DEMON_ASH.get(), getX(), getY() + getBbHeight() * 0.5, getZ(),
                    30, 0.3, getBbHeight() * 0.35, 0.3, 0.02);
            this.kill();   // a real death (quests see it), but no column or crumble: die() checks burnedBySun
            return;
        }
        if (this.tickCount % 10 == 0) {
            LivingEntity t = this.getTarget();
            if (t != null && !insideSummit(t)) this.setTarget(null);   // it won't chase into the wisteria
            double r = radius(this);
            if (r >= FujikasaneLayout.DEMON_R && r < FujikasaneLayout.R + 16) sear();
        }
        if (this.getTarget() != null && this.random.nextInt(600) == 0) mutter();
    }

    /** Pushed over the line anyway: the wisteria burns it and it recoils. */
    private void sear() {
        FujikasaneLayout layout = layout();
        if (layout == null) return;
        this.hurt(DamageSource.MAGIC, 2.0F);
        Vec3 back = new Vec3(-layout.dx(this.blockPosition()), 0, -layout.dz(this.blockPosition())).normalize().scale(0.6);
        this.setDeltaMovement(back.x, 0.25, back.z);
        ((ServerLevel) this.level).sendParticles(ModParticles.WISTERIA_PETAL.get(), getX(), getY() + 1.2, getZ(), 6, 0.4, 0.5, 0.4, 0.0);
    }

    private void mutter() {
        int line = this.random.nextInt(8);
        ModMessages.sendToTrackingAndSelf(new NpcSpeechS2CPacket(this.getId(),
                Component.translatable("npc.demonslayer.captive_demon.mutter." + line), 70), this);
    }

    // ------------------------------------------------------------------ death: a column of ash

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.dead) return;   // the death was cancelled
        if (this.level instanceof ServerLevel server && !burnedBySun) {
            double h = getBbHeight();
            for (int i = 0; i < 6; i++) {
                server.sendParticles(ModParticles.DEMON_ASH.get(), getX(), getY() + h * i / 6.0, getZ(), 14, 0.28, 0.12, 0.28, 0.035);
            }
            server.playSound(null, getX(), getY(), getZ(), ModSounds.DEMON_CRUMBLE.get(), SoundSource.HOSTILE, 0.7F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    @Override
    protected void tickDeath() {
        // the body is gone the instant it dies: what's left is the ash
        ++this.deathTime;
        if (this.deathTime >= 2 && !this.level.isClientSide) this.remove(RemovalReason.KILLED);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        if (source.getEntity() != null) super.playHurtSound(source);   // only blows from someone, not wisteria or fire
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.DEMON_HISS.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;   // the crumble plays from die()
    }

    @Override
    protected float getSoundVolume() {
        return 0.45F;
    }

    @Override
    public float getVoicePitch() {
        return (variant() == HORNED || build() == BRUTE ? 0.75F : 1.05F) + (this.random.nextFloat() - 0.5F) * 0.3F;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return variant() != HORNED && distance > 96 * 96;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
        tag.putInt("Build", build());
        tag.putInt("Tier", tier);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, tag.getInt("Variant"));
        this.entityData.set(BUILD, tag.contains("Build") ? tag.getInt("Build") : ORDINARY);
        refreshScale();
        tier = tag.getInt("Tier");
    }

    // ================================================================== goals

    /** Near the wisteria line: get back towards the middle of the summit. */
    static class ShunWisteriaGoal extends Goal {
        private final CaptiveDemonEntity demon;

        ShunWisteriaGoal(CaptiveDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        /** Game time before which no new path is tried (a failed path falls back to a shove inward). */
        private long retryAt;

        @Override
        public boolean canUse() {
            double r = demon.radius(demon);
            if (!(r > FujikasaneLayout.DEMON_R - EDGE_MARGIN / 2.0 && r < FujikasaneLayout.R + 16)) return false;
            if (demon.level.getGameTime() < retryAt) {
                // no path last time: nudge it inward instead of re-pathing every tick
                FujikasaneLayout layout = demon.layout();
                if (layout != null && demon.isOnGround() && demon.tickCount % 10 == 0) {
                    Vec3 in = new Vec3(-layout.dx(demon.blockPosition()), 0, -layout.dz(demon.blockPosition())).normalize().scale(0.25);
                    demon.setDeltaMovement(in.x, demon.getDeltaMovement().y, in.z);
                }
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            double r = demon.radius(demon);
            return r > FujikasaneLayout.DEMON_R - EDGE_MARGIN && r < FujikasaneLayout.R + 16 && !demon.getNavigation().isDone();
        }

        @Override
        public void start() {
            FujikasaneLayout layout = demon.layout();
            if (layout == null) return;
            int dx = layout.dx(demon.blockPosition()), dz = layout.dz(demon.blockPosition());
            double r = Math.max(1, FujikasaneLayout.dist(dx, dz));
            double k = (FujikasaneLayout.DEMON_R - EDGE_MARGIN - 10) / r;
            if (!demon.getNavigation().moveTo(layout.centerX + dx * k, demon.getY(), layout.centerZ + dz * k, 1.1D)) {
                retryAt = demon.level.getGameTime() + 40;
            }
        }

        @Override
        public void stop() {
            if (demon.radius(demon) > FujikasaneLayout.DEMON_R - EDGE_MARGIN) retryAt = demon.level.getGameTime() + 40;
        }
    }

    /** Idle wandering that stays well inside the summit, spread over it. */
    static class SummitStrollGoal extends Goal {
        private final CaptiveDemonEntity demon;

        SummitStrollGoal(CaptiveDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (demon.getTarget() != null || demon.random.nextInt(60) != 0) return false;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return demon.getTarget() == null && !demon.getNavigation().isDone();
        }

        @Override
        public void start() {
            FujikasaneLayout layout = demon.layout();
            double ang = demon.random.nextDouble() * Math.PI * 2, dist = 6 + demon.random.nextDouble() * 12;
            double x = demon.getX() + Math.cos(ang) * dist, z = demon.getZ() + Math.sin(ang) * dist;
            if (layout != null) {
                double dx = x - layout.centerX, dz = z - layout.centerZ, r = Math.sqrt(dx * dx + dz * dz);
                double max = FujikasaneLayout.DEMON_R - EDGE_MARGIN;
                if (r > max) {
                    x = layout.centerX + dx / r * max;
                    z = layout.centerZ + dz / r * max;
                }
            }
            demon.getNavigation().moveTo(x, demon.getY(), z, 0.8D);
        }
    }
}
