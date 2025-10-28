package com.example.vmoov;

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

public class MetricsAdapter extends RecyclerView.Adapter<MetricsAdapter.MetricsViewHolder> {

    private List<Metric> metricsList;

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
        holder.gameNameTextView.setText(metric.getGameName());

        // Formatear fechas
        String formattedStartTime = formatDate(metric.getStartTime());
        String formattedEndTime = formatDate(metric.getEndTime());

        holder.gameDateTextView.setText(formattedStartTime);
        holder.totalDurationTextView.setText("- Duración Total: " + GameDurationCalculator.calculateGameDuration(metric.getStartTime(), metric.getEndTime()));
        holder.averageTimeTextView.setText("- Tiempo Promedio: " + String.format("%.2f s", metric.getAverageTime()));
        holder.successRateTextView.setText("- Movimientos Exitosos: " + metric.getTrueCount() + "/" + metric.getStepCount());
    }

    @Override
    public int getItemCount() {
        return metricsList.size();
    }

    public static class MetricsViewHolder extends RecyclerView.ViewHolder {
        TextView gameNameTextView, gameDateTextView, averageTimeTextView, successRateTextView, totalDurationTextView;

        public MetricsViewHolder(@NonNull View itemView) {
            super(itemView);
            gameNameTextView = itemView.findViewById(R.id.gameNameTextView);
            gameDateTextView = itemView.findViewById(R.id.gameDateTextView);
            averageTimeTextView = itemView.findViewById(R.id.averageTimeTextView);
            successRateTextView = itemView.findViewById(R.id.successRateTextView);
            totalDurationTextView = itemView.findViewById(R.id.totalDurationTextView);
        }
    }

    // Formatear timestamps
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // 🔹 Actualizar lista de métricas si cambian
    public void updateMetrics(List<Metric> newMetrics) {
        this.metricsList = newMetrics;
        notifyDataSetChanged();
    }
}
