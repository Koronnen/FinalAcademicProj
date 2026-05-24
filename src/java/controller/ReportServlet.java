package controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ReportServlet extends HttpServlet {

    // ── Colour palette ──────────────────────────────────────────────────────
    private static final BaseColor HEADER_BG   = new BaseColor(30,  58, 138);  // deep blue
    private static final BaseColor HEADER_TXT  = BaseColor.WHITE;
    private static final BaseColor ROW_ALT       = new BaseColor(239, 246, 255); // light blue tint
    private static final BaseColor ACCENT      = new BaseColor(59, 130, 246);  // mid blue
    private static final BaseColor FOOTER_TXT  = new BaseColor(107, 114, 128); // grey

    // ── Font definitions ────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   HEADER_BG);
    private static final Font FONT_SUB    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, FOOTER_TXT);
    private static final Font FONT_TH     = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   HEADER_TXT);
    private static final Font FONT_TD     = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font FONT_STAR   = new Font(Font.FontFamily.HELVETICA, 8,  Font.BOLD,   new BaseColor(220, 38, 38));

    // DB helpers
    private Connection getDerbyConnection() throws SQLException, ClassNotFoundException {
        ServletContext ctx = getServletContext();
        Class.forName(ctx.getInitParameter("derby.jdbcClassName"));
        String url = ctx.getInitParameter("derby.jdbcDriverURL") + "://"
                + ctx.getInitParameter("derby.dbHostName") + ":"
                + ctx.getInitParameter("derby.dbPort") + "/LoginDB";
        return DriverManager.getConnection(url,
                ctx.getInitParameter("derby.dbUserName"),
                ctx.getInitParameter("derby.dbPassword"));
    }

    private Connection getMySQLConnection() throws SQLException, ClassNotFoundException {
        ServletContext ctx = getServletContext();
        Class.forName(ctx.getInitParameter("mysql.jdbcClassName"));
        String url = ctx.getInitParameter("mysql.jdbcDriverURL") + "://"
                + ctx.getInitParameter("mysql.dbHostName") + ":"
                + ctx.getInitParameter("mysql.dbPort") + "/"
                + ctx.getInitParameter("mysql.databaseName");
        return DriverManager.getConnection(url,
                ctx.getInitParameter("mysql.dbUserName"),
                ctx.getInitParameter("mysql.dbPassword"));
    }

    private Connection getPostgresConnection() throws SQLException, ClassNotFoundException {
        ServletContext ctx = getServletContext();
        Class.forName(ctx.getInitParameter("postgres.jdbcClassName"));
        String url = ctx.getInitParameter("postgres.jdbcDriverURL") + "://"
                + ctx.getInitParameter("postgres.dbHostName") + ":"
                + ctx.getInitParameter("postgres.dbPort") + "/"
                + ctx.getInitParameter("postgres.databaseName");
        return DriverManager.getConnection(url,
                ctx.getInitParameter("postgres.dbUserName"),
                ctx.getInitParameter("postgres.dbPassword"));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        /* ── Auth guard ── */
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        Integer role = (Integer) session.getAttribute("role");
        if (role == null || role != 1) { // only ADMIN
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
            return;
        }

        String userId     = (String) session.getAttribute("USER_ID");
        String email      = (String) session.getAttribute("email");
        String reportType = request.getParameter("reportType");
        String fromDate   = request.getParameter("from");   // YYYY-MM-DD
        String toDate     = request.getParameter("to");     // YYYY-MM-DD

        if (reportType == null || reportType.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing reportType parameter.");
            return;
        }

        /* ── Timestamp & filename ── */
        SimpleDateFormat tsFormat   = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat dispFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm:ss a");
        String timestamp    = tsFormat.format(new Date());
        String generatedAt  = dispFormat.format(new Date());

        String filename;
        switch (reportType) {
            case "USERLIST":     filename = "USERLIST_"     + timestamp + ".pdf"; break;
            case "LOGALL":       filename = "LOGALL_"       + timestamp + ".pdf"; break;
            case "LOGMINE":      filename = "LOGMINE_"      + timestamp + ".pdf"; break;
            case "LOGTIMEBOUND": filename = "LOGTIMEBOUND_" + timestamp + ".pdf"; break;
            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown reportType.");
                return;
        }

        /* ── Resolve admin display name ── */
        String adminDisplay = resolveAdminName(userId, email);

        /* ── Resolve app name from web.xml ── */
        String appName = getServletContext().getInitParameter("appName");
        if (appName == null || appName.isEmpty()) appName = "Active Learning System";

        /* ── Stream PDF ── */
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        try (OutputStream out = response.getOutputStream()) {
            switch (reportType) {
                case "USERLIST":
                    generateUserListReport(out, appName, userId, adminDisplay, generatedAt);
                    break;
                case "LOGALL":
                    generateLogReport(out, appName, adminDisplay, generatedAt,
                            "ALL ACTIVITY LOGS", null, null, null);
                    break;
                case "LOGMINE":
                    generateLogReport(out, appName, adminDisplay, generatedAt,
                            "MY ACTIVITY LOGS", userId, null, null);
                    break;
                case "LOGTIMEBOUND":
                    if (fromDate == null || toDate == null || fromDate.isEmpty() || toDate.isEmpty()) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                "Parameters 'from' and 'to' are required for time-bound reports.");
                        return;
                    }
                    generateLogReport(out, appName, adminDisplay, generatedAt,
                            "ACTIVITY LOGS (" + fromDate + " to " + toDate + ")",
                            null, fromDate, toDate);
                    break;
            }
        } catch (DocumentException e) {
            throw new ServletException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private void generateUserListReport(OutputStream out, String appName, String loggedInUserId,
                                        String adminDisplay, String generatedAt)
            throws DocumentException, IOException {

        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 80, 60);
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        HeaderFooterEvent event = new HeaderFooterEvent(appName, adminDisplay, generatedAt);
        writer.setPageEvent(event);
        doc.open();

        addTitleBlock(doc, "USER LIST REPORT",
                "Generated by: " + adminDisplay + "   |   " + generatedAt,
                "(*) denotes the currently logged-in administrator account.");

        float[] colWidths = {5f, 15f, 15f, 65f};
        PdfPTable table = buildTableShell(colWidths);

        addHeaderCell(table, "#");
        addHeaderCell(table, "User ID");
        addHeaderCell(table, "Role");
        addHeaderCell(table, "Email Address");

        String sql = "SELECT USER_ID, USER_ROLE, EMAIL FROM USERS ORDER BY USER_ROLE, EMAIL";
        int rowNum = 0;
        try (Connection conn = getDerbyConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rowNum++;
                String uid   = rs.getString("USER_ID");
                String role  = rs.getString("USER_ROLE");
                String email = rs.getString("EMAIL");
                boolean isMe = uid.equals(loggedInUserId);
                BaseColor bg = (rowNum % 2 == 0) ? ROW_ALT : BaseColor.WHITE;

                if (isMe) {
                    addDataCell(table, bg, FONT_STAR, rowNum + " *");
                    addDataCell(table, bg, FONT_STAR, uid   + "  *");
                    addDataCell(table, bg, FONT_STAR, role);
                    addDataCell(table, bg, FONT_STAR, email);
                } else {
                    addDataCell(table, bg, FONT_TD, String.valueOf(rowNum));
                    addDataCell(table, bg, FONT_TD, uid);
                    addDataCell(table, bg, FONT_TD, role);
                    addDataCell(table, bg, FONT_TD, email);
                }
            }
        } catch (Exception e) {
            Paragraph err = new Paragraph("Error loading user data: " + e.getMessage(), FONT_SUB);
            doc.add(err);
        }

        if (rowNum == 0) {
            addEmptyRow(table, 4, "No user records found.");
        }

        doc.add(table);

        Paragraph summary = new Paragraph("Total records: " + rowNum, FONT_SUB);
        summary.setSpacingBefore(8);
        doc.add(summary);

        doc.close();
    }

    private void generateLogReport(OutputStream out, String appName, String adminDisplay,
                                   String generatedAt, String reportTitle,
                                   String authorFilter, String fromDate, String toDate)
            throws DocumentException, IOException {

        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 80, 60);
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        HeaderFooterEvent event = new HeaderFooterEvent(appName, adminDisplay, generatedAt);
        writer.setPageEvent(event);
        doc.open();

        String subtitle = "Generated by: " + adminDisplay + "   |   " + generatedAt;
        if (fromDate != null && toDate != null) {
            subtitle += "\nDate range: " + fromDate + " to " + toDate;
        }
        addTitleBlock(doc, reportTitle, subtitle, null);

        float[] colWidths = {4f, 10f, 43f, 18f, 13f, 12f};
        PdfPTable table = buildTableShell(colWidths);

        addHeaderCell(table, "#");
        addHeaderCell(table, "Log ID");
        addHeaderCell(table, "Action Performed");
        addHeaderCell(table, "Author (User ID)");
        addHeaderCell(table, "Date");
        addHeaderCell(table, "Time");

        StringBuilder sql = new StringBuilder("SELECT log_id, action_made, author, log_date, log_time FROM log");
        boolean hasWhere = false;

        if (authorFilter != null && !authorFilter.isEmpty()) {
            sql.append(" WHERE author = ?");
            hasWhere = true;
        }
        if (fromDate != null && toDate != null) {
            sql.append(hasWhere ? " AND" : " WHERE");
            sql.append(" log_date BETWEEN ? AND ?");
        }
        sql.append(" ORDER BY log_date DESC, log_time DESC");

        int rowNum = 0;
        try (Connection conn = getPostgresConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (authorFilter != null && !authorFilter.isEmpty()) {
                ps.setString(paramIndex++, authorFilter);
            }
            if (fromDate != null && toDate != null) {
                ps.setDate(paramIndex++, java.sql.Date.valueOf(fromDate));
                ps.setDate(paramIndex++, java.sql.Date.valueOf(toDate));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rowNum++;
                    BaseColor bg = (rowNum % 2 == 0) ? ROW_ALT : BaseColor.WHITE;

                    addDataCell(table, bg, FONT_TD, String.valueOf(rowNum));
                    addDataCell(table, bg, FONT_TD, rs.getString("log_id"));
                    addDataCell(table, bg, FONT_TD, rs.getString("action_made"));
                    addDataCell(table, bg, FONT_TD, rs.getString("author"));
                    addDataCell(table, bg, FONT_TD, rs.getString("log_date"));
                    addDataCell(table, bg, FONT_TD, rs.getString("log_time"));
                }
            }
        } catch (Exception e) {
            Paragraph err = new Paragraph("Error loading log data: " + e.getMessage(), FONT_SUB);
            doc.add(err);
        }

        if (rowNum == 0) {
            addEmptyRow(table, 6, "No log records found for the selected criteria.");
        }

        doc.add(table);

        Paragraph summary = new Paragraph("Total records: " + rowNum, FONT_SUB);
        summary.setSpacingBefore(8);
        doc.add(summary);

        doc.close();
    }

    private void addTitleBlock(Document doc, String title, String subtitle, String note)
            throws DocumentException {
        LineSeparator rule = new LineSeparator(2f, 100f, ACCENT, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(rule));

        Paragraph t = new Paragraph(title, FONT_TITLE);
        t.setAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(8);
        t.setSpacingAfter(4);
        doc.add(t);

        if (subtitle != null) {
            Paragraph s = new Paragraph(subtitle, FONT_SUB);
            s.setAlignment(Element.ALIGN_CENTER);
            s.setSpacingAfter(4);
            doc.add(s);
        }

        if (note != null) {
            Paragraph n = new Paragraph(note, FONT_SUB);
            n.setAlignment(Element.ALIGN_CENTER);
            n.setSpacingAfter(6);
            doc.add(n);
        }

        doc.add(new Chunk(new LineSeparator(1f, 100f, ACCENT, Element.ALIGN_CENTER, -2)));
        doc.add(Chunk.NEWLINE);
    }

    private PdfPTable buildTableShell(float[] relativeWidths) throws DocumentException {
        PdfPTable table = new PdfPTable(relativeWidths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
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

    private void addEmptyRow(PdfPTable table, int colSpan, String message) {
        PdfPCell cell = new PdfPCell(new Phrase(message, FONT_SUB));
        cell.setColspan(colSpan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(12);
        cell.setBackgroundColor(ROW_ALT);
        table.addCell(cell);
    }

    private String resolveAdminName(String userId, String email) {
        String name = null;
        String sql = "SELECT CONCAT(FNAME, ' ', LNAME) AS fullname FROM ADMINISTRATOR WHERE USER_ID = ?";
        
        try (Connection conn = getMySQLConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString("fullname");
                    if (name != null) {
                        name = name.trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return (name != null && !name.isEmpty()) ? name : (email != null ? email : userId);
    }

    // ── Page event – header & footer on every page ──────────────────────────
    private static class HeaderFooterEvent extends PdfPageEventHelper {

        private final String appName;
        private final String adminDisplay;
        private final String generatedAt;
        private PdfTemplate totalPageTemplate;
        private BaseFont bfHelveticaBold;

        private static final Font FNT_HDR_APP  = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD,   BaseColor.WHITE);
        private static final Font FNT_HDR_SUB  = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, new BaseColor(191, 219, 254));
        private static final Font FNT_FTR      = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, new BaseColor(107, 114, 128));
        private static final Font FNT_PAGE_NUM = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD,   new BaseColor(30, 58, 138));

        HeaderFooterEvent(String appName, String adminDisplay, String generatedAt) {
            this.appName      = appName;
            this.adminDisplay = adminDisplay;
            this.generatedAt  = generatedAt;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPageTemplate = writer.getDirectContent().createTemplate(30, 12);
            try {
                bfHelveticaBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            } catch (Exception e) {
                // Fallback safe initialization if system metrics fail
                bfHelveticaBold = null;
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle page   = document.getPageSize();

            float ml = document.leftMargin();
            float mr = document.rightMargin();
            float usableW = page.getWidth() - ml - mr;

            /* ── HEADER band ── */
            float hdrH  = 40f;
            float hdrY  = page.getTop() - hdrH;

            cb.saveState();
            cb.setColorFill(HEADER_BG);
            cb.rectangle(ml, hdrY, usableW, hdrH);
            cb.fill();
            cb.restoreState();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(appName, FNT_HDR_APP), ml + 8, hdrY + 24, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase("Activity & User Management Report", FNT_HDR_SUB), ml + 8, hdrY + 10, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase("Generated by: " + adminDisplay, FNT_HDR_SUB), ml + usableW - 8, hdrY + 24, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(generatedAt, FNT_HDR_SUB), ml + usableW - 8, hdrY + 10, 0);

            /* ── FOOTER ── */
            float ftrY = document.bottomMargin() - 12;

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase("CONFIDENTIAL – For authorized personnel only.", FNT_FTR), ml, ftrY, 0);

            float pageNumX = ml + usableW;
            int currentPage = writer.getPageNumber();
            Phrase pagePhrase = new Phrase("Page " + currentPage + " of ", FNT_PAGE_NUM);

            // Left aligned against right margin placeholder logic safely using ColumnText alignment parameters
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, pagePhrase, pageNumX - 15, ftrY, 0);
            cb.addTemplate(totalPageTemplate, pageNumX - 13, ftrY - 1);

            cb.saveState();
            cb.setColorStroke(ACCENT);
            cb.setLineWidth(0.5f);
            cb.moveTo(ml,       ftrY + 10);
            cb.lineTo(ml + usableW, ftrY + 10);
            cb.stroke();
            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPageTemplate != null) {
                totalPageTemplate.beginText();
                try {
                    if (bfHelveticaBold == null) {
                        bfHelveticaBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
                    }
                    totalPageTemplate.setFontAndSize(bfHelveticaBold, 8);
                } catch (Exception e) {
                    // Failover logic to system standard core font setup if instantiation is blocked
                }
                totalPageTemplate.setColorFill(HEADER_BG);
                totalPageTemplate.showText(String.valueOf(writer.getPageNumber()));
                totalPageTemplate.endText();
            }
        }
    }
}