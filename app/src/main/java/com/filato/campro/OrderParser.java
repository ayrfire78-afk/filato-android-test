package com.filato.campro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OrderParser {
    private static final Map<String, Integer> NUMBERS = new HashMap<String, Integer>();

    static {
        put(1, "один", "одна", "одно");
        put(2, "два", "две");
        put(3, "три");
        put(4, "четыре");
        put(5, "пять");
        put(6, "шесть");
        put(7, "семь");
        put(8, "восемь");
        put(9, "девять");
        put(10, "десять");
        put(11, "одиннадцать");
        put(12, "двенадцать");
        put(13, "тринадцать");
        put(14, "четырнадцать");
        put(15, "пятнадцать");
        put(16, "шестнадцать");
        put(17, "семнадцать");
        put(18, "восемнадцать");
        put(19, "девятнадцать");
        put(20, "двадцать");
        put(30, "тридцать");
        put(40, "сорок");
        put(50, "пятьдесят");
        put(60, "шестьдесят");
        put(70, "семьдесят");
        put(80, "восемьдесят");
        put(90, "девяносто");
        put(100, "сто");
        put(200, "двести");
        put(300, "триста");
        put(400, "четыреста");
        put(500, "пятьсот");
        put(600, "шестьсот");
        put(700, "семьсот");
        put(800, "восемьсот");
        put(900, "девятьсот");
    }

    private OrderParser() {}

    private static void put(int value, String... words) {
        for (String word : words) NUMBERS.put(word, value);
    }

    public static LinkedHashMap<Integer, Integer> parse(String text) {
        LinkedHashMap<Integer, Integer> result = new LinkedHashMap<Integer, Integer>();
        String normalized = normalize(text);
        if (normalized.length() == 0) return result;

        String[] tokens = normalized.split(" ");
        int i = 0;

        while (i < tokens.length) {
            int separator = -1;
            for (int k = i; k < tokens.length; k++) {
                if (isSeparator(tokens[k])) {
                    separator = k;
                    break;
                }
            }

            if (separator < 0) break;

            int left = separator - 1;
            while (left >= i && (isUnit(tokens[left]) || isConnector(tokens[left]))) left--;

            ArrayList<String> quantityTokens = new ArrayList<String>();
            while (left >= i && isNumberToken(tokens[left])) {
                quantityTokens.add(0, tokens[left]);
                left--;
            }

            int quantity = number(quantityTokens);

            int right = separator + 1;
            while (right < tokens.length && (isUnit(tokens[right]) || isConnector(tokens[right]))) right++;

            ArrayList<String> widthTokens = new ArrayList<String>();
            while (right < tokens.length) {
                String token = tokens[right];

                if (isUnit(token) || isConnector(token) || isSeparator(token)) {
                    if (!widthTokens.isEmpty()) break;
                    right++;
                    continue;
                }

                if (!isNumberToken(token)) break;

                if (!widthTokens.isEmpty() && startsNextPosition(tokens, right) && !shouldJoinWidth(widthTokens, tokens, right)) {
                    break;
                }

                widthTokens.add(token);
                right++;
            }

            int width = number(widthTokens);

            if (quantity > 0 && width > 0) {
                Integer old = result.get(width);
                result.put(width, (old == null ? 0 : old) + quantity);
            }

            i = Math.max(right, separator + 1);
        }

        return result;
    }

    public static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replace("x", " х ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace(';', ' ')
                .replace(',', ' ')
                .replace('.', ' ')
                .replaceAll("[^0-9а-яa-z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isSeparator(String token) {
        return "на".equals(token)
                || "по".equals(token)
                || "х".equals(token)
                || "x".equals(token)
                || "шириной".equals(token)
                || "размером".equals(token);
    }

    private static boolean isUnit(String token) {
        return "листов".equals(token)
                || "листа".equals(token)
                || "лист".equals(token)
                || "листы".equals(token)
                || "штук".equals(token)
                || "штуки".equals(token)
                || "шт".equals(token)
                || "деталей".equals(token)
                || "детали".equals(token)
                || "деталь".equals(token)
                || "мм".equals(token)
                || "миллиметров".equals(token)
                || "миллиметра".equals(token)
                || "миллиметр".equals(token);
    }

    private static boolean isConnector(String token) {
        return "еще".equals(token)
                || "и".equals(token)
                || "потом".equals(token)
                || "затем".equals(token)
                || "дальше".equals(token)
                || "плюс".equals(token)
                || "также".equals(token)
                || "ну".equals(token)
                || "там".equals(token)
                || "много".equals(token)
                || "добавь".equals(token)
                || "прибавь".equals(token)
                || "далее".equals(token);
    }

    private static boolean isNumberToken(String token) {
        if (token == null || token.length() == 0) return false;
        if (token.matches("\\d+")) return true;
        if ("тысяча".equals(token) || "тысячи".equals(token) || "тысяч".equals(token)) return true;
        return NUMBERS.containsKey(token);
    }

    private static int tokenValue(String token) {
        if (token.matches("\\d+")) return Integer.parseInt(token);
        if ("тысяча".equals(token) || "тысячи".equals(token) || "тысяч".equals(token)) return 1000;
        Integer value = NUMBERS.get(token);
        return value == null ? 0 : value;
    }

    private static boolean startsNextPosition(String[] tokens, int index) {
        int k = index;
        int count = 0;

        while (k < tokens.length && isNumberToken(tokens[k]) && count < 4) {
            count++;
            k++;
        }

        if (count == 0) return false;
        while (k < tokens.length && isUnit(tokens[k])) k++;
        return k < tokens.length && isSeparator(tokens[k]);
    }

    private static boolean shouldJoinWidth(ArrayList<String> widthTokens, String[] tokens, int index) {
        if (index + 1 < tokens.length && isSeparator(tokens[index + 1])) return false;

        if (index + 1 < tokens.length && isUnit(tokens[index + 1])) {
            int k = index + 1;
            while (k < tokens.length && isUnit(tokens[k])) k++;
            if (k < tokens.length && isSeparator(tokens[k])) return false;
        }

        int current = number(widthTokens);
        int candidate = tokenValue(tokens[index]);

        if (candidate <= 0) return false;
        if (current == 1000 && candidate < 1000) return true;
        return current > 0 && current < 1000 && current % 100 == 0 && candidate < 100;
    }

    private static int number(List<String> tokens) {
        int total = 0;
        int current = 0;
        boolean found = false;

        for (String token : tokens) {
            if (token == null || token.length() == 0) continue;

            if (token.matches("\\d+")) {
                current += Integer.parseInt(token);
                found = true;
                continue;
            }

            if ("тысяча".equals(token) || "тысячи".equals(token) || "тысяч".equals(token)) {
                if (current == 0) current = 1;
                total += current * 1000;
                current = 0;
                found = true;
                continue;
            }

            Integer value = NUMBERS.get(token);
            if (value != null) {
                current += value;
                found = true;
            }
        }

        return found ? total + current : 0;
    }
}
