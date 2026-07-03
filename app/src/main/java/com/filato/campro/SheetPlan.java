package com.filato.campro;

import java.util.ArrayList;
import java.util.List;

public class SheetPlan {
    public final List<Part> parts = new ArrayList<Part>();
    public double usedWidth;

    public String key() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) builder.append('+');
            builder.append(parts.get(i).nominalWidth);
        }
        return builder.toString();
    }
}
