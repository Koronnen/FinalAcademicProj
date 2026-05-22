<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%
    // 1. Get the current active session state
    HttpSession activeSession = request.getSession(false);

    // 2. Fetch the integer role status safely
    Object roleObj = (activeSession != null) ? activeSession.getAttribute("role") : null;
    int userType = (roleObj instanceof Integer) ? (Integer) roleObj : -1;

    // 3. Kick them out to index.jsp if they are not userType 1 (Admin)
    if (userType != 1) {
        System.out.println("reached, nto admin");
        if (activeSession != null) {
            activeSession.setAttribute("loginError", "Unauthorized access. Administrator privileges required.");
        }
        response.sendRedirect(request.getContextPath() + "/index.jsp");
        return; // Terminates page rendering immediately
    }
%>
<%!
    // Secure string escaping to prevent HTML tag leak or broken JS expressions
    public String escapeHtml(Object input) {
        if (input == null) return "";
        return input.toString().replace("&", "&amp;")
                               .replace("<", "&lt;")
                               .replace(">", "&gt;")
                               .replace("\"", "&quot;")
                               .replace("'", "&#x27;");
    }

    public String escapeJs(Object input) {
        if (input == null) return "";
        return input.toString().replace("\\", "\\\\")
                               .replace("'", "\\'")
                               .replace("\"", "\\\"")
                               .replace("\r", "\\r")
                               .replace("\n", "\\n");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Enterprise System Administration Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --sidebar-width: 260px;
            --primary-color: #1e293b;
            --secondary-color: #0f172a;
            --accent-blue: #3b82f6;
            --light-bg: #f8fafc;
        }
        body {
            background-color: var(--light-bg);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            overflow-x: hidden;
        }
        #sidebar-wrapper {
            min-height: 100vh;
            width: var(--sidebar-width);
            background-color: var(--primary-color);
            position: fixed;
            left: 0;
            top: 0;
            z-index: 1000;
            transition: all 0.3s ease;
        }
        .sidebar-heading {
            padding: 1.5rem 1.25rem;
            font-size: 1.15rem;
            font-weight: 700;
            color: #ffffff;
            letter-spacing: 0.5px;
            border-bottom: 1px solid rgba(255,255,255,0.1);
        }
        .sidebar-menu-item {
            padding: 0.9rem 1.5rem;
            display: block;
            color: #94a3b8;
            text-decoration: none;
            font-weight: 500;
            transition: all 0.2s ease;
            border-left: 4px solid transparent;
            cursor: pointer;
        }
        .sidebar-menu-item:hover, .sidebar-menu-item.active-tab {
            color: #ffffff;
            background-color: var(--secondary-color);
            border-left-color: var(--accent-blue);
        }
        .sidebar-menu-item i {
            margin-right: 12px;
            width: 20px;
            text-align: center;
        }
        #page-content-wrapper {
            margin-left: var(--sidebar-width);
            padding: 2.5rem;
            width: calc(100% - var(--sidebar-width));
            transition: all 0.3s ease;
        }
        .navbar-custom {
            background-color: #ffffff;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
            padding: 1rem 2.5rem;
            margin: -2.5rem -2.5rem 2.5rem -2.5rem;
        }
        .stat-card {
            background: #ffffff;
            border: none;
            border-radius: 10px;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03);
            transition: transform 0.2s;
        }
        .stat-card:hover {
            transform: translateY(-2px);
        }
        .card-table-container {
            background: #ffffff;
            border-radius: 12px;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
            border: none;
            padding: 1.5rem;
            margin-bottom: 2rem;
        }
        .table thead th {
            background-color: #f1f5f9;
            color: #475569;
            font-weight: 600;
            text-transform: uppercase;
            font-size: 0.75rem;
            letter-spacing: 0.5px;
        }
        .tab-panel-view {
            display: none;
        }
        .tab-panel-view.active-panel {
            display: block;
            animation: fadeIn 0.4s ease-in-out;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(8px); }
            to { opacity: 1; transform: translateY(0); }
        }
    </style>
</head>
<body>

    <div id="sidebar-wrapper">
        <div class="sidebar-heading">
            <i class="fa-solid fa-graduation-cap text-primary me-2"></i>Active Learning
        </div>
        <div class="mt-4">
            <div class="sidebar-menu-item active-tab" id="link-overview" onclick="switchDashboardTab('panel-overview')">
                <i class="fa-solid fa-chart-pie"></i>Overview Summary
            </div>
            <div class="sidebar-menu-item" id="link-instructors" onclick="switchDashboardTab('panel-instructors')">
                <i class="fa-solid fa-chalkboard-user"></i>Instructors Manager
            </div>
            <div class="sidebar-menu-item" id="link-students" onclick="switchDashboardTab('panel-students')">
                <i class="fa-solid fa-user-graduate"></i>Students Manager
            </div>
            <div class="sidebar-menu-item" id="link-courses" onclick="switchDashboardTab('panel-courses')">
                <i class="fa-solid fa-book-bookmark"></i>Curriculum Courses
            </div>
            <hr class="text-white-50 my-2">

            <a href="${pageContext.request.contextPath}/LogOutServlet" class="sidebar-menu-item text-decoration-none d-block text-danger border-0 bg-transparent mt-2">
                <i class="fa-solid fa-right-from-bracket me-3 text-danger"></i>Sign Out Session
            </a>
        </div>
    </div>

    <div id="page-content-wrapper">
        <div class="navbar-custom d-flex justify-content-between align-items-center">
            <h4 class="m-0 fw-bold text-dark">System Administration Console</h4>
            <div class="d-flex align-items-center">
                <span class="badge bg-danger p-2 me-3"><i class="fa-solid fa-database me-1"></i>Cross-DB Log: Enabled</span>
                <div class="text-end">
                    <small class="d-block text-muted">Signed in as</small>
                    <strong class="text-dark">Administrator</strong>
                </div>
            </div>
        </div>

        <%
            List<Map<String, String>> instructors = (List<Map<String, String>>) request.getAttribute("instructors");
            List<Map<String, String>> students = (List<Map<String, String>>) request.getAttribute("students");
            List<Map<String, String>> courses = (List<Map<String, String>>) request.getAttribute("courses");
            List<Map<String, String>> schedulesList = (List<Map<String, String>>) request.getAttribute("schedulesList");
            Map<String, List<Map<String, String>>> instructorCalendars = (Map<String, List<Map<String, String>>>) request.getAttribute("instructorCalendars");
            Map<String, List<Map<String, String>>> studentSchedules = (Map<String, List<Map<String, String>>>) request.getAttribute("studentSchedules");

            int countInst = (instructors != null) ? instructors.size() : 0;
            int countStu = (students != null) ? students.size() : 0;
            int countCourse = (courses != null) ? courses.size() : 0;
        %>

        <div id="panel-overview" class="tab-panel-view active-panel">
            <div class="row g-4 mb-4">
                <div class="col-12 col-sm-6 col-xl-4">
                    <div class="card stat-card p-4 d-flex flex-row align-items-center justify-content-between">
                        <div>
                            <h6 class="text-muted fw-semibold text-uppercase mb-1">Total Active Instructors</h6>
                            <h2 class="m-0 fw-bold text-dark"><%= countInst %></h2>
                        </div>
                        <div class="bg-primary-subtle text-primary p-3 rounded-3"><i class="fa-solid fa-chalkboard-user fa-2xl"></i></div>
                    </div>
                </div>
                <div class="col-12 col-sm-6 col-xl-4">
                    <div class="card stat-card p-4 d-flex flex-row align-items-center justify-content-between">
                        <div>
                            <h6 class="text-muted fw-semibold text-uppercase mb-1">Registered Students</h6>
                            <h2 class="m-0 fw-bold text-dark"><%= countStu %></h2>
                        </div>
                        <div class="bg-success-subtle text-success p-3 rounded-3"><i class="fa-solid fa-user-graduate fa-2xl"></i></div>
                    </div>
                </div>
                <div class="col-12 col-sm-6 col-xl-4">
                    <div class="card stat-card p-4 d-flex flex-row align-items-center justify-content-between">
                        <div>
                            <h6 class="text-muted fw-semibold text-uppercase mb-1">Managed Courses</h6>
                            <h2 class="m-0 fw-bold text-dark"><%= countCourse %></h2>
                        </div>
                        <div class="bg-warning-subtle text-warning p-3 rounded-3"><i class="fa-solid fa-book-bookmark fa-2xl"></i></div>
                    </div>
                </div>
            </div>

            <div class="card p-4 border-0 shadow-sm rounded-3 bg-white mb-4">
                <h5 class="fw-bold"><i class="fa-solid fa-circle-info text-primary me-2"></i>Operational Compliance Notice</h5>
                <p class="text-muted m-0 small">
                    All operations triggered inside this configuration panel utilize multi-database orchestration.
                    Base schema operations write data into the <strong>activelearning</strong> MySQL cluster engine.
                    Audit operations broadcast actions to the enterprise PostgreSQL telemetry logger schema.
                </p>
            </div>
        </div>

        <div id="panel-instructors" class="tab-panel-view">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold text-dark m-0">Faculty Instructor Database Profiles</h5>
                <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalAddInstructor">
                    <i class="fa-solid fa-plus me-1"></i> Add Faculty Member
                </button>
            </div>

            <div class="card card-table-container">
                <div class="table-responsive">
                    <table class="table table-hover align-middle m-0">
                        <thead class="table-light text-uppercase">
                            <tr>
                                <th>Instructor ID</th>
                                <th>Account User</th>
                                <th>Full Name</th>
                                <th>Email Base</th>
                                <th class="text-center">Schedules & Calendar View</th>
                                <th class="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (instructors != null) { for (Map<String, String> inst : instructors) { %>
                            <tr>
                                <td><span class="badge bg-secondary"><%= escapeHtml(inst.get("instId")) %></span></td>
                                <td><strong class="text-secondary"><%= escapeHtml(inst.get("userId")) %></strong></td>
                                <td><%= escapeHtml(inst.get("lastName")) %>, <%= escapeHtml(inst.get("firstName")) %></td>
                                <td><%= escapeHtml(inst.get("email")) %></td>
                                <td class="text-center">
                                    <button class="btn btn-outline-secondary btn-sm px-3 me-2" onclick="openScheduleModal('<%= escapeJs(inst.get("instId")) %>')">
                                        <i class="fa-solid fa-calendar-plus me-1"></i> Add Sched
                                    </button>
                                    <button class="btn btn-outline-dark btn-sm px-3" onclick="openCalendarModal('<%= escapeJs(inst.get("instId")) %>', '<%= escapeJs(inst.get("lastName")) %>')">
                                        <i class="fa-solid fa-eye me-1"></i> View Weekly Sched
                                    </button>
                                </td>
                                <td class="text-end">
                                    <button class="btn btn-light btn-sm text-warning me-1" onclick="populateEditInstructorModal('<%= escapeJs(inst.get("instId")) %>', '<%= escapeJs(inst.get("userId")) %>', '<%= escapeJs(inst.get("userId")) %>', '<%= escapeJs(inst.get("firstName")) %>', '<%= escapeJs(inst.get("lastName")) %>', '<%= escapeJs(inst.get("email")) %>')">
                                        <i class="fa-solid fa-pen-to-square"></i>
                                    </button>
                                    <form action="AdminDashboardServlet" method="POST" class="d-inline" onsubmit="return confirm('Delete this instructor?\nThis clears authentication details too.');">
                                        <input type="hidden" name="action" value="deleteInstructor">
                                        <input type="hidden" name="instId" value="<%= escapeHtml(inst.get("instId")) %>">
                                        <input type="hidden" name="userId" value="<%= escapeHtml(inst.get("userId")) %>">
                                        <button type="submit" class="btn btn-light btn-sm text-danger"><i class="fa-solid fa-trash-can"></i></button>
                                    </form>
                                </td>
                            </tr>
                            <% } } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="panel-students" class="tab-panel-view">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold text-dark m-0">Student Enrollment Ledger</h5>
                <div>
                    <button class="btn btn-outline-primary btn-sm me-2" data-bs-toggle="modal" data-bs-target="#modalEnrollStudent">
                        <i class="fa-solid fa-user-plus me-1"></i> Enroll to Course Offered
                    </button>
                    <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalAddStudent">
                        <i class="fa-solid fa-plus me-1"></i> Add New Student
                    </button>
                </div>
            </div>

            <div class="card card-table-container">
                <div class="table-responsive">
                    <table class="table table-hover align-middle m-0">
                        <thead class="table-light">
                            <tr>
                                <th>Student ID</th>
                                <th>Username Handle</th>
                                <th>Full Legal Name</th>
                                <th>Email Configuration</th>
                                <th>Enrolled Classes</th>
                                <th class="text-end">Actions Interface</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (students != null) { for (Map<String, String> stu : students) { %>
                            <tr>
                                <td><span class="badge bg-dark"><%= escapeHtml(stu.get("stuId")) %></span></td>
                                <td><span class="text-muted">@</span><%= escapeHtml(stu.get("userId")) %></td>
                                <td><strong><%= escapeHtml(stu.get("lastName")) %>, <%= escapeHtml(stu.get("firstName")) %></strong></td>
                                <td><%= escapeHtml(stu.get("email")) %></td>
                                <td>
                                    <%
                                        List<Map<String, String>> schedules = (studentSchedules != null) ? studentSchedules.get(stu.get("stuId")) : null;
                                        if (schedules != null && !schedules.isEmpty()) {
                                            for (Map<String, String> sched : schedules) {
                                    %>
                                    <div class="card border border-secondary-subtle mb-2 shadow-sm">
                                        <div class="card-body p-2" style="font-size: 0.85rem;">
                                            <div class="d-flex justify-content-between align-items-start border-bottom pb-1 mb-1">
                                                <strong class="text-primary"><%= escapeHtml(sched.get("courseName")) %></strong>
                                                <div class="ms-2 flex-shrink-0">
                                                    <button type="button" class="btn btn-sm btn-link p-0 text-warning me-1" onclick="populateEditStudentCourseModal('<%= escapeJs(sched.get("stuEnId")) %>', '<%= escapeJs(sched.get("courseId")) %>')">
                                                        <i class="fa-solid fa-pen-to-square"></i>
                                                    </button>
                                                    <form action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST" class="d-inline" onsubmit="return confirm('Remove this class from the student?');">
                                                        <input type="hidden" name="action" value="deleteStudentCourse">
                                                        <input type="hidden" name="stuCId" value="<%= escapeHtml(sched.get("stuEnId")) %>">
                                                        <button type="submit" class="btn btn-sm btn-link p-0 text-danger"><i class="fa-solid fa-trash-can"></i></button>
                                                    </form>
                                                </div>
                                            </div>
                                            <div class="text-dark">
                                                <div><strong>Day:</strong> <%= escapeHtml(sched.get("classDay")) %></div>
                                                <div><strong>Instructor:</strong> <%= escapeHtml(sched.get("instLName")) %>, <%= escapeHtml(sched.get("instFName")) %></div>
                                                <div><strong>Time:</strong> <%= escapeHtml(sched.get("timeStart")) %> - <%= escapeHtml(sched.get("timeEnd")) %></div>
                                            </div>
                                        </div>
                                    </div>
                                    <%
                                            }
                                        } else {
                                    %>
                                    <span class="text-muted small fst-italic">Not enrolled in any schedules</span>
                                    <% } %>
                                </td>
                                <td class="text-end">
                                    <button class="btn btn-light btn-sm text-warning me-1" onclick="populateEditStudentModal('<%= escapeJs(stu.get("stuId")) %>', '<%= escapeJs(stu.get("userId")) %>', '<%= escapeJs(stu.get("userId")) %>', '<%= escapeJs(stu.get("firstName")) %>', '<%= escapeJs(stu.get("lastName")) %>', '<%= escapeJs(stu.get("email")) %>')">
                                        <i class="fa-solid fa-pen-to-square"></i>
                                    </button>
                                    <form action="AdminDashboardServlet" method="POST" class="d-inline" onsubmit="return confirm('Purge this student profile record?\nAction logs to audit metrics.');">
                                        <input type="hidden" name="action" value="deleteStudent">
                                        <input type="hidden" name="stuId" value="<%= escapeHtml(stu.get("stuId")) %>">
                                        <input type="hidden" name="userId" value="<%= escapeHtml(stu.get("userId")) %>">
                                        <button type="submit" class="btn btn-light btn-sm text-danger"><i class="fa-solid fa-trash-can"></i></button>
                                    </form>
                                </td>
                            </tr>
                            <% } } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="panel-courses" class="tab-panel-view">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold text-dark m-0">Institutional Academic Curriculum Inventory</h5>
                <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalAddCourse">
                    <i class="fa-solid fa-plus me-1"></i> Create Syllabus Course Unit
                </button>
            </div>

            <div class="card card-table-container">
                <div class="table-responsive">
                    <table class="table table-hover align-middle m-0">
                        <thead class="table-light">
                            <tr>
                                <th>Course Index Code</th>
                                <th>Display Label</th>
                                <th>Duration Structural Magnitude</th>
                                <th class="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (courses != null) { for (Map<String, String> crs : courses) { %>
                            <tr>
                                <td><span class="badge bg-primary text-uppercase p-2"><%= escapeHtml(crs.get("courseCode")) %></span></td>
                                <td><strong><%= escapeHtml(crs.get("courseName")) %></strong></td>
                                <td><span class="text-muted"><%= escapeHtml(crs.get("courseLength")) %> Units/Hours</span></td>
                                <td class="text-end">
                                    <button class="btn btn-light btn-sm text-warning me-1" onclick="populateEditCourseModal('<%= escapeJs(crs.get("courseId")) %>', '<%= escapeJs(crs.get("courseCode")) %>', '<%= escapeJs(crs.get("courseName")) %>', '<%= escapeJs(crs.get("courseLength")) %>')">
                                        <i class="fa-solid fa-pen-to-square"></i>
                                    </button>
                                    <form action="AdminDashboardServlet" method="POST" class="d-inline" onsubmit="return confirm('Remove course asset?\nRelated elements might be restricted.');">
                                        <input type="hidden" name="action" value="deleteCourse">
                                        <input type="hidden" name="courseId" value="<%= escapeHtml(crs.get("courseId")) %>">
                                        <button type="submit" class="btn btn-light btn-sm text-danger"><i class="fa-solid fa-trash-can"></i></button>
                                    </form>
                                </td>
                            </tr>
                            <% } } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modalEditStudentCourse" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST">
                <input type="hidden" name="action" value="editStudentCourse">
                <input type="hidden" name="stuCId" id="editStuCId">

                <div class="modal-header bg-warning">
                    <h5 class="modal-title fw-bold text-dark"><i class="fa-solid fa-book-open-reader me-2"></i>Change Enrolled Class</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>

                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Select Available Schedule Matrix Option</label>
                        <select name="newSchedId" id="editNewSchedId" class="form-select form-select-sm" required>
                            <option value="" disabled selected>-- Select a slot --</option>
                        </select>
                    </div>
                </div>

                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-warning btn-sm fw-bold">Update Enrollment</button>
                </div>
            </form>
        </div>
    </div>

    <div class="modal fade" id="modalAddInstructor" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST">
                <input type="hidden" name="action" value="addInstructor">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title fw-bold"><i class="fa-solid fa-user-plus me-2"></i>Add Faculty Instructor</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="row g-3">
                        <div class="col-6"><label class="form-label small fw-bold">User System Login</label><input type="text" name="username" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">Security Password</label><input type="password" name="password" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">First Name</label><input type="text" name="firstName" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">Last Name</label><input type="text" name="lastName" class="form-control form-control-sm" required></div>
                        <div class="col-12"><label class="form-label small fw-bold">Institutional Contact Email</label><input type="email" name="email" class="form-control form-control-sm" required></div>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary btn-sm">Save Profile</button>
                </div>
            </form>
        </div>
    </div>

    <div class="modal fade" id="modalEditInstructor" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST">
                <input type="hidden" name="action" value="editInstructor">
                <input type="hidden" name="instId" id="editInstId">
                <input type="hidden" name="userId" id="editInstUserId">
                <div class="modal-header bg-warning">
                    <h5 class="modal-title fw-bold text-dark"><i class="fa-solid fa-user-pen me-2"></i>Modify Instructor Identity</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="row g-3">
                        <div class="col-6"><label class="form-label small fw-bold">Login Handle ID</label><input type="text" name="username" id="editInstUsername" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">Update Passcode</label><input type="password" name="password" placeholder="Leave empty or set new" class="form-control form-control-sm"></div>
                        <div class="col-6"><label class="form-label small fw-bold">First Name</label><input type="text" name="firstName" id="editInstFirstName" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">Last Name</label><input type="text" name="lastName" id="editInstLastName" class="form-control form-control-sm" required></div>
                        <div class="col-12"><label class="form-label small fw-bold">Email Configuration Address</label><input type="email" name="email" id="editInstEmail" class="form-control form-control-sm" required></div>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Dismiss</button>
                    <button type="submit" class="btn btn-warning btn-sm fw-bold">Apply Variations</button>
                </div>
            </form>
        </div>
    </div>

    <div class="modal fade" id="modalAssignSchedule" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" id="modalAssignScheduleForm" action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST">
                <input type="hidden" name="action" id="scheduleActionField" value="assignInstructorSchedule">
                <input type="hidden" name="schedId" id="scheduleIdField" value="">
                <input type="hidden" name="instId" id="scheduleInstId">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title fw-bold" id="scheduleModalHeaderTitle"><i class="fa-solid fa-calendar-plus me-2"></i>Configure Course Schedule Node</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Target Structural Curriculum Asset</label>
                        <select name="courseId" class="form-select form-select-sm" required>
                            <% if(courses != null) { for(Map<String, String> c : courses) { %>
                            <option value="<%= escapeHtml(c.get("courseId")) %>">[<%= escapeHtml(c.get("courseCode")) %>] <%= escapeHtml(c.get("courseName")) %></option>
                            <% } } %>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Recurrence Day Slot</label>
                        <select name="dayOfWeek" class="form-select form-select-sm" required>
                            <option value="Monday">Monday Matrix</option>
                            <option value="Tuesday">Tuesday Matrix</option>
                            <option value="Wednesday">Wednesday Matrix</option>
                            <option value="Thursday">Thursday Matrix</option>
                            <option value="Friday">Friday Matrix</option>
                            <option value="Saturday">Saturday Matrix</option>
                            <option value="Sunday">Sunday Matrix</option>
                        </select>
                    </div>
                    <div class="row g-3">
                        <div class="col-6">
                            <label class="form-label small fw-bold">Execution Start</label>
                            <input type="time" name="timeStart" class="form-control form-control-sm" required>
                        </div>
                        <div class="col-6">
                            <label class="form-label small fw-bold">Execution Limit Termination (End)</label>
                            <input type="time" name="timeEnd" class="form-control form-control-sm" required>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary btn-sm">Commit Sched Offering</button>
                </div>
            </form>
        </div>
    </div>

    <div class="modal fade" id="modalViewCalendar" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title fw-bold"><i class="fa-solid fa-calendar-days me-2"></i>Weekly Timetable Configuration: <span id="calendarModalInstructorLabel"></span></h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="table-responsive">
                        <table class="table table-bordered align-middle text-center m-0" id="calendarOutputTable">
                            <thead class="table-light">
                                <tr>
                                    <th>Monday</th>
                                    <th>Tuesday</th>
                                    <th>Wednesday</th>
                                    <th>Thursday</th>
                                    <th>Friday</th>
                                    <th>Saturday</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td id="cal-day-Monday" class="small p-3 text-muted"><em>Vacant Matrix Node</em></td>
                                    <td id="cal-day-Tuesday" class="small p-3 text-muted"><em>Vacant Matrix Node</em></td>
                                    <td id="cal-day-Wednesday" class="small p-3 text-muted"><em>Vacant Matrix Node</em></td>
                                    <td id="cal-day-Thursday" class="small p-3 text-muted"><em>Vacant Matrix Node</em></td>
                                    <td id="cal-day-Friday" class="small p-3 text-muted"><em>Vacant Matrix Node</em></td>
                                    <td id="cal-day-Saturday" class="small p-3 text-muted"><em>Vacant Matrix Node</em></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="modalAddStudent" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST">
                <input type="hidden" name="action" value="addStudent">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title fw-bold"><i class="fa-solid fa-graduation-cap me-2"></i>Register New Matriculated Student</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="row g-3">
                        <div class="col-6"><label class="form-label small fw-bold">Login Handles Identification</label><input type="text" name="username" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">Access Password Sequence</label><input type="password" name="password" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">First Given Name</label><input type="text" name="firstName" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">Last Surname Legal</label><input type="text" name="lastName" class="form-control form-control-sm" required></div>
                        <div class="col-12"><label class="form-label small fw-bold">Communication Core Email</label><input type="email" name="email" class="form-control form-control-sm" required></div>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary btn-sm">Commit Student Node</button>
                </div>
            </form>
        </div>
    </div>

    <div class="modal fade" id="modalEditStudent" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST">
                <input type="hidden" name="action" value="editStudent">
                <input type="hidden" name="stuId" id="editStuId">
                <input type="hidden" name="userId" id="editStuUserId">
                <div class="modal-header bg-warning">
                    <h5 class="modal-title fw-bold text-dark"><i class="fa-solid fa-user-pen me-2"></i>Modify Student Identity Metrics</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="row g-3">
                        <div class="col-6"><label class="form-label small fw-bold">Login Identification Name</label><input type="text" name="username" id="editStuUsername" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">Update Passcode</label><input type="password" name="password" class="form-control form-control-sm" placeholder="Set new to change string value"></div>
                        <div class="col-6"><label class="form-label small fw-bold">First Name</label><input type="text" name="firstName" id="editStuFirstName" class="form-control form-control-sm" required></div>
                        <div class="col-6"><label class="form-label small fw-bold">Last Name</label><input type="text" name="lastName" id="editStuLastName" class="form-control form-control-sm" required></div>
                        <div class="col-12"><label class="form-label small fw-bold">Communication Email Payload</label><input type="email" name="email" id="editStuEmail" class="form-control form-control-sm" required></div>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-warning btn-sm fw-bold">Apply Modifications</button>
                </div>
            </form>
        </div>
    </div>

    <div class="modal fade" id="modalEnrollStudent" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST">
                <input type="hidden" name="action" value="enrollStudent">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title fw-bold"><i class="fa-solid fa-link me-2"></i>Assign Course Enrollment Node</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Target Matriculated Student</label>
                        <select name="stuId" class="form-select form-select-sm" required>
                            <% if (students != null) { for(Map<String, String> s : students) { %>
                            <option value="<%= escapeHtml(s.get("stuId")) %>"><%= escapeHtml(s.get("lastName")) %>, <%= escapeHtml(s.get("firstName")) %> (STU-<%= escapeHtml(s.get("stuId")) %>)</option>
                            <% } } %>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Target Schedule Class Matcher</label>
                        <select name="schedId" class="form-select form-select-sm" required>
                            <% if (schedulesList != null) { for(Map<String, String> sch : schedulesList) { %>
                            <option value="<%= escapeHtml(sch.get("schedId")) %>"><%= escapeHtml(sch.get("courseName")) %> (<%= escapeHtml(sch.get("day")) %> : <%= escapeHtml(sch.get("start")) %>-<%= escapeHtml(sch.get("end")) %>)</option>
                            <% } } %>
                        </select>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary btn-sm">Finalize Enrollment Link</button>
                </div>
            </form>
        </div>
    </div>

    <div class="modal fade" id="modalAddCourse" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST">
                <input type="hidden" name="action" value="addCourse">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title fw-bold"><i class="fa-solid fa-book me-2"></i>Create Syllabus Course Unit</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Course Unique Code</label>
                        <input type="text" name="courseCode" class="form-control form-control-sm text-uppercase" placeholder="e.g., CS101" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Display Title Label</label>
                        <input type="text" name="courseName" class="form-control form-control-sm" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Duration Magnitude (Units / Hours)</label>
                        <input type="number" name="courseLength" class="form-control form-control-sm" min="1" max="100" required>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary btn-sm">Commit Course offering</button>
                </div>
            </form>
        </div>
    </div>

    <div class="modal fade" id="modalEditCourse" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <form class="modal-content" action="${pageContext.request.contextPath}/AdminDashboardServlet" method="POST">
                <input type="hidden" name="action" value="editCourse">
                <input type="hidden" name="courseId" id="editCourseId">
                <div class="modal-header bg-warning">
                    <h5 class="modal-title fw-bold text-dark"><i class="fa-solid fa-pen-to-square me-2"></i>Modify Course Definition</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Course Unique Code</label>
                        <input type="text" name="courseCode" id="editCourseCode" class="form-control form-control-sm text-uppercase" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Display Title Label</label>
                        <input type="text" name="courseName" id="editCourseName" class="form-control form-control-sm" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-bold">Duration Magnitude (Units / Hours)</label>
                        <input type="number" name="courseLength" id="editCourseLength" class="form-control form-control-sm" min="1" max="100" required>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-warning btn-sm fw-bold">Apply System Changes</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        const globalInstructorCalendars = {
            <% if (instructorCalendars != null) {
                for (Map.Entry<String, List<Map<String, String>>> entry : instructorCalendars.entrySet()) { %>
                    "<%= escapeJs(entry.getKey()) %>": [
                        <% for (Map<String, String> s : entry.getValue()) { %>
                            {
                                courseCode: "<%= escapeJs(s.get("courseCode")) %>",
                                courseName: "<%= escapeJs(s.get("courseName")) %>",
                                classDay: "<%= escapeJs(s.get("classDay")) %>",
                                timeStart: "<%= escapeJs(s.get("timeStart")) %>",
                                timeEnd: "<%= escapeJs(s.get("timeEnd")) %>"
                            },
                        <% } %>
                    ],
            <% } } %>
        };
        
        // Global registries containing server state safely escaped against rendering syntax exceptions
        const globalCourseSchedules = {
            <% if (schedulesList != null) {
                // Java 1.5 Compliant: Explicitly specify types on both sides (No diamond operator)
                Map<String, List<Map<String, String>>> groupedScheds = new HashMap<String, List<Map<String, String>>>();
                for (Map<String, String> sch : schedulesList) {
                    String cId = sch.get("courseId");
                    if (cId != null) {
                        // Java 1.5 Compliant: Classic lookups instead of lambda .computeIfAbsent()
                        List<Map<String, String>> existingList = groupedScheds.get(cId);
                        if (existingList == null) {
                            existingList = new ArrayList<Map<String, String>>();
                            groupedScheds.put(cId, existingList);
                        }
                        existingList.add(sch);
                    }
                }
                
                for (Map.Entry<String, List<Map<String, String>>> entry : groupedScheds.entrySet()) { %>
                    "<%= escapeJs(entry.getKey()) %>": [
                        <% for (Map<String, String> s : entry.getValue()) { %>
                            {
                                schedId: "<%= escapeJs(s.get("schedId")) %>",
                                courseName: "<%= escapeJs(s.get("courseName")) %>",
                                // Compatibility fallback keys matching both selection criteria styles
                                day: "<%= escapeJs(s.get("DAY_OF_WEEK") != null ? s.get("DAY_OF_WEEK") : (s.get("classDay") != null ? s.get("classDay") : s.get("day"))) %>",
                                start: "<%= escapeJs(s.get("TIME_START") != null ? s.get("TIME_START") : (s.get("timeStart") != null ? s.get("timeStart") : s.get("start"))) %>",
                                end: "<%= escapeJs(s.get("TIME_END") != null ? s.get("TIME_END") : (s.get("timeEnd") != null ? s.get("timeEnd") : s.get("end"))) %>"
                            },
                        <% } %>
                    ],
            <% } } %>
        };

        function switchDashboardTab(panelId) {
            document.querySelectorAll('.tab-panel-view').forEach(panel => panel.classList.remove('active-panel'));
            document.querySelectorAll('.sidebar-menu-item').forEach(link => link.classList.remove('active-tab'));
            const targetPanel = document.getElementById(panelId);
            if(targetPanel) targetPanel.classList.add('active-panel');

            const tabMap = {
                'panel-overview': 'link-overview',
                'panel-instructors': 'link-instructors',
                'panel-students': 'link-students',
                'panel-courses': 'link-courses'
            };
            const targetTab = document.getElementById(tabMap[panelId]);
            if (targetTab) targetTab.classList.add('active-tab');
        }

        function openScheduleModal(instructorId) {
            document.getElementById('scheduleInstId').value = instructorId;
            document.getElementById('scheduleActionField').value = "assignInstructorSchedule";
            document.getElementById('scheduleIdField').value = "";
            document.getElementById('scheduleModalHeaderTitle').innerHTML = '<i class="fa-solid fa-calendar-plus me-2"></i>Configure Course Schedule Node';
            new bootstrap.Modal(document.getElementById('modalAssignSchedule')).show();
        }

        function openCalendarModal(instructorId, instructorName) {
            document.getElementById('calendarModalInstructorLabel').innerText = instructorName;
            const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
            days.forEach(d => {
                const td = document.getElementById('cal-day-' + d);
                if (td) {
                    td.innerHTML = '<em>Vacant Matrix Node</em>';
                    td.className = 'small p-3 text-muted';
                }
            });
            const userScheds = globalInstructorCalendars[instructorId] || [];
            userScheds.forEach(s => {
                if (!s.classDay) return;
                const normalizedDay = s.classDay.charAt(0).toUpperCase() + s.classDay.slice(1).toLowerCase();
                const targetTd = document.getElementById('cal-day-' + normalizedDay);
                if (targetTd) {
                    if (targetTd.innerHTML.includes('Vacant Matrix Node')) {
                        targetTd.innerHTML = '';
                        targetTd.className = 'small p-2';
                    }

                    targetTd.innerHTML +=
                        '<div class="p-2 mb-2 bg-primary-subtle text-primary rounded-2 border border-primary-subtle text-start">' +
                            '<div class="fw-bold text-uppercase" style="font-size:0.75rem;">' + s.courseCode + '</div>' +
                            '<div class="small fw-semibold text-truncate" style="max-width:130px;">' + s.courseName + '</div>' +
                            '<div class="text-muted" style="font-size:0.7rem;">' +
                                '<i class="fa-regular fa-clock me-1"></i>' + s.timeStart + ' - ' + s.timeEnd +
                            '</div>' +
                        '</div>';
                }
            });
            new bootstrap.Modal(document.getElementById('modalViewCalendar')).show();
        }

        function populateEditInstructorModal(instId, uId, username, fName, lName, email) {
            document.getElementById('editInstId').value = instId;
            document.getElementById('editInstUserId').value = uId;
            document.getElementById('editInstUsername').value = username;
            document.getElementById('editInstFirstName').value = fName;
            document.getElementById('editInstLastName').value = lName;
            document.getElementById('editInstEmail').value = email;
            new bootstrap.Modal(document.getElementById('modalEditInstructor')).show();
        }

        function populateEditStudentModal(stuId, uId, username, fName, lName, email) {
            document.getElementById('editStuId').value = stuId;
            document.getElementById('editStuUserId').value = uId;
            document.getElementById('editStuUsername').value = username;
            document.getElementById('editStuFirstName').value = fName;
            document.getElementById('editStuLastName').value = lName;
            document.getElementById('editStuEmail').value = email;
            new bootstrap.Modal(document.getElementById('modalEditStudent')).show();
        }

        function populateEditCourseModal(courseId, cCode, cName, cLen) {
            document.getElementById('editCourseId').value = courseId;
            document.getElementById('editCourseCode').value = cCode;
            document.getElementById('editCourseName').value = cName;
            document.getElementById('editCourseLength').value = cLen;
            new bootstrap.Modal(document.getElementById('modalEditCourse')).show();
        }

        function populateEditStudentCourseModal(stuCId, currentCourseId) {
            document.getElementById('editStuCId').value = stuCId;
            const selectDropdown = document.getElementById('editNewSchedId');
            if (!selectDropdown) return;

            selectDropdown.innerHTML = '';
            const targetedSchedules = globalCourseSchedules[currentCourseId] || [];
            if (targetedSchedules.length === 0) {
                const opt = document.createElement('option');
                opt.value = "";
                opt.disabled = true;
                opt.selected = true;
                opt.innerText = "-- No alternative schedules configured for this course --";
                selectDropdown.appendChild(opt);
            } else {
                targetedSchedules.forEach(s => {
                    const opt = document.createElement('option');
                    opt.value = s.schedId;
                    opt.innerText = s.courseName + " (" + s.day + " : " + s.start + " - " + s.end + ")";
                    selectDropdown.appendChild(opt);
                });
            }
            new bootstrap.Modal(document.getElementById('modalEditStudentCourse')).show();
        }
    </script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>