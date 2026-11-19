package com.mx.mountain

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mx.mountain.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showStart()

        binding.buttonOptionOne.setOnClickListener {
            when (binding.buttonOptionOne.tag) {
                "rehydrate" -> showImprovement()
                "improved" -> showImproved()
                "infectious" -> showMedicationNo()
                "not_infectious" -> showMedicationCaution()
                "medication" -> showMedicationNo()
                "reset" -> showStart()
                else -> showHydration()
            }
        }

        binding.buttonOptionTwo.setOnClickListener {
            when (binding.buttonOptionTwo.tag) {
                "rehydrate", "improved", "alarm" -> showRedFlags()
                "infectious" -> showMedicationCaution()
                "not_infectious" -> showMedicationCaution()
                "reset" -> showStart()
                else -> showStart()
            }
        }

        binding.buttonReset.setOnClickListener { showStart() }
    }

    private fun showStart() {
        binding.textviewFirst.text = "INICIO: ¿Qué presenta la persona?"
        binding.decisionDetail.text = "Diarrea durante la excursión"
        binding.buttonOptionOne.text = "Iniciar hidratación oral"
        binding.buttonOptionOne.tag = "rehydrate"
        binding.buttonOptionTwo.text = "Presenta señales de alarma"
        binding.buttonOptionTwo.tag = "alarm"
    }

    private fun showHydration() {
        binding.textviewFirst.text = "Paso 1: Hidratación"
        binding.decisionDetail.text = "Da pequeños sorbos frecuentes de suero oral. Vigila si puede beber y si aparecen síntomas de deshidratación."
        binding.buttonOptionOne.text = "La persona puede beber"
        binding.buttonOptionOne.tag = "rehydrate"
        binding.buttonOptionTwo.text = "No puede beber o empeora"
        binding.buttonOptionTwo.tag = "alarm"
    }

    private fun showImprovement() {
        binding.textviewFirst.text = "Paso 2: ¿Mejora con hidratación?"
        binding.decisionDetail.text = "Observa la evolución después de los sorbos y de reforzar higiene."
        binding.buttonOptionOne.text = "Sí, mejora"
        binding.buttonOptionOne.tag = "improved"
        binding.buttonOptionTwo.text = "No mejora o empeora"
        binding.buttonOptionTwo.tag = "alarm"
    }

    private fun showImproved() {
        binding.textviewFirst.text = "Evolución favorable"
        binding.decisionDetail.text = "Continúa el suero oral según necesidad. Reforza higiene de manos, agua segura y evita alimentos no preparados. Reevalúa periódicamente."
        binding.buttonOptionOne.text = "Evaluar si sospecha infección"
        binding.buttonOptionOne.tag = "infectious"
        binding.buttonOptionTwo.text = "Aparecen señales de alarma"
        binding.buttonOptionTwo.tag = "alarm"
    }

    private fun showRedFlags() {
        binding.textviewFirst.text = "Buscar atención médica"
        binding.decisionDetail.text = "Detén la excursión y activa el plan de evacuación. Busca atención urgente ante deshidratación, sangre en heces, fiebre alta, confusión, incapacidad para beber, dolor intenso o embarazo."
        binding.buttonOptionOne.text = "Revisar medicación"
        binding.buttonOptionOne.tag = "medication"
        binding.buttonOptionTwo.text = "Volver al inicio"
        binding.buttonOptionTwo.tag = "reset"
    }

    private fun showMedicationNo() {
        binding.textviewFirst.text = "¿Hay sospecha de infección?"
        binding.decisionDetail.text = "Considera fiebre, sangre o pus en heces, inicio tras agua/alimentos dudosos o un brote entre varias personas."
        binding.buttonOptionOne.text = "Sí, sospecha infecciosa"
        binding.buttonOptionOne.tag = "infectious"
        binding.buttonOptionTwo.text = "No hay sospecha clara"
        binding.buttonOptionTwo.tag = "not_infectious"
    }

    private fun showMedicationCaution() {
        binding.textviewFirst.text = "Loperamida: no indicada aún"
        binding.decisionDetail.text = "Prioriza suero oral y consulta a un profesional antes de usar loperamida. No la uses si hay fiebre, sangre o pus, deshidratación grave o sospecha de infección."
        binding.buttonOptionOne.text = "Volver al inicio"
        binding.buttonOptionOne.tag = "reset"
        binding.buttonOptionTwo.text = "Ver señales de alarma"
        binding.buttonOptionTwo.tag = "alarm"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}