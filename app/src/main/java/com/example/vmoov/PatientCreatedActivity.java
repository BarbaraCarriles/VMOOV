package com.example.vmoov;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PatientCreatedActivity extends AppCompatActivity {

    private TextView generatedCodeText;
    private TextView buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_patient_unique_code);

        // Referencias a vistas según el nuevo XML
        generatedCodeText = findViewById(R.id.generated_code_text);
        buttonBack = findViewById(R.id.buttonBack);

        // Obtener código único desde el intent
        int uniqueCode = getIntent().getIntExtra("uniqueCode", -1);
        if(uniqueCode != -1) {
            generatedCodeText.setText(String.valueOf(uniqueCode));
        } else {
            generatedCodeText.setText("Error: no se generó código.");
        }

        // Botón para volver a NewContactActivity
        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(PatientCreatedActivity.this, NewContactActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
