package com.filato.campro;

public class Part {
    public final int nominalWidth;
    public final double actualWidth;
    public final double x;

    public Part(int nominalWidth, double actualWidth, double x) {
        this.nominalWidth = nominalWidth;
        this.actualWidth = actualWidth;
        this.x = x;
    }
}
