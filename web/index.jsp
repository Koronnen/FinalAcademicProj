<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Portal</title>
    <link rel="stylesheet" href="style.css">
    </head>
<body>
    <div class="login-container">
        <div class="login-card">
            <div class="login-header">
                <h2>Welcome Back</h2>
                <p>Please enter your details to sign in</p>
            </div>
            
            <form id="loginForm" onsubmit="handleLogin(event)">
                <div class="input-group">
                    <input type="email" id="email" required placeholder=" ">
                    <label for="email">Email Address</label>
                    <div class="input-line"></div>
                </div>

                <div class="input-group">
                    <input type="password" id="password" required placeholder=" ">
                    <label for="password">Password</label>
                    <div class="input-line"></div>
                </div>

                <div class="captcha-container">
                    <div class="g-recaptcha" data-sitekey="6Leisq4sAAAAAE0CRnKzI-jvprNJ8KfV2J5scKqB"></div>
                    <br/>
                </div>

                <div class="form-actions">
                    <label class="remember-me">
                        <input type="checkbox">
                        <span class="checkmark"></span>
                        Remember me
                    </label>
                    <a href="#" class="forgot-password">Forgot password?</a>
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