package com.spotme.config;

import com.spotme.application.usecase.CompleteWorkoutSession;
import com.spotme.application.usecase.ComputeNextPrescription;
import com.spotme.application.usecase.GetUserProfile;
import com.spotme.application.usecase.GetLatestWorkoutSession;
import com.spotme.application.usecase.ListRecentWorkoutSessions;
import com.spotme.application.usecase.LoginUser;
import com.spotme.application.usecase.LogSet;
import com.spotme.application.usecase.RegisterUser;
import com.spotme.application.usecase.RegisterWithCredentials;
import com.spotme.application.usecase.StartWorkoutSession;
import com.spotme.domain.port.AlphaAccessPort;
import com.spotme.domain.port.CredentialReadPort;
import com.spotme.domain.port.CredentialWritePort;
import com.spotme.domain.port.PasswordHashPort;
import com.spotme.domain.port.RulesConfigPort;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.port.UserWritePort;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseWiringConfig {


    @Bean
    public StartWorkoutSession startWorkoutSession(UserReadPort users, WorkoutWritePort write) {
        return new StartWorkoutSession(users, write);
    }

    @Bean
    public LogSet logSet(UserReadPort users, WorkoutReadPort read, WorkoutWritePort write) {
        return new LogSet(users, read, write);
    }

    @Bean
    public ComputeNextPrescription computeNextPrescription(UserReadPort users,
                                                           WorkoutReadPort read,
                                                           WorkoutWritePort write,
                                                           RulesConfigPort rules) {
        return new ComputeNextPrescription(users, read, write, rules);
    }

    @Bean
    public CompleteWorkoutSession completeWorkoutSession(UserReadPort users,
                                                         WorkoutReadPort read,
                                                         WorkoutWritePort write) {
        return new CompleteWorkoutSession(users, read, write);
    }

    @Bean
    public GetLatestWorkoutSession getLatestWorkoutSession(UserReadPort users, WorkoutReadPort read) {
        return new GetLatestWorkoutSession(users, read);
    }

    @Bean
    public ListRecentWorkoutSessions listRecentWorkoutSessions(UserReadPort users, WorkoutReadPort read) {
        return new ListRecentWorkoutSessions(users, read);
    }

    @Bean
    public RegisterUser registerUser(UserWritePort write) {
        return new RegisterUser(write);
    }

    @Bean
    public GetUserProfile getUserProfile(UserReadPort read) {
        return new GetUserProfile(read);
    }

    @Bean
    public RegisterWithCredentials registerWithCredentials(UserWritePort userWrite,
                                                           CredentialReadPort credentialRead,
                                                           CredentialWritePort credentialWrite,
                                                           AlphaAccessPort alphaAccess,
                                                           PasswordHashPort passwordHash) {
        return new RegisterWithCredentials(userWrite, credentialRead, credentialWrite, alphaAccess, passwordHash);
    }

    @Bean
    public LoginUser loginUser(CredentialReadPort credentialRead,
                               PasswordHashPort passwordHash) {
        return new LoginUser(credentialRead, passwordHash);
    }
}



