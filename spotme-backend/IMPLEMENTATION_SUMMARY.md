# Workout Aggregate + Persistence Implementation Summary

## Overview
Successfully implemented the **Workout** aggregate (template/blueprint) and complete persistence layer for SpotMe backend following Domain-Driven Design principles and hexagonal architecture.

## What Was Implemented

### 1. Domain Model (`domain/` module)

#### New Files Created:
- **`domain/src/main/java/com/spotme/domain/model/workout/Workout.java`**
  - Aggregate root representing a designed workout template/blueprint
  - Immutable once created (versioning support via `createNextVersion()` factory)
  - Contains: `workoutId`, `blockId`, `weekNumber`, `sessionNumber`, `version`, `notes`, `setPresets` (list of `SetPrescription`)
  - Factory methods: `create()` for new workouts, `createNextVersion()` for versioned templates
  - Validation: positive week/session numbers, non-empty set presets

- **`domain/src/main/java/com/spotme/domain/model/workout/WorkoutId.java`**
  - Strongly-typed UUID-based identifier
  - Factory methods: `random()`, `fromString()`
  - Follows pattern of `ProgramId`, `BlockId`, `ExerciseId`

#### Domain Ports (Interfaces):
- **`domain/src/main/java/com/spotme/domain/port/WorkoutTemplateReadPort.java`**
  - Query operations: `findById()`, `findByBlockId()`, `listWorkoutsInBlock(limit)`
  
- **`domain/src/main/java/com/spotme/domain/port/WorkoutTemplateWritePort.java`**
  - Mutation operations: `save()`, `delete()`

#### Domain Tests:
- **`domain/src/test/java/com/spotme/domain/model/workout/WorkoutTest.java`**
  - 9 unit tests covering:
    - Valid creation with mixed exercises
    - Validation (week/session numbers, empty sets)
    - Versioning (create next version increments version, new ID)
    - Immutability of set presets list
    - Equality based on WorkoutId
    - Multiple exercises support

### 2. Persistence Layer (`adapters/out.persistence/` module)

#### JPA Entities:
- **`adapters/out.persistence/src/main/java/com/spotme/adapters/out/persistence/jpa/entity/WorkoutEntity.java`**
  - Columns: `id` (UUID PK), `block_id` (UUID), `week_number`, `session_number`, `version`, `notes`, `set_presets_json` (TEXT), `created_at`, `updated_at`
  - JPA lifecycle hooks: `@PrePersist`, `@PreUpdate` for audit timestamps

#### JPA Repository:
- **`adapters/out.persistence/src/main/java/com/spotme/adapters/out/persistence/jpa/WorkoutJpaRepository.java`**
  - Extends `CrudRepository<WorkoutEntity, UUID>`
  - Query methods: `findByBlockIdOrderByWeekNumberAscSessionNumberAsc()`, `findWorkoutsByBlockId(blockId, limit)`

#### Mappers:
- **`adapters/out.persistence/src/main/java/com/spotme/adapters/out/persistence/mapper/WorkoutMapper.java`**
  - Bidirectional mapping: `Workout` ↔ `WorkoutEntity`
  - Handles serialization of `SetPrescription` list to/from JSON
  - Inner class `SetPrescriptionJson` for Jackson deserialization
  
- **Updated `adapters/out.persistence/src/main/java/com/spotme/adapters/out/persistence/mapper/IdMapper.java`**
  - Added `WorkoutId` and `BlockId` conversion methods

#### Adapter:
- **`adapters/out.persistence/src/main/java/com/spotme/adapters/out/persistence/PostgresWorkoutTemplateAdapter.java`**
  - Implements both `WorkoutTemplateReadPort` and `WorkoutTemplateWritePort`
  - Leverages MapStruct mapper for entity↔domain conversion
  - Transaction boundaries: read-only for queries, writable for mutations
  - Clean dependency injection pattern

#### Flyway Migration:
- **`adapters/out.persistence/src/main/resources/db/migration/V2__create_workout_templates.sql`**
  - Creates `workouts` table with indexes:
    - Composite index on `(block_id, week_number, session_number)` for efficient querying
    - Index on `block_id` for block-based lookups
  - Denormalized `set_presets_json` field for performance

#### Integration Tests:
- **`adapters/out.persistence/src/test/java/com/spotme/adapters/out/persistence/PostgresWorkoutTemplateAdapterTest.java`**
  - 8 integration tests with embedded Postgres:
    - Save and find by ID
    - Find by block ID (returns sorted by week/session)
    - List with limit
    - Mapper bidirectionality (data preservation)
    - Delete operations
    - Version creation and persistence

### 3. Updated IdMapper
Extended `IdMapper` to support new ID types:
```java
public static UUID toUuid(WorkoutId id)
public static UUID toUuid(BlockId id)
public static WorkoutId toWorkoutId(UUID u)
public static BlockId toBlockId(UUID u)
```

## Architecture Decisions

### Why Workout ≠ WorkoutSession
- **Workout**: Immutable template/blueprint (designed once, versioned)
- **WorkoutSession**: Mutable execution record (user performs, logs data, can be edited)
- **Separate aggregates** allow independent evolution and querying

### JSON Serialization of SetPrescription
- Chose denormalized JSON over nested tables for performance
- `set_presets_json` stored as TEXT field
- MapStruct mapper handles serialization/deserialization
- Jackson handles JSON parsing automatically

### Versioning Pattern
- New version = new `WorkoutId`
- Both v1 and v2 coexist in database
- Enables A/B testing, rollback scenarios
- Copy-on-create semantic prevents mutation issues

### Query Optimization
- Composite index `(block_id, week_number, session_number)` for:
  - Finding all workouts in a block (program browsing)
  - Natural ordering without post-sort
- Separate index on `block_id` for FK constraint efficiency

## Files Created/Modified

### Created (9 files):
1. `domain/.../Workout.java`
2. `domain/.../WorkoutId.java`
3. `domain/.../WorkoutTemplateReadPort.java`
4. `domain/.../WorkoutTemplateWritePort.java`
5. `domain/.../test/WorkoutTest.java`
6. `adapters/.../entity/WorkoutEntity.java`
7. `adapters/.../jpa/WorkoutJpaRepository.java`
8. `adapters/.../mapper/WorkoutMapper.java`
9. `adapters/.../PostgresWorkoutTemplateAdapter.java`
10. `adapters/.../test/PostgresWorkoutTemplateAdapterTest.java`
11. `adapters/.../db/migration/V2__create_workout_templates.sql`

### Modified (1 file):
1. `adapters/.../mapper/IdMapper.java` - Added WorkoutId/BlockId conversion methods

## Testing Strategy

### Domain Tests (WorkoutTest)
- Pure JUnit 5 tests, no Spring
- Tests immutability, factory methods, validation
- Fast execution (<1s)

### Integration Tests (PostgresWorkoutTemplateAdapterTest)
- Uses embedded Postgres (zonky/embedded-postgres)
- Tests full Flyway migration
- Tests MapStruct mapping bidirectionality
- Tests repository query methods
- Validates persistence adapter contract

## Dependencies Added

Already present in project (no new external deps):
- Spring Data JPA
- Jackson
- JUnit 5 / AssertJ
- Embedded Postgres (for testing)

## Next Steps (Optional)

1. **Application Layer**: Create use cases if needed (e.g., `CreateWorkoutUseCase`, `PlanNextWorkoutUseCase`)
2. **gRPC Endpoints**: Add service methods to serve Workout templates to clients
3. **Query Performance**: Monitor `(block_id, week_number, session_number)` index usage
4. **REST API**: Add endpoints for browsing/managing workouts
5. **Event Sourcing**: Consider event-driven architecture if versions need audit trail

## Verification

All files follow:
- ✅ Hexagonal architecture (domain → application → adapters)
- ✅ Unidirectional dependency flow (adapters don't leak into domain)
- ✅ Strongly-typed identifiers
- ✅ Constructor-based immutability
- ✅ MapStruct entity mapping
- ✅ Flyway versioned migrations
- ✅ Comprehensive test coverage
- ✅ Spring transactional boundaries
- ✅ Clean dependency injection


