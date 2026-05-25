package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

public class SignUpServlet extends HttpServlet {
    private Connection derbyConn;
    private Connection mysqlConn;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        try {
            Class.forName(context.getInitParameter("derby.jdbcClassName"));
            String derbyUser = context.getInitParameter("derby.dbUserName");
            String derbyPass = context.getInitParameter("derby.dbPassword");
            StringBuilder derbyUrl = new StringBuilder(context.getInitParameter("derby.jdbcDriverURL"))
                    .append("://").append(context.getInitParameter("derby.dbHostName"))
                    .append(":").append(context.getInitParameter("derby.dbPort"))
                    .append("/").append(context.getInitParameter("derby.databaseName"));
            derbyConn = DriverManager.getConnection(derbyUrl.toString(), derbyUser, derbyPass);
            System.out.println("Derby Database Connected Successfully.");

            Class.forName(context.getInitParameter("mysql.jdbcClassName"));
            String mysqlUser = context.getInitParameter("mysql.dbUserName");
            String mysqlPass = context.getInitParameter("mysql.dbPassword");
            StringBuilder mysqlUrl = new StringBuilder(context.getInitParameter("mysql.jdbcDriverURL"))
                    .append("://").append(context.getInitParameter("mysql.dbHostName"))
                    .append(":").append(context.getInitParameter("mysql.dbPort"))
                    .append("/").append(context.getInitParameter("mysql.databaseName"));
            mysqlConn = DriverManager.getConnection(mysqlUrl.toString(), mysqlUser, mysqlPass);
            System.out.println("MySQL Database Connected Successfully.");
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Database Initialization Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Connection getPostgresConnection() throws SQLException, ClassNotFoundException {
        String driver = getServletContext().getInitParameter("postgres.jdbcClassName");
        String url = getServletContext().getInitParameter("postgres.jdbcDriverURL") + "://"
                + getServletContext().getInitParameter("postgres.dbHostName") + ":"
                + getServletContext().getInitParameter("postgres.dbPort") + "/"
                + getServletContext().getInitParameter("postgres.databaseName");
        String user = getServletContext().getInitParameter("postgres.dbUserName");
        String pass = getServletContext().getInitParameter("postgres.dbPassword");
        Class.forName(driver);
        return DriverManager.getConnection(url, user, pass);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String authorId = "SYSTEM";
        ServletContext context = getServletContext();
        HttpSession s = request.getSession();
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        String email = request.getParameter("signEmail");
        String rawPass = request.getParameter("signPassword");
        String confirmPass = request.getParameter("confirmPass");

        if (email == null || email.trim().isEmpty() || rawPass == null || rawPass.trim().isEmpty()) {
            s.setAttribute("noInput", "Please enter your email and password.");
            response.sendRedirect(request.getContextPath() + "/signUp.jsp");
            return;
        }
        rawPass = rawPass.trim();
        confirmPass = (confirmPass != null) ? confirmPass.trim() : "";

        if (gRecaptchaResponse == null || gRecaptchaResponse.isEmpty()) {
            s.setAttribute("captchaError", "Please complete the CAPTCHA.");
            response.sendRedirect(request.getContextPath() + "/signUp.jsp");
            return;
        }
        if (!rawPass.equals(confirmPass)) {
            s.setAttribute("incorrectPass", "Password does not match.");
            response.sendRedirect(request.getContextPath() + "/signUp.jsp");
            return;
        }
        boolean isValid = verifyCaptcha(gRecaptchaResponse);
        if (!isValid) {
            s.setAttribute("captchaError", "CAPTCHA verification failed. Try again.");
            response.sendRedirect(request.getContextPath() + "/signUp.jsp");
            return;
        }

        String pass = Security.encrypt(rawPass, context);
        boolean userExist = checkUserInDerby(email);
        if (!userExist) {
            boolean registerSuccess = registerStudentDualWrite(email, pass, request);
            if (registerSuccess) {
                String STU = (String) s.getAttribute("STU_LOG");
                String LOG = (String) s.getAttribute("USER_ID");
                s.setAttribute("successMessage", "Registered successfully!");
                logAction("Created User: " + LOG, authorId);
                logAction("Created Student Profile: " + STU, authorId);
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            } else {
                s.setAttribute("captchaError", "Registration failed during database synchronization.");
                logAction("FAILED REGISTRATION ATTEMPT: Sync Error for email " + email, authorId);
                response.sendRedirect(request.getContextPath() + "/signUp.jsp");
            }
        } else {
            s.setAttribute("existError", "An account with that email already exists.");
            logAction("REJECTED REGISTRATION: Duplicate email hit (" + email + ")", authorId);
            response.sendRedirect(request.getContextPath() + "/signUp.jsp");
        }
    }

    public boolean registerStudentDualWrite(String email, String password, HttpServletRequest request) {
        HttpSession s = request.getSession();
        String insertUserStr = "INSERT INTO USERS(USER_ID, USER_ROLE, EMAIL, PASSWORD) VALUES (?, 'STUDENT', ?, ?)";
        String insertStudentStr = "INSERT INTO STUDENT(STU_ID, USER_ID, FNAME, LNAME) VALUES (?, ?, ?, ?)";
        try {
            String usrID = generateNextCustomID(derbyConn, "USERS", "USER_ID", "USR");
            String stuID = generateNextCustomID(mysqlConn, "STUDENT", "STU_ID", "STU");
            derbyConn.setAutoCommit(false);
            mysqlConn.setAutoCommit(false);

            try (PreparedStatement psDerbyUser = derbyConn.prepareStatement(insertUserStr)) {
                psDerbyUser.setString(1, usrID);
                psDerbyUser.setString(2, email);
                psDerbyUser.setString(3, password);
                psDerbyUser.executeUpdate();
            }
            try (PreparedStatement psMysqlUser = mysqlConn.prepareStatement(insertUserStr)) {
                psMysqlUser.setString(1, usrID);
                psMysqlUser.setString(2, email);
                psMysqlUser.setString(3, password);
                psMysqlUser.executeUpdate();
            }
            try (PreparedStatement psMysqlStudent = mysqlConn.prepareStatement(insertStudentStr)) {
                psMysqlStudent.setString(1, stuID);
                psMysqlStudent.setString(2, usrID);
                psMysqlStudent.setString(3, "");
                psMysqlStudent.setString(4, "");
                psMysqlStudent.executeUpdate();
            }
            derbyConn.commit();
            mysqlConn.commit();
            s.setAttribute("USER_ID", usrID);
            s.setAttribute("STU_LOG", stuID);
            s.setAttribute("USER_ROLE", "STUDENT");
            return true;
        } catch (SQLException err) {
            err.printStackTrace();
            try { derbyConn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { mysqlConn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { derbyConn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            try { mysqlConn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public boolean checkUserInDerby(String email) {
        String queryStr = "SELECT USER_ID FROM USERS WHERE LOWER(EMAIL) = LOWER(?)";
        try (PreparedStatement ps = derbyConn.prepareStatement(queryStr)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException err) {
            err.printStackTrace();
            return false;
        }
        return false;
    }

    private boolean verifyCaptcha(String gRecaptchaResponse) throws IOException {
        ServletConfig config = getServletConfig();
        final String SECRET_KEY = config.getInitParameter("CaptchaSecretKey");
        String url = "https://www.google.com/recaptcha/api/siteverify";
        String params = "secret=" + SECRET_KEY + "&response=" + gRecaptchaResponse;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            os.write(params.getBytes());
        }
        StringBuilder responseStr = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
        }
        JSONObject jsonResponse = new JSONObject(responseStr.toString());
        return jsonResponse.getBoolean("success");
    }

    private void logAction(String actionMade, String authorId) {
        String insertLogSQL = "INSERT INTO LOG (LOG_ID, ACTION_MADE, AUTHOR, LOG_DATE, LOG_TIME) VALUES (?, ?, ?, ?, ?)";
        if (authorId == null || authorId.trim().isEmpty()) {
            authorId = "SYSTEM";
        }
        try (Connection pgConn = getPostgresConnection();
                PreparedStatement pstmt = pgConn.prepareStatement(insertLogSQL)) {
            String logId = UUID.randomUUID().toString().substring(0, 9);
            pstmt.setString(1, logId);
            pstmt.setString(2, actionMade);
            pstmt.setString(3, authorId);
            pstmt.setDate(4, Date.valueOf(LocalDate.now()));
            pstmt.setTime(5, Time.valueOf(LocalTime.now()));
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("PostgreSQL Telemetry Log Drop: " + e.getMessage());
        }
    }

    @Override
    public void destroy() {
        try {
            if (derbyConn != null && !derbyConn.isClosed()) { derbyConn.close(); }
            if (mysqlConn != null && !mysqlConn.isClosed()) { mysqlConn.close(); }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String generateNextCustomID(Connection conn, String tableName, String idColumnName, String prefix) throws SQLException {
        String sql = "SELECT MAX(" + idColumnName + ") FROM " + tableName + " WHERE " + idColumnName + " LIKE '" + prefix + "%'";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                String latestId = rs.getString(1);
                if (latestId != null) {
                    String numericPart = latestId.substring(prefix.length());
                    int nextNum = Integer.parseInt(numericPart) + 1;
                    return prefix + String.format("%06d", nextNum);
                }
            }
        }
        return prefix + "000001";
    }
}