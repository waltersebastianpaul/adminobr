<?php
// Incluir el archivo de configuración
include '../db_config.php';

// Crear conexión
$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    die("Conexión fallida: " . $conn->connect_error);
}

$sql = "SELECT id_rol, nombre, descripcion FROM roles";
$result = $conn->query($sql);

$obras = array();
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $obras[] = array(
            "id" => $row["id_rol"],
            "nombre" => $row["nombre"],
            "descripcion" => $row["descripcion"]
        ); 
    }
}

// Devolver la respuesta en formato JSON
header('Content-Type: application/json');
echo json_encode($obras);

$conn->close();
?>