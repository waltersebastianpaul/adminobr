<?php
// Incluir el archivo de configuración
include '../../db_config.php';

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    die("Error de conexión: " . $conn->connect_error);
}

// Obtener el parámetro del equipo
$equipo = isset($_GET['equipo']) ? $conn->real_escape_string($_GET['equipo']) : '';

if (empty($equipo)) {
    die(json_encode(['error' => 'El parámetro equipo es requerido.']));
}

// Consulta SQL para obtener el último parte diario para el equipo dado, ordenado por la fecha más reciente
$sql = "SELECT
            pd.id_parte_diario,
            pd.fecha,
            pd.equipo_id,
            e.interno,
            pd.horas_inicio,
            pd.horas_fin,
            pd.horas_trabajadas,
            pd.observaciones,
            pd.obra_id,
            pd.user_created,
            pd.estado_id
        FROM
            partes_diarios pd
        JOIN
            equipos e ON pd.equipo_id = e.id_equipo
        WHERE
            e.interno = '$equipo'
        ORDER BY
            pd.fecha DESC, pd.id_parte_diario DESC
        LIMIT 1";

$result = $conn->query($sql);

$ultimoParte = null;

if ($result->num_rows > 0) {
    $ultimoParte = $result->fetch_assoc();
    // Formatear la fecha
    $ultimoParte['fecha'] = date("d-m-Y", strtotime($ultimoParte['fecha']));
}

// Enviar la respuesta JSON
header('Content-Type: application/json');
echo json_encode($ultimoParte);

$conn->close();
?>