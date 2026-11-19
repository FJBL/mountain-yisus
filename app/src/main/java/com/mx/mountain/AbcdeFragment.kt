package com.mx.mountain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mx.mountain.databinding.FragmentAbcdeBinding

class AbcdeFragment : Fragment() {
    private var _binding: FragmentAbcdeBinding? = null
    private val binding get() = _binding!!
    private var state = "START"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAbcdeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        showStart()
        binding.abcdeOptionYes.setOnClickListener { when (state) {
            "START", "A" -> showB()
            "B" -> showC()
            "C" -> showNeurological()
            "D" -> showPriority("D", "Alteración neurológica", "Situación de riesgo. Valora Glasgow, pupilas y glucosa si está disponible; vigila cambios y evacúa según el contexto.")
            "E" -> showPriority("E", "Lesión oculta o temperatura alterada", "Expón solo lo necesario, protege privacidad y entorno, evalúa temperatura y lesiones, y actúa según el hallazgo.")
            else -> showStart()
        } }
        binding.abcdeOptionNo.setOnClickListener { when (state) {
            "START", "A" -> showPriority("A", "Vía aérea comprometida", "Prioridad inmediata: resolver la obstrucción y asegurar entrada de aire. No continúes hasta estabilizar la vía aérea.")
            "B" -> showPriority("B", "No respira adecuadamente", "Prioridad respiratoria: controla respiración y oxigenación, activa ayuda y prepárate para una evacuación urgente.")
            "C" -> showPriority("C", "Circulación comprometida", "Prioridad circulatoria: controla pulso, sangrado y perfusión. Activa ayuda y evita dejar sola a la persona.")
            "D" -> showEStage()
            "E" -> showPriority("E", "Lesión oculta o temperatura alterada", "Expón solo lo necesario, protege privacidad y entorno, evalúa temperatura y lesiones, y actúa según el hallazgo.")
            else -> showStart()
        } }
        binding.abcdeReset.setOnClickListener { showStart() }
    }

    private fun showStart() {
        state = "START"
        binding.abcdeTitle.text = "INICIO: Posible urgencia"
        binding.abcdeDetail.text = "Realiza ABCDE en este orden y dedica como máximo aproximadamente un minuto a la primera valoración."
        binding.abcdeOptionYes.text = "Comenzar con A: vía aérea"
        binding.abcdeOptionNo.text = "La vía aérea no está permeable"
    }

    private fun showB() {
        state = "A"
        binding.abcdeTitle.text = "A: ¿Vía aérea permeable?"
        binding.abcdeDetail.text = "Valora si el aire pasa libremente y si existe obstrucción visible, incapacidad para hablar o ruidos."
        binding.abcdeOptionYes.text = "Sí, continúa con B"
        binding.abcdeOptionNo.text = "No, vía aérea comprometida"
    }

    private fun showC() {
        state = "B"
        binding.abcdeTitle.text = "B: ¿Respira adecuadamente?"
        binding.abcdeDetail.text = "Observa frecuencia, profundidad, esfuerzo respiratorio y sonidos anormales."
        binding.abcdeOptionYes.text = "Sí, continúa con C"
        binding.abcdeOptionNo.text = "No o respiración anormal"
    }

    private fun showD() {
        state = "C"
        binding.abcdeTitle.text = "C: ¿Circulación adecuada?"
        binding.abcdeDetail.text = "Revisa pulso, frecuencia aparente, coloración, sangrado y piel fría o húmeda."
        binding.abcdeOptionYes.text = "Sí, continúa con D"
        binding.abcdeOptionNo.text = "No, circulación comprometida"
    }

    private fun showE() {
        state = "D"
        binding.abcdeTitle.text = "D: ¿Alteración neurológica?"
        binding.abcdeDetail.text = "Valora nivel de conciencia, Glasgow si es posible, pupilas, glucosa si hay medios y signos neurológicos nuevos."
        binding.abcdeOptionYes.text = "Sí, existe alteración"
        binding.abcdeOptionNo.text = "No hay alteración: continúa con E"
    }

    private fun showEStage() {
        state = "E"
        binding.abcdeTitle.text = "E: ¿Lesiones ocultas o temperatura alterada?"
        binding.abcdeDetail.text = "Expón solo lo necesario, revisa cabeza a pies y temperatura corporal."
        binding.abcdeOptionYes.text = "Sí, existe hallazgo"
        binding.abcdeOptionNo.text = "No, continuar monitoreo"
    }

    private fun showNeurological() {
        state = "D"
        binding.abcdeTitle.text = "D: ¿Alteración neurológica?"
        binding.abcdeDetail.text = "Valora nivel de conciencia, Glasgow si es posible, pupilas, glucosa si hay medios y signos neurológicos nuevos."
        binding.abcdeOptionYes.text = "Sí, existe alteración"
        binding.abcdeOptionNo.text = "No hay alteración: continúa con E"
    }

    private fun showPriority(letter: String, title: String, detail: String) {
        state = "PRIORITY"
        binding.abcdeTitle.text = "$letter: $title"
        binding.abcdeDetail.text = detail
        binding.abcdeOptionYes.text = "Activar ayuda y reevaluar"
        binding.abcdeOptionNo.text = "Reiniciar evaluación ABCDE"
    }

    private fun showComplete() {
        state = "COMPLETE"
        binding.abcdeTitle.text = "E: Sin hallazgos iniciales"
        binding.abcdeDetail.text = "Continuar monitoreo: conserva calor o frescor según necesidad, registra cambios y mantiene preparada la evacuación."
        binding.abcdeOptionYes.text = "Reiniciar evaluación"
        binding.abcdeOptionNo.text = "Revisar lesiones ocultas"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
