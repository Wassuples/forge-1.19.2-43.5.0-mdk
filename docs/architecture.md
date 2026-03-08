# Architecture Overview

This document explains the main gameplay systems and how they map to code.

## Quick map (where to look)

- **Forms contract**: `com.wassuples.demonslayer.styles.IForm`
- **Breathing forms & styles**: `com.wassuples.demonslayer.styles.BreathingForms`, `com.wassuples.demonslayer.styles.BreathingStyles`
- **BDA (Blood Demon Arts) forms & styles**: `com.wassuples.demonslayer.styles.DemonForms`, `com.wassuples.demonslayer.styles.BloodDemonArts`
- **Active / hold / charge form runtime**: `com.wassuples.demonslayer.forms.*` (e.g. `ActiveFormManager`)
- **Cooldowns**: `com.wassuples.demonslayer.cooldown.PlayerCooldowns`
- **Networking channel + packets**: `com.wassuples.demonslayer.networking.ModMessages`, `com.wassuples.demonslayer.networking.packet.*`
- **Capability sync helpers**: `com.wassuples.demonslayer.util.ModUtils`
- **Breath capability**: `com.wassuples.demonslayer.breathing.PlayerBreath` (+ `PlayerBreathProvider`)
- **Breathing selection capability**: `com.wassuples.demonslayer.breathing.PlayerStyle` (+ `PlayerStyleProvider`)
- **Demon state capability**: `com.wassuples.demonslayer.demon.PlayerDemon` (+ `PlayerDemonProvider`)

## Forms

A **form** is an ability/action a user can execute.

### Common contract: `IForm`
All forms implement `IForm`, which provides:
- display data (name/description/icon)
- input behavior (`PRESS` vs `HOLD`)
- cost (`getBreathCost()` – historically named; interpreted by the caller)
- cooldown from `getCooldownTicks(user)`
- `performAction(user, level)` – the server-side action

### Breathing vs Demon (BDA)
There are two major form families:

#### Breathing
- Implemented in `BreathingForms` / `BreathingStyles`.
- Generally require holding a Nichirin sword.
- **Resource drain**: uses the **Breath** capability (`PlayerBreath`).

#### Demon / Blood Demon Arts (BDA)
- Implemented in `DemonForms` / `BloodDemonArts`.
- Only applicable for demon players while BDA mode is active.
- **Resource drain**: uses the player’s **hunger bar** (vanilla `FoodData`).

## Resources

### Breath (custom)
- Stored as capability: `PlayerBreath`.
- Server-authoritative.
- Synced to client using `PlayerBreathSyncS2CPacket`.

### Hunger (vanilla)
- Standard Minecraft `FoodData`.
- Used as the cost source for BDA forms.

## Cooldowns

Cooldowns are tracked per player via `PlayerCooldowns`.

Key points:
- Internally stores cooldowns as an **absolute expiry** using `Level#getGameTime()`.
- Saves cooldowns to NBT as **remaining ticks** (stable across save/load).
- Some call sites apply multipliers via `PlayerCooldowns.applyFormsCooldownMultiplier(level, baseTicks)`.

## Networking

### Channel registration
- Packets are registered in `ModMessages.register()`.

### Packet conventions
Packets generally follow this pattern:
- decode on receive
- `context.enqueueWork(...)` to run on the server main thread
- validate (capabilities, cooldown, requirements)
- mutate server state
- optionally sync to client

Important packets:
- `UseFormC2SPacket` – executes the currently selected form
- `AbilityKeyC2SPacket` – raw input events for press/release that drive hold/charge forms
- `SelectFormC2SPacket`, `ChangeFormC2SPacket`, `SwitchStyleC2SPacket` – update selection

## Capability sync

This codebase uses explicit sync packets.

Typical pattern:
- Server changes capability state
- Server calls `ModUtils.syncX(player)` to send a corresponding S2C packet

`ModUtils` provides:
- `syncProgression(player)`
- `syncMissions(player)`
- `syncStyle(player)`
- `syncBreath(player)`
- `syncDemon(player)`
- `syncAll(player)`

## Common flows

### Player uses a form
1. Client input (keybind / GUI)
2. Client sends C2S packet (`UseFormC2SPacket` or `AbilityKeyC2SPacket`)
3. Server validates requirements + cooldown
4. Server consumes resource (breath or hunger)
5. Server executes `performAction`
6. Server applies cooldown
7. Server syncs breath/progression/etc if needed

