package com.example.vmoov;

import java.util.List;

public class Metric {
    private String gameName;         // Nombre del juego
    private long startTime;          // Timestamp inicio
    private long endTime;            // Timestamp fin
    private int trueCount;           // Movimientos correctos totales
    private double averageTime;      // Tiempo promedio por paso
    private String gameDuration;     // Duración formateada
    private long gameDurationSeconds;// Duración total en segundos
    private int stepCount;           // Total de pasos (suma de todos los rounds)
    private int difficulty;          // Nivel de dificultad
    private Integer maxSteps;        // Límite de pasos (solo Simon)
    private List<StepDetail> steps;  // Para juegos lineales (game1)
    private List<SimonRound> rounds; // Para Simon (estructura jerárquica)

    // ---------------- CONSTRUCTORES ----------------
    public Metric(String gameName, long startTime, long endTime, int trueCount,
                  double averageTime, String gameDuration, int stepCount,
                  List<StepDetail> steps, int difficulty, Integer maxSteps, List<SimonRound> rounds) {
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
        this.rounds = rounds;
        this.gameDurationSeconds = (endTime - startTime) / 1000;
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
    public int getDifficulty() { return difficulty; }
    public Integer getMaxSteps() { return maxSteps; }
    public List<StepDetail> getSteps() { return steps; }
    public List<SimonRound> getRounds() { return rounds; }

    // ---------------- CLASES INTERNAS ----------------

    //  Round de Simon (lista de pasos)
    public static class SimonRound {
        private List<StepDetail> steps;

        public SimonRound(List<StepDetail> steps) {
            this.steps = steps;
        }

        public List<StepDetail> getSteps() {
            return steps;
        }

        // Devuelve true si todos los pasos de la ronda fueron correctos
        public boolean isRoundSuccessful() {
            return steps.stream().allMatch(StepDetail::isResult);
        }
    }

    //  StepDetail (unidad mínima de acción)
    public static class StepDetail {
        private boolean result;       // Correcto o no
        private double time;          // Duración
        private Integer buttonPressed;// Simon
        private Integer stepNumber;   // Game1
        private Long startTime;
        private Long endTime;

        // Constructor para Simon
        public StepDetail(boolean result, double time, Integer buttonPressed, Long startTime, Long endTime) {
            this.result = result;
            this.time = time;
            this.buttonPressed = buttonPressed;
            this.stepNumber = null;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        // Constructor para Game1
        public StepDetail(boolean result, double time, int stepNumber, Long startTime, Long endTime) {
            this.result = result;
            this.time = time;
            this.stepNumber = stepNumber;
            this.buttonPressed = null;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        // Getters
        public boolean isResult() { return result; }
        public double getTime() { return time; }
        public Integer getButtonPressed() { return buttonPressed; }
        public Integer getStepNumber() { return stepNumber; }
        public Long getStartTime() { return startTime; }
        public Long getEndTime() { return endTime; }
    }
}
