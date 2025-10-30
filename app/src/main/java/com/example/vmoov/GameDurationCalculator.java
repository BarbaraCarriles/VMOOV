package com.example.vmoov;

public class GameDurationCalculator {

    /**
     * Calcula la duración total entre dos timestamps (en milisegundos)
     * y la devuelve en formato legible tipo cronómetro: "hh:mm:ss (h:m:s)".
     *
     * @param startTime tiempo de inicio en milisegundos
     * @param endTime tiempo de fin en milisegundos
     * @return duración formateada
     */
    public static String calculateGameDuration(long startTime, long endTime) {
        if (startTime <= 0 || endTime <= 0 || endTime < startTime) {
            return "00:00:00 (h:m:s)";
        }

        long durationMillis = endTime - startTime;

        long hours = (durationMillis / (1000 * 60 * 60)) % 24;
        long minutes = (durationMillis / (1000 * 60)) % 60;
        long seconds = (durationMillis / 1000) % 60;

        return String.format("%02d:%02d:%02d (h:m:s)", hours, minutes, seconds);
    }

    /**
     * Devuelve la duración total en segundos (por si necesitás cálculos numéricos).
     */
    public static double calculateDurationSeconds(long startTime, long endTime) {
        if (startTime <= 0 || endTime <= 0 || endTime < startTime) {
            return 0.0;
        }
        return (endTime - startTime) / 1000.0;
    }
}

