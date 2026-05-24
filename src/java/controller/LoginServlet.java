/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

public class LoginServlet extends HttpServlet {

    Connection conn;

    public void init() throws ServletException {
        ServletContext context = getServletContext();
        try {
            Class.forName(context.getInitParameter("derby.jdbcClassName"));
            System.out.println("jdbcClassName: " + context.getInitParameter("derby.jdbcClassName"));
            String username = context.getInitParameter("derby.dbUserName");
            String password = context.getInitParameter("derby.dbPassword");
            StringBuffer url = new StringBuffer(context.getInitParameter("derby.jdbcDriverURL"))
                    .append("://")
                    .append(context.getInitParameter("derby.dbHostName"))
                    .append(":")
                    .append(context.getInitParameter("derby.dbPort"))
                    .append("/")
                    .append("LoginDB");
            conn
                    = DriverManager.getConnection(url.toString(), username, password);
            System.out.println("Done loading databases");
        } catch (SQLException sqle) {
            System.out.println("SQLException error occured - "
                    + sqle.getMessage());
        } catch (ClassNotFoundException nfe) {
            System.out.println("ClassNotFoundException error occured - "
                    + nfe.getMessage());
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletContext context = getServletContext();
        HttpSession s = request.getSession();
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");

        if (gRecaptchaResponse == null || gRecaptchaResponse.isEmpty()) {
            s.setAttribute("loginError", "Please complete the CAPTCHA.");
            System.out.println("Invalid captcha");
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        boolean isValid = verifyCaptcha(gRecaptchaResponse);
        if (isValid) {
            String email = request.getParameter("email").trim();
            String rawPass = request.getParameter("password").trim();
            String pass = Security.encrypt(rawPass, context);
            System.out.println("Encrypted pass: " + pass);
            int userType = checkUser(email, pass);

            if (userType == 1 || userType == 2 || userType == 3) {
                System.out.println("NAME IN DB");
                String id = getID(email);
                s.setAttribute("email", email);
                s.setAttribute("USER_ID", id);
                s.setAttribute("role", userType);
                System.out.println(email);
                System.out.println(id);
                System.out.println(userType);

                String userDisplay;
                if (userType == 1) {
                    userDisplay = "ADMIN";
                } else if (userType == 2) {
                    userDisplay = "STUDENT";
                } else {
                    userDisplay = "INSTRUCTOR";
                }

                logAction("User successfully logged into the system as " + userDisplay + ".", id);
                switch (userType) {
                    case 1:
                        System.out.println("reached trying to get tot admin");
                        response.sendRedirect(request.getContextPath() + "/AdminDashboardServlet");
                        break;
                    case 2:
                        response.sendRedirect(request.getContextPath() + "/StudentServlet");
                        break;
                    case 3:
                        response.sendRedirect(request.getContextPath() + "/InstructorProfileServlet");
                        break;
                    default:
                        break;
                }
                return;
            } else {
                s.setAttribute("loginError", "Invalid email or password.");
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                return;
            }
        } else {
            s.setAttribute("loginError", "CAPTCHA verification failed. Please try again.");
            System.out.println("Bad Captcha");
            response.sendRedirect(request.getContextPath() + "/index.jsp");
        }
    }

    public int checkUser(String email, String password) {
        try {
            String queryStr = "SELECT USER_ID, USER_ROLE, EMAIL, PASSWORD FROM USERS WHERE EMAIL = ? AND PASSWORD = ?";
            PreparedStatement ps = conn.prepareStatement(queryStr);
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            int role = 0;

            if (rs.next()) {
                String uRole = rs.getString("USER_ROLE");
                if (uRole.equals("ADMIN")) {
                    role = 1;
                } else if (uRole.equals("STUDENT")) {
                    role = 2;
                } else if (uRole.equals("INSTRUCTOR")) {
                    role = 3;
                }
                return role;
            }
            rs.close();
            ps.close();
        } catch (SQLException err) {
            err.printStackTrace();
        }
        return 0;
    }

    private boolean verifyCaptcha(String gRecaptchaResponse) throws
            IOException {
        ServletConfig config = getServletConfig();
        final String SECRET_KEY = (String) config.getInitParameter("CaptchaSecretKey");
        System.out.println("LOADED KEY: " + SECRET_KEY);
        String url = "https://www.google.com/recaptcha/api/siteverify";
        String params = "secret=" + SECRET_KEY + "&response="
                + gRecaptchaResponse;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        try {
            OutputStream os = con.getOutputStream();
            os.write(params.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getBoolean("success");
    }

    private String getID(String email) {
        String id = "";
        try {
            String queryStr = "SELECT USER_ID FROM USERS WHERE EMAIL = ?";
            PreparedStatement ps = conn.prepareStatement(queryStr);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                id = rs.getString("USER_ID");
            }
            rs.close();
            ps.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return id;
    }

    private void logAction(String actionMade, String authorId) {
        String insertLogSQL = "INSERT INTO LOG (LOG_ID, ACTION_MADE, AUTHOR, LOG_DATE, LOG_TIME) VALUES (?, ?, ?, ?, ?)";
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
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
