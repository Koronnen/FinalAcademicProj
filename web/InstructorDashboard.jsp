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
    
    HttpSession activeSession = request.getSession(false);
    if (activeSession == null || activeSession.getAttribute("USER_ID") == null) {
        activeSession.setAttribute("loginError", "Access denied. Please log-in again.");
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
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
