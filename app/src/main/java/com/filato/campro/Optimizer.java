package com.filato.campro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Optimizer {
    private Optimizer() {}

    public static OptimizerResult optimize(LinkedHashMap<Integer, Integer> order, Settings settings) {
        OptimizerResult result = new OptimizerResult();
        List<Integer> pieces = new ArrayList<Integer>();

        for (Map.Entry<Integer, Integer> entry : order.entrySet()) {
            int quantity = Math.max(0, entry.getValue());
            for (int i = 0; i < quantity; i++) pieces.add(entry.getKey());
        }

        Collections.sort(pieces, new Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) {
                return b - a;
            }
        });

        for (int nominal : pieces) place(result.sheets, nominal, settings);
        group(result);
        return result;
    }

    private static void place(List<SheetPlan> sheets, int nominal, Settings settings) {
        double actual = Math.max(1, nominal - settings.actualOffset);

        for (SheetPlan sheet : sheets) {
            double add = sheet.parts.isEmpty() ? actual : settings.kerf + actual;
            if (sheet.usedWidth + add <= settings.sheetWidth + 0.001) {
                double x = sheet.parts.isEmpty() ? 0 : sheet.usedWidth + settings.kerf;
                sheet.parts.add(new Part(nominal, actual, x));
                sheet.usedWidth = x + actual;
                return;
            }
        }

        SheetPlan sheet = new SheetPlan();
        sheet.parts.add(new Part(nominal, actual, 0));
        sheet.usedWidth = actual;
        sheets.add(sheet);
    }

    private static void group(OptimizerResult result) {
        LinkedHashMap<String, SheetGroup> map = new LinkedHashMap<String, SheetGroup>();

        for (int i = 0; i < result.sheets.size(); i++) {
            SheetPlan sheet = result.sheets.get(i);
            String key = sheet.key();
            SheetGroup group = map.get(key);
            if (group == null) {
                group = new SheetGroup();
                group.key = key;
                group.sample = sheet;
                map.put(key, group);
            }
            group.count++;
            group.sheetNumbers.add(i + 1);
        }

        result.groups.addAll(map.values());
    }
}
