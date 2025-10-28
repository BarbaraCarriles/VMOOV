package com.example.vmoov;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.Arrays;
import java.util.List;

public class BaseActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "AppPreferences";
    private static final String FONT_SCALE_KEY = "fontScale";
    private static final String LAST_REPORTED_TIMESTAMP_KEY = "lastReportedTimestamp";
    private static final String TAG = "BaseActivity";
    private static final String SESSION_KEY = "UserSessionActive";
    private boolean isListenerActive = false;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    private boolean firstLoad = true;
    private DataSnapshot lastSnapshot = null;
    private static final String CHANNEL_ID = "vmoov_alerts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyFontScale();
        super.onCreate(savedInstanceState);
        createNotificationChannel(); // 🔹 Restaurado
        checkAndRequestNotificationPermission(); // 🔹 Restaurado
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "✅ Usuario autenticado en onStart(): " + userId);

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean wasLoggedIn = prefs.getBoolean(SESSION_KEY, false);

            if (!wasLoggedIn) {
                Log.d(TAG, "🚀 Primera vez iniciando sesión en esta sesión.");
                prefs.edit().putBoolean(SESSION_KEY, true).apply();
            }

            if (!isListenerActive) {
                setupFirebaseListener(userId);
            }

            checkForPendingReports(userId); // 🔹 Verifica si hay partidas nuevas al iniciar sesión
            setupRequestListener(userId); // 🔹 Agregamos el listener para solicitudes

        } else {
            Log.d(TAG, "❌ No hay usuario autenticado. No se activa la escucha.");
            clearSession();
        }
    }

    private void showNotificationForContactRequest(String professionalName) {
        Intent intent = new Intent(this, NotPatientActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.rocket)
                .setContentTitle("Nueva Solicitud de Contacto")
                .setContentText(professionalName + " quiere vincularse contigo.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(2, builder.build());
        }
    }



    private void applyFontScale() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float fontScale = prefs.getFloat(FONT_SCALE_KEY, 1.0f);
        getResources().getConfiguration().fontScale = fontScale;
        getResources().updateConfiguration(getResources().getConfiguration(), getResources().getDisplayMetrics());
    }

    private void clearSession() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(SESSION_KEY).apply();
        isListenerActive = false;
        Log.d(TAG, "🛑 Sesión limpiada debido a usuario no autenticado.");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "VMOOV Alerts";
            String description = "Notificaciones sobre nuevos reportes de sesiones";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }
    private void setupFirebaseListener(String userId) {
        if (isListenerActive) {
            Log.d(TAG, "🔵 Listener ya activo, no se vuelve a registrar.");
            return;
        }

        databaseReference = FirebaseDatabase.getInstance()
                .getReference("patientmetrics").child(userId).child("gameplaydata");

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d(TAG, "⚠️ No hay datos en Firebase. No se muestra alerta.");
                    return;
                }

                if (firstLoad) {
                    Log.d(TAG, "🆕 Carga inicial de partidas en Firebase. No se muestra alerta.");
                    lastSnapshot = snapshot;
                    firstLoad = false;
                    return;
                }

                if (lastSnapshot != null) {
                    boolean nuevaPartida = detectarNuevaSesion(lastSnapshot, snapshot);
                    if (!nuevaPartida) {
                        Log.d(TAG, "🔵 No hay nuevas partidas en game1. No se muestra alerta.");
                        return;
                    }
                } else {
                    Log.d(TAG, "🆕 No hay snapshot previo. Guardando estado inicial...");
                }

                lastSnapshot = snapshot;

                long latestTimestamp = obtenerUltimaSesion(snapshot);
                guardarUltimaSesion(latestTimestamp); // 🔹 Guardar timestamp en SharedPreferences

                if (shouldShowAlert()) {
                    Log.d(TAG, "🔔 Nueva partida detectada en Firebase. Mostrando alerta.");
                    showAlert();
                } else {
                    Log.d(TAG, "🔕 Nueva partida detectada, pero estamos en una actividad no permitida. Mostrando notificación.");
                    showNotification(); // 🔹 Llamado correcto
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al leer datos de Firebase: " + error.getMessage());
            }
        };

        databaseReference.addValueEventListener(valueEventListener);
        isListenerActive = true;
    }

    private void showNotification() {
        Intent intent = new Intent(this, ReportActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.rocket)
                .setContentTitle("Nuevo Reporte Disponible")
                .setContentText("Se ha registrado una nueva sesión. Toca para ver detalles.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }
    }

    private boolean shouldShowAlert() {
        String currentActivity = this.getClass().getSimpleName();
        List<String> excludedActivities = Arrays.asList("MainActivity", "SignUpActivity", "RegisterActivity", "ReportActivity");
        return !excludedActivities.contains(currentActivity);
    }

    private void enviarReporte() {
        Log.d(TAG, "📤 Generando y enviando reporte...");

        Intent intent = new Intent(this, ReportActivity.class);
        startActivity(intent);
    }

    private boolean detectarNuevaSesion(DataSnapshot oldSnapshot, DataSnapshot newSnapshot) {
        for (DataSnapshot gameNode : newSnapshot.getChildren()) { // cada juego
            String gameName = gameNode.getKey();
            DataSnapshot oldGameNode = oldSnapshot.child(gameName);

            for (DataSnapshot sessionNode : gameNode.child("sessions").getChildren()) {
                String sessionId = sessionNode.getKey();
                if (!oldGameNode.child("sessions").hasChild(sessionId)) {
                    Log.d(TAG, "🆕 Nueva sesión detectada en " + gameName + ": " + sessionId);
                    return true;
                }
            }
        }
        return false;
    }


    private long obtenerUltimaSesion(DataSnapshot snapshot) {
        long latestTimestamp = 0;
        for (DataSnapshot gameNode : snapshot.getChildren()) { // cada juego
            for (DataSnapshot sessionNode : gameNode.child("sessions").getChildren()) {
                Long startTime = sessionNode.child("results").child("startTime").getValue(Long.class);
                if (startTime != null && startTime > latestTimestamp) {
                    latestTimestamp = startTime;
                }
            }
        }
        return latestTimestamp;
    }


    private void guardarUltimaSesion(long timestamp) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putLong(LAST_REPORTED_TIMESTAMP_KEY, timestamp).apply();
    }

    private void checkForPendingReports(String userId) {
        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                long latestTimestamp = obtenerUltimaSesion(snapshot);
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                long lastReportedTimestamp = prefs.getLong(LAST_REPORTED_TIMESTAMP_KEY, 0);

                if (latestTimestamp > lastReportedTimestamp) {
                    Log.d(TAG, "🔔 Sesión no reportada detectada. Mostrando alerta al iniciar sesión.");
                    showAlert();
                    guardarUltimaSesion(latestTimestamp);
                }
            }
        });
    }

    private void setupRequestListener(String userId) {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("requests");

        // 🔹 Filtrar solo las solicitudes donde patientId == currentUser
        Query patientRequestsQuery = requestsRef.orderByChild("patientId").equalTo(userId);

        patientRequestsQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String professionalId = snapshot.child("professionalId").getValue(String.class);
                String requestId = snapshot.getKey();

                if (professionalId == null || requestId == null) return;

                // Obtener nombre del profesional
                DatabaseReference profRef = FirebaseDatabase.getInstance().getReference("users").child(professionalId);
                profRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot profSnap) {
                        String firstName = profSnap.child("firstName").getValue(String.class);
                        String lastName = profSnap.child("lastName").getValue(String.class);
                        String professionalName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");

                        // Mostrar alerta y notificación solo al paciente
                        showContactRequestAlert(requestId, professionalId, userId, professionalName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Error al obtener datos del profesional: " + error.getMessage());
                    }
                });
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error en listener de solicitudes: " + error.getMessage());
            }
        });
    }

    private void showContactRequestAlert(String requestId, String professionalId, String patientId, String professionalName) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
            builder.setTitle("Solicitud de Contacto")
                    .setMessage(professionalName + " desea vincularse contigo. ¿Aceptas?")
                    .setPositiveButton("Aceptar", (dialog, which) -> {
                        acceptContactRequest(requestId, professionalId, patientId);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Rechazar", (dialog, which) -> {
                        rejectContactRequest(requestId);
                        dialog.dismiss();
                    })
                    .setCancelable(false);

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });

        // 🔹 Notificación push solo para el paciente
        showNotificationForContactRequest(professionalName);
    }


    private void acceptContactRequest(String requestId, String professionalId, String patientId) {
        DatabaseReference professionalsRef = FirebaseDatabase.getInstance()
                .getReference("healthProfessionals").child(professionalId).child("patients");

        professionalsRef.child(patientId).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseDatabase.getInstance().getReference("requests").child(requestId).removeValue();
                        Toast.makeText(BaseActivity.this, "Vinculación exitosa.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BaseActivity.this, "Error al vincular al paciente.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rejectContactRequest(String requestId) {
        FirebaseDatabase.getInstance().getReference("requests").child(requestId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(BaseActivity.this, "Solicitud rechazada.", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void showAlert() {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                        .setTitle("Nuevo Reporte Disponible")
                        .setMessage("Se han recibido nuevos datos del paciente. ¿Quieres enviar un reporte?")
                        .setPositiveButton("Enviar", (dialog, which) -> enviarReporte())
                        .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        }

    }
}
