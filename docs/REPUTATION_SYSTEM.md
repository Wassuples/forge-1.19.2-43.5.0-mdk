**REPUTATION & NOTORIETY SYSTEM**

*Complete Technical Reference — Revision 2*

com.wassuples.demonslayer.reputation

Minecraft Forge 1.19.2  ·  Demon Slayer Mod

# **Table of Contents**
- [1. Overview & Design Philosophy](#1-overview-design-philosophy)
- [2. Architecture & Data Model](#2-architecture-data-model)
- [3. Rank vs. Reputation - The Hard Separation](#3-rank-vs-reputation-the-hard-separation)
- [4. Demon Slayer Rank System](#4-demon-slayer-rank-system)
- [5. The Four Reputation Dimensions](#5-the-four-reputation-dimensions)
- [6. Reputation Tiers (Positive & Negative)](#6-reputation-tiers-positive-negative)
- [7. Faction Hard Lines - Permanent Blacklists](#7-faction-hard-lines-permanent-blacklists)
- [8. The Witness System](#8-the-witness-system)
- [9. Active Concealment](#9-active-concealment)
- [10. Witness Chance Modifier Table](#10-witness-chance-modifier-table)
- [11. Reputation Events Table](#11-reputation-events-table)
- [12. Tension, Flow State & Pacing](#12-tension-flow-state-pacing)
- [13. Integration - Nemesis System](#13-integration-nemesis-system)
- [14. Integration - Mission Branching (Full Spec)](#14-integration-mission-branching-full-spec)
- [15. Integration - Style Masters & Minigames](#15-integration-style-masters-minigames)
- [16. Integration - Persuasion System](#16-integration-persuasion-system)
- [17. Persistence & Serialization](#17-persistence-serialization)
- [18. Commands](#18-commands)
- [19. Class Reference Map](#19-class-reference-map)

# **1. Overview & Design Philosophy**
The Reputation System is the world's memory of the player. It is a four-dimensional record — Corps, Tanjiro, Muzan, Demons — each measuring how a distinct faction interprets the player's actions. Reputation is entirely separate from Demon Slayer Rank, which is a purely mechanical achievement ladder. A Hashira can be despised by the Corps. A Mizunoto can be feared by every demon in the hierarchy.

The system's central fantasy is the Double Agent: someone who plays all four factions simultaneously by controlling what each one knows. This is achievable but demands active, skilled management of the witness system, concealment tools, and atmospheric conditions. The design respects the Flow state — early game is low-stakes and exploratory, pressure builds naturally as the player's reputation grows into ranges that create contradiction.

Core Design Pillars:

- Rank and Reputation are separate systems. Never conflate them.

- Every dimension supports a full negative range. Being unknown is not the floor.

- Actions only reach a faction's ledger if a witness belonging to that faction reports them.

- Concealment is always available but always costly in some form.

- Demon-path players cannot hide anything from Muzan — the blood bond is absolute.

- Some actions permanently destroy a faction relationship. These are hard to do accidentally.

- Rewards are purely mechanical: stats, access, abilities, story branches. No cosmetics.

# **2. Architecture & Data Model**
## **World Singleton: ReputationManager (extends SavedData)**
Saved to reputation.dat in the overworld data directory. Entry point: ReputationManager.get(ServerLevel). Loaded on world open, saved on dirty flag.

**Class / Field**

| Class / Field | Type | Description |
| --- | --- | --- |
| ReputationManager | SavedData | World-saved singleton. Owns all player records and witness queue. |
| playerRecords | Map<UUID, PlayerRepRecord> | All player reputation data keyed by UUID. |
| witnessEventQueue | Queue<WitnessEvent> | Pending witness reports processed on next daily tick. |
| hardlineFlags | Map<UUID, Set<HardlineFlag>> | Permanent blacklist flags per player. Never cleared. |

## **Per-Player Record: PlayerRepRecord**

| Field | Type | Description |
| --- | --- | --- |
| corpsRep | int (-500 to 1000) | Standing with DSHQ. Negative = disgraced, hunted. |
| tanjiroRep | int (-500 to 1000) | Affinity with Tanjiro and crew. Negative = actively avoided/combated. |
| muzanRep | int (-500 to 1000) | Muzan's regard. Negative = he directs Kizuki to hunt the player. |
| demonRep | int (-500 to 1000) | Demon Notoriety. Negative = known weakling, bullied. |
| suspicion | int (0-500) | Corps internal suspicion sub-meter. Hidden from player. |
| slayerRank | SlayerRank | Current official Demon Slayer rank (separate from corpsRep). |
| killRecord | KillRecord | Kill counts by category, tracked per-rank for advancement gating. |
| recentSins | List<SinRecord> | Unconfirmed sins (max 50). Eligible for 14-day fade. |
| hardlineFlags | Set<HardlineFlag> | Permanent blacklist flags. Copied from ReputationManager. |

## **Supporting Types**
| Class | Package | Purpose |
| --- | --- | --- |
| WitnessEvent | reputation.witness | Immutable record of a witnessed action and its reporting chain. |
| WitnessType | reputation.witness | Enum: CROW, VILLAGER, DEMON_SLAYER, DEMON_ALIVE, DEMON_DYING. |
| WitnessScanner | reputation.witness | Stateless - scans entities near a BlockPos for active witnesses. |
| SuppressionEntry | reputation.concealment | Active suppression effect: strength, target type, expiry tick. |
| SinRecord | reputation.concealment | Unconfirmed sin entry eligible for natural 14-day fade. |
| HardlineFlag | reputation | Enum of all permanent blacklist conditions. Set, never cleared. |
| KillRecord | reputation.rank | Per-category kill counters used for rank advancement checks. |
| SlayerRank | reputation.rank | Enum: MIZUNOTO through HASHIRA. Separate from corpsRep. |
| RepEventType | reputation | Enum of all reputation-affecting action types (50+ entries). |
| RepDeltaCalculator | reputation | Stateless - computes dimension deltas for a given RepEventType. |
| CorpsSuspicionManager | reputation.corps | Manages suspicion sub-meter and Investigation event triggers. |
| Faction | reputation | Enum: DSHQ, TANJIRO_CREW, MUZAN, DEMON_WORLD. |

# **3. Rank vs. Reputation — The Hard Separation**
These are two completely independent systems that happen to share the same player. They never directly modify each other. Rank is a kill-based achievement system. Reputation is a relationship and social record. A player who has killed 1000 demons as a Kinoe is a Hashira regardless of whether the Corps despises them. A player with maximum Corps Rep is not automatically a higher rank.

**Property**

| Property | Demon Slayer Rank | Reputation Dimensions |
| --- | --- | --- |
| What it measures | Combat achievement: kill counts and feats | Faction relationships: how each group views the player |
| How it changes | Kill count thresholds + specific combat feats | Witnessed actions reported by faction-aligned entities |
| Can it go negative? | No - floor is Mizunoto | Yes - all four dimensions support negative ranges |
| Is it hidden? | No - rank is always visible to the player | Partially - suspicion is hidden; Muzan tier is vague |
| Can it be lost? | No - rank is permanent once earned | Yes - rep changes constantly based on actions |
| Affects each other? | Rank grants access; rep changes NPC dialogue | Rep never directly changes rank, only social outcomes |

Key consequence of separation:

A player disgraced by the Corps (corpsRep –300) who is being hunted by Hashira

can still be a Hashira themselves. The Corps cannot strip rank — it is earned through

blood. They can exile, hunt, and refuse to acknowledge a Hashira-ranked player,

but the rank itself does not decrease.

# **4. Demon Slayer Rank System**
Rank advancement is gated exclusively on kill counts achieved while holding the current rank. The only exception is Hashira, which adds an alternate feat-based path. Corps Reputation has no bearing on rank advancement.

**Rank**

| Rank | Kills Required (at current rank) | Alternate Path | Notes |
| --- | --- | --- | --- |
| Mizunoto | 0 (starting rank) | None | All players begin here after completing Final Selection. |
| Mizunoe | 10 demons killed | None | First advancement. Unlocks basic Corps mission pool. |
| Kanoto | 30 demons killed | None | - |
| Kanoe | 60 demons killed | None | - |
| Tsuchinoto | 100 demons killed | None | - |
| Tsuchinoe | 150 demons killed | None | Mid-rank. Wider mission access. |
   Hinoto  250 demons killed  None  - 
   Hinoe  300 demons killed  None  - 
   Kinoto  400 demons killed  None  - 
   Kinoe  500 demons killed  None  Prerequisite rank to become Hashira. 
| Hashira | 500 demons killed as Kinoe | Solo kill any Kizuki moon demon | The alternative path (Kizuki solo kill) can be done at any rank, but rank is still set to Hashira. |

## **KillRecord Tracking**
Kill counts are tracked per-rank. Kills made as Mizunote do not carry forward to count toward Kanoe requirements. Each rank's counter resets when the player advances. This prevents stockpiling kills before ranking up. The KillRecord stores:

- killsAtCurrentRank — count of demon kills since last rank advance
- kizukiSoloKills — lifetime count of solo Kizuki kills (used for Hashira alt path)
- totalDemonKills — lifetime total (used for taunt content and story flavor)
- killsByCategory — Map<DemonCategory, Integer> for mission/story tracking

Rank advancement is automatic — no confirmation screen, no trial mission.

When killsAtCurrentRank crosses the threshold, the rank is set immediately and a

notification is sent to the player. The Corps acknowledges advancement passively

through NPC dialogue shifts — there is no formal ceremony.

For the Hashira alternate path: a Kizuki solo kill is detected when the player

deals the killing blow to a moon-rank Nemesis demon and no other entity dealt

damage to that demon during the current BattleSession. This uses the existing

BattleSession.interferers check from the Nemesis system.

# **5. The Four Reputation Dimensions**
## **5.1  Corps Reputation**
Measures how DSHQ perceives the player as an agent, a soldier, and a person. High Corps Rep opens mission pools, changes how Hashira and NPCs speak to the player, and reduces the baseline rate at which Suspicion accumulates. Negative Corps Rep means the player is disgraced — Hashira are dispatched to bring them in or cut them down.

Corps Rep is also the only dimension the player can see a named tier for on demand — by speaking to Corps-aligned NPCs who will tell the player where they stand. The hidden Suspicion sub-meter (0–500) runs in parallel and is never shown directly.

## **5.2  Tanjiro Affinity**
Measures Tanjiro's personal trust in the player. It does not mirror Corps Rep. Tanjiro fights beside anyone in danger regardless of affinity tier — his character demands it. What affinity changes is how far he is willing to deviate from the anime's main plot on the player's behalf: sharing restricted information, hiding evidence, altering his own plans around the player's actions, and unlocking mission branches that do not exist in the base story.

Negative affinity means Tanjiro actively avoids the player. At deep negative he treats the player as an obstacle or threat and will engage them in combat if they interfere with his mission. He never permanently turns hostile unless the player kills a crew member — but at low affinity his combat assistance vanishes and his branching cooperation closes entirely.

## **5.3  Muzan's Favor**
Measures how Muzan regards the player. Positive favor does not mean Muzan calls off demon hunts — the Nemesis system's hunters are acting on their own motivations (territory, rivalry, ambition). Muzan actively wants demons fighting each other and resists allowing any demon to grow too powerful by making alliances. What positive Muzan favor does is: reduce the probability he personally intervenes, unlock demon-path content, and occasionally cause a Kizuki to pause before acting against the player.

Negative Muzan favor is where his power becomes direct. At sufficiently negative tiers, Muzan issues personal directives — he dispatches specific Kizuki to hunt the player, functioning identically to how he dispatched hunters against Tanjiro in the source material. The more negative the favor, the higher-ranked the Kizuki he sends. This is separate from and on top of the standard Nemesis hunter spawning.

Demon-path players (those who have drunk Muzan's blood) cannot conceal anything from Muzan. Every action they take is transmitted through the blood bond. There is no witness mechanic available for this channel.

## **5.4  Demon Notoriety**
Measures how the demon world perceives the player as a combatant. Positive notoriety means the player is known, feared, and respected — cowardly demons flee, ambitious ones seek the challenge, and demon taunts reference the player's deeds by name. Negative notoriety means the player is known as weak — a soft target. Cowardly demons who would normally flee now attack, while powerful duelist-personality demons consider the player beneath their attention entirely.

Demon Notoriety feeds directly into the Nemesis System's spawn chance and behavior override formulas. It is the most mechanically consequential dimension in combat.

# **6. Reputation Tiers (Positive & Negative)**
*Score range is –500 to 1000 for all dimensions. Tier names are inferred by the player through dialogue, taunts, and NPC behavior — they are never shown in a UI panel.*

## **6.1  Corps Reputation Tiers**
| Tier Name | Score Range | World Effect |
| --- | --- | --- |
| **Traitor** | -500 to -301 | Hunted on sight by Hashira. All Corps NPCs are hostile. Effectively exiled from any Corps-aligned location. |
| **Disgraced** | -300 to -101 | Corps NPCs refuse service. Hashira are cold and hostile. No missions available. Suspicion has no meaning at this range - the Corps already knows. |
| **Suspicious** | -100 to -1 | Corps NPCs are guarded. Rumors circulate. Mission pool is restricted to the lowest tier. |
| **Unknown** | 0 | Starting state. Corps is neutral and professional. Standard mission pool. |
| **Recognized** | 1-199 | Corps NPCs acknowledge the player positively. Wider mission pool. |
| **Trusted** | 200-399 | Hashira speak warmly. Most mission tiers available. |
| **Honored** | 400-599 | Senior Corps figures consult the player. All missions available. |
| **Revered** | 600-799 | Full Hashira training access. NPCs defer to the player. Suspicion growth rate halved. |
| **Pillar** | 800-1000 | The player is treated as a foundational member of the Corps. Maximum mission rewards. Ubuyashiki may acknowledge the player. |

## **6.2  Tanjiro Affinity Tiers**
| Tier Name | Score Range | World Effect |
| --- | --- | --- |
| **Enemy** | -500 to -301 | Tanjiro actively works against the player. He will engage them in combat if their missions conflict. |
| **Obstacle** | -300 to -101 | Tanjiro avoids the player. Refuses dialogue beyond mission necessity. No branching cooperation available. |
| **Stranger** | -100 to 99 | Tanjiro is neutral and polite. He fights alongside the player in combat because that is his nature - but no story branches open. |
| **Acquaintance** | 100-249 | Tanjiro greets the player and shares minor observations. Earliest branching paths crack open. |
| **Comrade** | 250-449 | Tanjiro treats the player as a trusted ally in the field. Mid-tier branching available. He may share restricted intel. |
| **Friend** | 450-649 | Tanjiro will bend rules for the player. He hides evidence, alters his plans around player choices. Deep branches available. |
| **Trusted** | 650-799 | Tanjiro vouches for the player to Hashira. He can suppress up to 100 Corps Suspicion on the player's behalf. |
| **Best Friend** | 800-1000 | Tanjiro will act against the Corps' direct orders to protect the player. Suppresses up to 200 Suspicion. All non-canon branches available. |

## **6.3  Muzan Favor Tiers**
Muzan's tier name is never shown to the player. They infer it through demon behavior, scripted story moments, and the frequency of Kizuki dispatches.

| Tier Name | Score Range | World Effect |
| --- | --- | --- |
| **Marked for Death** | -500 to -301 | Muzan dispatches Upper Moon Kizuki (UM4 or higher) personally to hunt the player. This is separate from Nemesis hunter spawning and cannot be stopped by any reputation action. |
| **Hunted** | -300 to -101 | Muzan dispatches Lower Moon or UM6/UM5 Kizuki periodically. Demon-world NPCs are hostile. |
| **Irrelevant** | -100 to 99 | Muzan is indifferent. No personal interventions. Standard Nemesis behavior. |
| **Noticed** | 100-249 | Muzan is aware of the player. Occasional Kizuki hesitate before attacking. No direct benefits. |
| **Observed** | 250-449 | Muzan watches with interest. For demon-path players: minor Blood Demon Arts upgrade access. |
| **Regarded** | 450-649 | A Kizuki may pause and observe rather than immediately attack. Corps Suspicion rises +3/day passively. |
| **Favored** | 650-799 | Demon-path players: full BDA upgrade access. Corps Suspicion +5/day. Human-path: near-certain Corps Investigation. |
| **Chosen** | 800-1000 | Scripted Muzan acknowledgment event triggers. Demon-path players gain unique abilities. Human-path: Corps Investigation triggers immediately regardless of current Suspicion. |

## **6.4  Demon Notoriety Tiers**
| Tier Name | Score Range | World Effect |
| --- | --- | --- |
| **Prey** | -500 to -301 | Demons regard the player as easy food. All demon personalities that would normally avoid combat now attack. Duelist/Berserker demons ignore the player entirely - beneath their notice. |
| **Weakling** | -300 to -101 | Cowardly and Scavenger demons attack instead of fleeing. Taunts are mocking and dismissive. |
| **Unknown** | -100 to 99 | Neutral. Standard Nemesis behavior. No taunt modifications. |
| **Spotted** | 100-199 | Some demons carry a description of the player. Fodder spawn chance slightly increased. |
| **Marked** | 200-349 | Lower Moons are aware of the player. Hunter spawn +15%. Taunts reference a specific past kill. |
| **Feared** | 350-499 | Cowardly and Scavenger demons begin fleeing on sight. Hunter spawn +30%. Upper Moon awareness begins. |
| **Hunted** | 500-699 | Upper Moons dispatch hunters. Spawn +50%. Taunts reference kill count by name. |
| **Legendary** | 700-849 | All Kizuki eligible to dispatch. Spawn +75%. Some Lower Moons refuse engagement entirely. |
| **Cursed Name** | 850-1000 | Maximum aggression from ambitious demons. Cowardly demons always flee. Taunts are runtime-composed referencing full kill history. |

# **7. Faction Hard Lines — Permanent Blacklists**
Hard line violations are stored as HardlineFlag enum values in the player's record. They are never cleared under any circumstance — no redemption arc, no quest, no command exists to reverse them in normal gameplay. Because they are permanent, they must be extremely difficult to trigger accidentally. Each is gated behind a multi-step confirmation or requires a deliberate, sustained action.

## **7.1  DSHQ Hard Lines**
PERMANENT BLACKLIST: The Corps will hunt this player forever.

Hashira are dispatched on sight. Corps Rep is set to -500 and locked there.

No missions, no training, no NPC service from any Corps-aligned entity.

| Action | How it is Gated | Detection Requirement |
| --- | --- | --- |
| Killing a Hashira NPC | Hashira are the only entities tagged as HardlineProtected for DSHQ. The game requires a second confirmation hit after the Hashira enters a 'last stand' wounded state - a single hit cannot trigger this. | Any witness (CROW, DEMON_SLAYER, VILLAGER) present, OR the death of a Hashira NPC is always auto-reported to DSHQ regardless of witnesses - Hashira deaths are never unwitnessed from the Corps' perspective. |
| Knowingly aiding Muzan or a Kizuki escape | Triggered only if the player actively shields or buffs a Kizuki during a Boss encounter and the Kizuki then survives. Passive proximity is not enough - the player must deal damage to Corps-allied entities protecting the fight, or use an ability that explicitly buffs the Kizuki target. | Auto-reported. The mission system logs the aiding action directly. |

## **7.2  Tanjiro Crew Hard Lines**
PERMANENT BLACKLIST: Tanjiro treats the player as an enemy forever.

Tanjiro affinity is set to -500 and locked. He will engage the player in combat

if their paths cross. His crew follows his lead.

| Action | How it is Gated | Detection Requirement |
| --- | --- | --- |
| Killing any named crew member (Tanjiro, Nezuko, Zenitsu, Inosuke, Kanao, Genya) | Named crew members have a HardlineProtected tag for the Tanjiro faction. They enter a 'downed' state before death — the player must deliver a second deliberate killing blow while they are downed. Accidentally hitting a downed crew member in AoE does not trigger this. | Auto-reported. Tanjiro always knows if a crew member was killed, regardless of witnesses. |

## **7.3  Muzan Hard Lines**
PERMANENT BLACKLIST: Muzan issues a standing kill order.

muzanRep is set to -500 and locked. He permanently dispatches Kizuki

at the highest available tier on a recurring schedule that never stops.

| Action | How it is Gated | Detection Requirement |
| --- | --- | --- |
| Delivering Tamayo's medicine to the Corps (completing the cure research chain) | Requires the player to have obtained Tamayo's medicine item, traveled to a Corps HQ location, and interacted with a specific Corps Research NPC to hand it over. Each step is a deliberate, multi-stage action with no accidental trigger path. | Auto-reported. Muzan detects the delivery through the blood network (the delivery location is a Corps stronghold where demons have previously been present). For demon-path players this is also an immediate death sentence via blood bond override. |

## **7.4  Demon World Hard Lines**
Demons have no hard line.

They respect power above all else. Any betrayal, any allegiance — all of it

can be recovered from in the demon world given sufficient strength and deeds.

Even killing a Kizuki demon is not unforgivable: it proves strength.

# **8. The Witness System**
An action only reaches a faction's ledger if at least one un-suppressed witness belonging to that faction is present when the act completes. The witness system performs a scan at the moment of the action, classifies nearby entities by WitnessType, applies all active modifiers, and determines which factions receive a WitnessEvent.

## **8.1  Witness Types**
| Type | Entity | Reports To | Reporting Behavior |
| --- | --- | --- | --- |
| CROW | Player's Kasugai Crow | DSHQ | Reports instantly on act completion. Can be killed - new crow assigned on next sleep. Can be blinded by Blindness/Darkness potion effect (Vanilla). Observation radius: 32 blocks standard, 8 blocks if perched. |
| VILLAGER | Vanilla Minecraft Villager entities in range | DSHQ | Files report at next daily simulation tick. Blinded by Blindness/Darkness. Uses full proximity and atmosphere modifier table. |
| DEMON_SLAYER | Demon Slayer NPC entities in range | DSHQ | Reports instantly on return to any settlement. Blinded by Blindness/Darkness. |
| DEMON_ALIVE | Any surviving Nemesis demon | Demon Notoriety (daily) + Muzan if it later dies | Carries memory through Nemesis intel network. Death later does not retroactively suppress the report - the memory was already propagated. |
| DEMON_DYING | Any Nemesis demon killed during the encounter | Muzan only (instant, at death) | Final vision transmitted to Muzan at moment of death regardless of any suppression. Cannot be blocked by any concealment method. For demon-path players this channel is always open. |

## **8.2  The Crow — Special Rules**
- The Crow can be killed by the player or by accident. A new crow is assigned during the next sleep cycle.
- Killing a crow intentionally generates a Suspicion spike (see §10 for exact values by method).
- The Crow cannot be dismissed — it always follows the player unless perched.
- Perching (Shift+Right-click): observation radius drops from 32 to 8 blocks. The Crow does not leave the perch point until the player Shift+Right-clicks again.
- The Crow CAN be affected by Blindness and Darkness Vanilla potion effects. While blinded, it does not generate WitnessEvents regardless of what it is near.
- The Crow reports to DSHQ instantly — there is no daily tick delay. Blinding or killing it before an act is the only defense against its report.

Strategic Crow Management:

The safest methods for avoiding Crow reporting are: (1) apply Blindness to it before acting,

(2) perch it and stay beyond 8 blocks. Killing it is an option but generates +40 Suspicion

if another DSHQ witness is present, or +20 if unwitnessed. Dumping it into lava or off a

cliff registers as an unwitnessed death (no Suspicion) only if no DEMON_SLAYER or VILLAGER

is within witness range at the time of death.

# **9. Active Concealment**
Concealment is deliberately imperfect. Every method has a cost, a failure mode, or a situational limit. Perfect concealment is possible but requires stacking multiple methods appropriately.

## **9.1  Blinding Witnesses**
Any witness entity affected by the Vanilla Blindness or Darkness potion effect is fully suppressed - they generate no WitnessEvent for acts committed while they are blinded, regardless of proximity. This includes the Crow. The player must apply the effect before committing the act; a witness blinded after an act has already been processed provides no retroactive suppression.

| Method | Effect | Duration | Notes |
| --- | --- | --- | --- |
| Throw Splash Potion of Blindness (Vanilla) | Full suppression on affected entities | 20-40s (standard potion) | Area of effect. Can hit multiple witnesses. Crow is a valid target. |
| Throw Splash Potion of Darkness (Vanilla) | Full suppression on affected entities | 20-40s | Shorter range than Blindness but harder to detect by other witnesses. |
| Mist Breathing: Obscuring Clouds technique | Applies Blindness to all non-player entities within 12 blocks | 8s per activation | Costs breathing stamina. Does not last long - plan timing carefully. |

## **9.2  Dialogue Concealment**
After a witnessable act, a time window (default 300 ticks / 15 seconds) opens during which the player can approach a VILLAGER or DEMON_SLAYER witness and attempt dialogue concealment. Available options and success rates depend on the player's current Demon Slayer Rank and the persuasion system (see §16).

| Method | Base Success | Cost | Failure Consequence |
| --- | --- | --- | --- |
| Bribery - standard items | 50% | Mid-value item stack | Witness reports + notes the bribery attempt (+15 Suspicion) |
| Bribery - high-value items | 85% | High-value item stack | Witness still reports, bribery attempt logged (+10 Suspicion) |
| Intimidation | 60% | None | Witness reports AND becomes hostile. Files assault report (+25 Suspicion) |
| Rank Persuasion | Rank-dependent (see §16) | None (resource-free) | Witness becomes skeptical - lower success on future persuasion attempts from this player |

## **9.3  Witness Elimination**
Killing a VILLAGER or DEMON_SLAYER witness before the daily tick prevents their report — but killing them is itself a witnessable act. If any other witness observes the killing, the murder is reported with its own severe rep consequences. Eliminating witnesses only works cleanly in truly isolated environments.

The Murder Loop:

Killing a witness generates a new WitnessEvent for the killing act itself.

If the Crow is alive and within 32 blocks, it witnesses the murder.

If another Villager or Demon Slayer is in range, they witness it too.

The only clean elimination is: Crow blinded or perched beyond 8 blocks,

no other DSHQ witnesses in 48-block radius.

# **10. Witness Chance Modifier Table**
All modifiers are multiplicative unless noted as additive (+/–). They apply to the base witness detection chance for each WitnessType during a scan. Modifiers stack.

## **10.1  Breathing Style Modifiers**
| Breathing Style | Modifier | Affects | Reason |
| --- | --- | --- | --- |
| Mist Breathing (active) | x0.70 (-30%) | All non-Crow witnesses | Movement blur and mist obscure the player's visual signature. |
| Thunder Breathing (active) | x1.25 (+25%) | All non-Crow witnesses | Lightning visual and sound draw immediate attention at long range. |
| Insect Breathing (active) | x0.80 (-20%) | All non-Crow witnesses | Small, precise motions - minimal visual signature. |
| Flame Breathing (active) | x1.15 (+15%) | All witnesses including Crow | Fire and smoke visible from extreme distances, even to the Crow. |
| Wind Breathing (active) | x0.88 (-12%) | VILLAGER only | Wind disrupts visual tracking for civilians but not trained Slayers. |
| Water Breathing (active) | x1.00 | No effect | Neutral visual signature. |
| Stone Breathing (active) | x0.92 (-8%) | VILLAGER only | Low-profile, ground-level movement. |
| Sound Breathing (active) | x1.20 (+20%) | All non-Crow witnesses | Sound-based attacks are heard at long range, drawing witnesses. |
| Love Breathing (active) | x1.10 (+10%) | VILLAGER only | Showy, flamboyant movements attract civilian attention. |
| Serpent Breathing (active) | x0.85 (-15%) | DEMON_SLAYER only | Serpentine movement makes it difficult for trained observers to track. |

## **10.2  Time of Day Modifiers**
| Condition | Modifier | Affects |
| --- | --- | --- |
| Daytime (full daylight) | x1.00 baseline | All types |
| Dusk / Dawn (transition) | x0.85 | VILLAGER only (transitioning indoors) |
| Night (standard) | x0.60 | VILLAGER (indoors, not patrolling); no effect on DEMON_ALIVE |
| Night (standard) | x1.20 | DEMON_ALIVE (demons are more alert at night) |
| Deep night (past midnight) | x0.45 | VILLAGER; most are asleep |
| Deep night (past midnight) | x1.35 | DEMON_ALIVE (peak demon awareness) |

## **10.3  Moon Phase Modifiers**
Moon phase affects all witness types. Full moon is a dangerous night to commit witnessed acts against the Corps — and the safest night for acts against demons, who are emboldened enough to be reckless and careless.

| Moon Phase | VILLAGER/DEMON_SLAYER Modifier | DEMON_ALIVE Modifier | Notes |
| --- | --- | --- | --- |
| New Moon | x0.75 | x0.60 | Dark night. Civilians stay in. Demons are weakened and less alert. |
| Crescent | x0.85 | x0.80 | - |
| Half Moon | x1.00 | x1.00 | Baseline. |
| Gibbous | x1.10 | x1.15 | - |
| Full Moon | x0.80 | x1.50 | Civilians fear the night and stay indoors. Demons are hyperaware and alert. |

## **10.4  Lighting & Environment Modifiers**
| Condition | Modifier | Affects |
| --- | --- | --- |
| Light level ≤ 4 (dark room / cave) | x0.50 | VILLAGER, DEMON_SLAYER |
| Light level 5–8 | x0.75 | VILLAGER, DEMON_SLAYER |
| Light level ≥ 9 (standard) | x1.00 | Baseline |
| Rain / Thunderstorm | x0.70 | VILLAGER, DEMON_SLAYER (sound masked, visibility reduced) |
| Fog biome (e.g. dark forest) | x0.80 | VILLAGER, DEMON_SLAYER |
| Underground / cave | x0.40 | VILLAGER (almost no civilians underground) |
| Town square / settlement center | x1.20 | VILLAGER, DEMON_SLAYER (high population density) |
| Dense canopy / jungle | x0.75 | VILLAGER, DEMON_SLAYER (sight lines blocked) |
| Open plains / desert | x1.15 | VILLAGER, DEMON_SLAYER (no cover) |
| Snowstorm / blizzard | x0.60 | VILLAGER, DEMON_SLAYER |

## **10.5  Player State Modifiers**
| Condition | Modifier | Affects |
| --- | --- | --- |
| Player is crouching (sneaking) | x0.70 | VILLAGER, DEMON_SLAYER |
| Player has Invisibility effect | x0.00 (immune) | VILLAGER, DEMON_SLAYER (visual only — DEMON_ALIVE uses scent, not affected) |
| Player has Blindness effect (self) | No change to witness scan | N/A — this affects the player's own vision, not witnesses |
| Player is on fire / burning | x1.30 | All types — light and sound signature |
| Crow is perched (not following) | Crow radius: 8 blocks instead of 32 | CROW only |
| Crow has Blindness/Darkness effect | x0.00 (Crow fully suppressed) | CROW only |

# **11. Reputation Events Table**
Base delta values shown. Runtime multipliers (target rank, difficulty, pure vs tainted outcome) are applied by RepDeltaCalculator. Events marked AUTO apply regardless of witness presence.

*Becoming a Kizuki rank lowers Corps Rep — the Corps views a Slayer claiming a demon rank as a betrayal of mission. The demon world views it as a legitimate dominance claim.*

## **11.1  Combat Events**
| Action | Corps | Tanjiro | Muzan | Demon | Witness / Notes |
| --- | --- | --- | --- | --- | --- |
| Kill a regular demon (witnessed) | +12 | 0 | -4 | +6 | CROW or DEMON_SLAYER required for Corps delta |
| Kill a regular demon (unwitnessed by Corps) | 0 | 0 | -4 | +6 | DEMON_DYING always fires to Muzan. DEMON_ALIVE fires to Demon dim if it survives. |
| Kill a Lower Moon (witnessed by Corps) | +30 | 0 | -15 | +25 | CROW or DEMON_SLAYER required |
| Kill a Lower Moon (unwitnessed by Corps) | 0 | 0 | -15 | +25 | AUTO for Muzan/Demon dims |
| Kill an Upper Moon (witnessed by Corps) | +60 | 0 | -40 | +50 | CROW or DEMON_SLAYER required |
| Kill an Upper Moon (pure Blood Battle) | +60 | 0 | -40 | +65 | AUTO - both combat dims |
| Kill an Upper Moon (tainted Blood Battle) | +60 | 0 | -40 | +35 | +30 Suspicion. Tainted = interferers present. Corps do not care how the demon died. |
| Claim a Kizuki rank (Blood Battle win) | -40 | -10 | -30 | +50 | AUTO. Corps views rank-claiming as crossing a line. Blood Battles only occur between demon-path players and Nemesis demons. |
| Claim a Kizuki rank (tainted) | -70 | -20 | -30 | +30 | AUTO. Tainted claim is worse for Corps standing. Blood Battles only occur between demon-path players. |
| Player flees from a demon | -8 | 0 | +3 | -8 | CROW or DEMON_SLAYER present |
| Kill a Villager (witnessed) | -60 | -30 | 0 | 0 | Any DSHQ witness. High Suspicion spike +50. |
| Kill a Demon Slayer NPC (witnessed) | -100 | -50 | 0 | 0 | Any witness. Suspicion +80. Approaches hard line. |
| Kill a Demon Slayer NPC (unwitnessed) | -60 | -30 | 0 | 0 | Still auto-reported partially via Corps network. |
| Protect a Villager from a demon (witnessed) | +15 | 0 | 0 | 0 | VILLAGER witness required |
| Survive sunrise against a demon | +8 | +5 | -8 | +5 | AUTO |
| A demon survives the encounter | 0 | 0 | +5 | -10 | AUTO - demon carries memory forward |

## **11.2  Mission & Story Events**
| Action | Corps | Tanjiro | Muzan | Demon | Notes |
| --- | --- | --- | --- | --- | --- |
| Complete a standard Corps mission | +18 | 0 | 0 | 0 | AUTO |
| Abandon a Corps mission | -20 | 0 | 0 | 0 | AUTO |
| Complete a mission alongside Tanjiro | +12 | +15 | 0 | 0 | AUTO |
| Report Nezuko's existence to DSHQ | +25 | -60 | 0 | 0 | AUTO. Does not trigger hard line but deeply damages Tanjiro rep. |
| Protect Nezuko secretly (witnessed by Tanjiro) | 0 | +35 | 0 | 0 | Tanjiro must be present (within 64 blocks). |
| Nezuko proven innocent (story beat) | +30 | +20 | 0 | 0 | AUTO when event fires. Reduces Suspicion by 50. |
| Allow Shinobu to take Daughter Spider kill | 0 | +5 | 0 | -12 | AUTO |
| Allow Giyu to take Rui (LM5) kill | +10 | +15 | -12 | -25 | AUTO. Giyu's kill is witnessed by the Corps. |
| Kill Enmu (LM1, Mission 9) | +35 | +12 | -25 | +22 | AUTO |
| Survive Akaza to sunrise (Mission 10, canon) | +25 | +18 | -15 | +18 | AUTO |
| Deplete Akaza HP (Mission 10, non-canon) | +50 | +25 | -50 | +55 | AUTO. Akaza gains extreme hatred of player in Nemesis. |
| High contribution in Swordsmith Village defense | +40 | +18 | 0 | +30 | AUTO. Tracked by damage dealt to boss entities. |
| Kill Kokushibo (UM1) | +80 | +30 | -80 | +80 | AUTO |
| Kill Muzan | +150 | +50 | -500 (hard line triggers) | +50 | AUTO. Muzan hard line fires simultaneously. |
| Cure Tanjiro (Mission 23, canon end) | +60 | +80 | -60 | -20 | AUTO. Highest single Tanjiro rep gain. |
| Kill demon Tanjiro (Mission 23, non-canon end) | +40 | -200 | -40 | +40 | AUTO. Massive Tanjiro rep loss — he is dead. |
| Assist Tamayo (Mission 3 clinic) | +8 | +12 | -8 | 0 | AUTO |
| Complete Hashira training session | +12 | 0 | 0 | 0 | AUTO |

## **11.3  Social & Concealment Events**
| Action | Corps | Tanjiro | Muzan | Demon | Notes |
| --- | --- | --- | --- | --- | --- |
| Bribe a witness (success) | 0 | 0 | 0 | 0 | No rep change — event suppressed. |
| Bribe a witness (failure) | 0 | 0 | 0 | 0 | +15 Suspicion. No rep delta. |
| Intimidate a witness (fail) | -8 | 0 | 0 | 0 | +25 Suspicion. |
| Kill a witness (seen by DSHQ) | -50 | -20 | 0 | 0 | +50 Suspicion. |
| Kill Crow directly (witnessed) | -10 | 0 | 0 | 0 | +40 Suspicion. |
| Kill Crow directly (unwitnessed) | 0 | 0 | 0 | 0 | +20 Suspicion. |
| Crow dies 'accidentally' (lava/fall, no DSHQ witness) | 0 | 0 | 0 | 0 | +0 Suspicion. Treated as natural death. |
| Drink Muzan's blood (demon path) | -150 | -80 | Muzan becomes omniscient | +50 | AUTO. Permanent — all future actions reported to Muzan regardless. |

# **12. Tension, Flow State & Pacing**
## **12.1  Pacing by Game Phase**
| Phase | Active Dimensions | Design Intent |
| --- | --- | --- |
| Early (Missions 0-3) | Corps only actively matters. Others exist but have low stakes. | Teach witness mechanics gently. Low population density means few witnesses. Concealment succeeds easily. Player builds habits. |
| Mid (Missions 4-9) | Corps + Demon Notoriety both bite. Tanjiro choices emerge. | First double-agent moment at Mission 7. Demon Notoriety starts modifying combat encounters. Stakes feel real. |
| Late-mid (Missions 10-15) | Three dimensions in tension. Muzan Favor starts mattering. | Corps Suspicion becomes a resource to manage. Hashira training gates create planning pressure. First hard Investigation risk. |
| Endgame (Missions 16+) | All four dimensions, hard lines in play. | Players who maintained all four dimensions unlock unique story branches. The payoff for the double-agent fantasy. |

## **12.2  Suspicion System**
Suspicion (0-500) is the Corps' internal unease about the player. Never displayed in UI. Inferred through NPC dialogue coldness, Hashira behavior, and access restrictions.

| Range | Effect |
| --- | --- |
| 0-99 | No effect. Hashira and Corps NPCs speak warmly. |
| 100-199 | Minor dialogue chill. Some NPCs slightly guarded. |
| 200-299 | Hashira dialogue noticeably cooler. Certain mission tiers locked. |
| 300-399 | DSHQ sends an optional 'check-in' mission. Suspicious to refuse (+30 Suspicion if declined). |
| 400-499 | Two Hashira refuse training until Suspicion drops. NPC whisper networks active. |
| 500 | Corps Investigation Event triggers. Player completes a loyalty mission or loses -150 Corps Rep and Suspicion resets to 250. |

## **12.3  Anti-Analysis-Paralysis Rules**
- Rep deltas from small actions are never shown as numbers. Only tier-crossing events produce named notifications.
- No action is permanently irreversible except hard line violations and the five listed permanent blacklist triggers.
- The player always has at least one concealment path available in any situation if they planned ahead.
- Unwitnessed sins in recentSins fade after 14 in-game days. Patient play is rewarded.
- Rewards are always mechanical: stat bonuses, ability unlocks, mission access, story branches. Never cosmetics or crafting ingredients.

# **13. Integration — Nemesis System**
## **13.1  Hunter Spawn Chance: Demon Notoriety Override**
Demon Notoriety tier directly modifies the Nemesis spawn formula's relationshipMod multiplier as a flat bonus applied per-player:

| Demon Notoriety Tier | relationshipMod Bonus | Behavioral Note |
| --- | --- | --- |
| Prey (-500 to -301) | -0.40 (reduces spawn chance) | Cowardly demons attack. Duelist/Berserker ignore entirely. |
| Weakling (-300 to -101) | -0.20 | Scavengers now include the player in candidate list. |
| Unknown (-100 to 99) | +0.00 (base formula) | Standard Nemesis behavior. |
| Spotted (100-199) | +0.10 | - |
| Marked (200-349) | +0.25 | - |
| Feared (350-499) | +0.50 | Cowardly/Scavenger demons flee on sight. |
| Hunted (500-699) | +0.80 | Upper Moons begin dispatching. |
| Legendary (700-849) | +1.20 | Some Lower Moons refuse engagement. |
| Cursed Name (850-1000) | +1.50 (capped at 95%) | Maximum aggression from ambitious types. |

## **13.2  Muzan Negative Favor - Personal Kizuki Dispatches**
When Muzan Favor is negative, he personally directs specific Kizuki to hunt the player. This is a separate spawning channel from the standard Nemesis hunter system - it runs on a fixed timer and dispatches regardless of the Kizuki's own relationship or territory preferences.

| Muzan Favor Tier | Dispatch Tier | Frequency |
| --- | --- | --- |
| Hunted (-300 to -101) | Lower Moon (random, weighted to LM6-LM4) | Once per 7 in-game days |
| Marked for Death (-500 to -301) | Upper Moon (UM6-UM4, escalating over time) | Once per 3 in-game days |

*These Muzan-dispatched hunters are tagged as MUZAN_DISPATCHED in the BattleSession. Killing them grants bonus Demon Notoriety but does not affect Muzan Favor directly.*

## **13.3  Taunt Content - Notoriety-Aware Keys**
| New Taunt Key | Trigger | Example Use |
| --- | --- | --- |
| prey_taunt | Player is Prey tier | Mocking, contemptuous - the demon sees an easy meal. |
| weakling_taunt | Player is Weakling tier | Dismissive and bullying. Cowardly demons feel brave. |
| notorious_greeting | Player is Feared+ tier | Demon references the player's known kill record. |
| cursed_name_greeting | Player is Cursed Name tier | Demon names the player specifically, with fear or obsession. |
| muzan_dispatch_taunt | Demon is MUZAN_DISPATCHED | Demon announces they were sent by Muzan directly. |
| corps_traitor_taunt | Demon witnessed high Muzan Favor via intel | Demon hints at knowing the player has loyalties on both sides. |

# **14. Integration — Mission Branching (Full Spec)**
All missions have a canonical path that is always completable regardless of reputation. Branches add, alter, or replace content — they never gate the critical story path.

*Rewards listed are mechanical only: stat bonuses, ability unlocks, mission access, story branches. No cosmetics or crafting ingredients.*

## **Mission 0: Final Selection**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Survive only (canon) | Default - reach sunrise on Day 7 | Mission clears. Hand Demon may or may not be killed. | None | Rank set to Mizunoto. Standard starting loadout. |
| Kill the Hand Demon | Player defeats Hand Demon before sunrise on Day 7 | Mission clears with high-achievement flag. | AUTO: +20 Corps, +15 Demon Notoriety | Rank set to Mizunoto with +20 kill count bonus (head start toward Mizunoe). Hand Demon's Nemesis slot is cleared. |
| Tanjiro assists (any tier) | Tanjiro spawns dynamically - his presence has no branch condition here | Tanjiro fights alongside player. Shared kill does not count as solo for Hashira path. | If Tanjiro is present for killing blow: no Kizuki solo flag set | No change to kill record - Tanjiro's presence is flavor here. |

## **Mission 1: Kidnapper's Bog**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard investigation | Default | Player gathers clues, finds portal, kills Swamp Demon. | AUTO: +12 Corps, +6 Demon Notoriety | Kill counts forward. Mission clears. |
| Tanjiro leads the investigation | Tanjiro affinity ≥ Acquaintance (100) | Tanjiro shares a direct clue that skips one NPC conversation step. | AUTO: +5 Tanjiro on mission complete | Reduced investigation time. Access to a second optional objective: find the Swamp Demon's lair before it is fully active for bonus Demon knowledge (unlocks a taunt entry in the Nemesis taunt pool). |
| Corps sends backup | Corps Rep ≥ Recognized (1+) AND Rank ≥ Mizunoe | A Demon Slayer NPC assists for the final fight. | None extra | Demon Slayer NPC stays as a temporary escort after mission completion, providing a DEMON_SLAYER witness presence that reduces ambush frequency for 3 in-game days. |

## **Mission 2: Asakusa**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Muzan encounter — lose him (default) | Default | Muzan flees. Generic Demon spawn rate increases temporarily. | AUTO: –4 Muzan Favor (he noted the player exists) | Standard mission progression. |
| Muzan encounter — Tanjiro present | Tanjiro affinity ≥ Comrade (250) | Tanjiro recognizes Muzan and reacts. Dialogue branch: Tanjiro shares what he knows about Muzan with the player. | AUTO: +8 Tanjiro | Player receives Muzan Profile intel — early partial entry in the story log about Muzan's appearance. |
| High Demon Notoriety encounter | Demon Notoriety ≥ Marked (200) | The Generic Demon recognizes the player on sight. Modified taunt. Harder fight variant. | AUTO: +8 Demon Notoriety on kill | Additional kill in demon category for KillRecord. |

## **Mission 3: Tamayo**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard ambush | Default | Survive Susamaru + Yahaba double fight. Tamayo and Yushiro present. | AUTO: +8 Corps, +12 Tanjiro, –8 Muzan per kill | Standard mission clear. |
| Tanjiro coordinates the fight | Tanjiro affinity ≥ Comrade (250) | Tanjiro actively coordinates — splits aggro between player and himself, making the dual fight easier. | AUTO: +8 Tanjiro on complete | Reduced incoming damage during fight. Tamayo offers a passive buff item (non-crafting — a one-use medicine that restores HP once in any future fight). |
| Spare Susamaru (attempt) | Any — player does not deliver killing blow to Susamaru | Susamaru is injured. Tamayo intervenes with her blood technique. Susamaru dissolves from Muzan's blood curse. | AUTO: +15 Tanjiro (witnessed mercy), –5 Corps (incomplete kill), –20 Muzan Favor | Tamayo shares additional demon biology research — unlocks a passive ability: player can detect demon weaknesses (type visible in HUD during first encounter with a new demon type). |

## **Mission 4: Tsuzumi Mansion**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard clear | Default | Kill Tongue Demon, Horned Demon, Kyogai in sequence. | AUTO: +30 Corps total, +25 Demon Notoriety | Kill counts forward. |
| Kyogai's drum notes (non-canon) | Tanjiro affinity ≥ Friend (450) | After defeating Kyogai, Tanjiro finds and preserves Kyogai's drum score notebooks. Non-canon moment — Kyogai's legacy acknowledged. | AUTO: +15 Tanjiro | Tanjiro later uses this — at Mission 9 (Mugen Train), Tanjiro's morale bonus from this moment increases his combat effectiveness alongside the player (passive: +10% Tanjiro NPC damage during Mission 9). |
| Solo clear (all three, no assist) | No other entities deal damage to any boss during the entire mission | Mission clears with solo flag. | AUTO: extra +15 Demon Notoriety | KillRecord: all three count as solo kills. Contributes toward Hashira alternate path tracking. |

## **Mission 5: Mt. Natagumo — Outer Perimeter**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard rescue | Default | Locate Murata, neutralize Puppets. | AUTO: +15 Corps, +10 Tanjiro if present | Mission clears. Murata remains as a recurring minor NPC. |
| Murata survives with no damage taken | Player shields Murata from all Puppet attacks throughout | Murata is fully unharmed. | AUTO: +20 Corps (clean rescue bonus) | Murata files a detailed report on the player's skill — reduces Suspicion by 20. |
| Tanjiro coordinates entry | Tanjiro affinity ≥ Comrade (250) AND Tanjiro spawns | Tanjiro spots an alternate entry path through the forest that bypasses the first Puppet wave. | AUTO: +8 Tanjiro | Skips the first Puppet wave entirely. Saves time, reduces damage taken entering the inner perimeter. |

## **Mission 6: Mother Spider Demon**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard kill | Default | Kill Mother's Doll mini-boss, then Mother Spider Demon. | AUTO: +20 Corps, +18 Demon Notoriety | Kill counts. |
| Spare the controlled children (non-canon) | Player does not kill any of the controlled child puppets during the Doll phase | Mother Spider Demon fight proceeds without the Doll phase interlude. | AUTO: +25 Tanjiro (witnessed mercy) | Tanjiro's trust deepens significantly. Unlocks a dialogue at Mission 7 where Tanjiro directly tells the player what they mean to him — early Tanjiro affinity confirmation without needing to ask him. |

## **Mission 6b: Spider Family**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Daughter Spider — kill in time | Defeat within time limit | Player gets full kill + loot. | AUTO: +20 Demon Notoriety | Kill counts toward KillRecord. |
| Daughter Spider — Shinobu takes kill | Timer expires | Shinobu arrives and finishes the fight. | AUTO: +8 Corps (Shinobu's presence noted), 0 Demon Notoriety | No kill credit. Shinobu observes the player — reduces Suspicion by 10 if Corps Rep ≥ Trusted (200). |
| Rui (LM5) — kill in time (solo) | Defeat within time limit, no interferers | Pure Blood Battle victory. | AUTO: +50 Demon Notoriety, –40 Corps, Rank: if Kinoe, counts as Kizuki solo toward Hashira path | Full LM5 BP. Power vacuum triggers in Nemesis. LM5 slot opens. |
| Rui (LM5) — kill in time (assisted) | Defeat within time limit, interferers present | Tainted Blood Battle victory. | AUTO: +35 Demon Notoriety, –70 Corps, +30 Suspicion | 50% Rui's BP. Rank claim marked tainted. |
| Rui (LM5) — Giyu takes kill | Timer expires | Giyu arrives and kills Rui. | AUTO: +10 Corps, +15 Tanjiro (Giyu is trusted by Tanjiro), –25 Demon Notoriety | No kill credit. No rank claim. Giyu reports to Corps favorably about the player's conduct (+15 Corps delayed). |

## **Mission 7: The Traitor**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Tanjiro resists (low affinity) | Tanjiro affinity < Acquaintance (100) | Tanjiro does not comply peacefully. Escort becomes a contested mission. Inosuke and Zenitsu may intervene. | If player uses force: –40 Tanjiro per crew member harmed | Mission still completes if player reaches DSHQ with Tanjiro, but at significant rep cost. |
| Tanjiro complies willingly | Tanjiro affinity ≥ Acquaintance (100) | Tanjiro agrees to come. Peaceful escort. | AUTO: +20 Tanjiro, +15 Corps | Smoother mission. Tanjiro speaks positively to the Hashira at DSHQ — reduces Suspicion by 15. |
| Nezuko proven innocent (early) | Corps Rep ≥ Trusted (200) AND Tanjiro affinity ≥ Friend (450) | Nezuko's innocence is established before the DSHQ trial — the player's standing with both factions accelerates the process. | AUTO: +30 Corps, +20 Tanjiro. Suspicion –50. | Nezuko is freed immediately. Tanjiro's protectiveness of Nezuko no longer creates tension in future missions. Accelerated story access to Mission 8. |
| Nezuko proven innocent (standard) | Default — always fires at DSHQ after escort regardless of rep | Nezuko is proven innocent at the DSHQ trial. Tanjiro's oath to Kagaya is acknowledged. | AUTO: +30 Corps, +20 Tanjiro. Suspicion –30. | Standard story progression. Nezuko's status no longer a political liability. |
| Player betrays Tanjiro to DSHQ (reports escape plan) | Player reports Tanjiro's planned resistance to a Demon Slayer NPC before mission starts | DSHQ sends an escort team. Mission is easier mechanically. | AUTO: –80 Tanjiro, +20 Corps, +40 Suspicion (DSHQ suspicious of player's knowledge) | Mission completes faster but Tanjiro's trust is severely damaged. Any future branch requiring Friend+ affinity becomes harder to reach. |

## **Mission 8: The Lower Moon**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard elimination | Default — find and kill the fleeing Lower Moon | Mission clears. | AUTO: varies by LM rank killed. See §11.2 combat events. | Kill counts. Nemesis power vacuum triggers. |
| Intel before the hunt | Tanjiro affinity ≥ Comrade (250) AND Tanjiro spawns | Tanjiro shares information about the target demon's last known territory — reduces search time. | AUTO: +5 Tanjiro | Search phase shortened. Player arrives before the demon fortifies a lair. |
| Demon flees successfully | Player fails to intercept before time limit | The Lower Moon escapes. Nemesis system marks them as having survived and gains a significant BP boost. | AUTO: –20 Corps, –15 Demon Notoriety | Mission fails. The demon grows stronger in the Nemesis hierarchy. |

## **Mission 9: Mugen Train**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard clear | Default | Board train, defeat minor demons, survive dream realm, kill Enmu. | AUTO: +35 Corps, +12 Tanjiro, –25 Muzan, +22 Demon Notoriety | Rengoku acknowledges the player. Kill counts. |
| Nightmare realm — self | Always triggers | Player faces a 'nightmare self' boss. Design of nightmare is based on player's dominant negative reputation dimension (worst faction standing becomes the nightmare's theme). | None — internal dream challenge | Clearing nightmare quickly (under 3 minutes): bonus to one stat permanently (+2 to lowest of Vigor/Strength/Agility as defined in DemonTrait stat framework). |
| Rengoku survives (non-canon) | Player has Corps Rep ≥ Revered (600) AND intervenes aggressively in Mission 10 | If player manages to prevent Akaza's victory condition — see Mission 10. | See Mission 10 branches | Rengoku remains available as a training master for Flame Breathing permanently. His survival is a non-canon divergence. |
| Kyogai morale bonus (carry-forward) | Completed Tsuzumi Mansion non-canon branch (Mission 4) | Tanjiro's morale bonus applies. +10% NPC damage from Tanjiro during this mission. | None additional | Minor mechanical advantage in the demon-clearing phase. |

## **Mission 10: Akaza**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Survive to sunrise (canon) | Default — endure Akaza until dawn | Akaza retreats. Canon path. | AUTO: +25 Corps, +18 Tanjiro, –15 Muzan, +18 Demon Notoriety | Standard rewards. Rengoku dies in canon. Akaza gains high hatred of player in Nemesis. |
| Deplete Akaza's HP bar (non-canon) | Player deals killing blow to Akaza before sunrise | Non-canon victory. Akaza is defeated but escapes moments before death (in-lore: retreats underground before sun touches him). | AUTO: +50 Corps, +25 Tanjiro, –50 Muzan, +55 Demon Notoriety, Akaza gains EXTREME hatred flag in Nemesis | Akaza's Kizuki UM3 slot opens (power vacuum). Akaza becomes the single most dangerous Nemesis hunter for this player permanently. |
| Rengoku survives (non-canon, requires Corps Rev. 600+) | Player uses a Corps-rank-gated ability or tactic that Akaza did not anticipate — mechanically: player must deal 40% of Akaza's total HP as damage before Rengoku falls below 20% HP | The fight outcome shifts — Rengoku survives critically wounded. Akaza still retreats at sunrise. | AUTO: +80 Corps (saving a Hashira is exceptional), +30 Tanjiro | Rengoku survives. Becomes a permanent Flame Breathing master. His training unlocks an additional Flame form not otherwise available. |

## **Mission 11: Entertainment District**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Infiltration — disguise | Default — player dresses as a civilian to enter Yoshiwara | Standard infiltration. Gather intel across several NPC conversations. | None during infiltration phase | Access to the inner district and Mission 12. |
| Infiltration — Corps contact | Corps Rep ≥ Trusted (200) | A Corps informant inside Yoshiwara provides direct location of the demon immediately. | AUTO: +5 Corps | Skips multiple investigation steps. Arrives at Mission 12 faster. |
| Infiltration — demon reputation | Demon Notoriety ≥ Feared (350) | A surviving demon inside Yoshiwara recognizes the player and panics — giving away the demon's location accidentally while fleeing. | AUTO: +5 Demon Notoriety (fear spreads) | Alternative skip of investigation steps. |

## **Mission 12: Daki and Gyutaro**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard kill | Default — kill both simultaneously | Mission clears. Both heads must be severed at the same time. | AUTO: +60 Corps, –40 Muzan, +50 Demon Notoriety. UM6 power vacuum in Nemesis. | Kill counts. Both UM6 siblings removed from Nemesis. |
| Tanjiro coordinates | Tanjiro affinity ≥ Friend (450) | Tanjiro signals the simultaneous kill timing — a visual/audio cue that makes the coordination window wider. | AUTO: +10 Tanjiro | Timing window extended from 1 second to 3 seconds for simultaneous kill condition. |
| Gyutaro's poison survives | Player is hit by Gyutaro's blood sickle poison without a cure | Player survives fight but carries poison debuff into Mission 13 | –10 Corps (operating impaired) | Poison debuff active during Mission 13 start. Can be cured by Tamayo if Tanjiro affinity ≥ Friend (she is reachable via Tanjiro's network). |

## **Mission 13: Swordsmith Village**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard defense | Default | Defend the village, kill Gyokko and Hantengu in any order with any assistance. | AUTO: scaled by contribution — see §11.2 | Kill counts. Standard rewards. |
| High contribution | Player deals ≥40% of total damage to both bosses | Mission clears with high-contribution flag. | AUTO: +40 Corps, +18 Tanjiro, +30 Demon Notoriety | Bonus: permanent stat point (+3 distributed by GrowthCalculator logic adapted for players) for exceptional combat contribution. |
| Protect the village (no civilian entity deaths) | No Villager entities in the village die during the mission | Defense succeeds with zero casualties. | AUTO: +25 Corps, +15 Tanjiro | Swordsmith village becomes a safe fast-travel waypoint for the player permanently. |
| Muichiro's memory unlocked (carry-forward) | Only if player has completed Muichiro Hashira training (Mission 14 prereq) | Muichiro fights with full memory and power from the start of the mission rather than recovering mid-fight. | None additional | Muichiro is significantly more effective as an NPC ally during the Gyokko fight. |

## **Mission 14: Hashira Training**
Each Hashira has individual access conditions based on Demon Slayer Rank and Corps Rep. See §15 for full style master specifications. Rep consequences for training completion are listed in §11.2.

| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard training | Meet each Hashira's access requirements | Player completes training session. Style form unlocks or advances. | AUTO: +12 Corps per completed session | Style ability unlock or advancement. See §15. |
| Full circuit (all 8 Hashira trained) | Complete all available Hashira training sessions | Full circuit complete. | AUTO: +40 Corps bonus on completion of final session | Passive combat stat bonus: +3 to all base combat stats (Vigor, Strength, Agility as in DemonTrait model applied to player). |
| Rengoku training (non-canon only) | Rengoku survived Mission 10 | Rengoku available as Flame Breathing master. Not otherwise accessible. | AUTO: +15 Corps per session | Exclusive Flame form unavailable through any other master. |

## **Missions 16–21: Infinity Castle**
The Infinity Castle arc missions share a common branch structure based on carry-forward conditions from earlier missions. Each boss fight has a canonical kill condition and no alternate win path except where noted.

| Mission | Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- | --- |
| 16: Doma vs Shinobu | Player assists Shinobu | Tanjiro affinity ≥ Comrade (250) | Shinobu's fight is aided — she survives the encounter (non-canon, normally she dies poisoning Doma). | AUTO: +30 Tanjiro, +25 Corps | Shinobu survives, skips Mission 19. Kanao and Inosuke freed for other Infinity Castle fights. |
| 16: Doma | Standard kill | Default — player kills Doma | Mission 19 is skipped if Doma dies here. | AUTO: +60 Corps, –50 Muzan, +60 Dem. Not. | UM2 power vacuum. |
| 17: Kaigaku | Standard kill | Default | Mission clears. | AUTO: +50 Corps, –40 Muzan, +50 Dem. Not. | UM3 power vacuum. Unlocks Zenitsu dialogue branch — he thanks the player if affinity ≥ Acquaintance. |
| 18: Akaza (Infinity Castle) | Player fights alongside Tanjiro and Giyu | Always — mission structure requires it | Three-way fight. Player contribution tracked. | AUTO: +60 Corps, –50 Muzan, +65 Dem. Not. If Akaza had EXTREME hatred flag: +20 extra Dem. Not. | If Akaza had EXTREME flag from Mission 10 non-canon: extra ability point. UM3 power vacuum. |
| 19: Doma (Kanao/Inosuke) | Only if Mission 16 standard clear | Doma is alive and must be killed here. | AUTO: same as Mission 16 standard | UM2 power vacuum. |
| 20: Nakime | Standard kill | Default | Mission clears. | AUTO: +50 Corps, –40 Muzan, +50 Dem. Not. | UM4 power vacuum. Castle structure begins collapsing — opens final mission. |
| 21: Kokushibo | Standard kill | Default | Mission clears. | AUTO: +80 Corps, +30 Tanjiro, –80 Muzan, +80 Dem. Not. | UM1 power vacuum. Entire Upper Moon tier emptied if all prior Infinity Castle missions complete. |

## **Mission 22: Demon Slayers vs Muzan**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Standard kill | Default — deplete Muzan's HP | Mission clears. Muzan dies at sunrise. | AUTO: +150 Corps, +50 Tanjiro, muzanRep –500 (hard line fires), +50 Dem. Not. | All Nemesis Kizuki slots destabilized — major power vacuums open across the hierarchy. Story continues to Mission 23. |
| Muzan acknowledges the player (scripted) | Demon Notoriety ≥ Cursed Name (850) | Opening dialogue of the fight is modified — Muzan references the player by name. | None extra | Pure flavor. Demon taunt pool gains a unique entry: Muzan's acknowledgment quote. |
| Tanjiro full power (carry-forward) | Tanjiro affinity ≥ Best Friend (800) | Tanjiro fights at maximum capacity alongside the player with no holding back. | None extra | Tanjiro NPC deal +25% damage during this mission. |

## **Mission 23: Demon Slayers vs Demon Tanjiro**
| Branch | Condition | Outcome | Rep Consequence | Reward |
| --- | --- | --- | --- | --- |
| Cure Tanjiro (canon end) | Tanjiro affinity ≥ Best Friend (800) at time of mission start | Tanjiro is restored to human. Canon ending. | AUTO: +60 Corps, +80 Tanjiro (largest single Tanjiro delta in the game). Suspicion –100. | Tanjiro becomes a permanent field ally. He can be summoned once per in-game day to assist in any non-Nemesis fight. |
| Kill Demon Tanjiro (non-canon end) | Tanjiro affinity < Best Friend (800) | Tanjiro is killed. Non-canon ending. | AUTO: +40 Corps, Tanjiro rep irrelevant (he is dead). muzanRep meaningless (Muzan already dead). | No permanent Tanjiro ally. Story ends. Post-game Nemesis continues with all vacancies from Infinity Castle. |
| Tanjiro fights the player (low affinity) | Tanjiro affinity < Stranger (–100) at mission start | Demon Tanjiro is even more aggressive — he has specific emotional targeting of this player. | If player wins: standard kill outcome | Demon Tanjiro deals +20% damage to the player during this mission. |

# **15. Integration — Style Masters & Minigames**
Each breathing style has at least one master who can teach it. Some are Hashira; some are not. Masters are fixed NPCs with known locations. The Hashira masters have access conditions tied to Demon Slayer Rank and Corps Rep. Non-Hashira masters have their own independent conditions.

## **15.1  Style Minigame Framework**
Every style has a single associated minigame. The minigame is identical regardless of which master teaches it — it represents the style's philosophy, not the teacher's personality. A single lesson from any master unlocks the style. After that, the player can learn all forms independently through continued minigame play. Masters do not gate individual forms; they can accelerate proficiency in specific ones, shortening the time to learn them.

Player-to-Player Teaching:

A player who has mastered all forms of a style becomes a style master themselves.

They can initiate the style's minigame for another player directly — no NPC required.

Talk to a friend, trigger the minigame, unlock the style. The minigame is identical

to the NPC version. The teaching player must have completed every form of that style.

| Style | Minigame Type | Mechanical Description | Failure Consequence |
| --- | --- | --- | --- |
| Water | Flow & Continuity | Circular cursor targets appear in waves along a flowing connected path. Click each before it shrinks to zero without breaking the chain. Breaking resets the attempt. | Retry immediately. No penalty. |
| Flame | Burst Timing | A charge bar fills rapidly. Release the attack key exactly at the peak - not before or after. Multiple bursts in sequence, increasing speed. Three lives per attempt. | All lives lost: 1 in-game day cooldown before retry. |
| Thunder | Reaction Speed | A single target flashes on screen for an extremely brief window, decreasing per tier. Strike it within the window. Pure single-reaction - no chains. | Miss: retry immediately. Persistence over perfection. |
| Mist | Pattern Recognition | A movement sequence plays as a combat animation preview. Reproduce it with directional inputs. Sequences grow longer each tier. Emphasises memory and subtlety. | Retry after 200 ticks. No rep penalty. |
| Wind | Sustained Pressure | A moving target follows an erratic wind-pattern path. Track it and click repeatedly. Too many consecutive misses fail the attempt. | Retry immediately. No penalty. |
| Stone | Endurance & Resolve | A click or keypress counter that runs for a fixed duration, escalating per tier. The player must maintain a minimum clicks-per-second threshold throughout the entire window - slowing down or stopping fails the attempt. The required rate and duration both increase with each form tier. | 1 in-game day cooldown. Stone demands commitment - and aching fingers. |
| Insect | Precision Timing | Multiple small fast targets appear simultaneously. A brief flash sequence indicates the required strike order. Hit all targets in order before they disappear. | Retry immediately. |
| Sound | Rhythm | A rhythm track plays. Hit inputs on beat across patterns of increasing complexity. Full-miss streaks reduce the attempt score below the pass threshold. | Retry immediately. Rhythm cannot be forced. |
| Love | Momentum & Escalation | A combo chain: each successful input extends the chain and appends a new input to the end. The chain must keep growing - stopping or breaking it resets the attempt. | Chain broken: attempt resets. |
| Serpent | Misdirection | Several identical targets flash briefly; only one is real, marked by a barely-visible tell. Identify and strike the correct one. Three lives per attempt. | Wrong target: lose a life. Teaches the style's deceptive nature. |

## **15.2  Style Master Access Table**
Any single master is sufficient to unlock a style. Where multiple masters exist for one style, they differ in access requirements and in which forms they can accelerate — not in whether they can unlock the style.

| Style | Master | Hashira? | Location | Access Condition |
| --- | --- | --- | --- | --- |
| Water | Giyu Tomioka | Yes | River district - 2-3 fixed rotating locations | Rank >= Tsuchinoto. Corps Rep >= Recognized (1+). |
| Water | Sakonji Urokodaki | No | Mt. Sagiri - fixed | Completed Mission 0. No rank requirement. Tanjiro affinity >= Acquaintance (100) provides a letter that waives the default 3-step warm-up dialogue. |
| Flame | Rengoku Kyojuro | Yes | Flame Hashira Estate - fixed | Rank >= Kinoe. Corps Rep >= Revered (600). |
| Flame | Shinjuro Rengoku | No | Flame Hashira Estate - inner room | Must have met Rengoku first. Corps Rep >= Recognized (1+). Shinjuro abandoned the style - he can accelerate fewer forms and is candid about the gaps in his knowledge. |
| Thunder | Jigoro Kuwajima | No | Remote mountain village - fixed, long travel | Rank >= Kanoto. No Corps Rep requirement. Travel distance is the primary gate. |
| Mist | Muichiro Tokito | Yes | Swordsmith Village (post-Mission 13) or Butterfly Estate | Rank >= Tsuchinoe. Corps Rep >= Honored (400). Teaches through the minigame with minimal dialogue. |
| Wind | Sanemi Shinazugawa | Yes | Wind Hashira dojo - fixed | Rank >= Tsuchinoe. Corps Rep >= Honored (400). Must have killed at least one Lower Moon. Refuses if he personally witnessed the player protect Nezuko. |
| Stone | Gyomei Himejima | Yes | Stone Hashira temple - fixed | Rank >= Kinoe. Corps Rep >= Revered (600). Tanjiro affinity >= Comrade (250) - Gyomei weighs character alongside rank. |
| Insect | Shinobu Kocho | Yes | Butterfly Estate - fixed | Rank >= Kanoe. Corps Rep >= Trusted (200). Refuses if Muzan Favor >= Favored (650). |
| Love | Mitsuri Kanroji | Yes | Love Hashira garden estate - fixed | Rank >= Kanoto. Corps Rep >= Recognized (1+). Most accessible Hashira master. |
| Serpent | Obanai Iguro | Yes | Serpent Hashira compound - fixed | Rank >= Kinoto. Corps Rep >= Honored (400). Suspicion <= 200 - Obanai will not teach Corps loyalty risks. |
| Sound | Tengen Uzui | Yes | Sound Hashira Estate - fixed | Rank >= Kanoe. Corps Rep >= Trusted (200). Teaches Sound Breathing only - no connection to Thunder Breathing. |
| Sound | Tengen Uzui (retired) | No | Entertainment District safe house - post-Mission 11 | Corps Rep >= Recognized (1+). No rank requirement. Lower access bar than the active Hashira path. |

*Tengen Uzui has no knowledge of Thunder Breathing and will never reference or offer it. Jigoro Kuwajima is the sole Thunder master.*

# **16. Integration — Persuasion System**
Persuasion is a dialogue-based concealment action available against VILLAGER and DEMON_SLAYER witnesses within the 300-tick window. It uses the player's Demon Slayer Rank as the source of authority — higher rank unlocks more commanding options with better base success. Entirely separate from bribery (item-based) and intimidation (fear-based).

## **16.1  Persuasion Options by Rank**
Each row is a dialogue option unlocked at that rank. On success, the witnessed act is suppressed. On failure, the consequences vary by option — none of them are silent.

| Rank Gate | Dialogue Line | Base % | On Success | On Failure |
| --- | --- | --- | --- | --- |
| Mizunoto+ | 'I am a Demon Slayer. This was necessary.' | 30% | Act suppressed. Witness stands down. | Witness files report. –15% to future persuasion from this player with this witness permanently. |
| Kanoe+ | 'This is Corps business. Stand down.' | 50% | Act suppressed. | +10 Suspicion. Witness files a conduct query. |
| Tsuchinoe+ | 'I will take responsibility for this.' | 65% | Act suppressed. | Witness reports but logs that the player accepted responsibility. Only +5 Suspicion instead of full amount. |
| Kinoto+ | 'You did not see this. Walk away.' | 75% | Act suppressed. | Witness alarmed. Reports with +20 Suspicion and notes pressure was applied. |
| Hashira | 'I am a Pillar. This conversation is over.' | 90% | Act suppressed. No further dialogue. | Rare failure. –20 Corps Rep, +30 Suspicion — a Hashira abusing authority is a serious incident. |

## **16.2  Persuasion Context Conditions**
Checked by PersuasionContextEvaluator at dialogue trigger time. Modifiers apply additively to the chosen option's base success chance.

| Condition | Modifier |
| --- | --- |
| Witnessed act was killing a demon (DEMON_ALIVE or DEMON_DYING present) | Base success +15% — the witness saw a threat neutralised. |
| Player protected this specific witness within the last 7 in-game days | Base success +25% — personal debt. |
| Player recently killed a civilian or Demon Slayer NPC near this witness | Base success –40% — context is damning. |
| Player Corps Rep is currently negative | Base success –20%. |
| Player previously successfully persuaded this witness | Base success –10% — they remember. |
| Witness observed a prior suspicious act from this player | Base success –30% — already on guard. |

## **16.3  Persuasion Cooldown**
Each VILLAGER or DEMON_SLAYER entity tracks the last time they were successfully persuaded. A witness persuaded within the last 14 in-game days cannot be persuaded again — only bribed, intimidated, or eliminated. Stored per-witness-entity-UUID via entity capability, so cooldowns survive unload and reload.

*PersuasionCooldown is stored per-witness-entity-UUID in the PersuasionManager. Entities that unload and reload retain their cooldown state via entity capability.*

# **17. Persistence & Serialization**
| Data | NBT Format | Serialized? |
| --- | --- | --- |
| playerRecords (all 4 dims + suspicion) | CompoundTag (UUID -> PlayerRepRecord NBT) | Yes |
| killRecord per player | CompoundTag within PlayerRepRecord | Yes |
| slayerRank per player | String (enum name) within PlayerRepRecord | Yes |
| hardlineFlags per player | ListTag of flag name strings | Yes - permanent, never cleared on load |
| witnessEventQueue | ListTag of WitnessEvent NBTs | Yes - events survive restart |
| recentSins per player | ListTag within PlayerRepRecord | Yes - pruned on load if > 14 days old |
| suppressionLog (active effects) | NOT serialized - transient | No - lost on restart intentionally |
| persuasionCooldowns per witness | Entity capability NBT on each witness entity | Yes - survives entity unload/reload |

Suppression loss on restart is intentional design.

Active blinding, bribery windows, and intimidation suppression effects are all lost

on server restart. WitnessEvents that have not yet been processed remain queued

and will be processed on the next daily tick after restart — but without any

suppression to intercept them. Do not log out mid-operation.

# **18. Commands**
All reputation commands are subcommands of /demonslayer, matching the existing convention. OP level 2 required unless noted.

| Command | Description |
| --- | --- |
| /demonslayer reputation view <player> | Shows all four dimension scores, suspicion, current rank, and any active hardline flags. |
| /demonslayer reputation set <player> <dim> <value> | Sets a dimension score. dim: corps \| tanjiro \| muzan \| demon \| suspicion. Rank is NOT a valid dim here - use the existing rank modification command instead. |
| /demonslayer reputation add <player> <dim> <value> | Adds/subtracts from a dimension. Accepts negative values. Respects -500/1000 clamp. |
| /demonslayer reputation reset <player> | Resets all four dimensions to 0. Clears sin log, suspicion, AND hardline flags. Full wipe. |
| /demonslayer reputation hardline set <player> <flag> | Manually sets a hardline flag. Irreversible in normal gameplay - OP command only. |
| /demonslayer reputation hardline clear <player> <flag> | Clears a specific hardline flag. Admin override only. |
| /demonslayer reputation sinlog <player> | Lists the player's recentSins log with timestamps and faction targets. |
| /demonslayer reputation witness simulate <player> <eventType> <witnessType> | Fires a test WitnessEvent through the full processing pipeline for QA. |
| /demonslayer reputation suppress clear <player> | Clears all active suppression entries for a player. |
| /demonslayer reputation investigate trigger <player> | Manually triggers the Corps Investigation event. |

# **19. Class Reference Map**
| Class | Package | Purpose |
| --- | --- | --- |
| ReputationManager | reputation | World-saved singleton. Core store + daily processing. |
| PlayerRepRecord | reputation | Per-player four-dimension scores, suspicion, rank, kill record, sin log, hardline flags. |
| KillRecord | reputation.rank | Per-category kill counters for rank advancement gating. |
| SlayerRank | reputation.rank | Enum: MIZUNOTO through HASHIRA. Independent of corpsRep. |
| RankAdvancementChecker | reputation.rank | Stateless - checks KillRecord against advancement thresholds each time a kill is registered. |
| HardlineFlag | reputation | Enum of all permanent blacklist conditions. |
| WitnessEvent | reputation.witness | Immutable record of a witnessed action and its reporting chain. |
| WitnessType | reputation.witness | Enum: CROW, VILLAGER, DEMON_SLAYER, DEMON_ALIVE, DEMON_DYING. |
| WitnessScanner | reputation.witness | Stateless - scans entities near a BlockPos, applies modifier table, returns WitnessSet. |
| WitnessModifierTable | reputation.witness | Holds all modifier values (breathing, time, moon, lighting, player state). Queried by WitnessScanner. |
| SuppressionEntry | reputation.concealment | Active suppression effect with strength, target type, expiry tick. |
| SinRecord | reputation.concealment | Unconfirmed sin entry. Pruned after 14 in-game days without confirmation. |
| ConcealmentHandler | reputation.concealment | Handles dialogue concealment action triggers and suppression application. |
| PersuasionManager | reputation.persuasion | Manages persuasion option evaluation, context modifiers, and per-witness cooldowns. |
| PersuasionContextEvaluator | reputation.persuasion | Stateless - evaluates context conditions for persuasion success at dialogue time. |
| RepEventType | reputation | Enum of all reputation-affecting action types (50+ entries). |
| RepDeltaCalculator | reputation | Stateless - computes dimension deltas for a given RepEventType + context. |
| CorpsSuspicionManager | reputation.corps | Manages Suspicion sub-meter and Investigation event triggering. |
| InvestigationEvent | reputation.corps | Dedicated mission-like event structure for the Corps Investigation story beat. |
| Faction | reputation | Enum: DSHQ, TANJIRO_CREW, MUZAN, DEMON_WORLD. |
| ReputationEvents | reputation | Forge event handler - hooks LivingHurtEvent, LivingDeathEvent, PlayerTickEvent, dialogue triggers. |
| StyleMinigameRegistry | reputation.style | Maps each breathing style to its MinigameType and tier unlock structure. |
| StyleMasterRegistry | reputation.style | Maps each style master NPC to access conditions and which style/forms they teach. |

*End of Document — Reputation & Notoriety System v2.0*
