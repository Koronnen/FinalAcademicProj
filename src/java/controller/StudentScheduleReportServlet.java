package controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class StudentScheduleReportServlet extends HttpServlet {

    // ── Colour palette (matches ReportServlet) ───────────────────────────────
    private static final BaseColor HEADER_BG  = new BaseColor(30,  58, 138);  // deep blue
    private static final BaseColor HEADER_TXT = BaseColor.WHITE;
    private static final BaseColor ROW_ALT    = new BaseColor(239, 246, 255); // light blue tint
    private static final BaseColor ACCENT     = new BaseColor(59, 130, 246);  // mid blue
    private static final BaseColor FOOTER_TXT = new BaseColor(107, 114, 128); // grey

    // ── Fonts ────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE   = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   HEADER_BG);
    private static final Font FONT_SUB     = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, FOOTER_TXT);
    private static final Font FONT_TH      = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   HEADER_TXT);
    private static final Font FONT_TD      = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font FONT_DAY_HDR = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);

    // ── Day ordering ─────────────────────────────────────────────────────────
    private static final String[] DAY_ORDER = {
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    };

    private Connection getMySQLConnection() throws SQLException, ClassNotFoundException {
        String driver = getServletContext().getInitParameter("mysql.jdbcClassName");
        String url    = getServletContext().getInitParameter("mysql.jdbcDriverURL") + "://"
                      + getServletContext().getInitParameter("mysql.dbHostName") + ":"
                      + getServletContext().getInitParameter("mysql.dbPort") + "/"
                      + getServletContext().getInitParameter("mysql.databaseName");
        Class.forName(driver);
        return DriverManager.getConnection(url,
                getServletContext().getInitParameter("mysql.dbUserName"),
                getServletContext().getInitParameter("mysql.dbPassword"));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Auth guard ────────────────────────────────────────────────────────
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        String userId = (String) session.getAttribute("USER_ID");

        // ── Resolve student info ──────────────────────────────────────────────
        String studentName = userId;
        String studentId   = "";

        try (Connection conn = getMySQLConnection()) {
            String nameSql = "SELECT STU_ID, CONCAT(FNAME, ' ', LNAME) AS fullname FROM STUDENT WHERE USER_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(nameSql)) {
                ps.setString(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        studentId   = rs.getString("STU_ID");
                        String fn   = rs.getString("fullname");
                        if (fn != null && !fn.trim().isEmpty()) studentName = fn.trim();
                    }
                }
            }
        } catch (Exception ignored) {}

        // ── Filename & timestamps ─────────────────────────────────────────────
        SimpleDateFormat tsFormat   = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat dispFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm:ss a");
        String timestamp   = tsFormat.format(new Date());
        String generatedAt = dispFormat.format(new Date());
        String filename    = "STUDENT_TIMETABLE_" + timestamp + ".pdf";

        String appName = getServletContext().getInitParameter("appName");
        if (appName == null || appName.isEmpty()) appName = "Active Learning System";

        // ── Stream PDF ────────────────────────────────────────────────────────
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        try (OutputStream out = response.getOutputStream()) {
            generateTimetablePDF(out, appName, studentName, studentId, userId, generatedAt);
        } catch (DocumentException e) {
            throw new ServletException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void generateTimetablePDF(OutputStream out, String appName, String studentName,
                                      String studentId, String userId, String generatedAt)
            throws DocumentException, IOException {

        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 80, 60);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new HeaderFooterEvent(appName, studentName, generatedAt, "Student Timetable Report"));
        doc.open();

        // ── Report title block ────────────────────────────────────────────────
        Paragraph title = new Paragraph("Student Weekly Timetable", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph sub = new Paragraph("Schedule overview for: " + studentName
                + (studentId.isEmpty() ? "" : "  |  ID: " + studentId), FONT_SUB);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(8);
        doc.add(sub);

        doc.add(new Chunk(new LineSeparator(1f, 100f, ACCENT, Element.ALIGN_CENTER, -2)));
        doc.add(Chunk.NEWLINE);

        // ── Fetch enrolled schedule grouped by day ────────────────────────────
        // Map: day -> list of rows
        java.util.Map<String, java.util.List<String[]>> byDay = new java.util.LinkedHashMap<>();
        for (String d : DAY_ORDER) byDay.put(d, new java.util.ArrayList<>());

        String sql = "SELECT s.DAY_OF_WEEK, s.TIME_START, s.TIME_END, "
                   + "c.COURSE_CODE, c.COURSE_NAME, "
                   + "CONCAT(i.FNAME, ' ', i.LNAME) AS instructor "
                   + "FROM ENROLLMENT e "
                   + "JOIN SCHEDULE s ON e.SCHED_ID = s.SCHED_ID "
                   + "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID "
                   + "JOIN COURSE c ON ic.COURSE_ID = c.COURSE_ID "
                   + "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID "
                   + "JOIN STUDENT st ON e.STU_ID = st.STU_ID "
                   + "WHERE st.USER_ID = ? "
                   + "ORDER BY FIELD(s.DAY_OF_WEEK,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'), s.TIME_START";

        try (Connection conn = getMySQLConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String day  = rs.getString("DAY_OF_WEEK");
                    String[] row = {
                        rs.getString("COURSE_CODE"),
                        rs.getString("COURSE_NAME"),
                        rs.getString("instructor"),
                        formatTime(rs.getString("TIME_START")) + " – " + formatTime(rs.getString("TIME_END"))
                    };
                    byDay.computeIfAbsent(day, k -> new java.util.ArrayList<>()).add(row);
                }
            }
        } catch (Exception e) {
            Paragraph err = new Paragraph("Error loading schedule data: " + e.getMessage(), FONT_SUB);
            doc.add(err);
            doc.close();
            return;
        }

        // ── Build one section per day that has classes ────────────────────────
        boolean anyData = false;
        for (String day : DAY_ORDER) {
            java.util.List<String[]> rows = byDay.get(day);
            if (rows == null || rows.isEmpty()) continue;
            anyData = true;

            // Day header bar
            PdfPTable dayHeader = new PdfPTable(1);
            dayHeader.setWidthPercentage(100);
            dayHeader.setSpacingBefore(12);
            PdfPCell dayCell = new PdfPCell(new Phrase(capitalize(day), FONT_DAY_HDR));
            dayCell.setBackgroundColor(ACCENT);
            dayCell.setPadding(7);
            dayCell.setBorder(Rectangle.NO_BORDER);
            dayHeader.addCell(dayCell);
            doc.add(dayHeader);

            // Data table
            PdfPTable table = buildTableShell(new float[]{18f, 38f, 28f, 22f});
            addHeaderCell(table, "Course Code");
            addHeaderCell(table, "Course Name");
            addHeaderCell(table, "Instructor");
            addHeaderCell(table, "Time Slot");

            boolean alt = false;
            for (String[] row : rows) {
                BaseColor bg = alt ? ROW_ALT : BaseColor.WHITE;
                for (String cell : row) addDataCell(table, bg, FONT_TD, cell);
                alt = !alt;
            }
            doc.add(table);
        }

        if (!anyData) {
            Paragraph noData = new Paragraph("No enrolled courses found for this student.", FONT_SUB);
            noData.setAlignment(Element.ALIGN_CENTER);
            noData.setSpacingBefore(20);
            doc.add(noData);
        }

        doc.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Converts "HH:mm:ss" → "HH:mm" */
    private String formatTime(String t) {
        if (t == null) return "";
        return t.length() > 5 ? t.substring(0, 5) : t;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private PdfPTable buildTableShell(float[] widths) throws DocumentException {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(0);
        table.setHeaderRows(1);
        table.getDefaultCell().setPadding(5);
        return table;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TH));
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(BaseColor.WHITE);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, BaseColor bg, Font font, String text) {
        if (text == null) text = "—";
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderColor(new BaseColor(209, 213, 219));
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    // ── Page event – header & footer ─────────────────────────────────────────
    private static class HeaderFooterEvent extends PdfPageEventHelper {

        private final String appName;
        private final String studentName;
        private final String generatedAt;
        private final String reportLabel;
        private PdfTemplate totalPageTemplate;
        private BaseFont bfBold;

        private static final Font FNT_HDR_APP = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD,   BaseColor.WHITE);
        private static final Font FNT_HDR_SUB = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, new BaseColor(191, 219, 254));
        private static final Font FNT_FTR     = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, new BaseColor(107, 114, 128));
        private static final Font FNT_PAGE    = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD,   new BaseColor(30,  58, 138));
        private static final BaseColor HDR_BG = new BaseColor(30, 58, 138);
        private static final BaseColor ACC    = new BaseColor(59, 130, 246);

        HeaderFooterEvent(String appName, String studentName, String generatedAt, String reportLabel) {
            this.appName     = appName;
            this.studentName = studentName;
            this.generatedAt = generatedAt;
            this.reportLabel = reportLabel;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPageTemplate = writer.getDirectContent().createTemplate(30, 12);
            try { bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false); }
            catch (Exception ignored) {}
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle page = document.getPageSize();
            float ml = document.leftMargin();
            float usableW = page.getWidth() - ml - document.rightMargin();

            float hdrH = 40f;
            float hdrY = page.getTop() - hdrH;

            cb.saveState();
            cb.setColorFill(HDR_BG);
            cb.rectangle(ml, hdrY, usableW, hdrH);
            cb.fill();
            cb.restoreState();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(appName, FNT_HDR_APP), ml + 8, hdrY + 24, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(reportLabel, FNT_HDR_SUB), ml + 8, hdrY + 10, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Student: " + studentName, FNT_HDR_SUB), ml + usableW - 8, hdrY + 24, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Generated: " + generatedAt, FNT_HDR_SUB), ml + usableW - 8, hdrY + 10, 0);

            float ftrY = document.bottomMargin() - 12;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("CONFIDENTIAL – For authorized personnel only.", FNT_FTR), ml, ftrY, 0);

            float pageNumX = ml + usableW;
            Phrase pagePhrase = new Phrase("Page " + writer.getPageNumber() + " of ", FNT_PAGE);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, pagePhrase, pageNumX - 15, ftrY, 0);
            cb.addTemplate(totalPageTemplate, pageNumX - 13, ftrY - 1);

            cb.saveState();
            cb.setColorStroke(ACC);
            cb.setLineWidth(0.5f);
            cb.moveTo(ml, ftrY + 10);
            cb.lineTo(ml + usableW, ftrY + 10);
            cb.stroke();
            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPageTemplate != null) {
                totalPageTemplate.beginText();
                try {
                    if (bfBold == null) bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
                    totalPageTemplate.setFontAndSize(bfBold, 8);
                } catch (Exception ignored) {}
                totalPageTemplate.setColorFill(HDR_BG);
                totalPageTemplate.showText(String.valueOf(writer.getPageNumber()));
                totalPageTemplate.endText();
            }
        }
    }
}