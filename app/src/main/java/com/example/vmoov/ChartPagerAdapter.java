package com.example.vmoov;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.vmoov.GameMetric.ButtonAccuracy;



import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChartPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private GameMetric gameMetric;
    private static final int VIEW_TYPE_RATIO = 0;
    private static final int VIEW_TYPE_TIME_TEXT = 1;
    private static final int VIEW_TYPE_SCORE = 2;
    private static final int VIEW_TYPE_NUMBER = 3;
    private static final int VIEW_TYPE_TEXT = 4;
    private static final int VIEW_TYPE_PIE_CHART = 5; // Para precisión por botón

    private static final int VIEW_TYPE_BAR_CHART= 6; // avgTime por boton

    public ChartPagerAdapter(Context context) {
        this.context = context;
    }

    public void setGameMetric(GameMetric metric) {
        this.gameMetric = metric;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return gameMetric != null ? gameMetric.getMetricsList().size() : 0;
    }

    public GameMetric getGameMetric() {
        return gameMetric;
    }


    @Override
    public int getItemViewType(int position) {
        if (gameMetric == null) return VIEW_TYPE_RATIO;
        GameMetric.MetricItem item = gameMetric.getMetricsList().get(position);
        switch (item.getType()) {
            case SCORE: return VIEW_TYPE_SCORE;
            case NUMBER: return VIEW_TYPE_NUMBER;
            case TIME_TEXT: return VIEW_TYPE_TIME_TEXT;
            case TEXT: return VIEW_TYPE_TEXT;
            case PIE_CHART: return VIEW_TYPE_PIE_CHART;
            case BAR_CHART : return VIEW_TYPE_BAR_CHART;
            default: return VIEW_TYPE_RATIO;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case VIEW_TYPE_TIME_TEXT:
                return new TimeTextViewHolder(inflater.inflate(R.layout.fragment_execution_time, parent, false));
            case VIEW_TYPE_SCORE:
                return new ScoreViewHolder(inflater.inflate(R.layout.fragment_score, parent, false));
            case VIEW_TYPE_NUMBER:
                return new NumberViewHolder(inflater.inflate(R.layout.fragment_number, parent, false));
            case VIEW_TYPE_TEXT:
                return new TextViewHolder(inflater.inflate(R.layout.fragment_text, parent, false));
            case VIEW_TYPE_PIE_CHART:
                return new PieChartViewHolder(inflater.inflate(R.layout.fragment_chart_grid, parent, false));
            case VIEW_TYPE_BAR_CHART:
                return new BarChartViewHolder(inflater.inflate(R.layout.fragment_chart_grid, parent, false));
            default:
                return new RatioViewHolder(inflater.inflate(R.layout.fragment_ratio, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (gameMetric == null) return;
        GameMetric.MetricItem item = gameMetric.getMetricsList().get(position);

        switch (getItemViewType(position)) {
            case VIEW_TYPE_SCORE:
                if (holder instanceof ScoreViewHolder) ((ScoreViewHolder) holder).bind(item.getValue());
                break;
            case VIEW_TYPE_TIME_TEXT:
                if (holder instanceof TimeTextViewHolder) ((TimeTextViewHolder) holder).bind(item.getValue(), item.getLabel());
                break;
            case VIEW_TYPE_NUMBER:
                if (holder instanceof NumberViewHolder) ((NumberViewHolder) holder).bind(item.getValue(), item.getLabel());
                break;
            case VIEW_TYPE_TEXT:
                if (holder instanceof TextViewHolder) ((TextViewHolder) holder).bind(item.getValue(), item.getLabel());
                break;
            case VIEW_TYPE_RATIO:
                if (holder instanceof RatioViewHolder) ((RatioViewHolder) holder).bind(item.getValue(), item.getTotalAbsolute(),item.getLabel());
                break;
            case VIEW_TYPE_PIE_CHART:
                if (holder instanceof PieChartViewHolder) {
                    ((PieChartViewHolder) holder).bind(item.getButtonAccuracies(), item.getLabel());
                }
                break;
            case VIEW_TYPE_BAR_CHART:
                if (holder instanceof BarChartViewHolder) {
                    ((BarChartViewHolder) holder).bind(item.getButtonAvgTimes(), item.getLabel());
                }
                break;
        }
    }

    /*** ViewHolders ***/
    public static class RatioViewHolder extends RecyclerView.ViewHolder {
        TextView completedText, totalText, descriptionText;
        ImageView statusImage;

        public RatioViewHolder(@NonNull View itemView) {
            super(itemView);
            completedText = itemView.findViewById(R.id.completed_text);
            totalText = itemView.findViewById(R.id.total_text);
            descriptionText = itemView.findViewById(R.id.description_text);
            statusImage = itemView.findViewById(R.id.status_image);
        }

        public void bind(int completed, int total, String description) {
            completedText.setText(String.valueOf(completed));
            totalText.setText(String.format(Locale.getDefault(), "/%d", total));
           // descriptionText.setText(description);
            //seteo  segun valor
            float progress = (total > 0) ? ((float) completed / total) * 100 : 0;
            if (progress < 50) {
                int color = Color.parseColor("#F18181");
                completedText.setTextColor(color);
                totalText.setTextColor(color);
                descriptionText.setTextColor(color);
                statusImage.setImageResource(R.drawable.keep);
            } else if (progress < 80) {
                int color = Color.parseColor("#F1C40F");
                completedText.setTextColor(color);
                totalText.setTextColor(color);
                descriptionText.setTextColor(color);
                statusImage.setImageResource(R.drawable.claps);
            } else {
                int color = Color.parseColor("#39e186");
                completedText.setTextColor(color);
                totalText.setTextColor(color);
                descriptionText.setTextColor(color);
                statusImage.setImageResource(R.drawable.thumbup);
            }

        }
    }


    public static class TimeTextViewHolder extends RecyclerView.ViewHolder {
        TextView textView, statusText, percentageSymbol ;
        ImageView statusImage;

        public TimeTextViewHolder(@NonNull View itemView)
        {
            super(itemView);
            textView = itemView.findViewById(R.id.percentage_text);
            statusText = itemView.findViewById(R.id.status_text);
            percentageSymbol = itemView.findViewById(R.id.percentage_symbol);
            statusImage = itemView.findViewById(R.id.bottom_image);
        }
        public void bind(int value, String label)
        {
            textView.setText(String.valueOf(value));
            if (value > 0) {
                // Más rápido
                int color = Color.parseColor("#39e186");
                statusText.setText("Más rápido");
                statusText.setTextColor(color);
                statusImage.setImageResource(R.drawable.thumbup);
                percentageSymbol.setTextColor(color);
                textView.setTextColor(color);


            } else if (value < 0) {
                // Más lento
                int color = Color.parseColor("#F18181");
                statusText.setText("Más lento");
                statusText.setTextColor(color);
                statusImage.setImageResource(R.drawable.keep);
                percentageSymbol.setTextColor(color);
                textView.setTextColor(color);

            } else {
                // Sin cambios
                int color = Color.parseColor("#dfaaff");
                statusText.setText("Sin cambios");
                statusText.setTextColor(color);
                percentageSymbol.setTextColor(color);
                textView.setTextColor(color);

            }

        }
    }

    public static class NumberViewHolder extends RecyclerView.ViewHolder {
        TextView numberText, descriptionText;
        ImageView statusImage;

        public NumberViewHolder(@NonNull View itemView) {
            super(itemView);
            numberText = itemView.findViewById(R.id.number_text);
            descriptionText = itemView.findViewById(R.id.description_text);
            statusImage = itemView.findViewById(R.id.status_image);
        }

        public void bind(int number, String description) {
            numberText.setText(String.valueOf(number));
           // descriptionText.setText(description);
            statusImage.setImageResource(R.drawable.claps);
        }
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView levelText, descriptionText;
        ImageView statusImage;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            levelText = itemView.findViewById(R.id.text);
            descriptionText = itemView.findViewById(R.id.description_text);
            statusImage = itemView.findViewById(R.id.status_image);
        }

        public void bind(int level, String description) {

            //caso niveles de dificultad
            if (description != null && description.toLowerCase().contains("dificultad")) {
                // Elegir la imagen según el nivel
                int drawableRes;
                String levelLabel="";
                switch (level) {
                    case 0:
                        levelLabel="Nivel fácil";
                        drawableRes = R.drawable.easylevel;
                        break;
                    case 1:
                        levelLabel="Nivel medio";
                        drawableRes = R.drawable.mediumlevel;
                        break;
                    case 2:
                        levelLabel="Nivel dificil";
                        drawableRes = R.drawable.hardlevel;
                        break;
                    default:
                        drawableRes = R.drawable.easylevel; // fallback
                }
                statusImage.setImageResource(drawableRes);
                levelText.setText(levelLabel);
            }
        }
    }


    public static class ScoreViewHolder extends RecyclerView.ViewHolder {
        TextView scoreText, statusText, scoreMaxText;
        ImageView statusImage;
        public ScoreViewHolder(@NonNull View itemView)
        { super(itemView);
            scoreText = itemView.findViewById(R.id.score_text);
            scoreMaxText = itemView.findViewById(R.id.score_max_text);
            statusText = itemView.findViewById(R.id.status_text);
            statusImage = itemView.findViewById(R.id.status_image);

        }
        public void bind(int score)
        {
            scoreText.setText(String.valueOf(score));
            if (score >= 80) {
                statusText.setText("¡Excelente!");
                int color = Color.parseColor("#39e186");
                scoreText.setTextColor(color);
                statusText.setTextColor(color);
                scoreMaxText.setTextColor(color);
                statusImage.setImageResource(R.drawable.thumbup);
            } else if (score >= 50) {
                statusText.setText("Buen desempeño");
                int color = Color.parseColor("#F1C40F");
                scoreText.setTextColor(color);
                statusText.setTextColor(color);
                scoreMaxText.setTextColor(color);
                statusImage.setImageResource(R.drawable.claps);
            } else {
                statusText.setText("Puedes mejorar");
                int color = Color.parseColor("#F18181");
                scoreText.setTextColor(color);
                statusText.setTextColor(color);
                scoreMaxText.setTextColor(color);
                statusImage.setImageResource(R.drawable.keep);
            }

        }
    }


    public static class PieChartViewHolder extends RecyclerView.ViewHolder {
        GridLayout gridLayout;

        public PieChartViewHolder(@NonNull View itemView) {
            super(itemView);
            gridLayout = itemView.findViewById(R.id.chart_grid);
        }

        public void bind(List<ButtonAccuracy> buttonAccuracies, String label) {
            gridLayout.removeAllViews();

            String[] colorNames = {"Rojo", "Azul", "Verde", "Amarillo"};

            // Colores principales (flúo)
            int[] brightColors = {
                    Color.parseColor("#FF4C4C"), // rojo fuerte
                    Color.parseColor("#4C6FFF"), // azul fuerte
                    Color.parseColor("#00FF72"), // verde flúo
                    Color.parseColor("#FFF74C")  // amarillo fuerte
            };

            // Colores pastel (para fondo)
            int[] pastelColors = {
                    Color.parseColor("#FFB3B3"),
                    Color.parseColor("#B3C6FF"),
                    Color.parseColor("#B3FFD1"),
                    Color.parseColor("#FFFAB3")
            };

            for (int i = 0; i < 4; i++) {
                float value = -1f; // -1 = sin datos
                boolean hasData = false;

                for (ButtonAccuracy ba : buttonAccuracies) {
                    if (ba.getButtonId() == i) {
                        value = ba.getAccuracy();
                        hasData = ba.hasData();
                        break;
                    }
                }

                LinearLayout container = new LinearLayout(itemView.getContext());
                container.setOrientation(LinearLayout.VERTICAL);
                container.setGravity(Gravity.CENTER);
                container.setPadding(16, 16, 16, 16);

                // Crear el PieChart
                com.github.mikephil.charting.charts.PieChart pieChart =
                        new com.github.mikephil.charting.charts.PieChart(itemView.getContext());
                pieChart.setLayoutParams(new LinearLayout.LayoutParams(400, 400));
                pieChart.getDescription().setEnabled(false);
                pieChart.getLegend().setEnabled(false);

                // Fondo pastel (como color base del gráfico)
                pieChart.setHoleColor(pastelColors[i]);

                // Configurar el centro del gráfico (círculo interno)
                pieChart.setDrawHoleEnabled(true);
                pieChart.setHoleRadius(70f); // tamaño del hueco blanco
                pieChart.setTransparentCircleRadius(0f);

                // Texto central: porcentaje
                if (hasData && value >= 0) {
                    ArrayList<PieEntry> pieEntries = new ArrayList<>();
                    pieEntries.add(new PieEntry(value, ""));
                    pieEntries.add(new PieEntry(100 - value, ""));

                    PieDataSet dataSet = new PieDataSet(pieEntries, "");
                    dataSet.setColors(brightColors[i], Color.LTGRAY);
                    dataSet.setValueTextSize(0f);

                    PieData data = new PieData(dataSet);
                    pieChart.setData(data);

                    // Mostrar el % en el centro
                    pieChart.setDrawCenterText(true);
                    pieChart.setCenterText(String.format("%.0f%%", value));
                    pieChart.setCenterTextSize(20f);
                    pieChart.setCenterTextColor(brightColors[i]);
                } else {
                    // Si no hay datos, mostrar gris
                    ArrayList<PieEntry> pieEntries = new ArrayList<>();
                    pieEntries.add(new PieEntry(100, ""));
                    PieDataSet dataSet = new PieDataSet(pieEntries, "");
                    dataSet.setColors(Color.LTGRAY);
                    dataSet.setValueTextSize(0f);

                    PieData data = new PieData(dataSet);
                    pieChart.setData(data);

                    pieChart.setDrawCenterText(true);
                    pieChart.setCenterText("–");
                    pieChart.setCenterTextSize(20f);
                    pieChart.setCenterTextColor(Color.DKGRAY);
                    pieChart.setHoleColor(Color.LTGRAY);
                }

                pieChart.invalidate();

                // Etiqueta debajo del gráfico (solo el color)
                TextView labelView = new TextView(itemView.getContext());
                labelView.setText(colorNames[i]);
                labelView.setTextColor(brightColors[i]);
                labelView.setGravity(Gravity.CENTER);
                labelView.setTextSize(14f);
                labelView.setPadding(0, 8, 0, 0);

                container.addView(pieChart);
                container.addView(labelView);
                gridLayout.addView(container);
            }
        }
    }

    public static class BarChartViewHolder extends RecyclerView.ViewHolder {
        private GridLayout gridLayout;

        public BarChartViewHolder(@NonNull View itemView) {
            super(itemView);
            gridLayout = itemView.findViewById(R.id.chart_grid);
        }

        public void bind(List<GameMetric.ButtonAvgTime> buttonAvgTimes, String label) {
            gridLayout.removeAllViews();
            gridLayout.setColumnCount(2);
            gridLayout.setRowCount(2);

            for (GameMetric.ButtonAvgTime bat : buttonAvgTimes) {
                View cell = LayoutInflater.from(gridLayout.getContext())
                        .inflate(R.layout.item_avg_time_simonbutton_cell, gridLayout, false);

                ImageView icon = cell.findViewById(R.id.avg_time_icon);
                TextView text = cell.findViewById(R.id.avg_time_text);

                // Configuración del texto
                if (bat.hasData()) {
                    text.setText(String.format(Locale.getDefault(), "%.2fs", bat.getAvgTime()));
                } else {
                    text.setText("—");
                }

                // Colores y recursos según el botón
                int color;
                int drawableId;
                switch (bat.getButtonId()) {
                    case 0: // Verde
                        color = Color.parseColor("#4CAF50");
                        drawableId = R.drawable.cronometro_verde;
                        break;
                    case 1: // Rojo
                        color = Color.parseColor("#F44336");
                        drawableId = R.drawable.cronometro_rojo;
                        break;
                    case 2: // Azul
                        color = Color.parseColor("#2196F3");
                        drawableId = R.drawable.cronometro_azul;
                        break;
                    case 3: // Amarillo
                        color = Color.parseColor("#FFEB3B");
                        drawableId = R.drawable.cronometro_amarillo;
                        break;
                    default:
                        color = Color.GRAY;
                        drawableId = R.drawable.cronometro_verde;
                }

                icon.setImageResource(drawableId);
                text.setTextColor(color);

                gridLayout.addView(cell);
            }
        }
    }



}
