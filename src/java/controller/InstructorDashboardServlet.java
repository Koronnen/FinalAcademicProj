/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.IOException;
import java.sql.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Javo
 */
public class InstructorDashboardServlet extends HttpServlet {

    private boolean isAuthorizedInstructor(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            return false;
        }

        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof Integer) {
            int userType = (Integer) roleObj;
            return userType == 3;
        }
        return false;
    }

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
            String logId = UUID.randomUUID().toString().substring(0, 9);
            pstmt.setString(1, logId);
            pstmt.setString(2, actionMade);
            pstmt.setString(3, authorId);
            pstmt.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
            pstmt.setTime(5, Time.valueOf(LocalTime.now()));
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("PostgreSQL Telemetry Log Drop: " + e.getMessage());
        }
    }

    private String generateNextCustomID(Connection conn, String tableName, String idColumnName, String prefix) throws SQLException {
        String sql = "SELECT " + idColumnName + " FROM " + tableName + " WHERE " + idColumnName + " LIKE '" + prefix + "%' ORDER BY " + idColumnName + " DESC LIMIT 1";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String latestId = rs.getString(idColumnName);
                String numericPart = latestId.substring(prefix.length());
                int nextNum = Integer.parseInt(numericPart) + 1;
                return prefix + String.format("%06d", nextNum);
            }
        }
        return prefix + "000001";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorizedInstructor(request)) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        HttpSession session = request.getSession(false);
        String userId = (String) session.getAttribute("USER_ID");

        List<Map<String, String>> scheduleList = new ArrayList<>();

        try (Connection conn = getMySQLConnection()) {
            String sql = "SELECT s.SCHED_ID, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END, ic.COURSE_ID "
                    + "FROM SCHEDULE s "
                    + "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID "
                    + "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID "
                    + "WHERE i.USER_ID = ? "
                    + "ORDER BY s.DAY_OF_WEEK, s.TIME_START";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> sched = new HashMap<>();
                        sched.put("dayOfWeek", rs.getString("DAY_OF_WEEK"));
                        sched.put("timeStart", rs.getString("TIME_START"));
                        sched.put("timeEnd", rs.getString("TIME_END"));

                        String courseId = rs.getString("COURSE_ID");
                        String courseName = courseId;
                        if ("CRS000001".equals(courseId)) {
                            courseName = "Advanced Probability and Statistics";
                        }
                        if ("CRS000002".equals(courseId)) {
                            courseName = "Applications Development";
                        }
                        sched.put("courseName", courseName);

                        sched.put("scheduleId", rs.getString("SCHED_ID"));

                        scheduleList.add(sched);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to fetch schedules: " + e.getMessage());
        }

        request.setAttribute("scheduleList", scheduleList);
        request.getRequestDispatcher("InstructorDashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorizedInstructor(request)) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        HttpSession session = request.getSession(false);
        String userId = (String) session.getAttribute("USER_ID");

        String action = request.getParameter("action");

        if ("delete".equals(action)) {
            String scheduleId = request.getParameter("scheduleId");

            try (Connection conn = getMySQLConnection()) {
                String deleteSQL = "DELETE FROM SCHEDULE WHERE SCHED_ID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
                    pstmt.setString(1, scheduleId);
                    pstmt.executeUpdate();
                }

                logAction("Instructor Deleted Schedule: " + scheduleId, userId);

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Instructor Schedule Deletion Failed: " + e.getMessage());
            }

            response.sendRedirect("InstructorDashboardServlet");
            return; // Stop execution here so it doesn't try to add a schedule
        }

        try (Connection conn = getMySQLConnection()) {
            conn.setAutoCommit(false);

            String instId = null;
            String getInstIdSql = "SELECT INST_ID FROM INSTRUCTOR WHERE USER_ID = ?";
            try (PreparedStatement ps = conn.prepareStatement(getInstIdSql)) {
                ps.setString(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        instId = rs.getString("INST_ID");
                    }
                }
            }

            if (instId == null) {
                throw new SQLException("Could not locate INSTRUCTOR profile for the active session USER_ID.");
            }

            String courseId = request.getParameter("course_id");
            String dayOfWeek = request.getParameter("day").toUpperCase();
            String timeStart = request.getParameter("start_time");
            String timeEnd = request.getParameter("end_time");

            String checkInstC = "SELECT INST_C_ID FROM INSTRUCTORS_COURSE WHERE INST_ID = ? AND COURSE_ID = ?";
            String instCId = null;
            try (PreparedStatement ps = conn.prepareStatement(checkInstC)) {
                ps.setString(1, instId);
                ps.setString(2, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        instCId = rs.getString("INST_C_ID");
                    }
                }
            }

            if (instCId == null) {
                instCId = generateNextCustomID(conn, "INSTRUCTORS_COURSE", "INST_C_ID", "ISC");
                String insertInstC = "INSERT INTO INSTRUCTORS_COURSE (INST_C_ID, INST_ID, COURSE_ID) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertInstC)) {
                    ps.setString(1, instCId);
                    ps.setString(2, instId);
                    ps.setString(3, courseId);
                    ps.executeUpdate();
                }
            }

            String insertSched = "INSERT INTO SCHEDULE (SCHED_ID, INST_C_ID, DAY_OF_WEEK, TIME_START, TIME_END) VALUES (?, ?, ?, ?, ?)";
            String nextSchedId = generateNextCustomID(conn, "SCHEDULE", "SCHED_ID", "SCD");
            try (PreparedStatement ps = conn.prepareStatement(insertSched)) {
                ps.setString(1, nextSchedId);
                ps.setString(2, instCId);
                ps.setString(3, dayOfWeek);
                ps.setString(4, timeStart);
                ps.setString(5, timeEnd);
                ps.executeUpdate();
            }

            conn.commit();
            logAction("Instructor Self-Assigned Schedule: " + nextSchedId + " (" + timeStart + "-" + timeEnd + ")", userId);

            response.sendRedirect("InstructorDashboardServlet");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Instructor Schedule Addition Failed: " + e.getMessage());
            response.sendRedirect("InstructorDashboard.jsp?error=true");
        }
    }
}