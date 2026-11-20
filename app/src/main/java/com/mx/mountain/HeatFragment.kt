package com.mx.mountain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mx.mountain.databinding.FragmentHeatBinding

class HeatFragment : Fragment() {
    private var _binding: FragmentHeatBinding? = null
    private val binding get() = _binding!!
    private var state = "START"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHeatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        showStart()

        binding.heatOptionYes.setOnClickListener {
            when (state) {
                "START" -> showTriadTemperature()
                "TEMPERATURE" -> showTriadNeurological()
                "NEUROLOGICAL" -> showTriadExposure()
                "EXPOSURE" -> showProbableHeatstroke()
                "COOLING" -> showConsciousQuestion()
                "CONSCIOUS" -> showCoolingMonitor()
                "HEATSTROKE" -> showCooling()
                "EVACUATION" -> showStart()
                else -> showStart()
            }
        }

        binding.heatOptionNo.setOnClickListener {
            when (state) {
                "START" -> showCooling()
                "TEMPERATURE", "NEUROLOGICAL", "EXPOSURE" -> showOtherCause()
                "COOLING" -> showEvacuation()
                "CONSCIOUS" -> showEvacuation()
                "MONITOR" -> showEvacuation()
                "HEATSTROKE" -> showEvacuation()
                "OTHER" -> showCooling()
                else -> showStart()
            }
        }

        binding.heatReset.setOnClickListener { showStart() }
    }

    private fun showStart() {
        state = "START"
        binding.heatTitle.text = "INICIO: Malestar por calor"
        binding.heatDetail.text = "Persona expuesta a calor intenso y/o esfuerzo físico presenta malestar. Valora la triada clínica."
        binding.heatOptionYes.text = "Evaluar sospecha clínica"
        binding.heatOptionNo.text = "Actuar de inmediato"
    }

    private fun showTriadTemperature() {
        state = "TEMPERATURE"
        binding.heatTitle.text = "Triada 1: temperatura"
        binding.heatDetail.text = "¿La temperatura es mayor de 40 °C o la piel está muy caliente?"
        binding.heatOptionYes.text = "Sí"
        binding.heatOptionNo.text = "No / no medible"
    }

    private fun showTriadNeurological() {
        state = "NEUROLOGICAL"
        binding.heatTitle.text = "Triada 2: compromiso neurológico"
        binding.heatDetail.text = "¿Hay mareo, confusión, desmayo o alteración de conciencia?"
        binding.heatOptionYes.text = "Sí"
        binding.heatOptionNo.text = "No"
    }

    private fun showTriadExposure() {
        state = "EXPOSURE"
        binding.heatTitle.text = "Triada 3: exposición"
        binding.heatDetail.text = "¿Hubo mucho calor ambiental o sobreesfuerzo físico reciente?"
        binding.heatOptionYes.text = "Sí: probable golpe de calor"
        binding.heatOptionNo.text = "No está claro"
    }

    private fun showProbableHeatstroke() {
        state = "HEATSTROKE"
        binding.heatTitle.text = "Probable golpe de calor"
        binding.heatDetail.text = "Se cumplen la temperatura muy alta o piel caliente, el compromiso neurológico y la exposición reciente a calor o sobreesfuerzo. Actúa inmediatamente."
        binding.heatOptionYes.text = "Iniciar enfriamiento"
        binding.heatOptionNo.text = "Hay desmayo o confusión"
    }

    private fun showCooling() {
        state = "COOLING"
        binding.heatTitle.text = "Actuación inmediata"
        binding.heatDetail.text = "Mueve a la persona a la sombra, alejada del sol. Enfría con agua y paños o ropa húmeda, ventilación o abanico y rociado de agua."
        binding.heatOptionYes.text = "Continuar evaluación"
        binding.heatOptionNo.text = "La persona está inconsciente"
    }

    private fun showOtherCause() {
        state = "OTHER"
        binding.heatTitle.text = "No se completa la triada"
        binding.heatDetail.text = "Considera otras causas y continúa el enfriamiento mientras evalúas la evolución. Vigila si aparecen temperatura alta o alteraciones neurológicas."
        binding.heatOptionYes.text = "Volver al inicio"
        binding.heatOptionNo.text = "Iniciar actuación inmediata"
    }

    private fun showConsciousQuestion() {
        state = "CONSCIOUS"
        binding.heatTitle.text = "¿Está consciente y puede beber?"
        binding.heatDetail.text = "Solo ofrece líquidos si mantiene conciencia y puede tragar sin dificultad."
        binding.heatOptionYes.text = "Sí: dar sorbos frecuentes"
        binding.heatOptionNo.text = "No: no dar líquidos por boca"
    }

    private fun showCoolingMonitor() {
        state = "MONITOR"
        binding.heatTitle.text = "Continuar manejo seguro"
        binding.heatDetail.text = "Continúa enfriamiento e hidratación en sorbos frecuentes si puede beber. Vigila consciencia y respiración de forma continua."
        binding.heatOptionYes.text = "Hay desmayo o confusión marcada"
        binding.heatOptionNo.text = "Sin alteraciones: continuar vigilancia"
    }

    private fun showEvacuation() {
        state = "EVACUATION"
        binding.heatTitle.text = "Evacuación inmediata"
        binding.heatDetail.text = "Existe desmayo, pérdida de conocimiento o alteración neurológica marcada. Traslada o inicia descenso/evacuación lo más pronto posible y activa ayuda médica."
        binding.heatOptionYes.text = "Volver al inicio"
        binding.heatOptionNo.text = "Continuar enfriando durante la evacuación"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
