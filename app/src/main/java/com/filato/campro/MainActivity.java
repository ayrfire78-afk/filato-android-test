package com.filato.campro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final int DARK = Color.rgb(23, 31, 44);
    private static final int INK = Color.rgb(35, 55, 86);
    private static final int BLUE = Color.rgb(38, 91, 161);
    private static final int GREEN = Color.rgb(28, 132, 78);
    private static final int RED = Color.rgb(170, 50, 42);
    private static final int CARD = Color.rgb(248, 250, 252);
    private static final int LINE = Color.rgb(178, 190, 205);
    private static final int WARN_BG = Color.rgb(255, 239, 195);

    private static final int REQ_SAVE_ORDER = 20;
    private static final int REQ_EXPORT_ZIP = 21;
    private static final int REQ_SAVE_CNC = 22;
    private static final int REQ_LOAD_ORDER = 23;

    private final int[] sizes = buildSizes();
    private final LinkedHashMap<Integer, Integer> order = new LinkedHashMap<Integer, Integer>();
    private final LinkedHashMap<Integer, EditText> quantityBoxes = new LinkedHashMap<Integer, EditText>();
    private final LinkedHashMap<String, EditText> settingBoxes = new LinkedHashMap<String, EditText>();

    private ScrollView scrollView;
    private LinearLayout root;
    private LinearLayout settingsSection;
    private LinearLayout orderSection;
    private LinearLayout previewSection;
    private LinearLayout gcodeSection;
    private LinearLayout checkSection;

    private TextView statusView;
    private TextView groupsView;
    private TextView cncView;
    private TextView checkView;
    private TextView logView;
    private TextView progressText;
    private EditText orderNameBox;
    private EditText orderTextBox;
    private CheckBox safetyBox;
    private CheckBox autoDepthBox;
    private CheckBox showCoordsBox;
    private ProgressBar progressBar;
    private Preview2DView preview2d;

    private Settings settings = Settings.defaults();
    private OptimizerResult result = new OptimizerResult();
    private String orderName = "ORDER_001";
    private int groupIndex = 0;
    private boolean paramsLocked = true;
    private boolean safetyAccepted = true;
    private double zoom2d = 1.0;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(DARK);
        for (int size : sizes) order.put(size, 0);
        buildInterface();
        refreshViews();
    }

    private static int[] buildSizes() {
        int[] values = new int[21];
        int index = 0;
        for (int size = 200; size <= 1200; size += 50) values[index++] = size;
        return values;
    }

    private void buildInterface() {
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setHorizontalScrollBarEnabled(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.setBackgroundColor(Color.rgb(235, 239, 245));
        scrollView.addView(root, new ScrollView.LayoutParams(-1, -2));

        buildHeader();
        buildQuickNav();
        buildStatus();

        settingsSection = card("1. Параметры станка");
        buildSettings(settingsSection);
        root.addView(settingsSection, lp(-1, -2, 0, 0, 0, 8));

        orderSection = card("2. Заказ, компоновка и расчет");
        buildOrder(orderSection);
        root.addView(orderSection, lp(-1, -2, 0, 0, 0, 8));

        previewSection = card("3. Preview 2D и группы CNC");
        buildPreview(previewSection);
        root.addView(previewSection, lp(-1, -2, 0, 0, 0, 8));

        gcodeSection = card("4. G-code текущей группы");
        buildGcode(gcodeSection);
        root.addView(gcodeSection, lp(-1, -2, 0, 0, 0, 8));

        checkSection = card("5. Проверка CNC, экспорт и отправка");
        buildCheckAndExport(checkSection);
        root.addView(checkSection, lp(-1, -2, 0, 0, 0, 8));

        setContentView(scrollView);
    }

    private void buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        header.setBackground(background(DARK, dp(8), Color.rgb(185, 195, 210), 1));
        root.addView(header, lp(-1, -2, 0, 0, 0, 7));

        TextView title = text("FILATO CAM PRO CNC", 22, Color.WHITE, true);
        title.setLetterSpacing(0.03f);
        header.addView(title);
        header.addView(text("Android V9 | адаптивный экран | портрет + альбом | без микрофона | без 3D", 12, Color.rgb(210, 218, 230), false));
    }

    private void buildQuickNav() {
        GridLayout nav = new GridLayout(this);
        nav.setColumnCount(3);
        addNav(nav, "Параметры", new View.OnClickListener() { @Override public void onClick(View v) { scrollTo(settingsSection); }});
        addNav(nav, "Заказ", new View.OnClickListener() { @Override public void onClick(View v) { scrollTo(orderSection); }});
        addNav(nav, "Preview", new View.OnClickListener() { @Override public void onClick(View v) { scrollTo(previewSection); }});
        addNav(nav, "G-code", new View.OnClickListener() { @Override public void onClick(View v) { scrollTo(gcodeSection); }});
        addNav(nav, "Проверка", new View.OnClickListener() { @Override public void onClick(View v) { scrollTo(checkSection); }});
        addNav(nav, "Рассчитать", new View.OnClickListener() { @Override public void onClick(View v) { calculate(); }});
        root.addView(nav, lp(-1, -2, 0, 0, 0, 7));
    }

    private void buildStatus() {
        statusView = text("Готово. Интерфейс больше не растягивается по X: все блоки идут вертикально и вписываются в экран.", 12, Color.rgb(35, 40, 48), false);
        statusView.setPadding(dp(8), dp(6), dp(8), dp(6));
        statusView.setBackground(background(Color.WHITE, dp(7), LINE, 1));
        root.addView(statusView, lp(-1, -2, 0, 0, 0, 7));
    }

    private void buildSettings(LinearLayout parent) {
        TextView warn = text("Голосовой ввод временно отключен. Добавим позже отдельным модулем, без микрофона клавиатуры.", 12, Color.rgb(120, 70, 0), false);
        warn.setPadding(dp(7), dp(7), dp(7), dp(7));
        warn.setBackground(background(WARN_BG, dp(7), Color.rgb(230, 190, 110), 1));
        parent.addView(warn, lp(-1, -2, 0, 0, 0, 6));

        LinearLayout buttons = rowWrap();
        buttons.addView(actionButton(paramsLocked ? "Разблокировать параметры" : "Заблокировать параметры", BLUE, Color.WHITE, new View.OnClickListener() { @Override public void onClick(View v) { toggleParams(); }}));
        buttons.addView(actionButton("Сбросить", Color.WHITE, RED, new View.OnClickListener() { @Override public void onClick(View v) { settings = Settings.defaults(); fillSettingsBoxes(); }}));
        parent.addView(buttons);

        settingBoxes.clear();
        addSetting(parent, "sheetWidth", "Ширина листа X", settings.sheetWidth);
        addSetting(parent, "sheetHeight", "Высота листа Y", settings.sheetHeight);
        addSetting(parent, "thickness", "Толщина фанеры", settings.thickness);
        addSetting(parent, "safeZ", "Safe Z", settings.safeZ);
        addSetting(parent, "edgeSkim", "Кромочный съем", settings.edgeSkim);
        addSetting(parent, "actualOffset", "Факт. ширина = номинал -", settings.actualOffset);
        addSetting(parent, "kerf", "Между деталями", settings.kerf);
        addSetting(parent, "depthPerPass", "Глубина прохода", settings.depthPerPass);
        addSetting(parent, "t4Feed", "T4 подача", settings.t4Feed);
        addSetting(parent, "t4Plunge", "T4 врезание", settings.t4Plunge);
        addSetting(parent, "t4Rpm", "T4 обороты", settings.t4Rpm);
        addSetting(parent, "t5Plunge", "T5 врезание", settings.t5Plunge);
        addSetting(parent, "t5Rpm", "T5 обороты", settings.t5Rpm);

        autoDepthBox = new CheckBox(this);
        autoDepthBox.setText("Авто: один проход на толщину фанеры");
        autoDepthBox.setTextSize(12);
        autoDepthBox.setChecked(true);
        parent.addView(autoDepthBox);
        applyParamLock();
    }

    private void buildOrder(LinearLayout parent) {
        rowEdit(parent, "Имя заказа", orderNameBox = edit(orderName));

        safetyBox = new CheckBox(this);
        safetyBox.setText("Я понимаю: CNC-геометрия Android пока тестовая до ArtCAM-пресетов");
        safetyBox.setTextSize(12);
        safetyBox.setChecked(safetyAccepted);
        safetyBox.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { safetyAccepted = safetyBox.isChecked(); }});
        parent.addView(safetyBox);

        parent.addView(label("Текст заказа / будущая загрузка голосовой записи"));
        orderTextBox = edit("два по триста ещё три по четыреста пять по пятьсот");
        orderTextBox.setMinLines(3);
        orderTextBox.setGravity(Gravity.TOP);
        parent.addView(orderTextBox, lp(-1, dp(105), 0, 0, 0, 6));

        LinearLayout textButtons = rowWrap();
        textButtons.addView(actionButton("Разобрать текст", GREEN, Color.WHITE, new View.OnClickListener() { @Override public void onClick(View v) { parseOrderTextBox(); }}));
        textButtons.addView(actionButton("Пример", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { fillExample(); }}));
        textButtons.addView(actionButton("Очистить текст", Color.WHITE, RED, new View.OnClickListener() { @Override public void onClick(View v) { orderTextBox.setText(""); }}));
        parent.addView(textButtons);

        parent.addView(label("Таблица количества по размерам, мм"));
        buildQuantityGrid(parent);

        LinearLayout mainButtons = rowWrap();
        mainButtons.addView(actionButton("Рассчитать", GREEN, Color.WHITE, new View.OnClickListener() { @Override public void onClick(View v) { calculate(); }}));
        mainButtons.addView(actionButton("Сохранить заказ", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { saveOrder(); }}));
        mainButtons.addView(actionButton("Загрузить", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { loadOrder(); }}));
        mainButtons.addView(actionButton("Очистить заказ", Color.WHITE, RED, new View.OnClickListener() { @Override public void onClick(View v) { clearOrder(); }}));
        parent.addView(mainButtons);

        progressText = text("Прогресс: ожидание", 12, INK, false);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        parent.addView(progressText);
        parent.addView(progressBar, lp(-1, dp(14), 0, 3, 0, 6));

        logView = text("Журнал будет здесь.", 12, Color.DKGRAY, false);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(dp(7), dp(7), dp(7), dp(7));
        logView.setBackground(background(Color.WHITE, dp(6), LINE, 1));
        parent.addView(logView, lp(-1, dp(95), 0, 0, 0, 0));
    }

    private void buildQuantityGrid(LinearLayout parent) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        quantityBoxes.clear();

        for (int size : sizes) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.HORIZONTAL);
            cell.setGravity(Gravity.CENTER_VERTICAL);
            cell.setPadding(dp(5), dp(3), dp(5), dp(3));
            cell.setBackground(background(Color.WHITE, dp(6), LINE, 1));

            TextView label = text(String.valueOf(size), 12, INK, true);
            EditText edit = edit(String.valueOf(order.containsKey(size) ? order.get(size) : 0));
            edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            edit.setGravity(Gravity.CENTER);
            edit.setSingleLine(true);
            quantityBoxes.put(size, edit);

            cell.addView(label, new LinearLayout.LayoutParams(0, dp(38), 1));
            cell.addView(edit, new LinearLayout.LayoutParams(dp(58), dp(38)));

            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 0;
            gp.height = dp(48);
            gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            gp.setMargins(0, 0, dp(5), dp(5));
            grid.addView(cell, gp);
        }
        parent.addView(grid, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildPreview(LinearLayout parent) {
        LinearLayout buttons = rowWrap();
        buttons.addView(actionButton("← группа", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { selectGroup(-1); }}));
        buttons.addView(actionButton("группа →", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { selectGroup(1); }}));
        buttons.addView(actionButton("− zoom", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { zoom2d = Math.max(0.5, zoom2d - 0.1); refreshViews(); }}));
        buttons.addView(actionButton("+ zoom", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { zoom2d = Math.min(2.0, zoom2d + 0.1); refreshViews(); }}));
        parent.addView(buttons);

        showCoordsBox = new CheckBox(this);
        showCoordsBox.setText("Показывать координаты и отверстия");
        showCoordsBox.setTextSize(12);
        showCoordsBox.setChecked(true);
        showCoordsBox.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { refreshViews(); }});
        parent.addView(showCoordsBox);

        groupsView = text("Нет расчета.", 12, INK, false);
        groupsView.setTypeface(Typeface.MONOSPACE);
        groupsView.setPadding(dp(7), dp(7), dp(7), dp(7));
        groupsView.setBackground(background(Color.WHITE, dp(6), LINE, 1));
        parent.addView(groupsView, lp(-1, dp(145), 0, 0, 0, 6));

        preview2d = new Preview2DView(this);
        preview2d.setBackgroundColor(Color.WHITE);
        parent.addView(preview2d, lp(-1, dp(360), 0, 0, 0, 0));
    }

    private void buildGcode(LinearLayout parent) {
        LinearLayout buttons = rowWrap();
        buttons.addView(actionButton("Сохранить CNC", BLUE, Color.WHITE, new View.OnClickListener() { @Override public void onClick(View v) { saveCnc(); }}));
        buttons.addView(actionButton("Копировать", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { copyCnc(); }}));
        buttons.addView(actionButton("Поделиться текстом", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { shareCurrentCncText(); }}));
        parent.addView(buttons);

        cncView = text("CNC появится после расчета.", 10, Color.rgb(20, 30, 40), false);
        cncView.setTypeface(Typeface.MONOSPACE);
        cncView.setTextIsSelectable(true);
        cncView.setPadding(dp(7), dp(7), dp(7), dp(7));
        cncView.setBackground(background(Color.WHITE, dp(6), LINE, 1));
        parent.addView(cncView, lp(-1, dp(360), 0, 6, 0, 0));
    }

    private void buildCheckAndExport(LinearLayout parent) {
        LinearLayout buttons = rowWrap();
        buttons.addView(actionButton("Проверить CNC", BLUE, Color.WHITE, new View.OnClickListener() { @Override public void onClick(View v) { refreshCheck(); }}));
        buttons.addView(actionButton("Создать ZIP заказа", GREEN, Color.WHITE, new View.OnClickListener() { @Override public void onClick(View v) { exportZip(); }}));
        buttons.addView(actionButton("Поделиться CNC", Color.WHITE, INK, new View.OnClickListener() { @Override public void onClick(View v) { shareCurrentCncText(); }}));
        parent.addView(buttons);

        TextView note = text("Android не дает приложению напрямую открыть произвольную папку без выбора пользователя. Поэтому кнопка ZIP создает папку заказа внутри архива: CNC, SUMMARY и .filato-order. После сохранения его можно отправить через меню поделиться.", 12, Color.DKGRAY, false);
        note.setPadding(dp(7), dp(7), dp(7), dp(7));
        note.setBackground(background(WARN_BG, dp(6), Color.rgb(230, 190, 110), 1));
        parent.addView(note, lp(-1, -2, 0, 6, 0, 6));

        checkView = text("Проверка появится после расчета.", 12, INK, false);
        checkView.setTypeface(Typeface.MONOSPACE);
        checkView.setTextIsSelectable(true);
        checkView.setPadding(dp(7), dp(7), dp(7), dp(7));
        checkView.setBackground(background(Color.WHITE, dp(6), LINE, 1));
        parent.addView(checkView, lp(-1, dp(260), 0, 0, 0, 0));
    }

    private void calculate() {
        captureOrder();
        captureSettings();
        if (!safetyAccepted) {
            toast("Поставь галочку безопасности.");
            scrollTo(orderSection);
            return;
        }
        result = Optimizer.optimize(order, settings);
        groupIndex = 0;
        if (progressBar != null) progressBar.setProgress(result.sheets.isEmpty() ? 0 : 100);
        if (progressText != null) progressText.setText(result.sheets.isEmpty() ? "Прогресс: заказ пустой" : "Прогресс: 100% | готово");
        if (result.sheets.isEmpty()) {
            status("Заказ пустой.");
            log("Расчет не выполнен: заказ пустой.");
        } else {
            status("Расчет готов: листов " + result.sheets.size() + ", групп " + result.groups.size());
            log("Расчет: листов " + result.sheets.size() + ", групп " + result.groups.size());
        }
        refreshViews();
        scrollTo(previewSection);
    }

    private void parseOrderTextBox() {
        String source = orderTextBox == null ? "" : orderTextBox.getText().toString();
        LinkedHashMap<Integer, Integer> parsed = OrderParser.parse(source);
        if (parsed.isEmpty()) {
            toast("Не разобрал заказ. Пример: два по триста ещё три по четыреста пять по пятьсот");
            log("Не разобрал текст: " + source);
            return;
        }
        for (int size : sizes) {
            int q = parsed.containsKey(size) ? parsed.get(size) : 0;
            order.put(size, q);
            EditText box = quantityBoxes.get(size);
            if (box != null) box.setText(String.valueOf(q));
        }
        int total = 0;
        for (Map.Entry<Integer, Integer> entry : parsed.entrySet()) total += entry.getValue();
        status("Текст заказа разобран: " + total + " деталей");
        log("Разобран текст: " + parsed);
    }

    private void fillExample() {
        if (orderTextBox != null) {
            orderTextBox.setText("десять листов на триста пять листов на шестьсот семь листов на семьсот пятьдесят двадцать семь листов на тысяча двести десять листов на тысяча сто");
        }
        parseOrderTextBox();
    }

    private void clearOrder() {
        for (int size : sizes) {
            order.put(size, 0);
            EditText box = quantityBoxes.get(size);
            if (box != null) box.setText("0");
        }
        result = new OptimizerResult();
        groupIndex = 0;
        refreshViews();
        status("Заказ очищен.");
    }

    private void refreshViews() {
        if (groupsView != null) groupsView.setText(groupsSummary());
        if (cncView != null) cncView.setText(currentCncNumbered());
        if (preview2d != null) {
            preview2d.setData(currentGroup(), settings, zoom2d, showCoordsBox == null || showCoordsBox.isChecked());
            preview2d.invalidate();
        }
        refreshCheck();
    }

    private void refreshCheck() {
        if (checkView == null) return;
        SheetGroup group = currentGroup();
        if (group == null) {
            checkView.setText("Нет CNC для проверки.");
            return;
        }
        CheckResult check = CncAnalyzer.check(CncGenerator.generate(group, settings), settings);
        StringBuilder builder = new StringBuilder();
        builder.append("Проверка группы ").append(groupIndex + 1).append("/").append(result.groups.size()).append("\n\n");
        builder.append("X ").append(FormatUtil.fmt(check.minX)).append(" .. ").append(FormatUtil.fmt(check.maxX)).append('\n');
        builder.append("Y ").append(FormatUtil.fmt(check.minY)).append(" .. ").append(FormatUtil.fmt(check.maxY)).append('\n');
        builder.append("Z ").append(FormatUtil.fmt(check.minZ)).append(" .. ").append(FormatUtil.fmt(check.maxZ)).append("\n\n");
        if (check.errors.isEmpty()) builder.append("Ошибки: нет\n");
        else {
            builder.append("Ошибки:\n");
            for (String error : check.errors) builder.append("- ").append(error).append('\n');
        }
        if (check.warnings.isEmpty()) builder.append("Предупреждения: нет\n");
        else {
            builder.append("Предупреждения:\n");
            for (String warning : check.warnings) builder.append("- ").append(warning).append('\n');
        }
        checkView.setText(builder.toString());
    }

    private String groupsSummary() {
        if (result.groups.isEmpty()) return "Нет расчета. Нажми Рассчитать.";
        StringBuilder builder = new StringBuilder();
        builder.append("Заказ: ").append(orderName).append('\n');
        builder.append("Листов: ").append(result.sheets.size()).append(" | групп: ").append(result.groups.size()).append("\n\n");
        for (int i = 0; i < result.groups.size(); i++) {
            SheetGroup group = result.groups.get(i);
            builder.append(i == groupIndex ? "> " : "  ");
            builder.append("Группа ").append(i + 1).append(": x").append(group.count).append(" | ").append(group.key).append(" | листы ").append(group.sheetNumbers).append('\n');
        }
        SheetGroup current = currentGroup();
        if (current != null) builder.append("\nИспользовано X: ").append(FormatUtil.fmt(current.sample.usedWidth)).append(" / ").append(FormatUtil.fmt(settings.sheetWidth)).append(" мм");
        return builder.toString();
    }

    private String currentCncNumbered() {
        SheetGroup group = currentGroup();
        if (group == null) return "CNC появится после расчета.";
        String[] lines = CncGenerator.generate(group, settings).split("\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) builder.append(String.format(Locale.US, "%04d  %s\n", i + 1, lines[i]));
        return builder.toString();
    }

    private String currentCncRaw() {
        SheetGroup group = currentGroup();
        return group == null ? "" : CncGenerator.generate(group, settings);
    }

    private SheetGroup currentGroup() {
        if (result.groups.isEmpty()) return null;
        if (groupIndex < 0) groupIndex = 0;
        if (groupIndex >= result.groups.size()) groupIndex = result.groups.size() - 1;
        return result.groups.get(groupIndex);
    }

    private void selectGroup(int delta) {
        if (result.groups.isEmpty()) return;
        groupIndex += delta;
        if (groupIndex < 0) groupIndex = result.groups.size() - 1;
        if (groupIndex >= result.groups.size()) groupIndex = 0;
        refreshViews();
    }

    private void captureOrder() {
        orderName = orderNameBox == null ? orderName : orderNameBox.getText().toString().trim();
        if (orderName.length() == 0) orderName = "ORDER_001";
        for (int size : sizes) {
            EditText box = quantityBoxes.get(size);
            int quantity = readInt(box, order.containsKey(size) ? order.get(size) : 0);
            order.put(size, Math.max(0, quantity));
        }
        if (safetyBox != null) safetyAccepted = safetyBox.isChecked();
    }

    private void captureSettings() {
        for (Map.Entry<String, EditText> entry : settingBoxes.entrySet()) settings.set(entry.getKey(), readDouble(entry.getValue(), settings.get(entry.getKey())));
        if (autoDepthBox != null && autoDepthBox.isChecked()) settings.depthPerPass = settings.thickness;
    }

    private void fillSettingsBoxes() {
        for (Map.Entry<String, EditText> entry : settingBoxes.entrySet()) entry.getValue().setText(FormatUtil.fmt(settings.get(entry.getKey())));
    }

    private void toggleParams() {
        if (!paramsLocked) {
            paramsLocked = true;
            applyParamLock();
            return;
        }
        final EditText code = edit("");
        code.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("Код разблокировки")
                .setMessage("Введите 777")
                .setView(code)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        if ("777".equals(code.getText().toString().trim())) {
                            paramsLocked = false;
                            applyParamLock();
                        } else toast("Неверный код");
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void applyParamLock() {
        for (EditText edit : settingBoxes.values()) edit.setEnabled(!paramsLocked);
        status(paramsLocked ? "Параметры заблокированы." : "Параметры разблокированы.");
    }

    private void saveOrder() {
        captureOrder();
        captureSettings();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, orderName + ".filato-order");
        startActivityForResult(intent, REQ_SAVE_ORDER);
    }

    private void loadOrder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_LOAD_ORDER);
    }

    private void saveCnc() {
        if (currentGroup() == null) {
            toast("Нет CNC.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, orderName + "_GROUP_" + (groupIndex + 1) + ".cnc");
        startActivityForResult(intent, REQ_SAVE_CNC);
    }

    private void exportZip() {
        if (result.groups.isEmpty()) {
            toast("Нет групп для ZIP.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, orderName + "_CNC.zip");
        startActivityForResult(intent, REQ_EXPORT_ZIP);
    }

    private void copyCnc() {
        String cnc = currentCncRaw();
        if (cnc.length() == 0) {
            toast("Нет CNC.");
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Filato CNC", cnc));
        toast("CNC скопирован.");
    }

    private void shareCurrentCncText() {
        String cnc = currentCncRaw();
        if (cnc.length() == 0) {
            toast("Нет CNC для отправки.");
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, orderName + " GROUP " + (groupIndex + 1));
        send.putExtra(Intent.EXTRA_TEXT, cnc);
        startActivity(Intent.createChooser(send, "Отправить CNC"));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                if (requestCode == REQ_SAVE_ORDER) writeText(uri, orderText());
                else if (requestCode == REQ_SAVE_CNC) writeText(uri, currentCncRaw());
                else if (requestCode == REQ_EXPORT_ZIP) writeZip(uri);
                else if (requestCode == REQ_LOAD_ORDER) parseSavedOrder(readText(uri));
            }
        } catch (Throwable throwable) {
            toast("Ошибка файла: " + throwable.getMessage());
        }
    }

    private String orderText() {
        StringBuilder builder = new StringBuilder();
        builder.append("FILATO_ORDER_V9\n");
        builder.append("name=").append(orderName).append('\n');
        for (Map.Entry<Integer, Integer> entry : order.entrySet()) {
            if (entry.getValue() > 0) builder.append("size.").append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        builder.append("settings.sheetWidth=").append(settings.sheetWidth).append('\n');
        builder.append("settings.sheetHeight=").append(settings.sheetHeight).append('\n');
        builder.append("settings.thickness=").append(settings.thickness).append('\n');
        builder.append("settings.safeZ=").append(settings.safeZ).append('\n');
        builder.append("settings.actualOffset=").append(settings.actualOffset).append('\n');
        builder.append("settings.kerf=").append(settings.kerf).append('\n');
        return builder.toString();
    }

    private void parseSavedOrder(String text) {
        clearOrder();
        String[] lines = text.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("name=")) {
                orderName = line.substring(5).trim();
                if (orderNameBox != null) orderNameBox.setText(orderName);
            } else if (line.startsWith("size.")) {
                int eq = line.indexOf('=');
                if (eq > 5) {
                    int size = Integer.parseInt(line.substring(5, eq));
                    int quantity = Integer.parseInt(line.substring(eq + 1));
                    order.put(size, quantity);
                    EditText box = quantityBoxes.get(size);
                    if (box != null) box.setText(String.valueOf(quantity));
                }
            } else if (line.startsWith("settings.")) {
                int eq = line.indexOf('=');
                if (eq > 9) settings.set(line.substring(9, eq), Double.parseDouble(line.substring(eq + 1)));
            }
        }
        fillSettingsBoxes();
        status("Заказ загружен.");
    }

    private String readText(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) builder.append(line).append('\n');
        reader.close();
        return builder.toString();
    }

    private void writeText(Uri uri, String text) throws Exception {
        OutputStream output = getContentResolver().openOutputStream(uri);
        output.write(text.getBytes(StandardCharsets.UTF_8));
        output.close();
        toast("Файл сохранен.");
    }

    private void writeZip(Uri uri) throws Exception {
        OutputStream output = getContentResolver().openOutputStream(uri);
        ZipOutputStream zip = new ZipOutputStream(output);
        for (int i = 0; i < result.groups.size(); i++) {
            SheetGroup group = result.groups.get(i);
            addZip(zip, orderName + "/CNC/" + orderName + "_GROUP_" + (i + 1) + "_x" + group.count + ".cnc", CncGenerator.generate(group, settings));
        }
        addZip(zip, orderName + "/SUMMARY.txt", groupsSummary());
        addZip(zip, orderName + "/" + orderName + ".filato-order", orderText());
        zip.close();
        output.close();
        toast("ZIP заказа сохранен.");
    }

    private void addZip(ZipOutputStream zip, String name, String text) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void addSetting(LinearLayout parent, String key, String title, double value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));
        TextView label = text(title, 12, INK, false);
        EditText edit = edit(FormatUtil.fmt(value));
        edit.setSingleLine(true);
        edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        settingBoxes.put(key, edit);
        row.addView(label, new LinearLayout.LayoutParams(0, dp(40), 1));
        row.addView(edit, new LinearLayout.LayoutParams(dp(110), dp(40)));
        parent.addView(row);
    }

    private void rowEdit(LinearLayout parent, String title, EditText edit) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(title, 12, INK, true), new LinearLayout.LayoutParams(dp(105), dp(42)));
        row.addView(edit, new LinearLayout.LayoutParams(0, dp(42), 1));
        parent.addView(row);
    }

    private void addNav(GridLayout nav, String title, View.OnClickListener listener) {
        Button button = actionButton(title, Color.WHITE, INK, listener);
        GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
        gp.width = 0;
        gp.height = dp(40);
        gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        gp.setMargins(0, 0, dp(5), dp(5));
        nav.addView(button, gp);
    }

    private LinearLayout card(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(background(CARD, dp(8), LINE, 1));
        TextView header = text(title, 17, INK, true);
        card.addView(header, lp(-1, -2, 0, 0, 0, 6));
        return card;
    }

    private LinearLayout rowWrap() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBaselineAligned(false);
        return row;
    }

    private TextView label(String value) {
        TextView label = text(value, 13, INK, true);
        label.setPadding(0, dp(8), 0, dp(3));
        return label;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private EditText edit(String value) {
        EditText edit = new EditText(this);
        edit.setText(value);
        edit.setTextSize(12);
        edit.setPadding(dp(5), 0, dp(5), 0);
        edit.setBackground(background(Color.WHITE, dp(5), LINE, 1));
        return edit;
    }

    private Button actionButton(String title, int bgColor, int textColor, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setMinHeight(dp(36));
        button.setPadding(dp(5), 0, dp(5), 0);
        button.setBackground(background(bgColor, dp(6), LINE, bgColor == Color.WHITE ? 1 : 0));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = lp(-1, dp(38), 0, 0, 0, 5);
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout.LayoutParams lp(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private GradientDrawable background(int color, int radius, int strokeColor, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke > 0) drawable.setStroke(stroke, strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int readInt(EditText edit, int fallback) {
        try {
            if (edit == null) return fallback;
            return Integer.parseInt(edit.getText().toString().trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private double readDouble(EditText edit, double fallback) {
        try {
            if (edit == null) return fallback;
            return Double.parseDouble(edit.getText().toString().trim().replace(',', '.'));
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private void scrollTo(final View target) {
        if (target == null) return;
        scrollView.post(new Runnable() {
            @Override public void run() { scrollView.smoothScrollTo(0, target.getTop()); }
        });
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }

    private void status(String value) {
        if (statusView != null) statusView.setText(value);
    }

    private void log(String value) {
        if (logView != null) logView.setText(time() + "  " + value + "\n" + logView.getText());
    }

    private String time() {
        return new java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(new java.util.Date());
    }
}
