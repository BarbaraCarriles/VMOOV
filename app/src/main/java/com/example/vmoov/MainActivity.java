package com.example.vmoov;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends BaseActivity {

    private EditText editText_user;
    private EditText editText_pass;
    private ImageView passwordToggle;
    private CardView guardarButton;
    private FirebaseAuth mAuth;

    private boolean passwordVisible = false;

    // Nombre de SharedPreferences
    private static final String SHARED_PREFS = "user_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Vinculación de elementos del layout
        editText_user = findViewById(R.id.user_text);
        editText_pass = findViewById(R.id.pass_text);
        passwordToggle = findViewById(R.id.password_toggle);
        guardarButton = findViewById(R.id.start_card);

        // Configurar visibilidad de contraseña
        setupPasswordVisibility();

        // Configuración del botón para iniciar sesión
        guardarButton.setOnClickListener(v -> {
            String email = editText_user.getText().toString();
            String password = editText_pass.getText().toString();

            // Iniciar sesión con email y contraseña en Firebase
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(MainActivity.this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();

                                // Verificar si el usuario es paciente o profesional de salud
                                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
                                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            // Obtener el tipo de usuario
                                            int userType = dataSnapshot.child("userType").getValue(Integer.class);

                                            // Guardar userId y userType en SharedPreferences
                                            SharedPreferences preferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                                            SharedPreferences.Editor editor = preferences.edit();
                                            editor.putString("userId", userId);
                                            editor.putInt("userType", userType);
                                            editor.apply();

                                            if (userType == 0) {
                                                // Redirigir a la actividad del paciente
                                                Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                                                startActivity(intent);
                                            } else {
                                                //guardar usuario y contraseña para reingresar luego de crear paciente rapido
                                                SharedPreferences.Editor editor2 = preferences.edit();
                                                editor2.putString("profEmail", email);
                                                editor2.putString("profPassword", password);
                                                editor2.apply();
                                                // Redirigir a la actividad del profesional de salud
                                                Intent intent = new Intent(MainActivity.this, NotPatientActivity.class);
                                                startActivity(intent);
                                            }
                                        } else {
                                            // Revisar en healthProfessionals si no está en users
                                            DatabaseReference healthRef = FirebaseDatabase.getInstance().getReference().child("healthProfessionals").child(userId);
                                            healthRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    if (snapshot.exists()) {
                                                        // Si está en healthProfessionals, redirigir a NotPatientActivity
                                                        SharedPreferences preferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                                                        SharedPreferences.Editor editor = preferences.edit();
                                                        editor.putString("userId", userId);
                                                        editor.putInt("userType", 1);
                                                        editor.apply();

                                                        Intent intent = new Intent(MainActivity.this, NotPatientActivity.class);
                                                        startActivity(intent);
                                                    } else {
                                                        Toast.makeText(MainActivity.this, "Tipo de usuario no encontrado", Toast.LENGTH_SHORT).show();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Toast.makeText(MainActivity.this, "Error al leer datos de usuario", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(MainActivity.this, "Error al leer datos de usuario", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Los datos ingresados son incorrectos", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Configuración del botón para redirigir al registro
        Button signUpButton = findViewById(R.id.smallSignUp_button);
        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Configuración del botón para redirigir a ForgotPasswordActivity
        Button forgotPasswordButton = findViewById(R.id.forgot_password_button);
        forgotPasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void setupPasswordVisibility() {
        passwordToggle.setOnClickListener(v -> {
            if (passwordVisible) {
                // Ocultar contraseña
                editText_pass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordToggle.setImageResource(R.drawable.ic_visibility_off);
            } else {
                // Mostrar contraseña
                editText_pass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordToggle.setImageResource(R.drawable.ic_visibility);
            }
            passwordVisible = !passwordVisible;
        });
    }
}
