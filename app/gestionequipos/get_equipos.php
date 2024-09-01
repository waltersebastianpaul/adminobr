<?php
// Incluir el archivo de configuración
include 'db_config.php';

// Crear conexión
$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    die("Conexión fallida: " . $conn->connect_error);
}

$sql = "SELECT id_equipo, interno, descripcion FROM equipos";
$result = $conn->query($sql);

$equipos = array();
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $equipos[] = array(
            "id" => $row["id_equipo"],
            "interno" => $row["interno"],
            "descripcion" => $row["descripcion"]
        ); 
    }
}

// Devolver la respuesta en formato JSON
header('Content-Type: application/json');
echo json_encode($equipos);

$conn->close();
?>