# Enhanced Progression Engine – Complete Documentation Index

## 📚 Documentation Files

This directory now contains comprehensive documentation for the enhanced progression engine that combines DOMS, RPE, and sleep quality into intelligent training load recommendations.

### 1. **PROGRESSION_ENGINE_DESIGN.md** (Primary Architecture Document)
**Purpose:** Complete technical design document  
**Audience:** Architects, senior developers, code reviewers  
**Length:** 220+ lines  
**Contains:**
- Overview and core architecture
- Essential components (5 new/enhanced classes)
- Detailed decision algorithm (6-step process)
- Weighting philosophy & rationale
- Real-world scenario walkthroughs
- Configuration structure
- 14 unit test descriptions
- Future enhancement roadmap

**Read this if:** You need to understand HOW and WHY the system works

---

### 2. **PROGRESSION_ENGINE_QUICK_REF.md** (Developer Cheatsheet)
**Purpose:** Quick-start guide for developers using the engine  
**Audience:** Developers implementing integration, adapters, use cases  
**Length:** 180+ lines  
**Contains:**
- Minimal working code example
- Decision flow diagram at a glance
- 4 common usage patterns
- Configuration tuning guide (beginner vs. advanced profiles)
- Metric reference tables
- Testing instructions
- Troubleshooting Q&A

**Read this if:** You need to integrate the engine or understand specific scenarios

---

### 3. **PROGRESSION_ENGINE_DIAGRAMS.md** (Visual Reference)
**Purpose:** ASCII diagrams and flowcharts for visual learners  
**Audience:** Anyone (visual reference)  
**Length:** 250+ lines  
**Contains:**
- Decision tree flowchart (ASCII art)
- Weight factor influence diagram
- Scenario comparison matrix (6 common cases)
- Signal → Action mapping
- Weighting sensitivity analysis
- Implementation architecture diagram
- Recovery score normalization breakdown

**Read this if:** You prefer visual explanations over text

---

### 4. **VERIFICATION_CHECKLIST.md** (Completeness Verification)
**Purpose:** Comprehensive checklist of what was implemented  
**Audience:** Project managers, QA, stakeholders  
**Length:** 200+ lines  
**Contains:**
- ✅ Implementation status (7 features complete)
- ✅ Build verification results
- ✅ Test results summary (15 tests passing)
- Files created/modified (detailed list)
- Integration readiness checklist
- Performance & architecture notes
- Code quality metrics

**Read this if:** You need proof of completeness or integration planning

---

### 5. **AGENTS.md** (AI Agent Instructions)
**Purpose:** Codebase guide for AI coding agents  
**Audience:** Future AI agents working on SpotMe codebase  
**Length:** 200+ lines  
**Contains:**
- Project architecture overview
- Dependency flow constraints
- Key modules and responsibilities
- Essential patterns and gotchas
- Critical workflows and commands
- Key files to know

**Read this if:** You're a future AI agent or need codebase guidelines

---

## 🗂️ File Structure

```
spotme-backend/
├── domain/
│   ├── src/main/java/com/spotme/domain/
│   │   ├── model/metrics/
│   │   │   ├── Rpe.java (existing)
│   │   │   ├── Doms.java (existing)
│   │   │   └── SleepQuality.java ⭐ NEW
│   │   ├── rules/
│   │   │   ├── ProgressionInput.java (enhanced)
│   │   │   ├── ProgressionPolicy.java (enhanced)
│   │   │   ├── ProgressionEngine.java (refactored)
│   │   │   └── RecoveryAssessment.java ⭐ NEW
│   │   └── port/
│   │       ├── RulesConfigPort.java (existing)
│   │       └── ... (other ports)
│   └── src/test/java/com/spotme/domain/
│       ├── ProgressionEngineTest.java (updated)
│       └── rules/
│           └── ProgressionEngineTest.java ⭐ NEW (14 tests)
│
├── adapters/
│   └── out.persistence/
│       └── src/main/java/.../InMemoryWorkoutAdapter.java (updated)
│
├── app/
│   └── src/main/resources/
│       └── progressionalgorithm.json (enhanced)
│
├── AGENTS.md (existing)
├── PROGRESSION_ENGINE_DESIGN.md ⭐ NEW
├── PROGRESSION_ENGINE_QUICK_REF.md ⭐ NEW
├── PROGRESSION_ENGINE_DIAGRAMS.md ⭐ NEW
├── VERIFICATION_CHECKLIST.md ⭐ NEW
└── PROGRESSION_ENGINE_INDEX.md (this file) ⭐ NEW
```

---

## 🎯 Quick Navigation by Use Case

### "I need to understand the system design"
1. Start: **AGENTS.md** (architecture overview)
2. Then: **PROGRESSION_ENGINE_DESIGN.md** (complete design)
3. Then: **PROGRESSION_ENGINE_DIAGRAMS.md** (visual reference)

### "I need to implement gRPC integration"
1. Start: **PROGRESSION_ENGINE_QUICK_REF.md** (code example)
2. Consult: **PROGRESSION_ENGINE_DESIGN.md** (algorithm details)
3. Reference: **VERIFICATION_CHECKLIST.md** (integration checklist)

### "I need to tune configuration for a specific athlete cohort"
1. Start: **PROGRESSION_ENGINE_QUICK_REF.md** (configuration tuning section)
2. Understand: **PROGRESSION_ENGINE_DESIGN.md** (weighting rationale)
3. Reference: **PROGRESSION_ENGINE_DIAGRAMS.md** (sensitivity analysis)

### "I need to verify everything was implemented correctly"
1. Review: **VERIFICATION_CHECKLIST.md** (✅ completeness)
2. Check: **PROGRESSION_ENGINE_DESIGN.md** (test descriptions)
3. Run: `mvn test -pl domain` (verify all 15 tests pass)

### "I need to debug a specific progression decision"
1. Start: **PROGRESSION_ENGINE_DIAGRAMS.md** (decision tree)
2. Reference: **PROGRESSION_ENGINE_QUICK_REF.md** (scenario patterns)
3. Check: **PROGRESSION_ENGINE_DESIGN.md** (algorithm details)

---

## 📊 Core Components at a Glance

| Component | Type | Purpose | Lines | Status |
|-----------|------|---------|-------|--------|
| SleepQuality | Value Object | Encapsulates sleep metric + recovery factor | 18 | ✅ |
| RecoveryAssessment | Domain Service | Combines 3 signals into unified recovery score | 150+ | ✅ |
| ProgressionInput | Record (Enhanced) | Input to engine; now includes sleep | - | ✅ |
| ProgressionPolicy | Record (Enhanced) | Config thresholds & weights from JSON | 60+ | ✅ |
| ProgressionEngine | Stateless Service | Core decision logic | 100+ | ✅ |
| Test Suite | JUnit 5 + AssertJ | 14 comprehensive tests | 350+ | ✅ All Pass |

---

## 🔑 Key Decision Principles

1. **DOMS is a hard constraint** (50% weight)
   - Severe DOMS (≥7) forces recovery score to -0.8
   - Overrides all positive signals from RPE/sleep

2. **RPE is secondary feedback** (30% weight)
   - Low RPE (≤7) indicates readiness
   - Optimal RPE (8-9) maintains steady progression
   - High RPE (≥9.5) signals fatigue

3. **Sleep is an amplifier** (20% weight)
   - Multiplies recovery factor [0.5, 1.5]
   - Poor sleep dampens; excellent sleep amplifies

4. **Weights are configurable**
   - Default: 50/30/20, but tunable per cohort
   - Beginner profiles: More DOMS sensitivity
   - Advanced profiles: Trust RPE more

5. **Signals output load adjustments**
   - `REDUCE_LOAD` → -2.5% weight minimum
   - `MIRROR_SESSION` → Same weight/reps
   - `STEADY_PROGRESSION` → +1 rep (before weight)
   - `INCREASE_LOAD` → +microload increment

---

## ✅ Verification Checklist

- [x] All domain logic implemented and immutable
- [x] All tests passing (15/15 ✓)
- [x] Full project builds successfully
- [x] No Spring dependencies in domain
- [x] RecoveryAssessment is pure domain logic
- [x] Configuration supports dynamic policies
- [x] Documentation is comprehensive
- [x] Code examples are correct
- [x] Diagrams are clear and accurate
- [x] Future integration path is clear

---

## 🚀 Next Steps

### Immediate (Week 1)
1. **Create Application Use Case**
   ```
   application/src/main/java/com/spotme/application/workout/
   └── ComputeNextPrescriptionUseCase.java
   ```

2. **Wire into gRPC Service**
   ```
   adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/
   └── WorkoutServiceImpl.java (call use case)
   ```

### Short-term (Week 2-3)
1. Persist progression history (JPA entity)
2. Add deload triggering logic
3. Create REST API endpoints (optional)

### Medium-term (Month 2)
1. Add HRV or other biometric inputs
2. Implement trend analysis
3. Build analytics dashboard

### Long-term (Month 3+)
1. Machine learning for personalized thresholds
2. Social features (compare with peers)
3. Mobile app notifications

---

## 📞 Support & Questions

### "How do I run the tests?"
```bash
mvn test -pl domain
```
Expected: 15 tests pass (1 legacy + 14 new)

### "How do I add a new recovery signal?"
1. Add metric class in `domain/model/metrics/`
2. Extend `RecoveryAssessment` calculation
3. Add tests
4. Update documentation

### "Can I change the weights?"
Yes! Modify `progressionalgorithm.json`:
```json
"weighting": {
  "doms_impact": 0.6,      // Increase DOMS sensitivity
  "rpe_influence": 0.25,   // Decrease RPE weight
  "sleep_influence": 0.15
}
```

### "What if the engine recommends the same weight every session?"
Likely causes:
1. DOMS is consistently moderate (4-6)
2. Sleep is consistently poor (≤4)
3. RPE is consistently high (≥9)

**Solution:** Check athlete recovery habits; may need deload week

---

## 🎓 Learning Resources

- **Decision Tree Logic:** PROGRESSION_ENGINE_DIAGRAMS.md
- **Example Scenarios:** PROGRESSION_ENGINE_DESIGN.md (page 7+)
- **Configuration Tuning:** PROGRESSION_ENGINE_QUICK_REF.md (section 3)
- **Unit Tests:** domain/src/test/java/.../ProgressionEngineTest.java
- **Domain Models:** domain/src/main/java/.../metrics/ and rules/

---

## 📝 Document Version History

| Date | Document | Version | Status |
|------|----------|---------|--------|
| 2026-04-13 | PROGRESSION_ENGINE_DESIGN.md | 1.0 | Complete |
| 2026-04-13 | PROGRESSION_ENGINE_QUICK_REF.md | 1.0 | Complete |
| 2026-04-13 | PROGRESSION_ENGINE_DIAGRAMS.md | 1.0 | Complete |
| 2026-04-13 | VERIFICATION_CHECKLIST.md | 1.0 | Complete |
| 2026-04-13 | PROGRESSION_ENGINE_INDEX.md | 1.0 | Complete |

---

## 🏁 Summary

**Status:** ✅ **COMPLETE & PRODUCTION-READY**

The enhanced progression engine is fully implemented, tested, documented, and ready for integration. It provides sophisticated yet configurable load recommendations based on DOMS (hard constraint), RPE (secondary), and sleep quality (amplifier).

All 15 tests pass. Full project builds successfully. Documentation is comprehensive and accessible.

Ready to integrate into application and gRPC layers.

**Next action:** Create `ComputeNextPrescriptionUseCase` in application layer to orchestrate the engine.

