package com.example.vmoov;

import java.util.List;

public class Metric {
    private String gameName;         // Nombre del juego
    private long startTime;          // Timestamp inicio
    private long endTime;            // Timestamp fin
    private int trueCount;           // Movimientos correctos
    private double averageTime;      // Tiempo promedio por paso
    private String gameDuration;     // Duración formateada
    private long gameDurationSeconds;// Duración total en segundos
    private int stepCount;           // Total de pasos/movimientos
    private int difficulty;          // Nivel de dificultad
    private Integer maxSteps;        // Solo para juegos como Simon (puede ser null)
    private List<StepDetail> steps;  // Detalle de cada paso

    // ---------------- CONSTRUCTORES ----------------
    public Metric(String gameName, long startTime, long endTime, int trueCount,
                  double averageTime, String gameDuration, int stepCount,
                  List<StepDetail> steps, int difficulty, Integer maxSteps) {
        this.gameName = gameName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.trueCount = trueCount;
        this.averageTime = averageTime;
        this.gameDuration = gameDuration;
        this.stepCount = stepCount;
        this.steps = steps;
        this.difficulty = difficulty;
        this.maxSteps = maxSteps;
        this.gameDurationSeconds = (endTime - startTime) / 1000;
    }

    // Constructor alternativo sin steps
    public Metric(String gameName, long startTime, long endTime, int trueCount,
                  double averageTime, String gameDuration, int stepCount,
                  int difficulty, Integer maxSteps) {
        this(gameName, startTime, endTime, trueCount, averageTime, gameDuration, stepCount, null, difficulty, maxSteps);
    }

    // ---------------- GETTERS ----------------
    public String getGameName() { return gameName; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public int getTrueCount() { return trueCount; }
    public double getAverageTime() { return averageTime; }
    public String getGameDuration() { return gameDuration; }
    public long getGameDurationSeconds() { return gameDurationSeconds; }
    public int getStepCount() { return stepCount; }
    public List<StepDetail> getSteps() { return steps; }
    public int getDifficulty() { return difficulty; }
    public Integer getMaxSteps() { return maxSteps; }

    // ---------------- StepDetail ----------------
    public static class StepDetail {
        private boolean result;      // Correcto o no
        private double time;         // Duración
        private int roundNumber;     // Simon
        private Integer stepNumber;  //
        private Integer buttonPressed; // Simon

        // Constructor para Simon
        public StepDetail(boolean result, double time, int roundNumber, Integer buttonPressed) {
            this.result = result;
            this.time = time;
            this.roundNumber = roundNumber;
            this.buttonPressed = buttonPressed;
            this.stepNumber = null;
        }

        // Constructor para game1
        public StepDetail(boolean result, double time, int stepNumber) {
            this.result = result;
            this.time = time;
            this.stepNumber = stepNumber;
            this.roundNumber = -1;
            this.buttonPressed = null;
        }

        // Getters
        public boolean isResult() { return result; }
        public double getTime() { return time; }
        public int getRoundNumber() { return roundNumber; }
        public Integer getStepNumber() { return stepNumber; }
        public Integer getButtonPressed() { return buttonPressed; }
    }
}
