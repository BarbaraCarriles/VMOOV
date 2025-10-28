package com.example.vmoov;

import java.util.List;

public class Metric {
    private String gameName; // Nombre del juego (por ejemplo: "game1" o "simon")
    private long startTime;  // Timestamp de inicio (ms)
    private long endTime;    // Timestamp de fin (ms)
    private int trueCount;       // Cantidad de movimientos correctos
    private double averageTime;  // Tiempo promedio por paso/movimiento
    private String gameDuration; // Duración total formateada (ej: "01:23")
    private long gameDurationSeconds; // Duración total en segundos
    private int stepCount;       // Total de pasos o movimientos

    // 🔹 Detalle paso a paso (solo se usa si se necesita analizar cada movimiento)
    private List<StepDetail> steps;


    // ------------------------------------------------------------------------
    //  CONSTRUCTORES
    // ------------------------------------------------------------------------

    /**
     * Constructor principal que se usa cuando también se quieren almacenar los pasos individuales.
     *
     *  Se utiliza en ambos juegos ("game1" y "simon") cuando se desea guardar el detalle completo.
     *
     * @param gameName       Nombre del juego ("game1" o "simon")
     * @param startTime      Inicio de la sesión
     * @param endTime        Fin de la sesión
     * @param trueCount      Cantidad de aciertos
     * @param averageTime    Tiempo promedio por paso
     * @param gameDuration   Duración total formateada
     * @param stepCount      Número total de pasos o movimientos
     * @param steps          Lista con el detalle de cada paso
     */
    public Metric(String gameName, long startTime, long endTime, int trueCount, double averageTime,
                  String gameDuration, int stepCount, List<StepDetail> steps) {
        this.gameName = gameName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.trueCount = trueCount;
        this.averageTime = averageTime;
        this.gameDuration = gameDuration;
        this.stepCount = stepCount;
        this.steps = steps;
        this.gameDurationSeconds = (endTime - startTime) / 1000;
    }

    /**
     * Constructor alternativo (sin lista de pasos).
     *
     *  Se usa cuando solo interesa la información general de la sesión
     *    (por ejemplo, para mostrar promedios o estadísticas globales).
     *
     */
    public Metric(String gameName, long startTime, long endTime, int trueCount, double averageTime,
                  String gameDuration, int stepCount) {
        this(gameName, startTime, endTime, trueCount, averageTime, gameDuration, stepCount, null);
    }


    // ------------------------------------------------------------------------
    // GETTERS
    // ------------------------------------------------------------------------

    public String getGameName() { return gameName; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public int getTrueCount() { return trueCount; }
    public double getAverageTime() { return averageTime; }
    public String getGameDuration() { return gameDuration; }
    public long getGameDurationSeconds() { return gameDurationSeconds; }
    public int getStepCount() { return stepCount; }
    public List<StepDetail> getSteps() { return steps; }


    // ------------------------------------------------------------------------
    // CLASE INTERNA: StepDetail
    // ------------------------------------------------------------------------
    /**
     * Representa un movimiento o paso dentro de una sesión de juego.
     *
     * Se usa de forma distinta según el juego:
     *
     * 🔸 En "game1":
     *      - Cada StepDetail representa un movimiento del paciente (por ejemplo, levantar brazo)
     *      - Solo se usan `result` y `time`
     *
     * 🔸 En "simon":
     *      - Cada StepDetail representa un movimiento dentro de una ronda.
     *      - Se usan `result`, `time` y también `roundNumber` para identificar la ronda.
     */
    public static class StepDetail {
        private boolean result;     // Si el paso fue correcto o no
        private double time;        // Tiempo del paso
        private int roundNumber;    // Número de ronda (solo en Simon)

        /**
         * Constructor general (para Simon u otros juegos con rondas).
         */
        public StepDetail(boolean result, double time, int roundNumber) {
            this.result = result;
            this.time = time;
            this.roundNumber = roundNumber;
        }

        /**
         * Constructor simple (para Game1 u otros sin rondas).
         */
        public StepDetail(boolean result, double time) {
            this(result, time, -1); // -1 indica que no pertenece a ninguna ronda
        }

        // Getters
        public boolean isResult() { return result; }
        public double getTime() { return time; }
        public int getRoundNumber() { return roundNumber; }
    }
}

