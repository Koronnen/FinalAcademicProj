package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; // Added import for Statement
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    
    private Connection getPostgresConnection() throws SQLException, ClassNotFoundException {
        String driver = getServletContext().getInitParameter("postgres.jdbcClassName");
        String url = getServletContext().getInitParameter("postgres.jdbcDriverURL") + "://" +
                     getServletContext().getInitParameter("postgres.dbHostName") + ":" +
                     getServletContext().getInitParameter("postgres.dbPort") + "/" +
                     getServletContext().getInitParameter("postgres.databaseName");
        String user = getServletContext().getInitParameter("postgres.dbUserName");
        String pass = getServletContext().getInitParameter("postgres.dbPassword");

        Class.forName(driver);
        return DriverManager.getConnection(url, user, pass);
    }

    // New custom ID generator method
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            response.sendRedirect("index.jsp");
            return;
        }

        String usrID = (String) session.getAttribute("USER_ID");
        String displayName = "Student";
        
        Map<String, String> profile = new HashMap<>();
        List<Map<String, String>> enrolledCourses = new ArrayList<>();
        List<Map<String, Object>> courseCatalog = new ArrayList<>();
        
        try (Connection sqlConn = getMySQLConnection()) {
            String sqlQuery = "SELECT STU_ID, FNAME, LNAME FROM STUDENT WHERE USER_ID = ?";
            try (PreparedStatement ps = sqlConn.prepareStatement(sqlQuery)) {
                ps.setString(1, usrID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        profile.put("stuId", rs.getString("STU_ID"));
                        profile.put("firstName", rs.getString("FNAME"));
                        profile.put("lastName", rs.getString("LNAME"));
                        
                        if (rs.getString("FNAME") != null && !rs.getString("FNAME").trim().isEmpty()) {
                            displayName = rs.getString("FNAME");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String studentId = profile.getOrDefault("stuId", "");

        try (Connection sqlConn = getMySQLConnection()) {
            if (!studentId.isEmpty()) {
                String enrolledSql = "SELECT c.COURSE_CODE, c.COURSE_NAME, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END " +
                                    "FROM ENROLLMENT e " +
                                    "JOIN SCHEDULE s ON e.SCHED_ID = s.SCHED_ID " +
                                    "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID " +
                                    "JOIN COURSE c ON ic.COURSE_ID = c.COURSE_ID " +
                                    "WHERE e.STU_ID = ?";
                try (PreparedStatement ps = sqlConn.prepareStatement(enrolledSql)) {
                    ps.setString(1, studentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String, String> row = new HashMap<>();
                            row.put("courseCode", rs.getString("COURSE_CODE"));
                            while (rs.next()) {
                                Map<String, String> r = new HashMap<>();
                                r.put("courseCode", rs.getString("COURSE_CODE"));
                                r.put("courseName", rs.getString("COURSE_NAME"));

                                // Smooth, verified string building string extraction
                                String day = rs.getString("DAY_OF_WEEK");
                                String start = rs.getString("TIME_START");
                                String end = rs.getString("TIME_END");
                                r.put("timeDetails", day + " (" + start + " - " + end + ")");

                                enrolledCourses.add(r);
                            }
                        }
                    }
                }
            }

            String catalogQuery = "SELECT c.COURSE_ID, c.COURSE_CODE, c.COURSE_NAME, " +
                                  "ic.INST_C_ID, i.FNAME AS iFName, i.LNAME AS iLName, " +
                                  "s.SCHED_ID, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END " +
                                  "FROM COURSE c " +
                                  "JOIN INSTRUCTORS_COURSE ic ON c.COURSE_ID = ic.COURSE_ID " +
                                  "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID " +
                                  "LEFT JOIN SCHEDULE s ON ic.INST_C_ID = s.INST_C_ID";
            
            try (PreparedStatement ps = sqlConn.prepareStatement(catalogQuery);
                 ResultSet rs = ps.executeQuery()) {
                
                Map<String, Map<String, Object>> aggregationMap = new HashMap<>();
                
                while (rs.next()) {
                    String instCourseId = rs.getString("INST_C_ID");
                    
                    if (!aggregationMap.containsKey(instCourseId)) {
                        Map<String, Object> courseDetails = new HashMap<>();
                        courseDetails.put("instCourseId", instCourseId);
                        courseDetails.put("courseCode", rs.getString("COURSE_CODE"));
                        courseDetails.put("courseName", rs.getString("COURSE_NAME"));
                        courseDetails.put("instructorName", "Prof. " + rs.getString("iFName") + " " + rs.getString("iLName"));
                        courseDetails.put("schedulesList", new ArrayList<Map<String, String>>());
                        
                        aggregationMap.put(instCourseId, courseDetails);
                    }
                    
                    String schedId = rs.getString("SCHED_ID");
                    if (schedId != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, String>> scheds = (List<Map<String, String>>) aggregationMap.get(instCourseId).get("schedulesList");
                        Map<String, String> timeSlot = new HashMap<>();
                        timeSlot.put("schedId", schedId);
                        timeSlot.put("timeDetails", rs.getString("DAY_OF_WEEK") + " (" + rs.getString("TIME_START") + " - " + rs.getString("TIME_END") + ")");
                        scheds.add(timeSlot);
                    }
                }
                courseCatalog.addAll(aggregationMap.values());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        request.setAttribute("displayName", displayName);
        request.setAttribute("profile", profile);
        request.setAttribute("enrolledCourses", enrolledCourses);
        request.setAttribute("availableCourses", courseCatalog);
        
        request.getRequestDispatcher("/StudentDashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Get the session if it exists
        HttpSession session = request.getSession(false);

        // 2. Fixed: Now consistently checks for USER_ID instead of USER_ROLE
        if (session == null || session.getAttribute("USER_ID") == null) {
            response.sendRedirect("index.jsp");
            return;
        }

        String usrID = (String) session.getAttribute("USER_ID");
        String action = request.getParameter("action");

        try (Connection sqlConn = getMySQLConnection()) {

            String stuId = "";
            String authorID = "";
            String findStudentSql = "SELECT STU_ID FROM STUDENT WHERE USER_ID = ?";
            try (PreparedStatement ps = sqlConn.prepareStatement(findStudentSql)) {
                ps.setString(1, usrID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stuId = rs.getString("STU_ID");
                        authorID = rs.getString("STU_ID");                        
                    }
                }
            }

            if ("updateProfile".equals(action)) {
                String inputFName = request.getParameter("fname");
                String inputLName = request.getParameter("lname");

                String finalFName = (inputFName != null) ? inputFName.trim() : "";
                String finalLName = (inputLName != null) ? inputLName.trim() : "";

                String updateSql = "UPDATE STUDENT SET FNAME = ?, LNAME = ? WHERE USER_ID = ?";
                try (PreparedStatement ps = sqlConn.prepareStatement(updateSql)) {
                    ps.setString(1, finalFName);
                    ps.setString(2, finalLName);
                    ps.setString(3, usrID);
                    ps.executeUpdate();
                }                
                logAction("Applied Student Profile Changes", authorID);
            } 
            else if ("clearProfile".equals(action)) {
                String clearSql = "UPDATE STUDENT SET FNAME = '', LNAME = '' WHERE USER_ID = ?";
                try (PreparedStatement ps = sqlConn.prepareStatement(clearSql)) {
                    ps.setString(1, usrID);
                    ps.executeUpdate();
                }
                logAction("Cleared " + authorID + " Student Profile", authorID);
            }
            else if ("enrollCourse".equals(action)) {
                String instCourseId = request.getParameter("instCourseId");

                if (!stuId.isEmpty() && instCourseId != null && !instCourseId.isEmpty()) {
                    // Replaced UUID generation with your custom ID generator
                    String enrollmentId = generateNextCustomID(sqlConn, "ENROLLMENT", "STU_EN_ID", "ENR");

                    String enrollSql = "INSERT INTO ENROLLMENT (STU_EN_ID, STU_ID, INST_C_ID) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = sqlConn.prepareStatement(enrollSql)) {
                        ps.setString(1, enrollmentId);
                        ps.setString(2, stuId);
                        ps.setString(3, instCourseId);
                        ps.executeUpdate();
                    }
                    logAction(authorID + " Successfully Enrolled to " + instCourseId, authorID);                    
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Redirects seamlessly back to the doGet to re-render the page with new data
        response.sendRedirect(request.getContextPath() + "/StudentServlet");
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
        return "Student Dashboard Controller";
    }
}