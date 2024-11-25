package com.elanco.countrypopulation.util;

import java.awt.*;

public class ColorMapper {
    public static String getColorForPopulationDensity(double density) {
        // Gradient from Green (low population) to Red (high population)
        if (density < 50) return "#2ECC71";   // Green
        if (density < 100) return "#F39C12";  // Yellow
        if (density < 200) return "#E74C3C";  // Orange
        return "#8E44AD";  // Deep Purple (extremely high density)
    }

    public static Color interpolateColor(Color start, Color end, double fraction) {
        fraction = Math.min(1, Math.max(0, fraction));

        int red = (int) (start.getRed() + fraction * (end.getRed() - start.getRed()));
        int green = (int) (start.getGreen() + fraction * (end.getGreen() - start.getGreen()));
        int blue = (int) (start.getBlue() + fraction * (end.getBlue() - start.getBlue()));

        return new Color(red, green, blue);
    }
}