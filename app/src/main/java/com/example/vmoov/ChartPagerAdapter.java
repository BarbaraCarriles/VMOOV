package com.example.vmoov;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChartPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private int[] chartData = new int[4]; // Datos de los gráficos
    private int prescribedSessions = 1; // Sesiones recetadas
    private int prescribedSteps = 1; // Pasos recetados
    private int totalSteps = 1;
    private int successfulSteps = 0;
    private int gamesPlayed = 0; //
    private int sessionsPlayed = 0; //
    private int executionTimeChange = 0; // Cambio porcentual en tiempo de ejecución
    private int generalScore = 0; // Nueva variable para almacenar el puntaje general
    //private int difficultyLevel = 0; // Nivel por defecto

    private static final int VIEW_TYPE_RATIO = 0;  // Para mostrar resultado/total
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_SCORE = 2; // Nueva vista para el Puntaje General
    private static final int VIEW_TYPE_NUMBER = 3; // Para cantidad sesiones
    private static final int VIEW_TYPE_DIFFICULTY = 4; // Para cantidad sesiones


    public ChartPagerAdapter(Context context) {
        this.context = context;
    }

   public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
        notifyDataSetChanged();
    }

    public void setSessionsPlayed(int sessionsPlayed) {
        this.sessionsPlayed = sessionsPlayed;
        notifyDataSetChanged();
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = Math.max(totalSteps, 1);
        notifyDataSetChanged();
    }

    public void setSuccessfulSteps(int steps) {
        this.successfulSteps = steps;
        notifyDataSetChanged();
    }

    public void setPrescribedSteps(int steps) {
        this.prescribedSteps = steps > 0 ? steps : 1;
        notifyDataSetChanged();
    }

    public void setPrescribedSessions(int sessions) {
        this.prescribedSessions = sessions > 0 ? sessions : 1;
        notifyDataSetChanged();
    }

    public void setExecutionTimeChange(int change) {
        this.executionTimeChange = change;
        notifyDataSetChanged();
    }
    public void setGeneralScore(int score) {
        this.generalScore = score;
        Log.d("ChartPagerAdapter", "🔵 setGeneralScore() - Puntaje General recibido: " + score);

        // Asegurar que la actualización se hace en el hilo principal
        new Handler(Looper.getMainLooper()).post(() -> {
            Log.d("ChartPagerAdapter", "🟢 Notificando cambio en RecyclerView con nuevo Puntaje General.");
            notifyDataSetChanged();
        });
    }

    /*
    public void setDifficultyLevel(int level) {
        this.difficultyLevel = level;
        notifyDataSetChanged();
    }*/


    @Override
    public int getItemViewType(int position) {
        switch (position) {
            case 0:
                return VIEW_TYPE_SCORE; // 🔹 Puntaje General
            case 1:
                return VIEW_TYPE_TEXT; // 🔹 Tiempo Promedio
            case 2:
                return VIEW_TYPE_RATIO; // 🔹 Movimientos Exitosos
            case 3:
                return modoValidacion ? VIEW_TYPE_NUMBER : VIEW_TYPE_RATIO; // 🔹 Sesiones Completadas
           /* case 4:
                return VIEW_TYPE_DIFFICULTY; //dificultad
*/
            default:
                return VIEW_TYPE_RATIO;
        }
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_TEXT) {
            View view = LayoutInflater.from(context).inflate(R.layout.fragment_execution_time, parent, false);
            return new ExecutionTimeViewHolder(view);
        } else if (viewType == VIEW_TYPE_SCORE) {
            View view = LayoutInflater.from(context).inflate(R.layout.fragment_score, parent, false);
            return new ScoreViewHolder(view);
        } else if (viewType == VIEW_TYPE_NUMBER) {
            View view = LayoutInflater.from(context).inflate(R.layout.fragment_number, parent, false);
            return new NumberViewHolder(view);
        }
      /*  else if (viewType == VIEW_TYPE_DIFFICULTY) {
            View view = LayoutInflater.from(context).inflate(R.layout.fragment_difficulty, parent, false);
            return new DifficultyViewHolder(view);
        }*/
        else {
            View view = LayoutInflater.from(context).inflate(R.layout.fragment_ratio, parent, false);
            return new RatioViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Log.d("ChartPagerAdapter", "📌 onBindViewHolder() - Posición: " + position);

        switch (position) {
            case 0: // 🔹 Puntaje General
                if (holder instanceof ScoreViewHolder) {
                    Log.d("ChartPagerAdapter", "🟢 Asignando Puntaje General al ScoreViewHolder: " + generalScore);
                    ((ScoreViewHolder) holder).bind(generalScore);
                }
                break;

            case 1: // 🔹 Tiempo Promedio de ejecución
                if (holder instanceof ExecutionTimeViewHolder) {
                    Log.d("ChartPagerAdapter", "🟡 Asignando ExecutionTimeChange: " + executionTimeChange);
                    ((ExecutionTimeViewHolder) holder).bind(executionTimeChange);
                }
                break;

            case 2: // 🔹 Movimientos Exitosos
                if (holder instanceof RatioViewHolder) {
                    if (modoValidacion) {
                        Log.d("ChartPagerAdapter", "🟣 Modo validación activo: mostrando valores absolutos de movimientos.");
                        ((RatioViewHolder) holder).bind(successfulSteps, totalSteps, "movimientos correctos");
                    } else {
                        Log.d("ChartPagerAdapter", "🟡 Mostrando ratio de movimientos exitosos.");
                        ((RatioViewHolder) holder).bind(successfulSteps, prescribedSteps, "movimientos correctos");
                    }
                }
                break;

            case 3: // Sesiones completadas
                if (modoValidacion) {
                    if (holder instanceof NumberViewHolder) {
                        ((NumberViewHolder) holder).bind(sessionsPlayed, "sesiones completadas");
                    }
                } else {
                    if (holder instanceof RatioViewHolder) {
                        ((RatioViewHolder) holder).bind(sessionsPlayed, prescribedSessions, "sesiones completadas");
                    }
                }
                break;

          /*  case 4: // 🔹 Nivel de dificultad
                if (holder instanceof DifficultyViewHolder) {
                    ((DifficultyViewHolder) holder).bind(difficultyLevel, "nivel");
                }
                break;*/

            default:
                Log.e("ChartPagerAdapter", "❌ ERROR: ViewHolder en posición inesperada - " + position);
                break;
        }
    }

    private boolean modoValidacion = false;

    public void setModoValidacion(boolean modoValidacion) {
        this.modoValidacion = modoValidacion;
        notifyDataSetChanged();
    }

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
            descriptionText.setText(description);

            // Cambiar color del número según progreso
            float progress = (total > 0) ? ((float) completed / total) * 100 : 0;

            if (progress < 50) {
                completedText.setTextColor(Color.parseColor("#F18181"));
                totalText.setTextColor(Color.parseColor("#F18181"));
                descriptionText.setTextColor(Color.parseColor("#F18181"));
                statusImage.setImageResource(R.drawable.addpatient);
            } else if (progress >= 50 && progress < 80) {
                completedText.setTextColor(Color.parseColor("#F1C40F")); // Naranja
                totalText.setTextColor(Color.parseColor("#F1C40F"));
                descriptionText.setTextColor(Color.parseColor("#F1C40F"));
                statusImage.setImageResource(R.drawable.keep);

            } else {
                completedText.setTextColor(Color.parseColor("#39e186")); // Verde
                totalText.setTextColor(Color.parseColor("#39e186"));
                descriptionText.setTextColor(Color.parseColor("#39e186"));
                statusImage.setImageResource(R.drawable.thumbup);

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
            descriptionText.setText(description);

            // Opcional: cambiar color según valor
            if (number < 5) {
                numberText.setTextColor(Color.parseColor("#F18181"));
                descriptionText.setTextColor(Color.parseColor("#F18181"));
                statusImage.setImageResource(R.drawable.keep);
            } else if (number < 10) {
                numberText.setTextColor(Color.parseColor("#F1C40F"));
                descriptionText.setTextColor(Color.parseColor("#F1C40F"));
                statusImage.setImageResource(R.drawable.claps);
            } else {
                numberText.setTextColor(Color.parseColor("#39e186"));
                descriptionText.setTextColor(Color.parseColor("#39e186"));
                statusImage.setImageResource(R.drawable.thumbup);
            }
        }
    }

    public static class DifficultyViewHolder extends RecyclerView.ViewHolder {
        TextView levelText, descriptionText;
        ImageView statusImage;

        public DifficultyViewHolder(@NonNull View itemView) {
            super(itemView);
            levelText = itemView.findViewById(R.id.level_text);
            descriptionText = itemView.findViewById(R.id.difficulty_description_text);
            statusImage = itemView.findViewById(R.id.difficulty_image);
        }

        public void bind(int level, String description) {
            levelText.setText(String.valueOf(level));
            descriptionText.setText(description);


            if (level < 3) {
                levelText.setTextColor(Color.parseColor("#F18181"));
                descriptionText.setTextColor(Color.parseColor("#F18181"));
                statusImage.setImageResource(R.drawable.keep);
            } else if (level < 6) {
                levelText.setTextColor(Color.parseColor("#F1C40F"));
                descriptionText.setTextColor(Color.parseColor("#F1C40F"));
                statusImage.setImageResource(R.drawable.claps);
            } else {
                levelText.setTextColor(Color.parseColor("#39e186"));
                descriptionText.setTextColor(Color.parseColor("#39e186"));
                statusImage.setImageResource(R.drawable.thumbup);
            }
        }
    }



    public static class ScoreViewHolder extends RecyclerView.ViewHolder {
        TextView scoreText, statusText,scoreMaxText;
        ImageView statusImage;

        public ScoreViewHolder(@NonNull View itemView) {
            super(itemView);
            scoreText = itemView.findViewById(R.id.score_text);
            statusText = itemView.findViewById(R.id.status_text);
            scoreMaxText = itemView.findViewById(R.id.score_max_text);
            statusImage = itemView.findViewById(R.id.status_image);

            // Verificar que los elementos no son null
            if (scoreText == null) Log.e("ScoreViewHolder", "❌ ERROR: scoreText es NULL");
            if (statusText == null) Log.e("ScoreViewHolder", "❌ ERROR: statusText es NULL");
            if (statusImage == null) Log.e("ScoreViewHolder", "❌ ERROR: statusImage es NULL");
        }

        public void bind(int generalScore) {
            Log.d("ScoreViewHolder", "📌 bind() - Puntaje recibido: " + generalScore);

            if (scoreText == null || statusText == null || statusImage == null) {
                Log.e("ScoreViewHolder", "❌ ERROR: Un elemento de UI es NULL");
                return;
            }

            scoreText.setText(String.format(Locale.getDefault(), "%d", generalScore));
            scoreMaxText.setText("/100");

            if (generalScore >= 80) {
                Log.d("ScoreViewHolder", "🏆 ¡Excelente! Puntaje: " + generalScore);
                statusText.setText("¡Excelente!");
                statusText.setTextColor(Color.parseColor("#39e186"));
                scoreMaxText.setTextColor(Color.parseColor("#39e186"));
                scoreText.setTextColor(Color.parseColor("#39e186"));
                statusImage.setImageResource(R.drawable.thumbup);
            } else if (generalScore >= 50) {
                Log.d("ScoreViewHolder", "👍 Buen desempeño. Puntaje: " + generalScore);
                statusText.setText("¡Buen desempeño!");
                statusText.setTextColor(Color.parseColor("#F1C40F"));
                scoreText.setTextColor(Color.parseColor("#F1C40F"));
                scoreMaxText.setTextColor(Color.parseColor("#F1C40F"));
                statusImage.setImageResource(R.drawable.claps);
            } else {
                Log.d("ScoreViewHolder", "⚠️ Puedes mejorar. Puntaje: " + generalScore);
                statusText.setText("¡Puedes mejorar!");
                statusText.setTextColor(Color.parseColor("#F18181"));
                scoreText.setTextColor(Color.parseColor("#F18181"));
                scoreMaxText.setTextColor(Color.parseColor("#F18181"));
                statusImage.setImageResource(R.drawable.keep);
            }
        }
    }


    @Override
    public int getItemCount() {
        return 4;
    }

    public static class ChartViewHolder extends RecyclerView.ViewHolder {
        PieChart pieChart;

        public ChartViewHolder(@NonNull View itemView) {
            super(itemView);
            pieChart = itemView.findViewById(R.id.pieChart);
        }
    }

    public static class ExecutionTimeViewHolder extends RecyclerView.ViewHolder {
        TextView percentageText, statusText, percentageSymb;
        ImageView rightImage;

        public ExecutionTimeViewHolder(@NonNull View itemView) {
            super(itemView);
            percentageText = itemView.findViewById(R.id.percentage_text);
            percentageSymb = itemView.findViewById(R.id.percentage_symbol);
            statusText = itemView.findViewById(R.id.status_text);
            rightImage = itemView.findViewById(R.id.bottom_image);
        }

        public void bind(int executionTimeChange) {
            if (executionTimeChange < 0) {
                percentageText.setText(String.format(Locale.getDefault(), "%d", Math.abs(executionTimeChange)));
                statusText.setText("más rápido");
                statusText.setTextColor(Color.parseColor("#39e186"));
                percentageSymb.setTextColor(Color.parseColor("#39e186"));
                percentageText.setTextColor(Color.parseColor("#39e186"));
                rightImage.setImageResource(R.drawable.thumbup);
            } else if (executionTimeChange > 0) {
                percentageText.setText(String.format(Locale.getDefault(), "%d", executionTimeChange));
                statusText.setText("más lento");
                statusText.setTextColor(Color.parseColor("#F18181"));
                percentageText.setTextColor(Color.parseColor("#F18181"));
                percentageSymb.setTextColor(Color.parseColor("#F18181"));
                rightImage.setImageResource(R.drawable.keep);
            } else {
                percentageText.setText("0%");
                statusText.setText("sin cambios");
                statusText.setTextColor(Color.parseColor("#dfaaff"));
                percentageText.setTextColor(Color.parseColor("#dfaaff"));
                percentageSymb.setTextColor(Color.parseColor("#dfaaff"));
                rightImage.setImageResource(R.drawable.work);
            }
        }
    }


    private void setupPieChart(PieChart chart, int value, int maxValue, String label) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(value, label));
        entries.add(new PieEntry(maxValue - value, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                context.getResources().getColor(R.color.violet, null),
                context.getResources().getColor(R.color.lightgrey, null)
        );

        PieData pieData = new PieData(dataSet);
        chart.setData(pieData);
        chart.setHoleRadius(60f);
        chart.setTransparentCircleRadius(65f);
        dataSet.setDrawValues(true);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.invalidate();
    }
}
