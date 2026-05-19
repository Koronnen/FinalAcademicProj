<%-- 
    Document   : error_3
    Created on : 02 16, 26, 7:48:53 PM
    Author     : Vinz
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" href="styles/errorStyles.css">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Login Error</title>
    </head>
    <body>
        <header>
            <jsp:include page="<%= (String)application.getAttribute("header") %>" />
        </header>
        <main>  
            <h1>Uh oh!</h1>
            <p>You have entered the wrong Email(Username) and Password!</p>
             <a href="index.jsp"><<< Go back</a>
        </main>
        <footer>
            <jsp:include page="<%= (String)application.getAttribute("footer") %>" />
        </footer>
    </body>
</html>
