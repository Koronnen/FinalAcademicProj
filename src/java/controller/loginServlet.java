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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

    public void init(ServletContext context) throws ServletException {
            ServletContext context = getServletContext();
            try {	
                    Class.forName(context.getInitParameter("derby.jdbcClassName"));
                    System.out.println("jdbcClassName: " + context.getInitParameter("derby.jdbcClassName"));
                    String username = context.getInitParameter("derby.dbUserName");
                    String password = context.getInitParameter("derby.dbPassword");
                    StringBuffer url = new StringBuffer(context.getInitParameter("jdbcDriverURL"))
                            .append("://")
                            .append(context.getInitParameter("derby.dbHostName"))
                            .append(":")
                            .append(context.getInitParameter("derby.dbPort"))
                            .append("/")
                            .append("LoginDB");
                    conn = 
                      DriverManager.getConnection(url.toString(),username,password);
                    System.out.println("Done loading databases");
            } catch (SQLException sqle){
                    System.out.println("SQLException error occured - " 
                            + sqle.getMessage());
            } catch (ClassNotFoundException nfe){
                    System.out.println("ClassNotFoundException error occured - " 
                    + nfe.getMessage());
            }
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
            s.setAttribute("captchaResult", false);
            System.out.println("Invalid captcha");
            response.sendRedirect("index.jsp"); //go back
            return; //Get out
        }
        boolean isValid = verifyCaptcha(gRecaptchaResponse);
        if (isValid){ //Valid Captcha, proceed with login check!
            // 1. Get the user type (This already checks email AND password in your DB)
            String email = request.getParameter("email").trim();
            String rawPass = request.getParameter("password").trim();
            String pass = Security.encrypt(rawPass, context);
            System.out.println("Encrypted pass: " + pass);
            int userType = checkUser(email, pass);

            // 2. Handle SUCCESS first
            if (userType == 1 || userType == 2) {
                s.setAttribute("email", email);
                s.setAttribute("role", (userType == 1) ? "GUEST" : "ADMIN");

                response.sendRedirect("success.jsp");
                return; // EXIT the method so no exceptions are thrown
            }

            // Default error for everything else
            throw new exception.AuthenticationException("Wrong Username and Password.");
        } else{
            s.setAttribute("captchaResult", false);
            System.out.println("Bad Captcha");
            response.sendRedirect("index.jsp");
        }
    }
    public boolean checkEmail(String email){
        boolean validEmail = false;
        try{
            String queryStr = "SELECT Email, Password, UserRole FROM USERS WHERE Email = ?";
            PreparedStatement ps = conn.prepareStatement(queryStr);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()){
                validEmail = true;
            }
            rs.close();
            ps.close();
        }catch(SQLException err){
            err.printStackTrace();
        }
        return validEmail;   
    }
    
    public int checkUser(String email, String password){
        try{
            String queryStr = "SELECT Email, Password, UserRole FROM USERS WHERE Email = ? AND Password = ?";
            PreparedStatement ps = conn.prepareStatement(queryStr);
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()){
                int role = rs.getString("UserRole").equals("GUEST") ? 1 : 2;
                return role;
            }
            rs.close();
            ps.close();
        }catch(SQLException err){
            err.printStackTrace();
        }
        return 0;   
    }
    private boolean verifyCaptcha(String gRecaptchaResponse) throws
        IOException {
            ServletConfig config = getServletConfig();
            final String SECRET_KEY = (String)config.getInitParameter("CaptchaSecretKey");
            System.out.println("LOADED KEY: " + SECRET_KEY);
            String url = "https://www.google.com/recaptcha/api/siteverify";
            String params = "secret=" + SECRET_KEY + "&response=" + 
            gRecaptchaResponse;
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            try {
                OutputStream os = con.getOutputStream();
                os.write(params.getBytes());
            }catch(IOException e){
                e.printStackTrace();
            }
            BufferedReader in = new BufferedReader(new
            InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getBoolean("success");
        }
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
