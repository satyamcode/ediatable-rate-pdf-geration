package com.ssmart.pricetag.controller;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
public class PdfController {

    private static final int MAX_COLS   = 3;
    private static final int MAX_ROWS   = 9;
    private static final int MAX_BLOCKS = MAX_COLS * MAX_ROWS; // 27

    private static final DeviceRgb NAVY    = new DeviceRgb(0, 51, 102);
    private static final DeviceRgb BLACK   = new DeviceRgb(20, 20, 20);
    private static final DeviceRgb CUT_CLR = new DeviceRgb(140, 140, 140); // gray dashed cut lines

    // ── Page geometry ────────────────────────────────────────────────────
    // A4 = 595 × 842 pt
    private static final float PAGE_W  = PageSize.A4.getWidth();   // 595
    private static final float PAGE_H  = PageSize.A4.getHeight();  // 842
    private static final float MARGIN  = 24f;   // doubled from 12
    private static final float COL_GAP =  6f;
    private static final float ROW_GAP =  5f;

    // Derived: label width & height that perfectly tile the A4 page
    private static final float LABEL_W = (PAGE_W - 2*MARGIN - (MAX_COLS-1)*COL_GAP) / MAX_COLS; // ≈187
    private static final float LABEL_H = (PAGE_H - 2*MARGIN - (MAX_ROWS-1)*ROW_GAP) / MAX_ROWS; // ≈86.4

    private static final float CORNER_R = 7f;   // rounded corner radius
    private static final float BORDER_W = 1.8f; // border line width

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("maxBlocks", MAX_BLOCKS);
        return "index";
    }

    @PostMapping("/generate-pdf")
    public void generatePdf(
            @RequestParam("blockCount") int blockCount,
            @RequestParam("prices") List<String> prices,
            @RequestParam(value = "labelType", defaultValue = "PRICE") String labelType,
            HttpServletResponse response) throws IOException {

        blockCount = Math.max(1, Math.min(blockCount, MAX_BLOCKS));

        // Filename reflects type
        String filename = switch (labelType) {
            case "AMOUNT_OFF"  -> "SS_Mart_Amount_OFF_Labels.pdf";
            case "PERCENT_OFF" -> "SS_Mart_Percent_OFF_Labels.pdf";
            default            -> "SS_Mart_Price_Labels.pdf";
        };

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        PdfWriter   writer   = new PdfWriter(response.getOutputStream());
        PdfDocument pdfDoc   = new PdfDocument(writer);
        Document    document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(0, 0, 0, 0);

        PdfPage   page      = pdfDoc.addNewPage(PageSize.A4);
        PdfCanvas pdfCanvas = new PdfCanvas(page);

        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // 1 ─ Dashed cut guidelines
        drawCutLines(pdfCanvas);

        // 2 ─ Draw each selected label
        int totalRows = (int) Math.ceil((double) blockCount / MAX_COLS);
        for (int row = 0; row < totalRows; row++) {
            for (int col = 0; col < MAX_COLS; col++) {
                int idx = row * MAX_COLS + col;
                if (idx >= blockCount) continue;

                String price = (idx < prices.size()) ? prices.get(idx).trim() : "0";
                float  x     = MARGIN + col * (LABEL_W + COL_GAP);
                float  y     = PAGE_H - MARGIN - (row + 1) * LABEL_H - row * ROW_GAP;

                drawLabel(pdfCanvas, boldFont, x, y, price, labelType);
            }
        }

        document.close();
    }

    // ── Drawing helpers ───────────────────────────────────────────────────

    /**
     * Draws one label card.
     * labelType controls the text format:
     *   PRICE       → "SPECIAL PRICE"  /  "Rs 100/-"   /  "SS MART"
     *   AMOUNT_OFF  → "SPECIAL DISCOUNT" / "Rs 100 OFF" / "SS MART"
     *   PERCENT_OFF → "SPECIAL OFFER"  /  "50% OFF"    /  "SS MART"
     */
    private void drawLabel(PdfCanvas pdfCanvas, PdfFont boldFont,
                            float x, float y, String value, String labelType) throws IOException {

        String display = (value == null || value.isEmpty()) ? "0" : value;

        // Resolve text based on type
        String topText = switch (labelType) {
            case "AMOUNT_OFF"  -> "SPECIAL DISCOUNT";
            case "PERCENT_OFF" -> "SPECIAL OFFER";
            default            -> "SPECIAL PRICE";
        };
        String midText = switch (labelType) {
            case "AMOUNT_OFF"  -> "Rs " + display + " OFF";
            case "PERCENT_OFF" -> display + "% OFF";
            default            -> "Rs " + display + "/-";
        };

        // ── White fill ──────────────────────────────────────────────────
        pdfCanvas.saveState();
        pdfCanvas.setFillColor(ColorConstants.WHITE);
        pdfCanvas.roundRectangle(x, y, LABEL_W, LABEL_H, CORNER_R);
        pdfCanvas.fill();
        pdfCanvas.restoreState();

        // ── Black rounded border ────────────────────────────────────────
        pdfCanvas.saveState();
        pdfCanvas.setStrokeColor(BLACK);
        pdfCanvas.setLineWidth(BORDER_W);
        pdfCanvas.roundRectangle(x, y, LABEL_W, LABEL_H, CORNER_R);
        pdfCanvas.stroke();
        pdfCanvas.restoreState();

        // ── Text layout ─────────────────────────────────────────────────
        float pad = 5f;
        float iW  = LABEL_W - 2 * pad;
        float iH  = LABEL_H - 2 * pad;
        float iX  = x + pad;
        float iY  = y + pad;

        float topH = iH * 0.20f;
        float midH = iH * 0.60f;
        float botH = iH * 0.20f;

        Table inner = new Table(new float[]{iW}).setWidth(iW);

        // Top: label type text
        inner.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setHeight(topH).setVerticalAlignment(VerticalAlignment.BOTTOM)
                .setTextAlignment(TextAlignment.CENTER).setPadding(0)
                .add(new Paragraph(topText)
                        .setFont(boldFont).setFontSize(11f).setFontColor(BLACK)
                        .setCharacterSpacing(1.2f).setTextAlignment(TextAlignment.CENTER)
                        .setMargin(0).setPadding(0).setMultipliedLeading(1f)));

        // Middle: price / amount / percent
        inner.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setHeight(midH).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER).setPadding(0)
                .add(new Paragraph(midText)
                        .setFont(boldFont).setFontSize(34f).setFontColor(BLACK)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMargin(0).setPadding(0).setMultipliedLeading(1f)));

        // Bottom: SS MART
        inner.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setHeight(botH).setVerticalAlignment(VerticalAlignment.TOP)
                .setTextAlignment(TextAlignment.CENTER).setPadding(0)
                .add(new Paragraph("SS MART")
                        .setFont(boldFont).setFontSize(11f).setFontColor(BLACK)
                        .setCharacterSpacing(1.2f).setTextAlignment(TextAlignment.CENTER)
                        .setMargin(0).setPadding(0).setMultipliedLeading(1f)));

        try (Canvas canvas = new Canvas(pdfCanvas, new Rectangle(iX, iY, iW, iH))) {
            canvas.add(inner);
        }
    }

    /**
     * Draws dashed gray cut-guide lines across the full page
     * between every row and between every column.
     */
    private void drawCutLines(PdfCanvas pdfCanvas) {
        pdfCanvas.saveState();
        pdfCanvas.setStrokeColor(CUT_CLR);
        pdfCanvas.setLineWidth(0.5f);
        float[] dash = {4f, 4f};
        pdfCanvas.setLineDash(dash, 0f);

        // Horizontal cut lines (between rows)
        for (int r = 1; r < MAX_ROWS; r++) {
            float yLine = PAGE_H - MARGIN - r * LABEL_H - (r - 1) * ROW_GAP - ROW_GAP / 2f;
            pdfCanvas.moveTo(MARGIN / 2f, yLine)
                     .lineTo(PAGE_W - MARGIN / 2f, yLine);
        }

        // Vertical cut lines (between columns)
        for (int c = 1; c < MAX_COLS; c++) {
            float xLine = MARGIN + c * LABEL_W + (c - 1) * COL_GAP + COL_GAP / 2f;
            pdfCanvas.moveTo(xLine, MARGIN / 2f)
                     .lineTo(xLine, PAGE_H - MARGIN / 2f);
        }

        pdfCanvas.stroke();
        pdfCanvas.restoreState();
    }

    // ── Font helper ───────────────────────────────────────────────────────

    private PdfFont loadUnicodeFont(PdfFont fallback) {
        String[] candidates = {
                "/Library/Fonts/Arial Unicode.ttf",
                "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
                "/System/Library/Fonts/Supplemental/Georgia Bold.ttf",
                "/System/Library/Fonts/Supplemental/Verdana Bold.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf",
                "/System/Library/Fonts/Supplemental/Times New Roman Bold.ttf",
                "C:/Windows/Fonts/arialbd.ttf",
                "C:/Windows/Fonts/arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        };
        for (String path : candidates) {
            try {
                if (new File(path).exists()) {
                    PdfFont f = PdfFontFactory.createFont(path,
                            PdfEncodings.IDENTITY_H,
                            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                    if (f.containsGlyph('\u20B9')) {
                        System.out.println("[PDF] ₹ font: " + path);
                        return f;
                    }
                }
            } catch (Exception ignored) {}
        }
        return fallback;
    }
}
