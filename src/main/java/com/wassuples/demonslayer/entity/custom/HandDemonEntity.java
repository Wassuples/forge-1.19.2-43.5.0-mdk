package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ai.AvoidWisteriaLeavesGoal;
import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket;
import com.wassuples.demonslayer.networking.packet.HandDemonMusicS2CPacket;
import com.wassuples.demonslayer.sound.HandDemonMusic;
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
 * apprentices among them. An enormous, bloated mass bound in coils of its own arms, squatting on hands, with a small head
 * clutched by more hands.
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

    /** Dying: who the outstretched hand reaches for, whether Tanjiro is the one to take it, and the outcome. */
    private static final EntityDataAccessor<Integer> REACH_TARGET = SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TANJIRO_HOLDS = SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OUTCOME = SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OUTCOME_AT = SynchedEntityData.defineId(HandDemonEntity.class, EntityDataSerializers.INT);

    // ---- the death and its reprise, in ticks since the killing blow (they match the music files' lengths)
    /** The demon half of the theme collapses; the natural reprise starts here. */
    public static final int REPRISE_AT = 50;
    /** The reprise has played: the hand reaches out. */
    public static final int REACH_AT = REPRISE_AT + 250;
    /** How long the hand waits for someone to take it (five seconds). */
    public static final int WINDOW = 100;
    /** Tanjiro, if he is there, takes it this long after it reaches. */
    public static final int TANJIRO_TAKES = 30;
    /** Lengths of the two endings; the body is gone when its ending has played. */
    public static final int END_HELD = 196, END_UNHELD = 166;
    public static final int OUTCOME_WAITING = 0, OUTCOME_HELD = 1, OUTCOME_UNHELD = 2;

    // ---- the fight's music (server side)
    /** Ticks without sight of its quarry before it gives up the hunt (the music fades over the same time). */
    private static final int LOSE_TICKS = 200;
    /** Regrowing back into an earlier phase needs this much health above the line, held this long, this rarely. */
    private static final float REGROW_MARGIN = 0.08F;
    private static final int REGROW_HOLD = 60, REGROW_COOLDOWN = 400;
    private int musicPhase;           // 0 before the fight, then 1..4
    private int rewinds;
    private int regrowHeld;
    private long lastRewind = -100000L;
    private int unseen;
    private long fightStart = -1L;
    private boolean musicOn;

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
        this.entityData.define(REACH_TARGET, -1);
        this.entityData.define(TANJIRO_HOLDS, false);
        this.entityData.define(OUTCOME, OUTCOME_WAITING);
        this.entityData.define(OUTCOME_AT, 0);
    }

    public int reachTarget() {
        return this.entityData.get(REACH_TARGET);
    }

    public boolean tanjiroHolds() {
        return this.entityData.get(TANJIRO_HOLDS);
    }

    public int outcome() {
        return this.entityData.get(OUTCOME);
    }

    public int outcomeAt() {
        return this.entityData.get(OUTCOME_AT);
    }

    /** True while its outstretched hand can still be taken. */
    public boolean handOpen() {
        return this.isDeadOrDying() && this.deathTime >= REACH_AT && this.deathTime < REACH_AT + WINDOW && outcome() == OUTCOME_WAITING;
    }

    /** How long the whole death lasts (known once the outcome is). */
    public int deathLength() {
        int o = outcome();
        if (o == OUTCOME_WAITING) return REACH_AT + WINDOW + END_UNHELD;
        return outcomeAt() + (o == OUTCOME_HELD ? END_HELD : END_UNHELD);
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
            if (!saidHalf && frac < 0.5f) {
                saidHalf = true;
                speak("hand.taunt.half");
            }
            if (!saidFear && frac < 0.2f) {
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
            tickMusic();
            if (this.tickCount % 140 == 0 && this.getTarget() != null && this.random.nextInt(3) == 0) {
                this.level.playSound(null, this.blockPosition(), ModSounds.DEMON_GROWL.get(), SoundSource.HOSTILE, 2.0F, 0.6F + this.random.nextFloat() * 0.2F);
            }
         }
    }

    // ------------------------------------------------------------------ its theme (server side)

    /** Health phase: 1 (100-75%), 2 (75-50%), 3 (50-20%), 4 (below 20%). */
    private static int phaseFor(float frac) {
        return frac >= 0.75F ? 1 : frac >= 0.5F ? 2 : frac >= 0.2F ? 3 : 4;
    }

    /** The health line a phase starts below (sliding back above it means regrowing into the previous phase). */
    private static float lineAbove(int phase) {
        return phase == 2 ? 0.75F : phase == 3 ? 0.5F : 0.2F;
    }

    private void tickMusic() {
        if (!(this.level instanceof ServerLevel server)) return;
        LivingEntity quarry = this.getTarget();
        if (quarry != null && quarry.isAlive()) {
            if (fightStart < 0) fightStart = server.getGameTime();
            // it hunts by sight: out of sight long enough and it gives up (the music fades as it does)
            if (this.hasLineOfSight(quarry)) unseen = 0;
            else if (++unseen >= LOSE_TICKS) {
                this.setTarget(null);
                unseen = 0;
            }
        } else {
            unseen = 0;
        }
        boolean fighting = this.getTarget() != null;

        // phase from health: dropping is immediate; regrowing back needs a margin, held, and not too often
        float frac = this.getHealth() / this.getMaxHealth();
        int want = phaseFor(frac);
        if (musicPhase == 0 || want > musicPhase) {
            musicPhase = Math.max(musicPhase, want);
            regrowHeld = 0;
        } else if (want < musicPhase && frac >= lineAbove(musicPhase) + REGROW_MARGIN) {
            if (++regrowHeld >= REGROW_HOLD && server.getGameTime() - lastRewind >= REGROW_COOLDOWN) {
                musicPhase--;
                rewinds++;
                regrowHeld = 0;
                lastRewind = server.getGameTime();
                sendMusic(server);
            }
        } else {
            regrowHeld = 0;
        }

        if (fighting) {
            HandDemonMusic.markActive(server);
            if (!musicOn || this.tickCount % 10 == 0) sendMusic(server);
            musicOn = true;
        } else if (musicOn) {
            musicOn = false;
            HandDemonMusic.broadcast(server, this.blockPosition(), HandDemonMusicS2CPacket.idle(this.getId()));
        }
    }

    private void sendMusic(ServerLevel server) {
        float intensity = 1.0F - Math.min(1.0F, unseen / (float) LOSE_TICKS);
        int age = fightStart < 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, server.getGameTime() - fightStart);
        HandDemonMusic.broadcast(server, this.blockPosition(),
                HandDemonMusicS2CPacket.fight(this.getId(), Math.max(1, musicPhase), rewinds, intensity, tanjiroNear(40.0D) != null, age));
    }

    @Nullable
    private StoryNpcEntity tanjiroNear(double range) {
        List<StoryNpcEntity> near = this.level.getEntitiesOfClass(StoryNpcEntity.class, this.getBoundingBox().inflate(range),
                n -> n.isAlive() && n.character() == StoryCharacter.TANJIRO);
        return near.isEmpty() ? null : near.get(0);
    }

    // ------------------------------------------------------------------ death: the collapse, the reprise, the hand

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level instanceof ServerLevel server) {
            speak("hand.death");
            server.sendParticles(ModParticles.DEMON_ASH.get(), getX(), getY() + 2.5, getZ(), 160, 1.4, 2.0, 1.4, 0.05);
            // Tanjiro, if he is there, takes the crumbling hand
            List<StoryNpcEntity> near = server.getEntitiesOfClass(StoryNpcEntity.class, this.getBoundingBox().inflate(24.0D),
                n -> n.character() == StoryCharacter.TANJIRO);
            if (!near.isEmpty()) near.get(0).say(Component.translatable("mission.demonslayer.final_selection.tanjiro.hand_held"), 200);
            musicOn = false;
            setAttack(NONE);
            HandDemonMusic.markActive(server);
            HandDemonMusic.broadcast(server, this.blockPosition(), HandDemonMusicS2CPacket.death(this.getId(), 0, OUTCOME_WAITING, 0));
            // Tanjiro, if he is near, comes to its side
            StoryNpcEntity tanjiro = tanjiroNear(24.0D);
            if (tanjiro != null) tanjiro.getNavigation().moveTo(this, 0.8D);
        }
    }

    /**
     * Beheaded, it does not simply crumble: its music collapses, then the natural reprise plays while the body chars
     * and sinks. When the reprise reaches its end the hand stretches out and waits five seconds. Tanjiro takes it if he
     * is there; if not, the player may. Then the body crumbles away while the ending plays.
     */
    @Override
    protected void tickDeath() {
        // crumble to ash rather than the vanilla death flop
        ++this.deathTime;
        if (this.level instanceof ServerLevel server && this.deathTime % 2 == 0) {
            server.sendParticles(ModParticles.DEMON_ASH.get(), getX(), getY() + 4.5 - this.deathTime * 0.1, getZ(), 10, 1.2, 0.8, 1.2, 0.03);
        this.setDeltaMovement(this.getDeltaMovement().multiply(0, 1, 0));
        if (this.level.isClientSide) {
            int len = deathLength();
            float k = Mth.clamp(this.deathTime / (float) len, 0.0F, 1.0F);
            int n = k > 0.85F ? 4 : (this.deathTime % 3 == 0 ? 1 : 0);
            for (int i = 0; i < n; i++) {
                this.level.addParticle(ModParticles.DEMON_ASH.get(),
                        getX() + (this.random.nextDouble() - 0.5) * getBbWidth(), getY() + this.random.nextDouble() * getBbHeight() * (1.0 - 0.6 * k),
                        getZ() + (this.random.nextDouble() - 0.5) * getBbWidth(),
                        (this.random.nextDouble() - 0.5) * 0.03, 0.02 + this.random.nextDouble() * 0.03, (this.random.nextDouble() - 0.5) * 0.03);
            }
            // while the hand waits (and Tanjiro isn't there to take it), a faint glimmer at its fingertips
            if (handOpen() && !tanjiroHolds() && this.deathTime - REACH_AT > 20 && this.deathTime % 4 == 0) {
                Entity t = this.level.getEntity(reachTarget());
                Vec3 dir = t != null ? t.position().subtract(this.position()).multiply(1, 0, 1).normalize()
                        : Vec3.directionFromRotation(0, this.yBodyRot);
                double reachLen = t != null ? Math.min(5.0, Math.max(0.5, t.distanceTo(this) - 1.2)) : 3.5;
                Vec3 tip = this.position().add(dir.scale(reachLen)).add(0, 1.5, 0);
                this.level.addParticle(ModParticles.RESOLVE_SPARK.get(), tip.x, tip.y, tip.z, 0, 0.01, 0);
            }
            return;
        }
        ServerLevel server2 = (ServerLevel) this.level;
        HandDemonMusic.markActive(server2);
        if (this.deathTime == REACH_AT) {
            StoryNpcEntity tanjiro = tanjiroNear(24.0D);
            if (tanjiro != null) {
                this.entityData.set(TANJIRO_HOLDS, true);
                this.entityData.set(REACH_TARGET, tanjiro.getId());
            } else {
                Player nearest = this.level.getNearestPlayer(this, 24.0D);
                if (nearest != null) this.entityData.set(REACH_TARGET, nearest.getId());
            }
        }
        if (outcome() == OUTCOME_WAITING && this.deathTime == REACH_AT + TANJIRO_TAKES && tanjiroHolds()) {
            Entity t = this.level.getEntity(reachTarget());
            if (t instanceof StoryNpcEntity tanjiro) tanjiro.say(Component.translatable("mission.demonslayer.final_selection.tanjiro.hand_held"), 200);
            settle(OUTCOME_HELD);
        }
        if (outcome() == OUTCOME_WAITING && this.deathTime >= REACH_AT + WINDOW) settle(OUTCOME_UNHELD);
        if (this.deathTime % 10 == 0) {
            HandDemonMusic.broadcast(server, this.blockPosition(), HandDemonMusicS2CPacket.death(this.getId(), this.deathTime, outcome(), outcomeAt()));
        }
        if (this.deathTime >= deathLength()) this.remove(RemovalReason.KILLED);
    }
    }

    private void settle(int outcome) {
        this.entityData.set(OUTCOME, outcome);
        this.entityData.set(OUTCOME_AT, this.deathTime);
        if (this.level instanceof ServerLevel server) {
            HandDemonMusic.broadcast(server, this.blockPosition(), HandDemonMusicS2CPacket.death(this.getId(), this.deathTime, outcome, this.deathTime));
        }
        if (this.deathTime >= 40 && !this.level.isClientSide) this.remove(RemovalReason.KILLED);
    }

    private void speak(String key) {
        if (speechCooldown > 0 && !key.equals("hand.death")) return;
                speechCooldown = 100;
                ModMessages.sendToTrackingAndSelf(new NpcSpeechS2CPacket(this.getId(), Component.translatable("mission.demonslayer.final_selection." + key), 120), this);
            }

            /** A player takes the outstretched hand: only while it waits, only if Tanjiro isn't there to, and only up close. */
    public void takeHand(ServerPlayer player) {
        if (!handOpen() || tanjiroHolds() || player.isSpectator() || player.distanceToSqr(this) > 8.0D * 8.0D) return;
        this.entityData.set(REACH_TARGET, player.getId());
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        settle(OUTCOME_HELD);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.DEMON_GROWL.get();
    }

    /** A low growl now and then when struck (rarely: its theme carries the fight). */
    @Override
    protected void playHurtSound(DamageSource source) {
        if (this.tickCount - lastHurtSound < 120) return;
        lastHurtSound = this.tickCount;
        super.playHurtSound(source);
    }

    private int lastHurtSound = -100;

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DEMON_GROWL.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.45F;
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
        tag.putInt("MusicPhase", musicPhase);
        tag.putInt("Rewinds", rewinds);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        saidNeck = tag.getBoolean("SaidNeck");
        saidHalf = tag.getBoolean("SaidHalf");
        saidFear = tag.getBoolean("SaidFear");
        musicPhase = tag.getInt("MusicPhase");
        rewinds = tag.getInt("Rewinds");
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
        return this.position().add(0, 3.3, 0).add(Vec3.directionFromRotation(0, this.yBodyRot).scale(0.5));
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
            demon.playSound(ModSounds.DEMON_GROWL.get(), 0.45F, attack == FIST ? 0.5F : 0.8F);
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
                    if (demon.grabDamage >= 18.0f) demon.speak("hand.taunt.release");
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
                    demon.playSound(ModSounds.DEMON_GROWL.get(), 0.6F, 0.7F);
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
                    s.playSound(null, ground, ModSounds.DEMON_GROWL.get(), SoundSource.HOSTILE, 0.35F, 1.3F);
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
                s.playSound(null, demon.blockPosition(), ModSounds.NIGHT_DRUM.get(), SoundSource.HOSTILE, 0.7F, 0.5F);
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
