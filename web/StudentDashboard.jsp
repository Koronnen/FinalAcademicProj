<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
    
    HttpSession activeSession = request.getSession(false);
    if (activeSession == null || activeSession.getAttribute("USER_ID") == null) {
        activeSession.setAttribute("loginError", "Access denied. Please log-in again.");
        return;
    }

    String displayName = (String) request.getAttribute("displayName");
    if (displayName == null || displayName.trim().isEmpty()) {
        displayName = "Student";
    }
    
    // Safely extract from the 'profile' Map exactly how StudentServlet sets it
    Map<String, String> profileMap = (Map<String, String>) request.getAttribute("profile");
    
    String studentFname = "";
    String studentLname = "";
    
    if (profileMap != null) {
        studentFname = profileMap.get("firstName") != null ? profileMap.get("firstName") : "";
        studentLname = profileMap.get("lastName") != null ? profileMap.get("lastName") : "";
    }

    List<Map<String, String>> enrolledCourses = (List<Map<String, String>>) request.getAttribute("enrolledCourses");
    List<Map<String, Object>> availableCourses = (List<Map<String, Object>>) request.getAttribute("availableCourses");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Student Dashboard Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --sidebar-width: 260px;
            --primary-color: #1e293b;      /* Slate Blue brand matching Admin Dashboard template */
            --secondary-color: #0f172a;    /* Dark Slate modifier */
            --accent-blue: #3b82f6;        /* Vibrant blue highlight border metric */
            --light-bg: #f8fafc;           /* Soft light slate structural background */
        }
        body {
            background-color: var(--light-bg);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            overflow-x: hidden;
            margin: 0;
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
        .card-table-container {
            background: #ffffff;
            border-radius: 12px;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
            border: none;
            padding: 1.5rem;
            margin-bottom: 2rem;
        }
        .table classical-thead th {
            background-color: #f1f5f9;    /* Muted grey-blue background */
            color: #475569;                /* Slate header text color */
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
        .profile-text-display {
            background-color: #f8fafc;
            border: 1px dashed #cbd5e1;
            border-radius: 8px;
        }
    </style>
</head>
<body>

    <div id="sidebar-wrapper">
        <div class="sidebar-heading">
            <i class="fa-solid fa-graduation-cap me-2" style="color: var(--accent-blue);"></i>Active Learning
        </div>
        <div class="mt-4">
            <div class="sidebar-menu-item active-tab" id="link-schedule" onclick="switchDashboardTab('panel-schedule')">
                <i class="fa-solid fa-calendar-days"></i>Current Schedule
            </div>
            <div class="sidebar-menu-item" id="link-profile" onclick="switchDashboardTab('panel-profile')">
                <i class="fa-solid fa-user"></i>My Profile
            </div>
            <div class="sidebar-menu-item" id="link-enrollment" onclick="switchDashboardTab('panel-enrollment')">
                <i class="fa-solid fa-book-medical"></i>Course Enrollment
            </div>
            <hr class="text-white-50 my-2">
            <a href="${pageContext.request.contextPath}/LogOutServlet" class="sidebar-menu-item text-decoration-none d-block text-danger border-0 bg-transparent mt-2" >
            <i class="fa-solid fa-right-from-bracket me-3 text-danger"></i> Log Out Account
            </a>
        </div>
    </div>

    <div id="page-content-wrapper">
        
        <div class="navbar-custom d-flex justify-content-between align-items-center">
            <h4 class="m-0 fw-bold text-dark">Student Dashboard Console</h4>
            <div class="d-flex align-items-center">
                <div class="text-end">
                    <small class="d-block text-muted">Welcome back,</small>
                    <strong class="text-dark"><%= displayName %></strong>
                </div>
            </div>
        </div>

        <div id="panel-schedule" class="tab-panel-view active-panel">
            <div class="mb-3">
                <h5 class="fw-bold text-dark m-0">My Registered Academic Syllabus</h5>
            </div>
            <div class="card card-table-container">
                <div class="table-responsive">
                    <table class="table table-hover align-middle m-0">
                        <thead class="table-light classical-thead">
                            <tr>
                                <th>Course Index</th>
                                <th>Course Name</th>
                                <th>Assigned Timetable Slot</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (enrolledCourses != null && !enrolledCourses.isEmpty()) { 
                                for (Map<String, String> course : enrolledCourses) { %>
                                <tr>
                                    <td><span class="badge bg-primary text-uppercase px-2 py-1"><%= course.get("courseCode") %></span></td>
                                    <td><strong><%= course.get("courseName") %></strong></td>
                                    <td><i class="fa-regular fa-clock me-1 text-muted"></i><%= course.get("timeDetails") %></td>
                                </tr>
                            <% } } else { %>
                                <tr>
                                    <td colspan="3" class="text-muted text-center py-4">You are not currently enrolled in any curriculum metrics.</td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="panel-profile" class="tab-panel-view">
            <h5 class="fw-bold text-dark mb-3">Manage Personal Profile Information</h5>
            
            <div class="row g-4">
                <div class="col-lg-12">
                    <div class="card card-table-container shadow-sm">
                        
                        <div class="profile-text-display p-4 mb-4">
                            <h6 class="fw-bold text-secondary mb-3"><i class="fa-solid fa-id-card me-2 text-primary"></i>Current Account Profile Overview</h6>
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <span class="text-muted small d-block uppercase fw-semibold">First Name</span>
                                    <span class="fs-6 fw-bold text-dark"><%= studentFname.isEmpty() ? "—" : studentFname %></span>
                                </div>
                                <div class="col-md-4">
                                    <span class="text-muted small d-block uppercase fw-semibold">Last Name</span>
                                    <span class="fs-6 fw-bold text-dark"><%= studentLname.isEmpty() ? "—" : studentLname %></span>
                                </div>
                            </div>
                        </div>
                        <form action="${pageContext.request.contextPath}/StudentServlet" method="POST">
                            <input type="hidden" name="action" value="updateProfile">
                            <h6 class="fw-bold text-secondary mb-3"><i class="fa-solid fa-pen-to-square me-2"></i>Modify Properties</h6>
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold">First Name</label>
                                    <input type="text" name="fname" class="form-control" value="<%= studentFname %>" required>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold">Last Name</label>
                                    <input type="text" name="lname" class="form-control" value="<%= studentLname %>" required>
                                </div>
                                <div class="col-12 text-end mt-4 d-flex justify-content-between align-items-center">
                                    <button type="button" class="btn btn-sm btn-link text-danger p-0 border-0 text-decoration-none fw-semibold" 
                                            onclick="if(confirm('Clear optional fields?')) { document.getElementById('clearProfileForm').submit(); }">
                                        <i class="fa-solid fa-trash-can me-1"></i> Clear Profile Properties
                                    </button>
                                    <button type="submit" class="btn btn-primary px-4" style="background-color: var(--primary-color); border-color: var(--primary-color);">
                                        <i class="fa-solid fa-floppy-disk me-2"></i>Save Account Changes
                                    </button>
                                </div>
                            </div>
                        </form>
                        
                        <form id="clearProfileForm" action="${pageContext.request.contextPath}/StudentServlet" method="POST" class="d-none">
                            <input type="hidden" name="action" value="clearProfile">
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <div id="panel-enrollment" class="tab-panel-view">
            <h5 class="fw-bold text-dark mb-3">Institutional Catalog Open Enrollments</h5>
            
            <div class="card card-table-container">
                <div class="table-responsive">
                    <table class="table table-hover align-middle m-0">
                        <thead class="table-light classical-thead">
                            <tr>
                                <th>Course Reference</th>
                                <th>Curriculum Topic Title</th>
                                <th>Assigned Instructor</th>
                                <th>Available Allocation Slot Matrix</th>
                                <th class="text-end">Execution Transaction</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (availableCourses != null && !availableCourses.isEmpty()) { 
                                for (Map<String, Object> course : availableCourses) { 
                                    List<Map<String, String>> schedulesList = (List<Map<String, String>>) course.get("schedulesList");
                            %>
                                <tr>
                                    <td><span class="badge text-uppercase" style="background-color: var(--primary-color); color: #ffffff;"><%= course.get("courseCode") %></span></td>
                                    <td><strong><%= course.get("courseName") %></strong></td>
                                    <td><span class="text-muted small fw-semibold"><%= course.get("instructorName") %></span></td>
                                    
                                    <form action="${pageContext.request.contextPath}/StudentServlet" method="POST" class="d-inline">
                                        <input type="hidden" name="action" value="enrollCourse">
                                        <input type="hidden" name="instCourseId" value="<%= course.get("instCourseId") %>">
                                        
                                        <td>
                                            <select name="schedId" class="form-select form-select-sm" required style="max-width: 280px;">
                                                <option value="" disabled selected>-- Select Available Slot --</option>
                                                <% if (schedulesList != null) {
                                                    for (Map<String, String> slot : schedulesList) { %>
                                                    <option value="<%= slot.get("schedId") %>"><%= slot.get("timeDetails") %></option>
                                                <% } } %>
                                            </select>
                                        </td>
                                        <td class="text-end">
                                            <button type="submit" class="btn btn-sm btn-outline-primary px-3">
                                                <i class="fa-solid fa-plus me-1"></i> Enroll Course
                                            </button>
                                        </td>
                                    </form>
                                </tr>
                            <% } } else { %>
                                <tr>
                                    <td colspan="5" class="text-muted text-center py-4">No active catalog curriculum structures are available for registration at this current time node.</td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bundle.min.js"></script>
    <script>
        // Use localStorage to remember which panel was active across form submissions and redirects
        document.addEventListener("DOMContentLoaded", function() {
            const lastActiveTab = localStorage.getItem("student_active_tab") || "panel-schedule";
            switchDashboardTab(lastActiveTab);
        });

        function switchDashboardTab(targetPanelId) {
            document.querySelectorAll('.sidebar-menu-item').forEach(element => {
                element.classList.remove('active-tab');
            });
            
            document.querySelectorAll('.tab-panel-view').forEach(panel => {
                panel.classList.remove('active-panel');
            });
            
            if(targetPanelId === 'panel-schedule') document.getElementById('link-schedule').classList.add('active-tab');
            if(targetPanelId === 'panel-profile') document.getElementById('link-profile').classList.add('active-tab');
            if(targetPanelId === 'panel-enrollment') document.getElementById('link-enrollment').classList.add('active-tab');
            
            const targetedWindow = document.getElementById(targetPanelId);
            if (targetedWindow) {
                targetedWindow.classList.add('active-panel');
                localStorage.setItem("student_active_tab", targetPanelId);
            }
        }
    </script>
</body>
</html>