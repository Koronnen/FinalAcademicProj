package controller;

import java.io.IOException;
import java.sql.*;
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

public class AdminDashboardServlet extends HttpServlet {
    
    private boolean isAuthorizedAdmin(HttpServletRequest request) {
        System.out.println("reached isAuthorizedAdmin");
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("session is null");
            return false;
        }

        // Retrieve the role object from the session
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof Integer) {
            int userType = (Integer) roleObj;
            System.out.println("usertype: " + userType);
            return userType == 1; // True if they are an Admin
        }
        System.out.println("False");
        return false;
    }
    
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

    // Helper method to generate sequential custom alphanumeric string IDs
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
        // Session Role Verification Check
        if (!isAuthorizedAdmin(request)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("loginError", "Unauthorized access. Administrator privileges required.");
            }
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return; // Terminate execution immediately to stop rendering unauthorized content
        }
        
        List<Map<String, String>> instructors = new ArrayList<>();
        List<Map<String, String>> students = new ArrayList<>();
        List<Map<String, String>> courses = new ArrayList<>();
        List<Map<String, String>> schedulesList = new ArrayList<>();
        Map<String, List<Map<String, String>>> instructorCalendars = new HashMap<>();

        try (Connection conn = getMySQLConnection()) {
            // 1. Fetch Instructors joined with USERS matching new DDL layout
            String instSql = "SELECT i.INST_ID, i.FNAME, i.LNAME, i.EMAIL, u.USER_ID FROM INSTRUCTOR i JOIN USERS u ON i.USER_ID = u.USER_ID";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(instSql)) {
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("instId", rs.getString("INST_ID"));
                    row.put("userId", rs.getString("USER_ID"));
                    row.put("firstName", rs.getString("FNAME"));
                    row.put("lastName", rs.getString("LNAME"));
                    row.put("email", rs.getString("EMAIL"));
                    instructors.add(row);
                }
            }

            // 2. Fetch Students joined with USERS matching new DDL layout
            String stuSql = "SELECT s.STU_ID, s.FNAME, s.LNAME, s.EMAIL, u.USER_ID FROM STUDENT s JOIN USERS u ON s.USER_ID = u.USER_ID";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(stuSql)) {
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("stuId", rs.getString("STU_ID"));
                    row.put("userId", rs.getString("USER_ID"));
                    row.put("firstName", rs.getString("FNAME"));
                    row.put("lastName", rs.getString("LNAME"));
                    row.put("email", rs.getString("EMAIL"));
                    students.add(row);
                }
            }

            // 3. Fetch Curriculum Courses
            String courseSql = "SELECT COURSE_ID, COURSE_CODE, COURSE_NAME, COURSE_LENGTH FROM COURSE";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(courseSql)) {
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("courseId", rs.getString("COURSE_ID"));
                    row.put("courseCode", rs.getString("COURSE_CODE"));
                    row.put("courseName", rs.getString("COURSE_NAME"));
                    row.put("courseLength", rs.getString("COURSE_LENGTH"));
                    courses.add(row);
                }
            }

            // 4. Fetch Active Course Offerings Roster for Student enrollment Dropdowns
            String schedOptionsSql = "SELECT s.SCHED_ID, ic.INST_C_ID, c.COURSE_NAME, i.LNAME, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END " +
                                    "FROM SCHEDULE s JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID " +
                                    "JOIN COURSE c ON ic.COURSE_ID = c.COURSE_ID JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(schedOptionsSql)) {
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("schedId", rs.getString("SCHED_ID"));
                    row.put("instCId", rs.getString("INST_C_ID"));
                    row.put("courseName", rs.getString("COURSE_NAME"));
                    row.put("instructor", rs.getString("LNAME"));
                    row.put("day", rs.getString("DAY_OF_WEEK"));
                    row.put("start", rs.getString("TIME_START"));
                    row.put("end", rs.getString("TIME_END")); // New Column Included
                    schedulesList.add(row);
                }
            }

            // 5. Build Academic Timetable Matrices safely matching the actual DDL constraints
            String calSql = "SELECT s.SCHED_ID, ic.INST_ID, ic.INST_C_ID, ic.COURSE_ID, c.COURSE_CODE, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END " +
                             "FROM SCHEDULE s " +
                             "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID " +
                             "JOIN COURSE c ON ic.COURSE_ID = c.COURSE_ID";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(calSql)) {
                while (rs.next()) {
                    String instId = rs.getString("INST_ID");
                    Map<String, String> appointment = new HashMap<>();
                    appointment.put("schedId", rs.getString("SCHED_ID"));
                    appointment.put("instCId", rs.getString("INST_C_ID"));
                    appointment.put("courseId", rs.getString("COURSE_ID"));
                    appointment.put("code", rs.getString("COURSE_CODE"));
                    appointment.put("day", rs.getString("DAY_OF_WEEK"));
                    appointment.put("start", rs.getString("TIME_START"));
                    appointment.put("end", rs.getString("TIME_END")); // New Column Included
                    instructorCalendars.computeIfAbsent(instId, k -> new ArrayList<>()).add(appointment);
                }
            }

        } catch (Exception e) {
            // EXPOSE THE ROOT ERROR: This prints the exact table/column name causing your crash to your IDE console!
            System.err.println("--- CRITICAL DASHBOARD INITIALIZATION ERROR ---");
            e.printStackTrace();
            
            // Safety Net: Ensure the JSP receives non-null collections even if the database fails
            if (request.getAttribute("instructors") == null) request.setAttribute("instructors", new ArrayList<>());
            if (request.getAttribute("students") == null) request.setAttribute("students", new ArrayList<>());
            if (request.getAttribute("courses") == null) request.setAttribute("courses", new ArrayList<>());
            if (request.getAttribute("schedulesList") == null) request.setAttribute("schedulesList", new ArrayList<>());
            if (request.getAttribute("instructorCalendars") == null) request.setAttribute("instructorCalendars", new HashMap<>());
        }
        
        System.out.println("Instructors size: " + instructors.size());
        System.out.println("Students size: " + students.size());
        System.out.println("Courses size: " + courses.size());
        System.out.println("Schedules size: " + schedulesList.size());
        
        // This line must run out here to render what is available
        request.setAttribute("instructors", instructors);
        request.setAttribute("students", students);
        request.setAttribute("courses", courses);
        request.setAttribute("schedulesList", schedulesList);
        request.setAttribute("instructorCalendars", instructorCalendars);
        request.getRequestDispatcher("AdminDashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Session Role Verification
        if (!isAuthorizedAdmin(request)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("loginError", "Unauthorized write action. Administrator privileges required.");
            }
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return; // Terminate immediately to discard the submission parameters
        }

        String action = request.getParameter("action");
        // pull actual user ID from the confirmed session parameters rather than relying on a hardcoded string
        String authorId = (String) request.getSession().getAttribute("USER_ID"); 

        try (Connection conn = getMySQLConnection()) {
            conn.setAutoCommit(true);
            switch (action) {
                case "addInstructor": {
                    conn.setAutoCommit(false);
                    String rawPassword = request.getParameter("password");
                    String encryptedPassword = Security.encrypt(rawPassword, getServletContext());

                    String nextUserId = generateNextCustomID(conn, "USERS", "USER_ID", "USR");
                    String nextInstId = generateNextCustomID(conn, "INSTRUCTOR", "INST_ID", "INS");

                    String uSql = "INSERT INTO USERS (USER_ID, USER_ROLE, EMAIL, PASSWORD) VALUES (?, 'INSTRUCTOR', ?, ?)";

                    // 1. Write to MySQL
                    try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                        ps.setString(1, nextUserId);
                        ps.setString(2, request.getParameter("email"));
                        ps.setString(3, encryptedPassword);
                        ps.executeUpdate();
                    }

                    // 2. Mirror directly to Derby
                    try (Connection derbyConn = getDerbyConnection()) {
                        derbyConn.setAutoCommit(false);
                        try (PreparedStatement psDerby = derbyConn.prepareStatement(uSql)) {
                            psDerby.setString(1, nextUserId);
                            psDerby.setString(2, request.getParameter("email"));
                            psDerby.setString(3, encryptedPassword);
                            psDerby.executeUpdate();
                        }
                        derbyConn.commit(); // Commit Derby entry
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Derby Mirroring Failed: " + e.getMessage());
                        throw new SQLException("Derby sync failure, rolling back transaction.", e);
                    }

                    // 3. Write to Instructor Table in MySQL
                    String iSql = "INSERT INTO INSTRUCTOR (INST_ID, USER_ID, FNAME, LNAME, EMAIL) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(iSql)) {
                        ps.setString(1, nextInstId);
                        ps.setString(2, nextUserId);
                        ps.setString(3, request.getParameter("firstName"));
                        ps.setString(4, request.getParameter("lastName"));
                        ps.setString(5, request.getParameter("email"));
                        ps.executeUpdate();
                    }

                    conn.commit(); // Commit MySQL entry
                    logAction("Created Alphanumeric Sequential Instructor Entry: " + nextInstId, authorId);
                    break;
                }
                case "editInstructor": {
                    conn.setAutoCommit(false);
                    String userId = request.getParameter("userId");
                    String rawPassword = request.getParameter("password");
                    
                    if (rawPassword != null && !rawPassword.trim().isEmpty()) {
                        String encryptedPassword = Security.encrypt(rawPassword, getServletContext());
                        String uSql = "UPDATE USERS SET EMAIL = ?, PASSWORD = ? WHERE USER_ID = ?";
                        try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                            ps.setString(1, request.getParameter("email"));
                            ps.setString(2, encryptedPassword);
                            ps.setString(3, userId);
                            ps.executeUpdate();
                        }
                    } else {
                        String uSql = "UPDATE USERS SET EMAIL = ? WHERE USER_ID = ?";
                        try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                            ps.setString(1, request.getParameter("email"));
                            ps.setString(2, userId);
                            ps.executeUpdate();
                        }
                    }
                    
                    String iSql = "UPDATE INSTRUCTOR SET FNAME = ?, LNAME = ?, EMAIL = ? WHERE INST_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(iSql)) {
                        ps.setString(1, request.getParameter("firstName"));
                        ps.setString(2, request.getParameter("lastName"));
                        ps.setString(3, request.getParameter("email"));
                        ps.setString(4, request.getParameter("instId"));
                        ps.executeUpdate();
                    }
                    conn.commit();
                    logAction("Modified credentials and user payload data for Instructor: " + request.getParameter("instId"), authorId);
                    break;
                }
                case "deleteInstructor": {
                    conn.setAutoCommit(false);
                    String uId = request.getParameter("userId");
                    String instId = request.getParameter("instId");
                    
                    // Cascade deletes sequentially through child operational mappings
                    String clearSched = "DELETE FROM SCHEDULE WHERE INST_C_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(clearSched)) {
                        ps.setString(1, instId);
                        ps.executeUpdate();
                    }
                    String clearEnroll = "DELETE FROM ENROLLMENT WHERE INST_C_ID IN (SELECT INST_C_ID FROM INSTRUCTORS_COURSE WHERE INST_C_ID = ?)";
                    try (PreparedStatement ps = conn.prepareStatement(clearEnroll)) {
                        ps.setString(1, instId);
                        ps.executeUpdate();
                    }
                    String clearInstCourse = "DELETE FROM INSTRUCTORS_COURSE WHERE INST_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(clearInstCourse)) {
                        ps.setString(1, instId);
                        ps.executeUpdate();
                    }
                    String iSql = "DELETE FROM INSTRUCTOR WHERE INST_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(iSql)) {
                        ps.setString(1, instId);
                        ps.executeUpdate();
                    }
                    String uSql = "DELETE FROM USERS WHERE USER_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                        ps.setString(1, uId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    logAction("Purged Instructor Profile Node: " + instId, authorId);
                    break;
                }
                case "assignInstructorSchedule": {
                    conn.setAutoCommit(false);
                    String courseId = request.getParameter("courseId");
                    String instId = request.getParameter("instId");
                    String dayOfWeek = request.getParameter("dayOfWeek").toUpperCase(); // Must be Uppercase to match ENUM
                    String timeStart = request.getParameter("timeStart");
                    String timeEnd = request.getParameter("timeEnd");

                    // 1. Check or create the association link in INSTRUCTORS_COURSE
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

                    // 2. Insert into SCHEDULE table matching your new 5-column scheme
                    String nextSchedId = generateNextCustomID(conn, "SCHEDULE", "SCHED_ID", "SCH");
                    String insertSched = "INSERT INTO SCHEDULE (SCHED_ID, INST_C_ID, DAY_OF_WEEK, TIME_START, TIME_END) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSched)) {
                        ps.setString(1, nextSchedId);
                        ps.setString(2, instCId);
                        ps.setString(3, dayOfWeek);
                        ps.setString(4, timeStart);
                        ps.setString(5, timeEnd);
                        ps.executeUpdate();
                    }

                    conn.commit();
                    logAction("Assigned course schedule assignment: " + nextSchedId + " (" + timeStart + "-" + timeEnd + ")", authorId);
                    break;
                }

                case "editInstructorSchedule": {
                    conn.setAutoCommit(false);
                    String schedId = request.getParameter("schedId");
                    String courseId = request.getParameter("courseId");
                    String instId = request.getParameter("instId");
                    String dayOfWeek = request.getParameter("dayOfWeek").toUpperCase();
                    String timeStart = request.getParameter("timeStart");
                    String timeEnd = request.getParameter("timeEnd");

                    // Ensure the instructor-course relation is matched/allocated
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

                    // Update the schedule matrix entry
                    String updateSched = "UPDATE SCHEDULE SET INST_C_ID = ?, DAY_OF_WEEK = ?, TIME_START = ?, TIME_END = ? WHERE SCHED_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSched)) {
                        ps.setString(1, instCId);
                        ps.setString(2, dayOfWeek);
                        ps.setString(3, timeStart);
                        ps.setString(4, timeEnd);
                        ps.setString(5, schedId);
                        ps.executeUpdate();
                    }

                    conn.commit();
                    logAction("Modified timetable configuration node: " + schedId, authorId);
                    break;
                }

                case "deleteInstructorSchedule": {
                    conn.setAutoCommit(true);
                    String schedId = request.getParameter("schedId");

                    String deleteSql = "DELETE FROM SCHEDULE WHERE SCHED_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                        ps.setString(1, schedId);
                        ps.executeUpdate();
                    }
                    logAction("Purged instructor timeline block assignment: " + schedId, authorId);
                    break;
                }
                case "addStudent": {
                    conn.setAutoCommit(false); // Begin transaction safely

                    String rawPassword = request.getParameter("password");
                    String encryptedPassword = Security.encrypt(rawPassword, getServletContext());

                    String nextUserId = generateNextCustomID(conn, "USERS", "USER_ID", "USR");
                    String nextStuId = generateNextCustomID(conn, "STUDENT", "STU_ID", "STU");

                    // Fetch parameters defensively
                    String firstName = request.getParameter("firstName");
                    String lastName = request.getParameter("lastName");
                    String email = request.getParameter("email");

                    // Block empty submissions before sending to database
                    if (firstName == null || lastName == null || email == null) {
                        throw new SQLException("Form payload validation failed. Input field names do not match Servlet properties.");
                    }

                    String uSql = "INSERT INTO USERS (USER_ID, USER_ROLE, EMAIL, PASSWORD) VALUES (?, 'STUDENT', ?, ?)";

                    // 1. Write to MySQL
                    try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                        ps.setString(1, nextUserId);
                        ps.setString(2, email);
                        ps.setString(3, encryptedPassword);
                        ps.executeUpdate();
                    }

                    // 2. Mirror directly to Derby
                    try (Connection derbyConn = getDerbyConnection()) {
                        derbyConn.setAutoCommit(false);
                        try (PreparedStatement psDerby = derbyConn.prepareStatement(uSql)) {
                            psDerby.setString(1, nextUserId);
                            psDerby.setString(2, email);
                            psDerby.setString(3, encryptedPassword);
                            psDerby.executeUpdate();
                        }
                        derbyConn.commit(); // Commit Derby entry
                    } catch (Exception e) {
                        System.err.println("Derby Mirroring Failed: " + e.getMessage());
                        throw new SQLException("Derby sync failure, rolling back transaction.", e);
                    }

                    // 3. Write to Student Table in MySQL
                    String sSql = "INSERT INTO STUDENT (STU_ID, USER_ID, FNAME, LNAME, EMAIL) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sSql)) {
                        ps.setString(1, nextStuId);
                        ps.setString(2, nextUserId);
                        ps.setString(3, firstName.trim()); 
                        ps.setString(4, lastName.trim());  
                        ps.setString(5, email.trim());
                        ps.executeUpdate();
                    }

                    conn.commit(); // Commit saved parameters cleanly to MySQL
                    logAction("Created Student Profile: " + nextStuId, authorId);
                    break;
                }
                case "editStudent": {
                    conn.setAutoCommit(false);
                    String userId = request.getParameter("userId");
                    String rawPassword = request.getParameter("password");
                    
                    if (rawPassword != null && !rawPassword.trim().isEmpty()) {
                        String encryptedPassword = Security.encrypt(rawPassword, getServletContext());
                        String uSql = "UPDATE USERS SET EMAIL = ?, PASSWORD = ? WHERE USER_ID = ?";
                        try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                            ps.setString(1, request.getParameter("email"));
                            ps.setString(2, encryptedPassword);
                            ps.setString(3, userId);
                            ps.executeUpdate();
                        }
                    } else {
                        String uSql = "UPDATE USERS SET EMAIL = ? WHERE USER_ID = ?";
                        try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                            ps.setString(1, request.getParameter("email"));
                            ps.setString(2, userId);
                            ps.executeUpdate();
                        }
                    }
                    
                    String sSql = "UPDATE STUDENT SET FNAME = ?, LNAME = ?, EMAIL = ? WHERE STU_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sSql)) {
                        ps.setString(1, request.getParameter("firstName"));
                        ps.setString(2, request.getParameter("lastName"));
                        ps.setString(3, request.getParameter("email"));
                        ps.setString(4, request.getParameter("stuId"));
                        ps.executeUpdate();
                    }
                    conn.commit();
                    logAction("Modified user payload record criteria for Student ID: " + request.getParameter("stuId"), authorId);
                    break;
                }
                case "deleteStudent": {
                    conn.setAutoCommit(false);
                    String uId = request.getParameter("userId");
                    String stuId = request.getParameter("stuId");
                    
                    String clearStuCourse = "DELETE FROM STUDENT_COURSE WHERE STU_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(clearStuCourse)) {
                        ps.setString(1, stuId);
                        ps.executeUpdate();
                    }
                    String clearEnroll = "DELETE FROM ENROLLMENT WHERE STU_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(clearEnroll)) {
                        ps.setString(1, stuId);
                        ps.executeUpdate();
                    }
                    String sSql = "DELETE FROM STUDENT WHERE STU_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sSql)) {
                        ps.setString(1, stuId);
                        ps.executeUpdate();
                    }
                    String uSql = "DELETE FROM USERS WHERE USER_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                        ps.setString(1, uId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    logAction("Purged Structural Student Profile Ledger Record: " + stuId, authorId);
                    break;
                }
                case "enrollStudent": {
                    conn.setAutoCommit(false);
                    String stuId = request.getParameter("stuId");
                    String instCId = request.getParameter("instCId");
                    
                    String courseId = "";
                    String findCourseSql = "SELECT COURSE_ID FROM INSTRUCTORS_COURSE WHERE INST_C_ID = ? LIMIT 1";
                    try (PreparedStatement ps = conn.prepareStatement(findCourseSql)) {
                        ps.setString(1, instCId);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) courseId = rs.getString("COURSE_ID");
                    }

                    // Populate operational synchronization across standard associative entities
                    String nextStcId = generateNextCustomID(conn, "STUDENT_COURSE", "STU_C_ID", "STC");
                    String scSql = "INSERT INTO STUDENT_COURSE (STU_C_ID, COURSE_ID, STU_ID) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(scSql)) {
                        ps.setString(1, nextStcId);
                        ps.setString(2, courseId);
                        ps.setString(3, stuId);
                        ps.executeUpdate();
                    }

                    String nextEnrId = generateNextCustomID(conn, "ENROLLMENT", "STU_EN_ID", "ENR");
                    String sSql = "INSERT INTO ENROLLMENT (STU_EN_ID, STU_ID, INST_C_ID) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sSql)) {
                        ps.setString(1, nextEnrId);
                        ps.setString(2, stuId);
                        ps.setString(3, instCId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    logAction("Affiliated Student ID " + stuId + " to Active Course Offering ENR Token: " + nextEnrId, authorId);
                    break;
                }
                case "addCourse": {
                    // Safely enforce transaction control
                    conn.setAutoCommit(false); 

                    String nextCourseId = generateNextCustomID(conn, "COURSE", "COURSE_ID", "CRS");
                    String cSql = "INSERT INTO COURSE (COURSE_ID, COURSE_CODE, COURSE_NAME, COURSE_LENGTH) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(cSql)) {
                        ps.setString(1, nextCourseId);
                        ps.setString(2, request.getParameter("courseCode"));
                        ps.setString(3, request.getParameter("courseName"));
                        ps.setInt(4, Integer.parseInt(request.getParameter("courseLength")));
                        ps.executeUpdate();
                    }

                    // COMMIT THE TRANSACTION SO IT REFLECTS IN THE DB
                    conn.commit(); 
                    logAction("Registered Alphanumeric Sequential Course Node: " + nextCourseId, authorId);
                    break;
                }
                case "editCourse": {
                    // Safely enforce transaction control
                    conn.setAutoCommit(false); 

                    String cSql = "UPDATE COURSE SET COURSE_CODE = ?, COURSE_NAME = ?, COURSE_LENGTH = ? WHERE COURSE_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(cSql)) {
                        ps.setString(1, request.getParameter("courseCode"));
                        ps.setString(2, request.getParameter("courseName"));
                        ps.setInt(3, Integer.parseInt(request.getParameter("courseLength")));
                        ps.setString(4, request.getParameter("courseId"));
                        ps.executeUpdate();
                    }

                    // COMMIT THE TRANSACTION SO IT REFLECTS IN THE DB
                    conn.commit(); 
                    logAction("Altered course core schema descriptor configurations for identification key: " + request.getParameter("courseId"), authorId);
                    break;
                }
                case "deleteCourse": {
                    conn.setAutoCommit(false);
                    String courseId = request.getParameter("courseId");
                    
                    String clearStuC = "DELETE FROM STUDENT_COURSE WHERE COURSE_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(clearStuC)) {
                        ps.setString(1, courseId);
                        ps.executeUpdate();
                    }
                    String clearSched = "DELETE FROM SCHEDULE WHERE INST_C_ID IN (SELECT INST_ID FROM INSTRUCTORS_COURSE WHERE COURSE_ID = ?)";
                    try (PreparedStatement ps = conn.prepareStatement(clearSched)) {
                        ps.setString(1, courseId);
                        ps.executeUpdate();
                    }
                    String clearEnroll = "DELETE FROM ENROLLMENT WHERE INST_C_ID IN (SELECT INST_C_ID FROM INSTRUCTORS_COURSE WHERE COURSE_ID = ?)";
                    try (PreparedStatement ps = conn.prepareStatement(clearEnroll)) {
                        ps.setString(1, courseId);
                        ps.executeUpdate();
                    }
                    String clearInstCourse = "DELETE FROM INSTRUCTORS_COURSE WHERE COURSE_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(clearInstCourse)) {
                        ps.setString(1, courseId);
                        ps.executeUpdate();
                    }
                    String cSql = "DELETE FROM COURSE WHERE COURSE_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(cSql)) {
                        ps.setString(1, courseId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    logAction("Purged operational active curriculum catalog course index: " + courseId, authorId);
                    break;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Transaction Aborted!");
            e.printStackTrace();
            // Send the error to the browser screen
            response.getWriter().println("SQL ERROR: " + e.getMessage());
            return; // Stop the redirect
        } catch (Exception e) {
            e.printStackTrace();
            // Send the error to the browser screen
            response.getWriter().println("GENERAL ERROR: " + e.getMessage());
            return; // Stop the redirect
        }

        // If we make it here, NO errors happened!
        response.sendRedirect("AdminDashboardServlet");
    }
}