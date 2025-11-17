package com.spotme.domain.model.program;

import java.util.List;

public class TrainingWeek {

    private final int weekNumber;
    private final List<SessionPlan> sessions;

    public TrainingWeek(int weekNumber, List<SessionPlan> sessions) {
        this.weekNumber = weekNumber;
        this.sessions = List.copyOf(sessions);
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public List<SessionPlan> getSessions() {
        return sessions;
    }

}
