package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                    .append("://")
                    .append(context.getInitParameter("derby.dbHostName"))
                    .append(":")
                    .append(context.getInitParameter("derby.dbPort"))
                    .append("/")
                    .append(context.getInitParameter("derby.databaseName"));
            derbyConn = DriverManager.getConnection(derbyUrl.toString(), derbyUser, derbyPass);
            System.out.println("Derby Database Connected Successfully.");

            Class.forName(context.getInitParameter("mysql.jdbcClassName"));
            String mysqlUser = context.getInitParameter("mysql.dbUserName");
            String mysqlPass = context.getInitParameter("mysql.dbPassword");
            StringBuilder mysqlUrl = new StringBuilder(context.getInitParameter("mysql.jdbcDriverURL"))
                    .append("://")
                    .append(context.getInitParameter("mysql.dbHostName"))
                    .append(":")
                    .append(context.getInitParameter("mysql.dbPort"))
                    .append("/")
                    .append(context.getInitParameter("mysql.databaseName"));
            mysqlConn = DriverManager.getConnection(mysqlUrl.toString(), mysqlUser, mysqlPass);
            System.out.println("MySQL Database Connected Successfully.");

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Database Initialization Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletContext context = getServletContext();
        HttpSession s = request.getSession();
        
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        String email = request.getParameter("signEmail");
        String rawPass = request.getParameter("signPassword");
        String confirmPass = request.getParameter("confirmPass");
        
        if (email == null || email.trim().isEmpty() || rawPass == null || rawPass.trim().isEmpty()) {
            s.setAttribute("noInput", "Please enter your email and password.");
            response.sendRedirect("signUp.jsp");
            return;
        }

        rawPass = rawPass.trim();
        confirmPass = (confirmPass != null) ? confirmPass.trim() : "";

        if (gRecaptchaResponse == null || gRecaptchaResponse.isEmpty()) {
            s.setAttribute("captchaError", "Please complete the CAPTCHA.");
            response.sendRedirect("signUp.jsp"); 
            return; 
        }
        
        if (!rawPass.equals(confirmPass)) {
            s.setAttribute("incorrectPass", "Password does not match.");
            response.sendRedirect("signUp.jsp");
            return;
        }
        
        boolean isValid = verifyCaptcha(gRecaptchaResponse);
        if (!isValid) {
            s.setAttribute("captchaError", "CAPTCHA verification failed. Try again.");
            response.sendRedirect("signUp.jsp");
            return;
        }

        String pass = Security.encrypt(rawPass, context);
        boolean userExist = checkUserInDerby(email); 

        if (!userExist) {
            boolean registerSuccess = registerStudentDualWrite(email, pass, request);
            if (registerSuccess) {
                s.setAttribute("successMessage", "Registration successful in both systems!");
                response.sendRedirect("index.jsp");
            } else {
                s.setAttribute("captchaError", "Registration failed during database synchronization.");
                response.sendRedirect("signUp.jsp");
            }
        } else {
            s.setAttribute("captchaError", "An account with that email already exists.");
            response.sendRedirect("signUp.jsp");
        }
    }

    public boolean registerStudentDualWrite(String email, String password, HttpServletRequest request) {
        HttpSession s = request.getSession();
        
        String selectUsers = "SELECT USER_ID FROM USERS";
        String selectStudents = "SELECT STU_ID FROM STUDENT";
        
        String insertUserStr = "INSERT INTO USERS(USER_ID, USER_ROLE, EMAIL, PASSWORD) VALUES (?, 'STUDENT', ?, ?)";
        String insertStudentStr = "INSERT INTO STUDENT(STU_ID, USER_ID, FNAME, LNAME, EMAIL) VALUES (?, ?, ?, ?, ?)";
        
        int lastUserNum = 0;
        int lastStudentNum = 0;

        try (PreparedStatement psUser = derbyConn.prepareStatement(selectUsers);
             ResultSet rsUser = psUser.executeQuery()) {
            
            while (rsUser.next()) {
                String currentUser = rsUser.getString("USER_ID");  
                if (currentUser != null && currentUser.startsWith("USR") && currentUser.length() > 3) {
                    try {
                        int currentId = Integer.parseInt(currentUser.substring(3));
                        if (currentId > lastUserNum) {
                            lastUserNum = currentId;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (SQLException err) {
            err.printStackTrace();
            return false;
        }
        
        try (PreparedStatement psStu = mysqlConn.prepareStatement(selectStudents);
             ResultSet rsStu = psStu.executeQuery()) {
            
            while (rsStu.next()) {
                String currentStu = rsStu.getString("STU_ID");  
                if (currentStu != null && currentStu.startsWith("STU") && currentStu.length() > 3) {
                    try {
                        int currentId = Integer.parseInt(currentStu.substring(3));
                        if (currentId > lastStudentNum) {
                            lastStudentNum = currentId;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (SQLException err) {
            err.printStackTrace();
            return false;
        }
        
        String usrID = String.format("USR%06d", (lastUserNum + 1));
        String stuID = String.format("STU%06d", (lastStudentNum + 1));

        try {
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
                psMysqlStudent.setString(5, email);
                psMysqlStudent.executeUpdate();
            }

            derbyConn.commit();
            mysqlConn.commit();

            s.setAttribute("email", email);
            s.setAttribute("USER_ID", usrID);
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
        boolean accExist = false;
        String queryStr = "SELECT USER_ID FROM USERS WHERE EMAIL = ?";
        try (PreparedStatement ps = derbyConn.prepareStatement(queryStr)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    accExist = true;
                }
            }
        } catch (SQLException err) {
            err.printStackTrace();
        }
        return accExist;   
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

    @Override
    public void destroy() {
        try {
            if (derbyConn != null && !derbyConn.isClosed()) derbyConn.close();
            if (mysqlConn != null && !mysqlConn.isClosed()) mysqlConn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}