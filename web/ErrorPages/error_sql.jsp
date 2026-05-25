<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true"%>
<%@page import="java.sql.SQLException"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Database Error</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/errorStyles.css">
</head>
<body>
    <header>

    </header>

    <main class="error-container">
        <div class="error-card">
            <div class="mb-2">
                <i class="fa-solid fa-database" style="font-size:5rem; color:#ef4444;"></i>
            </div>
            <h1 class="error-title">Database error</h1>
            <p class="error-message">A database error occurred while processing your request. This may be a temporary connection issue. Please try again shortly or contact your system administrator.</p>
            <%
                Throwable cause = (Throwable) request.getAttribute("javax.servlet.error.exception");
                if (cause instanceof java.sql.SQLException) {
                    java.sql.SQLException sqlEx = (java.sql.SQLException) cause;
            %>
            <p class="error-detail">
                <i class="fa-solid fa-circle-info me-1"></i>
                <strong>SQL State:</strong> <%= sqlEx.getSQLState() %> &nbsp;&bull;&nbsp;
                <strong>Error Code:</strong> <%= sqlEx.getErrorCode() %>
            </p>
            <% } %>
            <a class="error-btn" href="${pageContext.request.contextPath}/index.jsp">
                <i class="fa-solid fa-arrow-left me-2"></i>Return to login
            </a>
        </div>
    </main>

    <footer>
    </footer>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
