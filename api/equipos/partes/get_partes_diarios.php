<?php
// Incluir el archivo de configuración
include '../../db_config.php';

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    die("Error de conexión: " . $conn->connect_error);
}

// Obtener los parámetros de paginación
$page = isset($_GET['page']) ? intval($_GET['page']) : 1;
$pageSize = isset($_GET['pageSize']) ? intval($_GET['pageSize']) : 20;

// Calcular el offset
$offset = ($page - 1) * $pageSize;

// Obtener los parámetros de filtro
$equipo = isset($_GET['equipo']) ? $conn->real_escape_string($_GET['equipo']) : '';
$fechaInicio = isset($_GET['fechaInicio']) ? $conn->real_escape_string($_GET['fechaInicio']) : '';
$fechaFin = isset($_GET['fechaFin']) ? $conn->real_escape_string($_GET['fechaFin']) : '';

// Función para convertir fechas del formato dd/MM/yyyy al formato yyyy-MM-dd
function convertirFecha($fecha) {
    $partes = explode('/', $fecha);
    if (count($partes) == 3) {
        return $partes[2] . '-' . $partes[1] . '-' . $partes[0];
    }
    return $fecha;
}

// Convertir las fechas
if (!empty($fechaInicio)) {
    $fechaInicio = convertirFecha($fechaInicio);
}
if (!empty($fechaFin)) {
    $fechaFin = convertirFecha($fechaFin);
}

// Construir la cláusula WHERE
$whereClause = '1=1';
if (!empty($equipo)) {
    $whereClause .= " AND e.interno = '$equipo'";
}
if (!empty($fechaInicio) && !empty($fechaFin)) {
    $whereClause .= " AND pd.fecha BETWEEN '$fechaInicio' AND '$fechaFin'";
} elseif (!empty($fechaInicio)) {
    $whereClause .= " AND pd.fecha >= '$fechaInicio'";
} elseif (!empty($fechaFin)) {
    $whereClause .= " AND pd.fecha <= '$fechaFin'";
}

// Consulta SQL para obtener los registros de la página solicitada
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
        WHERE $whereClause
        ORDER BY pd.id_parte_diario DESC
        LIMIT $pageSize OFFSET $offset";

$result = $conn->query($sql);

// Crear un arreglo para almacenar los registros
$partesDiarios = array();

if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        // Formatear la fecha
        $row['fecha'] = date("d-m-Y", strtotime($row['fecha']));
        $partesDiarios[] = $row;
    }
}

// Enviar la respuesta JSON
header('Content-Type: application/json');
echo json_encode($partesDiarios);

// Cerrar la conexión
$conn->close();
?>