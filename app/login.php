<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Login</title>
    <link rel="icon" href="favicon.ico" type="image/x-icon">
    <!-- Incluir Bootstrap desde un CDN -->
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <!-- Incluir el CSS separado -->
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div class="container d-flex flex-column justify-content-center align-items-center">
        <div class="login-wrapper">
            <img src="/img/adminobr_logo.png" alt="Logo" class="logo">
            <!-- Texto adicional con estilo -->
            <div class="text-center mb-4">
                <span class="brand-text brand-text-color">admin<span class="brand-text brand-text-color-highlight">O</span>br</span>
            </div>
            <form id="loginForm">
                <div class="form-group">
                    <label for="usuario">Usuario</label>
                    <input type="text" class="form-control" id="usuario" name="usuario" required="">
                </div>
                <div class="form-group mb-0">
                    <label for="password">Contraseña</label>
                    <input type="password" class="form-control" id="password" name="password" required="">
                </div>
                <a href="#" class="forgot-password">¿Olvidó su contraseña?</a>
                <button type="submit" class="btn btn-primary btn-block">Iniciar Sesión</button>
            </form>
        </div>
    </div>

    <!-- JavaScript para el envío del formulario -->
    <script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@4.5.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        document.getElementById('loginForm').addEventListener('submit', function(event) {
            event.preventDefault();

            let usuario = document.getElementById('usuario').value;
            let password = document.getElementById('password').value;

            fetch('/auth/login.php', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ usuario, password })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    window.location.href = 'index.php';
                } else {
                    alert(data.message);
                }
            })
            .catch(error => console.error('Error:', error));
        });
    </script>
</body>
</html>