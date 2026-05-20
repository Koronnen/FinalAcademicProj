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
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

public class signUpServlet extends HttpServlet {

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
        
        String email = request.getParameter("signEmail");
        String rawPass = request.getParameter("signPassword").trim();
        
        if (gRecaptchaResponse == null || gRecaptchaResponse.isEmpty()) {
            s.setAttribute("captchaError", "Please complete the CAPTCHA.");
            System.out.println("Invalid captcha");
            response.sendRedirect("index.jsp"); 
            return; 
        }
        
        if(email.trim().isEmpty() || email == null || rawPass.trim().isEmpty() || rawPass == null){
            s.setAttribute("noInput", "Please enter your email and password.");
            response.sendRedirect("signUp.jsp");
            return;
        }
        
        String pass = Security.encrypt(rawPass, context);
        boolean userExist = checkUser(email, pass);
        boolean isValid = verifyCaptcha(gRecaptchaResponse);
        
        if(isValid){
            if(!userExist){
                boolean registerSuccess = registerStudent(email, pass);
                if (registerSuccess) {
                    s.setAttribute("successMessage", "Registration successful!");
                    response.sendRedirect("index.jsp");
                }
            }
        }
        
    }
    public boolean registerStudent(String email, String password){
        boolean success = false;
        String insertStr = "INSERT INTO USERS(USER_ID, USER_ROLE, EMAIL, PASSWORD) VALUES (?, 'STUDENT', ?, ?)";
        String selectStr = "SELECT USER_ID, USER_ROLE, EMAIL, PASSWORD FROM USERS";
        
        try(PreparedStatement ps = conn.prepareStatement(selectStr);
            ResultSet rs = ps.executeQuery()){
            
            int lastUser = 0;
            while(rs.next()){
                String currentUser = rs.getString("USER_ID");  
                if (currentUser != null && currentUser.startsWith("USR") && currentUser.length() > 3) {
                try {
                    String numericPart = currentUser.substring(3);
                    int currentId = Integer.parseInt(numericPart);
                    if (currentId > lastUser) {
                        lastUser = currentId;
                    }
                } catch (NumberFormatException e) {
                    throw new exception.UserException();
                }
            }
            int nextId = lastUser + 1;
            String usrID = String.format("USR%06d", nextId);
            try (PreparedStatement ps2 = conn.prepareStatement(insertStr)){
                ps.setString(1, usrID);
                ps.setString(2, email);
                ps.setString(3, password);
                
                int rowsAffected = ps.executeUpdate();
                if(rowsAffected>0){
                    success = true;
                }
                ps.close();
            }catch(SQLException err){
                err.printStackTrace();;
            }        
            }
        }catch(SQLException err){
            err.printStackTrace();;
        }       
        return success;
    }
    
    public boolean checkUser(String email, String password){
        boolean accExist = false;
        try{
            String queryStr = "SELECT USER_ID, USER_ROLE, EMAIL, PASSWORD FROM USERS WHERE EMAIL = ?";
            PreparedStatement ps = conn.prepareStatement(queryStr);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                accExist = true;
            }
            rs.close();
            ps.close();
        }catch(SQLException err){
            err.printStackTrace();
        }
        return accExist;   
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
