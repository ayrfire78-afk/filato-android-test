package com.filato.campro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

public class Preview2DView extends View {
    private static final int INK = Color.rgb(35, 55, 86);
    private static final int BLUE = Color.rgb(38, 91, 161);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private SheetGroup group;
    private Settings settings = Settings.defaults();
    private double zoom = 1.0;
    private boolean showCoordinates = true;

    public Preview2DView(Context context) {
        super(context);
        paint.setTypeface(Typeface.DEFAULT);
        setBackgroundColor(Color.WHITE);
    }

    public void setData(SheetGroup group, Settings settings, double zoom, boolean showCoordinates) {
        this.group = group;
        this.settings = settings == null ? Settings.defaults() : settings;
        this.zoom = zoom;
        this.showCoordinates = showCoordinates;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(250, 252, 255));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        float pad = 28;
        float scale = (float) Math.min((getWidth() - pad * 2) / settings.sheetWidth,
                (getHeight() - pad * 2) / settings.sheetHeight) * (float) zoom;
        float originX = pad;
        float originY = pad;

        drawSheet(canvas, originX, originY, scale);

        if (group == null || group.sample == null) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.DKGRAY);
            paint.setTextSize(28);
            canvas.drawText("Нет расчета", pad, Math.max(70, getHeight() / 2), paint);
            return;
        }

        for (Part part : group.sample.parts) drawPart(canvas, part, originX, originY, scale);
    }

    private void drawSheet(Canvas canvas, float originX, float originY, float scale) {
        float width = (float) (settings.sheetWidth * scale);
        float height = (float) (settings.sheetHeight * scale);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(238, 232, 205));
        canvas.drawRect(originX, originY, originX + width, originY + height, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(INK);
        canvas.drawRect(originX, originY, originX + width, originY + height, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(18);
        paint.setColor(INK);
        canvas.drawText("Лист " + FormatUtil.cnc(settings.sheetWidth) + " × " + FormatUtil.cnc(settings.sheetHeight),
                originX, originY - 7, paint);
    }

    private void drawPart(Canvas canvas, Part part, float originX, float originY, float scale) {
        float x = originX + (float) (part.x * scale);
        float y = originY;
        float width = (float) (part.actualWidth * scale);
        float height = (float) (settings.sheetHeight * scale);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(65, 45, 110, 190));
        canvas.drawRect(x, y, x + width, y + height, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(BLUE);
        canvas.drawRect(x, y, x + width, y + height, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        paint.setTextSize(17);
        canvas.drawText(part.nominalWidth + " / факт " + FormatUtil.cnc(part.actualWidth), x + 5, y + 23, paint);

        if (showCoordinates) drawDrillPoints(canvas, part, originX, originY, scale);
    }

    private void drawDrillPoints(Canvas canvas, Part part, float originX, float originY, float scale) {
        float x = originX + (float) (part.x * scale);
        float width = (float) (part.actualWidth * scale);
        double[] ys = {89, 560, 1486, 2112, 2712, 2911};

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(60, 90, 60));
        for (double yy : ys) {
            float cy = originY + (float) (yy * scale);
            float r = Math.max(3, 5 * scale);
            canvas.drawCircle(x + (float) (21 * scale), cy, r, paint);
            canvas.drawCircle(x + width - (float) (21 * scale), cy, r, paint);
        }
    }
}
