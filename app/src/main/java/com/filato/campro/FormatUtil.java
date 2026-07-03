package com.filato.campro;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class FormatUtil {
    private FormatUtil() {}

    public static String fmt(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("0.###", symbols);
        return format.format(value);
    }

    public static String cnc(double value) {
        return String.format(Locale.US, "%.3f", value);
    }
}
