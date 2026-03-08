# Documentation Guide

This repository uses JavaDoc and a small set of Markdown docs under `docs/`.

The goal is **useful documentation**:
- explain *what a system does*
- explain *why constraints exist*
- document *side effects* (packets/capability sync, cooldown application, server/client expectations)

Not the goal:
- documenting every trivial getter/setter
- repeating obvious parameter names

## Where documentation lives

### 1) Markdown docs (`docs/`)
Use Markdown docs for system-level explanations:
- `docs/architecture.md` – overview of major systems
- `docs/documentation.md` – this file

### 2) JavaDoc (inline, near code)
Use JavaDoc for:
- public APIs used across packages
- networking packets (C2S/S2C)
- capability classes and providers
- complex helpers (combat math, block breaking rules)

## JavaDoc conventions

### Class-level JavaDoc
Include:
- purpose of the class
- main responsibilities
- where it is used from

### Method-level JavaDoc
Use method docs when:
- the method has important preconditions
- the method has non-obvious side effects
- the method has tricky edge cases

Prefer "Contract" style when it clarifies behavior.

#### Suggested template
```java
/**
 * One-line summary.
 *
 * <p>Contract:
 * <ul>
 *   <li>Precondition: ...</li>
 *   <li>Side effect: ...</li>
 *   <li>Error mode: ...</li>
 * </ul>
 */
```

### Networking packet JavaDoc template
For every packet class in `com.wassuples.demonslayer.networking.packet`:

- Direction: C2S or S2C
- Source (keybind/gui/system)
- State changes (capability, world, UI)
- Thread expectation (`context.enqueueWork(...)`)
- Sync behavior (what packet or helper is sent back)

Suggested header:
```java
/**
 * Client-to-server (C2S) packet that ...
 *
 * <p>Typical source: ...
 *
 * <p>Contract:
 * <ul>
 *   <li>Runs on the server thread (executed via {@code context.enqueueWork(...)}).</li>
 *   <li>Validates ...</li>
 *   <li>Updates ...</li>
 *   <li>Optionally syncs via ...</li>
 * </ul>
 */
```

### Capability JavaDoc template
For capability data classes and providers:

- What data is stored
- Server-authoritative vs client-cached
- How it persists (NBT)
- How it syncs (S2C packet / `ModUtils`)
- Any invariants for values

## Linking and symbols

Use:
- `{@link SomeClass}` for types
- `{@code someCode()}` for literal code or method names

Avoid JavaDoc links to symbols that IDEs can’t resolve cleanly (especially nested method refs). If a link causes IDE errors, prefer `{@code ...}`.

## Keeping docs accurate

Documentation must be maintained alongside behavior changes.

When you change:
- resource costs (breath/hunger)
- cooldown rules
- networking validation

Update:
- packet JavaDoc
- `docs/architecture.md` if it changes system behavior

