package com.wassuples.demonslayer.gravity;

import net.minecraft.world.phys.Vec3;

public enum GravityDirection {
    DOWN(new Vec3(0, -1, 0)),
    UP(new Vec3(0, 1, 0)),
    NORTH(new Vec3(0, 0, -1)),
    SOUTH(new Vec3(0, 0, 1)),
    WEST(new Vec3(-1, 0, 0)),
    EAST(new Vec3(1, 0, 0));

    private final Vec3 vector;

    GravityDirection(Vec3 vector) {
        this.vector = vector;
    }

    public Vec3 getVector() {
        return vector;
    }
}

