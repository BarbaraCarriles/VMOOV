package com.example.vmoov;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class Chart1Fragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_TYPE = "type";
    private static final String ARG_BARS = "bars";
    private static final String ARG_IS_DURATION = "is_duration";
    private static final String ARG_IS_MOVEMENTS = "is_movements";
    private static final String ARG_GAME_ID = "game_id";

    private DatabaseReference mDatabase;

    public static Chart1Fragment newInstance(String title, String description, String type, int bars,
                                             boolean isDurationChart, boolean isTrueCountChart, String gameId) {
        Chart1Fragment fragment = new Chart1Fragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_TYPE, type);
        args.putInt(ARG_BARS, bars);
        args.putBoolean("isDurationChart", isDurationChart);
        args.putBoolean("isTrueCountChart", isTrueCountChart);
        args.putString(ARG_GAME_ID, gameId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chart1, container, false);

        TextView titleTextView = view.findViewById(R.id.chartTitle);
        TextView descriptionTextView = view.findViewById(R.id.chartDescription);
        BarChart barChart = view.findViewById(R.id.barChart);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            String description = getArguments().getString(ARG_DESCRIPTION);
            String type = getArguments().getString(ARG_TYPE);
            int numberOfBars = getArguments().getInt(ARG_BARS);
            boolean isDurationChart = getArguments().getBoolean("isDurationChart");
            boolean isTrueCountChart = getArguments().getBoolean("isTrueCountChart");
            String gameId = getArguments().getString(ARG_GAME_ID, "game1");

            titleTextView.setText(title);
            descriptionTextView.setText(description);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                mDatabase = FirebaseDatabase.getInstance()
                        .getReference("patientmetrics")
                        .child(currentUser.getUid())
                        .child("gameplaydata")
                        .child(gameId)
                        .child("sessions");

                updateChartData(barChart, type, numberOfBars, isDurationChart, isTrueCountChart);
            }
        }

        return view;
    }

    private void updateChartData(BarChart barChart, String type, int numberOfBars,
                                 boolean isDurationChart, boolean isTrueCountChart) {
        if (mDatabase == null) return;

        mDatabase.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<DataSnapshot> games = new ArrayList<>();
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    games.add(snapshot);
                }

                games.sort((g1, g2) -> {
                    Long t1 = g1.child("results").child("startTime").getValue(Long.class);
                    Long t2 = g2.child("results").child("startTime").getValue(Long.class);
                    return t1 != null && t2 != null ? t1.compareTo(t2) : 0;
                });

                switch (type) {
                    case "Sesiones":
                        updateChartsForSessions(barChart, games, numberOfBars, isDurationChart, isTrueCountChart);
                        break;
                    case "Semanas":
                        updateChartsForWeeks(barChart, games, numberOfBars, isDurationChart, isTrueCountChart);
                        break;
                    case "Meses":
                        updateChartsForMonths(barChart, games, numberOfBars, isDurationChart, isTrueCountChart);
                        break;
                }
            }
        });
    }

    private void updateChartsForSessions(BarChart barChart, List<DataSnapshot> games, int numberOfBars,
                                         boolean isDurationChart, boolean isTrueCountChart) {
        List<BarEntry> barEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int totalGames = games.size();
        int startIndex = Math.max(0, totalGames - numberOfBars);

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

        for (int i = startIndex; i < totalGames; i++) {
            DataSnapshot gameSnapshot = games.get(i);
            DataSnapshot resultsSnapshot = gameSnapshot.child("results");
            double value = isDurationChart ? getDurationTime(resultsSnapshot) : getTrueCount(resultsSnapshot);

            Long startTime = resultsSnapshot.child("startTime").getValue(Long.class);
            String label = startTime != null ? dateFormatter.format(new Date(startTime)) : "Sin fecha";

            barEntries.add(new BarEntry(i - startIndex, (float) value));
            labels.add(label);
        }

        configureBarChart(barChart, barEntries, labels, isDurationChart);
    }

    private void updateChartsForWeeks(BarChart barChart, List<DataSnapshot> games, int numberOfBars,
                                      boolean isDurationChart, boolean isTrueCountChart) {
        SimpleDateFormat weekFormatter = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        Map<Long, List<DataSnapshot>> weeklyData = new TreeMap<>();
        Calendar calendar = Calendar.getInstance();

        for (DataSnapshot game : games) {
            Long startTime = game.child("results").child("startTime").getValue(Long.class);
            if (startTime != null) {
                calendar.setTimeInMillis(startTime);
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long weekStart = calendar.getTimeInMillis();

                weeklyData.putIfAbsent(weekStart, new ArrayList<>());
                weeklyData.get(weekStart).add(game);
            }
        }

        List<Map.Entry<Long, List<DataSnapshot>>> orderedWeeks = new ArrayList<>(weeklyData.entrySet());
        int startIndex = Math.max(0, orderedWeeks.size() - numberOfBars);
        orderedWeeks = orderedWeeks.subList(startIndex, orderedWeeks.size());

        List<BarEntry> barEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<Long, List<DataSnapshot>> entry : orderedWeeks) {
            List<DataSnapshot> weekGames = entry.getValue();
            double totalValue = 0;
            for (DataSnapshot game : weekGames) {
                totalValue += isDurationChart ? getDurationTime(game.child("results")) : getTrueCount(game.child("results"));
            }
            double averageValue = totalValue / weekGames.size();

            calendar.setTimeInMillis(entry.getKey());
            String weekLabel = weekFormatter.format(calendar.getTime()) + " - ";
            calendar.add(Calendar.DATE, 6);
            weekLabel += weekFormatter.format(calendar.getTime());

            barEntries.add(new BarEntry(index, (float) averageValue));
            labels.add(weekLabel);
            index++;
        }

        configureBarChart(barChart, barEntries, labels, isDurationChart);
    }

    private void updateChartsForMonths(BarChart barChart, List<DataSnapshot> games, int numberOfBars,
                                       boolean isDurationChart, boolean isTrueCountChart) {
        SimpleDateFormat monthFormatter = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        Map<Long, List<DataSnapshot>> monthlyData = new TreeMap<>();
        Calendar calendar = Calendar.getInstance();

        for (DataSnapshot game : games) {
            Long startTime = game.child("results").child("startTime").getValue(Long.class);
            if (startTime != null) {
                calendar.setTimeInMillis(startTime);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long monthStart = calendar.getTimeInMillis();

                monthlyData.putIfAbsent(monthStart, new ArrayList<>());
                monthlyData.get(monthStart).add(game);
            }
        }

        List<Map.Entry<Long, List<DataSnapshot>>> orderedMonths = new ArrayList<>(monthlyData.entrySet());
        int startIndex = Math.max(0, orderedMonths.size() - numberOfBars);
        orderedMonths = orderedMonths.subList(startIndex, orderedMonths.size());

        List<BarEntry> barEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<Long, List<DataSnapshot>> entry : orderedMonths) {
            List<DataSnapshot> monthGames = entry.getValue();
            double totalValue = 0;
            for (DataSnapshot game : monthGames) {
                totalValue += isDurationChart ? getDurationTime(game.child("results")) : getTrueCount(game.child("results"));
            }
            double averageValue = totalValue / monthGames.size();

            calendar.setTimeInMillis(entry.getKey());
            String monthLabel = capitalizeFirstLetter(monthFormatter.format(calendar.getTime()));

            barEntries.add(new BarEntry(index, (float) averageValue));
            labels.add(monthLabel);
            index++;
        }

        configureBarChart(barChart, barEntries, labels, isDurationChart);
    }

    private double getDurationTime(DataSnapshot resultsSnapshot) {
        double total = 0;
        int count = 0;

        if (resultsSnapshot.child("steps").exists()) {
            for (DataSnapshot step : resultsSnapshot.child("steps").getChildren()) {
                Double time = step.child("time").getValue(Double.class);
                if (time != null) {
                    total += time;
                    count++;
                }
            }
        } else if (resultsSnapshot.child("rounds").exists()) {
            for (DataSnapshot round : resultsSnapshot.child("rounds").getChildren()) {
                for (DataSnapshot step : round.getChildren()) {
                    Double time = step.child("time").getValue(Double.class);
                    if (time != null) {
                        total += time;
                        count++;
                    }
                }
            }
        }

        return count > 0 ? total / count : 0;
    }

    private int getTrueCount(DataSnapshot resultsSnapshot) {
        int count = 0;

        if (resultsSnapshot.child("steps").exists()) {
            for (DataSnapshot step : resultsSnapshot.child("steps").getChildren()) {
                Boolean result = step.child("result").getValue(Boolean.class);
                if (result == null) result = step.child("correct").getValue(Boolean.class);
                if (result != null && result) count++;
            }
        } else if (resultsSnapshot.child("rounds").exists()) {
            for (DataSnapshot round : resultsSnapshot.child("rounds").getChildren()) {
                for (DataSnapshot step : round.getChildren()) {
                    Boolean correct = step.child("correct").getValue(Boolean.class);
                    if (correct != null && correct) count++;
                }
            }
        }

        return count;
    }

    private void configureBarChart(BarChart chart, List<BarEntry> entries, List<String> labels, boolean isDurationChart) {
        if (!isAdded()) return;

        if (entries.isEmpty()) {
            chart.clear();
            chart.setNoDataText("No hay datos disponibles");
            chart.invalidate();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(
                ContextCompat.getColor(requireContext(), R.color.violet),
                ContextCompat.getColor(requireContext(), R.color.blue)
        );
        Typeface typeface = ResourcesCompat.getFont(requireContext(), R.font.verdana);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTypeface(typeface);

        // 🔹 Formateamos distinto según el tipo de gráfico
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (isDurationChart) {
                    return String.format(Locale.getDefault(), "%.1f", value); // segundos con decimales
                } else {
                    return String.format(Locale.getDefault(), "%.0f", value); // movimientos enteros
                }
            }
        });

        BarData barData = new BarData(dataSet);
        chart.setData(barData);

        float maxY = entries.isEmpty() ? 1 : Collections.max(entries, Comparator.comparing(BarEntry::getY)).getY();

        // 🔹 Si son movimientos, ajustamos el eje Y a enteros
        if (!isDurationChart) {
            chart.getAxisLeft().setGranularity(1f);
            chart.getAxisLeft().setGranularityEnabled(true);
        }

        chart.getAxisLeft().setAxisMaximum(maxY + 1);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return isDurationChart ? value + " s" : String.format(Locale.getDefault(), "%.0f mov.", value);
            }
        });

        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setTypeface(typeface);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setLabelRotationAngle(270);
        xAxis.setTextSize(12f);
        xAxis.setLabelCount(labels.size());
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setXOffset(-50f);

        chart.getLegend().setEnabled(false);
        chart.setFitBars(true);
        chart.getDescription().setEnabled(false);
        chart.animateY(1500);
        chart.invalidate();
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

}
