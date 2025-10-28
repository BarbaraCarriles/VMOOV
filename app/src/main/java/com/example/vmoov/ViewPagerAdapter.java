package com.example.vmoov;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private int numberOfBars;
    private String type;
    private String gameId;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, int numberOfBars, String type, String gameId) {
        super(fragmentActivity);
        this.numberOfBars = numberOfBars;
        this.type = type;
        this.gameId = gameId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: //grafico duracion promedio
                return Chart1Fragment.newInstance(
                        "Duración Promedio",
                        "Tiempo promedio de ejecución de cada movimiento.",
                        type,
                        numberOfBars,
                        true,   // Este gráfico es de duración
                        false,  // No es de cantidad de movimientos
                        gameId  // Pasa el gameId dinámico
                );
            case 1: //grafico movimientos exitosos
                return Chart1Fragment.newInstance(
                        "Movimientos Exitosos",
                        "Número de movimientos correctos realizados.",
                        type,
                        numberOfBars,
                        false,  // No es gráfico de duración
                        true,   // Este gráfico es de cantidad de movimientos
                        gameId  // Pasa el gameId dinámico
                );
            default:
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Dos gráficos en el ViewPager
    }
}
