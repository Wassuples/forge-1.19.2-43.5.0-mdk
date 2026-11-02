package com.wassuples.demonslayer.gravity;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GravityMovementTest {
    private static final double EPSILON = 2.0E-4; // Vanilla uses a sine lookup table.

    @Test
    void keyboardDirectionsFollowProjectedWorldLookForAllGravityDirections() {
        Vec3[] looks = {new Vec3(0.4, -0.7, 0.8), new Vec3(-0.8, 0.2, -0.5),
            new Vec3(0.1, 0.9, -0.3)};
        for (GravityDirection direction : GravityDirection.values()) {
            for (Vec3 look : looks) checkKeyboardDirections(look.normalize(), direction);
        }
    }

    @Test
    void gravityCameraAnglesProduceMatchingKeyboardMovement() {
        for (GravityDirection direction : GravityDirection.values()) {
            for (float yaw : new float[] {0, 45, 90, 180, -135}) {
                for (float pitch : new float[] {-60, 0, 60}) {
                    float[] camera = GravityUtil.gravityLookToVanilla(yaw, pitch, direction);
                    Vec3 look = vanillaLook(camera[0], camera[1]);
                    assertEquals(1, look.length(), EPSILON);
                    checkKeyboardDirections(look, direction);
                }
            }
        }
    }

    @Test
    void eastGravityForwardCanMoveAlongNegativeAbsoluteY() {
        Vec3 look = new Vec3(0, -1, 0);
        float yaw = GravityUtil.getMovementYaw(look, GravityDirection.EAST, 0);
        assertVectorEquals(look, keyboardMovement(0, 1, yaw, GravityDirection.EAST));
        assertVectorEquals(new Vec3(0, 1, 0), keyboardMovement(0, -1, yaw, GravityDirection.EAST));
        float[] camera = GravityUtil.gravityLookToVanilla(180, 0, GravityDirection.EAST);
        assertVectorEquals(look, vanillaLook(camera[0], camera[1]));
        checkKeyboardDirections(vanillaLook(camera[0], camera[1]), GravityDirection.EAST);
    }

    @Test
    void gravityPolesRetainFallbackYawInsteadOfIntroducingAnArbitraryHeading() {
        for (GravityDirection direction : GravityDirection.values()) {
            Vec3 up = direction.getVector().scale(-1);
            Vec3 tangent = Math.abs(up.x) < 0.5 ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
            for (float fallback : new float[] {-143.5F, 0, 87.25F}) {
                assertEquals(fallback, GravityUtil.getMovementYaw(up, direction, fallback));
                assertEquals(fallback, GravityUtil.getMovementYaw(up.scale(-1), direction, fallback));
                assertEquals(fallback, GravityUtil.getMovementYaw(Vec3.ZERO, direction, fallback));
                assertEquals(fallback, GravityUtil.getMovementYaw(up.add(tangent.scale(0.00001)), direction, fallback));
                assertTrue(Float.isFinite(GravityUtil.getMovementYaw(up.add(tangent.scale(0.001)), direction, fallback)));
            }
        }
    }

    private static void checkKeyboardDirections(Vec3 worldLook, GravityDirection direction) {
        // Independent geometric expectation: project camera look onto the world gravity plane.
        Vec3 up = direction.getVector().scale(-1);
        Vec3 forward = worldLook.subtract(up.scale(worldLook.dot(up))).normalize();
        Vec3 left = up.cross(forward).normalize();
        float yaw = GravityUtil.getMovementYaw(worldLook, direction, 17);
        assertVectorEquals(forward, keyboardMovement(0, 1, yaw, direction));
        assertVectorEquals(forward.scale(-1), keyboardMovement(0, -1, yaw, direction));
        assertVectorEquals(left, keyboardMovement(1, 0, yaw, direction));
        assertVectorEquals(left.scale(-1), keyboardMovement(-1, 0, yaw, direction));
        for (int strafe : new int[] {-1, 1}) {
            for (int forwards : new int[] {-1, 1}) {
                Vec3 actual = keyboardMovement(strafe, forwards, yaw, direction);
                assertVectorEquals(left.scale(strafe).add(forward.scale(forwards)).normalize(), actual);
                assertEquals(1, actual.length(), EPSILON, "Diagonal input must not increase speed");
                assertEquals(0, actual.dot(up), EPSILON, "Movement must stay on gravity plane");
            }
        }
    }

    private static Vec3 keyboardMovement(double left, double forward, float yaw, GravityDirection direction) {
        return GravityUtil.localToWorld(
            GravityUtil.getInputVector(new Vec3(left, 0, forward), 1.0F, yaw), direction);
    }

    private static Vec3 vanillaLook(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    private static void assertVectorEquals(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPSILON, "world X");
        assertEquals(expected.y, actual.y, EPSILON, "world Y");
        assertEquals(expected.z, actual.z, EPSILON, "world Z");
    }
}
