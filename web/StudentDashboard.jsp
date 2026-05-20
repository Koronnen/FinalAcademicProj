<%-- 
    Document   : StudentDashboard
    Created on : 05 20, 26, 9:37:12 AM
    Author     : Vinz
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.sql.*;"%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
    
    if (request.getAttribute("displayName") == null) { 
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
    }
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Student Dashboard</title>
    </head>
    <body>
        <div class="welcome"> 
        <h1> Welcome back to your dashboard, <%= request.getAttribute("displayName") == null %>!</h1> 
        </div>
        <div class="profile-form">
            <h2>Edit your Profile Information</h2>
            <form action="${pageContext.request.contextPath}/StudentServlet" method="POST">
                <div class="form-group">
                    <label>First Name:</label>
                    <input type="text" name="fname" value="<%= request.getAttribute("fname") %>" placeholder="Enter your first name" required>
                </div>
                <div class="form-group">
                    <label>Last Name:</label>
                    <input type="text" name="lname" value="<%= request.getAttribute("lname") %>" placeholder="Enter your first name" required>
                </div>
                <div class="form-group">
                    <label>Email Address:</label>
                    <input type="text" name="fname" value="<%= request.getAttribute("email") %>" placeholder="Enter your e" required>
                </div>
                <br>
                <button type="submit">Save Updates</button>
            </form>
        </div>

    </body>
        
</html>
