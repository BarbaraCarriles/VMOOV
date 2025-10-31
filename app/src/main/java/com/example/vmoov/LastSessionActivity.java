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

import java.text.SimpleDateFormat;
import java.util.*;

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
            "Nivel de dificultad",
    };

    private final String[] chartDescriptionsEntrenamiento = {
            "Evaluación general del desempeño del paciente basada en múltiples métricas.",
            "Cambio porcentual en el tiempo promedio de ejecución entre las últimas dos sesiones.",
            "Proporción de movimientos exitosos sobre el total de la prescripción.",
            "Cantidad de sesiones realizadas respecto del total recetado.",
            "Nivel de dificultad de la última sesion jugada.",

    };

    private final String[] chartDescriptionsValidacion = {
            "Evaluación general del desempeño del paciente basada en múltiples métricas.",
            "Cambio porcentual en el tiempo promedio de ejecución entre las últimas dos sesiones.",
            "Proporción de movimientos exitosos sobre el total de intentos realizados.",
            "Cantidad total de sesiones realizadas por el paciente.",
            "Nivel de dificultad de la última sesion jugada.",

    };

    private String[] chartDescriptionsActuales = chartDescriptionsEntrenamiento;

    private int prescribedSteps = 0;

    private int prescribedSessions = 0;
    private String fechaUltimaSesion = "Fecha desconocida";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last_session);

        // UI
        titleText = findViewById(R.id.title_text);
        subtitleText = findViewById(R.id.subtitle_text);
        motivationalText = findViewById(R.id.motivational_text);
        chartTitle = findViewById(R.id.chart_title);
        chartDescription = findViewById(R.id.chart_description);
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tab_layout);

        chartPagerAdapter = new ChartPagerAdapter(this);
        viewPager.setAdapter(chartPagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText("#" + (position + 1))).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                chartTitle.setText(chartTitles[position]);
                chartDescription.setText(chartDescriptionsActuales[position]);
            }
        });

        motivationalText.setText(getRandomMotivationalPhrase());

        ImageButton homeButton = findViewById(R.id.back_button);
        homeButton.setOnClickListener(v -> startActivity(new Intent(LastSessionActivity.this, MenuActivity.class)));

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado.");
            titleText.setText("Usuario no autenticado");
            return;
        }

        String userId = currentUser.getUid();

        // Primero obtenemos prescripciones, luego métricas
        obtenerPrescripciones(userId, () -> fetchAndDisplayMetrics(userId));
    }

    private void obtenerPrescripciones(String userId, Runnable callback) {
        FirebaseDataHelper.fetchPatientPrescriptions(userId, new FirebaseDataHelper.FirebaseCallback<List<Prescription>>() {
            @Override
            public void onSuccess(List<Prescription> result) {
                if (result != null && !result.isEmpty()) {
                    Prescription p = result.get(0); // tomamos la primera (asumiendo una prescripción por paciente)
                    prescribedSteps = p.getSteps();
                    prescribedSessions = p.getSessions();
                    chartPagerAdapter.setModoValidacion(false);
                    chartDescriptionsActuales = chartDescriptionsEntrenamiento;
                } else {
                    prescribedSteps = 0;
                    prescribedSessions = 0;
                    chartPagerAdapter.setModoValidacion(true);
                    chartDescriptionsActuales = chartDescriptionsValidacion;
                }
                callback.run();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error obteniendo prescripciones", e);
                prescribedSteps = 0;
                prescribedSessions = 0;
                chartPagerAdapter.setModoValidacion(true);
                chartDescriptionsActuales = chartDescriptionsValidacion;
                callback.run();
            }
        });
    }

    private void fetchAndDisplayMetrics(String userId) {
        FirebaseDataHelper.fetchAllMetrics(userId, new FirebaseDataHelper.FirebaseCallback<List<Metric>>() {
            @Override
            public void onSuccess(List<Metric> allMetrics) {
                if (allMetrics == null || allMetrics.isEmpty()) return;

                // Última sesión
                Metric lastMetric = allMetrics.stream()
                        .max(Comparator.comparingLong(Metric::getStartTime))
                        .orElse(null);
                if (lastMetric == null) return;

                // Sesión anterior del mismo juego
                List<Metric> sameGameMetrics = new ArrayList<>();
                for (Metric m : allMetrics) if (m.getGameName().equals(lastMetric.getGameName())) sameGameMetrics.add(m);
                sameGameMetrics.sort((m1, m2) -> Long.compare(m2.getStartTime(), m1.getStartTime()));
                Metric prevMetric = sameGameMetrics.size() > 1 ? sameGameMetrics.get(1) : null;

                //Para calcular el cambio porcentual en el tiempo entre las ultimas 2 sesiones
                double cambioTiempo = 0;
                if (prevMetric != null && prevMetric.getAverageTime() > 0) {
                    cambioTiempo = ((lastMetric.getAverageTime() - prevMetric.getAverageTime()) / prevMetric.getAverageTime()) * 100;
                }

                int totalSuccessfulSteps = lastMetric.getTrueCount();
                int totalGamesPlayed = (int) allMetrics.stream().map(Metric::getGameName).distinct().count();
                int totalSessionsPlayed = allMetrics.size();
                int difficultyLevel = lastMetric.getDifficulty();

                chartPagerAdapter.setExecutionTimeChange((int) cambioTiempo);
                chartPagerAdapter.setTotalSteps(lastMetric.getStepCount());
                chartPagerAdapter.setSuccessfulSteps(totalSuccessfulSteps);
                chartPagerAdapter.setSessionsPlayed(totalSessionsPlayed);
                chartPagerAdapter.setGamesPlayed(totalGamesPlayed);
                chartPagerAdapter.setPrescribedSteps(prescribedSteps);
                chartPagerAdapter.setPrescribedSessions(prescribedSessions);
                chartPagerAdapter.setDifficultyLevel(difficultyLevel);

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                fechaUltimaSesion = dateFormat.format(new Date(lastMetric.getStartTime()));

                runOnUiThread(() -> viewPager.getAdapter().notifyDataSetChanged());

                // Calcular puntaje general de la última sesión
                calcularPuntajeGeneral(userId, lastMetric, prevMetric);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error obteniendo métricas", e);
            }
        });
    }

    private void calcularPuntajeGeneral(String userId, Metric lastMetric, Metric prevMetric) {
        boolean modoValidacion = prescribedSteps == 0;

        double lastAvgTime = lastMetric.getAverageTime();
        int movimientosExitosos = lastMetric.getTrueCount();
        int lastStepCount = lastMetric.getStepCount();
        double prevAvgTime = prevMetric != null ? prevMetric.getAverageTime() : -1;

        double puntajeGeneral;
        if (!modoValidacion) {
            puntajeGeneral = Math.min(1.0, (double) movimientosExitosos / Math.max(1, prescribedSteps));
        } else {
            double precisionMovimientos = Math.min(1.0, (double) movimientosExitosos / Math.max(1, lastStepCount));
            double mejoraTiempo = 0;
            if (prevAvgTime > 0) {
                mejoraTiempo = (prevAvgTime - lastAvgTime) / prevAvgTime;
                mejoraTiempo = Math.max(0, Math.min(1.0, mejoraTiempo));
            }
            puntajeGeneral = 0.7 * precisionMovimientos + 0.3 * mejoraTiempo;
        }

        int puntajeFinal = (int) (puntajeGeneral * 100);
        chartPagerAdapter.setGeneralScore(puntajeFinal);
        runOnUiThread(() -> viewPager.getAdapter().notifyDataSetChanged());
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

}
