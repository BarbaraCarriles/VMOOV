package com.example.vmoov;

public class Prescription {
    private int steps;
    private int sessions;
    private int duration;
    private String observations;

    public Prescription() {}

    public Prescription(int steps, int sessions, int duration, String observations) {
        this.steps = steps;
        this.sessions = sessions;
        this.duration = duration;
        this.observations = observations;
    }

    public int getSteps() {
        return steps;
    }

    public int getSessions() {
        return sessions;
    }

    public int getDuration() {
        return duration;
    }

    public String getObservations() {
        return observations;
    }
}
