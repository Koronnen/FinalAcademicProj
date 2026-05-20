<%-- 
    Document   : StudentDashboard
    Created on : 05 20, 26, 9:37:12 AM
    Author     : Vinz
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.sql.*;"%>
<%@page import="controller.Security"%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
    
    if (session.getAttribute("USER_ID") == null || session == null) { 
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
    }
    
    String usrID = (String) session.getAttribute("USER_ID");
    String email = (String) session.getAttribute("email");
    String role = (String) session.getAttribute("USER_ROLE");
    
    String displayName = "";
    String dbStudentId = "";
    String dbFirstName = "";
    String dbLastName  = "";
    String dbEmail = "";
    
    Connection conn;
    PreparedStatement ps;
    if ("STUDENT".equalsIgnoreCase("USER_ROLE")) {
        ps = conn.prepareStatement("SELECT STU_ID, FNAME, LNAME, EMAIL WHERE USER_ID = ?");
        ps.setString(1, email);
    } else {
        response.sendRedirect("index.jsp");
    }
    ResultSet rs = ps.executeQuery();
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Student Dashboard</title>
    </head>
    <body>
        <div class="welcome"> 
        <h1> Welcome back to your dashboard, <%= displayName %>!</h1> 
        </div>
        <div class="profile-form">
            <h2>Edit your Profile Information</h2>
            <form action="${pageContext.request.contextPath}/StudentServlet" method="POST">
                <div class="form-group">
                    <label>First Name:</label>
                    <input type="text" name="fname" value="<%= dbFirstName != null ? dbFirstName : ""%>" placeholder="Enter your first name" required>
                </div>
                <div class="form-group">
                    <label>Last Name:</label>
                    <input type="text" name="lname" value="<%= dbLastName != null ? dbLastName : ""%>" placeholder="Enter your first name" required>
                </div>
                <div class="form-group">
                    <label>Email Address:</label>
                    <input type="text" name="fname" value="<%= dbFirstName != null ? dbFirstName : ""%>" placeholder="Enter your e" required>
                </div>
            </form>
        </div>
        
        
        
        
        
        
    </body>
        
</html>
