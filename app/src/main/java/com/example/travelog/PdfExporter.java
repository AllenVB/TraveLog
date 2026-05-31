package com.example.travelog;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Anı detaylarını Android PdfDocument ile PDF olarak dışa aktarır.
 * Harici kütüphane gerektirmez.
 */
public class PdfExporter {

    private static final int PAGE_W = 595; // A4 @ 72dpi
    private static final int PAGE_H = 842;
    private static final int MARGIN = 48;

    /** PDF oluştur, MediaStore'a kaydet. Başarıda URI döner, hata halinde null. */
    public static Uri export(Context ctx, Memory memory) {
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create();
        PdfDocument.Page page = pdf.startPage(info);
        Canvas c = page.getCanvas();

        // ── Renkler & Fırtçalar ──────────────────────────────────────────────
        Paint pTitle  = makePaint(Color.parseColor("#1565C0"), 22, Paint.Align.LEFT, true);
        Paint pSub    = makePaint(Color.parseColor("#039BE5"), 13, Paint.Align.LEFT, false);
        Paint pBody   = makePaint(Color.parseColor("#1A1A2E"), 12, Paint.Align.LEFT, false);
        Paint pSmall  = makePaint(Color.parseColor("#6B7280"), 10, Paint.Align.LEFT, false);
        Paint pLine   = new Paint();
        pLine.setColor(Color.parseColor("#E0E0E0"));
        pLine.setStrokeWidth(1);

        int y = MARGIN;

        // ── Üst bant ────────────────────────────────────────────────────────
        Paint pBand = new Paint();
        pBand.setColor(Color.parseColor("#1565C0"));
        c.drawRect(0, 0, PAGE_W, 56, pBand);
        Paint pBrandTxt = makePaint(Color.WHITE, 18, Paint.Align.LEFT, true);
        c.drawText("✈ TraveLog", MARGIN, 36, pBrandTxt);
        y = 80;

        // ── Kapak fotoğrafı ──────────────────────────────────────────────────
        if (memory.imageUri != null && !memory.imageUri.isEmpty()) {
            try {
                InputStream is = ctx.getContentResolver().openInputStream(
                        Uri.parse(memory.imageUri));
                if (is != null) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    if (bmp != null) {
                        int imgW = PAGE_W - 2 * MARGIN;
                        int imgH = (int) (bmp.getHeight() * (imgW / (float) bmp.getWidth()));
                        imgH = Math.min(imgH, 200);
                        Bitmap scaled = Bitmap.createScaledBitmap(bmp, imgW, imgH, true);
                        c.drawBitmap(scaled, MARGIN, y, null);
                        y += imgH + 14;
                    }
                }
            } catch (IOException ignored) {}
        }

        // ── Başlık ───────────────────────────────────────────────────────────
        c.drawText(memory.title != null ? memory.title : "", MARGIN, y += 26, pTitle);
        c.drawLine(MARGIN, y + 6, PAGE_W - MARGIN, y + 6, pLine);
        y += 20;

        // ── Meta bilgileri ────────────────────────────────────────────────────
        c.drawText("📍 " + nvl(memory.city), MARGIN, y += 18, pSub);
        c.drawText("📅 " + nvl(memory.date), MARGIN + 200, y, pSub);
        if (memory.weather != null && !memory.weather.isEmpty())
            c.drawText("🌤 " + memory.weather, MARGIN, y += 16, pSmall);

        c.drawLine(MARGIN, y + 10, PAGE_W - MARGIN, y + 10, pLine);
        y += 24;

        // ── Açıklama (kelime kaydırma) ────────────────────────────────────────
        if (memory.description != null) {
            y = drawWrappedText(c, memory.description, pBody, MARGIN, y, PAGE_W - MARGIN);
        }

        // ── Alt bilgi ────────────────────────────────────────────────────────
        c.drawLine(MARGIN, PAGE_H - 36, PAGE_W - MARGIN, PAGE_H - 36, pLine);
        Paint pFooter = makePaint(Color.parseColor("#9E9E9E"), 9, Paint.Align.CENTER, false);
        c.drawText("TraveLog ile oluşturuldu", PAGE_W / 2f, PAGE_H - 20, pFooter);

        pdf.finishPage(page);

        // ── Kaydet (MediaStore API 29+ veya eski yol) ─────────────────────────
        Uri uri = savePdf(ctx, pdf, memory.title);
        pdf.close();
        return uri;
    }

    // ── Yardımcı metotlar ─────────────────────────────────────────────────────

    private static int drawWrappedText(Canvas c, String text, Paint p,
                                       int x, int y, int maxX) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (p.measureText(test) > maxX - x) {
                c.drawText(line.toString(), x, y += 18, p);
                line = new StringBuilder(word);
                if (y > PAGE_H - 60) break; // sayfa taşmasını önle
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) c.drawText(line.toString(), x, y += 18, p);
        return y;
    }

    private static Paint makePaint(int color, int size, Paint.Align align, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setTextAlign(align);
        if (bold) p.setFakeBoldText(true);
        return p;
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    private static Uri savePdf(Context ctx, PdfDocument pdf, String title) {
        String filename = "TraveLog_" + (title != null ? title.replaceAll("[^a-zA-Z0-9]", "_") : "ani") + ".pdf";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                cv.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = ctx.getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri != null) {
                    try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                        pdf.writeTo(os);
                    }
                    return uri;
                }
            } else {
                java.io.File dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(dir, filename);
                try (OutputStream os = new java.io.FileOutputStream(file)) {
                    pdf.writeTo(os);
                }
                return Uri.fromFile(file);
            }
        } catch (IOException ignored) {}
        return null;
    }
}
