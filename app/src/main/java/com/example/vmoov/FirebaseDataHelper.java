package com.example.vmoov;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseDataHelper {

    private static final String TAG = "FirebaseDataHelper";

    // ------------------------------------------------------------------------
    //  Obtener todas las métricas de un usuario
    // ------------------------------------------------------------------------
    public static void fetchAllMetrics(String userId, FirebaseCallback<List<Metric>> callback) {
        DatabaseReference gameplayRef = FirebaseDatabase.getInstance()
                .getReference("patientmetrics")
                .child(userId)
                .child("gameplaydata");

        gameplayRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onFailure(task.getException());
                return;
            }

            List<Metric> allMetrics = new ArrayList<>();
            DataSnapshot dataSnapshot = task.getResult();

            for (DataSnapshot gameNode : dataSnapshot.getChildren()) {
                String gameName = gameNode.getKey();
                DataSnapshot sessionsNode = gameNode.child("sessions");

                for (DataSnapshot sessionSnapshot : sessionsNode.getChildren()) {
                    DataSnapshot results = sessionSnapshot.child("results");
                    Metric metric = processGenericSession(gameName, results);
                    if (metric != null) allMetrics.add(metric);
                }
            }

            callback.onSuccess(allMetrics);
        });
    }

    // ------------------------------------------------------------------------
    //  Procesar sesión genérica (game1, simon, y otros futuros juegos)
    // ------------------------------------------------------------------------
    public static Metric processGenericSession(String gameName, DataSnapshot sessionSnapshot) {
        try {
            long startTime = sessionSnapshot.child("startTime").getValue(Long.class) != null ?
                    sessionSnapshot.child("startTime").getValue(Long.class) : 0;
            long endTime = sessionSnapshot.child("endTime").getValue(Long.class) != null ?
                    sessionSnapshot.child("endTime").getValue(Long.class) : startTime;

            Integer difficulty = sessionSnapshot.child("difficulty").getValue(Integer.class);
            if (difficulty == null) difficulty = 0;

            Integer maxSteps = sessionSnapshot.child("maxSteps").getValue(Integer.class);

            int trueCount = 0;
            int stepCount = 0;
            double totalTime = 0;
            List<Metric.StepDetail> stepDetails = new ArrayList<>();

            // GAME 1: pasos planos
            if (sessionSnapshot.child("steps").exists()) {
                for (DataSnapshot stepSnap : sessionSnapshot.child("steps").getChildren()) {
                    Boolean result = stepSnap.child("result").getValue(Boolean.class);
                    Double time = stepSnap.child("time").getValue(Double.class);
                    Long stepNumberLong = stepSnap.child("step").getValue(Long.class);
                    int stepNumber = (stepNumberLong != null) ? stepNumberLong.intValue() : stepCount;

                    if (result != null && result) trueCount++;
                    if (time != null) totalTime += time;

                    stepDetails.add(new Metric.StepDetail(
                            result != null && result,
                            time != null ? time : 0,
                            stepNumber
                    ));
                    stepCount++;
                }
            }

            // SIMON: rondas con múltiples pasos
            else if (sessionSnapshot.child("rounds").exists()) {
                int roundNumber = 0;
                for (DataSnapshot roundSnap : sessionSnapshot.child("rounds").getChildren()) {
                    for (DataSnapshot stepSnap : roundSnap.getChildren()) {
                        Boolean correct = stepSnap.child("correct").getValue(Boolean.class);
                        Double time = stepSnap.child("time").getValue(Double.class);
                        Integer buttonPressed = stepSnap.child("buttonPressed").getValue(Integer.class);

                        if (correct != null && correct) trueCount++;
                        if (time != null) totalTime += time;

                        stepDetails.add(new Metric.StepDetail(
                                correct != null && correct,
                                time != null ? time : 0,
                                roundNumber,
                                buttonPressed
                        ));
                        stepCount++;
                    }
                    roundNumber++;
                }
            }

            double averageTime = (stepCount > 0) ? totalTime / stepCount : 0;
            String gameDuration = GameDurationCalculator.calculateGameDuration(startTime, endTime);

            return new Metric(
                    gameName,
                    startTime,
                    endTime,
                    trueCount,
                    averageTime,
                    gameDuration,
                    stepCount,
                    stepDetails,
                    difficulty,
                    maxSteps
            );

        } catch (Exception e) {
            Log.e(TAG, "Error procesando sesión de " + gameName, e);
            return null;
        }
    }

    // ------------------------------------------------------------------------
    //  Calcular métricas globales a partir de todas las sesiones
    // ------------------------------------------------------------------------
    public static GlobalMetrics calculateGlobalMetrics(List<Metric> allMetrics) {
        if (allMetrics == null || allMetrics.isEmpty()) return null;

        int totalSteps = 0;
        int totalSuccessfulSteps = 0;
        double totalAverageTime = 0;

        for (Metric m : allMetrics) {
            totalSteps += m.getStepCount();
            totalSuccessfulSteps += m.getTrueCount();
            totalAverageTime += m.getAverageTime();
        }

        double averageTime = allMetrics.size() > 0 ? totalAverageTime / allMetrics.size() : 0;
        int totalSessions = allMetrics.size();
        int totalGamesPlayed = (int) allMetrics.stream().map(Metric::getGameName).distinct().count();

        return new GlobalMetrics(totalSteps, totalSuccessfulSteps, totalSessions, totalGamesPlayed, averageTime);
    }

    public static class GlobalMetrics {
        public final int totalSteps;
        public final int totalSuccessfulSteps;
        public final int totalSessions;
        public final int totalGamesPlayed;
        public final double averageTime;

        public GlobalMetrics(int totalSteps, int totalSuccessfulSteps, int totalSessions, int totalGamesPlayed, double averageTime) {
            this.totalSteps = totalSteps;
            this.totalSuccessfulSteps = totalSuccessfulSteps;
            this.totalSessions = totalSessions;
            this.totalGamesPlayed = totalGamesPlayed;
            this.averageTime = averageTime;
        }
    }

    // ------------------------------------------------------------------------
    //  Obtener prescripciones del paciente
    // ------------------------------------------------------------------------
    public static void fetchPatientPrescriptions(String patientId, FirebaseCallback<List<Prescription>> callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("healthProfessionals");

        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onFailure(task.getException());
                return;
            }

            List<Prescription> prescriptions = new ArrayList<>();
            DataSnapshot professionalsSnapshot = task.getResult();

            for (DataSnapshot professionalSnap : professionalsSnapshot.getChildren()) {
                DataSnapshot patientsSnap = professionalSnap.child("patients");
                if (patientsSnap.hasChild(patientId)) {
                    DataSnapshot patientNode = patientsSnap.child(patientId);
                    if (patientNode.hasChild("prescription")) {
                        DataSnapshot p = patientNode.child("prescription");
                        int steps = p.child("steps").getValue(Integer.class) != null ? p.child("steps").getValue(Integer.class) : 0;
                        int sessions = p.child("sessions").getValue(Integer.class) != null ? p.child("sessions").getValue(Integer.class) : 0;
                        int duration = p.child("duration").getValue(Integer.class) != null ? p.child("duration").getValue(Integer.class) : 0;
                        String obs = p.child("observations").getValue(String.class) != null ? p.child("observations").getValue(String.class) : "";

                        prescriptions.add(new Prescription(steps, sessions, duration, obs));
                    }
                }
            }

            callback.onSuccess(prescriptions);
        });
    }


    // ------------------------------------------------------------------------
    //  Procesar steps
    // ------------------------------------------------------------------------

    public static List<Metric.StepDetail> processStepDetails(String gameName, DataSnapshot sessionSnapshot) {
        List<Metric.StepDetail> stepDetails = new ArrayList<>();

        if (gameName.equals("game1") && sessionSnapshot.child("steps").exists()) {
            // GAME1: steps planos con stepNumber
            for (DataSnapshot stepSnap : sessionSnapshot.child("steps").getChildren()) {
                Boolean result = stepSnap.child("result").getValue(Boolean.class);
                Double time = stepSnap.child("time").getValue(Double.class);
                Long stepNumberLong = stepSnap.child("step").getValue(Long.class);
                int stepNumber = stepNumberLong != null ? stepNumberLong.intValue() : stepDetails.size();

                stepDetails.add(new Metric.StepDetail(
                        result != null && result,
                        time != null ? time : 0,
                        stepNumber
                ));
            }
        } else if (gameName.equals("simon") && sessionSnapshot.child("rounds").exists()) {
            // SIMON: cada round tiene steps
            int roundNumber = 0;
            for (DataSnapshot roundSnap : sessionSnapshot.child("rounds").getChildren()) {
                for (DataSnapshot stepSnap : roundSnap.getChildren()) {
                    Boolean correct = stepSnap.child("correct").getValue(Boolean.class);
                    Double time = stepSnap.child("time").getValue(Double.class);
                    Integer buttonPressed = stepSnap.child("buttonPressed").getValue(Integer.class);

                    stepDetails.add(new Metric.StepDetail(
                            correct != null && correct,
                            time != null ? time : 0,
                            roundNumber,
                            buttonPressed
                    ));
                }
                roundNumber++;
            }
        }

        return stepDetails;
    }


    // ------------------------------------------------------------------------
    //  Calcular estadistitcas por boton: %aciertos y tiempo promedio
    // ------------------------------------------------------------------------
    public static class ButtonStats {
        public final int button;
        public final int correctCount;
        public final int totalCount;
        public final double precision;      // 0..1
        public final double averageTime;    // en segundos

        public ButtonStats(int button, int correctCount, int totalCount, double precision, double averageTime) {
            this.button = button;
            this.correctCount = correctCount;
            this.totalCount = totalCount;
            this.precision = precision;
            this.averageTime = averageTime;
        }
    }

    public static List<ButtonStats> calculateButtonStats(List<Metric.StepDetail> stepDetails) {
        // Map<boton, List<StepDetail>>
        Map<Integer, List<Metric.StepDetail>> map = new HashMap<>();

        for (Metric.StepDetail step : stepDetails) {
            if (step.getButtonPressed() == null) continue; // ignorar game1
            map.computeIfAbsent(step.getButtonPressed(), k -> new ArrayList<>()).add(step);
        }

        List<ButtonStats> statsList = new ArrayList<>();
        for (Map.Entry<Integer, List<Metric.StepDetail>> entry : map.entrySet()) {
            int button = entry.getKey();
            List<Metric.StepDetail> steps = entry.getValue();

            int total = steps.size();
            int correct = 0;
            double totalTime = 0;

            for (Metric.StepDetail s : steps) {
                if (s.isResult()) correct++;
                totalTime += s.getTime();
            }

            double precision = total > 0 ? (double) correct / total : 0;
            double avgTime = total > 0 ? totalTime / total : 0;

            statsList.add(new ButtonStats(button, correct, total, precision, avgTime));
        }

        return statsList;
    }


    // ------------------------------------------------------------------------
    //  Interfaz genérica para callbacks Firebase
    // ------------------------------------------------------------------------
    public interface FirebaseCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
