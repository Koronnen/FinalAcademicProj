<%-- 
    Document   : InstructorProfile
    Created on : 05 23, 26, 8:14:57 PM
    Author     : Javo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="model.Schedule"%>
<%
    // 1. Get the current active session state
    HttpSession activeSession = request.getSession(false);

    // 2. Fetch the integer role status safely
    Object roleObj = (activeSession != null) ? activeSession.getAttribute("role") : null;
    int userType = (roleObj instanceof Integer) ? (Integer) roleObj : -1;

    if (userType != 3) {
        System.out.println("reached, nto admin");
        if (activeSession != null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            activeSession.setAttribute("loginError", "Unauthorized access. Administrator privileges required.");
        }
        response.sendRedirect(request.getContextPath() + "/index.jsp");
        return; // Terminates page rendering immediately
    }
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Instructor Dashboard</title>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <style>            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
                font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            }
            body {
                background-color: #f4f6f9; 
                color: #333;
                display: flex;
                height: 100vh;
                overflow: hidden;
            }

            .sidebar {
                width: 260px;
                background-color: #243040; 
                color: white;
                display: flex;
                flex-direction: column;
                justify-content: space-between;
            }
            .sidebar-brand {
                padding: 25px 20px;
                font-size: 1.2rem;
                font-weight: bold;
                display: flex;
                align-items: center;
                gap: 10px;
                border-bottom: 1px solid rgba(255, 255, 255, 0.05);
            }
            .sidebar-menu {
                list-style: none;
                margin-top: 20px;
            }
            .sidebar-menu li {
                display: flex;
                cursor: pointer;
                transition: 0.2s;
            }
            .sidebar-menu li a {
                padding: 15px 20px;
                display: flex;
                align-items: center;
                gap: 15px;
                color: #aeb6c0;
                text-decoration: none;
                width: 100%;
                transition: 0.2s;
            }
            .sidebar-menu li:hover a { 
                color: white; 
            }
            .sidebar-menu li.active {
                background-color: #1a2330;
                border-left: 4px solid #3b82f6;
            }
            .sidebar-menu li.active a {
                color: white;
            }
            .logout-container {
                padding: 20px;
                border-top: 1px solid rgba(255, 255, 255, 0.05);
            }
            .logout-btn {
                color: #ef4444;
                text-decoration: none;
                display: flex;
                align-items: center;
                gap: 15px;
                font-weight: 600;
            }

            .main-content {
                flex: 1;
                display: flex;
                flex-direction: column;
                overflow-y: auto;
            }
            .header {
                background-color: white;
                padding: 20px 40px;
                display: flex;
                justify-content: space-between;
                align-items: center;
                border-bottom: 1px solid #e5e7eb;
            }
            .header-title {
                font-size: 1.4rem;
                font-weight: 700;
                color: #1e293b;
            }
            .user-info {
                text-align: right;
                font-size: 0.9rem;
                color: #64748b;
            }
            .user-name {
                font-weight: bold;
                color: #1e293b;
                display: block;
            }

            .content-area { padding: 30px 40px; }
            .section-title {
                font-size: 1.2rem;
                font-weight: 700;
                margin-bottom: 15px;
                color: #1e293b;
            }
            .card {
                background: white;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.04);
                padding: 25px;
                margin-bottom: 30px;
            }

            table {
                width: 100%;
                border-collapse: collapse;
            }
            th {
                text-align: left;
                padding: 12px 15px;
                border-bottom: 2px solid #e5e7eb;
                color: #1e293b;
                font-weight: 700;
                font-size: 0.95rem;
            }
            td {
                padding: 15px;
                border-bottom: 1px solid #e5e7eb;
                color: #334155;
                font-size: 0.95rem;
                vertical-align: middle;
            }
            tr:last-child td { border-bottom: none; }

            .form-grid {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 20px;
            }
            .form-group { margin-bottom: 15px; }
            .form-group.full-width { grid-column: span 2; }
            label {
                display: block;
                font-weight: 600;
                margin-bottom: 8px;
                color: #475569;
                font-size: 0.9rem;
            }
            select, input[type="time"] {
                width: 100%;
                padding: 10px 12px;
                border: 1px solid #cbd5e1;
                border-radius: 6px;
                font-size: 0.95rem;
                outline: none;
                transition: border-color 0.2s;
            }
            select:focus, input[type="time"]:focus { border-color: #3b82f6; }

            .btn {
                padding: 10px 20px;
                border-radius: 6px;
                font-weight: 600;
                cursor: pointer;
                border: none;
                transition: 0.2s;
            }
            .btn-primary { background-color: #3b82f6; color: white; margin-top: 10px; }
            .btn-primary:hover { background-color: #2563eb; }
            .btn-secondary { background-color: #94a3b8; color: white; margin-top: 10px;}
            .btn-secondary:hover { background-color: #64748b; }

            .btn-edit {
                background-color: #f1f5f9;
                color: #475569;
                padding: 6px 12px;
                margin-right: 8px;
                font-size: 0.85rem;
                border: none;
                border-radius: 4px;
                font-weight: 600;
                cursor: pointer;
            }
            .btn-edit:hover { background-color: #e2e8f0; }

            .btn-delete {
                background-color: #fef2f2;
                color: #ef4444;
                padding: 6px 12px;
                font-size: 0.85rem;
                border: none;
                border-radius: 4px;
                font-weight: 600;
                cursor: pointer;
            }
            .btn-delete:hover { background-color: #fee2e2; }

            .modal-overlay {
                display: none;
                position: fixed;
                top: 0; left: 0; width: 100%; height: 100%;
                background-color: rgba(15, 23, 42, 0.6);
                z-index: 1000;
                justify-content: center;
                align-items: center;
                backdrop-filter: blur(3px);
            }
            .modal-content {
                background: white;
                padding: 30px;
                border-radius: 12px;
                width: 100%;
                max-width: 500px;
                box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
                transform: translateY(-20px);
                transition: transform 0.3s ease-out;
            }
            .modal-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 20px;
                padding-bottom: 15px;
                border-bottom: 1px solid #e5e7eb;
            }
            .modal-header h3 { color: #1e293b; font-size: 1.25rem; }
            .close-modal-btn {
                background: none; border: none; font-size: 1.5rem; color: #94a3b8; cursor: pointer;
            }
            .close-modal-btn:hover { color: #ef4444; }
            .modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 15px; }
        </style>
    </head>
    <body>
        <%
            String firstName = (session.getAttribute("firstName") != null) ? session.getAttribute("firstName").toString() : "Instructor";
            String lastName = (session.getAttribute("lastName") != null) ? session.getAttribute("lastName").toString() : "";
            String fullName = firstName + (lastName.isEmpty() ? "" : " " + lastName);
        %>

        <aside class="sidebar">
            <div>
                <div class="sidebar-brand">
                    <i class="fa-solid fa-graduation-cap" style="color: #60a5fa;"></i>
                    Active Learning
                </div>
                <ul class="sidebar-menu">
                    <li>
                        <a href="InstructorProfileServlet">
                            <i class="fa-regular fa-user"></i>
                            My Profile
                        </a>
                    </li>                     
                    <li class="active">
                        <a href="InstructorDashboardServlet">
                            <i class="fa-regular fa-calendar-days"></i>
                            My Timetable
                        </a>
                    </li>
                </ul>
            </div>

            <div class="logout-container">
                <a href="index.jsp" class="logout-btn">
                    <i class="fa-solid fa-arrow-right-from-bracket"></i>
                    Log Out Account
                </a>
            </div>
        </aside>

        <main class="main-content">
            <header class="header">
                <div class="header-title">Instructor Dashboard Console</div>
                <div class="user-info">
                    Welcome back,
                    <span class="user-name"><%= fullName%></span>
                </div>
            </header>

            <div class="content-area">

                <h2 class="section-title" style="display:flex; justify-content:space-between; align-items:center;">
                    My Registered Academic Syllabus
                    <a href="InstructorScheduleReportServlet"
                       style="font-size:0.85rem; font-weight:600; color:#ef4444; text-decoration:none;
                              border:1px solid #ef4444; padding:7px 14px; border-radius:6px;
                              display:inline-flex; align-items:center; gap:6px; transition:background 0.2s, color 0.2s;"
                       onmouseover="this.style.background='#ef4444';this.style.color='#fff';"
                       onmouseout="this.style.background='transparent';this.style.color='#ef4444';"
                       title="Download your weekly teaching schedule as a PDF">
                        <i class="fa-solid fa-file-pdf"></i> Download Schedule PDF
                    </a>
                </h2>
                <div class="card">
                    <table style="width: 100%; border-collapse: collapse;">
                        <thead>
                            <tr>
                                <th style="padding: 12px 15px; border-bottom: 2px solid #e5e7eb; text-align: left; color: #64748b;">Course Name</th>
                                <th style="padding: 12px 15px; border-bottom: 2px solid #e5e7eb; text-align: left; color: #64748b;">Day of Week</th>
                                <th style="padding: 12px 15px; border-bottom: 2px solid #e5e7eb; text-align: left; color: #64748b;">Assigned Timetable Slot</th>
                                <th style="padding: 12px 15px; border-bottom: 2px solid #e5e7eb; text-align: left; color: #64748b;">Enrolled</th>
                                <th style="padding: 12px 15px; border-bottom: 2px solid #e5e7eb; text-align: right; color: #64748b;">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <%
                                Map<String, List<Schedule>> groupedSchedules = (Map<String, List<Schedule>>) request.getAttribute("groupedSchedules");

                                if (groupedSchedules != null && !groupedSchedules.isEmpty()) {
                                    for (Map.Entry<String, List<Schedule>> entry : groupedSchedules.entrySet()) {
                                        String courseName = entry.getKey();
                                        List<Schedule> schedules = entry.getValue();

                                        int rowSpanCount = schedules.size();
                                        boolean isFirstRow = true;

                                        for (Schedule sched : schedules) {
                            %>
                            <tr>
                                <%-- ONLY print the course name cell if it's the first row for this subject --%>
                                <% if (isFirstRow) {%>
                                <td rowspan="<%= rowSpanCount%>" style="padding: 15px; border-bottom: 1px solid #cbd5e1; border-right: 1px solid #f1f5f9; color: #0f172a; font-weight: bold; vertical-align: top;">
                                    <i class="fa-solid fa-book-bookmark" style="color: #3b82f6; margin-right: 8px;"></i>
                                    <%= courseName%>
                                </td>
                                <% }%>

                                <td style="padding: 15px; border-bottom: 1px solid #f1f5f9; color: #475569; font-weight: 500;">
                                    <%= sched.getDayOfWeek()%>
                                </td>
                                <td style="padding: 15px; border-bottom: 1px solid #f1f5f9; color: #475569;">
                                    <i class="fa-regular fa-clock" style="color: #94a3b8; margin-right: 5px;"></i>
                                    <%= sched.getStartTime()%> - <%= sched.getEndTime()%>
                                </td>
                                <td style="padding: 15px; border-bottom: 1px solid #f1f5f9; color: #16a34a; font-weight: bold;">
                                    <i class="fa-solid fa-user-group" style="margin-right: 5px;"></i>
                                    <%= sched.getStudentCount()%>
                                </td>
                                <td style="padding: 15px; border-bottom: 1px solid #f1f5f9; text-align: right;">

                                    <button type="button" class="btn-edit" onclick="openEditModal('<%= sched.getSchedId()%>', '<%= sched.getDayOfWeek()%>', '<%= sched.getStartTime()%>', '<%= sched.getEndTime()%>')">
                                        Edit
                                    </button>

                                    <form action="InstructorDashboardServlet" method="POST" style="display: inline-block; margin: 0;">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="scheduleId" value="<%= sched.getSchedId()%>">
                                        <button type="submit" class="btn-delete" onclick="return confirm('Are you sure you want to delete this schedule?');">
                                            Delete
                                        </button>
                                    </form>

                                </td>
                            </tr>
                            <%
                                        isFirstRow = false;
                                    }
                                }
                            } else {
                            %>
                            <tr>
                                <td colspan="5" style="text-align: center; padding: 30px; color: #94a3b8;">
                                    No syllabus registered yet.
                                </td>
                            </tr>
                            <% }%>
                        </tbody>
                    </table>
                </div>

                <h2 class="section-title">Schedule New Course</h2>
                <div class="card">
                    <form action="InstructorDashboardServlet" method="POST">
                        <div class="form-grid">
                            <div class="form-group">
                                <label for="courseSelect">Select Course:</label>
                                <select id="courseSelect" name="course_id">
                                    <%
                                        Map<String, String> allCourses = (Map<String, String>) request.getAttribute("allCourses");
                                        if (allCourses != null && !allCourses.isEmpty()) {
                                            for (Map.Entry<String, String> course : allCourses.entrySet()) {
                                    %>
                                    <option value="<%= course.getKey() %>"><%= course.getValue() %></option>
                                    <%
                                            }
                                        } else {
                                    %>
                                    <option value="" disabled>No courses available</option>
                                    <%  } %>
                                </select>
                            </div>

                            <div class="form-group">
                                <label for="daySelect">Day of the Week:</label>
                                <select id="daySelect" name="day">
                                    <option value="Monday">Monday</option>
                                    <option value="Tuesday">Tuesday</option>
                                    <option value="Wednesday">Wednesday</option>
                                    <option value="Thursday">Thursday</option>
                                    <option value="Friday">Friday</option>
                                    <option value="Saturday">Saturday</option>
                                </select>
                            </div>

                            <div class="form-group">
                                <label for="startTime">Start Time:</label>
                                <input type="time" id="startTime" name="start_time">
                            </div>

                            <div class="form-group">
                                <label for="endTime">End Time:</label>
                                <input type="time" id="endTime" name="end_time">
                            </div>
                        </div>

                        <button type="submit" class="btn btn-primary">Add New Schedule</button>
                    </form>
                </div>

            </div>
        </main>

        <div id="editModal" class="modal-overlay">
            <div class="modal-content" id="modalBox">
                <div class="modal-header">
                    <h3>Edit Timetable Slot</h3>
                    <button class="close-modal-btn" onclick="closeEditModal()">&times;</button>
                </div>

                <form action="EditScheduleServlet" method="POST">
                    <input type="hidden" name="scheduleId" id="edit_scheduleId">

                    <div class="form-grid">
                        <div class="form-group full-width">
                            <label>Day of the Week:</label>
                            <select name="day" id="edit_dayOfWeek">
                                <option value="MONDAY">Monday</option>
                                <option value="TUESDAY">Tuesday</option>
                                <option value="WEDNESDAY">Wednesday</option>
                                <option value="THURSDAY">Thursday</option>
                                <option value="FRIDAY">Friday</option>
                                <option value="SATURDAY">Saturday</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <label>Start Time:</label>
                            <input type="time" name="start_time" id="edit_startTime" required>
                        </div>

                        <div class="form-group">
                            <label>End Time:</label>
                            <input type="time" name="end_time" id="edit_endTime" required>
                        </div>
                    </div>

                    <div class="modal-actions">
                        <button type="button" class="btn btn-secondary" onclick="closeEditModal()">Cancel</button>
                        <button type="submit" class="btn btn-primary" style="margin-top: 0;">Save Changes</button>
                    </div>
                </form>
            </div>
        </div>

        <script>
            function openEditModal(scheduleId, dayOfWeek, startTime, endTime) {
                document.getElementById("edit_scheduleId").value = scheduleId;
                document.getElementById("edit_dayOfWeek").value = dayOfWeek;

                if (startTime.length > 5)
                    startTime = startTime.substring(0, 5);
                if (endTime.length > 5)
                    endTime = endTime.substring(0, 5);

                document.getElementById("edit_startTime").value = startTime;
                document.getElementById("edit_endTime").value = endTime;

                const modal = document.getElementById("editModal");
                const modalBox = document.getElementById("modalBox");
                modal.style.display = "flex";

                setTimeout(() => {
                    modalBox.style.transform = "translateY(0)";
                }, 10);
            }

            function closeEditModal() {
                const modal = document.getElementById("editModal");
                const modalBox = document.getElementById("modalBox");

                modalBox.style.transform = "translateY(-20px)";
                setTimeout(() => {
                    modal.style.display = "none";
                }, 300);
            }

            window.onclick = function (event) {
                const modal = document.getElementById("editModal");
                if (event.target === modal) {
                    closeEditModal();
                }
            }
        </script>
    </body>
</html>