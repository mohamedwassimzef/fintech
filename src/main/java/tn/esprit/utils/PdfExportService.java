package tn.esprit.utils;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import tn.esprit.entities.Complaint;
import tn.esprit.entities.Transaction;

import java.awt.Color;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportService {

    // ── Colour palette matching the app ──────────────────────────────────────
    private static final Color COL_BG        = new Color(15,  23,  42);   // #0f172a
    private static final Color COL_HEADER    = new Color(30,  58, 138);   // #1e3a8a
    private static final Color COL_ACCENT    = new Color(250,204, 21);    // #facc15
    private static final Color COL_DEBIT     = new Color(127, 29,  29);   // #7f1d1d
    private static final Color COL_CREDIT    = new Color(  6, 78,  59);   // #064e3b
    private static final Color COL_PENDING   = new Color(251,191, 36);    // #fbbf24
    private static final Color COL_RESOLVED  = new Color( 34,197, 94);    // #22c55e
    private static final Color COL_REJECTED  = new Color(239, 68, 68);    // #ef4444
    private static final Color COL_WHITE     = Color.WHITE;
    private static final Color COL_LIGHT     = new Color(203,213,225);    // #cbd5e1
    private static final Color COL_MUTED     = new Color(100,116,139);    // #64748b
    private static final Color COL_ROW_ALT   = new Color(30,  41,  59);   // #1e293b
    private static final Color COL_ROW_EVEN  = new Color(15,  23,  42);   // #0f172a

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static Font font(int size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TRANSACTIONS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * @param transactions  the list already shown to the user (already filtered)
     * @param userName      name of the logged-in user
     * @param isAdmin       whether the user is admin (affects title wording)
     * @param filePath      absolute path where the PDF will be saved
     */
    public void exportTransactions(List<Transaction> transactions,
                                   String userName,
                                   boolean isAdmin,
                                   String filePath) throws Exception {

        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath));

        // Page border event
        writer.setPageEvent(new PageBorderEvent(COL_BG));
        doc.open();

        // ── Header ────────────────────────────────────────────────────────────
        addHeader(doc, writer,
                "Historique des Transactions",
                userName,
                isAdmin ? "Administrateur" : "Utilisateur",
                transactions.size() + " transaction(s) exportée(s)");

        doc.add(Chunk.NEWLINE);

        // ── Summary KPIs ──────────────────────────────────────────────────────
        BigDecimal totalDebit  = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if ("debit".equalsIgnoreCase(t.getType()))        totalDebit  = totalDebit.add(t.getAmount());
            else if ("credit".equalsIgnoreCase(t.getType())) totalCredit = totalCredit.add(t.getAmount());
        }
        addTransactionSummary(doc, transactions.size(), totalDebit, totalCredit);

        doc.add(Chunk.NEWLINE);

        // ── Table ─────────────────────────────────────────────────────────────
        if (transactions.isEmpty()) {
            Paragraph empty = new Paragraph("Aucune transaction à afficher.",
                    font(11, Font.ITALIC, COL_MUTED));
            empty.setAlignment(Element.ALIGN_CENTER);
            doc.add(empty);
        } else {
            addTransactionTable(doc, transactions);
        }

        addFooter(doc);
        doc.close();
    }

    private void addTransactionSummary(Document doc, int count,
                                       BigDecimal totalDebit, BigDecimal totalCredit) throws Exception {
        PdfPTable kpi = new PdfPTable(3);
        kpi.setWidthPercentage(100);
        kpi.setSpacingBefore(4);
        kpi.setSpacingAfter(4);
        kpi.setWidths(new float[]{1f, 1f, 1f});

        kpi.addCell(kpiCell("Total", count + " transaction(s)", COL_HEADER));
        kpi.addCell(kpiCell("Total Débit", totalDebit + " TND", COL_DEBIT));
        kpi.addCell(kpiCell("Total Crédit", totalCredit + " TND", COL_CREDIT));

        doc.add(kpi);
    }

    private void addTransactionTable(Document doc, List<Transaction> transactions) throws Exception {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setWidths(new float[]{0.5f, 1.2f, 1.5f, 1.5f, 1f, 1f, 2f});

        // Column headers
        String[] headers = {"#", "Type", "De", "Vers", "Montant", "Statut", "Date"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font(9, Font.BOLD, COL_ACCENT)));
            cell.setBackgroundColor(COL_HEADER);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Rows
        boolean alternate = false;
        for (Transaction t : transactions) {
            Color rowBg = alternate ? COL_ROW_ALT : COL_ROW_EVEN;
            boolean isDebit = "debit".equalsIgnoreCase(t.getType());

            table.addCell(bodyCell(String.valueOf(t.getId()), rowBg, Element.ALIGN_CENTER));
            table.addCell(typeBadgeCell(t.getType(), rowBg));
            table.addCell(bodyCell(t.getSenderName() != null ? t.getSenderName() : "-", rowBg, Element.ALIGN_LEFT));
            table.addCell(bodyCell(t.getReceiverName() != null ? t.getReceiverName() : "-", rowBg, Element.ALIGN_LEFT));
            table.addCell(bodyCell(t.getAmount() + " " + t.getCurrency(), rowBg, Element.ALIGN_RIGHT));
            table.addCell(statusCell(t.getStatus(), rowBg));
            table.addCell(bodyCell(t.getCreatedAt() != null ? t.getCreatedAt().format(DT_FMT) : "-", rowBg, Element.ALIGN_CENTER));

            alternate = !alternate;
        }

        doc.add(table);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  COMPLAINTS
    // ═════════════════════════════════════════════════════════════════════════

    public void exportComplaints(List<Complaint> complaints,
                                 String userName,
                                 boolean isAdmin,
                                 String filePath) throws Exception {

        Document doc = new Document(PageSize.A4, 30, 30, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        writer.setPageEvent(new PageBorderEvent(COL_BG));
        doc.open();

        addHeader(doc, writer,
                "Historique des Réclamations",
                userName,
                isAdmin ? "Administrateur" : "Utilisateur",
                complaints.size() + " réclamation(s) exportée(s)");

        doc.add(Chunk.NEWLINE);

        // Summary
        addComplaintSummary(doc, complaints);
        doc.add(Chunk.NEWLINE);

        if (complaints.isEmpty()) {
            Paragraph empty = new Paragraph("Aucune réclamation à afficher.",
                    font(11, Font.ITALIC, COL_MUTED));
            empty.setAlignment(Element.ALIGN_CENTER);
            doc.add(empty);
        } else {
            addComplaintTable(doc, complaints);
        }

        addFooter(doc);
        doc.close();
    }

    private void addComplaintSummary(Document doc, List<Complaint> complaints) throws Exception {
        int pending = 0, resolved = 0, rejected = 0;
        for (Complaint c : complaints) {
            switch (c.getStatus().toLowerCase()) {
                case "pending"  -> pending++;
                case "resolved" -> resolved++;
                case "rejected" -> rejected++;
            }
        }
        PdfPTable kpi = new PdfPTable(4);
        kpi.setWidthPercentage(100);
        kpi.setSpacingBefore(4);
        kpi.setSpacingAfter(4);

        kpi.addCell(kpiCell("Total", complaints.size() + " réclamation(s)", COL_HEADER));
        kpi.addCell(kpiCell("En attente", String.valueOf(pending),  new Color(146,110, 19)));
        kpi.addCell(kpiCell("Résolues",   String.valueOf(resolved), new Color(20, 110, 55)));
        kpi.addCell(kpiCell("Rejetées",   String.valueOf(rejected), new Color(139, 40, 40)));

        doc.add(kpi);
    }

    private void addComplaintTable(Document doc, List<Complaint> complaints) throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setWidths(new float[]{0.5f, 4f, 1f, 1.2f});

        String[] headers = {"#", "Sujet", "Statut", "Date"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font(9, Font.BOLD, COL_ACCENT)));
            cell.setBackgroundColor(COL_HEADER);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        boolean alternate = false;
        for (Complaint c : complaints) {
            Color rowBg = alternate ? COL_ROW_ALT : COL_ROW_EVEN;

            table.addCell(bodyCell(String.valueOf(c.getId()), rowBg, Element.ALIGN_CENTER));

            // Subject + optional response
            PdfPCell subjectCell = new PdfPCell();
            subjectCell.setBackgroundColor(rowBg);
            subjectCell.setBorder(Rectangle.NO_BORDER);
            subjectCell.setPadding(7);
            Paragraph subjectPara = new Paragraph(c.getSubject(), font(9, Font.NORMAL, COL_WHITE));
            subjectPara.setSpacingAfter(0);
            subjectCell.addElement(subjectPara);
            if (c.getResponse() != null && !c.getResponse().isBlank()) {
                Paragraph respPara = new Paragraph("Réponse: " + c.getResponse(),
                        font(8, Font.ITALIC, COL_MUTED));
                respPara.setSpacingBefore(3);
                subjectCell.addElement(respPara);
            }
            table.addCell(subjectCell);

            table.addCell(statusCell(c.getStatus(), rowBg));
            table.addCell(bodyCell(
                    c.getComplaintDate() != null ? c.getComplaintDate().format(D_FMT) : "-",
                    rowBg, Element.ALIGN_CENTER));

            alternate = !alternate;
        }

        doc.add(table);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Shared building blocks
    // ═════════════════════════════════════════════════════════════════════════

    private void addHeader(Document doc, PdfWriter writer,
                           String title, String userName,
                           String role, String subtitle) throws Exception {
        // Dark header band drawn directly on the canvas
        PdfContentByte cb = writer.getDirectContentUnder();
        float w = doc.getPageSize().getWidth();
        cb.setColorFill(new com.lowagie.text.pdf.CMYKColor(COL_HEADER.getRed()   / 255f,
                COL_HEADER.getGreen() / 255f,
                COL_HEADER.getBlue()  / 255f, 0));
        // Use a simple rectangle for the header background via a table instead
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3f, 1f});
        header.setSpacingAfter(10);

        // Left: FINTECH + title
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setBackgroundColor(COL_HEADER);
        left.setPadding(14);
        left.addElement(new Paragraph("FINTECH", font(18, Font.BOLD, COL_ACCENT)));
        left.addElement(new Paragraph(title, font(13, Font.BOLD, COL_WHITE)));
        left.addElement(new Paragraph(subtitle, font(9, Font.NORMAL, COL_LIGHT)));
        header.addCell(left);

        // Right: user info + date
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setBackgroundColor(COL_BG);
        right.setPadding(14);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph(userName, font(11, Font.BOLD, COL_WHITE)));
        right.addElement(new Paragraph(role, font(9, Font.NORMAL, COL_MUTED)));
        right.addElement(new Paragraph(
                "Généré le " + LocalDateTime.now().format(DT_FMT),
                font(8, Font.ITALIC, COL_MUTED)));
        header.addCell(right);

        doc.add(header);

        // Accent divider line
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingAfter(8);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(COL_ACCENT);
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setFixedHeight(3f);
        line.addCell(lineCell);
        doc.add(line);
    }

    private void addFooter(Document doc) throws Exception {
        doc.add(Chunk.NEWLINE);
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(
                "FINTECH – Document généré automatiquement – Ne pas modifier",
                font(8, Font.ITALIC, COL_MUTED)));
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(COL_MUTED);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        footer.addCell(cell);
        doc.add(footer);
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private PdfPCell kpiCell(String label, String value, Color bg) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.addElement(new Paragraph(label, font(8, Font.NORMAL, COL_LIGHT)));
        cell.addElement(new Paragraph(value, font(13, Font.BOLD, COL_WHITE)));
        return cell;
    }

    private PdfPCell bodyCell(String text, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font(9, Font.NORMAL, COL_LIGHT)));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(7);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell typeBadgeCell(String type, Color rowBg) {
        boolean isDebit = "debit".equalsIgnoreCase(type);
        String label = isDebit ? "DEBIT" : "CREDIT";
        Color badgeBg = isDebit ? new Color(220, 38, 38) : new Color(22, 163, 74);
        PdfPCell cell = new PdfPCell(new Phrase(label, font(8, Font.BOLD, COL_WHITE)));
        cell.setBackgroundColor(badgeBg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(7);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell statusCell(String status, Color rowBg) {
        Color badgeBg = switch (status.toLowerCase()) {
            case "pending"   -> new Color(180, 130, 10);
            case "completed", "resolved" -> new Color(22, 130, 60);
            case "failed", "rejected"    -> new Color(185, 40, 40);
            default -> COL_MUTED;
        };
        String label = status.toUpperCase();
        PdfPCell cell = new PdfPCell(new Phrase(label, font(8, Font.BOLD, COL_WHITE)));
        cell.setBackgroundColor(badgeBg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(7);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    // ── Page border painter ───────────────────────────────────────────────────

    static class PageBorderEvent extends PdfPageEventHelper {
        private final Color bgColor;
        PageBorderEvent(Color bgColor) { this.bgColor = bgColor; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            Rectangle rect = document.getPageSize();
            cb.saveState();
            cb.setColorFill(bgColor);
            cb.rectangle(0, 0, rect.getWidth(), rect.getHeight());
            cb.fill();
            cb.restoreState();

            // Page number
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                cb.beginText();
                cb.setFontAndSize(bf, 8);
                cb.setColorFill(new Color(100, 116, 139));
                cb.showTextAligned(PdfContentByte.ALIGN_RIGHT,
                        "Page " + writer.getPageNumber(),
                        rect.getWidth() - 30, 20, 0);
                cb.endText();
            } catch (Exception e) {
                // Non-fatal: skip page number if font fails to load
            }
        }
    }
}