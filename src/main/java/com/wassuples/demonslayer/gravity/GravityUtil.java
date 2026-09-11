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
        Vec3 right = forwardOnPlane.cross(up);
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

    public static Vec3 getMovementVector(Vec3 lookVector, float gYaw, Vec3 localVector, GravityDirection gravityDirection) {
        if (gravityDirection == GravityDirection.DOWN) {
            float f = Mth.sin(gYaw * ((float)Math.PI / 180F));
            float f1 = Mth.cos(gYaw * ((float)Math.PI / 180F));
            return new Vec3(localVector.x * f1 - localVector.z * f, localVector.y, localVector.z * f1 + localVector.x * f);
        }

        Vec3 gUp = getWorldUp(gravityDirection);
        Vec3 relForward = projectOntoPlane(lookVector, gUp);
        if (relForward.lengthSqr() < EPSILON) {
            relForward = getYawForwardOnGravityPlane(gYaw, gravityDirection);
        } else {
            relForward = relForward.normalize();
        }

        Vec3 relLeft = gUp.cross(relForward);
        if (relLeft.lengthSqr() < EPSILON) {
            relLeft = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            relLeft = relLeft.normalize();
        }

        return relLeft.scale(localVector.x)
                .add(gUp.scale(localVector.y))
                .add(relForward.scale(localVector.z));
    }

    private static Vec3 getYawForwardOnGravityPlane(float gYaw, GravityDirection gravityDirection) {
        Vec3 gUp = getWorldUp(gravityDirection);
        Vec3 ref = (Math.abs(gUp.y) < 0.9D) ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 gForward = projectOntoPlane(ref, gUp).normalize();
        Vec3 gRight = gForward.cross(gUp).normalize();

        double radY = Math.toRadians(gYaw);
        double sinY = Math.sin(radY);
        double cosY = Math.cos(radY);
        return gForward.scale(cosY).add(gRight.scale(sinY)).normalize();
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
        return 0.0F;
    }

    public static float getCameraPitchOffsetDegrees(GravityDirection direction) {
        return 0.0F;
    }

    public static float[] gravityLookToVanilla(float gYaw, float gPitch, GravityDirection direction) {
        Vec3 gUp = getWorldUp(direction);
        Vec3 ref = (Math.abs(gUp.y) < 0.9) ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1);
        Vec3 gForward = projectOntoPlane(ref, gUp).normalize();
        Vec3 gRight = gForward.cross(gUp).normalize();

        double radP = Math.toRadians(gPitch);
        double radY = Math.toRadians(gYaw);

        double cosP = Math.cos(radP);
        double sinP = Math.sin(radP);
        double cosY = Math.cos(radY);
        double sinY = Math.sin(radY);

        // look = Forward * cosP * cosY + Right * cosP * sinY + Up * sinP
        Vec3 look = gForward.scale(cosP * cosY)
            .add(gRight.scale(cosP * sinY))
            .add(gUp.scale(sinP));

        // Vanilla look vector rotation (yaw, pitch) is:
        // x = -sin(yaw) * cos(pitch)
        // y = -sin(pitch)
        // z = cos(yaw) * cos(pitch)
        float vYaw   = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float vPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-look.y, -1.0, 1.0)));

        // Calculate Roll to make gUp point UP on screen.
        // Camera Up (Roll=0):
        // Up0 = RotateYaw(vYaw) * RotatePitch(vPitch) * (0, 1, 0)
        double radVY = Math.toRadians(vYaw);
        double radVP = Math.toRadians(vPitch);
        double sinVY = Math.sin(radVY);
        double cosVY = Math.cos(radVY);
        double sinVP = Math.sin(radVP);
        double cosVP = Math.cos(radVP);

        Vec3 vUp0 = new Vec3(-sinVY * sinVP, cosVP, cosVY * sinVP);

        // Desired Up is gUp projected onto the plane perpendicular to look.
        Vec3 desiredUp = gUp.subtract(look.scale(gUp.dot(look))).normalize();

        // Roll is angle between vUp0 and desiredUp around look.
        Vec3 vRight0 = look.cross(vUp0);
        double cosR = vUp0.dot(desiredUp);
        double sinR = vRight0.dot(desiredUp);
        float vRoll = (float) Math.toDegrees(Math.atan2(sinR, cosR));

        return new float[]{vYaw, vPitch, vRoll};
    }

    public static float[] vanillaToGravityLook(float vanillaYaw, float vanillaPitch, GravityDirection direction) {
        Vec3 look = Vec3.directionFromRotation(vanillaPitch, vanillaYaw);

        Vec3 gUp = getWorldUp(direction);
        Vec3 ref = (Math.abs(gUp.y) < 0.9) ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1);
        Vec3 gForward = projectOntoPlane(ref, gUp).normalize();
        Vec3 gRight = gForward.cross(gUp).normalize();

        // Project look onto gForward, gRight, gUp
        double lookF = look.dot(gForward);
        double lookR = look.dot(gRight);
        double lookU = look.dot(gUp);

        float gYaw = (float) Math.toDegrees(Math.atan2(lookR, lookF));
        float gPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(lookU, -1.0, 1.0)));
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

    public static boolean isAboveGroundForCustomGravity(Entity entity, GravityDirection direction, float stepHeight) {
        if (direction == GravityDirection.DOWN) {
            return entity.isOnGround();
        }

        return entity.isOnGround()
            || entity.fallDistance < stepHeight
            && !entity.level.noCollision(entity, entity.getBoundingBox().move(getWorldDown(direction).scale(stepHeight - entity.fallDistance)));
    }

    public static Vec3 maybeBackOffFromEdge(Entity entity, Vec3 movement, GravityDirection direction, float stepHeight) {
        if (direction == GravityDirection.DOWN || stepHeight <= 0.0F) {
            return movement;
        }

        Vec3 down = getWorldDown(direction);
        if (movement.dot(down) < -EPSILON || !isAboveGroundForCustomGravity(entity, direction, stepHeight)) {
            return movement;
        }

        Vec3[] lateralAxes = getLateralAxes(direction);
        Vec3 axisA = lateralAxes[0];
        Vec3 axisB = lateralAxes[1];
        double amountA = movement.dot(axisA);
        double amountB = movement.dot(axisB);
        double amountDown = movement.dot(down);
        Vec3 supportProbe = down.scale(stepHeight);
        AABB box = entity.getBoundingBox();

        while (amountA != 0.0D && entity.level.noCollision(entity, box.move(axisA.scale(amountA).add(supportProbe)))) {
            amountA = backOff(amountA);
        }

        while (amountB != 0.0D && entity.level.noCollision(entity, box.move(axisB.scale(amountB).add(supportProbe)))) {
            amountB = backOff(amountB);
        }

        while (amountA != 0.0D && amountB != 0.0D
            && entity.level.noCollision(entity, box.move(axisA.scale(amountA).add(axisB.scale(amountB)).add(supportProbe)))) {
            amountA = backOff(amountA);
            amountB = backOff(amountB);
        }

        return axisA.scale(amountA).add(axisB.scale(amountB)).add(down.scale(amountDown));
    }

    private static Vec3[] getLateralAxes(GravityDirection direction) {
        return switch (direction) {
            case DOWN, UP -> new Vec3[]{new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D)};
            case EAST, WEST -> new Vec3[]{new Vec3(0.0D, 1.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D)};
            case NORTH, SOUTH -> new Vec3[]{new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D)};
        };
    }

    private static double backOff(double amount) {
        double step = 0.05D;
        if (amount < step && amount >= -step) {
            return 0.0D;
        }
        return amount > 0.0D ? amount - step : amount + step;
    }

    private static void clampVelocityIntoSurface(Entity entity, GravityDirection direction) {
        Vec3 down = getWorldDown(direction);
        Vec3 velocity = entity.getDeltaMovement();
        double downSpeed = velocity.dot(down);
        entity.setDeltaMovement(velocity.subtract(down.scale(downSpeed)));
    }
}
