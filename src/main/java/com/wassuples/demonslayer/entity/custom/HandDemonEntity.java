package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ai.AvoidWisteriaLeavesGoal;
import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket;
import com.wassuples.demonslayer.particle.ModParticles;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The Hand Demon (手鬼): fifty candidates eaten over forty-seven years on Mt. Fujikasane, Sakonji Urokodaki's thirteen
 * apprentices among them. A bloated mass of olive-green arms that crawls on its hands.
 *
 * <p>Fights with flesh manipulation, all telegraphed so they can be dodged:
 * <ul>
 *   <li><b>Arm barrage</b>: coils, then shoots up to four arms at everyone in reach; each strikes where its target
 *       stood when it was thrown.</li>
 *   <li><b>Grasping arm</b>: one arm snatches a target, drags them to its mouth and bites, unless it takes a heavy
 *       blow first (or the victim breaks free).</li>
 *   <li><b>Burrowing arms</b>: plunges its arms into the ground; the earth cracks under its targets, then the arms
 *       burst up and fling them.</li>
 *   <li><b>Cluster fist</b>: knots many arms into one huge arm and slams the ground in a line.</li>
 *   <li><b>Sweep</b>: when surrounded, flails every arm around itself.</li>
 * </ul>
 * Its neck and arms are notoriously hard: weak hits are mostly turned aside. It regrows constantly unless kept under
 * heavy attack. It fears death, and in the end only wants someone to hold its hand.
 *
 * <p>Attack state is synced ({@link #ATTACK}, {@link #ATTACK_TICK}, targets, burrow points) so the renderer can draw
 * the reaching arms.
 */
public class HandDemonEntity extends Monster {
    public static final int NONE = 0, BARRAGE = 1, GRAB = 2, BURROW = 3, FIST = 4, SWEEP = 5;
    public static final int BARRAGE_HIT = 16, GRAB_HIT = 14, BURROW_ERUPT = 32, FIST_SLAM = 24, SWEEP_HIT = 10;
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_TICK = SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.INT);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] TARGETS = new EntityDataAccessor[]{
            SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.INT)};
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<BlockPos>[] POINTS = new EntityDataAccessor[]{
            SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.BLOCK_POS),
            SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.BLOCK_POS),
            SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.BLOCK_POS)};

    /** Hits weaker than this glance off its hardened flesh (most of the damage is turned aside). */
    private static final float HARD_FLESH = 7.0F;

    // server-side fight state
    private final Vec3[] aimed = new Vec3[4];
    private int cooldown = 40;
    private int regenPause;
    private float grabDamage;
    private boolean saidNeck, saidHalf, saidFear;
    private int speechCooldown;

    public HandDemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.maxUpStep = 1.5F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 800.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK, NONE);
        this.entityData.define(ATTACK_TICK, 0);
        for (EntityDataAccessor<Integer> t : TARGETS) this.entityData.define(t, -1);
        for (EntityDataAccessor<BlockPos> p : POINTS) this.entityData.define(p, BlockPos.ZERO);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidWisteriaLeavesGoal(this, 4, 1.2D));
        this.goalSelector.addGoal(2, new FightGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, p -> !((Player) p).isCreative() && !p.isSpectator()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, StoryNpcEntity.class, true, e -> ((StoryNpcEntity) e).character().fighter()));
    }

    // ------------------------------------------------------------------ synced attack state (read by the renderer)

    public int attack() {
        return this.entityData.get(ATTACK);
    }

    public int attackTick() {
        return this.entityData.get(ATTACK_TICK);
    }

    public int targetId(int i) {
        return this.entityData.get(TARGETS[i]);
    }

    public BlockPos point(int i) {
        return this.entityData.get(POINTS[i]);
    }

    private void setAttack(int attack) {
        this.entityData.set(ATTACK, attack);
        this.entityData.set(ATTACK_TICK, 0);
        if (attack == NONE) {
            for (EntityDataAccessor<Integer> t : TARGETS) this.entityData.set(t, -1);
            for (EntityDataAccessor<BlockPos> p : POINTS) this.entityData.set(p, BlockPos.ZERO);
        }
    }

    // ------------------------------------------------------------------ toughness, regeneration, fear

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level.isClientSide && !source.isBypassInvul() && amount > 0) {
            regenPause = Math.max(regenPause, 60);   // anything that cuts it slows its regrowth
            if (amount < HARD_FLESH) {
                amount *= 0.55F;   // the blade skids off corded muscle
                if (!saidNeck && source.getEntity() instanceof Player) {
                    saidNeck = true;
                    speak("hand.taunt.neck");
                }
            } else {
                regenPause = 120;  // a real wound: it stops regrowing for a while
            }
            if (attack() == GRAB) grabDamage += amount;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level.isClientSide) {
            float frac = this.getHealth() / this.getMaxHealth();
            if (!saidHalf && frac < 0.5F) {
                saidHalf = true;
                speak("hand.taunt.half");
            }
            if (!saidFear && frac < 0.2F) {
                saidFear = true;
                speak("hand.taunt.fear");
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide) return;
        if (regenPause > 0) regenPause--;
        else if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) this.heal(3.0F);   // regrowing arms
        if (speechCooldown > 0) speechCooldown--;
        if (this.tickCount % 140 == 0 && this.getTarget() != null && this.random.nextInt(3) == 0) {
            this.level.playSound(null, this.blockPosition(), ModSounds.DEMON_GROWL.get(), SoundSource.HOSTILE, 2.0F, 0.6F + this.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level instanceof ServerLevel server) {
            speak("hand.death");
            server.sendParticles(ModParticles.DEMON_ASH.get(), getX(), getY() + 1.5, getZ(), 220, 1.6, 1.4, 1.6, 0.05);
            // Tanjiro, if he is there, takes the crumbling hand
            List<StoryNpcEntity> near = server.getEntitiesOfClass(StoryNpcEntity.class, this.getBoundingBox().inflate(24.0D),
                    n -> n.character() == StoryCharacter.TANJIRO);
            if (!near.isEmpty()) near.get(0).say(Component.translatable("mission.demonslayer.final_selection.tanjiro.hand_held"), 200);
        }
    }

    @Override
    protected void tickDeath() {
        // crumble to ash rather than the vanilla death flop
        ++this.deathTime;
        if (this.level instanceof ServerLevel server && this.deathTime % 2 == 0) {
            server.sendParticles(ModParticles.DEMON_ASH.get(), getX(), getY() + 0.5 + this.deathTime * 0.08, getZ(), 12, 1.3, 0.6, 1.3, 0.03);
        }
        if (this.deathTime >= 40 && !this.level.isClientSide) this.remove(RemovalReason.KILLED);
    }

    private void speak(String key) {
        if (speechCooldown > 0 && !key.equals("hand.death")) return;
        speechCooldown = 100;
        ModMessages.sendToTrackingAndSelf(new NpcSpeechS2CPacket(this.getId(), Component.translatable("mission.demonslayer.final_selection." + key), 120), this);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.DEMON_GROWL.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DEMON_GROWL.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + this.random.nextFloat() * 0.15F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SaidNeck", saidNeck);
        tag.putBoolean("SaidHalf", saidHalf);
        tag.putBoolean("SaidFear", saidFear);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        saidNeck = tag.getBoolean("SaidNeck");
        saidHalf = tag.getBoolean("SaidHalf");
        saidFear = tag.getBoolean("SaidFear");
    }

    // ------------------------------------------------------------------ the fight

    private List<LivingEntity> foesWithin(double range) {
        return this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range), e -> e != this && e.isAlive() && isFoe(e)
                && this.distanceToSqr(e) <= range * range);
    }

    private static boolean isFoe(LivingEntity e) {
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return e instanceof StoryNpcEntity npc && npc.character().fighter();
    }

    private Vec3 shoulder() {
        return this.position().add(0, 2.6, 0).add(Vec3.directionFromRotation(0, this.yBodyRot).scale(1.2));
    }

    private void strike(LivingEntity victim, float damage, double knock, double lift) {
        if (victim.hurt(DamageSource.mobAttack(this), damage)) {
            Vec3 push = victim.position().subtract(this.position()).multiply(1, 0, 1).normalize().scale(knock);
            victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, lift, push.z));
            victim.hurtMarked = true;
        }
    }

    /** Picks and runs attacks; crawls toward its target in between. */
    static class FightGoal extends Goal {
        private final HandDemonEntity demon;
        @Nullable
        private LivingEntity grabbed;

        FightGoal(HandDemonEntity demon) {
            this.demon = demon;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = demon.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || demon.attack() != NONE;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            release();
            demon.setAttack(NONE);
        }

        @Override
        public void tick() {
            LivingEntity target = demon.getTarget();
            int attack = demon.attack();
            if (attack == NONE) {
                if (target == null) return;
                demon.getLookControl().setLookAt(target, 20.0F, 20.0F);
                double dist = demon.distanceTo(target);
                if (dist > 7.0D) demon.getNavigation().moveTo(target, 1.0D);
                else demon.getNavigation().stop();
                if (--demon.cooldown <= 0) choose(target, dist);
                return;
            }
            int tick = demon.attackTick() + 1;
            demon.entityData.set(ATTACK_TICK, tick);
            demon.getNavigation().stop();
            if (target != null) demon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            switch (attack) {
                case BARRAGE -> barrage(tick);
                case GRAB -> grab(tick);
                case BURROW -> burrow(tick);
                case FIST -> fist(tick);
                case SWEEP -> sweep(tick);
                default -> demon.setAttack(NONE);
            }
        }

        private void choose(LivingEntity target, double dist) {
            List<LivingEntity> close = demon.foesWithin(5.0D);
            List<LivingEntity> reach = demon.foesWithin(18.0D);
            int roll = demon.random.nextInt(100);
            if (close.size() >= 2 && roll < 45) start(SWEEP);
            else if (dist <= 9 && roll < 35) start(FIST);
            else if (dist <= 14 && roll < 55) {
                start(GRAB);
                demon.entityData.set(TARGETS[0], target.getId());
            } else if (roll < 75 || reach.size() <= 1) {
                start(BARRAGE);
                aimAt(reach, target);
            } else {
                start(BURROW);
                List<LivingEntity> picks = pick(reach, target, 3);
                for (int i = 0; i < picks.size(); i++) demon.entityData.set(POINTS[i], picks.get(i).blockPosition());
            }
            demon.cooldown = 28 + demon.random.nextInt(24);
        }

        private void start(int attack) {
            demon.setAttack(attack);
            demon.playSound(ModSounds.DEMON_GROWL.get(), 1.4F, attack == FIST ? 0.5F : 0.8F);
        }

        private List<LivingEntity> pick(List<LivingEntity> reach, LivingEntity first, int max) {
            List<LivingEntity> out = new ArrayList<>();
            out.add(first);
            for (LivingEntity e : reach) {
                if (out.size() >= max) break;
                if (e != first) out.add(e);
            }
            return out;
        }

        private void aimAt(List<LivingEntity> reach, LivingEntity first) {
            List<LivingEntity> picks = pick(reach, first, 3);
            for (int i = 0; i < 4; i++) {
                demon.entityData.set(TARGETS[i], i < picks.size() ? picks.get(i).getId() : -1);
                demon.aimed[i] = null;
            }
        }

        @Nullable
        private LivingEntity targetAt(int i) {
            int id = demon.targetId(i);
            if (id < 0) return null;
            Entity e = demon.level.getEntity(id);
            return e instanceof LivingEntity l && l.isAlive() ? l : null;
        }

        // ---- arm barrage: coil (0-11), throw: aim locked at 12, strikes at BARRAGE_HIT, retract to 26
        private void barrage(int tick) {
            if (tick == 8) {   // aim locks 8 ticks before the strike: a sprint gets you clear
                for (int i = 0; i < 4; i++) {
                    LivingEntity t = targetAt(i);
                    demon.aimed[i] = t == null ? null : t.position().add(0, t.getBbHeight() * 0.5, 0);
                    if (i < 3) demon.entityData.set(POINTS[i], demon.aimed[i] == null ? BlockPos.ZERO : new BlockPos(demon.aimed[i]));
                }
            }
            if (tick == BARRAGE_HIT) {
                for (int i = 0; i < 4; i++) {
                    LivingEntity t = targetAt(i);
                    Vec3 aim = demon.aimed[i];
                    if (t == null || aim == null) continue;
                    if (t.position().add(0, t.getBbHeight() * 0.5, 0).distanceToSqr(aim) < 1.6 * 1.6) demon.strike(t, 8.0F, 0.9, 0.25);
                    if (demon.level instanceof ServerLevel s) s.sendParticles(ModParticles.DEMON_ASH.get(), aim.x, aim.y, aim.z, 8, 0.3, 0.3, 0.3, 0.02);
                }
            }
            if (tick >= 26) demon.setAttack(NONE);
        }

        // ---- grasping arm: reaches by GRAB_HIT; holds and drags to the mouth until 60, bites; broken by a heavy blow
        private void grab(int tick) {
            LivingEntity t = targetAt(0);
            if (tick == GRAB_HIT) {
                if (t != null && demon.distanceTo(t) < 15.5) {
                    grabbed = t;
                    demon.grabDamage = 0;
                } else {
                    demon.setAttack(NONE);
                    return;
                }
            }
            if (grabbed != null) {
                if (!grabbed.isAlive() || demon.grabDamage >= 18.0F) {
                    if (demon.grabDamage >= 18.0F) demon.speak("hand.taunt.release");
                    release();
                    demon.setAttack(NONE);
                    return;
                }
                Vec3 mouth = demon.position().add(Vec3.directionFromRotation(0, demon.yBodyRot).scale(2.2)).add(0, 1.6, 0);
                Vec3 at = grabbed.position();
                Vec3 next = at.add(mouth.subtract(at).scale(0.12));
                if (grabbed instanceof ServerPlayer) {
                    // players are hauled by velocity (teleporting every tick would lock their view)
                    grabbed.setDeltaMovement(next.subtract(at));
                    grabbed.hurtMarked = true;
                } else {
                    grabbed.teleportTo(next.x, next.y, next.z);
                    grabbed.setDeltaMovement(Vec3.ZERO);
                }
                grabbed.fallDistance = 0;
                if (tick % 10 == 5 && tick < 55) grabbed.hurt(DamageSource.mobAttack(demon), 2.0F);
                if (tick >= 60) {
                    demon.strike(grabbed, 14.0F, 1.6, 0.5);   // the bite, then it throws what's left
                    demon.playSound(ModSounds.DEMON_GROWL.get(), 1.6F, 0.7F);
                    release();
                    demon.setAttack(NONE);
                }
            } else if (tick > GRAB_HIT) {
                demon.setAttack(NONE);
            }
        }

        private void release() {
            grabbed = null;
        }

        // ---- burrowing arms: plunge (0-14), cracks under targets (15-31), erupt at BURROW_ERUPT, withdraw by 46
        private void burrow(int tick) {
            if (!(demon.level instanceof ServerLevel s)) return;
            for (int i = 0; i < 3; i++) {
                BlockPos p = demon.point(i);
                if (p.equals(BlockPos.ZERO)) continue;
                BlockPos ground = s.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
                if (tick >= 15 && tick < BURROW_ERUPT && tick % 3 == 0) {
                    s.sendParticles(ModParticles.DEMON_ASH.get(), ground.getX() + 0.5, ground.getY() + 0.1, ground.getZ() + 0.5, 6, 0.9, 0.05, 0.9, 0.01);
                }
                if (tick == BURROW_ERUPT) {
                    AABB zone = new AABB(ground).inflate(1.8, 2.0, 1.8);
                    for (LivingEntity e : s.getEntitiesOfClass(LivingEntity.class, zone, x -> x != demon && isFoe(x))) demon.strike(e, 11.0F, 0.2, 0.95);
                    s.sendParticles(ModParticles.DEMON_ASH.get(), ground.getX() + 0.5, ground.getY() + 0.5, ground.getZ() + 0.5, 30, 0.6, 0.8, 0.6, 0.08);
                    s.playSound(null, ground, ModSounds.DEMON_GROWL.get(), SoundSource.HOSTILE, 1.0F, 1.3F);
                }
            }
            if (tick >= 46) demon.setAttack(NONE);
        }

        // ---- cluster fist: arms knot into one (0-19), raise, slam at FIST_SLAM along a 9-block line ahead
        private void fist(int tick) {
            if (tick == FIST_SLAM && demon.level instanceof ServerLevel s) {
                Vec3 fwd = Vec3.directionFromRotation(0, demon.yBodyRot);
                Vec3 from = demon.position();
                for (LivingEntity e : demon.foesWithin(11.0D)) {
                    Vec3 rel = e.position().subtract(from);
                    double along = rel.dot(fwd);
                    double side = rel.subtract(fwd.scale(along)).horizontalDistance();
                    if (along > 0 && along < 10 && side < 2.2) demon.strike(e, 17.0F, 0.6, 0.8);
                }
                for (int i = 2; i <= 9; i++) {
                    Vec3 p = from.add(fwd.scale(i));
                    s.sendParticles(ModParticles.DEMON_ASH.get(), p.x, p.y + 0.2, p.z, 6, 0.8, 0.1, 0.8, 0.05);
                }
                s.playSound(null, demon.blockPosition(), ModSounds.NIGHT_DRUM.get(), SoundSource.HOSTILE, 1.5F, 0.5F);
            }
            if (tick >= 36) demon.setAttack(NONE);
        }

        // ---- sweep: every arm flails round (0-SWEEP_HIT), hits all close foes
        private void sweep(int tick) {
            if (tick == SWEEP_HIT) {
                for (LivingEntity e : demon.foesWithin(5.5D)) demon.strike(e, 9.0F, 1.4, 0.35);
            }
            if (tick >= 20) demon.setAttack(NONE);
        }
    }

    /** Fraction 0..1 of how far the reaching arms are out right now (renderer helper; mirrors the server timings). */
    public static float reach(int attack, float tick) {
        return switch (attack) {
            case BARRAGE -> tick < 12 ? 0 : tick < BARRAGE_HIT ? (tick - 12) / (BARRAGE_HIT - 12) : tick < 20 ? 1 : Math.max(0, 1 - (tick - 20) / 6);
            case GRAB -> tick < 6 ? 0 : tick < GRAB_HIT ? (tick - 6) / (GRAB_HIT - 6) : 1;
            case BURROW -> tick < BURROW_ERUPT ? 0 : tick < BURROW_ERUPT + 4 ? (tick - BURROW_ERUPT) / 4 : Math.max(0, 1 - (tick - BURROW_ERUPT - 6) / 8);
            default -> 0;
        };
    }

    /** Smooth bounded wobble for the renderer. */
    public static float wobble(float t, float rate, float phase) {
        return Mth.sin(t * rate + phase);
    }
}
