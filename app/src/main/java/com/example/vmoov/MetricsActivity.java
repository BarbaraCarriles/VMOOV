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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MetricsActivity extends BaseActivity {

    private TextView trueCountTextView;
    private TextView lastSessionTextView;
    private TextView averageTimeTextView;
    private BarChart barChart;
    private BarChart barChart2;
    private ImageButton logOutButton;
    private ImageButton settingsButton;
    private ImageButton connectButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private final String TAG = "MetricsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pruebas);

        // Vincular vistas
        trueCountTextView = findViewById(R.id.true_count);
        lastSessionTextView = findViewById(R.id.birth_date);
        averageTimeTextView = findViewById(R.id.average_time);
        barChart = findViewById(R.id.barChart);
        barChart2 = findViewById(R.id.barChart2);

        logOutButton = findViewById(R.id.buttonLogOut);
        settingsButton = findViewById(R.id.buttonSettings);
        connectButton = findViewById(R.id.buttonConnect);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            getGameData(userId);
        }

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
    }

    private void getGameData(String userId) {
        DatabaseReference gameplayRef = mDatabase
                .child("patientmetrics")
                .child(userId)
                .child("gameplaydata");

        gameplayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    trueCountTextView.setText("No data");
                    averageTimeTextView.setText("No data");
                    return;
                }

                List<Metric> allMetrics = new ArrayList<>();
                long latestTime = 0;

                // Recorre todos los juegos
                for (DataSnapshot gameSnapshot : dataSnapshot.getChildren()) {
                    String gameName = gameSnapshot.getKey();

                    for (DataSnapshot sessionSnapshot : gameSnapshot.getChildren()) {
                        DataSnapshot results = sessionSnapshot.child("results");
                        Metric metric = processGenericSession(gameName, results);
                        if (metric == null) continue;

                        allMetrics.add(metric);

                        if (metric.getStartTime() > latestTime) latestTime = metric.getStartTime();
                    }
                }

                lastSessionTextView.setText(latestTime != 0 ? convertTimestampToDate(latestTime) : "No disponible");

                // Calcula métricas por juego y prepara entradas para gráficos
                calcularMetricasParaGraficos(allMetrics);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                trueCountTextView.setText("Error");
                averageTimeTextView.setText("Error");
            }
        });
    }

    private Metric processGenericSession(String gameName, DataSnapshot results) {
        double totalTime = 0;
        int stepCount = 0;
        int trueCount = 0;

        if (gameName.equals("game1")) {
            for (DataSnapshot stepSnapshot : results.child("steps").getChildren()) {
                Boolean result = stepSnapshot.child("result").getValue(Boolean.class);
                Double time = stepSnapshot.child("time").getValue(Double.class);

                boolean isCorrect = result != null && result;
                double stepTime = time != null ? time : 0;

                if (isCorrect) trueCount++;
                totalTime += stepTime;
                stepCount++;
            }

        } else if (gameName.equals("simon")) {
            for (DataSnapshot roundSnapshot : results.child("rounds").getChildren()) {
                for (DataSnapshot moveSnapshot : roundSnapshot.getChildren()) {
                    Boolean correct = moveSnapshot.child("correct").getValue(Boolean.class);
                    Double time = moveSnapshot.child("time").getValue(Double.class);

                    boolean isCorrect = correct != null && correct;
                    double stepTime = time != null ? time : 0;

                    if (isCorrect) trueCount++;
                    totalTime += stepTime;
                    stepCount++;
                }
            }
        } else {
            Log.w(TAG, "Juego desconocido: " + gameName);
            return null;
        }

        double avgTime = stepCount > 0 ? totalTime / stepCount : 0;
        long startTime = results.child("startTime").getValue(Long.class) != null ? results.child("startTime").getValue(Long.class) : 0;
        long endTime = results.child("endTime").getValue(Long.class) != null ? results.child("endTime").getValue(Long.class) : startTime;
        String durationStr = GameDurationCalculator.calculateGameDuration(startTime, endTime);

        return new Metric(gameName, startTime, endTime, trueCount, avgTime, durationStr, stepCount);
    }

    private void calcularMetricasParaGraficos(List<Metric> metrics) {
        if (metrics == null || metrics.isEmpty()) return;

        // Agrupa métricas por juego
        Map<String, List<Metric>> metricsPorJuego = new HashMap<>();
        for (Metric m : metrics) {
            metricsPorJuego.computeIfAbsent(m.getGameName(), k -> new ArrayList<>()).add(m);
        }

        int totalGames = metricsPorJuego.size();
        int totalSuccessfulSteps = 0;
        int totalSessions = 0;
        double promedioCambioTiempo = 0;

        List<BarEntry> avgTimeEntries = new ArrayList<>();
        List<BarEntry> trueCountEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, List<Metric>> entry : metricsPorJuego.entrySet()) {
            List<Metric> lista = entry.getValue();
            lista.sort((m1, m2) -> Long.compare(m2.getStartTime(), m1.getStartTime()));

            double lastAvgTime = lista.get(0).getAverageTime();
            double prevAvgTime = (lista.size() > 1) ? lista.get(1).getAverageTime() : -1;

            double cambio = 0;
            if (prevAvgTime > 0) cambio = ((lastAvgTime - prevAvgTime) / prevAvgTime) * 100;
            promedioCambioTiempo += cambio;

            int gameTrueCount = 0;
            for (Metric m : lista) {
                totalSuccessfulSteps += m.getTrueCount();
                gameTrueCount += m.getTrueCount();
            }

            totalSessions += lista.size();

            // Entradas para gráficos
            avgTimeEntries.add(new BarEntry(index, (float) lastAvgTime));
            trueCountEntries.add(new BarEntry(index, gameTrueCount));
            labels.add(entry.getKey());
            index++;
        }

        promedioCambioTiempo /= Math.max(1, totalGames);

        // Actualiza gráficos
        configureBarChart(barChart, avgTimeEntries, labels);
        configureBarChart(barChart2, trueCountEntries, labels);

        // Actualiza métricas globales (si tuvieras un ChartPagerAdapter)
        averageTimeTextView.setText(String.format(Locale.getDefault(), "%.2f", promedioCambioTiempo));
        trueCountTextView.setText(String.valueOf(totalSuccessfulSteps));
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
