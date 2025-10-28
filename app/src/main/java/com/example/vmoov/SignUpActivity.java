package com.example.vmoov;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class SignUpActivity extends BaseActivity {

    private EditText editText_fname, editText_lname, editText_dni, editText_phone;
    private EditText editText_email, editText_pass, editText_repass;
    private Spinner spinnerGender;
    private CardView signUpButton, backButton;
    private CheckBox checkBox;
    private ImageView togglePass, toggleRepass;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private int userType = 0;

    private static final String TAG = "SignUpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Inicializar FirebaseAuth y DatabaseReference
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Vincular elementos del layout
        editText_fname = findViewById(R.id.firstname_text);
        editText_lname = findViewById(R.id.lastname_text);
        editText_dni = findViewById(R.id.dni_text);
        spinnerGender = findViewById(R.id.gender_spinner);
        editText_phone = findViewById(R.id.phone_text);
        editText_email = findViewById(R.id.email_text);
        editText_pass = findViewById(R.id.pass_text);
        editText_repass = findViewById(R.id.repass_text);
        signUpButton = findViewById(R.id.signUp_button);
        backButton = findViewById(R.id.back_button);
        checkBox = findViewById(R.id.checkboxUserType);
        togglePass = findViewById(R.id.toggle_pass);
        toggleRepass = findViewById(R.id.toggle_repass);

        // Configurar Spinner de género con un hint
        setupGenderSpinner();

        // Listener para el checkbox (determina si es profesional de salud)
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> userType = isChecked ? 1 : 0);

        // Alternar visibilidad de contraseña
        togglePass.setOnClickListener(v -> togglePasswordVisibility(editText_pass, togglePass));
        toggleRepass.setOnClickListener(v -> togglePasswordVisibility(editText_repass, toggleRepass));

        // Botón de registro
        signUpButton.setOnClickListener(v -> validateAndRegister());

        // Botón de regresar
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        });
    }

    private void setupGenderSpinner() {
        String[] genderArray = getResources().getStringArray(R.array.gender_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, genderArray) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0; // La opción "Seleccione un género..." no es seleccionable
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;

                int hintColor = ContextCompat.getColor(getContext(), R.color.lightblue);
                int normalColor = ContextCompat.getColor(getContext(), R.color.blue);

                textView.setTextColor(position == 0 ? hintColor : normalColor);
                return view;
            }
        };
        spinnerGender.setAdapter(adapter);
        spinnerGender.setSelection(0); // Establecer el hint como opción inicial
    }

    private void togglePasswordVisibility(EditText passwordField, ImageView toggleButton) {
        if (passwordField.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility);
        } else {
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_off);
        }
        passwordField.setSelection(passwordField.getText().length());
    }

    private void validateAndRegister() {
        String firstName = editText_fname.getText().toString().trim();
        String lastName = editText_lname.getText().toString().trim();
        String dni = editText_dni.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();
        String phone = editText_phone.getText().toString().trim();
        String email = editText_email.getText().toString().trim();
        String password = editText_pass.getText().toString().trim();
        String confirmPassword = editText_repass.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || dni.isEmpty() || phone.isEmpty() ||
                email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showToast("Por favor, complete todos los campos.");
            return;
        }

        if (spinnerGender.getSelectedItemPosition() == 0) {
            showToast("Seleccione un género válido.");
            return;
        }

        if (!dni.matches("\\d{8}")) {
            showToast("El DNI debe contener exactamente 8 dígitos numéricos.");
            return;
        }

        if (!phone.matches("\\d{10,15}")) {
            showToast("Ingrese un número de teléfono válido.");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Ingrese un correo electrónico válido.");
            return;
        }

        if (!isValidPassword(password)) {
            showToast("La contraseña debe tener al menos 6 caracteres, 1 mayúscula y 1 carácter especial.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showToast("Las contraseñas no coinciden.");
            return;
        }

        if (userType == 1) {
            // 🔹 Si es profesional, crea la cuenta y guarda los datos en Firebase
            registerProfessional(firstName, lastName, dni, gender, phone, email, password);
        } else {
            // 🔹 Si es paciente, solo pasa los datos a `PatientSignUpActivity`
            Intent intent = new Intent(SignUpActivity.this, PatientSignUpActivity.class);
            intent.putExtra("firstName", firstName);
            intent.putExtra("lastName", lastName);
            intent.putExtra("dni", dni);
            intent.putExtra("gender", gender);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            intent.putExtra("password", password);
            startActivity(intent);
            finish();
        }
    }

    // 🔹 Método para crear la cuenta si es profesional
    private void registerProfessional(String firstName, String lastName, String dni, String gender, String phone, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(SignUpActivity.this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        User user = new User(firstName, lastName, dni, gender, phone, email, password, 1);

                        mDatabase.child("users").child(userId).setValue(user)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {

                                        // 🔹 Solo UID del profesional
                                        mDatabase.child("healthProfessionals").child(userId).setValue(true);

                                        showToast("Registro exitoso.");
                                        Intent intent = new Intent(SignUpActivity.this, HealthProfessionalInfoActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        showToast("Error al guardar datos del usuario.");
                                    }
                                });
                    } else {
                        showToast("Error en el registro: " + task.getException().getMessage());
                    }
                });
    }


    private boolean isValidPassword(String password) {
        Pattern pattern = Pattern.compile("^(?=.*[A-Z])(?=.*[@#$%^&+=!._,]).{6,}$");
        return pattern.matcher(password).matches();
    }

    private void registerUser(String firstName, String lastName, String dni, String gender, String phone, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(SignUpActivity.this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        User user = new User(firstName, lastName, dni, gender, phone, email, password, userType);

                        mDatabase.child("users").child(userId).setValue(user)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        showToast("Registro exitoso.");
                                        redirectUser(userId);
                                    } else {
                                        showToast("Error al guardar datos del usuario.");
                                    }
                                });
                    } else {
                        showToast("Error en el registro: " + task.getException().getMessage());
                    }
                });
    }

    private void redirectUser(String userId) {
        Intent intent;
        if (userType == 1) {
            intent = new Intent(SignUpActivity.this, HealthProfessionalInfoActivity.class);
        } else {
            intent = new Intent(SignUpActivity.this, PatientSignUpActivity.class);
        }

        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
