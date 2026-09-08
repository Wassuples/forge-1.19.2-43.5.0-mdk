# AGENTS.md — Demon Slayer Minecraft Forge Mod

## Agent Workflow

For implementation, debugging, and review tasks, read [docs/AGENT_WORKFLOW.md](docs/AGENT_WORKFLOW.md) before editing. Use its validation matrix for the systems touched.

- Start with `git status --short` and inspect relevant existing diffs. Preserve user changes, IDE settings, logs, and saved worlds; stage only task-owned files when a commit is requested.
- Define observable acceptance criteria, then trace the affected code before implementing. For multi-step work, use [docs/tasks/TEMPLATE.md](docs/tasks/TEMPLATE.md) to record scope, decisions, validation, and the next step. Small fixes can keep this in the task conversation.
- Continue through implementation, focused validation, and review within the authorized scope. Ask only for missing decisions that materially block correctness or actions requiring authorization.
- Use multiple agents by default for implementation and debugging: a lead, scope reviewer, bounded implementers, independent validator, and code reviewers. Follow the workflow's phase gates and two review rounds; this instruction explicitly requests delegation when tools support it. Review-only requests remain read-only. If delegation is unavailable, record the limitation and follow the documented handoff fallback; do not claim independent reviews.
- Keep Java 17 / Forge 1.19.2 compatibility. Verify current code and build configuration when older architecture notes disagree.
- Report checks as passed, failed, or not run with evidence. Compilation does not establish in-game correctness.

## Code Review Rules

- Flag client-only class references reachable from dedicated-server code and gameplay mutations performed only on the client.
- Check C2S sender/state validation, server-thread handling, costs, cooldowns, and synchronization; clients must not authorize their own combat outcomes.
- For capabilities and SavedData, check death/respawn, save/load compatibility, dirty marking, and reconnect behavior. Preserve the PlayerAnimator workaround unless the task explicitly addresses it.
- For mixins, check configured class existence, target signatures, logical/physical side, and runtime application. A successful Java compile is insufficient.
- Review only the task's changes as introduced defects; distinguish pre-existing problems. Give a concrete trigger, impact, and file location for each actionable finding.

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
├── ClientEvents (client input/HUD/render hooks + glow capability attachment)
└── ModMessages (networking channel)
```

### Player State (Forge Capabilities)
Player and combat-target state use the capability pattern. Providers in each package:
- `breathing/` — `PlayerBreath`, `PlayerStyle` (slayer breathing energy & current style)
- `demon/` — `PlayerDemon` (demon transformation state, blood points)
- `ranking/` — `PlayerProgression` (slayer rank progression)
- `cooldown/` — `PlayerCooldowns` (ability cooldown tracking)
- `crow/` — `PlayerCrow` (crow companion state)
- `missions/` — `PlayerMissions` (active mission tracking)
- `glow/` — `GlowCapability` (entity highlight state, attached to `LivingEntity` in `ClientEvents.ForgeEventHandlers`)

**Critical**: Player capabilities are attached in `ModEvents.onAttachCapabilities()`. Data persistence across death uses map-based workarounds due to PlayerAnimator conflicts — see the comment block at the top of `ModEvents.java`.

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
- **Battles**: `nemesis/BloodBattle.java` (active duel state), `nemesis/battle/` (session tracking/outcomes)
- **Operator commands**: `command/DemonSlayerCommands.java` contains `/demonslayer nemesis social ...` QoL controls (packs, alliances, compatibility previews, mobilization, maintenance). Full command reference: `docs/NEMESIS_SYSTEM.md` -> `Player Interaction — Commands & Debug`.

### Networking
- Single `SimpleChannel` in `networking/ModMessages.java`
- Packets: `UseFormC2SPacket`, `ChangeFormC2SPacket`, various sync packets (S2C)
- Registration happens in `commonSetup` phase

### Mixins
- Config: `src/main/resources/demonslayer.mixins.json`
- Includes `mixin/FireBlockAccessor.java` and gravity mixins. Read the current config and implementation; the list changes during movement work.

## Build & Development Commands

```bash
# Build the mod
./gradlew build

# Run unit tests
./gradlew test

# Run client (with IDE integration)
./gradlew genIntellijRuns  # First time only
# Then use IDE run configs: runClient, runServer, runData, runGameTestServer

# Check whitespace; match surrounding Java formatting
git diff --check

# Refresh dependencies (if libraries missing)
./gradlew --refresh-dependencies

# Clean build artifacts
./gradlew clean
```

On Windows PowerShell, use `.\gradlew.bat` in place of `./gradlew`.
Spotless is not configured in the current `build.gradle`; do not claim `spotlessApply` or `spotlessCheck` passed. If formatting automation is added later, update these commands. Avoid repository-wide formatting during unrelated tasks.

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
- **Nemesis Blueprints**: JSON in `run/config/demonslayer/blueprints/`, loaded by `BlueprintLoader` (defaults are seeded from bundled `data/demonslayer/nemesis/blueprints` resources)
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

4. **Blueprint loading**: First-time defaults are seeded from bundled `data/demonslayer/nemesis/blueprints` resources (packaged via `build.gradle` `processResources` from `run/config/demonslayer/blueprints/`). Runtime configs in `run/config/demonslayer/blueprints/` override defaults.

5. **Entity spawn registration**: Spawn placements registered in `commonSetup` via `event.enqueueWork()` — must be thread-safe.

6. **Glow capability attachment location**: `GlowCapability` is attached to `LivingEntity` in `events/ClientEvents.java` (`ForgeEventHandlers.onAttachCapabilities`), not in `ModEvents.onAttachCapabilities()` with player capabilities.
