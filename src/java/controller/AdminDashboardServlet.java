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
import javax.servlet.http.HttpSession; // Imported for Session Tracking


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
            return;
        }

        // ADDED HERE: Verify login status via USER_ID session token
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            response.sendRedirect("index.jsp");
            return;
        }

        List<Map<String, String>> instructors = new ArrayList<>();
        List<Map<String, String>> students = new ArrayList<>();
        List<Map<String, String>> courses = new ArrayList<>();
        List<Map<String, String>> schedulesList = new ArrayList<>();
        Map<String, List<Map<String, String>>> instructorCalendars = new HashMap<>();
        Map<String, List<Map<String, String>>> studentSchedules = new HashMap<>();

        try (Connection conn = getMySQLConnection()) {
            // 1. Fetch Instructors joined with USERS matching updated layout (EMAIL extracted from USERS)
            String instSql = "SELECT i.INST_ID, i.FNAME, i.LNAME, u.EMAIL, u.USER_ID FROM INSTRUCTOR i JOIN USERS u ON i.USER_ID = u.USER_ID";

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
            // 2. Fetch Students joined with USERS matching updated layout (EMAIL extracted from USERS)
            String stuSql = "SELECT s.STU_ID, s.FNAME, s.LNAME, u.EMAIL, u.USER_ID FROM STUDENT s JOIN USERS u ON s.USER_ID = u.USER_ID";
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
                    row.put("end", rs.getString("TIME_END"));
                    schedulesList.add(row);
                }
            }

            // 5. Build Academic Timetable Matrices safely matching the updated DDL constraints
            String calSql = "SELECT s.SCHED_ID, ic.INST_ID, ic.INST_C_ID, ic.COURSE_ID, c.COURSE_CODE, c.COURSE_NAME, s.DAY_OF_WEEK, s.TIME_START, s.TIME_END " +
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

                    // CHANGED: Fixed keys to match JS template strings (${s.courseCode}, ${s.courseName}, ${s.classDay})
                    appointment.put("courseCode", rs.getString("COURSE_CODE"));
                    appointment.put("courseName", rs.getString("COURSE_NAME"));
                    appointment.put("classDay", rs.getString("DAY_OF_WEEK"));

                    appointment.put("timeStart", rs.getString("TIME_START"));
                    appointment.put("timeEnd", rs.getString("TIME_END"));

                    instructorCalendars.computeIfAbsent(instId, k -> new ArrayList<>()).add(appointment);
                }
            }

            // Fetch the courses each student is taking (Schedules and courses determined cleanly via ENROLLMENT table)
            String schedSql = "SELECT e.STU_EN_ID, e.STU_ID, s.SCHED_ID, " +
                                "c.COURSE_ID, c.COURSE_CODE, c.COURSE_NAME, " +
                                "s.DAY_OF_WEEK, s.TIME_START, s.TIME_END, " +
                                "i.LNAME, i.FNAME " +
                                "FROM ENROLLMENT e " +
                                "JOIN SCHEDULE s ON e.SCHED_ID = s.SCHED_ID " +
                                "JOIN INSTRUCTORS_COURSE ic ON s.INST_C_ID = ic.INST_C_ID " +
                                "JOIN INSTRUCTOR i ON ic.INST_ID = i.INST_ID " +
                                "JOIN COURSE c ON ic.COURSE_ID = c.COURSE_ID";

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(schedSql)) {
                while (rs.next()) {
                    String stuId = rs.getString("STU_ID");
                    Map<String, String> schedData = new HashMap<>();
                    // To maintain naming conventions and visual mapping for layout, we save STU_EN_ID as "stuCId"
                    // because the frontend views lookups map with 'stuCId' expressions.
                    schedData.put("stuCId", rs.getString("STU_EN_ID"));
                    schedData.put("schedId", rs.getString("SCHED_ID"));
                    schedData.put("courseId", rs.getString("COURSE_ID"));
                    schedData.put("courseCode", rs.getString("COURSE_CODE"));
                    schedData.put("courseName", rs.getString("COURSE_NAME"));
                    schedData.put("classDay", rs.getString("DAY_OF_WEEK"));
                    schedData.put("timeStart", rs.getString("TIME_START"));
                    schedData.put("timeEnd", rs.getString("TIME_END"));
                    schedData.put("instLName", rs.getString("LNAME"));
                    schedData.put("instFName", rs.getString("FNAME"));

                    studentSchedules.computeIfAbsent(stuId, k -> new ArrayList<>()).add(schedData);
                }
            }

        } catch (Exception e) {
            System.err.println("--- CRITICAL DASHBOARD INITIALIZATION ERROR ---");
            e.printStackTrace();

            if (request.getAttribute("instructors") == null) request.setAttribute("instructors", new ArrayList<>());
            if (request.getAttribute("students") == null) request.setAttribute("students", new ArrayList<>());
            if (request.getAttribute("courses") == null) request.setAttribute("courses", new ArrayList<>());
            if (request.getAttribute("schedulesList") == null) request.setAttribute("schedulesList", new ArrayList<>());
            if (request.getAttribute("instructorCalendars") == null) request.setAttribute("instructorCalendars", new HashMap<>());
            if (request.getAttribute("studentSchedules") == null) request.setAttribute("studentSchedules", new HashMap<>());
        }

        request.setAttribute("instructors", instructors);
        request.setAttribute("students", students);
        request.setAttribute("courses", courses);
        request.setAttribute("schedulesList", schedulesList);
        request.setAttribute("instructorCalendars", instructorCalendars);
        request.setAttribute("studentSchedules", studentSchedules);

        request.getRequestDispatcher("AdminDashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorizedAdmin(request)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("loginError", "Unauthorized write action. Administrator privileges required.");
            }
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        String action = request.getParameter("action");
        String authorId = (String) request.getSession(false).getAttribute("USER_ID");
        // ADDED HERE: Verify login status via USER_ID session token
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            response.sendRedirect("index.jsp");
            return;
        }

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

                    // 1. Write to MySQL (USERS contains the single source of truth for email)
                    try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                        ps.setString(1, nextUserId);
                        ps.setString(2, request.getParameter("email"));
                        ps.setString(3, encryptedPassword);
                        ps.executeUpdate();
                    }

                    try (Connection derbyConn = getDerbyConnection()) {
                        derbyConn.setAutoCommit(false);
                        try (PreparedStatement psDerby = derbyConn.prepareStatement(uSql)) {
                            psDerby.setString(1, nextUserId);
                            psDerby.setString(2, request.getParameter("email"));
                            psDerby.setString(3, encryptedPassword);
                            psDerby.executeUpdate();
                        }
                        derbyConn.commit();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Derby Mirroring Failed: " + e.getMessage());
                        throw new SQLException("Derby sync failure, rolling back transaction.", e);
                    }
                    // 3. Write to Instructor Table in MySQL (EMAIL column removed completely)
                    String iSql = "INSERT INTO INSTRUCTOR (INST_ID, USER_ID, FNAME, LNAME) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(iSql)) {
                        ps.setString(1, nextInstId);
                        ps.setString(2, nextUserId);
                        ps.setString(3, request.getParameter("firstName"));
                        ps.setString(4, request.getParameter("lastName"));
                        ps.executeUpdate();
                    }
                    conn.commit();
                    logAction("Created Alphanumeric Sequential Instructor Entry: " + nextInstId, authorId);
                    break;
                }
                case "editInstructor": {
                    conn.setAutoCommit(false);
                    String userId = request.getParameter("userId");
                    String rawPassword = request.getParameter("password");

                    // Update main email address or profile security parameters inside USERS table
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

                    // Update details in INSTRUCTOR table (EMAIL column removed)
                    String iSql = "UPDATE INSTRUCTOR SET FNAME = ?, LNAME = ?, WHERE INST_ID = ?";
                    String cleanISql = "UPDATE INSTRUCTOR SET FNAME = ?, LNAME = ? WHERE INST_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(cleanISql)) {
                        ps.setString(1, request.getParameter("firstName"));
                        ps.setString(2, request.getParameter("lastName"));
                        ps.setString(3, request.getParameter("instId"));
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
                    String clearSched = "DELETE FROM SCHEDULE WHERE INST_C_ID IN (SELECT INST_C_ID FROM INSTRUCTORS_COURSE WHERE INST_ID = ?)";
                    try (PreparedStatement ps = conn.prepareStatement(clearSched)) {
                        ps.setString(1, instId);
                        ps.executeUpdate();
                    }

                    // Enrollments link strictly on SCHED_ID, and trace through schedules tied to this instructor
                    String clearEnroll = "DELETE FROM ENROLLMENT WHERE SCHED_ID IN (SELECT SCHED_ID FROM SCHEDULE WHERE INST_C_ID IN (SELECT INST_C_ID FROM INSTRUCTORS_COURSE WHERE INST_ID = ?))";
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
                    String dayOfWeek = request.getParameter("dayOfWeek").toUpperCase();
                    String timeStart = request.getParameter("timeStart");
                    String timeEnd = request.getParameter("timeEnd");

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
                    conn.commit();
                    logAction("Assigned course schedule assignment: " + nextSchedId + " (" + timeStart + "-" + timeEnd + ")", authorId);
                    break;
                    }
                }

                case "editInstructorSchedule": {
                    String schedId = request.getParameter("schedId");
                    String classDay = request.getParameter("classDay");
                    String timeStart = request.getParameter("timeStart");
                    String timeEnd = request.getParameter("timeEnd");

                    String updateScheduleSql = "UPDATE SCHEDULE SET DAY_OF_WEEK = ?, TIME_START = ?, TIME_END = ? WHERE SCHED_ID = ?";

                    try (PreparedStatement ps = conn.prepareStatement(updateScheduleSql)) {

                        ps.setString(1, classDay);
                        ps.setString(2, timeStart);
                        ps.setString(3, timeEnd);
                        ps.setString(4, schedId);

                        int affectedRows = ps.executeUpdate();
                        if (affectedRows > 0) {
                            logAction("Modified timetable schedule option metadata index: " + schedId, authorId);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        response.getWriter().println("SQL EXCEPTION ENCOUNTERED: " + e.getMessage());
                        return;
                    }
                    break;
                }

                case "deleteInstructorSchedule": {
                    String schedId = request.getParameter("schedId");

                    // Safe database orchestration step using transactions to safely cascadingly purge student linkages first
                    String clearEnrollmentsSql = "DELETE FROM ENROLLMENT WHERE SCHED_ID = ?";
                    String deleteScheduleSql = "DELETE FROM SCHEDULE WHERE SCHED_ID = ?";

                    try {
                        conn.setAutoCommit(false); // Begin transaction block scope

                        try (PreparedStatement psEnroll = conn.prepareStatement(clearEnrollmentsSql);
                             PreparedStatement psSched = conn.prepareStatement(deleteScheduleSql)) {

                            // 1. Un-enroll students tied down to this single instructor structural schedule slot
                            psEnroll.setString(1, schedId);
                            psEnroll.executeUpdate();

                            // 2. Clear out the schedule entity index from the database
                            psSched.setString(1, schedId);
                            psSched.executeUpdate();

                            conn.commit(); // Finalize structural multi-table block change safely
                            logAction("Purged instructor specific scheduled timetable catalog index: " + schedId, authorId);

                        } catch (SQLException e) {
                            conn.rollback(); // Rollback if the inner statement executions fail
                            throw e; // Rethrow to let the outer block print/handle it
                        }

                    } catch (SQLException e) {
                        e.printStackTrace();
                        response.getWriter().println("DATABASE AGGREGATION EXCEPTION: " + e.getMessage());
                        return; // Halt further servlet execution on failure
                    } finally {
                        try {
                            conn.setAutoCommit(true); // Restore default transactional behavior
                        } catch (SQLException ignored) {}
                    }

                    break; // Proceed naturally to response.sendRedirect() at the end of the doPost method
                }
                case "addStudent": {
                    conn.setAutoCommit(false);

                    String rawPassword = request.getParameter("password");
                    String encryptedPassword = Security.encrypt(rawPassword, getServletContext());

                    String nextUserId = generateNextCustomID(conn, "USERS", "USER_ID", "USR");
                    String nextStuId = generateNextCustomID(conn, "STUDENT", "STU_ID", "STU");

                    String firstName = request.getParameter("firstName");
                    String lastName = request.getParameter("lastName");
                    String email = request.getParameter("email");

                    if (firstName == null || lastName == null || email == null) {
                        throw new SQLException("Form payload validation failed. Input field names do not match Servlet properties.");
                    }

                    String uSql = "INSERT INTO USERS (USER_ID, USER_ROLE, EMAIL, PASSWORD) VALUES (?, 'STUDENT', ?, ?)";
                    // 1. Write to USERS table in MySQL
                    try (PreparedStatement ps = conn.prepareStatement(uSql)) {
                        ps.setString(1, nextUserId);
                        ps.setString(2, email);
                        ps.setString(3, encryptedPassword);
                        ps.executeUpdate();
                    }

                    try (Connection derbyConn = getDerbyConnection()) {
                        derbyConn.setAutoCommit(false);
                        try (PreparedStatement psDerby = derbyConn.prepareStatement(uSql)) {
                            psDerby.setString(1, nextUserId);
                            psDerby.setString(2, email);
                            psDerby.setString(3, encryptedPassword);
                            psDerby.executeUpdate();
                        }
                        derbyConn.commit();
                    } catch (Exception e) {
                        System.err.println("Derby Mirroring Failed: " + e.getMessage());
                        throw new SQLException("Derby sync failure, rolling back transaction.", e);
                    }

                    // 3. Write to STUDENT Table in MySQL (EMAIL field removed entirely)
                    String sSql = "INSERT INTO STUDENT (STU_ID, USER_ID, FNAME, LNAME) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sSql)) {
                        ps.setString(1, nextStuId);
                        ps.setString(2, nextUserId);
                        ps.setString(3, firstName.trim());
                        ps.setString(4, lastName.trim());
                        ps.executeUpdate();
                    }

                    conn.commit();
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

                    // Modifying STUDENT name properties (EMAIL omitted to resolve 3NF validation violations)
                    String sSql = "UPDATE STUDENT SET FNAME = ?, LNAME = ? WHERE STU_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sSql)) {
                        ps.setString(1, request.getParameter("firstName"));
                        ps.setString(2, request.getParameter("lastName"));
                        ps.setString(3, request.getParameter("stuId"));
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

                    // Deleted STUDENT_COURSE clean-up reference block (Dropped table optimization)
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
                    String schedId = request.getParameter("schedId");

                    // We write to ENROLLMENT directly via SCHED_ID mapping (STUDENT_COURSE operation dropped completely)
                    String nextEnrId = generateNextCustomID(conn, "ENROLLMENT", "STU_EN_ID", "ENR");
                    String sSql = "INSERT INTO ENROLLMENT (STU_EN_ID, STU_ID, SCHED_ID) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sSql)) {
                        ps.setString(1, nextEnrId);
                        ps.setString(2, stuId);
                        ps.setString(3, schedId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    logAction("Affiliated Student ID " + stuId + " to Active Course Schedule ENR Token: " + nextEnrId, authorId);
                    break;
                }
                case "deleteStudentCourse": {
                    conn.setAutoCommit(false);
                    // To maintain visual frontend layout naming conventions, request pulls 'stuCId'
                    // which maps to the ENROLLMENT primary identification column: STU_EN_ID
                    String stuCId = request.getParameter("stuCId");

                    if (stuCId != null && !stuCId.trim().isEmpty()) {
                        String sql = "DELETE FROM ENROLLMENT WHERE STU_EN_ID = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, stuCId);
                            ps.executeUpdate();
                        }
                        conn.commit();
                        logAction("Unenrolled student completely (Dropped Course Registration Link via Enrollment Token: " + stuCId + ")", authorId);
                    } else {
                        System.err.println("Student Course Deletion aborted: 'stuCId' parameter was empty.");
                    }
                    break;
                }
                case "editStudentCourse": {
                String stuCId = request.getParameter("stuCId");
                String newSchedId = request.getParameter("newSchedId");

                if (stuCId != null && newSchedId != null) {
                    String sql = "UPDATE ENROLLMENT SET SCHED_ID = ? WHERE STU_EN_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, newSchedId);
                        ps.setString(2, stuCId);
                        ps.executeUpdate();

                        conn.commit();
                        logAction("Modified student course configuration map node: " + stuCId + " linked to schedule option ID: " + newSchedId, authorId);
                    }
                }
                break;
            }
                case "addCourse": {
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

                    conn.commit();
                    logAction("Registered Alphanumeric Sequential Course Node: " + nextCourseId, authorId);
                    break;
                }
                case "editCourse": {
                    conn.setAutoCommit(false);

                    String cSql = "UPDATE COURSE SET COURSE_CODE = ?, COURSE_NAME = ?, COURSE_LENGTH = ? WHERE COURSE_ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(cSql)) {
                        ps.setString(1, request.getParameter("courseCode"));
                        ps.setString(2, request.getParameter("courseName"));
                        ps.setInt(3, Integer.parseInt(request.getParameter("courseLength")));
                        ps.setString(4, request.getParameter("courseId"));
                        ps.executeUpdate();
                    }

                    conn.commit();
                    logAction("Altered course core schema descriptor configurations for identification key: " + request.getParameter("courseId"), authorId);
                    break;
                }
                case "deleteCourse": {
                    conn.setAutoCommit(false);
                    String courseId = request.getParameter("courseId");

                    // Cascade cleanup through dependent operational blocks without touching dropped tables
                    String clearSched = "DELETE FROM SCHEDULE WHERE INST_C_ID IN (SELECT INST_C_ID FROM INSTRUCTORS_COURSE WHERE COURSE_ID = ?)";
                    try (PreparedStatement ps = conn.prepareStatement(clearSched)) {
                        ps.setString(1, courseId);
                        ps.executeUpdate();
                    }

                    String clearEnroll = "DELETE FROM ENROLLMENT WHERE SCHED_ID IN (SELECT SCHED_ID FROM SCHEDULE WHERE INST_C_ID IN (SELECT INST_C_ID FROM INSTRUCTORS_COURSE WHERE COURSE_ID = ?))";
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
            response.getWriter().println("SQL ERROR: " + e.getMessage());

            return;
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("GENERAL ERROR: " + e.getMessage());
            return;
        }

        response.sendRedirect("AdminDashboardServlet");
    }
}