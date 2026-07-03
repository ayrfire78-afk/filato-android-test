package com.filato.campro;

public final class CncGenerator {
    private CncGenerator() {}

    public static String generate(SheetGroup group, Settings settings) {
        if (group == null || group.sample == null) return "";

        StringBuilder builder = new StringBuilder();
        line(builder, "(FILATO CAM PRO ANDROID V9)");
        line(builder, "(GROUP " + group.key + " COUNT " + group.count + ")");
        line(builder, "(WARNING: TEST GEOMETRY UNTIL ARTCAM PRESETS ARE LOADED)");
        line(builder, "G90 G80 G54");
        line(builder, "T5");
        line(builder, "G43 H5");
        line(builder, "G0 X0.000 Y0.000 Z" + FormatUtil.cnc(settings.safeZ));
        line(builder, "M05");
        line(builder, "M06 T5");
        line(builder, "G43 H5");
        line(builder, "M03 S" + (int) settings.t5Rpm);

        for (Part part : group.sample.parts) drillPart(builder, part, settings);

        line(builder, "M5");
        line(builder, "M06 T4");
        line(builder, "G43 H4");
        line(builder, "M03 S" + (int) settings.t4Rpm);
        line(builder, "G00 X0.000 Y0.000 Z" + FormatUtil.cnc(settings.safeZ));

        for (Part part : group.sample.parts) cutSlots(builder, part, settings);

        cutOuter(builder, group.sample, settings);

        line(builder, "G00 Z" + FormatUtil.cnc(settings.safeZ));
        line(builder, "G00 X0.000 Y0.000");
        line(builder, "M5");
        line(builder, "M30");
        return builder.toString();
    }

    private static void drillPart(StringBuilder builder, Part part, Settings settings) {
        double leftX = part.x + 21;
        double rightX = part.x + part.actualWidth - 21;
        double[] ys = {89, 560, 1486, 2112, 2712, 2911};
        for (double y : ys) {
            drill(builder, leftX, y, settings);
            drill(builder, rightX, y, settings);
        }
    }

    private static void drill(StringBuilder builder, double x, double y, Settings settings) {
        line(builder, "G00 X" + FormatUtil.cnc(x) + " Y" + FormatUtil.cnc(settings.sheetHeight - y) + " Z" + FormatUtil.cnc(settings.safeZ));
        line(builder, "G00 Z" + FormatUtil.cnc(settings.thickness + 1.8));
        line(builder, "G1 Z-" + FormatUtil.cnc(settings.thickness) + " F" + (int) settings.t5Plunge);
        line(builder, "G00 Z" + FormatUtil.cnc(settings.safeZ));
    }

    private static void cutSlots(StringBuilder builder, Part part, Settings settings) {
        double x1 = part.x + 8;
        double x2 = part.x + part.actualWidth - 8;
        double[] ys = {560, 1637, 2112, 2712};
        for (double y : ys) rectCut(builder, x1, y - 9, x2, y + 9, settings);
    }

    private static void cutOuter(StringBuilder builder, SheetPlan sheet, Settings settings) {
        double x1 = -settings.edgeSkim;
        double y1 = -settings.edgeSkim;
        double x2 = sheet.usedWidth + settings.edgeSkim;
        double y2 = settings.sheetHeight - settings.edgeSkim;
        rectCut(builder, x1, y1, x2, y2, settings);
    }

    private static void rectCut(StringBuilder builder, double x1, double y1, double x2, double y2, Settings settings) {
        int passes = Math.max(1, (int) Math.ceil(settings.thickness / Math.max(0.1, settings.depthPerPass)));
        for (int pass = 1; pass <= passes; pass++) {
            double z = Math.min(settings.thickness, pass * settings.depthPerPass);
            line(builder, "G00 X" + FormatUtil.cnc(x1) + " Y" + FormatUtil.cnc(settings.sheetHeight - y1) + " Z" + FormatUtil.cnc(settings.safeZ));
            line(builder, "G1 Z-" + FormatUtil.cnc(z) + " F" + (int) settings.t4Plunge);
            line(builder, "G1 X" + FormatUtil.cnc(x2) + " Y" + FormatUtil.cnc(settings.sheetHeight - y1) + " F" + (int) settings.t4Feed);
            line(builder, "G1 X" + FormatUtil.cnc(x2) + " Y" + FormatUtil.cnc(settings.sheetHeight - y2));
            line(builder, "G1 X" + FormatUtil.cnc(x1) + " Y" + FormatUtil.cnc(settings.sheetHeight - y2));
            line(builder, "G1 X" + FormatUtil.cnc(x1) + " Y" + FormatUtil.cnc(settings.sheetHeight - y1));
            line(builder, "G00 Z" + FormatUtil.cnc(settings.safeZ));
        }
    }

    private static void line(StringBuilder builder, String line) {
        builder.append(line).append('\n');
    }
}
