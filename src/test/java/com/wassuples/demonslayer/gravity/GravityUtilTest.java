package com.wassuples.demonslayer.gravity;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GravityUtilTest {
    private static final double EPSILON = 1.0E-12;

    @Test
    void localWorldTransformsRoundTripForEveryDirection() {
        Vec3 local = new Vec3(1.25D, -2.5D, 3.75D);
        for (GravityDirection direction : GravityDirection.values()) {
            assertVectorEquals(local,
                GravityUtil.worldToLocal(GravityUtil.localToWorld(local, direction), direction));
        }
    }

    @Test
    void localDownMapsToConfiguredWorldDownForEveryDirection() {
        for (GravityDirection direction : GravityDirection.values()) {
            assertVectorEquals(direction.getVector(),
                GravityUtil.localToWorld(new Vec3(0.0D, -1.0D, 0.0D), direction));
        }
    }

    @Test
    void transformPreservesLengthAndOrthogonalAxes() {
        Vec3[] axes = {new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)};
        for (GravityDirection direction : GravityDirection.values()) {
            Vec3 x = GravityUtil.localToWorld(axes[0], direction);
            Vec3 y = GravityUtil.localToWorld(axes[1], direction);
            Vec3 z = GravityUtil.localToWorld(axes[2], direction);
            assertEquals(1.0D, x.lengthSqr(), EPSILON);
            assertEquals(1.0D, y.lengthSqr(), EPSILON);
            assertEquals(1.0D, z.lengthSqr(), EPSILON);
            assertEquals(0.0D, x.dot(y), EPSILON);
            assertEquals(0.0D, x.dot(z), EPSILON);
            assertEquals(0.0D, y.dot(z), EPSILON);
        }
    }

    private static void assertVectorEquals(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }
}
