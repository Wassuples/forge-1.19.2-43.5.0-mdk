# Custom gravity cleanup

## Scope and acceptance

Baseline revision: `c08b01413a5e1fe67e3b8c23837ab4798502d7a8`.

Clean obsolete diagnostics from the custom gravity mixins and remove only demonstrably redundant hot-path work while preserving every existing transform, collision order, threshold, injection, side boundary, and gameplay outcome.

Observable acceptance:

- Gravity production sources contain no `GravityDebug`, debug markers, or direct debug console output.
- Mouse turning no longer allocates an array to return the fixed `(deltaX, -deltaY)` mapping.
- Step comparison computes the same Euclidean length once.
- Mixin configuration and algorithm constants/formulas remain unchanged.
- Focused compilation, tests, build, whitespace scan, and independent reviews provide evidence; runtime behavior is explicitly not inferred from compilation.

## Workflow limitation

The root `AGENTS.md` requires `docs/AGENT_WORKFLOW.md`, `docs/tasks/TEMPLATE.md`, and `docs/agents/ROLES.md`, but none existed at the baseline revision. The team therefore followed the workflow and role requirements stated directly in `AGENTS.md` and this request, and recorded the missing-document limitation rather than inventing their contents.

## Agents, revisions, and evidence

| Phase | Real agent identity | Revision / state | Findings and evidence |
| --- | --- | --- | --- |
| Lead | `/root` | Baseline `c08b01413a5e1fe67e3b8c23837ab4798502d7a8`; current working tree | Coordinated gates, inspected the complete diff, records validation, owns repairs/commit/PR. |
| Scope review | `/root/scope_review` | Baseline `c08b01413a5e1fe67e3b8c23837ab4798502d7a8`, clean tree | Bounded production scope to four diagnostic-bearing mixins plus `GravityUtil`; found nonexistent `GravityDebug` references, fixed-result array allocation, repeated step length; prohibited formula, threshold, collision-order, injection, configuration, lifecycle, and networking changes. |
| Implementation | `/root/implementer` | Uncommitted five-file diff from baseline | Removed diagnostic imports/calls/comments; inlined the fixed mouse mapping; removed the unused helper; cached exact step length. Agent was interrupted during extended validation after edits became visible; lead inspected and retained the bounded diff. |
| Independent validation | `/root/validator` | Uncommitted diff from baseline | Passed static review: no defects; debug/helper scans clean; mixin config SHA-256 matched baseline; `git diff --check` passed. Gradle was blocked before compilation by the absent Java 17 toolchain. |
| Code review round 1 | `/root/review_round1` | Uncommitted five-file diff from baseline | No findings. Verified arithmetic equivalence, exact cached-length comparison, debug-only nature of removals, no helper callers, unchanged mixin side placement, and passed `git diff --check`. Runtime testing not performed. |
| Code review round 2 | `/root/scope_review` (follow-up turn) | Uncommitted diff from baseline | One low finding: preserve the original pitch double-negation/cast/add expression exactly rather than using float subtraction. No other findings. |
| Acceptance revisit | `/root/scope_review` (later follow-up turn) | Final repaired diff at HEAD `c08b01413a5e1fe67e3b8c23837ab4798502d7a8` | Accepted with zero open findings. Confirmed debug cleanup, allocation/recalculation removal, exact pitch-expression repair, unchanged algorithms/configuration, and clean formatting; Java 17 and runtime checks remain explicitly unverified. |

## Decisions

1. Preserve the custom-gravity algorithms exactly; no squared-distance substitutions or trigonometric/vector refactors.
2. Inline `yawDelta` and negated `pitchDelta` in `GravityMouseHandlerMixin` because the removed helper ignored gravity direction and always returned those values.
3. Cache `steppedLateral.length()` rather than changing the comparison domain.
4. Remove diagnostics across all four affected gravity mixins because their calls execute on high-frequency mouse/movement/travel paths and reference an absent class.
5. Leave `demonslayer.mixins.json` untouched.

## Finding repair

- **R2-1:** Replaced `gravityPitch - (float) pitchDelta` with `gravityPitch + (float) -pitchDelta`. This keeps the allocation removal while restoring the original negation-before-cast and addition sequence. The affected static checks were rerun successfully.

## Validation and review closure

- Passed: `git diff --check`.
- Passed: static scan for `GravityDebug`, `DEBUG`, direct console output, `println`, and `remapTurnDeltas` (no matches; `rg` exit 1 is the expected no-match result).
- Passed: `git diff --exit-code c08b014 -- src/main/resources/demonslayer.mixins.json`.
- Not run successfully: `bash gradlew test`; Gradle configured with Java 21 but stopped before compilation because the project requires Java 17 and no matching local toolchain or download repository exists.
- Not run: runtime client/server mixin application and six-direction in-game checks. Compilation, even when available, would not establish runtime mixin correctness.
- Final scope-agent acceptance revisit: accepted with zero open findings; all static acceptance criteria passed. Automated Gradle and runtime criteria remain environment-blocked/not run as stated above.

## Absolute world-Y movement follow-up

Implementation agent `/root/gravity_implementer` investigated the regression against revision `67c186d`. Vanilla `Player.travel`'s creative-flight tail replaces the post-`LivingEntity.travel` world-Y component with the captured pre-travel world-Y component multiplied by `0.6`. Under sideways gravity, world Y is a lateral movement axis, so that write discards the movement just calculated by the gravity-relative `moveRelative` redirect. The previous HEAD/RETURN correction then replaced the gravity-up component from the pre-travel snapshot and could also discard flight input incorporated during travel.

The fix redirects that exact three-double `setDeltaMovement` call. DOWN retains the supplied vanilla components. Other directions retain the complete post-travel vector and replace only its gravity-up component with the captured pre-travel gravity-up component multiplied by vanilla's unchanged `0.6` factor. The client-only grounded `0.91` lateral damping was also removed to match the already-corrected server path; movement friction remains owned by `GravityTravelMixin` on both logical sides.

Implementation checks: `git diff --check` passed. Runtime mixin application and six-direction movement/creative-flight checks were not run by this agent. No review claims are made here.

### Follow-up delegation and repair log

| Phase | Real agent identity | Revision / state | Findings and evidence |
| --- | --- | --- | --- |
| Scope/root-cause review | `/root/scope_regression` | HEAD `67c186d` | Found vanilla `Player.travel`'s creative tail overwrote absolute world Y and the old RETURN correction could not recover it; also identified client/server lateral-friction inconsistency. |
| Bounded implementation | `/root/gravity_implementer` | Uncommitted diff over `67c186d` | Added the Player travel redirect, removed duplicate client damping, and documented the investigation. |
| Independent validation | `/root/regression_validator` | Final uncommitted diff over `67c186d` | No findings. Confirmed generated refmap owners/descriptors for both redirects, passed Java 17 tests and `git diff --check`, and verified DOWN passthrough plus custom-gravity velocity handling. |
| Code review round 1 | `/root/regression_review1` | Uncommitted diff over `67c186d` | Found client creative flight cleared `noGravity`, diverging from the server; final re-review closed the finding. |
| Code review round 2 | `/root/regression_review2` | Uncommitted diff over `67c186d` | Found creative Space/Shift still needed remapping at its actual LocalPlayer velocity mutation; final re-review closed the finding. |

### Follow-up repairs

1. `GravityPlayerTravelMixin` now redirects the creative-flight tail setter: DOWN forwards vanilla arguments, while custom gravity preserves post-travel lateral velocity (including absolute world Y) and applies vanilla's `0.6` damping to gravity-up.
2. `GravityLocalPlayerMixin` redirects the sole mapped `LocalPlayer.aiStep` `setDeltaMovement(Vec3)` invocation that applies signed creative Space/Shift input. It forwards DOWN unchanged and applies the same signed input along gravity-up for other directions.
3. `ClientEvents` now mirrors the server's separate `isCustomGravity`/`useCustomGravity` gates, retaining `noGravity` during custom-gravity creative flight while skipping the gravity-pull step.
4. The duplicate client-only grounded lateral `0.91` damping was removed; common travel friction remains authoritative on both sides.

Mapped 1.19.2 bytecode inspection confirmed one `LocalPlayer.setDeltaMovement(Vec3)` call in `LocalPlayer.aiStep` (the creative input mutation) and the expected `Player.setDeltaMovement(DDD)` creative tail call. Both non-author review rounds report zero open findings after repair.

### Follow-up validation

- Passed: `JAVA_HOME=$(mise where java@17) PATH=$(mise where java@17)/bin:$PATH bash gradlew test --no-daemon` (`BUILD SUCCESSFUL`; 7 tasks, 3 executed and 4 up-to-date on the final rerun).
- Passed: `JAVA_HOME=$(mise where java@17) PATH=$(mise where java@17)/bin:$PATH bash gradlew build --no-daemon` (`BUILD SUCCESSFUL`; 13 tasks, 7 executed and 6 up-to-date).
- Passed: `git diff --check`.
- Passed: gravity debug scan (no matches).
- Not run: live client/dedicated-server mixin application and manual six-direction movement/creative-flight checks.

Final scope acceptance revisit by `/root/scope_regression`: accepted with zero open static findings. It confirmed that the walking gravity plane retains absolute world Y, creative lateral Y survives the vanilla flight tail, Space/Shift maps to gravity-up, client/server `noGravity` policy is aligned, DOWN delegates unchanged, and both non-author review findings are closed. Runtime six-direction testing remains the documented limitation.

## Creative-flight world-Y friction follow-up

Implementation agent `/root/y_axis_implement` traced the remaining EAST-gravity W/S
regression to `GravityTravelMixin`'s RETURN hook. Although the creative-flight redirects
preserved absolute world-Y velocity, the missing-Y ground-friction hook could immediately
damp that velocity whenever the custom surface still reported the player as grounded.

The initial implementation bypassed the hook for actively flying players. Review found that
this made the two gravity-lateral axes use different damping and that its predicate did not
match vanilla's flight branch. The final repair below replaces that initial approach.

### Final friction repair workflow

| Phase | Real agent identity | Revision / state | Findings and evidence |
| --- | --- | --- | --- |
| Scope/root-cause review | `/root/y_axis_scope` | HEAD `267c9da` | Traced the remaining EAST/WEST W/S suppression to custom world-Y ground friction running during creative flight. |
| Bounded implementation | `/root/y_axis_implement` | Uncommitted diff over `267c9da` | Added the initial flight bypass and this record; the lead subsequently repaired review findings. |
| Independent validation | `/root/y_validator` | Final uncommitted diff over `267c9da` | Zero findings. Confirmed vanilla predicate/call ordering with mapped bytecode, generated refmap targets, Java 17 tests, and whitespace. |
| Code review round 1 | `/root/y_review1` | Uncommitted diff over `267c9da` | Found the initial bypass created lateral damping asymmetry and later found its `flying && mayfly` predicate differed from vanilla; final re-review closed both findings. |
| Code review round 2 | `/root/y_review2` | Uncommitted diff over `267c9da` | Independently found damping asymmetry and a float-literal precision mismatch; both were repaired, with no alternate Y clobber found. |
| Acceptance revisit | `/root/y_axis_scope` | Final uncommitted diff over `267c9da` | Accepted with zero open source findings; confirmed vanilla predicate, exact damping parity, bounded branch behavior, and the remaining runtime limitation. |

The initial full bypass was replaced with gravity-axis parity: for dry, non-elytra flight,
world Y has already received vanilla's `0.98D` damping, so the hook multiplies it by
`(double) 0.91F / 0.98D` to produce the same exact float-derived `0.91F` damping used by
vanilla's other lateral axes. `GravityMoveMixin` no longer synthesizes custom-surface
`onGround` state while the vanilla flight branch (`flying && !isPassenger()`) is active,
preventing block friction from being selected continuously at the wall. Fluid, elytra,
passenger, DOWN/UP, and non-flight paths retain their prior handling.

Final evidence:

- Passed: `JAVA_HOME=$(mise where java@17) PATH=$(mise where java@17)/bin:$PATH bash gradlew test --no-daemon` (`BUILD SUCCESSFUL`; 7 actionable tasks).
- Passed: `git diff --check 267c9da`.
- Passed: generated refmap inspection for `GravityMoveMixin.move` and `GravityTravelMixin.travel`.
- Not run: live-client EAST/WEST and NORTH/SOUTH creative-flight testing; runtime behavior remains a manual validation boundary.

Final scope acceptance by `/root/y_axis_scope`: accepted with zero open source findings.
