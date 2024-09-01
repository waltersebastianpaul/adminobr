<?php
// Incluir el archivo de configuración
include '../db_config.php';

// Crear conexión
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