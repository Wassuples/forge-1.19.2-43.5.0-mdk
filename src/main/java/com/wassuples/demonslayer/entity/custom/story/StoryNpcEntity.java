package com.wassuples.demonslayer.entity.custom.story;

import com.wassuples.demonslayer.dialogue.DialogueManager;
import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A named story character (see {@link StoryCharacter}): the Ubuyashiki twins who run the Final Selection, and the
 * candidates who take it with you (Tanjiro, Zenitsu, Inosuke, Kanao, Genya).
 *
 * <p>Behaviour is a single anchor: the NPC keeps within {@link #leash} blocks of {@link #anchor} (strolling around
 * it, walking back when drawn away, teleporting home when left far behind), unless it has a {@link #companion}, in
 * which case it keeps near that player instead. Fighters attack demons nearby. Right-clicking opens a dialogue
 * ({@link DialogueManager}); NPCs speak in the world through speech bubbles ({@link #say}), never in chat.
 *
 * <p>Story NPCs can't be killed: the twins take no damage at all, candidates are left at 1 health at worst, and
 * players can't hurt either.
 */
public class StoryNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> CHARACTER = SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.INT);
    /** Ticks after being hit (or seeing a demon close by) during which the NPC won't stop to chat. */
    private static final int COMBAT_GRACE = 60;
    /** Persistent-data UUID: the player a swordsmith has come to deliver a blade to. */
    public static final String SMITH_FOR = "demonslayer.smith_for";
    private int smithLeaving = -1;

    @Nullable
    private BlockPos anchor;
    private int leash = 6;
    @Nullable
    private UUID companion;
    private int keepWithin = 12;
    private boolean fightsHandDemon;
    /** Facing used while standing at the anchor (twins face the plaza). */
    private float restYaw;
    /** Breathing style whose forms it uses in a fight (NONE: plain sword work). */
    private com.wassuples.demonslayer.styles.BreathingStyles style = com.wassuples.demonslayer.styles.BreathingStyles.NONE;
    private int formCooldown = 40;
    /** A form about to be used: after the breath is drawn (and, for aerial forms, once airborne). */
    @Nullable
    private com.wassuples.demonslayer.styles.BreathingForms pendingForm;
    private int pendingDelay;
    /** Hiding (Zenitsu in the wisteria during the selection): won't fight at all. Set by the shrine's keeper. */
    private boolean hiding;
    /** Player currently in a dialogue with this NPC (server-side, not saved). */
    @Nullable
    private UUID talkingTo;
    private int talkTicks;

    public StoryNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARACTER, 0);
        this.entityData.define(VARIANT, 0);
    }

    /** Look of an anonymous {@link StoryCharacter#CANDIDATE} (0..{@link StoryCharacter#CANDIDATE_VARIANTS}-1). */
    public int variant() {
        return this.entityData.get(VARIANT);
    }

    /** Makes this NPC an anonymous candidate with its own name, look and breathing style. */
    public void setCandidate(String name, int variant, com.wassuples.demonslayer.styles.BreathingStyles style) {
        setCharacter(StoryCharacter.CANDIDATE);
        this.style = style;
        this.entityData.set(VARIANT, Math.floorMod(variant, StoryCharacter.CANDIDATE_VARIANTS));
        this.setCustomName(Component.literal(name));
    }

    /** The name to show: the character's, or an anonymous candidate's own. */
    public Component storyName() {
        return character() == StoryCharacter.CANDIDATE && this.getCustomName() != null ? this.getCustomName() : character().displayName();
    }

    public StoryCharacter character() {
        return StoryCharacter.byId(this.entityData.get(CHARACTER));
    }

    public void setCharacter(StoryCharacter character) {
        this.entityData.set(CHARACTER, character.ordinal());
        this.setCustomName(character.displayName());
        this.setCustomNameVisible(false);   // our own name plate is drawn by the renderer
        this.scale = character.scale();
        this.refreshDimensions();
        equipFor(character);
        // breathing at the Final Selection: Tanjiro is still a Water user, Zenitsu knows only Thunder's First Form,
        // Inosuke has his self-taught Beast breathing, Kanao Flower; Genya can't use breathing at all
        this.style = defaultStyle(character);
        // the candidates are a match for one starving demon, not for a crowd of them
        double damage = switch (character) {
            case INOSUKE, GENYA -> 3.5D;
            case ZENITSU -> 2.5D;
            case CANDIDATE -> 3.0D;
            default -> 3.0D;
        };
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        if (character == StoryCharacter.CANDIDATE) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
            this.getAttribute(Attributes.ARMOR).setBaseValue(2.0D);
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64.0D);   // they climb the whole stairway to the shrine
            this.setHealth(40.0F);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CHARACTER.equals(key)) {
            this.scale = character().scale();
            this.refreshDimensions();
        }
    }

    /** Mirrors the character's scale; read by {@link #getDimensions}, which can run before synced data exists. */
    private float scale;

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(scale <= 0.0F ? 1.0F : scale);
    }

    private void equipFor(StoryCharacter character) {
        if (character.blades() >= 1) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.NICHIRIN_BLADE.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (character.blades() >= 2) {
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.NICHIRIN_BLADE.get()));
            this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        } else {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FaceTalkerGoal(this));
        this.goalSelector.addGoal(1, new FleeGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal(this, 0.42F) {
            @Override
            public boolean canUse() {   // berserkers (Genya) throw themselves at their foe
                return behavior() == com.wassuples.demonslayer.nemesis.ai.Behavior.BERSERKER && !hiding && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true) {
            @Override
            public boolean canUse() {
                return character().fighter() && !hiding && sleepTicks <= 0 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return sleepTicks <= 0 && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new KeepNearGoal(this));
        this.goalSelector.addGoal(4, new IdleActGoal(this));
        this.goalSelector.addGoal(5, new StrollNearAnchorGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !character().isTwin() && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class, StoryNpcEntity.class) {
            @Override
            public boolean canUse() {
                return character().fighter() && super.canUse();
            }
        });
        this.targetSelector.addGoal(2, new ProtectAllyGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, this::isFoe) {
            @Override
            public boolean canUse() {
                return character().fighter() && !hiding && super.canUse();
            }
        });
    }

    private boolean isFoe(LivingEntity e) {
        if (e instanceof HandDemonEntity hand) return fightsHandDemon || hand.getTarget() == this;
        if (e instanceof NemesisDemonEntity) return true;
        return e instanceof Monster && !(e instanceof Creeper);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (!character().fighter() || hiding || target instanceof Player || target instanceof StoryNpcEntity) return false;
        if (target instanceof HandDemonEntity hand && !fightsHandDemon && hand.getTarget() != this && this.getLastHurtByMob() != hand) return false;
        return super.canAttack(target);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        equipFor(character());
        return result;
    }

    // ------------------------------------------------------------------ survival

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (!character().fighter() && !source.isBypassInvul()) return true;   // the twins, the swordsmith
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player && !source.isBypassInvul()) return false;
        if (source.getEntity() instanceof StoryNpcEntity && !source.isBypassInvul()) return false;   // allies' forms
        if (!character().fighter() && !source.isBypassInvul()) return false;
        if (character().named() && !source.isBypassInvul() && this.getHealth() - amount < 1.0F) {
            amount = Math.max(0.0F, this.getHealth() - 1.0F);   // the story needs them: badly hurt, never killed
        }
        boolean hit = super.hurt(source, amount);
        // Zenitsu, cornered and terrified, faints... and fights in his sleep
        if (hit && character() == StoryCharacter.ZENITSU && sleepTicks <= 0 && source.getEntity() instanceof Monster
                && this.getHealth() < this.getMaxHealth() * 0.75F) {
            sleepTicks = 300;
            shout(Component.translatable("npc.demonslayer.zenitsu_agatsuma.faint"), 60);
        }
        return hit;
    }

    // ------------------------------------------------------------------ personality

    /** Zenitsu asleep on his feet (fainted): he fights only with Thunderclap and Flash, without moving otherwise. */
    private int sleepTicks;
    private int fightBarkCooldown = 100;

    /**
     * Each character's temperament, reusing the Nemesis demons' personality archetypes: it sets what they say in a
     * fight and how they idle.
     */
    public com.wassuples.demonslayer.nemesis.ai.Personality personality() {
        return switch (character()) {
            case TANJIRO -> com.wassuples.demonslayer.nemesis.ai.Personality.PROTECTIVE;
            case ZENITSU -> com.wassuples.demonslayer.nemesis.ai.Personality.WHINING;
            case INOSUKE -> com.wassuples.demonslayer.nemesis.ai.Personality.COCKY;
            case KANAO -> com.wassuples.demonslayer.nemesis.ai.Personality.STOIC;
            case GENYA -> com.wassuples.demonslayer.nemesis.ai.Personality.RUTHLESS;
            case CANDIDATE -> variant() % 3 == 0 ? com.wassuples.demonslayer.nemesis.ai.Personality.NERVOUS
                    : variant() % 3 == 1 ? com.wassuples.demonslayer.nemesis.ai.Personality.CAUTIOUS : com.wassuples.demonslayer.nemesis.ai.Personality.INSECURE;
            default -> com.wassuples.demonslayer.nemesis.ai.Personality.STOIC;
        };
    }

    /** ...and the Nemesis combat behaviours: how they move in a fight. */
    public com.wassuples.demonslayer.nemesis.ai.Behavior behavior() {
        return switch (character()) {
            case TANJIRO -> com.wassuples.demonslayer.nemesis.ai.Behavior.BODYGUARD;   // goes to whoever is in trouble
            case ZENITSU, CANDIDATE -> com.wassuples.demonslayer.nemesis.ai.Behavior.COWARDLY;
            case INOSUKE, GENYA -> com.wassuples.demonslayer.nemesis.ai.Behavior.BERSERKER;
            case KANAO -> com.wassuples.demonslayer.nemesis.ai.Behavior.SKIRMISHER;    // strikes and steps away
            default -> com.wassuples.demonslayer.nemesis.ai.Behavior.CALM;
        };
    }

    public boolean isAsleep() {
        return sleepTicks > 0;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && behavior() == com.wassuples.demonslayer.nemesis.ai.Behavior.SKIRMISHER && this.isOnGround()) {
            // Kanao: a clean cut, then a light step back out of reach
            Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                away = away.normalize().scale(0.55);
                this.setDeltaMovement(away.x, 0.28, away.z);
                this.hasImpulse = true;
            }
        }
        return hit;
    }

    /** Personality in a fight: a line now and then as they engage (by temperament; Kanao says nothing). */
    private void tickPersonality() {
        if (fightBarkCooldown > 0) fightBarkCooldown--;
        if (sleepTicks > 0) {
            sleepTicks--;
            this.getNavigation().stop();
            this.setXRot(35.0F);   // head hanging
            if (sleepTicks % 80 == 40) shout(Component.translatable("npc.demonslayer.zenitsu_agatsuma.sleep"), 40);
            return;
        }
        LivingEntity foe = this.getTarget();
        if (foe != null && fightBarkCooldown <= 0 && this.distanceToSqr(foe) < 10 * 10 && personality() != com.wassuples.demonslayer.nemesis.ai.Personality.STOIC) {
            fightBarkCooldown = 240 + this.random.nextInt(240);
            String key = character() == StoryCharacter.CANDIDATE ? "candidate" : character().key();
            shout(Component.translatable("npc.demonslayer." + key + ".fight." + this.random.nextInt(3)), 50);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide) return;
        if (this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth() && this.getTarget() == null) this.heal(2.0F);
        if (talkTicks > 0 && --talkTicks == 0) talkingTo = null;
        tickSpeech();
        // a fight breaks off any conversation
        if (talkingTo != null && inCombat() && this.level.getPlayerByUUID(talkingTo) instanceof ServerPlayer sp) {
            DialogueManager.closeChatWith(sp, this);   // story-opened conversations (Tanjiro's offer) stay open
            talkingTo = null;
        }
        if (character() == StoryCharacter.SWORDSMITH) {
            tickSwordsmith();
            return;
        }
        tickPersonality();
        tickBreathing();
        // Tanjiro (or anyone told to) goes for the Hand Demon when it is close
        if (fightsHandDemon && this.getTarget() == null && this.tickCount % 10 == 0) {
            java.util.List<HandDemonEntity> hands = this.level.getEntitiesOfClass(HandDemonEntity.class, this.getBoundingBox().inflate(40.0D), LivingEntity::isAlive);
            if (!hands.isEmpty()) this.setTarget(hands.get(0));
        }
    }

    /** The swordsmith walks up to his slayer, hands over the blade, then goes on his way and is gone. */
    private void tickSwordsmith() {
        if (smithLeaving >= 0) {
            if (++smithLeaving > 200) this.discard();
            return;
        }
        if (!this.getPersistentData().hasUUID(SMITH_FOR) || !(this.level.getPlayerByUUID(this.getPersistentData().getUUID(SMITH_FOR)) instanceof ServerPlayer owner)) {
            if (this.tickCount > 100) this.discard();   // his slayer isn't here
            return;
        }
        if (this.tickCount > 600) {   // never reached them: the blade arrives anyway (FinalSelectionMission), he leaves
            this.discard();
            return;
        }
        if (this.distanceToSqr(owner) < 3.0 * 3.0) {
            boolean handed = owner.getCapability(com.wassuples.demonslayer.missions.PlayerMissionsProvider.PLAYER_MISSIONS)
                    .map(d -> com.wassuples.demonslayer.missions.story.FinalSelectionMission.deliverBlade(owner, d)).orElse(false);
            this.getLookControl().setLookAt(owner, 30, 30);
            if (handed) say(Component.translatable("mission.demonslayer.final_selection.smith.deliver"), 160);
            smithLeaving = 0;
            this.setCompanion(null);
            net.minecraft.world.phys.Vec3 away = this.position().subtract(owner.position()).normalize().scale(24).add(this.position());
            this.setAnchor(new BlockPos(away), 1);
        }
    }

    // ------------------------------------------------------------------ breathing

    /**
     * Forms an NPC can't use sensibly: toggles that would never be switched off (Splashing Water Flow, Constant Flux,
     * Flame Tiger, Crazy Cutting, Whirling Fangs), Blessed Rain (only for a foe who yields), Dead Calm (Giyu's own) and
     * the charged forms.
     */
    private static final java.util.Set<String> NPC_UNUSABLE = java.util.Set.of("WATER_5", "WATER_9", "WATER_10", "WATER_11",
            "FLAME_5", "BEAST_5", "BEAST_10");
    /** Forms that strike from the air: the NPC leaps first. */
    private static final java.util.Set<String> AERIAL = java.util.Set.of("WATER_8", "FLAME_3");
    private static final java.util.Set<String> THRUSTS = java.util.Set.of("WATER_7", "FLAME_1", "THUNDER_1", "WIND_1", "STONE_1",
            "BEAST_1", "BEAST_8", "BEAST_9", "BEAST_11");
    private static final java.util.Set<String> RISING = java.util.Set.of("WATER_2", "WATER_8", "FLAME_2", "FLAME_3", "STONE_2");
    private static final java.util.Set<String> SPINS = java.util.Set.of("WATER_3", "WATER_6", "THUNDER_2", "THUNDER_3", "WIND_3",
            "STONE_3", "FLOWER_5", "FLOWER_6");

    /** The forms it knows: Zenitsu only the First Form; anonymous candidates their style's first three. */
    private java.util.List<com.wassuples.demonslayer.styles.BreathingForms> forms() {
        java.util.List<com.wassuples.demonslayer.styles.BreathingForms> out = new java.util.ArrayList<>();
        for (com.wassuples.demonslayer.styles.BreathingForms f : com.wassuples.demonslayer.styles.BreathingForms.values()) {
            if (f.getStyle() != style || f.isBuff()) continue;   // buffs only tick for players
            if (f.getInputType() != com.wassuples.demonslayer.styles.IForm.InputType.PRESS || NPC_UNUSABLE.contains(f.name())) continue;
            if (character() == StoryCharacter.ZENITSU && !f.name().equals("THUNDER_1")) continue;
            if (character() == StoryCharacter.CANDIDATE && out.size() >= 3) break;
            out.add(f);
        }
        return out;
    }

    private static int strokeOf(com.wassuples.demonslayer.styles.BreathingForms f) {
        String n = f.name();
        if (THRUSTS.contains(n)) return com.wassuples.demonslayer.entity.custom.BreathSlashEntity.THRUST;
        if (RISING.contains(n)) return com.wassuples.demonslayer.entity.custom.BreathSlashEntity.RISING;
        if (SPINS.contains(n)) return com.wassuples.demonslayer.entity.custom.BreathSlashEntity.SPIN;
        return com.wassuples.demonslayer.entity.custom.BreathSlashEntity.ARC;
    }

    /**
     * Breathing in a fight: every few seconds, a form that fits the distance to its foe (dashes and thrusts from further
     * off). The NPC draws a sharp breath, calls out the form (as slayers do), then strikes; the stroke is drawn in its
     * style's element ({@link com.wassuples.demonslayer.entity.custom.BreathSlashEntity}).
     */
    private void tickBreathing() {
        if (formCooldown > 0) formCooldown--;
        if (!(this.level instanceof ServerLevel server)) return;
        LivingEntity foe = this.getTarget();
        if (pendingForm != null) {
            pendingDelay--;
            boolean aerial = AERIAL.contains(pendingForm.name());
            boolean ready = aerial ? (!this.isOnGround() && this.getDeltaMovement().y < 0.05) || pendingDelay <= -6 : pendingDelay <= 0;
            if (ready) {
                com.wassuples.demonslayer.styles.BreathingForms f = pendingForm;
                pendingForm = null;
                LivingEntity at = foe;
                if (at == null && sleepTicks > 0) {
                    java.util.List<Monster> near = this.level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(7.0D), LivingEntity::isAlive);
                    if (!near.isEmpty()) at = near.get(0);
                }
                if (at != null && at.isAlive() && (!hiding || sleepTicks > 0)) strike(f, at, server);
            }
            return;
        }
        if (sleepTicks > 0 && foe == null) {
            java.util.List<Monster> near = this.level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(7.0D), LivingEntity::isAlive);
            if (!near.isEmpty()) foe = near.get(0);
        }
        if (formCooldown > 0 || foe == null || !foe.isAlive() || (hiding && sleepTicks <= 0) || style == com.wassuples.demonslayer.styles.BreathingStyles.NONE) return;
        if (!this.hasLineOfSight(foe)) return;
        double d2 = this.distanceToSqr(foe);
        java.util.List<com.wassuples.demonslayer.styles.BreathingForms> fit = new java.util.ArrayList<>();
        for (com.wassuples.demonslayer.styles.BreathingForms f : forms()) {
            double reach = strokeOf(f) == com.wassuples.demonslayer.entity.custom.BreathSlashEntity.THRUST ? 7.0 : 4.5;
            if (d2 < reach * reach) fit.add(f);
        }
        if (fit.isEmpty()) return;
        com.wassuples.demonslayer.styles.BreathingForms f = fit.get(this.random.nextInt(fit.size()));
        // Total Concentration: the breath, and the name of the form called out
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(), com.wassuples.demonslayer.sound.ModSounds.BREATH_INTAKE.get(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 0.9F + this.random.nextFloat() * 0.2F);
        if (character().named() || this.random.nextInt(2) == 0) {
            shout(Component.translatable("npc.demonslayer.form_shout", style.getDisplayName(), f.getDisplayName()), 50);
        }
        face(foe);
        if (AERIAL.contains(f.name()) && this.isOnGround()) {
            Vec3 to = foe.position().subtract(this.position()).multiply(1, 0, 1);
            Vec3 leap = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(0.3) : Vec3.ZERO;
            this.setDeltaMovement(leap.x, 0.62, leap.z);
            this.hasImpulse = true;
        }
        pendingForm = f;
        pendingDelay = 5;
        formCooldown = sleepTicks > 0 ? 50 : 90 + this.random.nextInt(90);
    }

    private void strike(com.wassuples.demonslayer.styles.BreathingForms f, LivingEntity foe, ServerLevel server) {
        face(foe);
        f.performAction(this, server);
        server.addFreshEntity(new com.wassuples.demonslayer.entity.custom.BreathSlashEntity(server, this, style, strokeOf(f), this.getYRot()));
        this.swing(InteractionHand.MAIN_HAND);
    }

    /** Squares up to {@code foe} at once (forms strike along the user's facing). */
    private void face(LivingEntity foe) {
        double dx = foe.getX() - this.getX(), dz = foe.getZ() - this.getZ();
        double dy = foe.getEyeY() - this.getEyeY();
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(net.minecraft.util.Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI));
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.setXRot(pitch);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level.isClientSide) com.wassuples.demonslayer.forms.ActiveFormManager.forget(this);
        super.remove(reason);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            com.wassuples.demonslayer.forms.baseForms.ActiveForm active = com.wassuples.demonslayer.forms.ActiveFormManager.getActiveForm(this);
            if (active != null) active.tick();
        }
    }

    private static com.wassuples.demonslayer.styles.BreathingStyles defaultStyle(StoryCharacter character) {
        return switch (character) {
            case TANJIRO -> com.wassuples.demonslayer.styles.BreathingStyles.WATER;
            case ZENITSU -> com.wassuples.demonslayer.styles.BreathingStyles.THUNDER;
            case INOSUKE -> com.wassuples.demonslayer.styles.BreathingStyles.BEAST;
            case KANAO -> com.wassuples.demonslayer.styles.BreathingStyles.FLOWER;
            default -> com.wassuples.demonslayer.styles.BreathingStyles.NONE;
        };
    }

    public com.wassuples.demonslayer.styles.BreathingStyles style() {
        return style;
    }

    public void setHiding(boolean hiding) {
        this.hiding = hiding;
        if (hiding) this.setTarget(null);
    }

    public boolean isHiding() {
        return hiding;
    }

    /** Locked in a fight: its foe is close, or it was hit in the last few seconds. */
    public boolean inCombat() {
        LivingEntity t = this.getTarget();
        if (t != null && t.isAlive() && t.distanceToSqr(this) < 8 * 8) return true;
        return this.getLastHurtByMob() != null && this.tickCount - this.getLastHurtByMobTimestamp() < COMBAT_GRACE;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !character().isTwin() && super.isPushable();
    }

    // ------------------------------------------------------------------ talking

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp && this.isAlive()) {
            if (character().fighter() && inCombat()) {
                // no time to talk mid-fight: a shout instead
                shout(Component.translatable("npc.demonslayer.busy." + this.random.nextInt(4)), 50);
            } else {
                DialogueManager.open(sp, this);
            }
        }
        return InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    /** Stops and faces {@code player} for a while (dialogue). */
    public void beginTalking(Player player, int ticks) {
        this.talkingTo = player.getUUID();
        this.talkTicks = ticks;
        this.getNavigation().stop();
    }

    public void endTalking(Player player) {
        if (player.getUUID().equals(talkingTo)) {
            talkingTo = null;
            talkTicks = 0;
        }
    }

    @Nullable
    Player talker() {
        return talkingTo == null ? null : this.level.getPlayerByUUID(talkingTo);
    }

    /**
     * Speaks in the world: a speech bubble above the head for everyone nearby. People take turns: while this NPC or
     * anyone standing next to it (the twins side by side...) is still being read, the line waits its turn, so bubbles
     * never cover one another.
     */
    public void say(Component text, int ticks) {
        if (this.level.isClientSide) return;
        if (speechQueue.isEmpty() && this.level.getGameTime() >= turnFree()) emit(text, ticks);
        else if (speechQueue.size() < 4) speechQueue.add(new Object[]{text, ticks});
    }

    /** Speech bubble sized to the text: roughly 1 s per 14 characters, at least 3 s. */
    public void say(Component text) {
        say(text, readingTicks(text));
    }

    /** How long a line stays up: roughly 1 s per 14 characters, at least 3 s, at most 12 s. */
    public static int readingTicks(Component text) {
        return Math.max(60, Math.min(240, 30 + text.getString().length() * 3 / 2));
    }

    /** A shout in the heat of a fight: said now if nobody nearby is talking, otherwise dropped (never queued). */
    public void shout(Component text, int ticks) {
        if (!this.level.isClientSide && speechQueue.isEmpty() && this.level.getGameTime() >= turnFree()) emit(text, ticks);
    }

    private final java.util.ArrayDeque<Object[]> speechQueue = new java.util.ArrayDeque<>();
    /** Game time until which this NPC's current line is being read. */
    private long speakingUntil;

    private void emit(Component text, int ticks) {
        speakingUntil = this.level.getGameTime() + ticks;
        ModMessages.sendToTrackingAndSelf(new NpcSpeechS2CPacket(this.getId(), text, ticks), this);
    }

    /** When it's next this NPC's turn to speak: after its own line and those of anyone within a few blocks. */
    private long turnFree() {
        long free = speakingUntil;
        for (StoryNpcEntity other : this.level.getEntitiesOfClass(StoryNpcEntity.class, this.getBoundingBox().inflate(5.0D), o -> o != this)) {
            free = Math.max(free, other.speakingUntil);
        }
        return free;
    }

    private void tickSpeech() {
        if (speechQueue.isEmpty() || this.tickCount % 5 != 0 || this.level.getGameTime() < turnFree()) return;
        Object[] next = speechQueue.poll();
        emit((Component) next[0], (Integer) next[1]);
    }

    // ------------------------------------------------------------------ story controls

    public void setAnchor(@Nullable BlockPos anchor, int leash) {
        this.anchor = anchor;
        this.leash = Math.max(0, leash);
    }

    @Nullable
    public BlockPos anchor() {
        return anchor;
    }

    public int leash() {
        return leash;
    }

    public void setRestYaw(float yaw) {
        this.restYaw = yaw;
    }

    public void setCompanion(@Nullable UUID companion) {
        this.companion = companion;
    }

    @Nullable
    public UUID companionId() {
        return companion;
    }

    @Nullable
    public Player getCompanion() {
        return companion == null ? null : this.level.getPlayerByUUID(companion);
    }

    /** Join the fight against the Hand Demon (accepted) or keep to other demons (declined). */
    public void setFightsHandDemon(boolean fights) {
        this.fightsHandDemon = fights;
        this.keepWithin = fights ? 8 : 26;
        if (!fights && this.getTarget() instanceof HandDemonEntity) this.setTarget(null);
    }

    public boolean fightsHandDemon() {
        return fightsHandDemon;
    }

    /** Appears near {@code player} (used when the story needs them close). */
    public boolean comeTo(Player player) {
        return teleportNear(player.position(), 4);
    }

    /** Teleports to a standable spot about {@code radius} blocks from {@code centre}. */
    public boolean teleportNear(Vec3 centre, int radius) {
        if (!(this.level instanceof ServerLevel server)) return false;
        for (int i = 0; i < 16; i++) {
            double ang = this.random.nextDouble() * Math.PI * 2;
            double r = radius * (0.4 + this.random.nextDouble() * 0.6);
            BlockPos p = new BlockPos(centre.x + Math.cos(ang) * r, centre.y, centre.z + Math.sin(ang) * r);
            for (int dy = 4; dy >= -4; dy--) {
                BlockPos q = p.offset(0, dy, 0);
                if (server.getBlockState(q.below()).isFaceSturdy(server, q.below(), net.minecraft.core.Direction.UP)
                        && server.getBlockState(q).getCollisionShape(server, q).isEmpty()
                        && server.getBlockState(q.above()).getCollisionShape(server, q.above()).isEmpty()
                        && server.getFluidState(q).isEmpty()) {
                    this.teleportTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
                    this.getNavigation().stop();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Character", character().name());
        tag.putInt("Variant", variant());
        tag.putString("Style", style.name());
        if (anchor != null) {
            tag.putInt("AnchorX", anchor.getX());
            tag.putInt("AnchorY", anchor.getY());
            tag.putInt("AnchorZ", anchor.getZ());
        }
        tag.putInt("Leash", leash);
        if (companion != null) tag.putUUID("Companion", companion);
        tag.putBoolean("FightsHandDemon", fightsHandDemon);
        tag.putInt("KeepWithin", keepWithin);
        tag.putFloat("RestYaw", restYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            this.entityData.set(CHARACTER, StoryCharacter.valueOf(tag.getString("Character")).ordinal());
        } catch (IllegalArgumentException ignored) {
            this.entityData.set(CHARACTER, 0);
        }
        this.scale = character().scale();
        this.entityData.set(VARIANT, tag.getInt("Variant"));
        try {
            style = com.wassuples.demonslayer.styles.BreathingStyles.valueOf(tag.getString("Style"));
        } catch (IllegalArgumentException e) {
            style = defaultStyle(character());
        }
        anchor = tag.contains("AnchorX") ? new BlockPos(tag.getInt("AnchorX"), tag.getInt("AnchorY"), tag.getInt("AnchorZ")) : null;
        leash = tag.getInt("Leash");
        companion = tag.hasUUID("Companion") ? tag.getUUID("Companion") : null;
        fightsHandDemon = tag.getBoolean("FightsHandDemon");
        if (tag.contains("KeepWithin")) keepWithin = tag.getInt("KeepWithin");
        restYaw = tag.getFloat("RestYaw");
        this.refreshDimensions();
    }

    // ================================================================== goals

    /** Stand still and face the player you're talking with. */
    static class FaceTalkerGoal extends Goal {
        private final StoryNpcEntity npc;

        FaceTalkerGoal(StoryNpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = npc.talker();
            return p != null && npc.getTarget() == null && npc.distanceToSqr(p) < 100.0D;
        }

        @Override
        public void start() {
            npc.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player p = npc.talker();
            if (p != null) npc.getLookControl().setLookAt(p, 30.0F, 30.0F);
        }
    }

    /** Keeps near the companion, or within the leash of the anchor; teleports when left very far behind. */
    static class KeepNearGoal extends Goal {
        private final StoryNpcEntity npc;
        private int repath;

        KeepNearGoal(StoryNpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Nullable
        private Vec3 goal() {
            Player p = npc.getCompanion();
            if (p != null && p.level == npc.level) return p.position();
            return npc.anchor == null ? null : Vec3.atBottomCenterOf(npc.anchor);
        }

        private double range() {
            return npc.getCompanion() != null ? npc.keepWithin : Math.max(1.5, npc.leash);
        }

        @Override
        public boolean canUse() {
            Vec3 g = goal();
            if (g == null || npc.getTarget() != null || npc.talker() != null) return false;
            double r = range();
            return npc.position().distanceToSqr(g) > r * r;
        }

        @Override
        public boolean canContinueToUse() {
            Vec3 g = goal();
            if (g == null || npc.getTarget() != null || npc.talker() != null) return false;
            double r = Math.max(1.0, range() * 0.5);
            return npc.position().distanceToSqr(g) > r * r;
        }

        @Override
        public void tick() {
            Vec3 g = goal();
            if (g == null) return;
            if (npc.position().distanceToSqr(g) > 64.0D * 64.0D) {
                if (npc.teleportNear(g, npc.getCompanion() != null ? 4 : Math.max(1, npc.leash))) return;
            }
            if (--repath <= 0) {
                repath = 10;
                npc.getNavigation().moveTo(g.x, g.y, g.z, 1.1D);
            }
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
            if (npc.companion == null && npc.leash <= 1 && npc.anchor != null) {
                npc.setYRot(npc.restYaw);
                npc.setYHeadRot(npc.restYaw);
                npc.setYBodyRot(npc.restYaw);
            }
        }
    }

    /** Idle strolling within the leash (not for NPCs leashed tight, like the twins at the shrine). */
    static class StrollNearAnchorGoal extends Goal {
        private final StoryNpcEntity npc;
        @Nullable
        private Vec3 target;

        StrollNearAnchorGoal(StoryNpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (npc.anchor == null || npc.leash < 3 || npc.getCompanion() != null || npc.talker() != null) return false;
            if (npc.getRandom().nextInt(90) != 0) return false;
            Vec3 pos = DefaultRandomPos.getPos(npc, Math.min(npc.leash, 8), 3);
            if (pos == null || pos.distanceToSqr(Vec3.atBottomCenterOf(npc.anchor)) > (double) npc.leash * npc.leash) return false;
            target = pos;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && !npc.getNavigation().isDone() && npc.talker() == null;
        }

        @Override
        public void start() {
            if (target != null) npc.getNavigation().moveTo(target.x, target.y, target.z, 0.8D);
        }
    }

    /**
     * The fearful (Zenitsu always when a demon comes close; the anonymous candidates once badly hurt) run from the
     * fight, shouting, until they are clear of it. A fainted Zenitsu doesn't run: he sleeps and strikes.
     */
    static class FleeGoal extends Goal {
        private final StoryNpcEntity npc;
        @Nullable
        private Vec3 to;
        @Nullable
        private LivingEntity threat;

        FleeGoal(StoryNpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (npc.behavior() != com.wassuples.demonslayer.nemesis.ai.Behavior.COWARDLY || npc.sleepTicks > 0 || !npc.character().fighter()) return false;
            boolean zenitsu = npc.character() == StoryCharacter.ZENITSU;
            if (!zenitsu && npc.getHealth() > npc.getMaxHealth() * 0.35F) return false;
            java.util.List<Monster> near = npc.level.getEntitiesOfClass(Monster.class, npc.getBoundingBox().inflate(zenitsu ? 7.0D : 9.0D), LivingEntity::isAlive);
            if (near.isEmpty()) return false;
            threat = near.get(0);
            to = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(npc, 12, 5, threat.position());
            return to != null;
        }

        @Override
        public boolean canContinueToUse() {
            return to != null && npc.sleepTicks <= 0 && !npc.getNavigation().isDone() && threat != null && threat.isAlive() && threat.distanceToSqr(npc) < 14 * 14;
        }

        @Override
        public void start() {
            if (to != null) npc.getNavigation().moveTo(to.x, to.y, to.z, 1.4D);
            npc.setTarget(null);
            if (npc.random.nextInt(2) == 0) {
                String key = npc.character() == StoryCharacter.CANDIDATE ? "candidate" : npc.character().key();
                npc.shout(Component.translatable("npc.demonslayer." + key + ".flee"), 50);
            }
        }
    }

    /** Tanjiro can't leave anyone in trouble: he goes after whatever is mauling a badly hurt candidate nearby. */
    static class ProtectAllyGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final StoryNpcEntity npc;
        @Nullable
        private LivingEntity attacker;

        ProtectAllyGoal(StoryNpcEntity npc) {
            super(npc, false);
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (npc.behavior() != com.wassuples.demonslayer.nemesis.ai.Behavior.BODYGUARD || npc.hiding || npc.tickCount % 10 != 0) return false;
            for (StoryNpcEntity ally : npc.level.getEntitiesOfClass(StoryNpcEntity.class, npc.getBoundingBox().inflate(20.0D),
                    a -> a != npc && a.isAlive() && a.character().fighter() && a.getHealth() < a.getMaxHealth() * 0.5F)) {
                LivingEntity foe = ally.getLastHurtByMob();
                if (foe instanceof Monster && foe.isAlive() && ally.tickCount - ally.getLastHurtByMobTimestamp() < 100) {
                    attacker = foe;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            npc.setTarget(attacker);
            npc.shout(Component.translatable("npc.demonslayer.tanjiro_kamado.protect"), 50);
            super.start();
        }
    }

    /**
     * Life between fights, by temperament: practising strokes, looking about, pacing, going over to a fellow candidate,
     * Tanjiro sniffing the air, Zenitsu trembling, Kanao tossing her coin, Genya glaring at whoever comes near.
     */
    static class IdleActGoal extends Goal {
        private final StoryNpcEntity npc;
        private int ticks, act;
        @Nullable
        private LivingEntity other;

        IdleActGoal(StoryNpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (npc.getTarget() != null || npc.talker() != null || npc.sleepTicks > 0 || npc.getCompanion() != null) return false;
            if (npc.character() == StoryCharacter.SWORDSMITH || npc.random.nextInt(npc.character().isTwin() ? 900 : 260) != 0) return false;
            act = npc.random.nextInt(4);
            if (npc.character().isTwin() && act == 3) act = 1;   // the twins never leave their post
            other = null;
            if (act == 3) {
                java.util.List<StoryNpcEntity> near = npc.level.getEntitiesOfClass(StoryNpcEntity.class, npc.getBoundingBox().inflate(10.0D),
                        o -> o != npc && o.getTarget() == null);
                if (near.isEmpty()) act = 0;
                else other = near.get(npc.random.nextInt(near.size()));
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return ticks > 0 && npc.getTarget() == null && npc.talker() == null && npc.sleepTicks <= 0;
        }

        @Override
        public void start() {
            ticks = 60 + npc.random.nextInt(60);
            String key = npc.character().key();
            switch (act) {
                case 0 -> {   // a characteristic gesture and, sometimes, a word to nobody in particular
                    if (npc.random.nextInt(3) == 0 && npc.level.getNearestPlayer(npc, 12.0D) != null && !npc.character().isTwin()) {
                        npc.say(Component.translatable("npc.demonslayer." + (npc.character() == StoryCharacter.CANDIDATE ? "candidate" : key) + ".idle." + npc.random.nextInt(3)));
                    }
                }
                case 3 -> {
                    if (other != null) npc.getNavigation().moveTo(other, 0.8D);
                }
                default -> {
                }
            }
        }

        @Override
        public void tick() {
            ticks--;
            switch (act) {
                case 1 -> {   // practising strokes (or, for the twins, standing very straight)
                    if (npc.character().fighter() && ticks % 15 == 0) npc.swing(InteractionHand.MAIN_HAND);
                }
                case 2 -> {   // looking about, by temperament
                    float yaw = npc.getYHeadRot() + Mth.sin(ticks * (npc.personality() == com.wassuples.demonslayer.nemesis.ai.Personality.WHINING ? 0.9F : 0.12F)) * 50.0F;
                    npc.getLookControl().setLookAt(npc.getX() - Mth.sin(yaw * Mth.DEG_TO_RAD) * 4, npc.getEyeY()
                            + (npc.character() == StoryCharacter.TANJIRO ? 1.2 : 0.0), npc.getZ() + Mth.cos(yaw * Mth.DEG_TO_RAD) * 4);
                    if (npc.character() == StoryCharacter.GENYA) {
                        Player p = npc.level.getNearestPlayer(npc, 8.0D);
                        if (p != null) npc.getLookControl().setLookAt(p, 30.0F, 30.0F);   // glaring
                    }
                }
                case 3 -> {
                    if (other != null) {
                        npc.getLookControl().setLookAt(other, 30.0F, 30.0F);
                        if (npc.distanceToSqr(other) < 2.5 * 2.5) npc.getNavigation().stop();
                    }
                }
                default -> {
                }
            }
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
        }
    }
}
