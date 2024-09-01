<?php
//Incluir el archivo de configuración
include '../db_config.php';

$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar la conexión
if ($conn->connect_error) {
    error_log("Error de conexión: " . $conn->connect_error); // Log del error de conexión
    die("Error de conexión: " . $conn->connect_error);
}

// Obtener la fecha y hora actuales
$current_date = date("Y-m-d H:i:s");

// Insertar un nuevo usuario
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // Obtener los datos del usuario del POST
    $legajo = mysqli_real_escape_string($conn, $_POST["legajo"]);
    $email = mysqli_real_escape_string($conn, $_POST["email"]);
    $dni = mysqli_real_escape_string($conn, $_POST["dni"]);
    $password = mysqli_real_escape_string($conn, $_POST["password"]);
    $nombre = mysqli_real_escape_string($conn, $_POST["nombre"]);
    $apellido = mysqli_real_escape_string($conn, $_POST["apellido"]);
    $telefono = mysqli_real_escape_string($conn, $_POST["telefono"]);
    $user_id = isset($_POST["userCreated"]) ? mysqli_real_escape_string($conn, $_POST["userCreated"]) : 1;
    $estado_id = mysqli_real_escape_string($conn, $_POST["estado_id"]);

    // Verificar si el email, DNI o legajo ya existen
    $checkSql = "SELECT * FROM usuarios WHERE email = '$email' OR dni = '$dni' OR legajo = '$legajo'";
    $checkResult = $conn->query($checkSql);

    if ($checkResult->num_rows > 0) {
        // Obtener el campo duplicado
        $row = $checkResult->fetch_assoc();
        if ($row['email'] == $email) {
            $duplicateField = "email";
        } elseif ($row['dni'] == $dni) {
            $duplicateField = "DNI";
        } elseif ($row['legajo'] == $legajo) {
            $duplicateField = "legajo";
        }

        $response = array("success" => false, "message" => "El $duplicateField ya está en uso.");
    } else {
        // Insertar un nuevo usuario
        $sql = "INSERT INTO usuarios (legajo, email, dni, password, date_created, date_updated, user_created, user_updated, estado_id, nombre, apellido, telefono) 
        VALUES('$legajo', '$email', '$dni', '$password', '$current_date', '$current_date', '$user_id', '$user_id', '$estado_id', '$nombre', '$apellido', '$telefono')";

        if ($conn->query($sql) === TRUE) {
            $newId = $conn->insert_id;
            $response = array("success" => true, "message" => "Usuario guardado correctamente.", "id" => $newId);
        } else {
            $response = array("success" => false, "message" => "Error al guardar el usuario: " . $conn->error);
        }
    }

    // Devolver la respuesta en formato JSON
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($response);
}

$conn->close();
?>
