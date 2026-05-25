package controller;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Javo
 */
@WebServlet(name = "InstructorProfileServlet", urlPatterns = {"/InstructorProfileServlet"})
public class InstructorProfileServlet extends HttpServlet {

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
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            response.sendRedirect("index.jsp");
            return;
        }

        String userId = (String) session.getAttribute("USER_ID");

        int totalSchedulesCount = 0;
        int totalSubjectsCount = 0;
        List<String> subjectList = new ArrayList<>();
        
        int totalHoursCount = 0; 

        if (userId != null) {
            try (Connection conn = getMySQLConnection()) {
                
                String countSql = "SELECT COUNT(s.SCHED_ID) " +
                                  "FROM SCHEDULE s " +
                                  "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID " +
                                  "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID " +
                                  "WHERE i.USER_ID = ?";
                
                try (PreparedStatement psCount = conn.prepareStatement(countSql)) {
                    psCount.setString(1, userId);
                    try (ResultSet rsCount = psCount.executeQuery()) {
                        if (rsCount.next()) {
                            totalSchedulesCount = rsCount.getInt(1);
                        }
                    }
                }

                String subjectSql = "SELECT DISTINCT c.COURSE_NAME " +
                                    "FROM COURSE c " +
                                    "JOIN INSTRUCTORS_COURSE ic ON c.COURSE_ID = ic.COURSE_ID " +
                                    "JOIN SCHEDULE s ON ic.INST_C_ID = s.INST_C_ID " + 
                                    "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID " +
                                    "WHERE i.USER_ID = ?";
                
                try (PreparedStatement psSubj = conn.prepareStatement(subjectSql)) {
                    psSubj.setString(1, userId);
                    try (ResultSet rsSubj = psSubj.executeQuery()) {
                        while (rsSubj.next()) {
                            subjectList.add(rsSubj.getString(1));
                        }
                    }
                }
                totalSubjectsCount = subjectList.size();

                String hoursSql = "SELECT SUM(TIMESTAMPDIFF(MINUTE, s.TIME_START, s.TIME_END)) " +
                                  "FROM SCHEDULE s " +
                                  "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID " +
                                  "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID " +
                                  "WHERE i.USER_ID = ?";
                
                try (PreparedStatement psHours = conn.prepareStatement(hoursSql)) {
                    psHours.setString(1, userId);
                    try (ResultSet rsHours = psHours.executeQuery()) {
                        if (rsHours.next()) {
                            int totalMinutes = rsHours.getInt(1);
                            
                            totalHoursCount = (int) Math.ceil(totalMinutes / 60.0);
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
        request.setAttribute("subjectList", subjectList);

        request.getRequestDispatcher("InstructorProfile.jsp").forward(request, response);
    }
}