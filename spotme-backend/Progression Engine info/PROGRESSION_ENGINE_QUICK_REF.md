# Progression Engine Quick Reference

## Using the Enhanced Engine

### Minimal Example

```java
import com.spotme.domain.model.metrics.*;
import com.spotme.domain.rules.*;
import com.spotme.domain.model.plan.Prescription;

// Inputs from user feedback after workout
var doms = new Doms(5);              // 0-10 soreness level
var rpe = new Rpe(8.5);              // Rate of Perceived Exertion
var sleep = new SleepQuality(7);     // Last night's sleep quality

// Last session's performance
var input = new ProgressionInput(
    100.0,           // kg (last weight)
    8,               // reps (last reps)
    rpe,
    doms,
    sleep
);

// Get policy from JSON (loaded by ClasspathRulesConfigAdapter)
var policy = ProgressionPolicy.fromJson(rulesJson, "barbell_compound");

// Generate next prescription
var engine = new ProgressionEngine();
var prescription = engine.nextFor(exerciseId, input, policy);

// Apply to next session
var nextWeight = prescription.sets().get(0).prescribedWeightKg();
var nextReps = prescription.sets().get(0).prescribedReps();
```

---

## Decision Flow at a Glance

```
┌─ Input: lastWeight, lastReps, RPE, DOMS, Sleep
│
├─ 1. Create RecoveryAssessment
│     └─ Combines all three signals with weights
│
├─ 2. Check DOMS Level
│     ├─ SEVERE (≥7) → Signal = REDUCE_LOAD (or MIRROR)
│     ├─ MODERATE (4-6) → Signal = MIRROR (unless excellent sleep)
│     └─ MILD/NONE (0-3) → Continue to RPE check
│
├─ 3. Check RPE Level (if not SEVERE DOMS)
│     ├─ Low (≤7.0) → Signal = INCREASE_LOAD
│     ├─ Optimal (8.0-9.0) → Signal = STEADY_PROGRESSION
│     └─ High (≥9.5) → Signal = REDUCE_LOAD
│
├─ 4. Apply Sleep Multiplier
│     ├─ Poor sleep (≤4) → Dampen recovery by 30%+
│     ├─ Fair sleep (5-6) → Neutral
│     └─ Excellent sleep (7-10) → Amplify recovery by 0-40%
│
├─ 5. Output Signal
│     ├─ REDUCE_LOAD     → weight - (2.5% minimum)
│     ├─ MIRROR_SESSION  → same weight, same reps
│     ├─ STEADY_PROG     → same weight, +1 rep (if optimal RPE)
│     └─ INCREASE_LOAD   → weight + microload
│
└─ Return Prescription (weight, reps)
```

---

## Common Patterns

### Pattern 1: Athlete Has Severe DOMS
```java
var input = new ProgressionInput(100, 8, new Rpe(7.0), new Doms(8), new SleepQuality(9));
// Result: 100 kg × 8 reps (MIRROR_SESSION)
// Reason: Severe DOMS overrides excellent RPE/sleep
```

### Pattern 2: Athlete Has Poor Sleep
```java
var input = new ProgressionInput(100, 8, new Rpe(6.5), new Doms(2), new SleepQuality(2));
// Result: 100 kg × 8 reps (MIRROR_SESSION or REDUCE)
// Reason: Poor sleep dampens recovery despite low DOMS/RPE
```

### Pattern 3: Green Light for Progression
```java
var input = new ProgressionInput(100, 8, new Rpe(6.5), new Doms(1), new SleepQuality(8));
// Result: 101.25 kg × 8 reps (INCREASE_LOAD)
// Reason: All signals positive → add microload
```

### Pattern 4: Building Volume Before Intensity
```java
var input = new ProgressionInput(100, 8, new Rpe(8.5), new Doms(3), new SleepQuality(7));
// Result: 100 kg × 9 reps (STEADY_PROGRESSION)
// Reason: Optimal RPE + good recovery → increase reps first
// (Next session: if they hit 9 reps easily → increase weight)
```

---

## Configuration Tuning

### Default Policy Weights
```json
"weighting": {
  "doms_impact": 0.5,        // 50% → dominant factor
  "rpe_influence": 0.3,      // 30% → secondary
  "sleep_influence": 0.2     // 20% → amplifier
}
```

### Adjusting for Different Cohorts

**Beginners** (more conservative):
```json
{
  "doms_impact": 0.6,        // Increase DOMS sensitivity
  "rpe_influence": 0.25,
  "sleep_influence": 0.15,
  "severeDomsThreshold": 6   // Lower threshold
}
```

**Advanced Athletes** (more aggressive):
```json
{
  "doms_impact": 0.4,        // Reduce DOMS penalty
  "rpe_influence": 0.4,      // Trust their RPE reading
  "sleep_influence": 0.2,
  "severeDomsThreshold": 8   // Higher threshold
}
```

---

## Testing Your Changes

Run domain tests:
```bash
mvn test -pl domain
```

Expected output:
```
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

---

## Metric Reference

### DOMS Levels
| Range | Category | Interpretation |
|-------|----------|---|
| 0 | None | No soreness; ready for normal progression |
| 1–3 | Mild | Minor soreness; proceed normally |
| 4–6 | Moderate | Noticeable soreness; conservative progression |
| 7–10 | Severe | Significant soreness; mirror or reduce |

### RPE Scale
| Value | Meaning |
|-------|---------|
| 6–7 | Easy; reps in reserve |
| 8–9 | Hard; 1–2 reps to failure |
| 9–9.5 | Very hard; 0–1 rep to failure |
| 10 | Maximum effort; failure reached |

### Sleep Quality
| Range | Category | Effect |
|-------|----------|--------|
| 0–2 | Very Poor | Severe recovery dampening (0.5× multiplier) |
| 3–5 | Poor/Fair | Moderate dampening (0.7–0.8× multiplier) |
| 6–8 | Good | Neutral to positive (1.0–1.2× multiplier) |
| 9–10 | Excellent | Strong amplification (1.4–1.5× multiplier) |

---

## Troubleshooting

**Q: Weight never increases, even with great signals**  
A: Check `severeDomsThreshold` in policy. If DOMS history is high, you need a deload week first.

**Q: Progression too aggressive despite moderate DOMS**  
A: Increase `doms_impact` weight (e.g., 0.6 instead of 0.5) or lower `moderateDomsThreshold`.

**Q: Sleep isn't affecting progression**  
A: Check that `sleep_influence` weight > 0. Default is 0.2 (20%); boost to 0.3 if sleep matters more.

**Q: Tests fail on record signature**  
A: ProgressionInput now requires 5 parameters (added SleepQuality). Update any test creating ProgressionInput.

---

## Next Steps

1. Integrate into use cases (application layer)
2. Wire into gRPC service handlers (adapters/in.grpc)
3. Persist progression history for trend analysis
4. Add deload triggers (consecutive high-stress weeks)
5. Extend with HRV or other biometric inputs

