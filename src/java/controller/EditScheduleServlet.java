/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.IOException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Javo
 */
@WebServlet(name = "EditScheduleServlet", urlPatterns = {"/EditScheduleServlet"})
public class EditScheduleServlet extends HttpServlet {

    private Connection getMySQLConnection() throws SQLException, ClassNotFoundException {
        String driver = getServletContext().getInitParameter("mysql.jdbcClassName");
        String url = getServletContext().getInitParameter("mysql.jdbcDriverURL") + "://"
                + getServletContext().getInitParameter("mysql.dbHostName") + ":"
                + getServletContext().getInitParameter("mysql.dbPort") + "/"
                + getServletContext().getInitParameter("mysql.databaseName");
        String user = getServletContext().getInitParameter("mysql.dbUserName");
        String pass = getServletContext().getInitParameter("mysql.dbPassword");

        Class.forName(driver);
        return DriverManager.getConnection(url, user, pass);
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

    private void logAction(String actionMade, String authorId) {
        String insertLogSQL = "INSERT INTO LOG (LOG_ID, ACTION_MADE, AUTHOR, LOG_DATE, LOG_TIME) VALUES (?, ?, ?, ?, ?)";
        try (Connection pgConn = getPostgresConnection();
                PreparedStatement pstmt = pgConn.prepareStatement(insertLogSQL)) {
            String logId = java.util.UUID.randomUUID().toString().substring(0, 9);
            pstmt.setString(1, logId);
            pstmt.setString(2, actionMade);
            pstmt.setString(3, authorId);
            pstmt.setDate(4, java.sql.Date.valueOf(java.time.LocalDate.now()));
            pstmt.setTime(5, java.sql.Time.valueOf(java.time.LocalTime.now()));
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("PostgreSQL Telemetry Log Drop: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String scheduleId = request.getParameter("scheduleId");

        if (scheduleId == null || scheduleId.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/InstructorDashboardServlet");
            return;
        }

        try (Connection conn = getMySQLConnection()) {
            String sql = "SELECT DAY_OF_WEEK, TIME_START, TIME_END FROM SCHEDULE WHERE SCHED_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, scheduleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        request.setAttribute("scheduleId", scheduleId);
                        request.setAttribute("dayOfWeek", rs.getString("DAY_OF_WEEK"));
                        request.setAttribute("timeStart", rs.getString("TIME_START"));
                        request.setAttribute("timeEnd", rs.getString("TIME_END"));
                    } else {
                        response.sendRedirect("InstructorDashboardServlet");
                        return;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.getRequestDispatcher("/ErrorPages/error_sql.jsp").forward(request, response);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        request.getRequestDispatcher("EditSchedule.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String scheduleId = request.getParameter("scheduleId");
        String dayOfWeek = request.getParameter("day").toUpperCase();
        String timeStart = request.getParameter("start_time");
        String timeEnd = request.getParameter("end_time");

        HttpSession session = request.getSession(false);
        String userId = (session != null) ? (String) session.getAttribute("USER_ID") : "UNKNOWN";

        try (Connection conn = getMySQLConnection()) {
            String updateSQL = "UPDATE SCHEDULE SET DAY_OF_WEEK = ?, TIME_START = ?, TIME_END = ? WHERE SCHED_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                ps.setString(1, dayOfWeek);
                ps.setString(2, timeStart);
                ps.setString(3, timeEnd);
                ps.setString(4, scheduleId);
                ps.executeUpdate();
            }

            logAction("Instructor Updated Schedule: " + scheduleId + " to " + dayOfWeek + " " + timeStart + "-" + timeEnd, userId);

        } catch (SQLException e) {
            e.printStackTrace();
            request.getRequestDispatcher("/ErrorPages/error_sql.jsp").forward(request, response);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        response.sendRedirect("InstructorDashboardServlet");
    }
}