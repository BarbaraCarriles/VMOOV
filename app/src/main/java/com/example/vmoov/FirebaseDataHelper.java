package com.example.vmoov;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import com.example.vmoov.Metric;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseDataHelper {

    /**
     * Descarga todas las métricas de un usuario desde Firebase.
     * Devuelve una lista de Metric (una por sesión de juego).
     */
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

            List<Metric> metrics = new ArrayList<>();

            for (DataSnapshot gameNode : task.getResult().getChildren()) {
                String gameName = gameNode.getKey();
                for (DataSnapshot sessionSnap : gameNode.child("sessions").getChildren()) {
                    //procesa la sesion segun que juego sea
                    Metric metric = processSessionSnapshot(gameName, sessionSnap);
                    if (metric != null) metrics.add(metric);
                }
            }

            callback.onSuccess(metrics);
        });
    }

    /**
     * Procesa un snapshot individual de sesión y genera un Metric según el juego.
     */
    private static Metric processSessionSnapshot(String gameName, DataSnapshot sessionSnap) {
        //chequea que juego hay que procesar
        if ("simon".equalsIgnoreCase(gameName)) {
            return processSimonSession(sessionSnap);
        } else if ("game1".equalsIgnoreCase(gameName)) {
            return processGame1Session(sessionSnap);
        }
        return null;
    }

    // ------------------- SIMON -------------------
    private static Metric processSimonSession(DataSnapshot sessionSnap) {
        DataSnapshot resultsSnap = sessionSnap.child("results");
        if (!resultsSnap.exists()) return null;

        // Crea la lista de rounds
        List<Metric.SimonRound> rounds = new ArrayList<>();
        int trueCount = 0;
        double totalTime = 0;
        int stepCount = 0;

        // ✅ fijate que ahora recorre resultsSnap.child("rounds")
        for (DataSnapshot roundSnap : resultsSnap.child("rounds").getChildren()) {
            List<Metric.StepDetail> roundSteps = new ArrayList<>();
            for (DataSnapshot stepSnap : roundSnap.getChildren()) {
                Boolean correct = stepSnap.child("correct").getValue(Boolean.class);
                Double time = stepSnap.child("time").getValue(Double.class);
                Integer buttonPressed = stepSnap.child("buttonPressed").getValue(Integer.class);
                Long startTime = stepSnap.child("startTime").getValue(Long.class);
                Long endTime = stepSnap.child("endTime").getValue(Long.class);

                boolean result = correct != null && correct;
                double stepTime = time != null ? time : 0;

                if (result) trueCount++;
                totalTime += stepTime;
                stepCount++;

                roundSteps.add(new Metric.StepDetail(result, stepTime, buttonPressed, startTime, endTime));
            }
            rounds.add(new Metric.SimonRound(roundSteps));
        }

        double avgTime = stepCount > 0 ? totalTime / stepCount : 0;

        // ✅ también leer start/end/difficulty desde resultsSnap
        long start = resultsSnap.child("startTime").getValue(Long.class) != null
                ? resultsSnap.child("startTime").getValue(Long.class)
                : 0;
        long end = resultsSnap.child("endTime").getValue(Long.class) != null
                ? resultsSnap.child("endTime").getValue(Long.class)
                : start;
        int difficulty = resultsSnap.child("difficulty").getValue(Integer.class) != null
                ? resultsSnap.child("difficulty").getValue(Integer.class)
                : 0;
        Integer maxSteps = resultsSnap.child("maxSteps").getValue(Integer.class);

        String duration = MetricsCalculator.formatDuration(start, end);

        return new Metric(
                "simon",
                start,
                end,
                trueCount,
                avgTime,
                duration,
                stepCount,
                null, // game1 steps
                difficulty,
                maxSteps,
                rounds
        );
    }


    // ------------------- GAME 1 -------------------
    private static Metric processGame1Session(DataSnapshot sessionSnap) {
        DataSnapshot resultsSnap = sessionSnap.child("results");
        if (!resultsSnap.exists()) return null;

        List<Metric.StepDetail> stepDetails = new ArrayList<>();
        int trueCount = 0;
        double totalTime = 0;
        int stepCount = 0;

        //recorre todos los steps
        for (DataSnapshot stepSnap : resultsSnap.child("steps").getChildren()) {
            Boolean result = stepSnap.child("result").getValue(Boolean.class);
            Double time = stepSnap.child("time").getValue(Double.class);
            Long stepNum = stepSnap.child("step").getValue(Long.class);
            Long startTime = stepSnap.child("startTime").getValue(Long.class);
            Long endTime = stepSnap.child("endTime").getValue(Long.class);
            int stepNumber = stepNum != null ? stepNum.intValue() : stepCount;

            boolean isTrue = result != null && result;
            double stepTime = time != null ? time : 0;

            if (isTrue) trueCount++;
            totalTime += stepTime;

            // Usamos el constructor de Game1
            stepDetails.add(new Metric.StepDetail(isTrue, stepTime, stepNumber, startTime, endTime));
            stepCount++;
        }

        double avgTime = stepCount > 0 ? totalTime / stepCount : 0;

        //parametros generales de la session
        long start = resultsSnap.child("startTime").getValue(Long.class) != null
                ? resultsSnap.child("startTime").getValue(Long.class)
                : 0;
        long end = resultsSnap.child("endTime").getValue(Long.class) != null
                ? resultsSnap.child("endTime").getValue(Long.class)
                : start;
        int difficulty = resultsSnap.child("difficulty").getValue(Integer.class) != null
                ? resultsSnap.child("difficulty").getValue(Integer.class)
                : 0;

        String duration = MetricsCalculator.formatDuration(start, end);

        return new Metric("game1", start, end, trueCount, avgTime, duration, stepCount, stepDetails, difficulty, null, null);
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

    //para manejar los callbacks
    public interface FirebaseCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }


}



