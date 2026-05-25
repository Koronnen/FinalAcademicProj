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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import model.Schedule;

/**
 *
 * @author Javo
 */
public class InstructorDashboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(InstructorDashboardServlet.class.getName());

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
            LOGGER.log(Level.SEVERE, "PostgreSQL Telemetry Log Drop: " + e.getMessage(), e);
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
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to InstructorDashboardServlet.");
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        HttpSession session = request.getSession(false);
        String userId = (String) session.getAttribute("USER_ID");

        Map<String, List<Schedule>> groupedSchedules = new java.util.HashMap<>(); 
        
        Schedule currentClass = null;
        List<Schedule> upcomingClassesToday = new ArrayList<>();
        
        String todayName = LocalDate.now().getDayOfWeek().name();
        LocalTime currentTime = LocalTime.now();

        String instId = null; 

        try {
            try (Connection conn = getMySQLConnection()) {

                String getInstIdSql = "SELECT INST_ID FROM INSTRUCTOR WHERE USER_ID = ?";
                
                try (PreparedStatement ps = conn.prepareStatement(getInstIdSql)) {
                    ps.setString(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            instId = rs.getString("INST_ID");
                        }
                    }
                }

                if (instId != null) {
                    LOGGER.log(Level.INFO, "Loading schedule for Instructor ID: {0}", instId);

                    // 2. Fetch schedules
                    String sql = "SELECT s.SCHED_ID, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END, c.COURSE_NAME, "
                               + "0 AS STUDENT_COUNT " 
                               + "FROM SCHEDULE s "
                               + "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID " 
                               + "JOIN COURSE c ON ic.COURSE_ID = c.COURSE_ID " 
                               + "WHERE TRIM(ic.INST_ID) = ?"; 

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, instId.trim()); 
                        
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                String schedId = rs.getString("SCHED_ID");
                                String dayOfWeek = rs.getString("DAY_OF_WEEK");
                                java.sql.Time startTime = rs.getTime("TIME_START");
                                java.sql.Time endTime = rs.getTime("TIME_END");
                                int studentCount = rs.getInt("STUDENT_COUNT");
                                String courseName = rs.getString("COURSE_NAME");

                                Schedule schedule = new Schedule(schedId, dayOfWeek, startTime, endTime, studentCount);
                                schedule.setCourseName(courseName); 

                                if (!groupedSchedules.containsKey(courseName)) {
                                    groupedSchedules.put(courseName, new java.util.ArrayList<>());
                                }
                                groupedSchedules.get(courseName).add(schedule);

                                if (todayName.equalsIgnoreCase(dayOfWeek)) {
                                    LocalTime start = startTime.toLocalTime();
                                    LocalTime end = endTime.toLocalTime();
                                    
                                    if (!currentTime.isBefore(start) && !currentTime.isAfter(end)) {
                                        currentClass = schedule; 
                                    } else if (currentTime.isBefore(start)) {
                                        upcomingClassesToday.add(schedule); 
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LOGGER.log(Level.WARNING, "No Instructor ID found for User ID: {0}", userId);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Database connection error in InstructorDashboardServlet: " + e.getMessage(), e);
            request.setAttribute("errorMessage", "Database connection error: " + e.getMessage());
        }

        LOGGER.log(Level.INFO, "Schedule loaded successfully. Courses mapped: {0}", groupedSchedules.size());
        
        request.setAttribute("groupedSchedules", groupedSchedules);
        request.setAttribute("currentClass", currentClass);
        request.setAttribute("upcomingClassesToday", upcomingClassesToday);
        
        request.getRequestDispatcher("/InstructorDashboard.jsp").forward(request, response);
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
                LOGGER.log(Level.INFO, "Schedule deleted: {0} by User: {1}", new Object[]{scheduleId, userId});

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Instructor Schedule Deletion Failed: " + e.getMessage(), e);
            }

            response.sendRedirect("InstructorDashboardServlet");
            return; 
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
            LOGGER.log(Level.INFO, "Schedule added: {0} for Course: {1}", new Object[]{nextSchedId, courseId});

            response.sendRedirect("InstructorDashboardServlet");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Instructor Schedule Addition Failed: " + e.getMessage(), e);
            response.sendRedirect("InstructorDashboard.jsp?error=true");
        }
    }
}