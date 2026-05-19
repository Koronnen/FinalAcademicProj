<%-- 
    Document   : error_session.jsp
    Created on : 02 16, 26, 6:53:29 PM
    Author     : Vinz
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" href="styles/errorStyles.css">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Session Invalid</title>
    </head>
    <body>
        <header>
            <jsp:include page="<%= (String) application.getAttribute("header")%>" />
        </header>
        <main>  
            <h1>Session error</h1>
            <p>
                For your security, your session has timed out due to inactivity or an invalid access attempt. 
                Please return to the login page to re-authenticate.
            </p>
            <a href="index.jsp"><<< Go back</a>
        </main>
        <footer>
            <jsp:include page="<%= (String) application.getAttribute("footer")%>" />
        </footer>
    </body>
</html>
