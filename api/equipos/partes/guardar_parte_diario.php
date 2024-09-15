<?php
// Verificar que los datos necesarios están presentes en la solicitud POST
if (!isset($_POST['empresaDbName']) ) {
    http_response_code(400);
    die(json_encode(['success' => false, 'message' => 'Datos de empresaDbName incompletos']));
}

// Obtener los valores desde el POST
$empresaDbName = $_POST['empresaDbName'];

// Incluir el archivo de configuración
include '../../db_config.php';
$dbname = $empresaDbName; // Sobreescribir $dbname con empresaDbName 

// Conectar a la base de datos usando el dbname obtenido
$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar la conexión
if ($conn->connect_error) {
    die("Error de conexión: " . $conn->connect_error);
}

// Obtener los datos del parte diario enviados desde la aplicación
$fecha = mysqli_real_escape_string($conn, $_POST["fecha"]);
$horas_inicio = mysqli_real_escape_string($conn, $_POST["horasInicio"]);
$horas_fin = mysqli_real_escape_string($conn, $_POST["horasFin"]);
$horas_trabajadas = mysqli_real_escape_string($conn, $_POST["horasTrabajadas"]);
$observaciones = mysqli_real_escape_string($conn, $_POST["observaciones"]);
$user_id = mysqli_real_escape_string($conn, $_POST["userCreated"]); // ID del usuario que realiza la acción
$equipo_id = mysqli_real_escape_string($conn, $_POST["equipoId"]);
$obra_id = mysqli_real_escape_string($conn, $_POST["obraId"]);
$estado_id = mysqli_real_escape_string($conn, $_POST["estadoId"]);

// Obtener la fecha y hora actuales
$current_date = date("Y-m-d H:i:s");

// Insertar un nuevo parte diario
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $sql = "INSERT INTO partes_diarios (fecha, equipo_id, horas_inicio, horas_fin, horas_trabajadas, observaciones, obra_id, date_created, date_updated, user_created, user_updated, estado_id) 
    VALUES ('$fecha', '$equipo_id', '$horas_inicio', '$horas_fin', '$horas_trabajadas', '$observaciones', '$obra_id', '$current_date', '$current_date', '$user_id', '$user_id', '$estado_id')";

    if ($conn->query($sql) === TRUE) {
        $newId = $conn->insert_id; // Obtiene el ID del nuevo registro
        $response = array("success" => true, "message" => "Parte diario guardado correctamente.", "id" => $newId);
    } else {
        $response = array("success" => false, "message" => "Error al guardar el parte diario: " . $conn->error);
    }
}

// Actualizar un parte diario existente
if ($_SERVER["REQUEST_METHOD"] == "PUT") {
    // Obtener el ID del parte diario a actualizar
    $id_parte_diario = mysqli_real_escape_string($conn, $_POST["id_parte_diario"]);

    // Actualizar el parte diario en la base de datos
    $sql = "UPDATE partes_diarios SET 
        fecha = '$fecha', 
        equipo_id = '$equipo_id', 
        horas_inicio = '$horas_inicio', 
        horas_fin = '$horas_fin', 
        horas_trabajadas = '$horas_trabajadas', 
        observaciones = '$observaciones', 
        obra_id = '$obra_id', 
        date_updated = '$current_date', 
        user_updated = '$user_id', 
        estado_id = '$estado_id' 
    WHERE id_parte_diario = '$id_parte_diario'";

    if ($conn->query($sql) === TRUE) {
        $response =array("success" => true, "message" => "Parte diario actualizado correctamente.");
    } else {
        $response = array("success" => false, "message" => "Error al actualizar el parte diario: " . $conn->error);
    }
}

if ($_SERVER["REQUEST_METHOD"] == "DELETE") {
    // Obtener el ID del parte diario a eliminar
    $id_parte_diario = mysqli_real_escape_string($conn, $_POST["id_parte_diario"]);

    // Eliminar el parte diario de la base de datos
    $sql = "DELETE FROM partes_diarios WHERE id_parte_diario = '$id_parte_diario'";

    if ($conn->query($sql) === TRUE) {
        $response = array("success" => true, "message" => "Parte diario eliminado correctamente.");
    } else {
        $response = array("success" => false, "message" => "Error al eliminar el parte diario: " . $conn->error);
    }
}

// Devolver la respuesta en formato JSON
header('Content-Type: application/json');
echo json_encode($response);

$conn->close();
?>