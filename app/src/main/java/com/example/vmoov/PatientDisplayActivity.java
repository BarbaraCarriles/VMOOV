package com.example.vmoov;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.*;

public class PatientDisplayActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private MetricsAdapter metricsAdapter;
    private List<Metric> metricsList;
    private List<Metric> filteredMetricsList;
    private TextView patientNameTextView;
    private Spinner spinnerMonth, spinnerOrder, spinnerGame;
    private CardView backCard;

    private static final String TAG = "PatientDisplayActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_display);

        patientNameTextView = findViewById(R.id.user_name);
        recyclerView = findViewById(R.id.recyclerViewMetrics);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        backCard = findViewById(R.id.back_card);
        spinnerMonth = findViewById(R.id.spinner_month);
        spinnerOrder = findViewById(R.id.spinner_order);
        spinnerGame = findViewById(R.id.spinner_game);


        metricsList = new ArrayList<>();
        filteredMetricsList = new ArrayList<>();
        metricsAdapter = new MetricsAdapter(filteredMetricsList);
        recyclerView.setAdapter(metricsAdapter);

        backCard.setOnClickListener(v -> {
            startActivity(new Intent(PatientDisplayActivity.this, NotPatientActivity.class));
        });

        String patientId = getIntent().getStringExtra("userId");
        Log.d(TAG, "Received patientId: " + patientId);

        if (patientId != null) {
            fetchPatientName(patientId);
            fetchPatientMetrics(patientId);
        } else {
            Log.e(TAG, "No patientId provided in Intent.");
        }

        setupSpinners();
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(this,
                R.array.months_array, android.R.layout.simple_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFiltersAndSorting(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<CharSequence> orderAdapter = ArrayAdapter.createFromResource(this,
                R.array.order_array, android.R.layout.simple_spinner_item);
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrder.setAdapter(orderAdapter);
        spinnerOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFiltersAndSorting(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        //spinner nombre del juego
        ArrayAdapter<CharSequence> gameAdapter = ArrayAdapter.createFromResource(this,
                R.array.filter_game_array, android.R.layout.simple_spinner_item);
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGame.setAdapter(gameAdapter);
        spinnerGame.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFiltersAndSorting();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }

    private void fetchPatientName(String patientId) {

        FirebaseDatabase.getInstance().getReference().child("users").child(patientId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String firstName = snapshot.child("firstName").getValue(String.class);
                            String lastName = snapshot.child("lastName").getValue(String.class);
                            patientNameTextView.setText((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : ""));
                        } else {
                            patientNameTextView.setText("Nombre no disponible");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
    }

    private void fetchPatientMetrics(String patientId) {
        FirebaseDataHelper.fetchAllMetrics(patientId, new FirebaseDataHelper.FirebaseCallback<List<Metric>>() {
            @Override
            public void onSuccess(List<Metric> result) {
                if (result == null || result.isEmpty()) return;

                metricsList.clear();
                metricsList.addAll(result);
                applyFiltersAndSorting();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error fetching metrics: " + e.getMessage());
            }
        });
    }

    private void applyFiltersAndSorting() {
        String selectedMonth = spinnerMonth.getSelectedItem().toString().toLowerCase();
        String selectedOrder = spinnerOrder.getSelectedItem().toString();
        String selectedGame = spinnerGame.getSelectedItem().toString().toLowerCase();

        filteredMetricsList.clear();

        for (Metric metric : metricsList) {
            String metricMonth = getMonthFromTimestamp(metric.getStartTime()).toLowerCase();
            String metricGame = metric.getGameName() != null ? metric.getGameName().toLowerCase() : "";

            boolean matchesMonth = selectedMonth.equals("todos") || metricMonth.equals(selectedMonth);
            boolean matchesGame = selectedGame.equals("todos") || metricGame.equals(selectedGame);

            if (matchesMonth && matchesGame) {
                filteredMetricsList.add(metric);
            }
        }

        switch (selectedOrder) {
            case "Fecha (más antiguo a más nuevo)":
                filteredMetricsList.sort(Comparator.comparingLong(Metric::getStartTime));
                break;
            case "Fecha (más nuevo a más antiguo)":
                filteredMetricsList.sort((m1, m2) -> Long.compare(m2.getStartTime(), m1.getStartTime()));
                break;
            case "Tiempo de ejecución (Mayor a menor)":
                filteredMetricsList.sort((m1, m2) -> Double.compare(m2.getAverageTime(), m1.getAverageTime()));
                break;
            case "Duración de sesión (Mayor a menor)":
                filteredMetricsList.sort((m1, m2) -> Double.compare(
                        GameDurationCalculator.calculateDurationSeconds(m2.getStartTime(), m2.getEndTime()),
                        GameDurationCalculator.calculateDurationSeconds(m1.getStartTime(), m1.getEndTime())
                ));
                break;
            case "Cantidad de movimientos correctos (Mayor a menor)":
                filteredMetricsList.sort((m1, m2) -> Integer.compare(m2.getTrueCount(), m1.getTrueCount()));
                break;
        }

        metricsAdapter.notifyDataSetChanged();
    }

    private String getMonthFromTimestamp(long timestamp) {
        if (timestamp <= 0) return "desconocido";
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
