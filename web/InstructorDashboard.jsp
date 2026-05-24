<%-- 
    Document   : InstructorDashboard
    Created on : 05 20, 26, 9:37:56 AM
    Author     : Vinz / AI Collaborator
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List, java.util.Map"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Instructor Dashboard</title>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <style>
            /* Reset and Base Styles */
            * {
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

            /* Sidebar Styles - Fixed Link Styling */
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

            /* Content Area and Cards */
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

            /* Table Styles */
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
            .course-name { font-weight: 600; color: #1e293b; }
            .time-slot {
                color: #64748b;
                display: flex;
                align-items: center;
                gap: 8px;
            }

            /* Form Styles */
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

            /* Buttons */
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
                margin-right: 5px;
                font-size: 0.85rem;
            }
            .btn-delete {
                background-color: #fef2f2;
                color: #ef4444;
                padding: 6px 12px;
                font-size: 0.85rem;
            }
            .btn-edit:hover { background-color: #e2e8f0; }
            .btn-delete:hover { background-color: #fee2e2; }

            /* GUI MODAL STYLES */
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
            // Fallback safe session variable reading for Instructor Name
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
                    <span class="user-name"><%= fullName %></span>
                </div>
            </header>

            <div class="content-area">

                <h2 class="section-title">My Registered Academic Syllabus</h2>
                <div class="card">
                    <table>
                        <thead>
                            <tr>
                                <th>Day of Week</th>
                                <th>Course Name</th>
                                <th>Assigned Timetable Slot</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <%
                                List<Map<String, String>> scheduleList = (List<Map<String, String>>) request.getAttribute("scheduleList");

                                if (scheduleList != null && !scheduleList.isEmpty()) {
                                    for (Map<String, String> sched : scheduleList) {
                            %>
                            <tr>
                                <td><%= sched.get("dayOfWeek")%></td>
                                <td class="course-name"><%= sched.get("courseName")%></td>
                                <td>
                                    <div class="time-slot">
                                        <i class="fa-regular fa-clock"></i>
                                        <%= sched.get("timeStart")%> - <%= sched.get("timeEnd")%>
                                    </div>
                                </td>
                                <td>
                                    <button type="button" class="btn btn-edit" 
                                            onclick="openEditModal('<%= sched.get("scheduleId")%>', '<%= sched.get("dayOfWeek")%>', '<%= sched.get("timeStart")%>', '<%= sched.get("timeEnd")%>')">
                                        Edit
                                    </button>

                                    <form action="InstructorDashboardServlet" method="POST" style="display:inline;">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="scheduleId" value="<%= sched.get("scheduleId")%>">
                                        <button type="submit" class="btn btn-delete" onclick="return confirm('Are you sure you want to delete this schedule?');">Delete</button>
                                    </form>
                                </td>
                            </tr>
                            <%
                                    }
                                } else {
                            %>
                            <tr>
                                <td colspan="4" style="text-align: center; color: #64748b; padding: 30px;">
                                    No schedules added yet. Create one below!
                                </td>
                            </tr>
                            <%
                                }
                            %>
                        </tbody>
                    </table>
                </div>

                <h2 class="section-title">Schedule New Course</h2>
                <div class="card">
                    <form action="InstructorDashboardServlet" method="POST">
                        <div class="form-grid">
                            <div class="form-group full-width">
                                <label>Select Course:</label>
                                <select name="course_id">
                                    <option value="CRS000001">Advanced Probability and Statistics</option>
                                    <option value="CRS000002">Applications Development</option>
                                </select>
                            </div>

                            <div class="form-group full-width">
                                <label>Day of the Week:</label>
                                <select name="day">
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
                                <input type="time" name="start_time" required>
                            </div>

                            <div class="form-group">
                                <label>End Time:</label>
                                <input type="time" name="end_time" required>
                            </div>
                        </div>

                        <input type="submit" class="btn btn-primary" value="Add Schedule">
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
                
                if (startTime.length > 5) startTime = startTime.substring(0, 5);
                if (endTime.length > 5) endTime = endTime.substring(0, 5);
                
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

            window.onclick = function(event) {
                const modal = document.getElementById("editModal");
                if (event.target === modal) {
                    closeEditModal();
                }
            }
        </script>
    </body>
</html>