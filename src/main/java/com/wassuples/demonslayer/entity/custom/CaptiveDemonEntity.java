package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.missions.IncenseManager;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket;
import com.wassuples.demonslayer.particle.ModParticles;
import com.wassuples.demonslayer.sound.ModSounds;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneLayout;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneSites;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
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
 *   <li><b>Looks</b>: {@link #VARIANT} picks one of {@value #SKINS} skins ({@link #HORNED} is the side quest's big horned
 *       demon) and its horns; {@link #BUILD} is a lean, ordinary or brutish body, each with its own model.</li>
 *   <li><b>Strength</b>: grows with the night ({@link #setTier}), with the build, with how many people it has eaten
 *       ({@link #eaten}, 1..6) and with a random spread, so no two hit or take hits alike.</li>
 *   <li><b>The wisteria</b>: they keep well inside the summit (wandering across all of it, never towards the edge),
 *       give up chases short of the wisteria, and are seared back if pushed over the line. Only a <i>frenzied</i> demon
 *       (sent after a candidate who cowers at the edge) braves the wisteria.</li>
 *   <li><b>Hunting</b>: the mountain's spawner ({@code FujikasaneDemons}) sends idle demons after a candidate's scent
 *       ({@link #huntTowards}); they climb walls like spiders, so towers and pillars are no refuge.</li>
 *   <li><b>Sunlight</b>: they burn away at once, without a sound. Those in deep shade live through the day, keep still
 *       and only snap at whoever comes close.</li>
 *   <li><b>Death</b>: beheaded, the head drops away and the body freezes, chars and crumbles into ash over a second
 *       and a half ({@link #DEATH_TICKS}), with one dry crumble. A hit is only a short hiss and a flinch.</li>
 * </ul>
 */
public class CaptiveDemonEntity extends Monster {
    public static final int HORNED = 4, SKINS = 32, LAST_SKIN = SKINS - 1;
    public static final int LEAN = 0, ORDINARY = 1, BRUTE = 2;
    /** Ticks a beheaded demon takes to crumble away; a sun death is quicker. */
    public static final int DEATH_TICKS = 32, SUN_DEATH_TICKS = 18;
    private static final int FLAG_CLIMBING = 1, FLAG_SUN = 2;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CaptiveDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BUILD = SynchedEntityData.defineId(CaptiveDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(CaptiveDemonEntity.class, EntityDataSerializers.BYTE);
    /** Demons give up a chase this far from the centre (the wisteria starts a few blocks past DEMON_R). */
    public static final int CHASE_LIMIT = FujikasaneLayout.DEMON_R - 6;
    /** Furthest out they wander or appear. */
    public static final int ROAM_LIMIT = FujikasaneLayout.DEMON_R - 14;
    private int tier;
    /** People this demon has eaten (1..6): a little bigger and stronger for each. */
    private int eaten = 1;
    private boolean frenzied;
    @Nullable
    private BlockPos huntPos;
    private long huntUntil;
    private int lastHiss = -100;
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
        this.entityData.define(FLAGS, (byte) 0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);   // they scale walls and pillars like spiders
    }

    // ------------------------------------------------------------------ looks & strength

    public boolean diedToSun() {
        return flag(FLAG_SUN);
    }

    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public int build() {
        return this.entityData.get(BUILD);
    }

    public int eaten() {
        return eaten;
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, Mth.clamp(v, 0, LAST_SKIN));
        refreshScale();
    }

    /** A random ordinary look (never the horned demon's). */
    public static int randomSkin(RandomSource random) {
        int v = random.nextInt(SKINS - 1);
        return v >= HORNED ? v + 1 : v;
    }

    private boolean flag(int bit) {
        return (this.entityData.get(FLAGS) & bit) != 0;
    }

    private void setFlag(int bit, boolean on) {
        byte b = this.entityData.get(FLAGS);
        this.entityData.set(FLAGS, (byte) (on ? b | bit : b & ~bit));
    }

    public boolean isClimbing() {
        return flag(FLAG_CLIMBING);
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
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

    /** Overall size: the build's model already differs in shape; this is the height on top of it. */
    public static float scaleFor(int variant, int build) {
        if (variant == HORNED) return 1.45F;
        float base = 0.95F + (variant % 5) * 0.025F;
        return base * (build == BRUTE ? 1.12F : build == LEAN ? 1.03F : 1.0F);
    }

    /**
     * Scales the demon to the night of the selection (1..7) with a random build, appetite and spread, so no two are the
     * same; the horned demon is a few nights tougher still.
     */
    public void setTier(int night) {
        this.tier = night;
        RandomSource r = this.random;
        int roll = r.nextInt(100);
        int build = variant() == HORNED ? BRUTE : roll < 30 ? LEAN : roll < 78 ? ORDINARY : BRUTE;
        this.entityData.set(BUILD, build);
        refreshScale();
        // most have eaten one or two people; a few have gorged on many more
        int[] appetite = {1, 1, 1, 2, 2, 2, 3, 3, 4, 5, 6};
        eaten = appetite[r.nextInt(appetite.length)] + (night >= 5 && r.nextInt(3) == 0 ? 1 : 0);
        double hunger = 1.0 + 0.07 * (eaten - 1);
        double hp = (16 + 3.5 * night) * (0.8 + r.nextDouble() * 0.4) * hunger;
        double dmg = (2.5 + 0.45 * night) * (0.85 + r.nextDouble() * 0.3) * hunger;
        double speed = (0.26 + 0.006 * night) * (0.92 + r.nextDouble() * 0.16);
        double armor = 1.0 + r.nextInt(3), knockback = 0.0;
        switch (build) {
            case LEAN -> {
                hp *= 0.7;
                dmg *= 0.85;
                speed *= 1.25;
                armor = 0;
            }
            case BRUTE -> {
                hp *= 1.7;
                dmg *= 1.4;
                speed *= 0.85;
                armor += 3;
                knockback = 0.5;
            }
            default -> {
            }
        }
        if (variant() == HORNED) {
            eaten = 6;
            hp = 110 + 15 * night;
            dmg = 7 + night;
            speed = 0.27;
            armor = 8.0;
            knockback = 0.7;
        }
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dmg);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        this.getAttribute(Attributes.ARMOR).setBaseValue(armor);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(knockback);
        this.setHealth(this.getMaxHealth());
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ShunWisteriaGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(4, new HuntGoal(this));
        this.goalSelector.addGoal(6, new SummitStrollGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, CaptiveDemonEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> !((Player) p).isCreative() && !p.isSpectator() && mayChase(p) && (!isDayTime() || p.distanceToSqr(this) < 8 * 8)));
        // fellow candidates are fought when met, not hunted across the summit (the player is the quarry)
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, StoryNpcEntity.class, 10, true, false,
                e -> ((StoryNpcEntity) e).character().fighter() && !((StoryNpcEntity) e).isHiding() && mayChase(e)
                        && e.distanceToSqr(this) < (isDayTime() ? 8 * 8 : 14 * 14)));
    }

    private boolean isDayTime() {
        return this.level.isDay();
    }

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

    /** Whether {@code e} stands where this demon dares to follow: well short of the wisteria (anywhere, if frenzied). */
    private boolean mayChase(LivingEntity e) {
        if (frenzied) return true;
        double r = radius(e);
        return r < 0 || r < CHASE_LIMIT;
    }

    public boolean isFrenzied() {
        return frenzied;
    }

    /** Driven mad by a candidate cowering at the edge: it braves the wisteria to reach them. */
    public void setFrenzied(boolean frenzied) {
        this.frenzied = frenzied;
    }

    /** Sends this demon after a scent at {@code pos} for {@code ticks} (it walks there, then hunts by sight). */
    public void huntTowards(BlockPos pos, int ticks) {
        this.huntPos = pos;
        this.huntUntil = this.level.getGameTime() + ticks;
    }

    /** Neither fighting nor already hunting. */
    public boolean isIdle() {
        return this.getTarget() == null && (huntPos == null || this.level.getGameTime() > huntUntil);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) setFlag(FLAG_CLIMBING, this.horizontalCollision && this.isAlive());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide || !this.isAlive()) return;
        // daylight: burn away on the spot, quietly
        if (this.level.isDay() && this.level.canSeeSky(this.blockPosition())) {
            setFlag(FLAG_SUN, true);
            this.kill();   // a real death (quests see it); die() stays silent for the sun
            return;
        }
        if (this.tickCount % 10 == 0) {
            // a frenzy lasts as long as the hunt, or the fight it led to
            if (frenzied && this.getTarget() == null && this.level.getGameTime() > huntUntil) frenzied = false;
            LivingEntity t = this.getTarget();
            if (t != null && !mayChase(t)) this.setTarget(null);   // it won't chase into the wisteria
            double r = radius(this);
            if (r >= FujikasaneLayout.DEMON_R && r < FujikasaneLayout.R + 16) sear(r);
            if (IncenseManager.inCloud((ServerLevel) this.level, this.position()) && !frenzied) {
                this.setTarget(null);   // wisteria incense: it backs off out of the smoke
                Vec3 away = this.getLookAngle().multiply(-0.4, 0, -0.4);
                this.setDeltaMovement(away.x, this.getDeltaMovement().y, away.z);
            }
        }
        // demons knit back together when left alone
        if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()
                && this.tickCount - this.getLastHurtByMobTimestamp() > 100) {
            this.heal(0.5F + 0.1F * eaten);
        }
        if (this.getTarget() instanceof Player && this.random.nextInt(900) == 0) mutter();
    }

    /** Pushed over the line anyway: the wisteria burns it and it recoils (a frenzied demon pushes on, burning). */
    private void sear(double r) {
        FujikasaneLayout layout = layout();
        if (layout == null) return;
        this.hurt(DamageSource.MAGIC, frenzied ? 1.0F : 2.0F);
        if (!frenzied) {
            Vec3 back = new Vec3(-layout.dx(this.blockPosition()), 0, -layout.dz(this.blockPosition())).normalize().scale(0.6);
            this.setDeltaMovement(back.x, 0.25, back.z);
        }
        ((ServerLevel) this.level).sendParticles(ModParticles.WISTERIA_PETAL.get(), getX(), getY() + 1.2, getZ(), 4, 0.4, 0.5, 0.4, 0.0);
    }

    private void mutter() {
        int line = this.random.nextInt(8);
        ModMessages.sendToTrackingAndSelf(new NpcSpeechS2CPacket(this.getId(),
                Component.translatable("npc.demonslayer.captive_demon.mutter." + line), 70), this);
    }

    // ------------------------------------------------------------------ death: beheaded, then ash

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.dead) return;   // the death was cancelled
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level instanceof ServerLevel server && !diedToSun() && !com.wassuples.demonslayer.sound.HandDemonMusic.silencing(server)) {
            server.playSound(null, getX(), getY(), getZ(), ModSounds.DEMON_CRUMBLE.get(), SoundSource.HOSTILE, 0.55F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        this.setDeltaMovement(this.getDeltaMovement().multiply(0, 1, 0));
        int length = diedToSun() ? SUN_DEATH_TICKS : DEATH_TICKS;
        if (this.level.isClientSide) {
            // the ash is drawn locally by every client (no particle packets): it blows off the body as it crumbles
            float k = this.deathTime / (float) length;
            int n = diedToSun() ? 4 : (k > 0.25F ? 3 : 1);
            double h = getBbHeight() * (1.0 - 0.5 * k);
            for (int i = 0; i < n; i++) {
                this.level.addParticle(ModParticles.DEMON_ASH.get(),
                        getX() + (this.random.nextDouble() - 0.5) * getBbWidth(), getY() + this.random.nextDouble() * h,
                        getZ() + (this.random.nextDouble() - 0.5) * getBbWidth(),
                        (this.random.nextDouble() - 0.5) * 0.03, 0.02 + this.random.nextDouble() * 0.03, (this.random.nextDouble() - 0.5) * 0.03);
            }
        } else if (this.deathTime >= length) {
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean isPushable() {
        return this.isAlive() && super.isPushable();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        // only a blow from someone, and not every blow: a short hiss now and then
        if (source.getEntity() == null || this.tickCount - lastHiss < 30) return;
        if (com.wassuples.demonslayer.sound.HandDemonMusic.silencing(this.level)) return;   // the Hand Demon's theme: the others fall silent
        lastHiss = this.tickCount;
        super.playHurtSound(source);
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
        return 0.35F;
    }

    @Override
    public float getVoicePitch() {
        return (variant() == HORNED || build() == BRUTE ? 0.75F : build() == LEAN ? 1.15F : 1.0F) + (this.random.nextFloat() - 0.5F) * 0.2F;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        // on the mountain they stay (the selection's spawner keeps count); strays elsewhere despawn as usual
        if (variant() == HORNED) return false;
        return radius(this) < 0 && distance > 96 * 96;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
        tag.putInt("Build", build());
        tag.putInt("Tier", tier);
        tag.putInt("Eaten", eaten);
        tag.putBoolean("Frenzied", frenzied);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, Mth.clamp(tag.getInt("Variant"), 0, LAST_SKIN));
        this.entityData.set(BUILD, tag.contains("Build") ? tag.getInt("Build") : ORDINARY);
        refreshScale();
        tier = tag.getInt("Tier");
        eaten = Math.max(1, tag.getInt("Eaten"));
        frenzied = tag.getBoolean("Frenzied");
    }

    // ================================================================== goals

    /** A point at most {@code step} blocks from the demon towards {@code goal}, on the mountain's walking surface. */
    private Vec3 stepTowards(double gx, double gz, double step) {
        double dx = gx - getX(), dz = gz - getZ(), d = Math.sqrt(dx * dx + dz * dz);
        double k = d <= step ? 1.0 : step / d;
        double x = getX() + dx * k, z = getZ() + dz * k;
        FujikasaneLayout layout = layout();
        double y = layout == null ? getY() : layout.walkY((int) Math.floor(x) - layout.centerX, (int) Math.floor(z) - layout.centerZ) + 1;
        return new Vec3(x, y, z);
    }

    /** Near the wisteria line: get back towards the middle of the summit. */
    static class ShunWisteriaGoal extends Goal {
        private final CaptiveDemonEntity demon;
        /** Game time before which no new path is tried (a failed path falls back to a shove inward). */
        private long retryAt;

        ShunWisteriaGoal(CaptiveDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean nearEdge(double margin) {
            double r = demon.radius(demon);
            return r > margin && r < FujikasaneLayout.R + 16;
        }

        @Override
        public boolean canUse() {
            if (demon.frenzied || !nearEdge(CHASE_LIMIT)) return false;
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
            return !demon.frenzied && nearEdge(ROAM_LIMIT) && !demon.getNavigation().isDone();
        }

        @Override
        public void start() {
            FujikasaneLayout layout = demon.layout();
            if (layout == null) return;
            int dx = layout.dx(demon.blockPosition()), dz = layout.dz(demon.blockPosition());
            double r = Math.max(1, FujikasaneLayout.dist(dx, dz));
            double k = (ROAM_LIMIT - 6) / r;
            Vec3 p = demon.stepTowards(layout.centerX + dx * k, layout.centerZ + dz * k, 20);
            if (!demon.getNavigation().moveTo(p.x, p.y, p.z, 1.1D)) retryAt = demon.level.getGameTime() + 40;
        }

        @Override
        public void stop() {
            if (demon.radius(demon) > ROAM_LIMIT) retryAt = demon.level.getGameTime() + 40;
        }
    }

    /** Following a candidate's scent across the summit until they come into sight. */
    static class HuntGoal extends Goal {
        private final CaptiveDemonEntity demon;
        private int repath;

        HuntGoal(CaptiveDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean active() {
            return demon.huntPos != null && demon.getTarget() == null && demon.level.getGameTime() < demon.huntUntil
                    && !demon.isDayTime() && demon.position().distanceToSqr(Vec3.atBottomCenterOf(demon.huntPos)) > 5 * 5;
        }

        @Override
        public boolean canUse() {
            if (demon.huntPos != null && !active()) endHunt();
            return active();
        }

        /** The scent is lost or reached: a frenzy (sent by a candidate cowering at the edge) ends with the hunt. */
        private void endHunt() {
            demon.huntPos = null;
            if (demon.getTarget() == null || demon.level.getGameTime() > demon.huntUntil) demon.frenzied = false;
        }

        @Override
        public boolean canContinueToUse() {
            return active();
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            if (--repath > 0 || demon.huntPos == null) return;
            repath = 40;
            Vec3 p = demon.stepTowards(demon.huntPos.getX() + 0.5, demon.huntPos.getZ() + 0.5, 22);
            demon.getNavigation().moveTo(p.x, p.y, p.z, demon.frenzied ? 1.25D : 1.05D);
        }

        @Override
        public void stop() {
            demon.getNavigation().stop();
            if (demon.level.getGameTime() > demon.huntUntil && demon.getTarget() == null) demon.frenzied = false;
        }
    }

    /** Idle wandering that roams the whole summit, well inside the wisteria; by day it keeps still in the shade. */
    static class SummitStrollGoal extends Goal {
        private final CaptiveDemonEntity demon;
        @Nullable
        private Vec3 goal;
        private int repath;

        SummitStrollGoal(CaptiveDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (demon.getTarget() != null || demon.huntPos != null || demon.isDayTime() || demon.random.nextInt(80) != 0) return false;
            FujikasaneLayout layout = demon.layout();
            if (layout == null) {
                double a = demon.random.nextDouble() * Math.PI * 2, d = 6 + demon.random.nextDouble() * 10;
                goal = new Vec3(demon.getX() + Math.cos(a) * d, demon.getY(), demon.getZ() + Math.sin(a) * d);
                return true;
            }
            double ang = demon.random.nextDouble() * Math.PI * 2;
            double gx, gz;
            if (demon.random.nextInt(5) < 2) {
                // roam: anywhere on the summit, spread evenly over its area
                double r = ROAM_LIMIT * Math.sqrt(demon.random.nextDouble());
                gx = layout.centerX + Math.cos(ang) * r;
                gz = layout.centerZ + Math.sin(ang) * r;
            } else {
                double d = 6 + demon.random.nextDouble() * 12;
                gx = demon.getX() + Math.cos(ang) * d;
                gz = demon.getZ() + Math.sin(ang) * d;
                double dx = gx - layout.centerX, dz = gz - layout.centerZ, r = Math.sqrt(dx * dx + dz * dz);
                if (r > ROAM_LIMIT) {   // a step towards the edge turns back inward instead
                    gx = layout.centerX + dx / r * (ROAM_LIMIT - 8);
                    gz = layout.centerZ + dz / r * (ROAM_LIMIT - 8);
                }
            }
            goal = new Vec3(gx, 0, gz);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return goal != null && demon.getTarget() == null && demon.huntPos == null && !demon.isDayTime()
                    && demon.position().distanceToSqr(goal.x, demon.getY(), goal.z) > 3 * 3 && (repath > 0 || !demon.getNavigation().isDone());
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            if (goal == null || --repath > 0) return;
            repath = 60;
            Vec3 p = demon.stepTowards(goal.x, goal.z, 20);
            if (!demon.getNavigation().moveTo(p.x, p.y, p.z, 0.8D)) goal = null;
        }

        @Override
        public void stop() {
            goal = null;
        }
    }
}
