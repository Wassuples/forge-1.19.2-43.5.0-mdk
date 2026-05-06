package com.wassuples.demonslayer.infinitycastle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class InfinityCastleManager extends SavedData {

    private static final String DATA_NAME = "demonslayer_infinity_castle";

    // Store schedules as a generic list for basic functionality
    private final List<Schedule> schedules = new ArrayList<>();

    private static class Schedule {
        int sectionId;
        int degrees;
        int ticksRemaining;

        Schedule(int sectionId, int degrees, int ticksRemaining) {
            this.sectionId = sectionId;
            this.degrees = degrees;
            this.ticksRemaining = ticksRemaining;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("sectionId", sectionId);
            tag.putInt("degrees", degrees);
            tag.putInt("ticksRemaining", ticksRemaining);
            return tag;
        }

        static Schedule load(CompoundTag tag) {
            return new Schedule(tag.getInt("sectionId"), tag.getInt("degrees"), tag.getInt("ticksRemaining"));
        }
    }

    public InfinityCastleManager() {
    }

    public static InfinityCastleManager create() {
        return new InfinityCastleManager();
    }

    public static InfinityCastleManager load(CompoundTag nbt) {
        InfinityCastleManager manager = new InfinityCastleManager();
        if (nbt.contains("Schedules", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("Schedules", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                manager.schedules.add(Schedule.load(list.getCompound(i)));
            }
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (Schedule schedule : schedules) {
            list.add(schedule.save());
        }
        nbt.put("Schedules", list);
        return nbt;
    }

    public static InfinityCastleManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(InfinityCastleManager::load, InfinityCastleManager::create, DATA_NAME);
    }

    public void tick(ServerLevel level) {
        boolean dirty = false;
        List<Schedule> toRemove = new ArrayList<>();

        for (Schedule schedule : schedules) {
            schedule.ticksRemaining--;
            if (schedule.ticksRemaining <= 0) {
                rotateSectionNow(level, schedule.sectionId, schedule.degrees);
                toRemove.add(schedule);
                dirty = true;
            } else {
                dirty = true;
            }
        }

        schedules.removeAll(toRemove);

        if (dirty) {
            setDirty();
        }
    }

    public void rotateSection(int sectionId, int degrees) {
        // Schedule a rotation for immediate execution (or after some delay)
        schedules.add(new Schedule(sectionId, degrees, 20)); // delay 1 second
        setDirty();
    }

    private void rotateSectionNow(ServerLevel level, int sectionId, int degrees) {
        // Implementation for a 16x16x16 area based on sectionId
        int chunkX = sectionId % 100;
        int chunkZ = sectionId / 100;

        int startX = chunkX * 16;
        int startY = 64; // arbitrary base height
        int startZ = chunkZ * 16;

        int size = 16;

        // Compute center of rotation
        double centerX = startX + size / 2.0;
        double centerY = startY + size / 2.0;
        double centerZ = startZ + size / 2.0;

        net.minecraft.world.phys.AABB sectionBounds = new net.minecraft.world.phys.AABB(startX, startY, startZ, startX + size, startY + size, startZ + size);

        // 1. Find entities and rotate their gravity/yaw
        List<net.minecraft.world.entity.LivingEntity> entities = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, sectionBounds);
        for(net.minecraft.world.entity.LivingEntity entity : entities) {
            entity.setYRot(entity.getYRot() + degrees);
            entity.setYBodyRot(entity.yBodyRot + degrees);
            entity.setYHeadRot(entity.getYHeadRot() + degrees);

            // Re-map gravity capability if necessary
            entity.getCapability(com.wassuples.demonslayer.gravity.PlayerGravity.GRAVITY_CAPABILITY).ifPresent(cap -> {
                // simple gravity shift for demonstration
                boolean changed = false;
                com.wassuples.demonslayer.gravity.GravityDirection previous = cap.getGravity();
                if (degrees == 90 || degrees == -270) changed = cap.setGravity(com.wassuples.demonslayer.gravity.GravityDirection.EAST);
                else if (degrees == 180 || degrees == -180) changed = cap.setGravity(com.wassuples.demonslayer.gravity.GravityDirection.UP);
                else if (degrees == 270 || degrees == -90) changed = cap.setGravity(com.wassuples.demonslayer.gravity.GravityDirection.WEST);

                if (changed) {
                    com.wassuples.demonslayer.gravity.GravityDirection current = cap.getGravity();
                    com.wassuples.demonslayer.gravity.GravityUtil.reanchorEntityForGravityChange(entity, previous, current);
                    com.wassuples.demonslayer.gravity.GravityUtil.refreshGravityHitbox(entity, current);
                    entity.fallDistance = 0.0F;
                    entity.hasImpulse = true;
                }

                if (changed && entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.wassuples.demonslayer.util.ModUtils.syncGravity(serverPlayer);
                }
            });
        }

        // 2. Rotate blocks mathematically
        net.minecraft.world.level.block.Rotation rotation = net.minecraft.world.level.block.Rotation.NONE;
        if (degrees == 90 || degrees == -270) rotation = net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
        else if (degrees == 180 || degrees == -180) rotation = net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
        else if (degrees == 270 || degrees == -90) rotation = net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;

        if (rotation != net.minecraft.world.level.block.Rotation.NONE) {
            net.minecraft.world.level.block.state.BlockState[][][] oldBlocks = new net.minecraft.world.level.block.state.BlockState[size][size][size];
            net.minecraft.nbt.CompoundTag[][][] blockEntities = new net.minecraft.nbt.CompoundTag[size][size][size];

            // Snapshot old blocks
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    for (int z = 0; z < size; z++) {
                        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(startX + x, startY + y, startZ + z);
                        oldBlocks[x][y][z] = level.getBlockState(pos);
                        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                        if (be != null) {
                            blockEntities[x][y][z] = be.saveWithFullMetadata();
                        }
                        // Clear to void to prevent collision during placement
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                }
            }

            // Place rotated blocks
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    for (int z = 0; z < size; z++) {
                        net.minecraft.world.level.block.state.BlockState oldState = oldBlocks[x][y][z];
                        if (oldState == null || oldState.isAir()) continue;

                        // Default 2D rotation for structures using StructureTemplate logic:
                        int dx = x - (size / 2);
                        int dz = z - (size / 2);
                        int rx;
                        int rz;

                        if (rotation == net.minecraft.world.level.block.Rotation.CLOCKWISE_90) {
                            rx = -dz; rz = dx;
                        } else if (rotation == net.minecraft.world.level.block.Rotation.CLOCKWISE_180) {
                            rx = -dx; rz = -dz;
                        } else {
                            rx = dz; rz = -dx;
                        }

                        int newX = startX + (size / 2) + rx;
                        int newY = startY + y;
                        int newZ = startZ + (size / 2) + rz;

                        net.minecraft.core.BlockPos newPos = new net.minecraft.core.BlockPos(newX, newY, newZ);
                        net.minecraft.world.level.block.state.BlockState rotatedState = oldState.rotate(level, newPos, rotation);

                        level.setBlock(newPos, rotatedState, 3);
                        net.minecraft.nbt.CompoundTag beTag = blockEntities[x][y][z];
                        if (beTag != null) {
                            net.minecraft.world.level.block.entity.BlockEntity be = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(newPos, rotatedState, beTag);
                            if (be != null) level.setBlockEntity(be);
                        }
                    }
                }
            }
        }
    }
}
