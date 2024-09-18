<?php
// Obtener el contenido de la solicitud POST en caso de que sea JSON
$input = file_get_contents('php://input');
$data = json_decode($input, true);

// Verificar que los datos necesarios están presentes en el JSON
if (!$data || !isset($data['empresaDbName'])) {
    http_response_code(400);
    die(json_encode(['success' => false, 'message' => 'Datos de solicitud incompletos']));
}

$empresaDbName = $data['empresaDbName'];
$equipo = isset($data['equipo']) ? $data['equipo'] : null; // Obtener equipo si existe
$limit = isset($data['limit']) ? $data['limit'] : 1;
$userCreated = isset($data['user_created']) ? $data['user_created'] : null;


// Incluir el archivo de configuración
include '../../db_config.php';
$dbname = $empresaDbName; // Sobreescribir $dbname con empresaDbName 

// Conectar a la base de datos usando el dbname obtenido
$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar la conexión
if ($conn->connect_error) {
    die("Error de conexión: " . $conn->connect_error);
}

if (empty($equipo)) {
    //die(json_encode(['error' => 'El parámetro equipo es requerido.']));
}

// Consulta SQL base
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
            1=1"; // Condición siempre verdadera para facilitar la adición de condiciones

// Agregar condición para user_created si se proporciona
if ($userCreated !== null) {
    $sql .= " AND (pd.user_created = '$userCreated' OR pd.user_updated = '$userCreated')";
}

// Agregar condición para equipo si se proporciona
if (!empty($equipo)) {
    $sql .= " AND e.interno = '$equipo'";
}

// Ordenar y limitar los resultados
$sql .= " ORDER BY pd.fecha DESC, pd.id_parte_diario DESC LIMIT $limit";

$result = $conn->query($sql);

$partesDiarios = array(); // Array para almacenar los partes diarios

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

$conn->close();
?>