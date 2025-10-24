package com.example.vmoov;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PatientsAdapter extends RecyclerView.Adapter<PatientsAdapter.ViewHolder> {

    private Context context;
    private List<String> patientNames;
    private List<String> patientIDs;

    public PatientsAdapter(Context context, List<String> patientNames, List<String> patientIDs) {
        this.context = context;
        this.patientNames = patientNames;
        this.patientIDs = patientIDs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.patient_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= patientNames.size() || position >= patientIDs.size()) {
            return; // Previene errores por fuera de rango
        }

        String patientName = patientNames.get(position);
        String patientId = patientIDs.get(position);

        holder.patientName.setText(patientName);

        // Configurar el menú desplegable de los tres puntos
        holder.menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.menuButton);
            popupMenu.inflate(R.menu.patient_menu); // Asegúrate de tener este archivo XML en res/menu/

            popupMenu.setOnMenuItemClickListener(item -> {
                Intent intent = null;

                // Cambiar el switch a if-else
                if (item.getItemId() == R.id.view_results) {
                    intent = new Intent(context, PatientDisplayActivity.class);
                } else if (item.getItemId() == R.id.add_prescription) {
                    intent = new Intent(context, PrescriptionActivity.class);
                } else if (item.getItemId() == R.id.view_patient_info) {
                    intent = new Intent(context, PatientInfoActivity.class);
                } else if (item.getItemId() == R.id.view_unique_code) {
                    intent = new Intent(context, ShowUniqueCodeActivity.class);
                }

                if (intent != null) {
                    intent.putExtra("userId", patientId);
                    context.startActivity(intent);
                }
                return true;
            });

            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(patientNames.size(), patientIDs.size()); // Evita errores de tamaño desincronizado
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView patientName;
        ImageButton menuButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            patientName = itemView.findViewById(R.id.patientNameTextView); // Verifica este ID en item_patient.xml
            menuButton = itemView.findViewById(R.id.menuButton);
        }
    }
}
