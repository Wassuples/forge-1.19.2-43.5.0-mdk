# Infinity Castle System Design

## Overview

The Infinity Castle is a pocket dimension consisting of lamplit wooden rooms, long halls, and moving corridors. It acts as a surreal, ever-shifting environment serving as a primary stronghold for high-ranking demons.

A central feature of the Infinity Castle is its warped direction of gravity. Within this dimension, the standard rules of physics do not apply—entities can stand upright, upside down, perpendicularly on the walls, or move in physically impossible directions depending on their location or specific interactions within the space.

## Game Mechanics

1. **Procedural Shifting Labyrinth**: The castle is not a static world. Corridors can shift, doors lead to entirely different wings when reopened, and the layout feels fundamentally non-Euclidean.
2. **Warped Gravity Zones**: Various sections of the castle have localized gravitational pulls. Walking into a new room might instantly transition a player's gravity to the wall or ceiling.
3. **Biwa Teleportation**: High-ranking demons (specifically the Biwa Demon) can strike a biwa (a traditional Japanese lute) to instantly shift the architecture, alter gravity, or teleport entities traversing the castle.

## Technical Implementation

Implementing this system in Minecraft 1.19.2 (Forge) requires a combination of custom world generation, entity capabilities, and core rendering mixins.

### 1. Dimension and World Generation
- **Custom Dimension**: Registered via a standard datapack or code-based `DimensionAPI` as a single biome, void-like dimension.
- **Generation Strategy**: Instead of standard terrain generation, the dimension uses a massive, infinite 3D jigsaw structure. The `ChunkGenerator` will place interconnected "room" templates (lamplit wooden styles, shoji screens, tatami mats) across the chunk grid.
- **Shifting Corridors**: Certain doorway blocks will act as seamless teleporters. When traversed, they silently teleport the entity to a distant segment of the dimension to give the illusion of an infinite, non-Euclidean structure.
- **Section Metadata**: The Infinity Castle is not Infinite, just very large. As such, it is divided into sections (e.g., 16x16x16 block volumes, however the exact size is for you to decide) that have associated metadata defining their gravity direction and any special properties (e.g., "This section has a portal to the Nether," "Anyone in this section gets teleported out if they stay too long."").

### 2. Warped Gravity System
Altering gravity directions in Minecraft requires deep changes to vanilla physics and rendering logic.

- **Gravity Capability (`PlayerGravity`)**: A Forge capability attached to `LivingEntity` that stores the current gravity vector (e.g., `UP`, `DOWN`, `NORTH`, `SOUTH`, `EAST`, `WEST`).
- **Physics Mixins**:
  - Mixins targeting `LivingEntity.travel()` and physics calculations to redirect standard downward gravity (-Y) towards the vector defined by the capability.
  - Collision box adjustments to allow entities to properly stand on walls or ceilings without falling through corners.
- **Client Render & Camera Mixins**:
  - `Camera` mixin: Rotates the player's camera pitch/yaw and applies an absolute roll rotation corresponding to their gravity vector.
  - `EntityRenderer` mixin: Rotates entity models so they appear correctly oriented (e.g., sticking out of the wall) to other players.
- **Gravity Zones**: Invisible custom blocks or bounding box zones (`AABB`) within the structure NBT. When an entity enters an AABB, their `PlayerGravity` capability is updated, and the transition is smoothly interpolated on the client to avoid motion sickness.

### 3. The Biwa Command & Scheduling System
The "Biwa Modulator" acts as a central control interface or command system accessible to the Biwa Demon (or operators). Striking the biwa issues commands that manipulate the castle's architecture, route entities, or create portals.

- **Scheduled & Immediate Operations**: The Biwa user can define rules that the Castle manager executes either instantly or on a repeating timer.
  - *Examples*: "Every 5 seconds, rotate Section 128 by 90 degrees," or "Every 1 minute, rotate the entire castle 180 degrees on the X-axis."
- **Entity Management**: Commands can precisely target players or NPCs within the dimension.
  - *Example*: "Teleport Entity [X] to Section 90."
- **Moving Architecture**: The system can rotate or shift entire sections of the castle, which involves mathematically translating blocks and entities within a defined area. This should animate seamlessly to maintain immersion.
  - *Example*: "Rotate Section 45 by 90 degrees clockwise." "Translate Section 12 by [X, Y, Z] blocks."
- **Dynamic Portaling (Entrances & Exits)**: The system can dynamically generate portal doors linking the Infinity Castle to other dimensions (Overworld, Nether, etc.).
  - *Example*: "Create exit doors in sections 10, 15, and 102."
  - *Example*: "Manifest an entrance door at coordinates [X, Y, Z] in the Overworld."
- **Technical Backend (`InfinityCastleManager`)**: Uses a centralized `SavedData` class hooked into `TickEvent.LevelTickEvent` to track all active schedules, custom portal doors, and section metadata. When a scheduled rotation occurs, blocks within the targeted section boundaries are mathematically rotated, and entities within the selection have their relative offsets and gravity vectors updated.

## Data Structures (Draft)

```java
public enum GravityDirection {
    DOWN(new Vec3(0, -1, 0)),
    UP(new Vec3(0, 1, 0)),
    NORTH(new Vec3(0, 0, -1)),
    SOUTH(new Vec3(0, 0, 1)),
    WEST(new Vec3(-1, 0, 0)),
    EAST(new Vec3(1, 0, 0));

    // ... vector math
}

// Capability interface
public interface IGravityState {
    GravityDirection getGravity();
    void setGravity(GravityDirection direction);
    void tickInterpolation(); // for smooth camera transitions
}
```

## Summary / Challenges
The primary challenge in this system is the **Warped Gravity**. Modifying core Minecraft movement and collision to support 6 degrees of gravity is notoriously difficult, particularly keeping compatibility with external mods. It is highly recommended to study open-source 1.19.2 gravity manipulation mods or simplify the gravity mechanic to purely visual teleportation rotations if network/physics syncing becomes a bottleneck.
