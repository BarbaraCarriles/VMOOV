package com.example.vmoov;

import com.example.vmoov.Metric;
import java.util.*;

public class MetricsCalculator {

    // ----------------------------------------------------------
    //  FORMATEO DE TIEMPO
    // ----------------------------------------------------------
    // Recibe el tiempo de inicio y fin en milisegundos, calcula
    // la duración total en minutos y segundos, y la devuelve en
    // formato “MM:SS”.
    public static String formatDuration(long start, long end) {
        long totalSecs = (end - start) / 1000;
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    // ----------------------------------------------------------
//  MÉTRICAS DEL JUEGO "SIMON" CON ROUNDS
// ----------------------------------------------------------
    public static SimonStats calculateSimonStats(Metric metric) {
        if (!"simon".equalsIgnoreCase(metric.getGameName()) || metric.getRounds() == null) return null;

        int totalRounds = metric.getRounds().size(); // número total de rondas
        int completedRounds = 0; //rondas con todos los steps = true
        int totalSteps = 0;
        int correctSteps = 0;
        double totalTime = 0; //suma del time de todos los steps

        Map<Integer, ButtonStats> buttonStats = new HashMap<>();

        for (Metric.SimonRound round : metric.getRounds()) { //para cada round
            boolean roundSuccessful = true;

            for (Metric.StepDetail step : round.getSteps()) { //para cada step dentro del round
                totalSteps++;
                if (step.isResult())
                    correctSteps++;
                    else roundSuccessful = false;

                totalTime += step.getTime();

                // Estadísticas por botón
                Integer btn = step.getButtonPressed();
                if (btn != null) {
                    buttonStats.putIfAbsent(btn, new ButtonStats());
                    ButtonStats bs = buttonStats.get(btn);
                    bs.total++;
                    if (step.isResult()) bs.correct++;
                    bs.times.add(step.getTime());
                }
            }

            if (roundSuccessful) completedRounds++;
        }

        // Tiempo promedio por step
        double avgTime = totalSteps > 0 ? totalTime / totalSteps : 0;

        // Precisión global
        double accuracy = totalSteps > 0 ? (double) correctSteps / totalSteps : 0;

        int difficulty = metric.getDifficulty();

        double generalScore = Math.min(100,
                (accuracy * 70 + (8.0 - Math.min(avgTime, 8.0)) / 8.0 * 30) * (1 + difficulty * 0.25));

        // Calculamos tiempo promedio por botón
        for (ButtonStats bs : buttonStats.values()) {
            bs.avgTime = bs.times.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        return new SimonStats(completedRounds, totalRounds, accuracy, avgTime, generalScore, buttonStats, correctSteps, totalSteps);
    }

    // ----------------------------------------------------------
//  MÉTRICAS DEL JUEGO "GAME 1"
// ----------------------------------------------------------
    public static Game1Stats calculateGame1Stats(Metric metric) {
        if (!"game1".equalsIgnoreCase(metric.getGameName()) || metric.getSteps() == null) return null;

        int successfulMoves = metric.getTrueCount();
        int totalSteps = metric.getStepCount();

        // Tiempo promedio de step
        double avgTime = metric.getAverageTime();

        // Precisión global (movimientos exitosos / movimientos totales)
        double accuracy = totalSteps > 0 ? (double) successfulMoves / totalSteps : 0;


        int difficulty = metric.getDifficulty();


        double generalScore = Math.min(100,
                (accuracy * 70 + (8.0 - Math.min(avgTime, 8.0)) / 8.0 * 30) * (1 + difficulty * 0.25));


        return new Game1Stats(successfulMoves, totalSteps,avgTime, generalScore);
    }


    // ----------------------------------------------------------
    // CLASES AUXILIARES
    // ----------------------------------------------------------

    // ---- Resultados específicos para Simon
    public static class SimonStats {
        public int completedRounds;
        public int totalRounds;
        public double accuracy;
        public double avgTime;
        public double generalScore;

        public int totalSteps;
        public int correctSteps;
        public Map<Integer, ButtonStats> buttonStats;

        public SimonStats(int c, int t, double a, double at, double g, Map<Integer, ButtonStats> b, int cs, int ts) {
            completedRounds = c;
            totalRounds = t;
            accuracy = a;
            avgTime = at;
            generalScore = g;
            buttonStats = b;
            correctSteps=cs;
            totalSteps=ts;
        }
    }

    // ---- Resultados específicos Game1
    public static class Game1Stats {
        public int successfulMoves; // movimientos correctos

        public int totalMoves;
        public double avgTime;
        public double generalScore;

        public Game1Stats(int successfulMoves, int totalMoves, double avgTime, double generalScore) {
            this.successfulMoves = successfulMoves;
            this.totalMoves=totalMoves;
            this.avgTime = avgTime;
            this.generalScore = generalScore;
        }
    }

    // ---- Datos acumulados por cada botón (para Simon)
    public static class ButtonStats {
        public int total = 0;          // cuántas veces se presionó
        public int correct = 0;        // cuántas veces fue correcto
        public double avgTime = 0;     // tiempo promedio
        public List<Double> times = new ArrayList<>();

        // Devuelve la precisión (correctos / totales)
        public double getAccuracy() {
            return total > 0 ? (double) correct / total : 0;
        }
    }
}

