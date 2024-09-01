<?php
// Incluir el archivo de configuración
include '../db_config.php';
$dbname = "c2650924_db"; // Sobreescribir $dbname de db_config.php

// Crear conexión
$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    die("Conexión fallida: " . $conn->connect_error);
}

$sql = "SELECT id_empresa, nombre, db_name, db_username, db_password, estado_id FROM empresas";
$result = $conn->query($sql);

$empresas = array();
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $empresas[] = array(
            "id" => $row["id_empresa"],
            "nombre" => strtoupper($row["nombre"]), // Se convierte a MAYUSCULA
            "db_name" => $row["db_name"],
            "db_username" => $row["db_username"],
            "db_password" => $row["db_password"],
            "estado" => $row["estado_id"],
        ); 
    }
}

// Devolver la respuesta en formato JSON
header('Content-Type: application/json');
echo json_encode($empresas);

$conn->close();
?>