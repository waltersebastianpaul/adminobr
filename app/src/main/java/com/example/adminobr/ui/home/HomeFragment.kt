package com.example.adminobr.ui.home

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.adminobr.R
import com.example.adminobr.databinding.FragmentHomeBinding
import com.example.adminobr.viewmodel.HomeViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import com.example.adminobr.utils.SessionManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = downloadManager.getUriForDownloadedFile(downloadId)
                if (uri != null) {
                    installApk(uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
//        val textView: TextView = binding.textHome
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

        sessionManager = SessionManager(requireContext())

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.parteDiarioCardView.setOnClickListener {
            findNavController().navigate(R.id.nav_partediario)
        }

        binding.parteSimpleCardView.setOnClickListener {
            findNavController().navigate(R.id.nav_partesimple)
        }

        binding.listarPartesCardView.setOnClickListener {
            findNavController().navigate(R.id.nav_listarpartes)
        }

        // Obtener roles del usuario
        val userRoles = sessionManager.getUserRol()

        // Constrol de visivilidad segun roles
        if (userRoles?.contains("supervisor") == true || userRoles?.contains("administrador") == true) {
            binding.parteSimpleCardView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        // Cambia el color de la "O" de AdminOBR a otro color
        val title = SpannableString("AdminObr")
        val oColor =
            ContextCompat.getColor(this@HomeFragment.requireContext(), R.color.colorLogo) // Reemplaza con el color que quieras
        val oSpan = ForegroundColorSpan(oColor)
        title.setSpan(oSpan, 5, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Aplica el span a la "O"
        // Establecer el título en el ActionBar
        (activity as? AppCompatActivity)?.supportActionBar?.title = title


        // Configuración del FloatingActionButton regular
        val fab: FloatingActionButton = requireActivity().findViewById(R.id.fab)
        fab.visibility = View.VISIBLE
        fab.setImageResource(R.drawable.ic_add)
        fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        fab.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))

        fab.setOnClickListener {
            findNavController().navigate(R.id.nav_partediario)
        }

        // Registrar el BroadcastReceiver
        ContextCompat.registerReceiver( // Usar ContextCompat.registerReceiver()
            requireActivity(),
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            RECEIVER_NOT_EXPORTED // Agregar el flag
        )

//        // Configuración del ExtendedFloatingActionButton
//        val extendedFab: ExtendedFloatingActionButton? = requireActivity().findViewById(R.id.extended_fab)
//        extendedFab?.let { fab ->
//            fab.setIconResource(R.drawable.ic_add)
//            fab.text = getString(R.string.etiqueta_fab)
//            fab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
//            fab.backgroundTintList = ColorStateList.valueOf(
//                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
//            )
//            fab.iconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
//
//            fab.setOnClickListener {
//                try {
//                    findNavController().navigate(R.id.nav_partediario)
//                } catch (e: Exception) {
//                    Snackbar.make(requireView(), "Error al navegar: ${e.message}", Snackbar.LENGTH_LONG).show()
//                }
//            }
//        }

    }

    private fun installApk(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(), 0, // requestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Flags para Android 12+
        )

        try {
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            // Manejar la excepción si el PendingIntent se cancela
            Toast.makeText(requireContext(), "Error al instalar la actualización", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(downloadCompleteReceiver) // Usar requireActivity()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
