# AGENTS.md — Demon Slayer Minecraft Forge Mod

## Project Overview

This is a **Minecraft Forge 1.19.2 mod** (`demonslayer`) implementing a Demon Slayer-themed experience with:
- **Nemesis System**: A persistent, evolving demon hierarchy (Twelve Kizuki) with named NPCs, personalities, memories, and relationships
- **Combat Forms**: Enum-based ability system for both slayer breathing techniques and demon Blood Demon Arts
- **Player Capabilities**: Forge capabilities for breath, style, demon state, progression, cooldowns, and crow companion
- **Missions**: Data-driven quest system loaded from JSON

**Authors**: Wassuples, Inverted | **Version**: 0.0.9 | **Java**: 17

## Architecture

### Entry Point & Registration
- **Main class**: `src/main/java/com/wassuples/demonslayer/DemonSlayer.java` — `@Mod` annotated, bootstraps all registries
- All registries use Forge's `DeferredRegister` pattern (items, blocks, entities, effects, biomes, recipes, menus)
- Entity renderers registered in `ClientModEvents` inner class via `@Mod.EventBusSubscriber`

### Core Systems Map
```
DemonSlayer (bootstrap)
├── ModEvents (Forge event bus — capabilities, combat, player lifecycle)
├── NemesisSystemEvents (demon hierarchy lifecycle)
├── ActiveFormManager (server-side form ticking)
├── DemonHierarchyManager (SavedData — world-persistent demon roster)
└── ModMessages (networking channel)
```

### Player State (Forge Capabilities)
All player data uses the capability pattern. Providers in each package:
- `breathing/` — `PlayerBreath`, `PlayerStyle` (slayer breathing energy & current style)
- `demon/` — `PlayerDemon` (demon transformation state, blood points)
- `ranking/` — `PlayerProgression` (slayer rank progression)
- `cooldown/` — `PlayerCooldowns` (ability cooldown tracking)
- `crow/` — `PlayerCrow` (crow companion state)
- `missions/` — `PlayerMissions` (active mission tracking)

**Critical**: Capabilities are attached in `ModEvents.onAttachCapabilities()`. Data persistence across death uses map-based workarounds due to PlayerAnimator conflicts — see the comment block at the top of `ModEvents.java`.

### Combat Forms System
Forms are **enum constants** implementing `IForm` interface:
- `styles/BreathingForms.java` — slayer breathing techniques
- `styles/DemonForms.java` — demon Blood Demon Arts
- `styles/BloodDemonArts.java` — BDA type enum (CRYO, HAEMO, CRESCENT, etc.)

Execution flow: Client keybind → `UseFormC2SPacket` → `ActiveFormManager` → `IForm.performAction()`

Active forms tick server-side via `ActiveFormManager` (subscribed to `TickEvent.ServerTickEvent`).

### Nemesis System (Complex — see `docs/NEMESIS_SYSTEM.md`)
- **Core**: `nemesis/DemonHierarchyManager.java` (1900+ lines, extends `SavedData`)
- **Blueprints**: JSON files in `run/config/demonslayer/blueprints/` defining canonical Kizuki demons
- **Traits**: `nemesis/trait/DemonTrait.java` — personality, memories, emotions, relationships
- **AI**: `nemesis/ai/NemesisDemonEntity.java` — spawned nemesis demon entity
- **Battles**: `nemesis/battle/BloodBattle.java` — player vs demon combat sessions
- **Operator commands**: `command/DemonSlayerCommands.java` contains `/demonslayer nemesis social ...` QoL controls (packs, alliances, compatibility previews, mobilization, maintenance). Full command reference: `docs/NEMESIS_SYSTEM.md` -> `Player Interaction — Commands & Debug`.

### Networking
- Single `SimpleChannel` in `networking/ModMessages.java`
- Packets: `UseFormC2SPacket`, `ChangeFormC2SPacket`, various sync packets (S2C)
- Registration happens in `commonSetup` phase

### Mixins
- Config: `src/main/resources/demonslayer.mixins.json`
- Single mixin: `mixin/FireBlockAccessor.java` (accessor for fire block)

## Build & Development Commands

```bash
# Build the mod
./gradlew build

# Run client (with IDE integration)
./gradlew genIntellijRuns  # First time only
# Then use IDE run configs: runClient, runServer, runData, runGameTestServer

# Format code (required before commits)
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Refresh dependencies (if libraries missing)
./gradlew --refresh-dependencies

# Clean build artifacts
./gradlew clean
```

## Key Dependencies
- **GeckoLib 3.1.19** — Animated entity models (all custom entities use GeckoLib renderers)
- **PlayerAnimator 0.2.0+** — Player animation overrides (causes capability persistence quirks)
- **TerraBlender 2.0.1+** — Custom biome generation (Wisteria Forest)

## Project Conventions

### Package Structure
```
com.wassuples.demonslayer/
├── block/          — Custom blocks + block entities
├── breathing/      — Slayer breathing capability
├── client/         — GUI screens, overlays, keybindings
├── command/        — Chat commands
├── cooldown/       — Cooldown capability
├── crow/           — Crow companion system
├── demon/          — Demon player capability + AI generation
├── effects/        — Mob effects
├── entity/         — Entity types, custom entities, renderers
├── events/         — Forge event handlers
├── forms/          — Active form implementations (slayer/, demon/)
├── gamerule/       — Custom game rules
├── glow/           — Glow effect system
├── item/           — Custom items
├── missions/       — Mission system
├── mixin/          — Mixin classes
├── nemesis/        — Nemesis hierarchy system
├── networking/     — Network packets
├── ranking/        — Player rank progression
├── recipe/         — Custom recipes
├── styles/         — Form enums + interfaces
├── util/           — Helpers (CombatHelper, DemonThreatHelper, etc.)
└── worldgen/       — Biomes, TerraBlender regions, inventories
```

### Entity Registration Pattern
All entities in `entity/ModEntityTypes.java` using `DeferredRegister<EntityType<?>>`. Renderers registered in `DemonSlayer.ClientModEvents.registerEntityRenderers()`.

### Data-Driven Content
- **Missions**: JSON in `src/main/resources/data/demonslayer/missions/`, loaded via `MissionReloadListener`
- **Nemesis Blueprints**: JSON in `run/config/demonslayer/blueprints/`, loaded by `BlueprintLoader`
- **Generated resources**: `src/generated/resources/` (run `./gradlew runData` to regenerate)

### Documentation
Design docs in `docs/`:
- `NEMESIS_SYSTEM.md` — Complete technical reference for the nemesis hierarchy (combined v1+v2)
- `MISSIONS_SYSTEM.md` — Full mission system design with 12 arcs
- `REPUTATION_SYSTEM.md` — Four-dimensional reputation system design

## Common Pitfalls

1. **Capability data loss on death**: PlayerAnimator interferes with capability cloning. `ModEvents` saves capability data to maps on death and restores on respawn. Don't modify `onPlayerDeath`/`onPlayerRespawn` without understanding this workaround.

2. **Form execution is server-side only**: `IForm.performAction()` runs on logical server. Client-side effects must be synced via packets.

3. **Nemesis system is world-saved**: `DemonHierarchyManager` extends `SavedData` — changes persist across server restarts. Use `get(ServerLevel)` to access the singleton.

4. **Blueprint loading**: First-time defaults are seeded from bundled resources in `src/main/resources/`. Runtime configs in `run/config/demonslayer/blueprints/` override defaults.

5. **Entity spawn registration**: Spawn placements registered in `commonSetup` via `event.enqueueWork()` — must be thread-safe.
