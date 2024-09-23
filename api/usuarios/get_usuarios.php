<?php
// Obtener los datos de la solicitud (empresaDbName debe ser enviado como un parámetro POST)
$data = json_decode(file_get_contents('php://input'), true);

if (!$data || !isset($data['empresaDbName'])) {
    http_response_code(400);
    die(json_encode(['success' => false, 'message' => 'Datos de solicitud incompletos']));
}

$empresaDbName = $data['empresaDbName'] ?? null;

// Incluir el archivo de configuración
include '../db_config.php';
$dbname = $empresaDbName; // Sobreescribir $dbname con empresaDbName 

// Conectar a la base de datos usando el dbname obtenido
$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    die("Conexión fallida: " . $conn->connect_error);
}

$sql = "SELECT id_usuario, legajo, email, dni, date_created, date_updated, estado_id, nombre, apellido, telefono FROM usuarios";
$result = $conn->query($sql);

$obras = array();
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $obras[] = array(
            "id" => $row["id_usuario"],
            "legajo" => $row["legajo"],
            "email" => $row["email"],
            "dni" => $row["dni"],
            "date_created" => $row["date_created"],
            "date_updated" => $row["date_updated"],
            "estado_id" => $row["estado_id"],
            "nombre" => $row["nombre"],
            "apellido" => $row["apellido"],
            "telefono" => $row["telefono"]
        ); 
    }
}

// Devolver la respuesta en formato JSON
header('Content-Type: application/json');
echo json_encode($obras);

$conn->close();
?>
