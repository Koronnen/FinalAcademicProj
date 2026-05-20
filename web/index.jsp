<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Portal</title>
    <link rel="stylesheet" href="styles/indexStyles.css">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@600;700&family=Open+Sans:wght@400;500&display=swap" rel="stylesheet">
    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
    </head>
<body>
    <div class="login-container">
        <div class="login-card">
            <div class="login-header">
                <h2>Welcome Back</h2>
                <p>Please enter your details to sign in</p>
            </div>
            
            <% 
                String loginError = (String) session.getAttribute("loginError");
                if (loginError != null) { 
            %>
                <div class="error-message">
                    <%= loginError %>
                </div>
            <% 
                    session.removeAttribute("loginError"); 
                } 
            %>
            
            <form id="loginForm" onsubmit="handleLogin(event)" action="${pageContext.request.contextPath}/loginServlet" method="POST">
                <div class="input-group">
                    <input type="email" id="email" required placeholder=" " name="email">
                    <label for="email">Email Address</label>
                    <div class="input-line"></div>
                </div>

                <div class="input-group">
                    <input type="password" id="password" required placeholder=" " name="password">
                    <label for="password">Password</label>
                    <div class="input-line"></div>
                </div>

                <div class="captcha-container">
                    <div class="g-recaptcha" data-sitekey="6LfuAPIsAAAAAGghWubdgs_wrykIdva3AOUs9NnD"></div>
                    <br/>
                </div>

                <button type="submit" class="submit-btn">
                    <span class="btn-text">Sign In</span>
                    <div class="btn-loader"></div>
                </button>
            </form>

            <div class="login-footer">
                <p>Don't have an account? <a href="#">Sign up</a></p>
            </div>
        </div>
    </div>

    <script src="script.js"></script>
</body>
</html>