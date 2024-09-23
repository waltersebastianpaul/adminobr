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

$sql = "SELECT id_estado, nombre FROM estados";
$result = $conn->query($sql);

$estados = array();
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $estados[] = array(
            "id" => $row["id_estado"],
            "nombre" => $row["nombre"]
        ); 
    }
}

// Devolver la respuesta en formato JSON
header('Content-Type: application/json');
echo json_encode($estados);

$conn->close();
?>