package com.wassuples.demonslayer.gravity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GravityUtil {
    private static final double EPSILON = 1.0E-6;
    private static final double SUPPORT_CHECK_DISTANCE = 0.05D;
    public static final double GRAVITY_ACCEL_PER_TICK = 0.08D;
    public static final double AIR_DRAG_PER_TICK = 0.98D;
    public static final double TERMINAL_SPEED = 3.92D;

    private GravityUtil() {
    }

    public static Vec3 getWorldDown(GravityDirection direction) {
        return direction.getVector();
    }

    public static Vec3 getWorldUp(GravityDirection direction) {
        return direction.getVector().scale(-1.0D);
    }

    public static AABB makeBoundingBox(Entity entity, GravityDirection direction) {
        Pose pose = entity.getPose();
        EntityDimensions dimensions = entity.getDimensions(pose);
        return makeBoundingBox(entity.position(), dimensions, direction);
    }

    public static AABB makeBoundingBox(Vec3 position, EntityDimensions dimensions, GravityDirection direction) {
        double hw = dimensions.width * 0.5D;
        double h  = dimensions.height;
        double x  = position.x, y = position.y, z = position.z;

        return switch (direction) {
            // Vanilla case: feet at min-Y, body rises to +Y
            case DOWN  -> dimensions.makeBoundingBox(position);

            // Feet at max-Y (ceiling), body hangs downward
            case UP    -> new AABB(x - hw, y - h,  z - hw,
                x + hw, y,      z + hw);

            // Feet at max-X (+X wall), body extends toward -X
            case EAST  -> new AABB(x - h,  y - hw, z - hw,
                x,      y + hw, z + hw);

            // Feet at min-X (-X wall), body extends toward +X
            case WEST  -> new AABB(x,      y - hw, z - hw,
                x + h,  y + hw, z + hw);

            // Feet at max-Z (+Z wall), body extends toward -Z
            case SOUTH -> new AABB(x - hw, y - hw, z - h,
                x + hw, y + hw, z);

            // Feet at min-Z (-Z wall), body extends toward +Z
            case NORTH -> new AABB(x - hw, y - hw, z,
                x + hw, y + hw, z + h);
        };
    }

    /**
     * Converts an entity's gravity anchor (its stored position) to the center of its body volume.
     */
    public static Vec3 centerFromAnchor(Vec3 anchorPosition, double bodyHeight, GravityDirection direction) {
        return anchorPosition.subtract(getWorldDown(direction).scale(bodyHeight * 0.5D));
    }

    /**
     * Converts a body center to the gravity anchor expected by the given direction.
     */
    public static Vec3 anchorFromCenter(Vec3 centerPosition, double bodyHeight, GravityDirection direction) {
        return centerPosition.add(getWorldDown(direction).scale(bodyHeight * 0.5D));
    }

    /**
     * Reanchors a position across gravity changes while preserving the same physical body center.
     */
    public static Vec3 reanchorForGravityChange(Vec3 currentAnchor, double bodyHeight, GravityDirection from, GravityDirection to) {
        Vec3 center = centerFromAnchor(currentAnchor, bodyHeight, from);
        return anchorFromCenter(center, bodyHeight, to);
    }

    /**
     * Moves the entity's anchor to keep its collision volume stable when gravity changes.
     */
    public static boolean reanchorEntityForGravityChange(Entity entity, GravityDirection from, GravityDirection to) {
        if (from == to) {
            return false;
        }

        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        Vec3 newAnchor = reanchorForGravityChange(entity.position(), dimensions.height, from, to);
        entity.setPos(newAnchor.x, newAnchor.y, newAnchor.z);
        return true;
    }

    public static void refreshGravityHitbox(Entity entity, GravityDirection direction) {
        entity.setBoundingBox(makeBoundingBox(entity, direction));
    }

    public static Vec3 projectOntoPlane(Vec3 vector, Vec3 planeNormal) {
        double dot = vector.dot(planeNormal);
        return vector.subtract(planeNormal.scale(dot));
    }

    public static Vec3 getForwardOnGravityPlane(Vec3 lookVector, GravityDirection gravityDirection) {
        Vec3 down = getWorldDown(gravityDirection);
        Vec3 projected = projectOntoPlane(lookVector, down);
        if (projected.lengthSqr() < EPSILON) {
            Vec3 fallback = projectOntoPlane(new Vec3(0.0D, 0.0D, 1.0D), down);
            if (fallback.lengthSqr() < EPSILON) {
                fallback = projectOntoPlane(new Vec3(0.0D, 1.0D, 0.0D), down);
            }
            return fallback.normalize();
        }
        return projected.normalize();
    }

    public static Vec3 getRightOnGravityPlane(Vec3 forwardOnPlane, GravityDirection gravityDirection) {
        Vec3 up = getWorldUp(gravityDirection);
        Vec3 right = up.cross(forwardOnPlane);
        if (right.lengthSqr() < EPSILON) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return right.normalize();
    }

    public static Vec3 getMovementForward(Vec3 lookVector, float yRot, GravityDirection gravityDirection) {
        Vec3 down = getWorldDown(gravityDirection);
        Vec3 projected = projectOntoPlane(lookVector, down);
        if (projected.lengthSqr() < EPSILON) {
            Vec3 fallback = projectOntoPlane(Vec3.directionFromRotation(0.0F, yRot), down);
            if (fallback.lengthSqr() < EPSILON) {
                fallback = projectOntoPlane(new Vec3(1.0D, 0.0D, 0.0D), down);
            }
            return fallback.normalize();
        }
        return projected.normalize();
    }

    public static Vec3 getMovementVector(Vec3 lookVector, float yRot, Vec3 localVector, GravityDirection gravityDirection) {
        Vec3 forward = getMovementForward(lookVector, yRot, gravityDirection);
        Vec3 right = getRightOnGravityPlane(forward, gravityDirection);
        Vec3 up = getWorldUp(gravityDirection);
        return right.scale(-localVector.x)
                .add(up.scale(localVector.y))
                .add(forward.scale(-localVector.z));
    }

    public static Vec3 rotateWorldMovementForGravity(Vec3 vector, GravityDirection direction) {
        return switch (direction) {
            case DOWN -> vector;
            case UP -> new Vec3(-vector.x, -vector.y, vector.z);
            case EAST -> new Vec3(-vector.y, vector.x, vector.z);
            case WEST -> new Vec3(vector.y, -vector.x, vector.z);
            case NORTH -> new Vec3(vector.x, -vector.z, vector.y);
            case SOUTH -> new Vec3(vector.x, vector.z, -vector.y);
        };
    }

    /**
     * Backward-compatible transform used by existing gravity physics/jump hooks.
     */
    public static Vec3 transformBasedOnGravity(Vec3 localVector, GravityDirection direction) {
        Vec3 up = getWorldUp(direction);
        Vec3 forward = getForwardOnGravityPlane(new Vec3(0.0D, 0.0D, 1.0D), direction);
        Vec3 right = getRightOnGravityPlane(forward, direction);

        return right.scale(localVector.x)
                .add(up.scale(localVector.y))
                .add(forward.scale(localVector.z));
    }

    public static float getCameraRollDegrees(GravityDirection direction) {
        return switch (direction) {
            case UP -> 180.0F;
            case WEST -> -90.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }

    public static float getCameraPitchOffsetDegrees(GravityDirection direction) {
        return switch (direction) {
            case NORTH -> -90.0F;
            case SOUTH -> 90.0F;
            default -> 0.0F;
        };
    }

    public static double[] remapTurnDeltas(double deltaX, double deltaY, GravityDirection direction) {
        return switch (direction) {
            // Vanilla: no change.
            case DOWN  -> new double[]{deltaX,   deltaY};
            // Upside-down: only pitch inverts (screen top/bottom flip, left/right stays).
            case UP    -> new double[]{deltaX,  -deltaY};
            // East wall: right = world+Y (up in world). Swap and negate both.
            case EAST  -> new double[]{-deltaY, -deltaX};
            // West wall: right = world−Y (down in world). Swap, no negation.
            case WEST  -> new double[]{deltaY,   deltaX};
            // North/South: broken by gimbal lock regardless of remap.
            case NORTH -> new double[]{deltaY,  -deltaX};
            case SOUTH -> new double[]{-deltaY,  deltaX};
        };
    }

    public static float[] gravityLookToVanilla(float gYaw, float gPitch, GravityDirection direction) {
        Vec3 gForward = getForwardOnGravityPlane(new Vec3(1, 0, 0), direction); // use +X as stable reference
        Vec3 gRight   = getRightOnGravityPlane(gForward, direction);
        Vec3 gUp      = getWorldUp(direction);

        double cosP = Math.cos(Math.toRadians(gPitch));
        double sinP = Math.sin(Math.toRadians(gPitch));
        double cosY = Math.cos(Math.toRadians(gYaw));
        double sinY = Math.sin(Math.toRadians(gYaw));

        // World-space look direction built from the gravity basis.
        Vec3 look = gForward.scale(cosP * cosY)
            .add(gRight.scale(cosP * sinY))
            .add(gUp.scale(-sinP));

        float vanillaYaw   = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float vanillaPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-look.y, -1.0, 1.0)));
        return new float[]{vanillaYaw, vanillaPitch};
    }

    public static float[] vanillaToGravityLook(float vanillaYaw, float vanillaPitch, GravityDirection direction) {
        Vec3 look     = Vec3.directionFromRotation(vanillaPitch, vanillaYaw);
        Vec3 gForward = getForwardOnGravityPlane(new Vec3(1, 0, 0), direction);
        Vec3 gRight   = getRightOnGravityPlane(gForward, direction);
        Vec3 gUp      = getWorldUp(direction);

        double lookF  = look.dot(gForward);
        double lookR  = look.dot(gRight);
        double lookU  = look.dot(gUp);

        float gYaw   = (float) Math.toDegrees(Math.atan2(lookR, lookF));
        float gPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-lookU, -1.0, 1.0)));
        return new float[]{gYaw, gPitch};
    }

    /**
     * Applies one gravity step for the provided direction using vanilla-like drag and terminal speed.
     */
    public static Vec3 applyCustomGravityStep(Vec3 currentVelocity, GravityDirection direction, boolean supported) {
        Vec3 down = getWorldDown(direction);

        double downSpeed = currentVelocity.dot(down);
        Vec3 lateral = currentVelocity.subtract(down.scale(downSpeed));

        if (supported) {
            // Zero the full gravity-axis component when grounded — positive AND negative.
            // Preserving a negative (away-from-surface) speed is what causes the bounce.
            return lateral;
        }

        // Vanilla order: apply acceleration FIRST, then drag.
        double nextDownSpeed = downSpeed + GRAVITY_ACCEL_PER_TICK;
        nextDownSpeed = nextDownSpeed * AIR_DRAG_PER_TICK;
        if (nextDownSpeed > TERMINAL_SPEED) {
            nextDownSpeed = TERMINAL_SPEED;
        }
        return lateral.add(down.scale(nextDownSpeed));
    }

    public static boolean isStandingOnCustomSurface(Entity entity, GravityDirection direction) {
        Vec3 down = getWorldDown(direction).scale(SUPPORT_CHECK_DISTANCE);
        return !entity.level.noCollision(entity, entity.getBoundingBox().move(down));
    }

    public static boolean updateGroundStateForCustomGravity(Entity entity, GravityDirection direction, boolean forceUngrounded) {
        if (direction == GravityDirection.DOWN) {
            return false;
        }

        if (forceUngrounded) {
            entity.setOnGround(false);
            return false;
        }

        boolean grounded = isStandingOnCustomSurface(entity, direction);
        if (grounded) {
            clampVelocityIntoSurface(entity, direction);
        }

        entity.setOnGround(grounded);
        return grounded;
    }

    private static void clampVelocityIntoSurface(Entity entity, GravityDirection direction) {
        Vec3 down = getWorldDown(direction);
        Vec3 velocity = entity.getDeltaMovement();
        double downSpeed = velocity.dot(down);
        entity.setDeltaMovement(velocity.subtract(down.scale(downSpeed)));
    }
}
