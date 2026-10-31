# Toon Force BDA implementation record

## Scope and acceptance

**Requested behavior:** finish the seven-form Toon Force BDA (Eye Pop, Acme Anvil, Pie Splat, Banana Peel, Shadow Toss, Fake Tunnel, and Pocket outcomes), the edge-walk passive, hit-triggered dizzy stars, audiovisual feedback, and server-authoritative combat behavior.

**Observable acceptance criteria:**

1. Every Toon Force form starts through the existing server-side `ActiveFormManager` flow and has a distinct player/victim/projectile animation, particles, and audio cue.
2. Eye Pop ricochets for a prolonged period, then homes toward an eligible non-owner living target and applies damage/dizzy feedback.
3. Acme Anvil telegraphs its target, whistles, falls, damages, flattens, and slows victims.
4. Pie Splat only resolves after a successful parry and sends the screen decal packet to an attacking player.
5. Banana Peels remain small, coexist as separate trap entities, can catch their owner after a grace period, drop a held item, spin victims, and preserve sprint momentum in the launch.
6. Shadow Toss recasts against the owner's existing anchor and teleports only to a collision-safe location.
7. Fake Tunnel is placed on the aimed surface, ignores its painter, rebounds other moving living entities, and expires.
8. Pocket visibly rolls mallet/TNT/cookie/fish outcomes; TNT can transfer without immediate ping-pong, cookie has varied fortunes and a very rare jackpot, and fish records nemesis humiliation/hate.
9. Toon Force edge-walking arrests a fall until the user looks down; damaging Toon Force hits can apply dizzy stars.
10. Referenced Toon Force textures are loadable PNG resources rather than text placeholders.

## Trace and decisions

- Existing implementation at base revision `890ac58` already contained the seven form classes, entity registrations/renderers, animation packets, custom sound registrations/assets, pie overlay, passive event subscriber, and most gameplay behavior.
- Implementation revision `5d68042` closes the remaining observable gaps rather than replacing working server-authoritative systems.
- Eye Pop received a 30-second/128-bounce window and server-side target steering. Fake Tunnel now retains its owner, rotates to the cast direction, uses ray-hit placement, ignores its painter, remains available to collide with multiple victims, and lasts 15 seconds. Hot Potato now preserves attribution through transfers and adds an eight-tick transfer lock.
- Four text placeholder resources were replaced with deterministic 64x64 RGBA cartoon textures. The existing valid anvil texture and OGG sound assets were preserved.

## Delegation and identities

The requested collaboration roles could not be instantiated: this environment exposed no agent creation/list/wait tools, only a follow-up tool that requires an already-existing target identity. No target identities were supplied. The repository also does not contain the referenced `docs/AGENT_WORKFLOW.md`, `docs/tasks/TEMPLATE.md`, or `docs/agents/ROLES.md` files at base revision `890ac58`. Therefore, no independent-agent work or review is claimed.

| Role | Real identity | Revision | Result |
| --- | --- | --- | --- |
| Lead/implementation fallback | `/root` (primary agent) | `5d68042` | Implemented the bounded fixes and resources above. |
| Scope reviewer | **Pending — no agent identity available** | `890ac58..5d68042` | Acceptance revisit pending. |
| Separate implementer | **Pending — no agent identity available** | `890ac58..5d68042` | Independent implementation audit pending. |
| Independent validator | **Pending — no agent identity available** | `5d68042` | Runtime/gameplay validation pending. |
| Non-author review round 1 | **Pending — no agent identity available** | `5d68042` | Security/side/thread review pending. |
| Non-author review round 2 | **Pending — no agent identity available** | `5d68042` | Behavior/resource review pending. |

## Prepared handoffs

Because `docs/agents/ROLES.md` is absent, these handoffs follow the roles named by the repository `AGENTS.md` without pretending that the lead performed independent reviews.

### Scope reviewer handoff

At `5d68042`, trace each acceptance criterion above to code/resources. Specifically challenge whether “endlessly” is acceptably bounded for entity hygiene, whether the fake-tunnel collision volume aligns with its model, and whether all seven form descriptions match observed behavior. Return only omissions introduced or left unresolved by this task, with file/line and trigger.

### Separate implementer handoff

At `5d68042`, reproduce and repair any accepted scope finding. Preserve logical-server ownership of damage, teleportation, inventory mutation, capability changes, and entity spawning. Do not touch player capability death/respawn handling.

### Independent validator handoff

Run Java 17 `test` and `build`, then launch a dedicated server and client. Exercise every form against players and mobs, TNT transfer chains, owner/non-owner tunnel passage, stacked peels, sprint/non-sprint slips, safe/unsafe shadow anchors, edge walking, and fish against a nemesis. Capture logs and describe actual audiovisual observations; compilation alone does not pass gameplay acceptance.

### Non-author review round 1 handoff

Review `890ac58..5d68042` for dedicated-server client-class leakage, gameplay mutation side, damage attribution, C2S authorization assumptions, entity lifetime/unbounded work, transfer ownership/cooldown correctness, and safe teleport collision checks. Report concrete triggers and impact.

### Non-author review round 2 handoff

After round-1 repairs, review the new revision for regressions in GeckoLib resources, texture validity/UV readability, particle/audio pacing, repeated tunnel collisions, target selection, persistence tags, and the complete request. Rerun affected checks, then return the revision to the scope reviewer.

## Validation evidence

| Check | Result | Evidence |
| --- | --- | --- |
| `git diff --check` | Passed | No whitespace errors before implementation commit. |
| PNG decode/verify | Passed | Pillow decoded all five Toon Force entity textures as valid 64x64 PNG files. |
| `JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 PATH=/root/.local/share/mise/installs/java/17.0.2/bin:$PATH bash gradlew test --no-daemon` | Blocked by pre-existing source failure | Forge compilation reached `compileJava`, then failed because four gravity mixins import missing `com.wassuples.demonslayer.gravity.GravityDebug`; no Toon Force compiler diagnostic was emitted. |
| In-game client/dedicated-server validation | Not run | Requires interactive game clients; prepared for the independent validator above. |

## Findings and repairs

- **Repaired:** Fake Tunnel affected its owner and vanished after the first collision. Owner identity is now retained and exempt; the decal persists until timeout.
- **Repaired:** Fake Tunnel ignored aimed wall placement and facing. Casting now ray-traces up to six blocks and orients the entity from the user's view vector.
- **Repaired:** Eye Pop expired after eight seconds/ten bounces without satisfying “hits someone eventually.” Its bounded lifetime was extended and it steers toward a nearby eligible target.
- **Repaired:** Hot Potato transfer could retain stale owner data on the prior carrier and bounce immediately between overlapping entities. Transfer now snapshots attribution, clears the old carrier, and locks the recipient briefly.
- **Repaired:** Four renderer texture paths contained plain-text placeholders. They now contain valid cartoon PNG art.
- **Pending independent finding disposition:** none available because delegation tools/identities were unavailable.

## Next step

Run the prepared independent validation and two non-author review handoffs when agent delegation is available, repair findings, rerun affected checks, and have the scope reviewer revisit all acceptance criteria before merge. Keep the pull request unmerged.

## Cartoon presentation revision (`215ec7a`)

### User findings addressed

- **Pocket roulette was world-space vanilla text above the caster.** Replaced the primary presentation with a first-person HUD slot machine: a bordered, wobbling three-reel panel renders large mallet/TNT/cookie/fish item icons, spins in view for the caster, reveals the result, and is also sent briefly to player victims of mallet/TNT/fish outcomes. World text remains secondary punctuation only.
- **Shadow Toss looked like particles.** Replaced the GeckoLib slab renderer with a full-bright, camera-facing ink-and-purple blanket renderer that visibly wobbles in flight and expands when anchored. The rip now begins with a dense ground-shadow burst and a low cartoon boing.
- **Fake Tunnel did not implement caster-only passage.** Casting at a wall now stores its inward surface direction. When its owner runs into the painted opening, server code searches through up to four blocks for a collision-safe exit and teleports the owner through; non-owners reverse momentum, play the slam animation/audio, and produce BONK feedback. The renderer is now a popping, paper-thin set of concentric painted ellipses rather than a generic textured cuboid.
- **Acme Anvil resembled a vanilla anvil.** Replaced the GeckoLib model renderer with a deliberately flat, full-bright, thick-ink ACME prop assembled from 2D planes, including a yellow ACME label and squash/stretch wobble. Its telegraph and impact use the bundled Toon Force whistle/crash sounds, and flattening now matches the requested two seconds.
- **Bundled Toon audio was not consistently used.** Eye Pop, ricochets, Shadow Toss, anvil warning, and anvil impact now call registered Toon Force sound events instead of vanilla stand-ins.

### Review and validation revision

| Role | Real identity | Revision | Evidence/findings |
| --- | --- | --- | --- |
| Lead repair | `/root` (primary agent) | `215ec7a` | Implemented the presentation and tunnel corrections above. |
| Scope reviewer | **Pending — delegation unavailable** | `43cf5ae..215ec7a` | Handoff remains the acceptance list plus the four user-reported failures above. |
| Independent validator | **Pending — delegation unavailable** | `215ec7a` | Must verify first-person visibility, third-person readability, wall thicknesses 1–4, and victim HUD behavior in game. |
| Non-author review round 1 | **Pending — delegation unavailable** | `215ec7a` | Must audit S2C-only client references and server-authoritative tunnel teleport/collision logic. |
| Non-author review round 2 | **Pending — delegation unavailable** | `215ec7a` | Must review cartoon presentation, pacing, renderer transforms, and all repaired findings after round 1. |
| Scope acceptance revisit | **Pending — delegation unavailable** | `215ec7a` | Do not merge until the independent checks above are complete. |

No independent identity is claimed. The environment still provides no way to create or enumerate review agents, and the referenced workflow/role documents remain absent.

### Evidence

- `git diff --check` passed before commit `215ec7a`.
- A normal Java 17 `compileJava` remains blocked by the pre-existing missing `GravityDebug` class.
- To isolate this revision, a temporary, uncommitted no-op `GravityDebug` shim with the exact referenced `log(String, String, Object...)` signature was added, `compileJava` completed successfully, and the shim was immediately removed. This demonstrates that the committed Toon Force sources compile without adding unrelated gravity code; the normal repository build limitation remains disclosed.
- Interactive audiovisual acceptance is still pending and is not inferred from compilation.

## Responsiveness and reliability revision

### Scope acceptance revisit

The follow-up acceptance targets are observable and narrow:

1. Acme warning audio must finish before impact and the anvil must arrive in under one second after activation.
2. Banana Peel must visibly read as a tiny three-pronged yellow peel without using slime particles as its persistent representation.
3. Fake Tunnel must only place on a vertical wall, clearly explain an invalid cast, let the owner through a real wall even after collision zeros movement, and rebound non-owners.

### Findings and repairs

- **Acme pacing:** the form previously waited until tick 25 to spawn an anvil six blocks above the victim, while a normal-pitch whistle continued independently. The telegraph now starts at tick 2, the high-pitched warning begins then, the anvil spawns at tick 9 only 3.25 blocks above a victim, and the timed form closes at tick 10.
- **Banana representation:** the trap previously emitted `ITEM_SLIME` every three client ticks and used a shallow GeckoLib box. Persistent slime particles were removed. A custom full-bright renderer now draws a tiny, wobbling, black-outlined yellow peel with a stem and two spread prongs; slipping uses the registered Toon slip sound.
- **Fake Tunnel trigger:** wall collision can zero `deltaMovement`, so the old motion-dot-product gate prevented the caster from ever passing through. The trigger now finds the owner independently within 3.5 blocks and checks that the owner is looking into the tunnel. Its 0.2-block safe-exit scan must encounter collision before accepting an exit, preventing same-side teleports. Cast range increased to 12 blocks, horizontal wall placement is required, and invalid casts show `PAINT A WALL!` instead of spawning a nonfunctional tunnel.

### Identity, review, and evidence

- Lead repair identity: `/root` (primary agent).
- Scope reviewer, independent validator, and both non-author code review rounds remain pending because no agent creation/list/wait tooling or existing reviewer identities are available. No independent review is claimed.
- `git diff --check` passed after the repairs.
- Java 17 `compileJava` passed with the same temporary, uncommitted no-op `GravityDebug` shim used to isolate Toon Force compilation; the shim was removed after the command.
- Interactive timing, visual readability, and wall-passage testing remain pending. Keep the pull request unmerged until those checks and independent reviews complete.

## Visual asset and passive-effects revision

### Reported findings and disposition

- **Eye Pop UV distortion — repaired.** The cube model sampled a 64x64 texture through geometry declaring a 16x16 sheet. Eye Pop now uses a procedural, full-bright 2D renderer with separate ink outline, sclera, cyan iris, pupil, and highlight layers, so its appearance is independent of cube UVs.
- **Whistle outliving impact — repaired at the asset level.** Pitch changes could not bound a six-second source sample. The bundled OGG was trimmed from 5.956984 seconds to 0.380000 seconds with a 0.10-second fade, ensuring playback ends before the accelerated anvil impact.
- **Black/malformed Banana Peel — repaired.** The tinted-quad approximation was removed. The renderer now samples the authored transparent `toon_banana_peel.png` directly on a correctly proportioned 0.76-by-0.56-block cutout quad with a small wobble.
- **Shadow Toss rectangle — repaired.** The renderer now composes a head, torso, outstretched arms, and separated legs into a humanoid silhouette. Once stuck, it orients to the actual floor, ceiling, or wall normal instead of always facing the camera.
- **Pocket icons at top-left — repaired.** Minecraft 1.19's item renderer consumes absolute GUI coordinates rather than the surrounding PoseStack translation. Each reel icon is now rendered directly at its calculated screen coordinate.
- **Edge-walk passive — repaired and consolidated.** The duplicate catch-all fall-freezing subscriber was removed. The existing server-authoritative edge-height handler now runs once at END phase, holds the last grounded height only after walking over an edge, releases when looking down, and plays run animation, dust, and Toon run audio.
- **Dizzy-star passive — repaired.** The generic Toon Force damage subscriber now calls the shared 25% dizzy helper. The helper emits alternating firework/end-rod stars around the victim and no longer creates a zero-damage thorns event that could recurse through damage handling.

### Validation and review evidence

- Lead repair identity: `/root` (primary agent).
- `ffprobe` measured the replaced whistle asset at exactly `0.380000` seconds.
- `git diff --check` passed.
- Java 17 `compileJava` passed with the temporary, uncommitted `GravityDebug` shim; the shim was removed immediately afterward.
- Scope reviewer, independent validator, two non-author review rounds, and final acceptance revisit remain pending because agent creation/list/wait tooling and reviewer identities are unavailable. No independent review is claimed; keep the PR unmerged.
