package com.example.vmoov;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NewContactActivity extends AppCompatActivity {

    private EditText editTextInitials, editTextUniqueCode;
    private Spinner centerSpinner;
    private CardView createCard, saveCard, backCard;

    private FirebaseAuth mAuth; 
    private DatabaseReference mDatabase;

    private SharedPreferences preferences;
    private List<String> centerList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private String professionalUid;

    private static final String TAG = "NewContactActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newcontact);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        editTextInitials = findViewById(R.id.initials_text);
        editTextUniqueCode = findViewById(R.id.uniqueCode_text);
        centerSpinner = findViewById(R.id.center_spinner);
        createCard = findViewById(R.id.create_card);
        saveCard = findViewById(R.id.save_card);
        backCard = findViewById(R.id.back_card);

        // Transformar iniciales a mayúsculas automáticamente
        editTextInitials.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String upper = s.toString().toUpperCase();
                if (!upper.equals(s.toString())) {
                    editTextInitials.setText(upper);
                    editTextInitials.setSelection(upper.length());
                }
            }
        });

        // Obtener UID del profesional
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            professionalUid = user.getUid();
            loadCenters();
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }

        // Spinner adapter
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, centerList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        centerSpinner.setAdapter(spinnerAdapter);

        // -----------------------------
        // Crear paciente rápido
        // -----------------------------
        createCard.setOnClickListener(v -> {
            String initials = editTextInitials.getText().toString().trim();
            String center = centerSpinner.getSelectedItem().toString();

            if (initials.isEmpty()) {
                Toast.makeText(this, "Ingrese iniciales", Toast.LENGTH_SHORT).show();
                return;
            }
            if (center.equals("Seleccione centro...")) {
                Toast.makeText(this, "Seleccione un centro", Toast.LENGTH_SHORT).show();
                return;
            }

            preferences.edit().putString("lastCenter", center).apply();

            createQuickPatient(initials, center);
        });

        // -----------------------------
        // Volver
        // -----------------------------
        backCard.setOnClickListener(v -> finish());

        // -----------------------------
        // Vincular paciente existente
        // -----------------------------
        saveCard.setOnClickListener(v -> {
            String uniqueCode = editTextUniqueCode.getText().toString().trim();
            if (uniqueCode.isEmpty()) {
                Toast.makeText(this, "Ingrese código único", Toast.LENGTH_SHORT).show();
                return;
            }
            validateUniqueCodeAndAddContact(uniqueCode);
        });
    }

    private void loadCenters() {
        mDatabase.child("healthProfessionals").child(professionalUid).child("centers")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        centerList.clear();
                        centerList.add("Seleccione centro...");
                        for (DataSnapshot cSnap : snapshot.getChildren()) {
                            String centerName = cSnap.child("name").getValue(String.class);
                            if (centerName != null) {
                                centerList.add(centerName);
                            }
                        }
                        spinnerAdapter.notifyDataSetChanged();

                        String lastCenter = preferences.getString("lastCenter", null);
                        if (lastCenter != null && centerList.contains(lastCenter)) {
                            centerSpinner.setSelection(centerList.indexOf(lastCenter));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(NewContactActivity.this, "Error cargando centros", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createQuickPatient(String initials, String center) {
        String sanitizedCenter = sanitizeCenterName(center);
        String email = initials + "." + sanitizedCenter + "@vmoov.com";
        String password = initials + ",1234";

        // UID del profesional actual
        String professionalUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (professionalUid == null) {
            Toast.makeText(this, "Error: sesión no válida.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔍 Verificar si ya existe ese mail
        mDatabase.child("users")
                .orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(NewContactActivity.this,
                                    "Ya existe un paciente con esas iniciales en este centro.",
                                    Toast.LENGTH_LONG).show();
                            return; // 🚫 Cancelar creación
                        }

                        // ✅ Si no existe, crear paciente en Firebase Auth
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        String userId = mAuth.getCurrentUser().getUid();

                                        generateUniqueCode(uniqueCode -> {
                                            // 🔹 USERS (info general)
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("email", email);
                                            userData.put("password", password);
                                            userData.put("userType", 0);
                                            userData.put("firstName", initials);
                                            userData.put("lastName", sanitizedCenter);
                                            mDatabase.child("users").child(userId).setValue(userData);

                                            // 🔹 PATIENTS (info clínica)
                                            Map<String, Object> patientData = new HashMap<>();
                                            patientData.put("uniqueCode", uniqueCode);

                                            mDatabase.child("patients").child(userId).setValue(patientData)
                                                    .addOnCompleteListener(pdTask -> {
                                                        if (pdTask.isSuccessful()) {
                                                            // ✅ Asociar paciente al profesional
                                                            mDatabase.child("healthProfessionals")
                                                                    .child(professionalUid)
                                                                    .child("patients")
                                                                    .child(userId)
                                                                    .setValue(true)
                                                                    .addOnCompleteListener(linkTask -> {
                                                                        if (linkTask.isSuccessful()) {
                                                                            Log.d(TAG, "Paciente asociado correctamente al profesional.");
                                                                        } else {
                                                                            Log.e(TAG, "Error asociando paciente al profesional", linkTask.getException());
                                                                        }
                                                                    });

                                                            Toast.makeText(NewContactActivity.this, "Paciente creado: " + email, Toast.LENGTH_LONG).show();
                                                            editTextInitials.setText("");

                                                            // 🔹 Mantener sesión del profesional
                                                            relogProfessional();

                                                            // 🔹 Redirigir a pantalla de confirmación
                                                            Intent intent = new Intent(NewContactActivity.this, PatientCreatedActivity.class);
                                                            intent.putExtra("uniqueCode", uniqueCode);
                                                            startActivity(intent);
                                                            finish();
                                                        } else {
                                                            Toast.makeText(NewContactActivity.this, "Error guardando paciente.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        });
                                    } else {
                                        Toast.makeText(NewContactActivity.this, "Error creando paciente: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(NewContactActivity.this, "Error verificando duplicados.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // 🔹 Normaliza el nombre del centro para que sea válido en el correo
    private String sanitizeCenterName(String centerName) {
        return centerName.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private void relogProfessional() {
        String profEmail = preferences.getString("profEmail", null);
        String profPassword = preferences.getString("profPassword", null);

        if (profEmail != null && profPassword != null) {
            mAuth.signInWithEmailAndPassword(profEmail, profPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Relogueo profesional exitoso: " + profEmail);
                        } else {
                            Log.e(TAG, "Error relogueo profesional", task.getException());
                        }
                    });
        }
    }

    private void validateUniqueCodeAndAddContact(String uniqueCode) {
        try {
            int code = Integer.parseInt(uniqueCode);
            mDatabase.child("patients").orderByChild("uniqueCode").equalTo(code)
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()) {
                                for(DataSnapshot patientSnap : snapshot.getChildren()) {
                                    String patientId = patientSnap.getKey();
                                    sendContactRequest(patientId);
                                }
                            } else {
                                Toast.makeText(NewContactActivity.this, "No se encontró paciente con ese código.", Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Código único debe ser numérico.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendContactRequest(String patientId) {
        String professionalId = mAuth.getCurrentUser().getUid();
        DatabaseReference requestsRef = mDatabase.child("requests");

        String requestId = requestsRef.push().getKey();
        if(requestId != null) {
            Map<String,Object> requestData = new HashMap<>();
            requestData.put("professionalId", professionalId);
            requestData.put("patientId", patientId);

            requestsRef.child(requestId).setValue(requestData)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            Toast.makeText(NewContactActivity.this, "Solicitud enviada al paciente.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(NewContactActivity.this, "Error enviando solicitud.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void generateUniqueCode(OnCodeGeneratedListener listener) {
        Random random = new Random();
        int code = random.nextInt(9000) + 1000;

        mDatabase.child("patients").orderByChild("uniqueCode").equalTo(code)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()) {
                            generateUniqueCode(listener);
                        } else {
                            listener.onCodeGenerated(code);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    interface OnCodeGeneratedListener {
        void onCodeGenerated(int uniqueCode);
    }
}
