<?php
session_start();
header('Content-Type: application/json; charset=utf-8');

// Incluir el archivo de configuración
include 'db_config.php';

$conn = new mysqli($servername, $username, $password, $dbname);
if ($conn->connect_error) {
    http_response_code(500);
    die(json_encode(['success' => false, 'message' => 'Error de conexión a la base de datos: ' . $conn->connect_error]));
}

// Obtiene los datos de la solicitud
$data = json_decode(file_get_contents('php://input'), true);
if (!$data || !isset($data['usuario']) || !isset($data['password'])) {
    http_response_code(400);
    die(json_encode(['success' => false, 'message' => 'Datos de solicitud incompletos']));
}

$usuario = $data['usuario'];
$password = $data['password'];

// Consulta preparada para validar las credenciales
$stmt = $conn->prepare("SELECT * FROM usuarios WHERE (legajo = ? OR email = ?)");
if ($stmt === false) {
    http_response_code(500);
    die(json_encode(['success' => false, 'message' => 'Error al preparar la consulta: ' . $conn->error]));
}
$stmt->bind_param("ss", $usuario, $usuario);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $row = $result->fetch_assoc();

    // Verificar contraseña con password_verify
    if (password_verify($password, $row['password'])) { 

    // Verificar contraseña directamente
    //if ($password == $row['password']) {
        $userId = $row['id_usuario'];

        // Obtiene los roles del usuario
        $roles = [];
        $stmtRoles = $conn->prepare("SELECT r.nombre FROM roles r JOIN usuarios_roles ur ON r.id_rol = ur.rol_id WHERE ur.usuario_id = ?");
        if ($stmtRoles === false) {
            http_response_code(500);
            die(json_encode(['success' => false, 'message' => 'Error al preparar la consulta de roles: ' . $conn->error]));
        }
        $stmtRoles->bind_param("i", $userId);
        $stmtRoles->execute();
        $resultRoles = $stmtRoles->get_result();
        while ($rowRole = $resultRoles->fetch_assoc()) {
            $roles[] = $rowRole['nombre'];
        }
        $stmtRoles->close();

        // Obtiene los permisos del usuario
        $permisos = [];
        $stmtPermisos = $conn->prepare("SELECT p.nombre FROM permisos p JOIN roles_permisos rp ON p.id_permiso = rp.permiso_id JOIN usuarios_roles ur ON rp.rol_id = ur.rol_id WHERE ur.usuario_id = ?");
        if ($stmtPermisos === false) {
            http_response_code(500);
            die(json_encode(['success' => false, 'message' => 'Error al preparar la consulta de permisos: ' . $conn->error]));
        }
        $stmtPermisos->bind_param("i", $userId);
        $stmtPermisos->execute();
        $resultPermisos = $stmtPermisos->get_result();
        while ($rowPermiso = $resultPermisos->fetch_assoc()) {
            $permisos[] = $rowPermiso['nombre'];
        }
        $stmtPermisos->close();

        // Iniciar sesión y establecer variables de sesión
        $_SESSION['loggedin'] = true;
        $_SESSION['userId'] = $userId;

        // Envía la respuesta exitosa
        echo json_encode(['success' => true, 'message' => 'Inicio de sesión exitoso', 'user' => [
            'id' => $userId,
            'nombre' => $row['nombre'],
            'apellido' => $row['apellido'], // Agregar apellido
            'email' => $row['email'],
            'rol' => $roles,
            'permisos' => $permisos
        ]]);
    } else {
        // Contraseña incorrecta
        http_response_code(401);
        echo json_encode(['success' => false, 'message' => 'Contraseña incorrecta']);
    }
} else {
    // Usuario no encontrado
    http_response_code(401);
    echo json_encode(['success' => false, 'message' => 'Usuario no encontrado']);
}

$stmt->close();
$conn->close();