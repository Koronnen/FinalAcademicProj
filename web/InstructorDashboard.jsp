<%-- 
    Document   : InstructorDashboard
    Created on : 05 20, 26, 9:37:56 AM
    Author     : Vinz
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
    
    // 1. Get the current active session state
    HttpSession activeSession = request.getSession(false);

    // 2. Fetch the integer role status safely
    Object roleObj = (activeSession != null) ? activeSession.getAttribute("role") : null;
    int userType = (roleObj instanceof Integer) ? (Integer) roleObj : -1;

    // 3. Kick them out if they are NOT an Instructor (2)
    if (userType != 3) { 
        System.out.println("Access denied: User is not an instructor.");
        if (activeSession != null) {
            activeSession.setAttribute("loginError", "Access denied. Instructor privileges required.");
        }
        response.sendRedirect(request.getContextPath() + "/index.jsp");
        return; 
    }

%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <h1>Hello Instructor!</h1>
    </body>
</html>
