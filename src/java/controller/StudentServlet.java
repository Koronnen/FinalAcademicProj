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
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StudentServlet extends HttpServlet {
    
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
                            .append(context.getInitParameter("mysql.databaseName"));
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
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
