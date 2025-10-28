package com.example.vmoov;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SummaryActivity extends BaseActivity {

    private Spinner spinnerNumber, spinnerType, spinnerGame;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter viewPagerAdapter;
    private TabLayoutMediator tabLayoutMediator; // Para evitar múltiples adjuntos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // Botón Volver
        CardView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SummaryActivity.this, MenuActivity.class);
            startActivity(intent);
            finish();
        });

        // Inicializar vistas
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tab_layout);
        spinnerNumber = findViewById(R.id.spinner_number);
        spinnerType = findViewById(R.id.spinner_type);
        spinnerGame = findViewById(R.id.spinner_game);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance()
                    .getReference("patientmetrics")
                    .child(currentUser.getUid());
        }

        // Listeners para actualizar los gráficos al cambiar cualquier spinner
        SpinnerListener listener = new SpinnerListener();
        spinnerNumber.setOnItemSelectedListener(listener);
        spinnerType.setOnItemSelectedListener(listener);
        spinnerGame.setOnItemSelectedListener(listener);

        // Cargar gráficos por defecto
        updateCharts();
    }

    private class SpinnerListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            updateCharts();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private void updateCharts() {
        if (spinnerNumber.getSelectedItem() == null ||
                spinnerType.getSelectedItem() == null ||
                spinnerGame.getSelectedItem() == null) return;

        int numberOfBars = Integer.parseInt(spinnerNumber.getSelectedItem().toString());
        String type = spinnerType.getSelectedItem().toString();
        String gameId = spinnerGame.getSelectedItem().toString();

        // Crear adaptador ViewPager con los valores seleccionados
        viewPagerAdapter = new ViewPagerAdapter(this, numberOfBars, type, gameId);
        viewPager.setAdapter(viewPagerAdapter);

        // Evitar múltiples adjuntos de TabLayoutMediator
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
        }

        tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("#1");
                    break;
                case 1:
                    tab.setText("#2");
                    break;
            }
        });

        tabLayoutMediator.attach();
    }
}
