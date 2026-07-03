package com.filato.campro;

import java.util.ArrayList;
import java.util.List;

public class CheckResult {
    public double minX = 0;
    public double maxX = 0;
    public double minY = 0;
    public double maxY = 0;
    public double minZ = 0;
    public double maxZ = 0;
    public final List<String> errors = new ArrayList<String>();
    public final List<String> warnings = new ArrayList<String>();
}
