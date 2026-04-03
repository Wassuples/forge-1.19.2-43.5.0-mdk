**MISSIONS SYSTEM**

*Complete Design Reference — Version 1.0*

com.wassuples.demonslayer.missions

Minecraft Forge 1.19.2  ·  Demon Slayer Mod

# **Table of Contents**
- [1. Overview & Design Philosophy](#1-overview-design-philosophy)
- [2. Cross-System Dependencies](#2-cross-system-dependencies)
- [3. Mission Structure Specification](#3-mission-structure-specification)
- [4. Reward Categories](#4-reward-categories)
- [5. Tanjiro Spawn System](#5-tanjiro-spawn-system)
- [6. Nemesis Integration - Story Demons](#6-nemesis-integration-story-demons)
- [7. Arc I - Final Selection](#7-arc-i-final-selection)
- [8. Arc II - Kidnapper's Bog](#8-arc-ii-kidnappers-bog)
- [9. Arc III - Asakusa](#9-arc-iii-asakusa)
- [10. Arc IV - Tsuzumi Mansion](#10-arc-iv-tsuzumi-mansion)
- [11. Arc V - Mount Natagumo](#11-arc-v-mount-natagumo)
- [12. Arc VI - Rehabilitation Training](#12-arc-vi-rehabilitation-training)
- [13. Arc VII - Mugen Train](#13-arc-vii-mugen-train)
- [14. Arc VIII - Entertainment District](#14-arc-viii-entertainment-district)
- [15. Arc IX - Swordsmith Village](#15-arc-ix-swordsmith-village)
- [16. Arc X - Hashira Training](#16-arc-x-hashira-training)
- [17. Arc XI - Infinity Castle](#17-arc-xi-infinity-castle)
- [18. Arc XII - Sunrise Countdown](#18-arc-xii-sunrise-countdown)
- [19. Mission Number Reference](#19-mission-number-reference)

# **1. Overview & Design Philosophy**
The Missions System is the main story progression backbone of the Demon Slayer mod. It organises 24 missions across 12 arcs, adapts to the player's accumulated reputation and relationships, and integrates with both the Nemesis System (for live hierarchy demons) and the Reputation System (for branching outcomes and consequences).

Missions are not a strict linear queue. Later arcs are gated behind earlier mission completions, but within an arc the player has significant freedom in how they approach objectives. Branches are driven by accumulated reputation, not by dialogue choices in the moment — the world responds to what the player has already done, not to what they say.

Core Design Rules:

- Rewards are always mechanical: stat bonuses, ability/form mastery, story branches, reputation deltas, kill record bonuses, Blood Points (demon-path). No cosmetics.

- No generic demons spawn. Every demon in a mission has a named identity.

- The critical path is always completable regardless of reputation.

Branches add, alter, or replace content. They never gate story progression.

- Missions marked with ⚠ are rough outlines. Narrative and locations may change.

# **2. Cross-System Dependencies**
| System | How Missions Interact With It |
| --- | --- |
| Reputation System | Mission outcomes write reputation deltas via AUTO WitnessEvents. Branch conditions check Corps Rep, Tanjiro Affinity, Demon Notoriety, and Muzan Favor tiers. See Reputation System §11 for all mission-specific deltas. |
| Nemesis System | Story-arc demons (Akaza, Doma, Kokushibo, etc.) may be backed by live Nemesis Kizuki slots depending on the `canonKizuki` gamerule. Killing a story demon whose slot is occupied triggers the standard power vacuum. See §6 of this document. |
| Rank System | Mission completion contributes to KillRecord. Certain missions grant kill record bonuses. The Hashira rank (500 kills as Kinoe or solo Kizuki kill) can be achieved through mission combat. |
| Style Mastery | Combat during missions grants mastery progress toward forms of the player's active style, proportional to damage dealt using that style. Boss kills grant a bonus mastery pulse. |
| Blood Points | Demon-path players gain BP from killing mission demons proportionally to the demon's BP value. Story arc demons have fixed BP values defined per mission. |

# **3. Mission Structure Specification**
Each mission entry in this document contains:

- Arc header — arc number, name, and mission range
- Mission number and name
- Location — biome, structure, or named area
- Context — narrative framing, lore notes, or design intent
- Objectives — ordered list of what the player must do to complete the mission
- Branches table — condition, outcome, rewards/consequences, and a cross-reference to Reputation System §11 for rep deltas
- Tanjiro Spawn note — canonical presence, base spawn chance, and what his presence changes

Branch table columns:

| Column | Meaning |
| --- | --- |
| Condition | What must be true for this branch to be active. |
| Outcome | What happens narratively and mechanically. |
| Rewards / Consequences | Stat bonuses, ability unlocks, kill record bonuses, BP, and story access. |
| Rep (see Rep. Doc §11) | Cross-reference only - full deltas are in the Reputation System doc. |

# **4. Reward Categories**
| Category | Description | Examples |
| --- | --- | --- |
| Stat Bonus | Permanent increase to Vigor, Strength, or Agility as defined in DemonTrait stat model. | '+2 Vigor', '+1 Strength, +1 Agility' |
| Style Mastery | Bonus mastery progress toward forms of the player's currently active style. Proportional to contribution during the fight. Boss kills grant a mastery pulse. | 'High mastery pulse to active style forms', '+1 form progress' |
| Ability Unlock | A new technique, passive, or form that is not accessible through normal training. | 'Unlocks Form VII of active style', 'Unlocks Total Concentration Breathing passive' |
| Story Access | A branch, NPC relationship state, or lore entry that becomes available. | 'Rengoku survives (non-canon branch access)', 'Tamayo research entry unlocked' |
| Kill Record Bonus | Bonus kills added to KillRecord.killsAtCurrentRank toward rank advancement. | '+20 kill record bonus', '+50 kill record bonus' |
| Blood Points | Demon-path players receive BP from the defeated demon. Fixed value per story demon. | 'Gains Enmu\'s BP (800)', 'Gains 50% of Rui\'s BP (tainted)' |
| Rep Delta | See Reputation System §11 for all mission-specific values. Not repeated here. | Cross-reference only |

# **5. Tanjiro Spawn System**
Tanjiro Kamado and his current party members may appear in missions as friendly NPCs who assist in combat and dialogue. His spawn behavior follows a three-tier logic:

| Tier | Rule | Example |
| --- | --- | --- |
| Canonical Presence | Tanjiro is part of the mission's canonical story. He always spawns. No chance roll, no condition. | Mission 7 (The Traitor): Tanjiro is the mission objective. He is always present. |
| Plausible Presence | Tanjiro could reasonably be in the area. A fixed per-mission base spawn chance is rolled at mission start. Affinity does not modify this chance. | Mission 5 (Mt. Natagumo): Base 40% chance. If the roll succeeds, Tanjiro and available party members spawn as allies. |
| Not Present | Tanjiro cannot appear in this mission - either the location is impossible for him to be at, or the narrative explicitly places him elsewhere. | Mission 9 (The Lower Moon): Tanjiro is confined to the Butterfly Estate recovering from injuries. He cannot appear. |

Kizuki Override Rule:

Whenever Tanjiro is present in a mission and a Kizuki (any moon-rank demon) is

engaged, Tanjiro ALWAYS assists — regardless of whether the player accepted or

declined his help earlier in the same mission. He will not stand by while a Kizuki

threatens an ally. This override applies in every mission, not just Missions 0 and 4.

In Missions 0 and 4 specifically: Tanjiro offers to help at each encounter.

The player may accept or decline for non-Kizuki fights. The Kizuki override

then forces his assistance if a moon-rank demon appears.

## **Party Composition**
When Tanjiro spawns, the party members who spawn alongside him depend on story progression. Characters who have been killed in earlier missions (via player action in non-canon branches) do not appear.

- Before Mission 7: Tanjiro and Nezuko only.
- After Mission 7 (Nezuko proven innocent): Tanjiro, Nezuko, Zenitsu, Inosuke.
- After Mission 13: Kanao joins the pool.
- After Mission 15: Genya joins the pool (if alive).
*Party members who are the subject of their own missions in the Infinity Castle arc (Missions 16–21) are not available as bonus spawns during those missions — they are occupied with their own fight.*

# **6. Nemesis Integration — Story Demons**
Several story arc missions feature demons who may or may not be backed by live Nemesis Kizuki slots, depending on the server's canonKizuki gamerule and the state of the hierarchy at the time the mission triggers.

| Rule | `canonKizuki = true` | `canonKizuki = false` OR slot already upset |
| --- | --- | --- |
| Story demon occupies a Kizuki slot | The story demon IS the Nemesis Kizuki occupant of that rank. Killing them in their mission triggers the standard power vacuum cascade. | The relevant Kizuki slot's current occupant (whatever procedural demon holds it) takes the place of the story demon in the mission. The fight uses that demon's DemonTrait stats, taunts, and behavior. |
| Story demon is killed | Power vacuum fires. Slot vacated. Daily sim fills it. | Power vacuum fires for whoever was substituted. Slot vacated. |
| Story demon retreats or survives (non-canon branch) | Nemesis Kizuki slot remains occupied. The demon's BP and relationships update from the encounter. | Same - the substituted demon retains the slot. |
| Akaza (UM3) - special case | If Akaza is already dead (killed in Mission 10 non-canon), his slot may be held by a promoted demon. That demon becomes the Mission 18 boss instead. | Same rule applies - whoever holds UM3 at mission time is the boss. |

## **Fixed Story-Only Demons**
Some demons appear in missions but are never part of the live Nemesis hierarchy regardless of settings. They are spawned as mission-scoped entities only and are removed when the mission ends:

| Demon | Mission | Notes |
| --- | --- | --- |
| Hand Demon | 0 - Final Selection | One-off. Not part of the hierarchy. Does not affect Nemesis on death. |
| Swamp Demon | 1 - Kidnapper's Bog | One-off mission demon. |
| Aratsu | 2 - Asakusa | Named tutorial demon. Fixed identity, not in Nemesis pool. |
| Susamaru | 3 - Tamayo | Fixed story NPC. Dissolves from Muzan's blood curse - not a standard kill. |
| Yahaba | 3 - Tamayo | Fixed story NPC. |
| Tongue Demon | 4 - Tsuzumi Mansion | Mission-scoped. |
| Horned Demon | 4 - Tsuzumi Mansion | Mission-scoped. |
| Kyogai | 4 - Tsuzumi Mansion | Former Lower Moon. Not in current Nemesis hierarchy. Mission-scoped. |
| Spider Puppets | 5 - Mt. Natagumo | Controlled entities, not individual Nemesis demons. |
| Mother's Doll | 6 - Mother's Doll | Mini-boss. Mission-scoped. |
| Mother Spider | 6 - Mother's Doll | Mission-scoped. |
| Elder Brother | 7 - Spider Family | Mission-scoped. |
| Father Spider | 7 - Spider Family | Mission-scoped. |
| Daughter Spider | 7 - Spider Family | Mission-scoped. |

# **7. Arc I — Final Selection**

*Missions: Mission 0*

**Mission 0: Final Selection**

**Location:** Mt. Fujikasane — wisteria-enclosed mountain biome

**Context:** *The player's entrance into the Demon Slayer Corps. Survive seven days and seven nights on a demon-infested mountain. The Hand Demon — a monster that has consumed over 50 trainees — spawns on the final night.*

**Objectives**

- Enter the Mt. Fujikasane biome to begin the timer.
- Survive for 7 in-game days and 7 in-game nights (168,000 ticks).
- On the final night (Night 7), the Hand Demon spawns. Engaging is optional.
- Reach sunrise on Day 8 to complete the mission.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — reach sunrise without killing the Hand Demon | Mission completes. Player is assigned rank Mizunoto with 0 kills. | Rank: Mizunoto. Kill record: 0. Style mastery: minor pulse for active style from any demons killed during survival. | Corps +10, Demon Notoriety proportional to demons killed during survival. |
| Kill the Hand Demon before sunrise on Night 7 | Mission completes with high-achievement flag. Player is assigned Mizunoto with a substantial kill head-start. | Rank: Mizunoto. Kill record bonus: +20 kills. Style mastery: large pulse. Ability unlock: 'Unyielding' passive — the player's first lethal hit each in-game day is survived at 1 HP once. | Corps +20, Demon Notoriety +15 (AUTO). |
| Player dies during the 7-day period | Mission fails. Player is not accepted into the Corps. | No rank. No rewards. Player must retry Mission 0 to progress the story. | No rep change on death. |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro undergoes his own Final Selection on Mt. Fujikasane at the same time. He always spawns as a fellow participant. During the Hand Demon encounter on Night 7, Tanjiro appears near the player and offers to fight alongside them. The player chooses: accept his help (he assists in the Hand Demon fight) or decline (he steps back and fights his own demons separately, leaving the Hand Demon entirely to the player). If the player is not fighting a Kizuki, the choice stands. Note: the Hand Demon is not a Kizuki — the forced-assist rule does not apply here.

# **8. Arc II — Kidnapper's Bog**

*Missions: Mission 1*

**Mission 1: Kidnapper's Bog**

**Location:** Northwest Town — village and surrounding wetlands

**Context:** *Women are disappearing from a northwestern town. The player's first field assignment as a Demon Slayer.*

**Objectives**

- Investigate the village. Speak to at least 3 Villager NPCs to gather clues about the demon's location.
- Locate the Swamp Demon's portal in the wetlands northwest of the village.
- Defeat the Swamp Demon.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — find portal via NPC clues, kill Swamp Demon | Mission clears. | Kill record +1. Style mastery pulse. Kill record bonus for solo clear if no other entities dealt damage. | Corps +12, Tanjiro +0, Demon +6 (see Rep. Doc §11). |
| Speak to all 5 available NPCs (thorough investigation) | Additional clue reveals a second demon lair. Optional bonus encounter: a nameless Swamp Demon accomplice spawns. | Kill record +2 if accomplice killed. Style mastery large pulse. +5 kill record bonus. | Corps +5 bonus on top of base. |
| Tanjiro spawns (25% base chance) and assists | Tanjiro fights alongside the player. Kill does not count as solo. | Tanjiro affinity state noted. No kill record bonus for solo. Style mastery pulse still applies. | Tanjiro +5 on mission complete. |

Tanjiro Spawn: PLAUSIBLE PRESENCE — 25% base chance. If present, Tanjiro assists in combat and adds a unique dialogue line during the investigation phase, reducing the required clue count from 3 to 2.

# **9. Arc III — Asakusa**

*Missions: Missions 2–3*

**Mission 2: Asakusa**

**Location:** Tokyo — Asakusa District, crowded night market

**Context:** *The player patrols the Asakusa district. This mission introduces Muzan Kibutsuji and the named tutorial demon Aratsu — a mid-tier servant demon that Muzan dispatches to test the player after recognising them as a Slayer.*

**Objectives**

- Patrol the streets of Asakusa (enter the Asakusa District biome and move through 3 patrol zones).
- Optional: encounter Muzan Kibutsuji in his human disguise. He flees — do not attempt to fight him.
- Defeat Aratsu, the Asakusa Demon, who arrives at an accelerated rate if Muzan was encountered.
- Seek out the NPCs Tamayo and Yushiro following Aratsu's defeat.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — patrol, fight Aratsu, meet Tamayo and Yushiro | Mission clears. Tamayo and Yushiro are introduced as story NPCs. | Kill record +1. Style mastery pulse. Tamayo story access unlocked (Mission 3 available). | Corps +8, Demon +6, Muzan –4 (spotted the player). |
| Muzan encounter triggers (random chance during patrol, ~30%) | Muzan flees into the crowd. Aratsu spawns immediately rather than after full patrol. Fight is harder — Aratsu is empowered by Muzan's presence. | Empowered Aratsu: +20% HP and damage. Kill record +1 with bonus mastery pulse. If defeated: '+5 kill record bonus' for defeating an empowered demon. | Muzan –8 (he personally noted the player). Demon Notoriety +10 extra. |
| Muzan encounter occurs AND Tanjiro is present | Tanjiro recognizes Muzan. Dialogue branch: Tanjiro shares Muzan profile intel with the player early. | Muzan profile intel unlocked in story log. Tanjiro +8 on mission complete. | See above plus Tanjiro delta. |

Tanjiro Spawn: PLAUSIBLE PRESENCE — 20% base chance. If present, Tanjiro recognises Muzan during the encounter and triggers the additional dialogue branch.

**Mission 3: Tamayo**

**Location:** The Hidden Clinic — Tamayo's concealed safehouse

**Context:** *Tamayo's clinic is ambushed by two of Muzan's agents: Susamaru (Temari Demon) and Yahaba (Arrow Demon). The player must survive and defeat both.*

**Objectives**

- Enter Tamayo's hidden clinic.
- Survive the ambush — both Susamaru and Yahaba attack simultaneously.
- Defeat Yahaba.
- Defeat Susamaru (or allow Tamayo's intervention — see branches).
- Both demons neutralised to clear the mission.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — kill both Susamaru and Yahaba | Mission clears. | Kill record +2. Style mastery pulse (both fights contribute). Tamayo relationship advances. | Corps +8, Tanjiro +12, Muzan –8 (two agents lost), Demon +10. |
| Player does not deliver killing blow to Susamaru (mercy branch) | Tamayo intervenes with her blood technique. Susamaru dissolves from Muzan's internal blood curse — she cannot be cured, only released. Tamayo is visibly affected. | Kill record +1 (Yahaba only). Style mastery pulse. Ability unlock: Tamayo shares demon biology research — passive 'Demon Sense': first encounter with any new demon type reveals their weakness type in HUD. | Corps –5 (incomplete kill logged), Tanjiro +15 (witnessed mercy), Muzan –15. |
| Tanjiro is present (40% base chance) | Tanjiro assists both fights. His presence causes Susamaru to briefly hesitate, giving the player an opening. | Tanjiro +8 on complete. No change to kill record or mastery — Tanjiro's kills do not count for the player. | See above rep deltas. |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro is always present for the ambush at the clinic. He assists in both the Susamaru and Yahaba fights and is present when Tamayo and Yushiro are introduced. He draws aggro from one demon while the player handles the other.

# **10. Arc IV — Tsuzumi Mansion**

*Missions: Mission 4*

**Mission 4: Tsuzumi Mansion**

**Location:** The Drum House — a shifting, drum-controlled mansion

**Context:** *A disappearance report leads the player to a mansion whose rooms rotate when the demon Kyogai beats his drums. Three demons occupy the mansion: the Tongue Demon and Horned Demon are fodder guards; Kyogai, a former Lower Moon stripped of his rank, is the final boss.*

**Objectives**

- Enter the Drum House.
- Navigate the rotating mansion (room layout changes when Kyogai beats his drums).
- Defeat the Tongue Demon.
- Defeat the Horned Demon.
- Defeat Kyogai, the Drum Demon.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — kill all three demons | Mission clears. | Kill record +3. Style mastery pulse (scaled by damage dealt to each). Kyogai's BP award (demon-path: 200 BP fixed). | Corps +30, Demon Notoriety +25. |
| Solo clear — no other entities deal damage to any boss during the mission | All three kills counted as solo. | Kill record +3 with solo flag. Style mastery large pulse. Kill record bonus +10. Demon-path: full Kyogai BP. | Demon Notoriety extra +15 (AUTO). |
| Tanjiro is present AND player does not kill Kyogai's drum notebook pages (non-canon mercy) | Tanjiro finds and preserves Kyogai's score notebooks after the fight. A non-canon moment acknowledging Kyogai's legacy. | Tanjiro +15. Carry-forward: at Mission 9 (Mugen Train), Tanjiro's morale bonus applies (+10% NPC damage). | Tanjiro +15 (AUTO). |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro and Inosuke always enter the Drum House alongside the player and navigate the rotating rooms together. For the Tongue Demon and Horned Demon fights, Tanjiro offers to help — the player may accept or decline. For the Kyogai boss fight, Tanjiro again offers assistance. If the player accepts, he assists. If the player declines, he steps back. Kyogai is not a Kizuki, so the forced-assist rule does not apply. Note for both missions 0 and 4: when facing any Kizuki (moon-rank demon) at any point in the game, Tanjiro always assists regardless of the player's preference — he will not stand by while a Kizuki threatens an ally.

# **11. Arc V — Mount Natagumo**

*Missions: Missions 5–8*

**Mission 5: Mt. Natagumo**

**Location:** Spider Forest — Outer Perimeter

**Context:** *Mt. Natagumo is a forest controlled by the Spider Family, a group of demons posing as a human family. The outer perimeter is infested with demon-controlled Demon Slayer puppets — corpses of fallen Slayers reanimated by the Mother Spider's threads.*

**Objectives**

- Enter the Spider Forest outer perimeter.
- Locate the trapped NPC Murata (marked on map after entering the zone).
- Neutralize all Demon Slayer Puppets in Murata's area (thread-controlled — kill the threads, not the bodies).
- Escort Murata to the safe perimeter boundary.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — rescue Murata, neutralize all puppets | Mission clears. | Kill record: puppet kills count at 0.5 each (they are not true demons). Style mastery pulse. Murata becomes a recurring minor NPC. | Corps +15, Tanjiro +5 if present. |
| Murata survives with zero damage taken (perfect escort) | Murata is unharmed. He files a glowing report. | Kill record bonus +5. Corps Suspicion –20 (clean, professional conduct logged). | Corps +20 bonus (perfect rescue). |
| Tanjiro spawns (40% base chance) and spots an alternate path | Tanjiro finds a route that bypasses the first puppet wave entirely. | First puppet wave skipped. Reduced damage entering the inner perimeter. Tanjiro +8 on complete. | Tanjiro +8. |

Tanjiro Spawn: PLAUSIBLE PRESENCE — 40% base chance. Canonically, Tanjiro is on Mt. Natagumo during this arc. If present, he assists in combat and can identify an alternate route into the inner perimeter.

**Mission 6: Mother's Doll**

**Location:** Spider Forest — Inner Perimeter

**Context:** *The Mother Spider Demon controls the Slayer puppets from within the inner perimeter. She uses her own children as puppets. The Mother's Doll is a hardened puppet-construct she launches as a first wave.*

**Objectives**

- Penetrate the inner perimeter.
- Locate the Mother Spider Demon's position.
- Defeat the Mother's Doll (armoured puppet mini-boss).
- Defeat the Mother Spider Demon.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — defeat both Mother's Doll and Mother Spider Demon | Mission clears. | Kill record +2 (Mother counts as 1, Doll counts as 1). Style mastery pulse. Demon-path: 150 BP (Mother). | Corps +20, Demon +18. |
| Player does not kill any of the controlled child puppets during the Doll phase | The child puppets are innocent victims. Avoiding their deaths is an act of restraint. | Tanjiro +25 (witnessed restraint). Carry-forward: at end of Mother's Doll fight, Tanjiro shares what he thinks of the player — early Tanjiro affinity tier confirmation available via dialogue. | Tanjiro +25 (AUTO). |

Tanjiro Spawn: PLAUSIBLE PRESENCE — 40% base chance (same roll as Mission 5 — if Tanjiro spawned in Mission 5, he is automatically present here too). If present, he witnesses the child puppet interaction.

**Mission 7: Spider Family**

**Location:** Spider Forest — Deep Woods and River

**Context:** *The final sweep of Mt. Natagumo. The Elder Brother, Father Spider, Daughter Spider, and Rui (Lower Moon Five) must be defeated. Rui and Daughter Spider are timed encounters — Hashira arrive if the player takes too long.*

**Objectives**

- Defeat the Elder Brother Spider Demon.
- Defeat the Father Spider Demon (high-HP tank boss).
- Engage the Daughter Spider Demon — timed encounter (5 minutes).
- Engage Rui, Lower Moon Five — timed encounter (8 minutes).
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Daughter Spider killed within time limit | Player receives full loot. | Kill record +1. Style mastery pulse. Demon-path: Daughter's BP. | Demon +15, Corps +10. |
| Daughter Spider timer expires — Shinobu arrives | Shinobu kills Daughter Spider. Player receives no kill credit. | No kill record for Daughter Spider. Shinobu observes the player — if Corps Rep ≥ Trusted (200): Suspicion –10. | Corps +8 (Shinobu present). No Demon Notoriety from this kill. |
| Rui (LM5) killed within time limit — pure (no interferers) | Pure Blood Battle victory for demon-path players. Full solo kill for all players. | Kill record +1 with Kizuki flag. Demon-path: 100% Rui's BP. Kizuki solo kill counts toward Hashira alt path. LM5 power vacuum fires. | Corps –40, Demon +50 (AUTO). Tanjiro –10 if present (he wanted to fight Rui). |
| Rui (LM5) killed within time limit — tainted (interferers present) | Tainted Blood Battle for demon-path players. | Kill record +1. Demon-path: 50% Rui's BP. DISHONORED status applied. LM5 power vacuum fires. | Corps –70, Demon +30, +30 Suspicion. |
| Rui timer expires — Giyu arrives | Giyu kills Rui. Player receives no kill credit. | No Kizuki kill credit. No BP. No rank claim. Giyu files a favorable Corps report (+15 Corps delayed). | Corps +10, Tanjiro +15, Demon –25 (no kill). |

Tanjiro Spawn: CANONICAL PRESENCE (if alive) — Tanjiro is always present for the Rui fight. He and Nezuko execute their signature combination against Rui. If the player defeats Rui within the time limit before Tanjiro engages, Tanjiro's kill does not fire. If the timer is running low, Tanjiro engages regardless of the player's progress.

**Mission 8: The Traitor**

**Location:** Forest Outskirts — edge of Spider Forest

**Context:** *Following the Mt. Natagumo incident, the Corps has ordered Tanjiro and Nezuko to stand trial. Nezuko has been seen as a demon and the Corps wants her destroyed. The player must escort them to DSHQ. This is the system's first major reputation inflection point.*

**Objectives**

- Locate Tanjiro and Nezuko at the forest outskirts.
- Escort them to DSHQ.
- Nezuko's innocence will be established at DSHQ — this is a fixed story beat.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Tanjiro Affinity < Acquaintance (100) — low trust | Tanjiro does not comply peacefully. The escort becomes contested — Inosuke and Zenitsu may intervene defensively. | Mission completes but with rep cost if player used force. No kill record bonuses from this mission. | Tanjiro –40 per crew member harmed. Corps +10 (mission complete). See Rep. Doc §11. |
| Tanjiro Affinity ≥ Acquaintance (100) — willing compliance | Tanjiro agrees to come. Peaceful escort. He speaks positively to the Hashira at DSHQ. | Suspicion –15 (Tanjiro's endorsement). Tanjiro +20 on complete. | Corps +15, Tanjiro +20. |
| Corps Rep ≥ Trusted (200) AND Tanjiro Affinity ≥ Friend (450) — early proof | Nezuko's innocence is established before reaching DSHQ. Kagaya's letter combined with the player's standing accelerates the process. | Nezuko freed immediately. Story branch: Nezuko's status no longer creates political tension in any future mission. Suspicion –50. Kill record bonus +10 (goodwill). | Corps +30, Tanjiro +20, Suspicion –50 (AUTO). |
| Fixed beat — standard proof at DSHQ (always fires if early proof did not) | Nezuko proven innocent at trial regardless of rep. | Standard story progression. Nezuko's status permanently resolved. | Corps +30, Tanjiro +20, Suspicion –30 (AUTO). |
| Player reports Tanjiro's resistance to a Demon Slayer NPC before the mission starts | DSHQ sends a supervision escort. Mission is easier mechanically but Tanjiro's trust is severely damaged. | No kill record bonus. Future Friend+ affinity branches become harder to reach. | Corps +20, Tanjiro –80, Suspicion +40. |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro is the mission objective. He is always present. His behavior (willing or resistant) is driven entirely by accumulated affinity.

# **12. Arc VI — Rehabilitation Training**

*Missions: Mission 9*

**Mission 9: The Lower Moon  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Randomized — City, Forest, or Mountain (generated at mission start)

**Context:** *While Muzan is dismantling the Lower Moons following the Natagumo incident, one has escaped or is being hunted. The player is dispatched to intercept. The target is a randomly selected Lower Moon from the live Nemesis hierarchy, weighted toward LM6–LM4 and explicitly excluding Enmu (LM1, reserved for Mission 11).*

**Objectives**

- Receive the dispatch order specifying the target's last known territory.
- Track down the fleeing Lower Moon — search their territory biome for their spawn.
- Defeat the target Lower Moon before they escape (time limit: 2 in-game days).
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Kill the Lower Moon within the time limit | Mission clears. Power vacuum fires for the killed LM slot. | Kill record +1 with Kizuki flag. Style mastery large pulse. Demon-path: target's BP. LM slot vacated. | Corps delta by LM rank (see Rep. Doc §11). Demon Notoriety scales with LM rank. |
| Kill the Lower Moon — solo (no interferers) | Pure solo Kizuki kill. | Kill record +1 with solo Kizuki flag (counts toward Hashira alt path). All rewards above plus kill record bonus +15. | Demon Notoriety extra +10 (AUTO). |
| Time limit expires — Lower Moon escapes | Mission fails. The target survives and gains a significant BP boost in the Nemesis hierarchy. | No kill credit. Target gains +200 BP. Target's Nemesis relationships updated — they become more aggressive. | Corps –20, Demon Notoriety –15. |
| Tanjiro spawns (15% base chance) and provides intel | Tanjiro shares information about the target's last known movement, reducing the search phase. | Search phase shortened by approximately half an in-game day. | Tanjiro +5 on complete. |

Tanjiro Spawn: NOT PRESENT — Tanjiro is confined to the Butterfly Estate recovering from his Mt. Natagumo injuries during the Rehabilitation Training arc. He cannot appear in this mission under any condition.

# **13. Arc VII — Mugen Train**

*Missions: Missions 10–11*

**Mission 10: Mugen Train  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** The Mugen Train — a moving steam locomotive

**Context:** *Enmu, Lower Moon One, has been given blood by Muzan and commands an entire moving train. The player boards alongside the Flame Hashira Rengoku Kyojuro. The train is infested with demons, and Enmu attempts to trap all passengers in unbreakable dreams.*

**Objectives**

- Board the Mugen Train and rendezvous with Rengoku Kyojuro (Flame Hashira).
- Clear the demon-infested carriages — defeat all minor Train Demons before they kill passengers.
- Fall asleep (Enmu's ability triggers). Defeat the nightmare version of your own character in the dream realm to wake up.
- Fight Enmu atop the moving train — final boss of the mission.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — defeat Enmu, all passengers alive | Mission clears with good outcome. | Kill record +1 (Enmu). Style mastery large pulse. Demon-path: Enmu's BP (800 fixed). Kill record bonus +5 (passenger survival). | Corps +35, Tanjiro +12, Muzan –25, Demon +22. |
| Defeat Enmu but one or more passengers die | Mission clears but with casualty flag. | Kill record +1. Style mastery pulse. Demon-path: Enmu's BP. No passenger survival bonus. | Corps +25, Tanjiro +8. |
| Nightmare realm — fast clear (under 3 minutes real time) | Bonus reward for swift self-mastery. | Permanent stat bonus: +2 to the player's lowest stat (Vigor, Strength, or Agility). | No rep change from nightmare realm. |
| Kyogai notebook carry-forward (from Mission 4 non-canon branch) | If the player preserved Kyogai's notebooks in Mission 4, Tanjiro's morale applies here. | Tanjiro NPC deals +10% damage during the carriage-clearing phase. | No additional rep change. |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro, Zenitsu, and Inosuke are always present as fellow passengers. They assist in clearing carriages and wake themselves from the dream. They do not assist in the Enmu boss fight directly — that is the player's fight.

**Mission 11: Akaza  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Derailed Train Wreckage — field beside the ruined locomotive

**Context:** *Immediately following Enmu's defeat, the Upper Moon Three, Akaza, arrives. Rengoku engages him. In canon, Rengoku dies here. The player chooses whether to survive until sunrise or attempt the non-canon path of defeating Akaza directly.*

**Objectives**

- Engage Akaza (Upper Moon Three).
- Choose your path: fight to deplete his HP (non-canon) or survive until sunrise (canon).
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Survive until sunrise — Akaza retreats at dawn (canon path) | Akaza retreats as the sun rises. Rengoku dies from his wounds. Canon ending. | Kill record: no Akaza kill. Style mastery large pulse (surviving UM3 is a significant test). Demon-path: 100% Akaza BP. UM3 power vacuum fires. Akaza gains EXTREME HATRED flag in Nemesis — he becomes the most dangerous hunter for this player permanently. | Corps +25, Tanjiro +18, Muzan –15, Demon +18. |
| Deplete Akaza's HP before sunrise (non-canon path) | Akaza is defeated but vanishes before death — retreats underground before the sun can claim him. Rengoku survives critically wounded. | Kill record +1 with UM3 Kizuki flag (counts toward Hashira alt path). Style mastery very large pulse. Demon-path: 100% Akaza BP. UM3 power vacuum fires. Akaza gains EXTREME HATRED flag in Nemesis — he becomes the most dangerous hunter for this player permanently. | Corps +50, Tanjiro +25, Muzan –50, Demon +55 (AUTO). |
| Corps Rep ≥ Revered (600) AND player deals 40% of Akaza's HP before Rengoku drops below 20% | Rengoku survives critically wounded (non-canon survival). Requires the non-canon HP depletion branch OR this specific protection condition. | Rengoku survives. Becomes a permanent Flame Breathing training master. His survival is recorded as a story flag. | Corps +80 (saving a Hashira is exceptional). Tanjiro +30 (AUTO). |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro, Zenitsu, and Inosuke are present but severely exhausted from the train fight. They witness the Akaza battle but cannot meaningfully assist. Their presence is narrative only in this mission.

# **14. Arc VIII — Entertainment District**

*Missions: Missions 12–13*

**Mission 12: Entertainment District  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Yoshiwara — Red Light District

**Context:** *Demons have been preying on residents of Yoshiwara. The Sound Hashira Tengen Uzui has gone undercover. The player must infiltrate and locate the demons' hideout.*

**Objectives**

- Infiltrate Yoshiwara using one of the available methods (see branches).
- Gather information from NPCs within Yoshiwara — find enough clues to pinpoint the demon hideout.
- Locate the demons' lair (triggers Mission 13).
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — civilian disguise infiltration | Player blends in as a civilian. Requires speaking to 4 NPCs for enough intel. | No special rewards. Standard investigation path. Mission 13 unlocks on completion. | No rep change during infiltration. |
| Corps Rep ≥ Trusted (200) — Corps contact inside Yoshiwara | A Corps informant provides direct location intel, skipping most investigation steps. | Investigation skipped entirely. Arrives at Mission 13 faster. Kill record bonus +3 (efficiency recognized). | Corps +5. |
| Demon Notoriety ≥ Feared (350) — a demon inside Yoshiwara panics | A surviving demon inside recognises the player and flees — giving away the hideout's location by accident. | Investigation skipped. Demon's panic alerts Daki and Gyutaro that a powerful Slayer is coming — Mission 13 boss arrives pre-buffed (+10% HP). | Demon Notoriety +5 (fear spreads). |
| Tanjiro spawns (20% base chance) | Tanjiro, Zenitsu, and Inosuke have also infiltrated the district. Tanjiro shares observations, reducing the required NPC clue count from 4 to 2. | Investigation shortened. Tanjiro +5 on complete. | Tanjiro +5. |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro, Zenitsu, and Inosuke have all infiltrated Yoshiwara. Tanjiro is always present during the investigation phase. His presence reduces the required NPC clue count from 4 to 2 and adds unique dialogue when the hideout is located.

**Mission 13: Daki and Gyutaro  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Yoshiwara — Underground Demon Hideout

**Context:** *The Upper Moon Six are revealed to be two demons sharing one body: Daki and Gyutaro. Both heads must be severed simultaneously or they regenerate each other. Tengen Uzui is present and fights alongside the player.*

**Objectives**

- Engage Daki and Gyutaro in the dual boss fight.
- Sever both heads simultaneously to clear the mission. Killing only one triggers regeneration.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — simultaneous kill | Mission clears. Both UM6 siblings removed from Nemesis hierarchy. UM6 power vacuum fires. | Kill record +2 (both count as one Kizuki kill for the Kizuki flag, but +2 to total kill count). Style mastery very large pulse. Demon-path: Gyutaro's BP (UM6 value). UM6 power vacuum fires. | Corps +60, Muzan –40, Demon +50 (AUTO). |
| Tanjiro present (30% base chance) — signals the simultaneous kill timing | Tanjiro coordinates with the player, widening the simultaneous kill window from 1 second to 3 seconds. | Timing window extended. Tanjiro +10 on complete. | Tanjiro +10. |
| Player hit by Gyutaro's poison sickle and not cured before mission ends | Poison debuff carries into Mission 14 (Swordsmith Village) opening phase. | Poison debuff active at Mission 14 start. Curable via Tamayo (reachable through Tanjiro Affinity ≥ Friend (450) network). | No additional rep change. |
| Pre-buffed arrival (from Mission 12 Demon Notoriety branch) | Daki and Gyutaro begin the fight with +10% HP. | All rewards same as standard. No penalty for the buff — the challenge is simply harder. | No change. |

Tanjiro Spawn: PLAUSIBLE PRESENCE — 30% base chance. Canonically Tanjiro fights in Yoshiwara during this arc. If present, he focuses on Daki while the player engages Gyutaro.

# **15. Arc IX — Swordsmith Village**

*Missions: Mission 14*

**Mission 14: Swordsmith Village  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Swordsmith Village — a hidden village of swordsmiths

**Context:** *Two Upper Moons infiltrate the swordsmith village simultaneously. The player defends alongside Tanjiro, Mitsuri, Genya, and Muichiro. Contribution to the boss fights determines reward quality.*

**Objectives**

- Defend the village from demon incursion — protect Villager entities in the village.
- Boss Fight: Defeat Gyokko (Upper Moon Five).
- Boss Fight: Defeat Hantengu (Upper Moon Four) — four forms, each must be destroyed.
- The higher the player's damage contribution to each boss, the higher the rewards.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — both bosses defeated with any contribution | Mission clears. Both UM5 and UM4 power vacuums fire. | Kill record +2 (one per boss, with Kizuki flag). Style mastery pulse proportional to contribution. Demon-path: BP from both bosses proportional to contribution. Power vacuums for UM5 and UM4. | Corps +50, Tanjiro +20, Demon +30 (AUTO scaled by contribution). |
| High contribution — player deals ≥40% total damage to both bosses | High-contribution flag set. | Kill record bonus +10. Permanent stat bonus: +3 distributed across lowest stats. Style mastery large pulse. | Corps +40 bonus on top of base. |
| Perfect village defense — no Villager entities die | Village entirely protected. | Swordsmith Village becomes a permanent safe fast-travel waypoint. Kill record bonus +5. | Corps +25, Tanjiro +15 (AUTO). |
| Muichiro memory carry-forward (if player completed Muichiro Hashira training early) | Muichiro fights with full power and memory from the start rather than recovering mid-fight. | Muichiro NPC is significantly more effective against Gyokko. No direct reward change — the fight is easier. | No additional rep. |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro is always present and fights Hantengu's forms. His effectiveness scales with his current party composition and accumulated affinity — at Friend+ tier he coordinates attacks with the player.

# **16. Arc X — Hashira Training**

*Missions: Missions 15–16*

**Mission 15: Hashira Training  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Randomized Training Grounds — each Hashira has a designated training zone

**Context:** *The Hashira Training arc. Each Hashira runs intensive sessions in their specialty. Completing all sessions provides a cumulative stat bonus. Giyu and Shinobu are not listed here — they are available as style masters but not part of the arc's structured training sequence.*

**Objectives**

- Complete a training session with each of the six listed Hashira.
- Each session is gated by the style master access conditions defined in the Reputation System §15.2.
- Sessions can be completed in any order.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Tengen Uzui — Sound Training | Tengen's session focuses on vibration awareness. The player learns to read the battlefield through sound rather than sight. | ⚠ NOT FINAL — Ability unlock (placeholder: 'Resonance' passive — after 3 seconds of stillness, nearby enemy positions pulse on the HUD). Style mastery pulse to Sound forms. Exact values and implementation subject to change. | Corps +12. |
| Muichiro Tokito — Speed Training | Muichiro's session is nearly wordless. He forces the player to react to attacks faster than conscious thought. | ⚠ NOT FINAL — Ability unlock (placeholder: 'Temporal Clarity' — brief bullet-time window on dodge while below 30% HP). Style mastery large pulse to Mist forms. Exact values and implementation subject to change. | Corps +12. |
| Mitsuri Kanroji — Flexibility Training | Mitsuri teaches extension of reach and fluid momentum. Her unique body composition allows attacks that should be impossible — the player learns to approximate this. | ⚠ NOT FINAL — Ability unlock (placeholder: 'Extended Reach' passive — extended melee hitbox and reduced attack recovery). Style mastery pulse to Love forms. Exact values and implementation subject to change. | Corps +12. |
| Obanai Iguro — Precision Training | Obanai teaches the serpentine movement philosophy: never be where the enemy expects. | ⚠ NOT FINAL — Ability unlock (placeholder: 'Serpentine Step' — dodge leaves an afterimage that draws one enemy attack). Style mastery pulse to Serpent forms. Exact values and implementation subject to change. | Corps +12. |
| Sanemi Shinazugawa — Endurance Training | Sanemi's training is brutal. He believes the only way to learn pain tolerance is through pain. | ⚠ NOT FINAL — Ability unlock (placeholder: 'Pain Threshold' passive — reduced stagger and increased damage at low HP). Style mastery large pulse to Wind forms. Exact values and implementation subject to change. | Corps +12. |
| Gyomei Himejima — Strength Training | Gyomei's training is spiritual as much as physical. He teaches that strength comes from absolute resolve. | ⚠ NOT FINAL — Ability unlock (placeholder: 'Stone Resolve' — temporary stance negating knockback with armor-piercing attacks). Style mastery large pulse to Stone forms. Exact values and implementation subject to change. | Corps +12. |
| Complete all 6 sessions (full circuit) | Full circuit bonus. | ⚠ NOT FINAL — Permanent stat bonus (placeholder: +3 to all base stats). Kill record bonus +20. Exact values subject to change. | Corps +40 bonus on complete of final session. |

Tanjiro Spawn: PLAUSIBLE PRESENCE — 10% base chance per training session. Tanjiro is undergoing his own training at this time. If present, he trains alongside the player and adds encouragement dialogue but provides no mechanical bonus.

**Mission 16: The Infinity Castle Summoning  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Ubuyashiki's Mansion, then the Infinity Castle entrance

**Context:** *The final confrontation begins. Muzan arrives at the Ubuyashiki estate. Kagaya Ubuyashiki detonates the estate in a desperate gambit. Tamayo injects Muzan with a drug that begins suppressing his regeneration. The Infinity Castle erupts around everything, pulling all Demon Slayers and demons into its infinite interior. The player must survive the collapse and reach the Castle's first interior zone.*

**Objectives**

- Survive the collapse of Ubuyashiki's Mansion (debris and shockwave phase — timed survival, 60 seconds).
- Witness the Tamayo injection cutscene (story beat — cannot be skipped on first playthrough).
- Survive the Infinity Castle summoning — the world folds inward. Navigate through the collapsing transition zone.
- Reach the first interior anchor point of the Infinity Castle to complete the mission.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — survive the collapse and reach the Castle interior | Mission clears. All Infinity Castle missions (17–22) unlock simultaneously. | No kill record bonuses (survival mission, not a combat mission). Kill record bonus +10 for reaching the interior without taking damage during the final navigation phase. | Corps +20, Tanjiro +10, Muzan –30 (the trap worked), Demon +10 (AUTO). |
| Tanjiro Affinity ≥ Best Friend (800) at this point | Tanjiro and the player enter the Castle together. Their entry point puts them closer to the Akaza fight (Mission 19). | Story access: Tanjiro remains alongside the player at the start of Mission 19 (Infinity Castle Akaza fight). No other mechanical change. | Tanjiro +10 (AUTO). |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro, Nezuko, and available party members are always present for the summoning. The party is scattered by the Castle's collapse — each member ends up near their respective Infinity Castle mission. Tanjiro's location afterward depends on affinity tier.

# **17. Arc XI — Infinity Castle**

*Missions: Missions 17–22*

The Infinity Castle arc missions unlock simultaneously after Mission 16. They represent parallel fights happening across the Castle at the same time. The player chooses which fight to assist and in what order. Missions 17–21 can be completed in any order. Mission 22 (Kokushibo) requires all of 17–21 to be cleared first.

Nemesis Integration for Infinity Castle Missions:

If canonKizuki = true, the Infinity Castle story demons ARE the Nemesis Kizuki occupants.

Killing them triggers standard power vacuums.

If canonKizuki = false OR the relevant Kizuki slot was already upset before Mission 16:

the current occupant of that slot (whatever procedural demon holds it) is substituted

as the boss. The fight uses that demon's DemonTrait stats, taunts, and behavior.

See §6 of this document and Nemesis System §21 for full details.

**Mission 17: Shinobu Kocho vs. Doma  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Infinity Castle — Upper Floors

**Context:** *Doma, Upper Moon Two, has consumed countless people including Kanae Kocho (Shinobu's older sister). Shinobu has spent her life preparing poison to kill him. In canon, she sacrifices herself by turning her own body into poison. The player can intervene to change her fate.*

**Objectives**

- Reach the Upper Floors of the Infinity Castle.
- Locate Shinobu's fight with Doma.
- Defeat Doma (Upper Moon Two).
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — player fights Doma directly | Doma is defeated. Mission 20 (Kanao/Inosuke vs Doma) is skipped. | Kill record +1 with UM2 Kizuki flag. Style mastery very large pulse. Demon-path: Doma's BP (UM2 value). UM2 power vacuum fires. | Corps +60, Muzan –50, Demon +60 (AUTO). |
| Player assists Shinobu instead of fighting Doma directly (Tanjiro Affinity ≥ Comrade) | Shinobu survives the encounter (non-canon). Doma is weakened but not dead — Mission 20 becomes available. | Kill record: no Doma kill here. Shinobu survives permanently. Story access: Mission 20 available. Kanao and Inosuke freed for other Castle fights. Tanjiro +30. | Corps +25, Tanjiro +30, Muzan –20 (AUTO). |

Tanjiro Spawn: NOT PRESENT in this fight. Tanjiro is occupied elsewhere in the Castle. Kanao and Inosuke are present as Shinobu's allies.

**Mission 18: Zenitsu Agatsuma vs. Kaigaku  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Infinity Castle — Middle Corridors

**Context:** *Kaigaku, the disgraced former student of the Thunder Hashira Jigoro Kuwajima, has become a demon and been elevated to Upper Moon in the reshuffled hierarchy. Zenitsu must face his former senior. The player assists.*

**Objectives**

- Reach the Middle Corridors of the Infinity Castle.
- Locate Zenitsu's confrontation with Kaigaku.
- Defeat Kaigaku.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| canonKizuki = true — Kaigaku holds his Nemesis slot | Kaigaku is whoever holds the relevant Upper Moon slot at mission time. If it is still Kaigaku (canon), fight proceeds as designed. If Kaigaku was already killed earlier, the slot's current occupant is substituted. | Kill record +1 with Kizuki flag. Style mastery large pulse. Power vacuum fires for Kaigaku's slot. Zenitsu dialogue: thanks the player if Tanjiro Affinity ≥ Acquaintance. | Corps +50, Muzan –40, Demon +50 (AUTO). |
| canonKizuki = false OR slot already upset — procedural demon substituted | A procedural demon occupying the relevant rank is the boss instead of Kaigaku. Zenitsu's dialogue is adjusted to acknowledge the unfamiliar demon. | All rewards identical — kill record, mastery, BP, power vacuum. | Same rep deltas. |

Tanjiro Spawn: NOT PRESENT in this fight. Zenitsu handles this confrontation personally. Tanjiro is elsewhere in the Castle.

**Mission 19: Tanjiro Kamado and Giyu Tomioka vs. Akaza  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Infinity Castle — Central Spire

**Context:** *Akaza, Upper Moon Three, confronts Tanjiro and Giyu. If the player defeated Akaza in Mission 11 (non-canon), whoever currently holds UM3 in the Nemesis hierarchy is the boss instead. If Akaza carried an EXTREME HATRED flag from Mission 11, this fight is significantly harder.*

**Objectives**

- Reach the Central Spire of the Infinity Castle.
- Locate Tanjiro and Giyu's fight with the UM3 occupant.
- Defeat Akaza (or the current UM3 occupant).
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — Akaza is the UM3 occupant (canon or not yet killed) | Standard fight alongside Tanjiro and Giyu. | Kill record +1 with UM3 Kizuki flag. Style mastery very large pulse. Demon-path: Akaza's BP. UM3 power vacuum fires. | Corps +60, Muzan –50, Demon +65 (AUTO). |
| Akaza carries EXTREME HATRED flag (from Mission 11 non-canon defeat) | Akaza is significantly more aggressive and uses expanded move patterns targeting this player specifically. | All rewards above PLUS extra ability unlock: 'Scar of Akaza' passive — the player's damage output increases by 8% permanently, representing the growth forged by surviving this encounter twice. | Demon Notoriety extra +20 if Akaza killed (AUTO). |
| Tanjiro Affinity ≥ Best Friend (800) carry-forward (from Mission 16) | Tanjiro enters the fight alongside the player from the start instead of arriving mid-fight. | Tanjiro NPC deals +25% damage during this mission. | Tanjiro +10 (AUTO). |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro is always present for this fight. His combat effectiveness scales with current party state and the affinity carry-forward from Mission 16.

**Mission 20: Kanao Tsuyuri and Inosuke Hashibira vs. Doma  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Infinity Castle — Upper Floors (same area as Mission 17)

**Context:** *This mission is only available if the player chose the 'assist Shinobu' branch in Mission 17, leaving Doma alive. Kanao uses a special drug derived from Tamayo's research to finally kill Doma. The player assists.*

**Objectives**

- Available only if Doma survived Mission 17.
- Return to the Upper Floors.
- Defeat Doma alongside Kanao and Inosuke.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Mission 17 standard clear (Doma already dead) | This mission does not exist. Automatically skipped. | N/A. | N/A. |
| Mission 17 Shinobu survival branch — Doma survived | Mission is available. Doma is weakened from Shinobu's poison. His HP is reduced by 30% at the start. | Kill record +1 with UM2 Kizuki flag. Style mastery large pulse. Demon-path: Doma's BP. UM2 power vacuum fires. | Corps +50, Muzan –40, Demon +50 (AUTO). |

Tanjiro Spawn: NOT PRESENT. Kanao and Inosuke lead this fight. Tanjiro is elsewhere in the Castle.

**Mission 21: Mitsuri Kanroji and Obanai Iguro vs. Nakime  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Infinity Castle — The Castle's Heart

**Context:** *Nakime is the Upper Moon Four occupant of the standard Kizuki hierarchy. She controls the Infinity Castle's structure — as long as she lives, the Castle cannot be escaped or navigated reliably. She must be killed to stabilize the environment. If canonKizuki = false or UM4 was already vacated, the current UM4 occupant substitutes as the boss. Her death fires the standard UM4 power vacuum.*

**Objectives**

- Reach the Castle's Heart.
- Assist Mitsuri and Obanai in defeating Nakime.
- Defeat Nakime (Upper Moon Four, Castle gatekeeper).
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — defeat Nakime | Mission clears. The Castle's structure begins to destabilize. Kokushibo fight becomes available. | Kill record +1 with UM4 Kizuki flag. Style mastery large pulse. Demon-path: Nakime's BP (UM4 value). Castle destabilizes — Mission 22 unlocks. | Corps +50, Muzan –40, Demon +50 (AUTO). |
| High contribution (player deals ≥50% of Nakime's total HP) | Mitsuri and Obanai acknowledge the player's strength. | Kill record bonus +10. Style mastery large additional pulse. | Corps +15 bonus. |

Tanjiro Spawn: NOT PRESENT. Mitsuri and Obanai handle this fight. Tanjiro is occupied with his own confrontation.

**Mission 22: Demon Slayers vs. Kokushibo  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** Infinity Castle — Upper Moon One's Chamber

**Context:** *Kokushibo, Upper Moon One and the most powerful demon beneath Muzan, was once a human — Michikatsu Tsugikuni, twin brother of the first Breath of the Sun user. He is the final obstacle before Muzan himself. Requires Missions 17–21 all cleared before this unlocks.*

**Objectives**

- Clear Missions 17–21 first.
- Reach Upper Moon One's Chamber.
- Defeat Kokushibo.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — defeat Kokushibo | Mission clears. UM1 power vacuum fires. All Upper Moon slots are now vacant after the full Infinity Castle sweep. | Kill record +1 with UM1 Kizuki flag. Style mastery enormous pulse (UM1 is the pinnacle of demon combat). Demon-path: Kokushibo's BP (UM1 value — highest fixed BP in the game). Ability unlock: 'Moon Echo' — a passive that makes the player's highest-damage style form deal 10% bonus damage permanently. | Corps +80, Tanjiro +30, Muzan –80, Demon +80 (AUTO). |
| Solo clear (no other entities deal damage) | Rare achievement — defeating UM1 alone. | All above rewards. Kill record bonus +50. Solo Kizuki flag (counts toward Hashira alt path if not already Hashira). | Demon Notoriety extra +30 (AUTO). |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro is always present for the Kokushibo fight alongside the other surviving Demon Slayers and Hashira. He is exhausted from his earlier fights but present and fighting. At Best Friend affinity (800+) he finds a second wind and deals standard NPC damage despite his injuries.

# **18. Arc XII — Sunrise Countdown**

*Missions: Missions 23–24*

**Mission 23: Demon Slayers vs. Muzan Kibutsuji  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** TBD — Muzan's final stand location

**Context:** *Muzan, weakened by Tamayo's drug and the loss of all his Upper Moons, makes his final stand. He must be killed before sunrise. If he survives until sunrise he is destroyed by the sun — but if the fight extends into the night again he regenerates. The Demon Slayers must overwhelm him before dawn. Requires Mission 22 cleared.*

**Objectives**

- Engage Muzan Kibutsuji.
- Deplete Muzan's HP bar before sunrise.
- If sunrise arrives before Muzan dies, he begins taking continuous sun damage — the fight is won.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Default — deplete Muzan's HP | Mission clears. Muzan is dead. Story continues to Mission 24. | Kill record +5 (Muzan counts as 5 kills — the magnitude of the achievement). Style mastery enormous pulse. Demon-path: Muzan's BP (maximum value in the game). Ability unlock: 'Dawn's Edge' passive — the player's first attack in any fight deals 20% bonus damage. | Corps +150, Tanjiro +50, Muzan –500 (hard line fires), Demon +50 (AUTO). |
| Demon Notoriety ≥ Cursed Name (850) | Muzan acknowledges the player in opening dialogue. | Unique entry added to the story log: Muzan's acknowledgment quote. Taunt pool gains a permanent entry. | No additional rep. |
| Tanjiro Affinity ≥ Best Friend (800) | Tanjiro fights at full capacity with no holding back. | Tanjiro NPC deals +25% damage throughout this mission. | No additional rep. |

Tanjiro Spawn: CANONICAL PRESENCE — Tanjiro and all surviving party members are always present for this fight. The entire Demon Slayer Corps converges here. Party effectiveness scales with who survived the Infinity Castle arc.

**Mission 24: Demon Slayers vs. Demon King Tanjiro Kamado  ⚠ ROUGH OUTLINE — NOT FINALIZED**

**Location:** TBD — same location as Mission 23

**Context:** *Muzan's death is not clean. In his final moments he pours all of his blood into Tanjiro, attempting to continue his existence through him. Tanjiro becomes a demon — and unlike other demons, retains his memories and identity, making him devastatingly powerful. The remaining Slayers must either kill him or find a way to cure him.*

**Objectives**

- Engage Demon King Tanjiro Kamado.
- Choose your path: attempt the cure (requires Tanjiro Affinity ≥ Best Friend) or deplete his HP.
**Branches**

| Condition | Outcome | Rewards / Consequences | Rep (see Rep. Doc §11) |
| --- | --- | --- | --- |
| Tanjiro Affinity ≥ Best Friend (800) — Canon Victory: cure Tanjiro | The accumulated bond between the player and Tanjiro, combined with Kanao's Wisteria drug, restores him. Canon ending. | Tanjiro is cured and becomes human again. Permanent ally unlocked: Tanjiro can be summoned once per in-game day to assist in any non-Nemesis fight. Kill record bonus +20 (the hardest fight of the game, resolved without killing). Ability unlock: 'Bond of the Blade' — Tanjiro's summon cooldown is halved. | Corps +60, Tanjiro +80 (largest single Tanjiro delta), Suspicion –100 (AUTO). |
| Tanjiro Affinity < Best Friend (800) — Non-Canon: defeat Demon Tanjiro | The cure path is unavailable. Tanjiro must be killed. | Kill record +1. Style mastery enormous pulse. No permanent ally. Story ends on a darker note. | Corps +40, Tanjiro rep: irrelevant (he is dead) (AUTO). |
| Tanjiro Affinity < Stranger (–100) — Enemy path | Demon Tanjiro specifically targets this player — he has emotional memories of their conflict. | Demon Tanjiro deals +20% damage to this player throughout the fight. All other outcomes same as non-canon path. | Same as non-canon rep deltas. |

Tanjiro Spawn: THIS MISSION IS THE TANJIRO FIGHT. He is the boss. No spawn mechanics apply. All surviving party members are present as background characters reacting to events — none assist in combat.

# **19. Mission Number Reference**
Quick reference table for all missions in final numbering order.

| # | Name | Arc | Finalized? |
| --- | --- | --- | --- |
| 0 | Final Selection | I - Final Selection | Yes |
| 1 | Kidnapper's Bog | II - Kidnapper's Bog | Yes |
| 2 | Asakusa | III - Asakusa | Yes |
| 3 | Tamayo | III - Asakusa | Yes |
| 4 | Tsuzumi Mansion | IV - Tsuzumi Mansion | Yes |
| 5 | Mt. Natagumo | V - Mount Natagumo | Yes |
| 6 | Mother's Doll | V - Mount Natagumo | Yes |
| 7 | Spider Family | V - Mount Natagumo | Yes |
| 8 | The Traitor | V - Mount Natagumo | Yes |
| 9 | The Lower Moon | VI - Rehabilitation Training | Rough Outline |
| 10 | Mugen Train | VII - Mugen Train | Rough Outline |
| 11 | Akaza | VII - Mugen Train | Rough Outline |
| 12 | Entertainment District | VIII - Entertainment District | Rough Outline |
| 13 | Daki and Gyutaro | VIII - Entertainment District | Rough Outline |
| 14 | Swordsmith Village | IX - Swordsmith Village | Rough Outline |
| 15 | Hashira Training | X - Hashira Training | Rough Outline |
| 16 | The Infinity Castle Summoning | X - Hashira Training | Rough Outline |
| 17 | Shinobu Kocho vs. Doma | XI - Infinity Castle | Rough Outline |
| 18 | Zenitsu Agatsuma vs. Kaigaku | XI - Infinity Castle | Rough Outline |
| 19 | Tanjiro and Giyu vs. Akaza | XI - Infinity Castle | Rough Outline |
| 20 | Kanao and Inosuke vs. Doma | XI - Infinity Castle | Rough Outline |
| 21 | Mitsuri and Obanai vs. Nakime | XI - Infinity Castle | Rough Outline |
| 22 | Demon Slayers vs. Kokushibo | XI - Infinity Castle | Rough Outline |
| 23 | Demon Slayers vs. Muzan | XII - Sunrise Countdown | Rough Outline |
| 24 | Demon Slayers vs. Demon Tanjiro | XII - Sunrise Countdown | Rough Outline |

*End of Document — Missions System v1.0*
