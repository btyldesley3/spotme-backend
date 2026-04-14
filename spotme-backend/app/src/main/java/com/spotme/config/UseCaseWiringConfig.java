package com.spotme.config;

import com.spotme.application.usecase.CompleteWorkoutSession;
import com.spotme.application.usecase.ComputeNextPrescription;
import com.spotme.application.usecase.GetLatestWorkoutSession;
import com.spotme.application.usecase.ListRecentWorkoutSessions;
import com.spotme.application.usecase.LogSet;
import com.spotme.application.usecase.StartWorkoutSession;
import com.spotme.domain.port.RulesConfigPort;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseWiringConfig {


    @Bean
    public StartWorkoutSession startWorkoutSession(WorkoutWritePort write) {
        return new StartWorkoutSession(write);
    }

    @Bean
    public LogSet logSet(WorkoutReadPort read, WorkoutWritePort write) {
        return new LogSet(read, write);
    }

    @Bean
    public ComputeNextPrescription computeNextPrescription(WorkoutReadPort read,
                                                           WorkoutWritePort write,
                                                           RulesConfigPort rules) {
        return new ComputeNextPrescription(read, write, rules);
    }

    @Bean
    public CompleteWorkoutSession completeWorkoutSession(WorkoutReadPort read,
                                                         WorkoutWritePort write) {
        return new CompleteWorkoutSession(read, write);
    }

    @Bean
    public GetLatestWorkoutSession getLatestWorkoutSession(WorkoutReadPort read) {
        return new GetLatestWorkoutSession(read);
    }

    @Bean
    public ListRecentWorkoutSessions listRecentWorkoutSessions(WorkoutReadPort read) {
        return new ListRecentWorkoutSessions(read);
    }
}



