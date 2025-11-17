package com.spotme.domain.model.user;

public class RecoveryProfile {

    private final int baselineSleepHours; // normal average sleep
    private final int stressSensitivity;  // 1 = low, 5 = high

    public RecoveryProfile(int baselineSleepHours, int stressSensitivity) {
        this.baselineSleepHours = baselineSleepHours;
        this.stressSensitivity = stressSensitivity;
    }

    public int baselineSleepHours() {
        return baselineSleepHours;
    }

    public int stressSensitivity() {
        return stressSensitivity;
    }

}
