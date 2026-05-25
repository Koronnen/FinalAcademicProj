<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign Up - Portal (Students)</title>
    
    <link rel="stylesheet" href="styles/indexStyles.css">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@600;700&family=Open+Sans:wght@400;500&display=swap" rel="stylesheet">
    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
</head>
<body>
    <div class="login-container">
        <div class="login-card">
            <div class="login-header">
                <h2>Welcome to Active Learning!</h2>
                <p>Start your journey here and enroll yourselves to our courses!</p>
            </div>            
            <%  String captchaError = (String) session.getAttribute("captchaError");
                if (captchaError != null) { %>
                <div class="error-message"> <%= captchaError %> </div>
            <%   session.removeAttribute("captchaError");   } %>
            
            <%  String noInput = (String) session.getAttribute("noInput");
                if (noInput != null) { %>
                <div class="error-message"> <%= noInput %> </div>
            <%   session.removeAttribute("noInput");   } %>
            
            <%  String incorrectPass = (String) session.getAttribute("incorrectPass");
                if (incorrectPass != null) { %>
                <div class="error-message"> <%= incorrectPass %> </div>
            <%   session.removeAttribute("incorrectPass");   } %>
            
            <%  String existError = (String) session.getAttribute("existError");
                if (existError != null) { %>
                <div class="error-message"> <%= existError %> </div>
            <%   session.removeAttribute("existError");   } %>
            
            <form id="signUpForm" action="${pageContext.request.contextPath}/SignUpServlet" method="POST">
                
                <div class="input-group">
                    <input type="email" id="signEmail" required placeholder=" " name="signEmail">
                    <label for="signEmail">Email Address</label>
                    <div class="input-line"></div>
                </div>

                <div class="input-group">
                    <input type="password" id="signPassword" required placeholder=" " name="signPassword">
                    <label for="signPassword">Password</label>
                    <div class="input-line"></div>
                </div>
                
                <div class="input-group">
                    <input type="password" id="confirmPass" required placeholder=" " name="confirmPass">
                    <label for="confirmPass">Confirm Password</label>
                    <div class="input-line"></div>
                </div>
                
                <div class="captcha-container">
                    <div class="g-recaptcha" data-sitekey="6LfuAPIsAAAAAGghWubdgs_wrykIdva3AOUs9NnD"></div>
                    <br/>
                </div>

                <button type="submit" class="submit-btn">
                    <span class="btn-text">Sign Up</span>
                    <div class="btn-loader"></div>
                </button>
            </form>
            <div class="login-footer">
                <p>Already have an account? <a href="${pageContext.request.contextPath}/index.jsp">Log in</a></p>
            </div>
        </div>
    </div>
</body>
</html>
