package com.example.vmoov;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ConnectionActivity extends BaseActivity {

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        // Obtener el userId directamente desde Firebase Authentication
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();  // Obtén el userId desde FirebaseAuth

            // Referencia al TextView donde se mostrará el código
            TextView codeTextView = findViewById(R.id.generated_code_text);

            // Recuperar el uniqueCode desde Firebase
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mDatabase.child("patients").child(userId).child("uniqueCode").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        int uniqueCode = dataSnapshot.getValue(Integer.class);  // Recupera el uniqueCode
                        codeTextView.setText(String.valueOf(uniqueCode));  // Mostrar el uniqueCode en el TextView
                    } else {
                        codeTextView.setText("Error: No se pudo recuperar el código desde Firebase.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    codeTextView.setText("Error: " + databaseError.getMessage());
                }
            });

            // Configurar el botón "Comenzar" para volver a MetricsActivity
            CardView startButton = findViewById(R.id.start_card);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Crear un Intent para navegar a MetricsActivity
                    Intent intent = new Intent(ConnectionActivity.this, MenuActivity.class);
                    startActivity(intent);
                    finish(); // Cierra esta actividad para evitar que el usuario regrese a ella
                }
            });
        } else {
            // Si no hay un usuario autenticado, mostrar error
            Log.e("ConnectionActivity", "No se encontró un usuario autenticado.");
            TextView codeTextView = findViewById(R.id.generated_code_text);
            codeTextView.setText("Error: No se encontró el ID de usuario.");
        }
    }
}
