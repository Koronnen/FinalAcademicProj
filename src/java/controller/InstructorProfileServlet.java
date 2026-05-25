/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.Schedule;

/**
 *
 * @author Javo
 */

@WebServlet(name = "InstructorProfileServlet", urlPatterns = {"/InstructorProfileServlet"})
public class InstructorProfileServlet extends HttpServlet {
    
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorizedInstructor(request)) {
            HttpSession checkSession = request.getSession(false);
            if (checkSession == null || checkSession.getAttribute("USER_ID") == null) {
                request.getRequestDispatcher("/ErrorPages/error_session.jsp").forward(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }

        // ADDED HERE: Verify login status via USER_ID session token
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        String userId = (String) session.getAttribute("USER_ID");

        int totalSchedulesCount = 0;
        int totalSubjectsCount = 0;
        int totalHoursCount = 0;

        Map<String, List<Schedule>> groupedSchedules = new LinkedHashMap<>();

        if (userId != null) {
            try (Connection conn = getMySQLConnection()) {

                String countSql = "SELECT COUNT(s.SCHED_ID) "
                        + "FROM SCHEDULE s "
                        + "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID "
                        + "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID "
                        + "WHERE i.USER_ID = ?";

                try (PreparedStatement psCount = conn.prepareStatement(countSql)) {
                    psCount.setString(1, userId);
                    try (ResultSet rsCount = psCount.executeQuery()) {
                        if (rsCount.next()) {
                            totalSchedulesCount = rsCount.getInt(1);
                        }
                    }
                }

                // 2. Get Total Unique Subjects Count
                String subjectSql = "SELECT COUNT(DISTINCT c.COURSE_NAME) "
                        + "FROM COURSE c "
                        + "JOIN INSTRUCTORS_COURSE ic ON c.COURSE_ID = ic.COURSE_ID "
                        + "JOIN SCHEDULE s ON ic.INST_C_ID = s.INST_C_ID "
                        + "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID "
                        + "WHERE i.USER_ID = ?";

                try (PreparedStatement psSubj = conn.prepareStatement(subjectSql)) {
                    psSubj.setString(1, userId);
                    try (ResultSet rsSubj = psSubj.executeQuery()) {
                        if (rsSubj.next()) {
                            totalSubjectsCount = rsSubj.getInt(1);
                        }
                    }
                }

                String hoursSql = "SELECT SUM(TIMESTAMPDIFF(MINUTE, s.TIME_START, s.TIME_END)) "
                        + "FROM SCHEDULE s "
                        + "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID "
                        + "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID "
                        + "WHERE i.USER_ID = ?";

                try (PreparedStatement psHours = conn.prepareStatement(hoursSql)) {
                    psHours.setString(1, userId);
                    try (ResultSet rsHours = psHours.executeQuery()) {
                        if (rsHours.next()) {
                            int totalMinutes = rsHours.getInt(1);
                            totalHoursCount = (int) Math.ceil(totalMinutes / 60.0);
                        }
                    }
                }

                String groupedSql = "SELECT c.COURSE_NAME, s.SCHED_ID, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END, "
                        + "COUNT(e.STU_ID) AS STUDENT_COUNT "
                        + "FROM COURSE c "
                        + "JOIN INSTRUCTORS_COURSE ic ON c.COURSE_ID = ic.COURSE_ID "
                        + "JOIN SCHEDULE s ON ic.INST_C_ID = s.INST_C_ID "
                        + "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID "
                        + "LEFT JOIN ENROLLMENT e ON s.SCHED_ID = e.SCHED_ID "
                        + "WHERE i.USER_ID = ? "
                        + "GROUP BY c.COURSE_NAME, s.SCHED_ID, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END "
                        + "ORDER BY c.COURSE_NAME, "
                        + "FIELD(s.DAY_OF_WEEK, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'), "
                        + "s.TIME_START";

                try (PreparedStatement psGrouped = conn.prepareStatement(groupedSql)) {
                    psGrouped.setString(1, userId);

                    try (ResultSet rsGrouped = psGrouped.executeQuery()) {
                        while (rsGrouped.next()) {
                            String courseName = rsGrouped.getString("COURSE_NAME");

                            Schedule scheduleDetails = new Schedule(
                                    rsGrouped.getString("SCHED_ID"),
                                    rsGrouped.getString("DAY_OF_WEEK"),
                                    rsGrouped.getTime("TIME_START"),
                                    rsGrouped.getTime("TIME_END"),
                                    rsGrouped.getInt("STUDENT_COUNT")
                            );

                            if (!groupedSchedules.containsKey(courseName)) {
                                groupedSchedules.put(courseName, new ArrayList<>());
                            }

                            groupedSchedules.get(courseName).add(scheduleDetails);
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("Error fetching profile stats from MySQL: " + e.getMessage());
                e.printStackTrace();
            }
        }

        request.setAttribute("totalSchedules", totalSchedulesCount);
        request.setAttribute("totalSubjects", totalSubjectsCount);
        request.setAttribute("totalHours", totalHoursCount);
        request.setAttribute("groupedSchedules", groupedSchedules);

        request.getRequestDispatcher("InstructorProfile.jsp").forward(request, response);
    }
}