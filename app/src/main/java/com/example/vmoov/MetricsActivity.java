package com.example.vmoov;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class MetricsActivity extends BaseActivity {

    private TextView trueCountTextView;
    private TextView lastSessionTextView;
    private TextView averageTimeTextView;
    private BarChart barChart;
    private BarChart barChart2;
    private ImageButton logOutButton;
    private ImageButton settingsButton;
    private ImageButton connectButton;

    private final String TAG = "MetricsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pruebas);

        // Vistas
        /*trueCountTextView = findViewById(R.id.true_count);
        lastSessionTextView = findViewById(R.id.birth_date);
        averageTimeTextView = findViewById(R.id.average_time);*/
        barChart = findViewById(R.id.barChart);
        barChart2 = findViewById(R.id.barChart2);
        logOutButton = findViewById(R.id.buttonLogOut);
        settingsButton = findViewById(R.id.buttonSettings);
        connectButton = findViewById(R.id.buttonConnect);

        // Botones
        logOutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MetricsActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MetricsActivity.this, MainActivity.class));
            finish();
        });

        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(MetricsActivity.this, SettingsActivity.class)));

        connectButton.setOnClickListener(v ->
                startActivity(new Intent(MetricsActivity.this, ConnectionActivity.class)));

        // Carga de métricas
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            cargarMetricas(userId);
        }
    }

    private void cargarMetricas(String userId) {
        FirebaseDataHelper.fetchAllMetrics(userId, new FirebaseDataHelper.FirebaseCallback<List<Metric>>() {
            @Override
            public void onSuccess(List<Metric> metrics) {
                if (metrics == null || metrics.isEmpty()) {
                    runOnUiThread(() -> {
                        trueCountTextView.setText("Sin datos");
                        averageTimeTextView.setText("Sin datos");
                    });
                    return;
                }

                // 🔹 Agrupa por juego
                Map<String, List<Metric>> metricsPorJuego = new HashMap<>();
                for (Metric m : metrics) {
                    metricsPorJuego.computeIfAbsent(m.getGameName(), k -> new ArrayList<>()).add(m);
                }

                // 🔹 Prepara datos para los gráficos
                List<BarEntry> avgTimeEntries = new ArrayList<>();
                List<BarEntry> trueCountEntries = new ArrayList<>();
                List<String> labels = new ArrayList<>();

                double promedioCambioTiempo = 0;
                int totalSuccessfulSteps = 0;
                int totalGames = metricsPorJuego.size();
                int totalSessions = metrics.size();
                long latestTime = 0;
                int index = 0;

                for (Map.Entry<String, List<Metric>> entry : metricsPorJuego.entrySet()) {
                    List<Metric> lista = entry.getValue();
                    lista.sort((m1, m2) -> Long.compare(m2.getStartTime(), m1.getStartTime()));

                    Metric ultima = lista.get(0);
                    Metric anterior = lista.size() > 1 ? lista.get(1) : null;

                    double cambio = 0;
                    if (anterior != null && anterior.getAverageTime() > 0) {
                        cambio = ((ultima.getAverageTime() - anterior.getAverageTime()) / anterior.getAverageTime()) * 100;
                    }

                    promedioCambioTiempo += cambio;
                    totalSuccessfulSteps += ultima.getTrueCount();

                    avgTimeEntries.add(new BarEntry(index, (float) ultima.getAverageTime()));
                    trueCountEntries.add(new BarEntry(index, ultima.getTrueCount()));
                    labels.add(entry.getKey());
                    index++;

                    if (ultima.getStartTime() > latestTime) latestTime = ultima.getStartTime();
                }

                promedioCambioTiempo /= Math.max(1, totalGames);

                // 🔹 Actualiza UI
                String fechaUltimaSesion = convertTimestampToDate(latestTime);
                runOnUiThread(() -> {

                    // 🔹 Configura gráficos
                    configureBarChart(barChart, avgTimeEntries, labels);
                    configureBarChart(barChart2, trueCountEntries, labels);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error al cargar métricas", e);
                runOnUiThread(() -> Toast.makeText(MetricsActivity.this, "Error al cargar métricas", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void configureBarChart(BarChart chart, List<BarEntry> entries, List<String> labels) {
        BarDataSet dataSet = new BarDataSet(entries, null);
        dataSet.setColors(new int[]{0xFFA36BFA, 0xFF5C4CF1});
        dataSet.setValueTextSize(14f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.2f", value);
            }
        });

        BarData barData = new BarData(dataSet);
        chart.setData(barData);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getDescription().setEnabled(false);
        chart.animateY(1500);
        chart.invalidate();
    }

    private String convertTimestampToDate(long timestamp) {
        if (timestamp == 0) return "No disponible";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
