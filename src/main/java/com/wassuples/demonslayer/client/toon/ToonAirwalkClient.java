package com.wassuples.demonslayer.client.toon;

import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Toon Force cartoon physics: walk off a ledge and you keep walking on thin air at exactly the height you
 * left from, until you look down. Then you fall like normal.
 *
 * <p>Runs at the END of the local player's tick (after vanilla movement, before the position packet is sent),
 * because player movement is client-authoritative: the server simply receives a steady Y with onGround=true.
 *
 * <ul>
 *   <li><b>Small falls</b>: if the fall wouldn't hurt (vanilla fall-damage maths, including Jump Boost, Slow
 *   Falling, and landing in water/slime/powder snow/cobwebs or on hay/honey/beds), it's a normal step down —
 *   no airwalk. Airwalk only kicks in when the fall would actually cause fall damage.</li>
 *   <li><b>Jumping</b>: the take-off height is remembered; coming back down over a gap you land on that same
 *   invisible floor. You can jump again from it.</li>
 *   <li><b>Creative flight / elytra / water / lava / ladders / riding / spectator / levitation / custom gravity</b>:
 *   airwalk steps aside entirely and forgets the stored height, so stopping flight or leaving water mid-air
 *   is an ordinary fall.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class ToonAirwalkClient {
    /** Pitch (degrees, positive = down) at which you "notice" there's nothing under you. */
    public static final float LOOK_DOWN_PITCH = 50.0f;
    /** Deepest drop we look for ground in. Anything deeper is treated as a damaging fall. */
    private static final int MAX_SCAN = 384;
    /** How far below the stored height we still catch the player (one tick of falling is ~0.08–0.5). */
    private static final double CATCH_WINDOW = 0.6;

    private static double anchorY = Double.NaN;
    private static boolean airwalking;
    /** Looked down: fall normally until real ground (or water etc.) is reached. */
    private static boolean dropped;

    private ToonAirwalkClient() {
    }

    public static boolean isAirwalking() {
        return airwalking;
    }

    public static void reset() {
        anchorY = Double.NaN;
        airwalking = false;
        dropped = false;
    }

    public static void tick(LocalPlayer player) {
        if (!isEligible(player)) {
            reset();
            return;
        }

        if (hasRealGround(player)) {
            anchorY = player.getY();
            airwalking = false;
            dropped = false;
            return;
        }

        if (dropped || Double.isNaN(anchorY)) {
            airwalking = false;
            return;
        }

        // Looking down breaks the spell, whether walking on air or mid-jump.
        if (player.getXRot() > LOOK_DOWN_PITCH) {
            dropped = true;
            airwalking = false;
            anchorY = Double.NaN;
            return;
        }

        double y = player.getY();
        Vec3 velocity = player.getDeltaMovement();

        if (!airwalking) {
            boolean descending = velocity.y <= 0.0;
            boolean crossedAnchor = y <= anchorY + 1.0E-4 && y >= anchorY - CATCH_WINDOW;
            if (!descending || !crossedAnchor) {
                if (y < anchorY - CATCH_WINDOW) anchorY = Double.NaN; // knocked down past it: just fall
                return;
            }
            if (!wouldTakeFallDamage(player)) {
                anchorY = Double.NaN; // harmless drop: ordinary step down
                return;
            }
        }

        // Snap onto the invisible floor, if that spot isn't inside a block.
        AABB atAnchor = player.getBoundingBox().move(0.0, anchorY - y, 0.0);
        if (!player.level.noCollision(player, atAnchor)) {
            airwalking = false;
            anchorY = Double.NaN;
            return;
        }

        player.setPos(player.getX(), anchorY, player.getZ());
        player.setDeltaMovement(velocity.x, 0.0, velocity.z);
        player.fallDistance = 0.0f;
        // onGround lets the player jump and get ground friction; skipped while sneaking, because vanilla's
        // sneak edge-guard would otherwise refuse every step on a floor that has no blocks.
        player.setOnGround(!player.isShiftKeyDown());
        airwalking = true;
    }

    private static boolean isEligible(LocalPlayer player) {
        if (player.isSpectator() || player.isPassenger() || player.getAbilities().flying || player.isFallFlying()
                || player.isInWater() || player.isInLava() || player.onClimbable()
                || player.hasEffect(MobEffects.LEVITATION) || !player.isAlive()) {
            return false;
        }
        if (GravityUtil.getEffectiveGravity(player) != GravityDirection.DOWN) {
            return false;
        }
        return player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                .map(demon -> demon.isDemon() && demon.getEquippedBda() == BloodDemonArts.TOON_FORCE)
                .orElse(false);
    }

    private static boolean hasRealGround(LocalPlayer player) {
        return !player.level.noCollision(player, player.getBoundingBox().expandTowards(0.0, -0.06, 0.0));
    }

    /**
     * Predicts whether falling straight down from here would hurt, using vanilla's formula:
     * damage = ceil((fallDistance - 3 - jumpBoostLevel) * landingMultiplier).
     */
    private static boolean wouldTakeFallDamage(LocalPlayer player) {
        if (player.hasEffect(MobEffects.SLOW_FALLING)) return false;

        AABB box = player.getBoundingBox();
        double startY = box.minY;
        double minY = player.level.getMinBuildHeight();
        double landingY = Double.NaN;
        BlockPos landingBlock = null;

        // Sweep the hitbox down half a block at a time; refine to the exact surface on the first hit.
        for (double drop = 0.5; drop <= MAX_SCAN && startY - drop >= minY - 1; drop += 0.5) {
            AABB probe = box.move(0.0, -drop, 0.0);
            if (player.level.containsAnyLiquid(probe)) return false; // splashing into water/lava: no fall damage
            if (!player.level.noCollision(player, probe)) {
                double lo = drop - 0.5;
                double hi = drop;
                for (int i = 0; i < 8; i++) {
                    double mid = (lo + hi) * 0.5;
                    if (player.level.noCollision(player, box.move(0.0, -mid, 0.0))) lo = mid; else hi = mid;
                }
                landingY = startY - lo;
                landingBlock = new BlockPos(player.getX(), landingY - 0.2, player.getZ());
                break;
            }
        }
        if (Double.isNaN(landingY)) return true; // void or an enormous drop

        BlockState landing = player.level.getBlockState(landingBlock);
        if (landing.is(Blocks.SLIME_BLOCK) || landing.is(Blocks.POWDER_SNOW) || landing.is(Blocks.COBWEB)
                || landing.is(Blocks.SCAFFOLDING)) {
            return false;
        }
        float multiplier = 1.0f;
        if (landing.is(Blocks.HAY_BLOCK) || landing.is(Blocks.HONEY_BLOCK)) multiplier = 0.2f;
        else if (landing.is(BlockTags.BEDS)) multiplier = 0.5f;

        MobEffectInstance jump = player.getEffect(MobEffects.JUMP);
        float jumpBoost = jump == null ? 0.0f : jump.getAmplifier() + 1.0f;
        // fallDistance already holds what was fallen since the jump apex / leaving the ledge.
        float fallDistance = player.fallDistance + (float) (startY - landingY);
        return Mth.ceil((fallDistance - 3.0f - jumpBoost) * multiplier) > 0;
    }
}
