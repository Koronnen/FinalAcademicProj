<%-- 
    Document   : InstructorProfile
    Created on : 05 23, 26, 8:14:57 PM
    Author     : Javo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="model.Schedule"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Instructor Profile</title>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
            body { background-color: #f4f6f9; color: #333; display: flex; height: 100vh; overflow: hidden; }

            .sidebar { width: 260px; background-color: #243040; color: white; display: flex; flex-direction: column; justify-content: space-between; }
            .sidebar-brand { padding: 25px 20px; font-size: 1.2rem; font-weight: bold; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
            .sidebar-menu { list-style: none; margin-top: 20px; }
            .sidebar-menu li a { padding: 15px 20px; display: flex; align-items: center; gap: 15px; color: #aeb6c0; text-decoration: none; transition: 0.2s; }
            .sidebar-menu li a:hover { color: white; }
            .sidebar-menu li.active a { background-color: #1a2330; color: white; border-left: 4px solid #3b82f6; }
            .logout-container { padding: 20px; border-top: 1px solid rgba(255, 255, 255, 0.05); }
            .logout-btn { color: #ef4444; text-decoration: none; display: flex; align-items: center; gap: 15px; font-weight: 600; }

            .main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; }
            .header { background-color: white; padding: 20px 40px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e5e7eb; }
            .header-title { font-size: 1.4rem; font-weight: 700; color: #1e293b; }
            .user-info { text-align: right; font-size: 0.9rem; color: #64748b; }
            .user-name { font-weight: bold; color: #1e293b; display: block; }

            .content-area { padding: 30px 40px; }

            .stats-grid {
                display: grid;
                grid-template-columns: repeat(3, 1fr);
                gap: 20px;
                margin-bottom: 30px;
            }
            .stat-card {
                background: white;
                border-radius: 8px;
                padding: 25px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.04);
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            .stat-info h4 {
                font-size: 0.85rem;
                color: #64748b;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                margin-bottom: 8px;
            }
            .stat-info .number {
                font-size: 2rem;
                font-weight: 700;
                color: #1e293b;
            }
            .stat-icon {
                width: 60px;
                height: 60px;
                border-radius: 12px;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 1.8rem;
            }
            .icon-blue { background: #e0e7ff; color: #4f46e5; }
            .icon-green { background: #dcfce7; color: #16a34a; }
            .icon-yellow { background: #fef3c7; color: #d97706; }

            .section-title { font-size: 1.2rem; font-weight: 700; margin-bottom: 15px; color: #1e293b; margin-top: 30px; }
            .card { background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.04); padding: 25px; margin-bottom: 30px; }

            table { width: 100%; border-collapse: collapse; }
            th { text-align: left; padding: 12px 15px; border-bottom: 2px solid #e5e7eb; color: #1e293b; font-weight: 700; font-size: 0.95rem; }
            td { padding: 15px; border-bottom: 1px solid #e5e7eb; color: #334155; font-size: 0.95rem; vertical-align: middle; font-weight: 600;}
            tr:last-child td { border-bottom: none; }

            .course-badge {
                background-color: #3b82f6;
                color: white;
                padding: 4px 10px;
                border-radius: 4px;
                font-size: 0.8rem;
                font-weight: bold;
                margin-right: 15px;
            }

            .calendar-grid {
                display: grid;
                grid-template-columns: repeat(6, 1fr);
                gap: 12px;
                margin-top: 5px;
            }
            .calendar-col {
                background: #ffffff;
                border: 1px solid #e2e8f0;
                border-radius: 8px;
                min-height: 180px;
                padding: 12px;
                display: flex;
                flex-direction: column;
                gap: 8px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.02);
            }
            .calendar-col.today-highlight {
                border: 2px solid #3b82f6;
                background-color: #f8fafc;
            }
            .col-header {
                text-align: center;
                padding-bottom: 8px;
                border-bottom: 2px solid #f1f5f9;
                margin-bottom: 4px;
            }
            .col-day {
                font-weight: 700;
                font-size: 0.85rem;
                color: #1e293b;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }
            .col-date {
                font-size: 0.8rem;
                color: #64748b;
                font-weight: 500;
                margin-top: 2px;
            }
            .today-badge {
                display: inline-block;
                background: #3b82f6;
                color: white;
                font-size: 0.65rem;
                font-weight: bold;
                padding: 1px 5px;
                border-radius: 3px;
                margin-bottom: 2px;
            }
            .events-list {
                display: flex;
                flex-direction: column;
                gap: 6px;
                flex-grow: 1;
            }
            .no-classes-text {
                font-size: 0.75rem;
                color: #94a3b8;
                text-align: center;
                margin-top: 20px;
                font-style: italic;
            }

            .calendar-event {
                background-color: #eff6ff;
                color: #1e40af;
                border-left: 4px solid #3b82f6;
                padding: 8px 10px;
                border-radius: 4px;
                font-size: 0.8rem;
                font-weight: 700;
                position: relative;
                cursor: pointer;
                transition: all 0.2s ease;
                box-shadow: 0 1px 2px rgba(0,0,0,0.05);
            }
            .calendar-event:hover {
                background-color: #dbeafe;
                transform: translateY(-1px);
            }

            .calendar-event .tooltip {
                visibility: hidden;
                width: 210px;
                background-color: #1e293b;
                color: #ffffff;
                text-align: left;
                border-radius: 6px;
                padding: 10px 12px;
                position: absolute;
                z-index: 99;
                bottom: 115%; 
                left: 50%;
                transform: translateX(-50%);
                opacity: 0;
                transition: opacity 0.2s ease, transform 0.2s ease;
                box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
                font-size: 0.78rem;
                font-weight: 500;
                line-height: 1.5;
            }
            .calendar-event .tooltip::after {
                content: "";
                position: absolute;
                top: 100%;
                left: 50%;
                margin-left: -6px;
                border-width: 6px;
                border-style: solid;
                border-color: #1e293b transparent transparent transparent;
            }
            .calendar-event:hover .tooltip {
                visibility: visible;
                opacity: 1;
                transform: translateX(-50%) translateY(-2px);
            }
        </style>
    </head>
    <body>
        <%
            String totalSchedules = request.getAttribute("totalSchedules") != null ? String.valueOf(request.getAttribute("totalSchedules")) : "0";
            String totalSubjects = request.getAttribute("totalSubjects") != null ? String.valueOf(request.getAttribute("totalSubjects")) : "0";
            String totalHours = request.getAttribute("totalHours") != null ? String.valueOf(request.getAttribute("totalHours")) : "0";

            // Unpacking the neatly grouped schedules map from your servlet
            Map<String, List<Schedule>> groupedSchedules = (Map<String, List<Schedule>>) request.getAttribute("groupedSchedules");
        %>

        <aside class="sidebar">
            <div>
                <div class="sidebar-brand">
                    <i class="fa-solid fa-graduation-cap" style="color: #60a5fa;"></i>
                    Active Learning
                </div>
                <ul class="sidebar-menu">
                    <li class="active">
                        <a href="InstructorProfileServlet">
                            <i class="fa-regular fa-user"></i>
                            My Profile
                        </a>
                    </li>                     
                    <li>
                        <a href="InstructorDashboardServlet">
                            <i class="fa-regular fa-calendar-days"></i>
                            My Timetable
                        </a>
                    </li>
                </ul>
            </div>

            <div class="logout-container">
                <a href="LogOutServlet" class="logout-btn">
                    <i class="fa-solid fa-arrow-right-from-bracket"></i>
                    Log Out Account
                </a>
            </div>
        </aside>

        <main class="main-content">
            <header class="header">
                <div class="header-title">Instructor Overview Console</div>
                <div class="user-info">
                    Welcome back,
                    <span class="user-name">Instructor</span>
                </div>
            </header>

            <div class="content-area">

                <div class="stats-grid">
                    <div class="stat-card">
                        <div class="stat-info">
                            <h4>Total Schedules</h4>
                            <div class="number"><%= totalSchedules%></div>
                        </div>
                        <div class="stat-icon icon-blue">
                            <i class="fa-solid fa-calendar-check"></i>
                        </div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-info">
                            <h4>Managed Subjects</h4>
                            <div class="number"><%= totalSubjects%></div>
                        </div>
                        <div class="stat-icon icon-green">
                            <i class="fa-solid fa-book-open"></i>
                        </div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-info">
                            <h4>Total Weekly Hours</h4>
                            <div class="number"><%= totalHours%></div>
                        </div>
                        <div class="stat-icon icon-yellow">
                            <i class="fa-solid fa-clock"></i>
                        </div>
                    </div>
                </div>

                <h2 class="section-title" style="margin-top: 0;">Weekly Visual Planner</h2>
                <div class="calendar-grid">
                    <%
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        java.util.Calendar realToday = java.util.Calendar.getInstance();
                        java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEEE");
                        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd");

                        for (int i = 0; i < 7; i++) {
                            if (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY) {
                                String currentDayName = dayFormat.format(cal.getTime());
                                String currentStringDate = dateFormat.format(cal.getTime());

                                boolean isToday = (cal.get(java.util.Calendar.DAY_OF_YEAR) == realToday.get(java.util.Calendar.DAY_OF_YEAR)
                                        && cal.get(java.util.Calendar.YEAR) == realToday.get(java.util.Calendar.YEAR));
                    %>
                    <div class="calendar-col <%= isToday ? "today-highlight" : ""%>">
                        <div class="col-header">
                            <% if (isToday) { %>
                            <span class="today-badge">TODAY</span>
                            <% }%>
                            <div class="col-day"><%= currentDayName%></div>
                            <div class="col-date"><%= currentStringDate%></div>
                        </div>
                        <div class="events-list">
                            <%
                                boolean hasClasses = false;
                                if (groupedSchedules != null && !groupedSchedules.isEmpty()) {
                                    for (Map.Entry<String, List<Schedule>> entry : groupedSchedules.entrySet()) {
                                        String courseName = entry.getKey();
                                        for (Schedule sched : entry.getValue()) {

                                            String targetDay = sched.getDayOfWeek().toUpperCase();
                                            String systemDay = currentDayName.toUpperCase();
                                            if (targetDay.contains(systemDay) || systemDay.contains(targetDay)
                                                    || (targetDay.length() >= 3 && systemDay.startsWith(targetDay.substring(0, 3)))) {
                                                hasClasses = true;
                            %>
                            <div class="calendar-event">
                                <span class="event-name"><%= courseName%></span>

                                <div class="tooltip">
                                    <div style="border-bottom: 1px solid #475569; padding-bottom: 4px; margin-bottom: 6px; font-weight: 700; color: #60a5fa;">
                                        <%= courseName%>
                                    </div>
                                    <i class="fa-regular fa-clock" style="color: #fbbf24; margin-right: 4px;"></i> 
                                    <%= sched.getStartTime()%> – <%= sched.getEndTime()%><br/>
                                    <i class="fa-solid fa-users" style="color: #34d399; margin-right: 4px;"></i> 
                                    <%= sched.getStudentCount()%> Students Enrolled
                                </div>
                            </div>
                            <%
                                            }
                                        }
                                    }
                                }
                                if (!hasClasses) {
                            %>
                            <div class="no-classes-text">No Classes</div>
                            <%
                                }
                            %>
                        </div>
                    </div>
                    <%
                            }
                            cal.add(java.util.Calendar.DATE, 1);
                        }
                    %>
                </div>

                <h2 class="section-title">Detailed Schedule Assignment</h2>
                <div class="card">
                    <table>
                        <thead>
                            <tr>
                                <th>Subject / Course Name</th>
                                <th>Day of Week</th>
                                <th>Class Time Window</th>
                                <th>Class Size</th>
                            </tr>
                        </thead>
                        <tbody>
                            <%
                                if (groupedSchedules != null && !groupedSchedules.isEmpty()) {
                                    for (Map.Entry<String, List<Schedule>> entry : groupedSchedules.entrySet()) {
                                        String courseName = entry.getKey();
                                        List<Schedule> schedules = entry.getValue();

                                        for (Schedule sched : schedules) {
                            %>
                            <tr>
                                <td>
                                    <span class="course-badge">ACTIVE</span> 
                                    <%= courseName%>
                                </td>
                                <td>
                                    <i class="fa-regular fa-calendar" style="color: #4f46e5; margin-right: 8px;"></i>
                                    <%= sched.getDayOfWeek()%>
                                </td>
                                <td>
                                    <i class="fa-regular fa-clock" style="color: #d97706; margin-right: 8px;"></i>
                                    <%= sched.getStartTime()%> – <%= sched.getEndTime()%>
                                </td>
                                <td>
                                    <i class="fa-solid fa-users" style="color: #16a34a; margin-right: 8px;"></i>
                                    <%= sched.getStudentCount()%> Enrolled
                                </td>
                            </tr>
                            <%
                                    }
                                }
                            } else {
                            %>
                            <tr>
                                <td colspan="4" style="text-align: center; color: #64748b; padding: 25px;">
                                    <i class="fa-solid fa-inbox" style="font-size: 1.5rem; display: block; margin-bottom: 10px; color: #cbd5e1;"></i>
                                    No schedules assigned to your account yet.
                                </td>
                            </tr>
                            <%
                                }
                            %>
                        </tbody>
                    </table>
                </div>

            </div>
        </main>
    </body>
</html>