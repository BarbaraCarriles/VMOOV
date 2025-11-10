package com.example.vmoov;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameMetric {

    // Tipos de métricas para mostrar en la UI
    public enum MetricType {
        SCORE,        // Puntaje general
        NUMBER,       // Número simple
        RATIO,        // Ratio: completados / totales
        TIME_TEXT,         // Texto con valor de % cambio de tiempo
        TEXT,
        PIE_CHART, //grafico circular

        BAR_CHART
    }

    // Clase que representa un ítem de métrica
    public static class MetricItem {
        private MetricType type;
        private int value;               // Valor principal (score, movimientos correctos, etc)
        private int totalPrescribed;     // Total prescrito para ratios
        private int totalAbsolute;       // Total real para ratios
        private String label;            // Texto a mostrar

        //datos para graficos
        private List<ButtonAccuracy> buttonAccuracyList;
        private List<ButtonAvgTime> buttonAvgTimeList;

        // Constructor para métricas simples
        public MetricItem(MetricType type, int value, int totalPrescribed, int totalAbsolute, String label) {
            this.type = type;
            this.value = value;
            this.totalPrescribed = totalPrescribed;
            this.totalAbsolute = totalAbsolute;
            this.label = label;
            //this.barData = null;
            this.buttonAccuracyList=null;
        }

        // 🔹 Constructor para pie charts y barchart
        public MetricItem(MetricType type, List<?> buttonDataList, String label) {
            this.type = type;
            this.label = label;
            this.value = 0;
            this.totalPrescribed = 0;
            this.totalAbsolute = 0;

            if (buttonDataList != null && !buttonDataList.isEmpty()) {
                if (buttonDataList.get(0) instanceof ButtonAccuracy) {
                    // Asignar lista de precisión
                    this.buttonAccuracyList = (List<ButtonAccuracy>) buttonDataList;
                } else if (buttonDataList.get(0) instanceof ButtonAvgTime) {
                    // Asignar lista de tiempos
                    this.buttonAvgTimeList = (List<ButtonAvgTime>) buttonDataList;
                }
            }
        }


        // Getters
        public MetricType getType() { return type; }
        public int getValue() { return value; }
        public int getTotalPrescribed() { return totalPrescribed; }
        public int getTotalAbsolute() { return totalAbsolute; }
        public String getLabel() { return label; }
        public List<ButtonAccuracy> getButtonAccuracies() { return buttonAccuracyList; }

        public List<ButtonAvgTime> getButtonAvgTimes() { return buttonAvgTimeList; }
    }

    // 🔹 Clase específica para precisión por botón (pie charts)
    public static class ButtonAccuracy {
        private int buttonId;
        private float accuracy; // valor entre 0 y 100
        private boolean hasData;

        public ButtonAccuracy(int buttonId, float accuracy, boolean hasData) {
            this.buttonId = buttonId;
            this.accuracy = accuracy;
            this.hasData = hasData;
        }

        public int getButtonId() { return buttonId; }
        public float getAccuracy() { return accuracy; }
        public boolean hasData() { return hasData; }
    }

    //clase especifica para avg time por boton
    public static class ButtonAvgTime {
        private int buttonId;
        private double avgTime; // en segundos
        private boolean hasData;

        public ButtonAvgTime(int buttonId, double avgTime, boolean hasData) {
            this.buttonId = buttonId;
            this.avgTime = avgTime;
            this.hasData = hasData;
        }

        public int getButtonId() { return buttonId; }
        public double getAvgTime() { return avgTime; }
        public boolean hasData() { return hasData; }
    }

    private List<MetricItem> metricsList = new ArrayList<>();

    public void addMetricItem(MetricItem item) {
        metricsList.add(item);
    }

    public List<MetricItem> getMetricsList() {
        return metricsList;
    }

    // -------------------------
    // Métodos estáticos de conveniencia para construir GameMetric de distintos juegos
    // -------------------------

    // Construir GameMetric para Simon
    public static GameMetric buildSimonMetric(Metric metric) {
        MetricsCalculator.SimonStats stats = MetricsCalculator.calculateSimonStats(metric);
        GameMetric gm = new GameMetric();
        if (stats == null) return gm;

        // Puntaje general
        gm.addMetricItem(new MetricItem(MetricType.SCORE,
                (int) stats.generalScore, 0, 0, "Puntaje General"));

        // Dificultad
        gm.addMetricItem(new MetricItem(MetricType.TEXT,
                metric.getDifficulty(), 0, 0, "Dificultad"));

        // Rondas completas / totales
        gm.addMetricItem(new MetricItem(MetricType.RATIO,
                stats.completedRounds,
                0,
                stats.totalRounds,
                "Rondas completadas"));

        // Movimientos correctos / totales
        gm.addMetricItem(new MetricItem(MetricType.RATIO,
                stats.correctSteps,
                0,
                stats.totalSteps,
                "Movimientos correctos"));


        //Pie chart:precision por boton
        List<ButtonAccuracy> buttonAccuracies = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            MetricsCalculator.ButtonStats statsButton = stats.buttonStats.get(i);
            if (statsButton != null) {
                buttonAccuracies.add(new ButtonAccuracy(i, (float) (statsButton.getAccuracy() * 100), true));
            } else {
                buttonAccuracies.add(new ButtonAccuracy(i, 0f, false));
            }
        }

        //BarChart: avgTime por boton
        List<ButtonAvgTime> buttonAvgTimes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            MetricsCalculator.ButtonStats statsButton = stats.buttonStats.get(i);
            if (statsButton != null) {
                buttonAvgTimes.add(new ButtonAvgTime(i, statsButton.avgTime, true));
            } else {
                buttonAvgTimes.add(new ButtonAvgTime(i, 0, false));
            }
        }

        gm.addMetricItem(new MetricItem(MetricType.PIE_CHART, buttonAccuracies, "Precisión por botón"));
        gm.addMetricItem(new MetricItem(MetricType.BAR_CHART, buttonAvgTimes, "Tiempo promedio por botón"));


        return gm;
    }


    // Construir GameMetric para Game1 con comparación temporal
    public static GameMetric buildGame1Metric(Metric lastMetric, Metric previousMetric) {
        MetricsCalculator.Game1Stats lastStats = MetricsCalculator.calculateGame1Stats(lastMetric);
        MetricsCalculator.Game1Stats prevStats = previousMetric != null
                ? MetricsCalculator.calculateGame1Stats(previousMetric)
                : null;

        GameMetric gm = new GameMetric();
        if (lastStats == null) return gm;

        // Puntaje general
        gm.addMetricItem(new MetricItem(MetricType.SCORE,
                (int) lastStats.generalScore, 0, 0, "Puntaje General"));

        // Dificultad
        gm.addMetricItem(new MetricItem(MetricType.TEXT,
                lastMetric.getDifficulty(), 0, 0, "Dificultad"));

        // Movimientos exitosos / totales
        gm.addMetricItem(new MetricItem(MetricType.RATIO,
                lastStats.successfulMoves, lastMetric.getStepCount(),
                lastStats.totalMoves, "Movimientos exitosos"));

        // Cambio en tiempo promedio (%)
        int avgTimeChange = 0;
        String label;

        if (prevStats != null && prevStats.avgTime > 0) {
            avgTimeChange = (int) (((prevStats.avgTime - lastStats.avgTime) / prevStats.avgTime) * 100);
        }
        label = "Cambio porcentual en el tiempo promedio por movimiento";


        gm.addMetricItem(new MetricItem(MetricType.TIME_TEXT,
                avgTimeChange, 0, 0, label));

        return gm;
    }

}
