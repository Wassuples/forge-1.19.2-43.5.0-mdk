# Missions: implementation guide

How the story missions in `docs/MISSIONS_SYSTEM.md` are built. Package: `com.wassuples.demonslayer.missions`.

**House rule: never use vanilla presentation when the mod can do it itself.** People talk in the world (speech
bubbles) or in the dialogue screen, never in chat. Story messages use the mod's HUD (banners and notices), not the
vanilla title or action bar. Effects use the mod's particles (`particle/ModParticles`) and sounds (`sound/ModSounds`).

## Shape of the system

| Piece | Role |
| --- | --- |
| `StoryMission` | One class per mission (a script). Lifecycle hooks: `onAccept`, `tick` (1/s), `onKill`, `onScopedHurt` / `onScopedDeath`, `onPlayerDeath` (default: fail), `onLogin`, `onComplete(outcome)` (rewards), `onEnd`, `tracker` (HUD). Gating: `isUnlocked`, `acceptBlocker`, `allowsSleep(ctx)`, `requiresGiver()` / `giverHint()` (missions only an NPC hands out). |
| `MissionContext` | What a hook gets: the player, the run's persistent `state()` (CompoundTag saved with the player), `notice` / `warn` / `banner` (the mod's HUD), `sound`, `spawnScoped(entity, key)` (removed when the run ends), `complete(outcome)`, `fail(reason)`, `findSurfaceSpot`. |
| `MissionManager` | Server-side driver: validates requests (`tryAccept(player, id, force, fromGiver)`, `abandon`), ticks the active mission, routes Forge events, removes stale scoped entities, syncs `PlayerMissions` + the `MissionTracker` every second. `withActive(player, id, ctx -> ...)` lets dialogue answers act on the running mission. `notify(player, text, warning)` posts a HUD notice. |
| `PlayerMissions` | Capability: one active run (id + state), completed missions and outcome branch, failure counts, story flags, perks, and named story timers (`setTimer` / `getTimer`, e.g. when a blade is due). NBT version 2. |
| `MissionRegistry` | All missions in order. Unbuilt ones are `PlannedMission` placeholders (shown as sealed). |
| `MissionPerks` | Mission-granted passives (Unyielding). |
| Client | `client/mission/MissionTrackerOverlay` (top-right tracker), `MissionNoticeOverlay` (banner + notice slips), the crow dispatch Missions tab. Client code is only reached via `DistExecutor`. |

### Dialogue (`dialogue/`)
- `DialogueNode`: a page of speaker, text and answers. An answer is a `Function<DialogueSession, DialogueNode>`, and returning null ends the conversation. Use `primary(...)` for the story's main path.
- `Dialogue`: a script that returns the first node. `NpcDialogues.forNpc` picks the script for an NPC.
- `DialogueManager`: owns the session on the server. The client only sees the current page and answers by index. The server checks each answer (token, distance, NPC alive). `open` is called on right-click. `openRemote` is for conversations the story starts (allowed up to 40 blocks away).
- Client: `client/dialogue/DialogueScreen` (live portrait, typewriter text, numbered answers, keys 1-9, Space/click to skip; the game doesn't pause) and `SpeechBubbleRenderer` (paper bubbles and name plates).
- A node can name a custom picker (`DialogueNode.picker("ore")`): the client shows that screen instead of the answer list and still answers by index. `"ore"` is `client/dialogue/OreSelectScreen` (the five 3D ores, `item/selection_ore_0..4`, turning on a tray).
- Packets: `OpenDialogueS2CPacket`, `DialogueChoiceC2SPacket`, `NpcSpeechS2CPacket` (in-world speech for any entity), `MissionNoticeS2CPacket`.

### Story NPCs (`entity/custom/story/`)
`StoryNpcEntity` plays a `StoryCharacter`:
- Characters: Tanjiro, Zenitsu, Inosuke, Kanao, Genya, the twins Kiriya and Kanata, anonymous candidates and the swordsmith (hyottoko mask; brings the blade).
- Skins: `textures/entity/npc/`. Tanjiro's skin keeps `textures/entity/tanjiro_kamado.png`.
- Behaviour: one anchor plus a leash, or a companion player to follow.
- Fighters attack demons. The twins are invulnerable; candidates are left at 1 HP at worst.
- `say(text)` makes the NPC speak in the world.
- Breathing: fighters use real forms (`IForm.performAction`) when a foe is within 4.5 blocks, every 8-14 s. Tanjiro uses Water, Zenitsu only Thunder's First Form, Inosuke Beast, Kanao Flower; each anonymous candidate gets the first three forms of Water, Flame, Thunder, Wind or Stone. Genya has none (canon). Non-player users must tick their active form themselves (`ActiveFormManager` only ticks players).

## Mt. Fujikasane (`worldgen/fujikasane/`)
One mountain per world. It is a structure, not a biome:
- `structure_set/fujikasane.json` uses `concentric_rings` with count 1 and a salt.
- `FujikasaneLayout` is the shared geometry. The radius is 128, the most a structure can cover (chunks are only built within 8 chunks of its start).
  - **Summit** (r < 72): a broad, rugged dome, about 32 to 80 blocks up, with rock spires, the Hand Demon's den (a pit of bones) and a ruined hut. It has no wisteria and is where the demons live. Groves of spruce and pine, dead trees, fallen logs, thickets, tall grass and boulders break up the sightlines so the summit can't be watched from one spot.
  - **Terrace** (r 72 to 94): wisteria, with the shrine plaza on the south side.
  - **Foot** (r 94 to 124): wisteria all the way down. The south stairway (lanterns, torii) runs from the plaza to r 118, then a path continues.
- `FujikasaneMountainPiece` builds it in `top_layer_modification`, one chunk at a time.
- `FujikasaneSites` (SavedData) locates the mountain once and records the residents. `placeAt` raises the mountain in a world made before it existed.
- `FujikasaneResidents` keeps the twins and the five named candidates. At the shrine the candidates idle near their spots; during a selection each holds a separate post on the summit, which moves every night. Tanjiro joins a player only after accepting the Hand Demon offer. The candidates also make small talk.
- `FujikasaneDemons` spawns captive demons on the summit at night (every 3 s, out of sight, at least 10 blocks inside the wisteria line, never in incense smoke), capped at 60 on the mountain:
  - around a player whose selection is under way: up to 3 + night / 2 nearby, one at a time, drawn from a nightly budget of 10 + 4 × night (14 on the first night, 38 on the seventh). Once the budget is spent the night goes quiet.
  - **Lingering** (staying within 12 blocks of one spot for 60 s) brings a warning, then extra demons that ignore the budget, until the player moves.
  - **Hiding** at the wisteria edge: a warning after 30 s; after 60 s the night is voided (it won't count) and the Hand Demon withdraws for the night.
  - around each fighting candidate NPC on the summit: 1 + night / 3 for the named five, 1 for anonymous ones;
  - around anyone else up there: 3.
- `CaptiveDemonEntity` (`demonslayer:captive_demon`) is not part of the Nemesis hierarchy.
  - It has 20 looks; variant 4 is the side quest's horned demon. Builds: lean (weaker, faster, smaller), ordinary, brute (tougher, harder hitting, slower, bigger), plus a random spread per demon.
  - Its strength scales with the night.
  - It stays inside the summit and walks away from the wisteria. Targets that leave the summit are dropped. The wisteria sears it if it reaches the edge.
  - Dying: it collapses into a column of ash at once, with a crumbling sound; being hit only hisses. In sunlight it simply dissolves into ash, silently. It has no ambient sound.
  - It hunts players and candidate NPCs.
- Natural monster spawns are off over the whole mountain.

## The Hand Demon (`entity/custom/HandDemonEntity`, `entity/renderers/hand/`)
- Model and renderer are Java code (no GeckoLib). The body has wrapped arms, eight arms it crawls on, four writhing back arms and a head with a jagged mouth. Attack arms are drawn from synced state (attack id, tick, up to 4 target ids, 3 burrow points).
- Stats: 800 HP, armor 12.
- Hard flesh: hits under 9 damage are cut to 40%.
- It regrows 3 HP per second unless a heavy hit landed in the last 4 seconds.
- Attacks are all telegraphed:
  - **barrage**: up to 4 arms strike where their targets stood when thrown;
  - **grab**: it drags the target to its mouth and bites, unless it takes 18 damage first;
  - **burrow**: cracks appear under up to 3 targets, then arms erupt and fling them;
  - **cluster fist**: slams the ground in a line;
  - **sweep**: hits everyone close.
- It speaks: taunts when its neck holds, at half health and at 20% (fear), and asks for his brother's hand as he dies. If Tanjiro is near, he holds it.
- It uses the mod's own boss bar (`client/mission/BossBarOverlay`) instead of the vanilla one.

## Adding a mission

1. Create `missions/story/<Name>Mission.java` extending `StoryMission` (`super("<path>", number, Arc.X)`).
2. Replace its `PlannedMission` entry in `MissionRegistry` with the new class.
3. `isUnlocked`: usually `missions.isCompleted(<previous id>)`. If an NPC hands the mission out, override `requiresGiver()` and start it from that NPC's dialogue with `MissionManager.tryAccept(player, id, false, true)`.
4. Keep run data in `ctx.state()`, and spawn mission-only demons with `ctx.spawnScoped`. Story characters are `StoryNpcEntity`s.
5. Grant rewards in `onComplete(ctx, outcome)`, and add reputation deltas as `RepEventType` + `RepDeltaCalculator` entries.
6. Add text under `mission.demonslayer.<path>.*` and `mission.demonslayer.outcome.<branch>`. Dialogue lines go in the lang file too.

## Mission 0: Final Selection (implemented)

`missions/story/FinalSelectionMission`.
- **Getting there:** new civilians get a *Letter of Introduction* on first join. Reading it shows the letter and the way. Holding it shows a compass band to the shrine.
- **Gather:** only the twins' dialogue starts the mission (the crow refuses). The candidates wait at the shrine. The selection begins at nightfall (time of day 12000-17000) with the player at the shrine.
- **Briefing:** the twins speak six lines in the world, about 5 s apart, while the player stays near the shrine.
- **Trial:**
  - Climb the north stairs through the torii onto the summit (90 s grace). After that, leaving the summit for 30 s fails the selection.
  - The briefing night counts as Night 1. Only naturally lived nights count, and beds are refused.
  - Named Nemesis demons hunt on the summit. Tanjiro keeps by the player, and the other candidates fight across the summit.
  - On Night 7 the Hand Demon emerges and speaks. Tanjiro runs over and offers his help in a dialogue. With no answer within 30 s, he fights elsewhere.
  - At the eighth dawn the Hand Demon withdraws if it is still alive.
- **Bedroll:** the twins give one on accept (and take it back at the ceremony). Use it by day to unroll it (`block/LaidBedrollBlock`, removed when you get up) and lie down: everyone nearby sees you lying on it (`RestStateS2CPacket` + `client/mission/RestPoseHandler`). Sneak, move, get hit or let a monster come within 10 blocks to get up. Time skips to 12600, just before dusk, once the share of overworld players resting (bedroll or bed) reaches the `playersSleepingPercentage` game rule (`missions/RestManager`). It also works on the summit by day during the Trial, and nights still have to be lived.
- **Other candidates:** 12 anonymous candidates (`StoryCharacter.CANDIDATE`, scoped `cand0..11`) climb up when you accept. They spread over the summit and can die; the survivors gather at the shrine, and the ceremony counts them.
- **Side quests** (`missions/story/FinalSelectionExtras`). They are found in the world, not announced on the HUD: a cornered candidate screams (heard far off) and sends up a column of distress smoke; Inosuke shouts his count; the horned demon roars; the lost sword glints. Candidate deaths aren't announced.
  - Cornered (nights 2–6);
  - Inosuke's 12-kill race;
  - Zenitsu's 8-kill plea;
  - Tanjiro's horned demon (night 3 on);
  - the lost sword at the den;
  - Kanao's daily coin.
  - Rewards: onigiri, wisteria incense (30 s demon-repelling smoke), mastery.
- NPC slayers don't stop to talk mid-fight: they shout instead, and a fight closes any open conversation.
- **Den foreshadowing** (nights 3–6): growls and ash from the ravine; Tanjiro smells it.
- **Return:** at the shrine Genya grabs Kanata's hair demanding his blade, and Tanjiro steps in (a short in-world scene). Talk to the twins: they take back the bedroll, measure the uniform, engrave the rank (Mizunoto), have you choose an ore in the 3D picker (stored as flag `final_selection.ore.N`) and assign a Kasugai crow, which speaks its first words. That completes the mission.
- **Branches:** `survived` and `hand_demon_slain` (the killing blow, or at least half the damage).
  - Both give: rank Mizunoto, a mastery pulse, reputation, a crow at once, and the Nichirin blade two in-game days later (timer `final_selection.blade_due`): the crow announces the swordsmith, who walks up to you, hands it over and leaves. If he can't reach you within 30 s, the blade is delivered directly.
  - The Hand Demon branch adds +20 kill record and Unyielding.
- **Failure:** dying during the Trial, leaving the summit, or becoming a demon. Dying before or after the Trial costs nothing. You can retry.
- Kasugai crows are now only given to players at rank Mizunoto or above (and to demons, as before). The crow is a Java model (`entity/models/KasugaiCrowModel`): wings folded when standing or perched, spread and beating in flight. The Kasugai screen (`client/gui/CrowDispatchScreen`) sizes itself to the window; the mission list and the details pane scroll separately (wheel, drag bar, Page Up/Down) and show every objective.

### Testing commands (OP 2)
- `/demonslayer fujikasane locate | tp | place`: find the mountain, go to its shrine, or raise one at your position (heavy).
- `/demonslayer mission start <player> demonslayer:final_selection` skips the checks, including the giver.
- `/demonslayer mission advance <player>`: gather → briefing → trial; during the trial, start a counted night, then survive it; in return, complete. To see the Hand Demon: `/time set 14600`, then `advance` until Night 7 starts. You must stand on the summit.
- `/demonslayer mission complete <player> survived|hand_demon_slain`, `/demonslayer mission deliver <player>` (the blade arrives now).
- `/demonslayer mission status|fail|reset <player>`.
