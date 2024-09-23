<?php

 // Incluir el archivo de configuración
 include '../../db_config.php';

 $response = array("success" => false, "message" => "Método no soportado.");
 $conn = null; // Inicializar $conn como null

 if ($_SERVER["REQUEST_METHOD"] == "POST" || $_SERVER["REQUEST_METHOD"] == "PUT") {
    // Verificar que los datos necesarios están presentes en la solicitud POST
    if (!isset($_POST['empresaDbName'])) {
        http_response_code(400);
        die(json_encode(['success' => false, 'message' => 'Datos de empresaDbName incompletos']));
    }

    // Obtener los valores desde el POST
    $empresaDbName = $_POST['empresaDbName'];
    $dbname = $empresaDbName; // Sobreescribir $dbname con empresaDbName 

    // Conectar a la base de datos usando el dbname obtenido
    $conn = new mysqli($servername, $username, $password, $dbname);

    // Verificar la conexión
    if ($conn->connect_error) {
        die("Error de conexión: " . $conn->connect_error);
    }

    // Obtener los datos del parte diario enviados desde la aplicación
    // id_parte_diario solo será relevante si es un método PUT
    $id_parte_diario = ($_SERVER["REQUEST_METHOD"] == "PUT") ? mysqli_real_escape_string($conn, $_POST["id_parte_diario"] ?? null) : null;

    $fecha = mysqli_real_escape_string($conn, $_POST["fecha"]);
    $horas_inicio = mysqli_real_escape_string($conn, $_POST["horasInicio"]);
    $horas_fin = mysqli_real_escape_string($conn, $_POST["horasFin"]);
    $horas_trabajadas = mysqli_real_escape_string($conn, $_POST["horasTrabajadas"]);
    $observaciones = mysqli_real_escape_string($conn, $_POST["observaciones"]);
    $user_id = mysqli_real_escape_string($conn, $_POST["userCreated"]); // ID del usuario que realiza la acción
    $equipo_id = mysqli_real_escape_string($conn, $_POST["equipoId"]);
    $obra_id = mysqli_real_escape_string($conn, $_POST["obraId"]);
    $estado_id = mysqli_real_escape_string($conn, $_POST["estadoId"]);

    // Función para validar y convertir fecha
    function convertirFecha($fecha) {
        $date = DateTime::createFromFormat('d/m/Y', $fecha);
        if ($date && $date->format('d/m/Y') === $fecha) {
            return $date->format('Y-m-d'); // Convertir a formato Y-m-d para MySQL
        }
        return false; // Retorna false si el formato no es válido
    }

    $fechaConvertida = convertirFecha($fecha);
    if (!$fechaConvertida) {
        die(json_encode(['success' => false, 'message' => 'Formato de fecha inválido. Use dd/mm/yyyy']));
    }

    // Obtener la fecha y hora actuales
    $current_date = date("Y-m-d H:i:s");

    if ($_SERVER["REQUEST_METHOD"] == "POST") {
        $sql = "INSERT INTO partes_diarios (fecha, equipo_id, horas_inicio, horas_fin, horas_trabajadas, observaciones, obra_id, date_created, date_updated, user_created, user_updated, estado_id) 
        VALUES ('$fechaConvertida', '$equipo_id', '$horas_inicio', '$horas_fin', '$horas_trabajadas', '$observaciones', '$obra_id', '$current_date', '$current_date', '$user_id', '$user_id', '$estado_id')";

        if ($conn->query($sql) === TRUE) {
            $newId = $conn->insert_id; // Obtiene el ID del nuevo registro
            $response = array("success" => true, "message" => "Parte diario guardado correctamente.", "id" => $newId);
        } else {
            $response = array("success" => false, "message" => "Error al guardar el parte diario: " . $conn->error);
        }
    } elseif ($_SERVER["REQUEST_METHOD"] == "PUT") {
        
        // Actualizar el parte diario en la base de datos
        $sql = "UPDATE partes_diarios SET 
            fecha = '$fechaConvertida', 
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

} elseif ($_SERVER["REQUEST_METHOD"] == "DELETE") {
    // Obtener empresaDbName e id_parte_diario de $_GET
    $empresaDbName = isset($_GET['empresaDbName']) ? $_GET['empresaDbName'] : null;
    $id_parte_diario = isset($_GET["id_parte_diario"]) ? $_GET["id_parte_diario"] : null;

    if (is_null($empresaDbName) || is_null($id_parte_diario)) {
        $response = array("success" => false, "message" => "ID de parte diario o empresaDbName no especificado.");
        http_response_code(400); // Bad Request
    } else {
        $dbname = $empresaDbName;
        // Conectar a la base de datos usando el dbname obtenido
        $conn = new mysqli($servername, $username, $password, $dbname);

        // Verificar la conexión
        if ($conn->connect_error) {
            die("Error de conexión: " . $conn->connect_error);
        }
        
        // Escapar los valores después de conectar a la base de datos
        $id_parte_diario = mysqli_real_escape_string($conn, $id_parte_diario);
        $empresaDbName = mysqli_real_escape_string($conn, $empresaDbName);

        // Eliminar el parte diario de la base de datos
        $sql = "DELETE FROM partes_diarios WHERE id_parte_diario = '$id_parte_diario'";

        if ($conn->query($sql) === TRUE) {
            $response = array("success" => true, "message" => "Parte diario eliminado correctamente.");
        } else {
            $response = array("success" => false, "message" => "Error al eliminar el parte diario: " . $conn->error);
            http_response_code(500); // Internal Server Error
        }
    }
}


// Devolver la respuesta en formato JSON
header('Content-Type: application/json');
echo json_encode($response);

if (!is_null($conn)) { // Verificar si $conn es nulo antes de cerrarlo
    $conn->close();
}
?>