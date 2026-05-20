/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class StudentServlet extends HttpServlet {
    
    private Connection getDerbyConnection() throws SQLException, ClassNotFoundException {
        String driver = getServletContext().getInitParameter("derby.jdbcClassName");
        String url = getServletContext().getInitParameter("derby.jdbcDriverURL") + "://" +
                     getServletContext().getInitParameter("derby.dbHostName") + ":" +
                     getServletContext().getInitParameter("derby.dbPort") + "/" +
                     getServletContext().getInitParameter("derby.databaseName");
        String user = getServletContext().getInitParameter("derby.dbUserName");
        String pass = getServletContext().getInitParameter("derby.dbPassword");

        Class.forName(driver);
        return DriverManager.getConnection(url, user, pass);
    }

    private Connection getMySQLConnection() throws SQLException, ClassNotFoundException {
        String driver = getServletContext().getInitParameter("mysql.jdbcClassName");
        String url = getServletContext().getInitParameter("mysql.jdbcDriverURL") + "://" +
                     getServletContext().getInitParameter("mysql.dbHostName") + ":" +
                     getServletContext().getInitParameter("mysql.dbPort") + "/" +
                     getServletContext().getInitParameter("mysql.databaseName");
        String user = getServletContext().getInitParameter("mysql.dbUserName");
        String pass = getServletContext().getInitParameter("mysql.dbPassword");
        
        Class.forName(driver);
        return DriverManager.getConnection(url, user, pass);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        // 3. Role Restriction Protection
        String userRole = (String) session.getAttribute("USER_ROLE");
        if (!"STUDENT".equalsIgnoreCase(userRole)) {
            response.sendRedirect("index.jsp");
            return;
        }

        String usrID = (String) session.getAttribute("USER_ID");
        String displayName = "Student";
        String dbStudentId = "", dbFName = "", dbLName = "", dbEmail = "";
        
        try(Connection sqlConn = getMySQLConnection();
            Connection derbyConn = getDerbyConnection();
            PreparedStatement ps = sqlConn.prepareStatement("SELECT * FROM STUDENT WHERE USER_ID = ?")){
            ps.setString(1, usrID);
            
            try(ResultSet rs = ps.executeQuery()){
                if (rs.next()) {
                    dbStudentId = rs.getString("STU_ID");
                    dbFName = rs.getString("FNAME");
                    dbLName = rs.getString("LNAME");
                    dbEmail = rs.getString("EMAIL");
                    
                    if (dbFName != null && !dbFName.trim().isEmpty()) {
                        displayName = dbFName;
                    }
                } else if(derbyConn != null){
                    try(PreparedStatement psD = sqlConn.prepareStatement("SELECT EMAIL FROM USERS WHERE USER_ID = ?")){                  
                    ps.setString(1, usrID);
            
                    try(ResultSet rsD = psD.executeQuery()){
                        if (rs.next()) {
                            dbStudentId = rs.getString("STU_ID");
                            dbFName = rs.getString("FNAME");
                            dbLName = rs.getString("LNAME");
                            dbEmail = rs.getString("EMAIL");

                            if (dbFName != null && !dbFName.trim().isEmpty()) {
                                displayName = dbFName;
                            }
                        }
                    }
                }
            }
            }
        }catch(Exception e){
            e.printStackTrace();
            //DATABASE ERROR 
        }
        
    
    }
    
    
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
