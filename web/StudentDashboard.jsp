<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String displayName = (String) request.getAttribute("displayName");
    if (displayName == null || displayName.trim().isEmpty()) {
        displayName = "Student";
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Student Dashboard Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css" rel="stylesheet">
    <style>
        body { font-family: Arial, sans-serif; margin: 30px; }
        :root {
            --sidebar-width: 260px;
            --primary-color: #064e3b;      
            --secondary-color: #022c22;    
            --accent-green: #10b981;       
            --light-bg: #f0fdf4;           
            --success-bg-translucent: rgba(34, 197, 94, 0.15); 
            --success-border-translucent: rgba(34, 197, 94, 0.4); 
            --success-text-color: #15803d;  
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
            color: #a7f3d0; 
            text-decoration: none;
            font-weight: 500;
            transition: all 0.2s ease;
            border-left: 4px solid transparent;
            cursor: pointer;
        }
        .sidebar-menu-item:hover, .sidebar-menu-item.active-tab {
            color: #ffffff;
            background-color: var(--secondary-color);
            border-left-color: var(--accent-green);
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
        .success-status-box {
            background: var(--success-bg-translucent);
            border: 1px solid var(--success-border-translucent);
            color: var(--success-text-color);
            border-radius: 8px;
            padding: 12px 16px;
            font-weight: 500;
        }
        .student-card {
            background: #ffffff;
            border: none;
            border-radius: 10px;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03);
            transition: transform 0.2s;
        }
        .student-card:hover {
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
        .table classical-thead th {
            background-color: #d1fae5; 
            color: #065f46;            
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
        .section-card { border: 1px solid #ddd; padding: 20px; margin-bottom: 25px; border-radius: 5px; background: #fff;}
        .form-field { margin-bottom: 12px; }
        .form-field label { display: inline-block; width: 120px; font-weight: bold; }
        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
    </style>
</head>
<body>

    <div class="header">
        <h1>Welcome back to your dashboard, <%= displayName %>!</h1>
    </div>

    <div class="section-card">
        <h3>Manage Personal Profile Information</h3>
        <form action="${pageContext.request.contextPath}/StudentServlet" method="POST">
            <input type="hidden" name="action" value="updateProfile">
            
            <div class="form-field">
                <label>First Name:</label>
                <input type="text" name="fname" value="<%= request.getAttribute("fname") != null ? request.getAttribute("fname") : "" %>">
            </div>
            <div class="form-field">
                <label>Last Name:</label>
                <input type="text" name="lname" value="<%= request.getAttribute("lname") != null ? request.getAttribute("lname") : "" %>">
            </div>
            <div class="form-field">
                <label>Email Address:</label>
                <input type="email" name="email" value="<%= request.getAttribute("email") != null ? request.getAttribute("email") : "" %>">
            </div>
            
            <button type="submit">Save Changes</button>
        </form>

        <br>
        <form action="${pageContext.request.contextPath}/StudentServlet" method="POST">
            <input type="hidden" name="action" value="clearProfile">
            <button type="submit" onclick="return confirm('Clear optional fields?');" style="color: #cc0000;">Clear Profile Properties</button>
        </form>
    </div>

    <div class="section-card">
        <h3>Available Course Catalog Enrollments</h3>
        <table>
            <thead>
                <tr>
                    <th>Code</th>
                    <th>Course Name</th>
                    <th>Assigned Instructor</th>
                    <th>Choose Schedule Slot</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                <%
                    List<Map<String, Object>> courses = (List<Map<String, Object>>) request.getAttribute("availableCourses");
                    if (courses != null && !courses.isEmpty()) {
                        for (Map<String, Object> course : courses) {
                            List<Map<String, String>> schedules = (List<Map<String, String>>) course.get("schedulesList");
                %>
                <tr>
                    <td><%= course.get("courseCode") %></td>
                    <td><%= course.get("courseName") %></td>
                    <td><%= course.get("instructorName") %></td>
                    <td>
                        <form action="${pageContext.request.contextPath}/StudentServlet" method="POST" style="margin:0;">
                            <input type="hidden" name="action" value="enrollCourse">
                            <input type="hidden" name="instCourseId" value="<%= course.get("instCourseId") %>">
                            
                            <select name="schedId" required>
                                <option value="" disabled selected>-- Select Available Slot --</option>
                                <% 
                                    if (schedules != null) {
                                        for (Map<String, String> slot : schedules) { 
                                %>
                                    <option value="<%= slot.get("schedId") %>"><%= slot.get("timeDetails") %></option>
                                <% 
                                        }
                                    } 
                                %>
                            </select>
                    </td>
                    <td>
                        <button type="submit">Enroll Course</button>
                        </form>
                    </td>
                </tr>
                <% 
                        }
                    } else { 
                %>
                <tr>
                    <td colspan="5">No courses are currently available.</td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </div>

</body>
</html>