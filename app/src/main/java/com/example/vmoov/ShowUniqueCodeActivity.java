package com.example.vmoov;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ShowUniqueCodeActivity extends AppCompatActivity {

    private TextView codeTextView;
    private CardView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_unique_code);

        codeTextView = findViewById(R.id.generated_code_text);
        backButton = findViewById(R.id.back_card);

        // Recuperar el ID del paciente que se pasó desde el adapter
        String patientId = getIntent().getStringExtra("userId");

        if (patientId != null) {
            // Leer el código único desde Firebase
            fetchPatientCode(patientId);
        } else {
            codeTextView.setText("ID no disponible");
        }

        // Botón Volver
        backButton.setOnClickListener(v -> finish());
    }

    private void fetchPatientCode(String patientId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("patients").child(patientId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.child("uniqueCode").getValue() != null) {
                    String code = String.valueOf(snapshot.child("uniqueCode").getValue());                    codeTextView.setText(code);
                } else {
                    codeTextView.setText("Código no disponible");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                codeTextView.setText("Error al cargar código");
            }
        });
    }
}
