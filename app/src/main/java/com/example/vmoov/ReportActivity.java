package com.example.vmoov;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends BaseActivity {

    private static final String TAG = "ReportActivity";

    private FirebaseAuth mAuth;
    private String userId;

    private CardView sendReportButton;
    private CardView backButton;

    // Datos de sesión
    private String professionalEmail;
    private String patientName = "Desconocido";
    private String patientLastName = "Desconocido";
    private Metric lastMetric;
    private int totalSessions = 0;
    private String fechaUltimaSesion = "Fecha desconocida";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        userId = currentUser != null ? currentUser.getUid() : null;

        sendReportButton = findViewById(R.id.btn_send_report);
        backButton = findViewById(R.id.back_button);

        sendReportButton.setOnClickListener(v -> generarYEnviarReporte());
        backButton.setOnClickListener(v -> startActivity(new Intent(this, MenuActivity.class)));

        if (userId != null) {
            fetchPatientData();
            fetchProfessionalEmail();
            fetchMetrics();
        } else {
            Log.e(TAG, "❌ No hay usuario autenticado");
        }
    }

    // ------------------ DATOS DEL PACIENTE ------------------

    private void fetchPatientData() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    patientName = snapshot.child("firstName").getValue(String.class);
                    patientLastName = snapshot.child("lastName").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error obteniendo datos del paciente: " + error.getMessage());
            }
        });
    }

    // ------------------ EMAIL PROFESIONAL ------------------

    private void fetchProfessionalEmail() {
        DatabaseReference professionalsRef = FirebaseDatabase.getInstance().getReference("healthProfessionals");
        professionalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot professionalSnap : snapshot.getChildren()) {
                    if (professionalSnap.child("patients").hasChild(userId)) {
                        String professionalId = professionalSnap.getKey();
                        if (professionalId != null) fetchEmailFromUser(professionalId);
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error buscando email del profesional: " + error.getMessage());
            }
        });
    }

    private void fetchEmailFromUser(String professionalId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(professionalId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) professionalEmail = snapshot.child("email").getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error obteniendo email profesional: " + error.getMessage());
            }
        });
    }

    // ------------------ MÉTRICAS ------------------

    private void fetchMetrics() {
        FirebaseDataHelper.fetchAllMetrics(userId, new FirebaseDataHelper.FirebaseCallback<List<Metric>>() {
            @Override
            public void onSuccess(List<Metric> allMetrics) {
                if (allMetrics.isEmpty()) return;

                lastMetric = allMetrics.stream()
                        .max((m1, m2) -> Long.compare(m1.getStartTime(), m2.getStartTime()))
                        .orElse(null);

                totalSessions = allMetrics.size();

                if (lastMetric != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    fechaUltimaSesion = sdf.format(new Date(lastMetric.getStartTime()));
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Error obteniendo métricas: " + e.getMessage());
            }
        });
    }

    // ------------------ GENERAR Y ENVIAR REPORTE ------------------

    private void generarYEnviarReporte() {
        if (lastMetric == null) {
            mostrarMensaje("No hay métricas disponibles para generar el reporte.");
            return;
        }

        String[] reportLines = {
                "Reporte de Sesión",
                "Fecha de la sesión: " + fechaUltimaSesion,
                "Paciente: " + patientName + " " + patientLastName,
                "Sesión número: " + totalSessions,
                "Juego: " + lastMetric.getGameName(),
                "Movimientos exitosos: " + lastMetric.getTrueCount(),
                "Tiempo promedio de ejecución de movimiento: " + String.format(Locale.US, "%.2f segundos", lastMetric.getAverageTime())
        };

        File pdfFile = PdfGenerator.createPdf(this, "VMOOV_Reporte_" + patientName + patientLastName + ".pdf", reportLines);

        if (pdfFile != null) enviarCorreoConAdjunto(professionalEmail, pdfFile.getAbsolutePath());
    }

    private void enviarCorreoConAdjunto(String email, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "❌ El archivo no existe: " + filePath);
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("application/pdf");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Reporte de Sesión - VMOOV");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte de sesión en PDF.");
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(emailIntent, "Enviar reporte"));
            Log.d(TAG, "📤 Enviando correo con archivo adjunto: " + filePath);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error al enviar el correo: " + e.getMessage());
        }
    }

    private void mostrarMensaje(String mensaje) {
        runOnUiThread(() -> Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show());
    }
}
