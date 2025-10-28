package com.example.vmoov;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class LastSessionActivity extends BaseActivity {
    private TextView titleText, subtitleText, motivationalText, chartTitle, chartDescription;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ChartPagerAdapter chartPagerAdapter;

    private final String TAG = "LastSessionActivity";

    private final String[] chartTitles = {
            "Puntaje General",
            "Tiempo Promedio por Paso",
            "Movimientos Exitosos",
            "Sesiones Completadas",
    };

    // Descripciones para modo entrenamiento (con prescripción)
    private final String[] chartDescriptionsEntrenamiento = {
            "Evaluación general del desempeño del paciente basada en múltiples métricas.",
            "Cambio porcentual en el tiempo promedio de ejecución entre las últimas dos sesiones.",
            "Proporción de movimientos exitosos sobre el total de la prescripción.",
            "Cantidad de sesiones realizadas respecto del total recetado.",
    };

    // Descripciones para modo validación (sin prescripción)
    private final String[] chartDescriptionsValidacion = {
            "Evaluación general del desempeño del paciente basada en múltiples métricas.",
            "Cambio porcentual en el tiempo promedio de ejecución entre las últimas dos sesiones.",
            "Proporción de movimientos exitosos sobre el total de intentos realizados.",
            "Cantidad total de sesiones realizadas por el paciente.",
    };

    // Variable que se actualizará dinámicamente
    private String[] chartDescriptionsActuales = chartDescriptionsEntrenamiento;


    private int prescribedSteps = 0;
    private int totalSessions = 0;
    private String fechaUltimaSesion = "Fecha desconocida";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last_session);

        Log.d(TAG, "Iniciando LastSessionActivity...");

        // Botón de regreso
        ImageButton homeButton = findViewById(R.id.back_button);
        homeButton.setOnClickListener(v -> startActivity(new Intent(LastSessionActivity.this, MenuActivity.class)));

        // Referencias UI
        titleText = findViewById(R.id.title_text);
        subtitleText = findViewById(R.id.subtitle_text);
        motivationalText = findViewById(R.id.motivational_text);
        chartTitle = findViewById(R.id.chart_title);
        chartDescription = findViewById(R.id.chart_description);
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tab_layout);
        chartPagerAdapter = new ChartPagerAdapter(this);
        viewPager.setAdapter(chartPagerAdapter);

        // Configura tabs
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText("#" + (position + 1))).attach();

        // Actualiza título y descripción al cambiar de tab
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                chartTitle.setText(chartTitles[position]);
                chartDescription.setText(chartDescriptionsActuales[position]); // 🔹
            }
        });

        // Frase motivacional
        motivationalText.setText(getRandomMotivationalPhrase());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado.");
            titleText.setText("Usuario no autenticado");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Usuario autenticado: " + userId);

        // Primero obtiene prescripciones y luego métricas de juego
        obtenerPrescripciones(userId, () -> obtenerMetricasJuego(userId));
    }

    private void obtenerPrescripciones(String userId, Runnable callback) {
        DatabaseReference prescriptionRef = FirebaseDatabase.getInstance().getReference("healthProfessionals");

        prescriptionRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                boolean found = false;

                for (DataSnapshot professionalSnapshot : task.getResult().getChildren()) {
                    DataSnapshot patientsSnapshot = professionalSnapshot.child("patients");
                    if (patientsSnapshot.hasChild(userId)) {
                        DataSnapshot presNode = patientsSnapshot.child(userId).child("prescription");

                        if (presNode.exists()) { //hay prescripciones
                            totalSessions = presNode.child("sessions").getValue(Integer.class) != null ?
                                    presNode.child("sessions").getValue(Integer.class) : 0;
                            prescribedSteps = presNode.child("steps").getValue(Integer.class) != null ?
                                    presNode.child("steps").getValue(Integer.class) : 0;
                            chartPagerAdapter.setModoValidacion(false);
                            chartDescriptionsActuales = chartDescriptionsEntrenamiento;
                        } else {
                            //  Sin prescripción: modo validación
                            totalSessions = 0;
                            prescribedSteps = 0;
                            chartPagerAdapter.setModoValidacion(true);
                            chartDescriptionsActuales = chartDescriptionsValidacion;
                        }

                        found = true;
                        break;
                    }
                }

                if (!found) {
                    Log.w(TAG, "⚠ No se encontró al paciente dentro de healthProfessionals.");
                    totalSessions = 0;
                    prescribedSteps = 0;
                }

                Log.d(TAG, "🔍 Sesiones prescriptas: " + totalSessions);
                Log.d(TAG, "🔍 Pasos prescriptos: " + prescribedSteps);

                callback.run();
            } else {
                Log.e(TAG, "Error al obtener prescripciones", task.getException());
            }
        });
    }

    private void obtenerMetricasJuego(String userId) {
        DatabaseReference gameplayRef = FirebaseDatabase.getInstance()
                .getReference("patientmetrics")
                .child(userId)
                .child("gameplaydata");

        gameplayRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return;

            DataSnapshot dataSnapshot = task.getResult();
            List<Metric> allMetrics = new ArrayList<>();
            long latestTimestamp = 0;

            for (DataSnapshot gameNode : dataSnapshot.getChildren()) {
                String gameName = gameNode.getKey();
                DataSnapshot sessionsNode = gameNode.child("sessions");

                for (DataSnapshot sessionSnapshot : sessionsNode.getChildren()) {
                    DataSnapshot results = sessionSnapshot.child("results");
                    Metric metric = processGenericSession(gameName, results);
                    if (metric != null) {
                        allMetrics.add(metric);
                        if (metric.getStartTime() > latestTimestamp) latestTimestamp = metric.getStartTime();
                    }
                }
            }

            // Actualiza fecha de última sesión
            if (latestTimestamp > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                fechaUltimaSesion = dateFormat.format(new Date(latestTimestamp));
                runOnUiThread(() -> subtitleText.setText(fechaUltimaSesion));
            }

            calcularMetricasParaGraficos(allMetrics);
        });
    }

    private Metric processGenericSession(String gameName, DataSnapshot results) {
        double totalTime = 0;
        int stepCount = 0;
        int trueCount = 0;
        List<Metric.StepDetail> steps = new ArrayList<>();

        if (gameName.equals("game1")) {
            for (DataSnapshot stepSnap : results.child("steps").getChildren()) {
                Boolean correct = stepSnap.child("result").getValue(Boolean.class);
                Double time = stepSnap.child("time").getValue(Double.class);

                boolean isCorrect = correct != null && correct;
                double stepTime = time != null ? time : 0;
                if (isCorrect) trueCount++;
                totalTime += stepTime;
                stepCount++;

                steps.add(new Metric.StepDetail(isCorrect, stepTime, 0));
            }

        } else if (gameName.equals("simon")) {
            int roundNumber = 1;
            for (DataSnapshot roundSnap : results.child("rounds").getChildren()) {
                for (DataSnapshot moveSnap : roundSnap.getChildren()) {
                    Boolean correct = moveSnap.child("correct").getValue(Boolean.class);
                    Double time = moveSnap.child("time").getValue(Double.class);

                    boolean isCorrect = correct != null && correct;
                    double stepTime = time != null ? time : 0;
                    if (isCorrect) trueCount++;
                    totalTime += stepTime;
                    stepCount++;

                    steps.add(new Metric.StepDetail(isCorrect, stepTime, roundNumber));
                }
                roundNumber++;
            }
        } else {
            Log.w(TAG, "Juego desconocido: " + gameName);
            return null;
        }

        double avgTime = stepCount > 0 ? totalTime / stepCount : 0;
        long startTime = results.child("startTime").getValue(Long.class) != null ? results.child("startTime").getValue(Long.class) : 0;
        long endTime = results.child("endTime").getValue(Long.class) != null ? results.child("endTime").getValue(Long.class) : startTime;
        String durationStr = GameDurationCalculator.calculateGameDuration(startTime, endTime);

        return new Metric(gameName, startTime, endTime, trueCount, avgTime, durationStr, stepCount, steps);
    }

    private void calcularMetricasParaGraficos(List<Metric> allMetrics) {
        if (allMetrics == null || allMetrics.isEmpty()) return;

        Map<String, List<Metric>> metricsPorJuego = new HashMap<>();
        for (Metric m : allMetrics) {
            metricsPorJuego.computeIfAbsent(m.getGameName(), k -> new ArrayList<>()).add(m);
        }

        Metric lastMetric = allMetrics.stream()
                .max(Comparator.comparingLong(Metric::getStartTime))
                .orElse(null);
        if (lastMetric == null) return;

        List<Metric> metricsSameGame = metricsPorJuego.getOrDefault(lastMetric.getGameName(), new ArrayList<>());
        metricsSameGame.sort((m1, m2) -> Long.compare(m2.getStartTime(), m1.getStartTime()));

        Metric prevSameGame = metricsSameGame.size() > 1 ? metricsSameGame.get(1) : null;

        double cambioTiempo = 0;
        if (prevSameGame != null) {
            cambioTiempo = ((lastMetric.getAverageTime() - prevSameGame.getAverageTime())
                    / prevSameGame.getAverageTime()) * 100;
        }

        int totalSuccessfulSteps = lastMetric.getTrueCount();
        int totalSessionsPlayed = allMetrics.size();
        int totalGamesPlayed = metricsPorJuego.size();

        chartPagerAdapter.setExecutionTimeChange((int) cambioTiempo);
        chartPagerAdapter.setGamesPlayed(totalGamesPlayed);
        chartPagerAdapter.setTotalSteps(lastMetric.getStepCount());
        chartPagerAdapter.setSuccessfulSteps(totalSuccessfulSteps);
        chartPagerAdapter.setTotalSessions(totalSessionsPlayed);
        chartPagerAdapter.setPrescribedSteps(prescribedSteps);

        runOnUiThread(() -> viewPager.getAdapter().notifyDataSetChanged());

        int lastMetricStepCount = lastMetric.getStepCount();
        double lastMetricPrevAvgTime = prevSameGame != null ? prevSameGame.getAverageTime() : -1;

        calcularPuntajeGeneral(
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                lastMetric.getAverageTime(),
                lastMetric.getTrueCount(),
                lastMetricStepCount,
                lastMetricPrevAvgTime
        );

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        fechaUltimaSesion = dateFormat.format(new Date(lastMetric.getStartTime()));
        runOnUiThread(() -> subtitleText.setText(fechaUltimaSesion));
    }

    private void calcularPuntajeGeneral(String userId, double lastGameAvgExecutionTime, int movimientosExitosos,
                                        int lastMetricStepCount, double lastMetricPrevAvgTime) {

        boolean modoValidacion = (prescribedSteps == 0);

        if (!modoValidacion) {
            DatabaseReference prescriptionRef = FirebaseDatabase.getInstance().getReference("healthProfessionals");
            prescriptionRef.get().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) return;

                int prescribedStepsLocal = 0;
                double tiempoIdeal = 0;

                for (DataSnapshot professionalSnapshot : task.getResult().getChildren()) {
                    DataSnapshot patientsSnapshot = professionalSnapshot.child("patients");
                    if (patientsSnapshot.hasChild(userId)) {
                        prescribedStepsLocal = patientsSnapshot.child(userId)
                                .child("prescription").child("steps").getValue(Integer.class);
                        tiempoIdeal = patientsSnapshot.child(userId)
                                .child("prescription").child("duration").getValue(Double.class);
                        break;
                    }
                }

                double precisionMovimientos = (prescribedStepsLocal > 0)
                        ? Math.min(1.0, (double) movimientosExitosos / prescribedStepsLocal)
                        : 0;
                double tiempoFactor = Math.min(1.0, tiempoIdeal / lastGameAvgExecutionTime);

                double puntajeGeneral = (0.7 * precisionMovimientos) + (0.3 * tiempoFactor);
                int puntajeFinal = (int) (puntajeGeneral * 100);

                chartPagerAdapter.setGeneralScore(puntajeFinal);
                runOnUiThread(() -> viewPager.getAdapter().notifyDataSetChanged());
            });

        } else {
            double precisionMovimientos = Math.min(1.0, (double) movimientosExitosos / Math.max(1, lastMetricStepCount));

            double mejoraTiempo = 0;
            if (lastMetricPrevAvgTime > 0) {
                mejoraTiempo = (lastMetricPrevAvgTime - lastGameAvgExecutionTime) / lastMetricPrevAvgTime;
                mejoraTiempo = Math.max(0, Math.min(1.0, mejoraTiempo));
            }

            double puntajeGeneral = (0.7 * precisionMovimientos) + (0.3 * mejoraTiempo);
            int puntajeFinal = (int) (puntajeGeneral * 100);

            chartPagerAdapter.setGeneralScore(puntajeFinal);
            runOnUiThread(() -> viewPager.getAdapter().notifyDataSetChanged());
        }
    }

    private String getRandomMotivationalPhrase() {
        String[] phrases = {
                "Cada paso cuenta. ¡Sigue adelante!",
                "Pequeños logros llevan a grandes cambios.",
                "La constancia es la clave del éxito.",
                "Hoy es un buen día para mejorar.",
                "El esfuerzo de hoy es el éxito de mañana.",
                "Sigue avanzando, cada sesión suma.",
                "No te rindas, ¡estás progresando!",
                "Cree en ti. ¡Puedes lograrlo!",
                "Cada intento te acerca más a tu meta.",
                "Tu determinación te hace más fuerte."
        };
        return phrases[new Random().nextInt(phrases.length)];
    }

    public String getFechaUltimaSesion() {
        return fechaUltimaSesion;
    }
}
