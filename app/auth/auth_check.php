<?php
session_start();

// Verificar si la sesión está activa y el usuario está autenticado
if (!isset($_SESSION['loggedin']) || $_SESSION['loggedin'] !== true) {
    // Redirigir al usuario a la página de inicio de sesión
    header("Location: /login.php");
    exit;
}
?>