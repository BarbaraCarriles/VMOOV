package com.example.vmoov;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MetricsAdapter extends RecyclerView.Adapter<MetricsAdapter.MetricsViewHolder> {

    private final List<Metric> metricsList;

    public MetricsAdapter(List<Metric> metricsList) {
        this.metricsList = metricsList;
    }

    @NonNull
    @Override
    public MetricsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.metric_item, parent, false);
        return new MetricsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetricsViewHolder holder, int position) {
        Metric metric = metricsList.get(position);

        // Mostrar nombre del juego
        holder.gameNameTextView.setText(metric.getGameName().equalsIgnoreCase("simon") ?
                "Juego: Simon" : "Juego: Game 1");

        // Fecha
        holder.gameDateTextView.setText(formatDate(metric.getStartTime()));

        // Limpiar campos
        holder.metric1TextView.setText("");
        holder.metric2TextView.setText("");
        holder.metric3TextView.setText("");
        holder.metric4TextView.setText("");
        holder.metric5TextView.setText("");

        // --- GAME 1 ---
        if ("game1".equalsIgnoreCase(metric.getGameName())) {
            MetricsCalculator.Game1Stats stats = MetricsCalculator.calculateGame1Stats(metric);
            if (stats != null) {
                holder.metric1TextView.setText("✅ Movimientos exitosos: " + stats.successfulMoves + " / " + stats.totalMoves);
                holder.metric2TextView.setText("⏱️ Tiempo promedio por movimiento:" + String.format(Locale.getDefault(), "%.2f s", stats.avgTime));
                holder.metric3TextView.setText("🎚️ Dificultad: " + getDifficultyLabel(metric.getDifficulty()));
                holder.metric4TextView.setText("⭐ Puntaje general: " + String.format(Locale.getDefault(), "%.1f", stats.generalScore));
            }

            // --- SIMON ---
        } else if ("simon".equalsIgnoreCase(metric.getGameName())) {
            MetricsCalculator.SimonStats stats = MetricsCalculator.calculateSimonStats(metric);
            if (stats != null) {
                holder.metric1TextView.setText("🔄 Rondas correctas: " + stats.completedRounds + " / " + stats.totalRounds);

                // Precisión por botón (con color + salto de línea)
                StringBuilder precisionBuilder = new StringBuilder();
                for (Map.Entry<Integer, MetricsCalculator.ButtonStats> entry : stats.buttonStats.entrySet()) {
                    int btn = entry.getKey();
                    MetricsCalculator.ButtonStats bs = entry.getValue();
                    String colorName = getButtonColorName(btn);
                    String colorHex = getButtonColorHex(btn);

                    precisionBuilder.append("<font color='")
                            .append(colorHex)
                            .append("'><b>")
                            .append(colorName)
                            .append("</b></font>: ")
                            .append(String.format(Locale.getDefault(), "%.0f%%", bs.getAccuracy() * 100))
                            .append("<br>");
                }

                holder.metric2TextView.setText(
                        precisionBuilder.length() > 0 ?
                                Html.fromHtml("🎯 Precisión por botón:<br>" + precisionBuilder, Html.FROM_HTML_MODE_LEGACY)
                                : "🎯 Precisión por botón: Sin datos"
                );

                // Tiempo promedio por botón (con color + salto de línea)
                StringBuilder timeBuilder = new StringBuilder();
                for (Map.Entry<Integer, MetricsCalculator.ButtonStats> entry : stats.buttonStats.entrySet()) {
                    int btn = entry.getKey();
                    MetricsCalculator.ButtonStats bs = entry.getValue();
                    String colorName = getButtonColorName(btn);
                    String colorHex = getButtonColorHex(btn);

                    timeBuilder.append("<font color='")
                            .append(colorHex)
                            .append("'><b>")
                            .append(colorName)
                            .append("</b></font>: ")
                            .append(String.format(Locale.getDefault(), "%.2f s", bs.avgTime))
                            .append("<br>");
                }

                holder.metric3TextView.setText(
                        timeBuilder.length() > 0 ?
                                Html.fromHtml("⏱️ Tiempo promedio por botón:<br>" + timeBuilder, Html.FROM_HTML_MODE_LEGACY)
                                : "⏱️ Tiempo promedio por botón: Sin datos"
                );

                holder.metric4TextView.setText("🎚️ Dificultad: " + getDifficultyLabel(metric.getDifficulty()));
                holder.metric5TextView.setText("⭐ Puntaje general: " + String.format(Locale.getDefault(), "%.1f", stats.generalScore));
            }
        }
    }

    @Override
    public int getItemCount() {
        return metricsList.size();
    }

    // ---- ViewHolder ----
    public static class MetricsViewHolder extends RecyclerView.ViewHolder {
        TextView gameNameTextView, gameDateTextView;
        TextView metric1TextView, metric2TextView, metric3TextView, metric4TextView, metric5TextView;

        public MetricsViewHolder(@NonNull View itemView) {
            super(itemView);
            gameNameTextView = itemView.findViewById(R.id.gameNameTextView);
            gameDateTextView = itemView.findViewById(R.id.gameDateTextView);
            metric1TextView = itemView.findViewById(R.id.metric1TextView);
            metric2TextView = itemView.findViewById(R.id.metric2TextView);
            metric3TextView = itemView.findViewById(R.id.metric3TextView);
            metric4TextView = itemView.findViewById(R.id.metric4TextView);
            metric5TextView = itemView.findViewById(R.id.metric5TextView);
        }
    }

    // ---- Helpers ----
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String getDifficultyLabel(int diff) {
        switch (diff) {
            case 0: return "Fácil";
            case 1: return "Medio";
            case 2: return "Difícil";
            default: return "Desconocido";
        }
    }

    private String getButtonColorName(int buttonId) {
        switch (buttonId) {
            case 0: return "Rojo";
            case 1: return "Azul";
            case 2: return "Verde";
            case 3: return "Amarillo";
            default: return "Desconocido";
        }
    }

    private String getButtonColorHex(int buttonId) {
        switch (buttonId) {
            case 0: return "#E53935"; // rojo
            case 1: return "#1E88E5"; // azul
            case 2: return "#43A047"; // verde
            case 3: return "#FDD835"; // amarillo
            default: return "#555555"; // gris
        }
    }
}
