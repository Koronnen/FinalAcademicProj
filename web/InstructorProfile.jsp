<%-- 
    Document   : InstructorProfile
    Created on : 05 23, 26, 8:14:57 PM
    Author     : Javo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
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

            .section-title { font-size: 1.2rem; font-weight: 700; margin-bottom: 15px; color: #1e293b; }
            .card { background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.04); padding: 25px; }

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
        </style>
    </head>
    <body>
        <%
            String totalSchedules = request.getAttribute("totalSchedules") != null ? String.valueOf(request.getAttribute("totalSchedules")) : "0";
            String totalSubjects = request.getAttribute("totalSubjects") != null ? String.valueOf(request.getAttribute("totalSubjects")) : "0";
            String totalHours = request.getAttribute("totalHours") != null ? String.valueOf(request.getAttribute("totalHours")) : "0";
            List<String> subjectList = (List<String>) request.getAttribute("subjectList");
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

                <h2 class="section-title">Subjects Currently Handled</h2>
                <div class="card">
                    <table>
                        <tbody>
                            <%
                                if (subjectList != null && !subjectList.isEmpty()) {
                                    for (String subject : subjectList) {
                            %>
                            <tr>
                                <td>
                                    <span class="course-badge">ACTIVE</span> 
                                    <%= subject%>
                                </td>
                            </tr>
                            <%
                                }
                            } else {
                            %>
                            <tr>
                                <td style="text-align: center; color: #64748b; padding: 20px;">
                                    No subjects assigned yet.
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