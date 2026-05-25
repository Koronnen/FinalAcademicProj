<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>403 – Access Denied</title>
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
                <i class="fa-solid fa-lock" style="font-size:5rem; color:#f59e0b;"></i>
            </div>
            <h1 class="error-title">Access denied</h1>
            <p class="error-message">You do not have the required permissions to access this resource. If you believe this is an error, please contact your system administrator or log in with an authorized account.</p>

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
