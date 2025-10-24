package com.example.vmoov;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotPatientActivity extends BaseActivity implements RecyclerViewInterface {

    private RecyclerView recyclerViewPatients;
    private PatientsAdapter patientsAdapter;
    private List<String> patientNames;
    private List<String> patientIDs;
    private TextView professionalNameTextView; // TextView para mostrar el nombre del profesional

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_not_patient);

        recyclerViewPatients = findViewById(R.id.recyclerViewPatients);
        recyclerViewPatients.setLayoutManager(new LinearLayoutManager(this));

        patientNames = new ArrayList<>();
        patientIDs = new ArrayList<>();
        patientsAdapter = new PatientsAdapter(this, patientNames, patientIDs);
        recyclerViewPatients.setAdapter(patientsAdapter);

        professionalNameTextView = findViewById(R.id.user_name_textview); // Referencia al TextView

        // Obtener el usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            fetchProfessionalName(userId); // Obtener el nombre del profesional de salud
            fetchPatientNames(userId); // Iniciar la búsqueda de los pacientes vinculados
        }

        setupButtons();  // Configurar los botones
    }

    private void fetchProfessionalName(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String firstName = dataSnapshot.child("firstName").getValue(String.class);
                    String lastName = dataSnapshot.child("lastName").getValue(String.class);

                    if (firstName != null && lastName != null) {
                        String fullName = firstName + " " + lastName;
                        professionalNameTextView.setText(fullName); // Mostrar el nombre en pantalla
                    } else {
                        professionalNameTextView.setText("Nombre no disponible");
                    }
                } else {
                    professionalNameTextView.setText("No se encontró información del profesional");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("NotPatientActivity", "Error en la base de datos: " + databaseError.getMessage());
            }
        });
    }


    private void fetchPatientNames(String userId) {
        DatabaseReference healthProfessionalRef = FirebaseDatabase.getInstance().getReference()
                .child("healthProfessionals").child(userId).child("patients");

        // Escuchar los cambios en tiempo real
        healthProfessionalRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 🧹 Limpiar listas antes de volver a cargar
                patientNames.clear();
                patientIDs.clear();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot patientSnapshot : dataSnapshot.getChildren()) {
                        String patientId = patientSnapshot.getKey();
                        Log.d("NotPatientActivity", "Obtenido patientId: " + patientId);
                        fetchUserName(patientId);
                    }
                } else {
                    Log.d("NotPatientActivity", "No se encontraron pacientes vinculados para el userId: " + userId);
                    patientsAdapter.notifyDataSetChanged(); // limpia la vista si ya no hay pacientes
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("NotPatientActivity", "Error en la base de datos: " + databaseError.getMessage());
            }
        });
    }

    private void fetchUserName(String patientId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(patientId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String firstName = dataSnapshot.child("firstName").getValue(String.class);
                    String lastName = dataSnapshot.child("lastName").getValue(String.class);

                    if (firstName != null && lastName != null) {
                        String fullName = firstName + " " + lastName;
                        Log.d("NotPatientActivity", "Nombre del paciente: " + fullName);
                        patientNames.add(fullName);
                        patientIDs.add(patientId);
                    } else {
                        Log.d("NotPatientActivity", "Nombre no disponible para patientId: " + patientId);
                        patientNames.add("Nombre no disponible");
                        patientIDs.add(patientId);
                    }

                    patientsAdapter.notifyDataSetChanged();
                } else {
                    Log.d("NotPatientActivity", "No se encontró el nodo del usuario para patientId: " + patientId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("NotPatientActivity", "Error en la base de datos: " + databaseError.getMessage());
            }
        });
    }

    private void setupButtons() {
        // Botón para agregar nuevo contacto
        ImageButton addContactButton = findViewById(R.id.buttonAddContact);
        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NotPatientActivity.this, NewContactActivity.class);
                startActivity(intent);
            }
        });

        // Botón para cerrar sesión
        ImageButton logOutButton = findViewById(R.id.buttonLogOut);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(NotPatientActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // ✅ Botón para ir a HealthProfessionalInfoActivity
        ImageButton addInfoButton = findViewById(R.id.buttonAddInfo);
        addInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NotPatientActivity.this, HealthProfessionalInfoActivity.class);
                intent.putExtra("source", "NotPatientActivity"); // Indicar que vienes desde NotPatientActivity
                startActivity(intent);
            }
        });
    }

    @Override
    public void onItemClick(int position) {
        Log.d("NotPatientActivity", "Patient ID list size: " + patientIDs.size());
        Log.d("NotPatientActivity", "Clicked position: " + position);

        if (position < patientIDs.size()) {
            Intent intent = new Intent(NotPatientActivity.this, PatientDisplayActivity.class);
            intent.putExtra("userId", patientIDs.get(position));
            Log.d("NotPatientActivity", "Opening PatientDisplayActivity with userId: " + patientIDs.get(position));
            startActivity(intent);
        } else {
            Log.e("NotPatientActivity", "Invalid position clicked: " + position);
        }
    }
}
