package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || !"STUDENT".equalsIgnoreCase((String) session.getAttribute("USER_ROLE"))) {
            response.sendRedirect("index.jsp");
            return;
        }

        String usrID = (String) session.getAttribute("USER_ID");
        String displayName = "Student";
        String dbStudentId = "", dbFName = "", dbLName = "", dbEmail = "";
        
        try (Connection sqlConn = getMySQLConnection()) {
            // 1. Attempt to grab profile data from primary MySQL Database
            String sqlQuery = "SELECT STU_ID, FNAME, LNAME, EMAIL FROM STUDENT WHERE USER_ID = ?";
            try (PreparedStatement ps = sqlConn.prepareStatement(sqlQuery)) {
                ps.setString(1, usrID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dbStudentId = rs.getString("STU_ID");
                        dbFName = rs.getString("FNAME");
                        dbLName = rs.getString("LNAME");
                        dbEmail = rs.getString("EMAIL");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // MySQL failed or record missing, attempting secondary Apache Derby fallback authentication
            try (Connection derbyConn = getDerbyConnection()) {
                String derbyQuery = "SELECT EMAIL FROM USERS WHERE USER_ID = ?";
                try (PreparedStatement psD = derbyConn.prepareStatement(derbyQuery)) {
                    psD.setString(1, usrID);
                    try (ResultSet rsD = psD.executeQuery()) {
                        if (rsD.next()) {
                            dbEmail = rsD.getString("EMAIL");
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Set dynamic presentation greeting logic
        if (dbFName != null && !dbFName.trim().isEmpty()) {
            displayName = dbFName;
        }
        
        request.setAttribute("displayName", displayName);
        request.setAttribute("stuId", dbStudentId);
        request.setAttribute("fname", dbFName);
        request.setAttribute("lname", dbLName);
        request.setAttribute("email", dbEmail);

        // 2. Load Available Courses, Assigned Instructors, and Schedules from MySQL
        List<Map<String, Object>> courseCatalog = new ArrayList<>();
        try (Connection sqlConn = getMySQLConnection()) {
            String catalogQuery = "SELECT c.COURSE_ID, c.COURSE_CODE, c.COURSE_NAME, " +
                                  "ic.INST_C_ID, i.FNAME AS iFName, i.LNAME AS iLName, " +
                                  "s.SCHED_ID, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END " +
                                  "FROM COURSE c " +
                                  "JOIN INSTRUCTORS_COURSE ic ON c.COURSE_ID = ic.COURSE_ID " +
                                  "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID " +
                                  "LEFT JOIN SCHEDULE i_sched ON ic.INST_C_ID = i_sched.INST_C_ID " +
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
        
        request.setAttribute("availableCourses", courseCatalog);
        request.getRequestDispatcher("/StudentDashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || !"STUDENT".equalsIgnoreCase((String) session.getAttribute("USER_ROLE"))) {
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
                String inputEmail = request.getParameter("email");

                String finalFName = (inputFName != null) ? inputFName.trim() : "";
                String finalLName = (inputLName != null) ? inputLName.trim() : "";
                String finalEmail = (inputEmail != null) ? inputEmail.trim() : "";

                String updateSql = "UPDATE STUDENT SET FNAME = ?, LNAME = ?, EMAIL = ? WHERE USER_ID = ?";
                try (PreparedStatement ps = sqlConn.prepareStatement(updateSql)) {
                    ps.setString(1, finalFName);
                    ps.setString(2, finalLName);
                    ps.setString(3, finalEmail);
                    ps.setString(4, usrID);
                    ps.executeUpdate();
                }                
                logAction("Applied Student Profile Changes", authorID);
            } 
            else if ("clearProfile".equals(action)) {
                // Clear the optional profile details safely while observing database NOT NULL constraints
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
                    // Generate a tracking unique key constraint string ID matching CHAR(9) length constraints
                    String enrollmentId = UUID.randomUUID().toString().substring(0, 9).toUpperCase();
                    
                    String enrollSql = "INSERT INTO ENROLLMENT (STU_EN_ID, STU_ID, INST_C_ID) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = sqlConn.prepareStatement(enrollSql)) {
                        ps.setString(1, enrollmentId);
                        ps.setString(2, stuId);
                        ps.setString(3, instCourseId);
                        ps.executeUpdate();
                    }
                    logAction(authorID + "Successfully Enrolled to " + instCourseId, authorID);                    
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Post-Redirect-Get pattern clears transaction data and pulls fresh state in doGet
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