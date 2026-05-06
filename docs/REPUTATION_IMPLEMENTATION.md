# Reputation System Implementation Notes

This document tracks what is wired, what is partially wired, and what is intentionally deferred because the dependent gameplay systems are not finished yet.

## Current Integration Status

- **Core data model (`SavedData`)**: complete
- **Witness queue + daily processing**: complete
- **Command surface (`/demonslayer reputation ...`)**: complete
- **Combat event wiring (villager/hashira/demon kills)**: complete for currently implemented entities
- **Mission lifecycle wiring**: partial (default mission complete/abandon + currently available mission IDs)
- **Nemesis integration**: partial (blood battle conclusion + battle outcome drain + hunter spawn notoriety modifier)
- **Crow + Tamayo integration**: complete for existing crow death and Tamayo offer flows
- **Persuasion / witness dialogue gameplay loops**: not yet wired (dependent systems missing)

## Wired Coverage Matrix

### Core persistence and routing

- `src/main/java/com/wassuples/demonslayer/reputation/ReputationManager.java`
- `src/main/java/com/wassuples/demonslayer/reputation/PlayerRepRecord.java`
- `src/main/java/com/wassuples/demonslayer/reputation/RepDeltaCalculator.java`
- `src/main/java/com/wassuples/demonslayer/reputation/ReputationService.java`

### Forge event wiring

- `src/main/java/com/wassuples/demonslayer/reputation/ReputationEvents.java`
  - kill-based witness scanning
  - daily queue processing
- `src/main/java/com/wassuples/demonslayer/events/ModEvents.java`
  - mission-completion hook calls `ReputationService.onMissionCompleted(...)`

### Mission networking hooks

- `src/main/java/com/wassuples/demonslayer/networking/packet/AbandonMissionC2SPacket.java`
  - abandonment now calls `ReputationService.onMissionAbandoned(...)`

### Demon-path hook

- `src/main/java/com/wassuples/demonslayer/networking/packet/TamayoOfferResponseC2SPacket.java`
  - accepted transformation calls `ReputationService.onDemonPathAccepted(...)`

### Crow hook

- `src/main/java/com/wassuples/demonslayer/entity/custom/CrowEntity.java`
  - crow death now calls `ReputationService.onCrowDeath(...)`

### Nemesis hooks

- `src/main/java/com/wassuples/demonslayer/nemesis/NemesisSystemEvents.java`
  - blood battle conclusion calls `ReputationService.onBloodBattleConclusion(...)`
  - server tick drains battle outcomes and calls `ReputationService.onBattleOutcome(...)`
- `src/main/java/com/wassuples/demonslayer/nemesis/battle/BattleSessionTracker.java`
  - added `drainFinalizedOutcomes()` to expose finalized outcomes for external systems
- `src/main/java/com/wassuples/demonslayer/nemesis/DemonHierarchyManager.java`
  - hunter spawn formula now includes demon notoriety relationship bonus

## Pending / Deferred Integrations (Dependent Systems Incomplete)

These are intentionally not fully wired yet because the required systems/content do not exist yet in this workspace:

1. **Full mission arc mapping from `docs/REPUTATION_SYSTEM.md`**
   - The design references many mission IDs/branches that are not present in `src/main/resources/data/demonslayer/missions/` yet.
   - Current mission datapack set is minimal, so only baseline mission complete/abandon events are fully wired.

2. **Tanjiro crew relationship-driven events**
   - No concrete runtime entities/systems for Tanjiro/Nezuko/Zenitsu/Inosuke/Kanao/Genya branch detection are currently present.
   - Events such as `COMPLETE_MISSION_WITH_TANJIRO`, `PROTECT_NEZUKO_SECRETLY`, etc. are represented in enums and delta tables but not auto-triggered yet.

3. **Persuasion/concealment dialogue loop**
   - The design requires per-witness cooldown state and dialogue actions (bribe/intimidate/persuade).
   - Foundational helper classes exist, but no active dialogue gameplay pipeline currently invokes them.

4. **Hardline gated multi-step confirmations**
   - Full confirmation-flow UX for hardline actions (e.g., downed-state second confirmation) depends on combat/story systems that are not complete.

5. **Muzan omniscience channel for demon-path players**
   - Core trigger exists on demon-path acceptance.
   - Full always-on "all future actions auto-report to Muzan" routing across every action source is not fully implemented yet.

## Command Surface

- `/demonslayer reputation view <player>`
- `/demonslayer reputation set <player> <dim> <value>`
- `/demonslayer reputation add <player> <dim> <value>`
- `/demonslayer reputation reset <player>`
- `/demonslayer reputation hardline set <player> <flag>`
- `/demonslayer reputation hardline clear <player> <flag>`
- `/demonslayer reputation sinlog <player>`
- `/demonslayer reputation witness simulate <player> <eventType> <witnessType>`
- `/demonslayer reputation suppress clear <player>`
- `/demonslayer reputation investigate trigger <player>`

## Verification

Unit tests:

- `src/test/java/com/wassuples/demonslayer/reputation/RepDeltaCalculatorTest.java`

Tried command:

```powershell
Set-Location "D:\Programming\forge-1.19.2-43.5.0-mdk"
.\gradlew.bat test --no-daemon
```

So compile/test validation in this environment is currently blocked by JVM/runtime configuration rather than source compile errors in the edited files.
