package com.filato.campro;

public class Settings {
    public double sheetWidth = 1540;
    public double sheetHeight = 3000;
    public double thickness = 18;
    public double safeZ = 50;
    public double edgeSkim = 1;
    public double actualOffset = 27;
    public double kerf = 12;
    public double depthPerPass = 18;
    public double t4Feed = 8000;
    public double t4Plunge = 1500;
    public double t4Rpm = 18000;
    public double t5Plunge = 1500;
    public double t5Rpm = 15000;

    public static Settings defaults() {
        return new Settings();
    }

    public double get(String key) {
        if ("sheetWidth".equals(key)) return sheetWidth;
        if ("sheetHeight".equals(key)) return sheetHeight;
        if ("thickness".equals(key)) return thickness;
        if ("safeZ".equals(key)) return safeZ;
        if ("edgeSkim".equals(key)) return edgeSkim;
        if ("actualOffset".equals(key)) return actualOffset;
        if ("kerf".equals(key)) return kerf;
        if ("depthPerPass".equals(key)) return depthPerPass;
        if ("t4Feed".equals(key)) return t4Feed;
        if ("t4Plunge".equals(key)) return t4Plunge;
        if ("t4Rpm".equals(key)) return t4Rpm;
        if ("t5Plunge".equals(key)) return t5Plunge;
        if ("t5Rpm".equals(key)) return t5Rpm;
        return 0;
    }

    public void set(String key, double value) {
        if ("sheetWidth".equals(key)) sheetWidth = value;
        else if ("sheetHeight".equals(key)) sheetHeight = value;
        else if ("thickness".equals(key)) thickness = value;
        else if ("safeZ".equals(key)) safeZ = value;
        else if ("edgeSkim".equals(key)) edgeSkim = value;
        else if ("actualOffset".equals(key)) actualOffset = value;
        else if ("kerf".equals(key)) kerf = value;
        else if ("depthPerPass".equals(key)) depthPerPass = value;
        else if ("t4Feed".equals(key)) t4Feed = value;
        else if ("t4Plunge".equals(key)) t4Plunge = value;
        else if ("t4Rpm".equals(key)) t4Rpm = value;
        else if ("t5Plunge".equals(key)) t5Plunge = value;
        else if ("t5Rpm".equals(key)) t5Rpm = value;
    }
}
