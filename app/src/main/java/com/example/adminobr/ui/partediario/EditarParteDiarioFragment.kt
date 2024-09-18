//package com.example.adminobr.ui.partediario
//
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
//import com.example.adminobr.databinding.FragmentParteDiarioBinding
//import com.example.adminobr.viewmodel.ParteDiarioViewModel
//
//class EditarParteDiarioFragment : Fragment() {
//    // ...
//
//    private lateinit var binding: FragmentParteDiarioBinding
//    private val viewModel: ParteDiarioViewModel by viewModels()
//
//    override fun onViewCreated(...) {
//        // ...
//        val args = arguments?.let { EditarParteDiarioFragmentArgs.fromBundle(it) }
//        val parteDiario = args?.parteDiario
//
//        // Rellenar los campos editables con los datos del parteDiario
//        binding.fechaEditText.setText(parteDiario?.fecha)
//        // ... otros campos
//
//        binding.saveButton.setOnClickListener {
//            val updatedParteDiario = ListarPartesDiarios(
//                // Crear un nuevo objeto con los datos modificados
//                id_parte_diario = parteDiario?.id_parte_diario,
//                fecha = binding.fechaEditText.text.toString(),
//                interno = binding.internoEditText.text.toString(),
//                horas_inicio = binding.horasInicioEditText.text.toString(),
//                horas_fin = binding.horasFinEditText.text.toString(),
//                observaciones = binding.observacionesEditText.text.toString(),
//                obra = binding.obraEditText.text.toString(),
//                estado = binding.estadoEditText.text.toString(),
//                user_created = binding.userCreatedEditText.text.toString(),
//                user_updated = binding.userUpdatedEditText.text.toString(),
//
//                // ... otros campos
//            )
//
//            val empresaDbName = sessionManager.getEmpresaData()?.db_name
//            viewModel.updateParteDiario(updatedParteDiario, empresaDbName) { success ->
//                // Manejar el resultado de la actualización
//            }
//        }
//    }
//}