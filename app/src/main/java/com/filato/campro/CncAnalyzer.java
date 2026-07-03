package com.filato.campro;

public final class CncAnalyzer {
    private CncAnalyzer() {}

    public static CheckResult check(String cnc, Settings settings) {
        CheckResult result = new CheckResult();
        double x = 0;
        double y = 0;
        double z = settings.safeZ;
        result.minZ = settings.safeZ;
        result.maxZ = settings.safeZ;

        boolean hasT4 = cnc.contains("M06 T4");
        boolean hasT5 = cnc.contains("M06 T5");
        boolean hasSpindle = cnc.contains("M03") || cnc.contains("M3");

        String[] lines = cnc.split("\\n");
        for (String raw : lines) {
            String line = raw.trim();
            Double vx = axis(line, 'X');
            Double vy = axis(line, 'Y');
            Double vz = axis(line, 'Z');
            if (vx != null) x = vx;
            if (vy != null) y = vy;
            if (vz != null) z = vz;

            result.minX = Math.min(result.minX, x);
            result.maxX = Math.max(result.maxX, x);
            result.minY = Math.min(result.minY, y);
            result.maxY = Math.max(result.maxY, y);
            result.minZ = Math.min(result.minZ, z);
            result.maxZ = Math.max(result.maxZ, z);
        }

        double tolerance = 8;
        if (result.minX < -tolerance) result.errors.add("X ниже 0: " + FormatUtil.cnc(result.minX));
        if (result.minY < -tolerance) result.errors.add("Y ниже 0: " + FormatUtil.cnc(result.minY));
        if (result.maxX > settings.sheetWidth + tolerance) result.errors.add("X больше листа: " + FormatUtil.cnc(result.maxX));
        if (result.maxY > settings.sheetHeight + tolerance) result.errors.add("Y больше листа: " + FormatUtil.cnc(result.maxY));
        if (result.minZ < -settings.thickness - 0.05) result.errors.add("Z глубже толщины: " + FormatUtil.cnc(result.minZ));

        if (!hasT5) result.warnings.add("Нет смены T5.");
        if (!hasT4) result.warnings.add("Нет смены T4.");
        if (!hasSpindle) result.warnings.add("Нет запуска шпинделя.");
        if (result.errors.isEmpty()) result.warnings.add("Проверка координат пройдена. ArtCAM-идентичность пока не проверялась.");
        return result;
    }

    private static Double axis(String line, char axis) {
        int start = line.indexOf(axis);
        if (start < 0) return null;

        int end = start + 1;
        while (end < line.length()) {
            char c = line.charAt(end);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == ',') end++;
            else break;
        }

        if (end == start + 1) return null;

        try {
            return Double.parseDouble(line.substring(start + 1, end).replace(',', '.'));
        } catch (Throwable ignored) {
            return null;
        }
    }
}
