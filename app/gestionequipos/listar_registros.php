<?php
include '../auth/auth_check.php';
?>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>DataTable con PHP y MySQL</title>
    <link rel="icon" href="../../favicon.ico" type="image/x-icon">

    <!-- Incluir Bootstrap y DataTables desde un CDN -->
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.6/css/dataTables.bootstrap4.min.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/buttons/2.4.1/css/buttons.dataTables.min.css">
</head>
<body>
    <div class="container mt-5">
        <h2 class="text-center">Lista de Partes Diarios</h2>
        <table id="partesDiariosTable" class="table table-striped table-bordered">
            <thead>
                <tr>
                    <th>Equipo</th>
                    <th>Fecha Carga</th>
                    <th>Hora Inicio</th>
                    <th>Hora Final</th>
                    <th>Total Horas</th>
                    <th>Observaciones</th>
                    <th>Fecha Creación</th>
                    <th>Legajo</th>
                </tr>
            </thead>
            <tbody>
                <?php
                // Incluir el archivo de configuración
                include 'db_config.php';

                $conn = new mysqli($servername, $username, $password, $dbname);

                if ($conn->connect_error) {
                    die("Error de conexión: " . $conn->connect_error);
                }

                // Consulta SQL
                $sql = "SELECT 
                            e.interno AS nombre_equipo, 
                            pd.fecha AS fecha_carga, 
                            pd.horas_inicio, 
                            pd.horas_fin, 
                            pd.horas_trabajadas, 
                            pd.observaciones, 
                            pd.date_created AS fecha_creacion, 
                            u.legajo 
                        FROM 
                            partes_diarios pd
                        JOIN 
                            equipos e ON pd.equipo_id = e.id_equipo
                        JOIN 
                            usuarios u ON pd.user_created = u.id_usuario";

                $result = $conn->query($sql);

                if ($result === false) {
                    die("Error en la consulta: " . $conn->error);
                }

                if ($result->num_rows > 0) {
                    while($row = $result->fetch_assoc()) {
                        echo "<tr>";
                        echo "<td>" . htmlspecialchars($row["nombre_equipo"]) . "</td>";
                        echo "<td>" . date("d-m-y", strtotime($row["fecha_carga"])) . "</td>";
                        echo "<td>" . htmlspecialchars($row["horas_inicio"]) . "</td>";
                        echo "<td>" . htmlspecialchars($row["horas_fin"]) . "</td>";
                        echo "<td>" . htmlspecialchars($row["horas_trabajadas"]) . "</td>";
                        echo "<td>" . htmlspecialchars($row["observaciones"]) . "</td>";
                        echo "<td>" . date("d-m-y", strtotime($row["fecha_creacion"])) . "</td>";
                        echo "<td>" . htmlspecialchars($row["legajo"]) . "</td>";

                    }
                } else {
                    echo "<tr><td colspan='9'>No se encontraron registros.</td></tr>";
                }

                $conn->close();
                ?>
            </tbody>
        </table>
    </div>

    <!-- JavaScript de DataTables y Bootstrap -->
    <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.6/js/dataTables.bootstrap4.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@4.5.2/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.datatables.net/buttons/2.4.1/js/dataTables.buttons.min.js"></script>
    <script src="https://cdn.datatables.net/buttons/2.4.1/js/buttons.html5.min.js"></script>
    <script src="https://cdn.datatables.net/buttons/2.4.1/js/buttons.print.min.js"></script>
    <!-- JSZip and pdfmake for PDF and Excel export -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jszip/3.1.3/jszip.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.1.36/pdfmake.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.1.36/vfs_fonts.js"></script>

    <script>
        $(document).ready(function() {
            var table = $('#partesDiariosTable').DataTable({
                dom: 'Bfrtip',
                buttons: [
                    'copy', 'excel', 'pdf', 'print'
                ]
            });
        });
    </script>
</body>
</html>
