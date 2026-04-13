package com.spotme.config;

import com.spotme.application.usecase.CompleteWorkoutSession;
import com.spotme.application.usecase.ComputeNextPrescription;
import com.spotme.application.usecase.GetLatestWorkoutSession;
import com.spotme.application.usecase.ListRecentWorkoutSessions;
import com.spotme.domain.port.RulesConfigPort;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class UseCaseWiringConfig {

    @Bean
    public RulesConfigPort rulesConfigPort(ObjectMapper objectMapper) {
        return version -> {
            try {
                var resource = new ClassPathResource("progressionalgorithm.json");
                return objectMapper.readTree(resource.getInputStream());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load progression rules JSON", e);
            }
        };
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



