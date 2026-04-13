# Progression Engine Decision Tree & Flow Diagrams

## Decision Tree Flowchart

```
START: User completes workout
       ↓
INPUT: Weight, Reps, RPE, DOMS, Sleep
       ↓
┌──────────────────────────────────────┐
│ CREATE RECOVERY ASSESSMENT           │
│ (Combines all 3 signals + weights)   │
└──────────────────┬───────────────────┘
                   ↓
        ┌──────────────────┐
        │ DOMS ≥ 7 ?       │ (SEVERE)
        └────┬─────────┬───┘
         YES │         │ NO
            ↓          │
    HARD VETO:        │
    Score = -0.8     │
            ↓         │
    Signal:          │
    REDUCE_LOAD      │
    or MIRROR        │
            ↓         │
            │         ↓
            │      ┌──────────────────┐
            │      │ DOMS ≥ 4 ?       │ (MODERATE)
            │      └────┬─────────┬───┘
            │       YES │         │ NO
            │          ↓         │
            │      ┌─────────────────────┐
            │      │ RPE ≥ 9.5 ?         │
            │      └────┬────────┬───────┘
            │       YES │        │ NO
            │          ↓        │
            │       MIRROR      ├─→ Score: -0.2 to 0.0
            │       SESSION     │   Signal: MIRROR or
            │          ↓        │   STEADY_PROG
            │          │        ↓
            │          │     Continue to RPE check ↓
            │          │        │
            │          │     ┌──────────────┐
            │          └────→│ RPE ≤ 7.0 ?  │
            │                 └────┬────┬───┘
            │                  YES │    │ NO
            │                     ↓    │
            │              Score +0.5  │
            │              (Ready to   │
            │               progress)  ↓
            │                     ┌────────────┐
            │                     │ RPE 8.0-9.0│
            │                     │ (OPTIMAL)  │
            │                     └────┬───┬───┘
            │                       │   │
            │                       ↓   │ NO
            │                Score +0.2 ↓
            │                (Steady    ┌────────────┐
            │                 state)    │ RPE ≥ 9.5? │
            │                           └────┬───┬───┘
            │                             YES │   │ NO
            │                                ↓   │
            │                           Score -0.4
            │                           to -0.6
            │                           (Tired)
            │                                ↓
            │      APPLY SLEEP MULTIPLIER ←──┘
            │         (0.5 to 1.5)
            │              ↓
            │      ┌────────────────────┐
            │      │ Final Recovery     │
            │      │ Score: [-1, 1]     │
            │      └────────┬───────────┘
            │              ↓
            ├─→ ┌─────────────────────────┐
                │ Determine Signal        │
                │ based on Score & DOMS   │
                └────┬──┬──┬──┬───────────┘
                     │  │  │  │
    ┌────────────────┘  │  │  └─────────────────┐
    │                   │  │                    │
    ↓                   ↓  ↓                    ↓
REDUCE_LOAD      MIRROR_SESSION   STEADY_PROG  INCREASE_LOAD
(-0.8 to -0.3)   (-0.3 to 0.1)    (0.1 to 0.4) (>0.4)
    ↓                   ↓              ↓            ↓
Weight -2.5%      Weight = same   Weight = same  Weight + microload
Reps = same       Reps = same     Reps += 1      Reps = same
    ↓                   ↓              ↓            ↓
┌───────────────────────────────────────────────────┐
│ OUTPUT: Next Prescription (Weight, Reps)         │
└───────────────────────────────────────────────────┘
```

---

## Weight Factor Influence Diagram

```
┌────────────────────────────────────────────────────────────┐
│                    RECOVERY SCORE                          │
│                      [-1.0, 1.0]                           │
└────────────────────────────────────────────────────────────┘
         ↑                ↑                ↑
         │                │                │
         │                │                │
    ┌────┴─────┐   ┌──────┴──────┐   ┌────┴──────┐
    │           │   │             │   │           │
50% DOMS     30% RPE          20% SLEEP
(Dominant)   (Secondary)      (Amplifier)
    │           │               │
    │           │               │
    ↓           ↓               ↓
┌──────────┐ ┌──────────┐  ┌──────────┐
│  Severe  │ │Low RPE   │  │Excellent │
│  (≥7):   │ │(≤7.0):   │  │ Sleep    │
│  -0.8    │ │  +0.5    │  │ (7-10):  │
│          │ │          │  │  +0.4    │
│Moderate  │ │Optimal   │  │Good      │
│(4-6):    │ │(8-9):    │  │ Sleep    │
│-0.5 to0  │ │  +0.2    │  │(6-8):    │
│          │ │          │  │  +0.2    │
│Mild/None │ │High RPE  │  │Fair      │
│(0-3):    │ │(≥9.5):   │  │ Sleep    │
│ +0.1     │ │ -0.4 -0.6│  │(4-6):    │
│          │ │          │  │  -0.2    │
└──────────┘ └──────────┘  │Poor      │
                           │ Sleep    │
                           │(0-3):    │
                           │  -0.5    │
                           └──────────┘
         ↓
    WEIGHTED SUM
    (scaled by weights)
         ↓
┌────────────────────┐
│ FINAL SCORE        │
│ [-1.0, 1.0]        │
└────────┬───────────┘
         ↓
    ┌─────────────────────────────┐
    │ SIGNAL DETERMINATION        │
    ├─────────────────────────────┤
    │ Score < -0.3: REDUCE_LOAD   │
    │ Score -0.3 to 0.1: MIRROR   │
    │ Score 0.1 to 0.4: STEADY    │
    │ Score > 0.4: INCREASE       │
    └─────────────────────────────┘
```

---

## Scenario Comparison Matrix

```
╔════════════════════════════════════════════════════════════════════════╗
║                    RECOVERY SIGNAL COMBINATIONS                        ║
╠═══════════╦═══════════╦═══════════╦══════════════╦════════════════════╣
║   DOMS    ║    RPE    ║   SLEEP   ║   SIGNAL     ║    OUTPUT          ║
╠═══════════╬═══════════╬═══════════╬══════════════╬════════════════════╣
║ 0 (none)  ║ 6.5 (low) ║ 9 (excel) ║INCREASE_LOAD ║ 100kg → 101.25kg   ║
║ Excellent recovery                                 │ (microload +)      ║
╠═══════════╬═══════════╬═══════════╬══════════════╬════════════════════╣
║ 2 (mild)  ║ 8.5 (opt) ║ 7 (good)  ║STEADY_PROG   ║ 100kg × 8 →        ║
║ Good recovery                                      │ 100kg × 9 (reps)   ║
╠═══════════╬═══════════╬═══════════╬══════════════╬════════════════════╣
║ 5 (mod)   ║ 7.5 (low) ║ 7 (good)  ║MIRROR        ║ 100kg × 8 →        ║
║ Adequate recovery                                  │ 100kg × 8 (same)   ║
╠═══════════╬═══════════╬═══════════╬══════════════╬════════════════════╣
║ 3 (mild)  ║ 6.5 (low) ║ 3 (poor)  ║MIRROR/REDUCE ║ 100kg → 99.25kg    ║
║ Poor sleep overrides good DOMS/RPE                │ (2.5% reduction)   ║
╠═══════════╬═══════════╬═══════════╬══════════════╬════════════════════╣
║ 8 (SEV!)  ║ 6.5 (low) ║ 9 (excel) ║REDUCE_LOAD   ║ 100kg → 97.5kg     ║
║ DOMS VETO! Overrides all positive signals          │ (HARD CONSTRAINT)  ║
╠═══════════╬═══════════╬═══════════╬══════════════╬════════════════════╣
║ 5 (mod)   ║ 9.5(high) ║ 2 (poor)  ║REDUCE_LOAD   ║ 100kg → 97.5kg     ║
║ Triple negative: DOMS + high RPE + poor sleep      │ OR MIRROR          ║
╚═══════════╩═══════════╩═══════════╩══════════════╩════════════════════╝
```

---

## Signal → Action Mapping

```
┌──────────────────────┐
│ REDUCE_LOAD          │
│ Recovery: -0.8 to    │
│           -0.3       │
├──────────────────────┤
│ Action: Decrease     │
│ weight by ≥2.5%      │
│ Hold reps same       │
│                      │
│ Use case: Severe     │
│ DOMS, high RPE +     │
│ poor sleep, or       │
│ accumulated fatigue  │
└──────────────────────┘

┌──────────────────────┐
│ MIRROR_SESSION       │
│ Recovery: -0.3 to    │
│           0.1        │
├──────────────────────┤
│ Action: Exact same   │
│ weight × reps as     │
│ last session         │
│                      │
│ Use case: Moderate   │
│ DOMS, adequate       │
│ sleep, normal effort │
└──────────────────────┘

┌──────────────────────┐
│ STEADY_PROGRESSION   │
│ Recovery: 0.1 to     │
│           0.4        │
├──────────────────────┤
│ Action: Same weight, │
│ add 1 rep            │
│ (OR small weight +   │
│  same reps)          │
│                      │
│ Use case: Good       │
│ recovery, optimal    │
│ RPE (8-9)            │
└──────────────────────┘

┌──────────────────────┐
│ INCREASE_LOAD        │
│ Recovery: >0.4       │
├──────────────────────┤
│ Action: Increase     │
│ weight by microload  │
│ (e.g., +1.25kg)      │
│ Hold reps same       │
│                      │
│ Use case: Excellent  │
│ recovery across all  │
│ signals (DOMS,       │
│ RPE, sleep)          │
└──────────────────────┘
```

---

## Weighting Sensitivity Analysis

```
Scenario: Weight = 100 kg, last RPE = 8.5 (optimal)

CHANGING DOMS (keeping RPE 8.5, Sleep 7 constant):
┌────────┬──────────────┬────────────────────┐
│ DOMS   │ Weighting    │ Recommendation     │
├────────┼──────────────┼────────────────────┤
│ 0      │ 50% DOMS +ve │ 101.25 kg (prog)   │
│ 2      │ 50% DOMS +ve │ 101.25 kg (prog)   │
│ 4      │ Moderate -ve │ 100 kg (mirror)    │
│ 6      │ Moderate -ve │ 100 kg (mirror)    │
│ 7      │ SEVERE veto  │ 97.5 kg (reduce!)  │
│ 8      │ SEVERE veto  │ 97.5 kg (reduce!)  │
└────────┴──────────────┴────────────────────┘

CHANGING RPE (keeping DOMS 2, Sleep 7 constant):
┌────────┬──────────────┬────────────────────┐
│ RPE    │ Interpretation  │ Recommendation    │
├────────┼──────────────┼────────────────────┤
│ 6.5    │ Low; ready   │ 101.25 kg (prog)   │
│ 7.5    │ Good; ready  │ 101.25 kg (prog)   │
│ 8.5    │ Optimal      │ 100 kg × 9 (reps)  │
│ 9.0    │ Upper optimal│ 100 kg × 9 (reps)  │
│ 9.5    │ High; tired  │ 100 kg (mirror)    │
│ 9.8    │ Very high    │ 97.5 kg (reduce)   │
└────────┴──────────────┴────────────────────┘

CHANGING SLEEP (keeping DOMS 2, RPE 8.5 constant):
┌────────┬──────────────┬────────────────────┐
│ Sleep  │ Effect       │ Recommendation     │
├────────┼──────────────┼────────────────────┤
│ 2      │ Dampen 30%   │ 100 kg (mirror)    │
│ 4      │ Dampen 20%   │ 100 kg (mirror)    │
│ 6      │ Neutral      │ 100 kg × 9 (reps)  │
│ 7      │ Amplify 10%  │ 100 kg × 9 (reps)  │
│ 9      │ Amplify 40%  │ 101.25 kg (prog)   │
│ 10     │ Amplify 50%  │ 101.25 kg (prog)   │
└────────┴──────────────┴────────────────────┘
```

---

## Implementation Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    USER FEEDBACK                             │
│         (DOMS: 1-10, RPE: 1-10, Sleep: 1-10)                │
└────────────────────┬────────────────────────────────────────┘
                     ↓
        ┌────────────────────────┐
        │ ProgressionInput       │
        │ • weight               │
        │ • reps                 │
        │ • RPE                  │
        │ • DOMS                 │
        │ • SleepQuality         │
        └────────┬───────────────┘
                 ↓
        ┌────────────────────────┐
        │ ProgressionPolicy      │
        │ (loaded from JSON)      │
        │ • weights (50/30/20)   │
        │ • thresholds           │
        └────────┬───────────────┘
                 ↓
    ┌────────────────────────────────┐
    │ RecoveryAssessment             │
    │ 1. Score DOMS [-0.8 to +0.1]  │
    │ 2. Score RPE  [-0.6 to +0.5]  │
    │ 3. Apply Sleep [×0.5 to ×1.5] │
    │ 4. Weighted combine            │
    │ 5. Determine Signal            │
    └────────┬───────────────────────┘
             ↓
    ┌────────────────────────────────┐
    │ ProgressionEngine              │
    │ • computeNextWeight()          │
    │ • computeNextReps()            │
    └────────┬───────────────────────┘
             ↓
    ┌────────────────────────────────┐
    │ Prescription                   │
    │ • SetPrescription[              │
    │   - weight (rounded to 0.25kg) │
    │   - reps                       │
    │ ]                              │
    └────────┬───────────────────────┘
             ↓
    ┌────────────────────────────────┐
    │ OUTPUT TO ATHLETE              │
    │ "Next session: 100kg × 8 reps" │
    └────────────────────────────────┘
```

---

## Recovery Score Normalization

```
Raw Signals from -1 to +1:

DOMS Signal              RPE Signal              Sleep Signal
┌────────────────┐      ┌────────────────┐      ┌────────────────┐
│ +0.1 (none)    │      │ +0.5 (low 6.5) │      │ +0.4 (sleep 9) │
│ 0.0 (mild)     │      │ +0.2 (opt 8.5) │      │ 0.0 (sleep 5)  │
│ -0.5 (moderate)│  ×50%│ -0.4 (high 9.5)│  ×30%│ -0.5 (poor 2)  │  ×20%
│ -0.8 (severe)  │      │ -0.6 (very)    │      │               │
└────────────────┘      └────────────────┘      └────────────────┘

                    ↓ WEIGHTED SUM
        
    (-0.5 × 0.5) + (+0.2 × 0.3) + (0.0 × 0.2)
  = -0.25 + 0.06 + 0.0
  = -0.19

                    ↓ NORMALIZED

         FINAL RECOVERY SCORE: -0.19
    (Slightly negative → MIRROR_SESSION)
```


