# 📘 SpotMe – Adaptive Hypertrophy Training App (Backend)

SpotMe is a beginner-friendly training companion designed to function as your **gym partner**, **progress tracker**, and **personal trainer**.  
It uses modern hypertrophy principles and real-time user feedback (RPE, DOMS, sleep) to dynamically adjust training programs and provide personalised progression.

This repository contains the **backend**, written in **Java**, following **Domain-Driven Design (DDD)** principles, with future support for both **gRPC** and **REST APIs**.  
A React frontend will be developed in a separate project.

---

## 🧱 Architecture Overview

The backend is structured around Domain-Driven Design with clear separation between **domain**, **application**, and **infrastructure** layers.


### **Domain Layer**
The pure business logic of the application, independent of frameworks:

- **Program Aggregate**  
  Represents a user’s entire training program, composed of multiple blocks.

- **TrainingBlock**  
  Represents a phase of training (e.g., hypertrophy, strength, deload).  
  Each block is typed using `BlockType` for clear periodisation.

- **Workout Exercises**  
  Represent exercises performed within a block or workout, using `Exercise` and `ExerciseSet`.

- **Exercise Definitions**  
  A static library of exercises including metadata like movement pattern and muscle groups.

The distinction between **ExerciseDefinition** (what an exercise *is*) and **Exercise** (how it appears in a specific workout) supports flexible and adaptive programming.

### **Application Layer (Planned)**
Will contain use cases such as:

- Creating programs
- Generating adaptive training blocks
- Updating workouts based on user feedback
- Managing progression logic

### **Infrastructure Layer (Planned)**
Will include:

- Repository implementations
- Database integration
- gRPC and REST controllers
- Mappers and adapters

---

###  **Project Structure**
```
com.spotme
├─ domain                                   # Core domain (pure Java, no frameworks)
│  ├─ shared
│  │  ├─ value                              # Generic value objects
│  │  │  ├─ Name.java
│  │  │  └─ Percentage.java
│  │  ├─ id                                 # Strongly typed identifiers
│  │  │  ├─ ProgramId.java
│  │  │  ├─ BlockId.java
│  │  │  ├─ ExerciseId.java
│  │  │  ├─ UserId.java
│  │  │  └─ FeedbackId.java
│  │  └─ event                              # Domain events
│  │     ├─ ProgramUpdated.java
│  │     ├─ ExerciseCompleted.java
│  │     └─ FeedbackProvided.java
│  │
│  ├─ program                                # Training program bounded context
│  │  ├─ Program.java
│  │  ├─ ProgramRepository.java               # Port
│  │  ├─ block
│  │  │  ├─ Block.java
│  │  │  ├─ BlockType.java
│  │  │  └─ BlockPolicy.java
│  │  └─ policy
│  │     └─ ProgressionPolicy.java
│  │
│  ├─ exercise                               # Exercises & definitions
│  │  ├─ Exercise.java
│  │  ├─ ExerciseDefinition.java
│  │  ├─ ExerciseRepository.java              # Port
│  │  └─ category
│  │     ├─ MuscleGroup.java
│  │     └─ EquipmentType.java
│  │
│  ├─ feedback                               # User feedback domain
│  │  ├─ Feedback.java
│  │  ├─ RpeValue.java
│  │  ├─ DomsLevel.java
│  │  └─ SleepQuality.java
│  │
│  └─ user                                   # User aggregate (athlete)
│     ├─ User.java
│     ├─ AthleteProfile.java
│     └─ UserRepository.java                 # Port
│
├─ application                               # Use cases → orchestrate domain
│  ├─ program
│  │  ├─ CreateProgramUseCase.java
│  │  ├─ UpdateProgramProgressUseCase.java
│  │  └─ GenerateNextBlockUseCase.java
│  ├─ exercise
│  │  ├─ AddExerciseDefinitionUseCase.java
│  │  └─ GetExerciseCatalogueUseCase.java
│  ├─ feedback
│  │  ├─ SubmitFeedbackUseCase.java
│  │  └─ EvaluateFeedbackService.java
│  └─ user
│     └─ RegisterUserUseCase.java
│
├─ infrastructure                            # Adapters to technical concerns
│  ├─ persistence
│  │  ├─ jpa
│  │  │  ├─ entities
│  │  │  │  ├─ ProgramEntity.java
│  │  │  │  ├─ BlockEntity.java
│  │  │  │  ├─ ExerciseEntity.java
│  │  │  │  └─ UserEntity.java
│  │  │  ├─ converters
│  │  │  │  └─ DomainIdConverters...
│  │  │  ├─ repositories
│  │  │  │  ├─ SpringDataProgramJpa.java
│  │  │  │  ├─ SpringDataExerciseJpa.java
│  │  │  │  └─ SpringDataUserJpa.java
│  │  │  ├─ mappers
│  │  │  │  ├─ ProgramMapper.java
│  │  │  │  └─ ExerciseMapper.java
│  │  │  └─ JpaProgramRepository.java        # Adapter implementing port
│  │  └─ migrations
│  │     └─ Flyway scripts...
│  │
│  ├─ messaging
│  │  └─ events → Kafka/SQS adapters
│  │
│  └─ grpc / rest
│     ├─ grpc
│     │  └─ (gRPC service implementations)
│     └─ rest
│        └─ (Spring REST adapters)
│
├─ api                                       # Delivery layer (REST or gRPC)
│  ├─ web
│  │  ├─ controllers
│  │  │  ├─ ProgramController.java
│  │  │  ├─ ExerciseController.java
│  │  │  └─ FeedbackController.java
│  │  ├─ dto
│  │  │  ├─ ProgramResponse.java
│  │  │  ├─ ExerciseResponse.java
│  │  │  └─ FeedbackRequest.java
│  │  ├─ mappers
│  │  │  ├─ ProgramDtoMapper.java
│  │  │  └─ ExerciseDtoMapper.java
│  │  └─ filters
│  │     └─ LoggingFilter.java
│  └─ config
│     └─ ApiConfig.java
│
├─ support                                   # Cross-cutting utilities
│  ├─ exceptions
│  │  ├─ DomainException.java
│  │  ├─ NotFoundException.java
│  │  └─ ValidationException.java
│  ├─ util
│  │  └─ DomainUtils.java
│  └─ logging
│     └─ StructuredLogger.java
│
└─ bootstrap
└─ Application.java                        # Spring Boot entry point
```

## 🎯 Core Concept

SpotMe adapts dynamically to the user’s real-world recovery and performance.  
After each workout, users provide feedback:

- **RPE** (Rate of Perceived Exertion)
- **DOMS** soreness
- **Sleep quality**

This feedback influences adjustments to:

- Training volume
- Intensity and load
- RPE targets
- Exercise selection
- Block transitions

The goal is to provide smarter progression than static templates.

---

## 🚀 Current Status

- Domain model foundations established
- ExerciseDefinition and Exercise aggregates implemented
- Strongly-typed identifiers across the domain
- Program → Block structure in place
- Architecture prepared for future expansion (gRPC/REST/persistence)

---

## 🛠 Roadmap

- Implement Workout aggregate
- Add UserFeedback models
- Build adaptive progression engine
- Implement Application layer use cases
- Add gRPC APIs
- Add REST APIs where appropriate
- Implement persistence layer
- Build React frontend
- Add analytics, achievements, and social features

