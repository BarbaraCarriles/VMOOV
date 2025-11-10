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
import java.util.stream.Collectors;

public class LastSessionActivity extends BaseActivity {

    private TextView titleText, gameNameText, subtitleText, motivationalText, chartTitle, chartDescription;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ChartPagerAdapter chartPagerAdapter;

    private final String TAG = "LastSessionActivity";

    private String fechaUltimaSesion = "Fecha desconocida";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last_session);

        // UI
        titleText = findViewById(R.id.title_text);
        gameNameText = findViewById(R.id.game_name_text);
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
                updateChartTitleAndDescription(position);
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
        fetchAndDisplayMetrics(userId);
    }

    // -----------------------------
    // obtener prescripciones de paciente, actualmente sin uso
    // -----------------------------
    private void obtenerPrescripciones(String userId, Runnable callback) {
        FirebaseDataHelper.fetchPatientPrescriptions(userId, new FirebaseDataHelper.FirebaseCallback<List<Prescription>>() {
            @Override
            public void onSuccess(List<Prescription> result) {
                // Por ahora no se hace nada con las prescripciones
                callback.run();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error obteniendo prescripciones", e);
                callback.run();
            }
        });
    }

    private void fetchAndDisplayMetrics(String userId) {
        FirebaseDataHelper.fetchAllMetrics(userId, new FirebaseDataHelper.FirebaseCallback<List<Metric>>() {
            @Override
            public void onSuccess(List<Metric> allMetrics) {
                if (allMetrics == null || allMetrics.isEmpty()) return;

                Metric lastMetric = allMetrics.stream()
                        .max(Comparator.comparingLong(Metric::getStartTime))
                        .orElse(null);
                if (lastMetric == null) return;

                GameMetric gameMetric;
                gameNameText.setText(lastMetric.getGameName());

                if ("simon".equalsIgnoreCase(lastMetric.getGameName())) {
                    gameMetric = GameMetric.buildSimonMetric(lastMetric);
                } else if ("game1".equalsIgnoreCase(lastMetric.getGameName())) {
                    Metric previousMetric = findPreviousMetric(allMetrics, lastMetric); // función auxiliar que busca la penúltima del mismo juego
                    gameMetric = GameMetric.buildGame1Metric(lastMetric, previousMetric);

                } else {
                    return; // juego no soportado
                }

                // -------------------------
                // Agregamos número de sesiones totales jugadas
                // -------------------------
                int totalSessionsPlayed = allMetrics.size();
                GameMetric.MetricItem totalSessionsItem = new GameMetric.MetricItem(
                        GameMetric.MetricType.NUMBER,
                        totalSessionsPlayed,
                        0,
                        0,
                        "Sesiones totales"
                );
                gameMetric.addMetricItem(totalSessionsItem);

                chartPagerAdapter.setGameMetric(gameMetric);

                // Fecha de la última sesión
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                fechaUltimaSesion = dateFormat.format(new Date(lastMetric.getStartTime()));

                runOnUiThread(() -> {
                    subtitleText.setText(fechaUltimaSesion);
                    viewPager.getAdapter().notifyDataSetChanged();
                    updateChartTitleAndDescription(viewPager.getCurrentItem());
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error obteniendo métricas", e);
            }
        });
    }

    private void updateChartTitleAndDescription(int position) {
        if (chartPagerAdapter.getGameMetric() == null) return;
        List<GameMetric.MetricItem> metrics = chartPagerAdapter.getGameMetric().getMetricsList();
        if (position < 0 || position >= metrics.size()) return;

        GameMetric.MetricItem item = metrics.get(position);
        chartTitle.setText(item.getLabel());
        chartDescription.setText(getDescriptionForMetric(item));
    }


    private Metric findPreviousMetric(List<Metric> allMetrics, Metric lastMetric) {
        List<Metric> filtered = allMetrics.stream()
                .filter(m -> m.getGameName().equalsIgnoreCase(lastMetric.getGameName()))
                .sorted(Comparator.comparingLong(Metric::getStartTime).reversed())
                .collect(Collectors.toList());
        return filtered.size() > 1 ? filtered.get(1) : null;
    }

    private String getDescriptionForMetric(GameMetric.MetricItem item) {
        switch (item.getType()) {
            case SCORE:
                return "Evaluación general del desempeño del paciente basada en múltiples métricas.";
            case RATIO:
                if (item.getLabel().toLowerCase().contains("movimiento")) {
                    return "Proporción de movimientos exitosos sobre el total de intentos.";
                }
                else if (item.getLabel().toLowerCase().contains("rondas")) {
                    return "Proporción de rondas exitosas sobre el total de rondas establecidas.";
                } else return "";
            case NUMBER:
                if (item.getLabel().toLowerCase().contains("sesiones")) {
                    return "Cantidad total de sesiones realizadas por el paciente.";
                } else return "";
            case TIME_TEXT:
                return "Cambio porcentual en el tiempo promedio por movimiento en comparación a la anteúltima sesion.";
            case TEXT:
                if (item.getLabel().toLowerCase().contains("dificultad")) {
                    return "Nivel de dificultad de la última sesión jugada.";
                } else return "";
            case PIE_CHART:
                return "Precisión por botón en la última sesión.";
            case BAR_CHART:
                return "Tiempo promedio por botón en la última sesión. Un menor tiempo indica mejor rendimiento";
            default:
                return "";
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

}
