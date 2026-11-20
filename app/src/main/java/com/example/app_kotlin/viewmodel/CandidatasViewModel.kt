package com.example.app_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_kotlin.data.remote.mensajeAmigable
import com.example.app_kotlin.data.repository.CandidataRepository
import com.example.app_kotlin.model.Candidata
import com.example.app_kotlin.model.EstadoCandidatura
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CandidatasUiState(
    val candidatas: List<Candidata> = emptyList(),
    val busquedaTexto: String = "",
    val filtroEstado: EstadoCandidatura? = null,
    val cargando: Boolean = false,
    val actualizando: Boolean = false,
    val errorRed: String? = null
)

class CandidatasViewModel(
    private val candidataRepository: CandidataRepository
) : ViewModel() {

    private val _busquedaTexto = MutableStateFlow("")
    private val _filtroEstado = MutableStateFlow<EstadoCandidatura?>(null)
    private val _actualizando = MutableStateFlow(false)
    private val _errorRed = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CandidatasUiState> = combine(
        candidataRepository.todasCandidatas,
        _busquedaTexto,
        _filtroEstado,
        _actualizando,
        _errorRed
    ) { lista, busqueda, estado, actualizando, error ->
        val filtradas = lista.filter { candidata ->
            val coincideTexto = busqueda.isBlank() ||
                    candidata.nombre.contains(busqueda, ignoreCase = true) ||
                    candidata.area.contains(busqueda, ignoreCase = true) ||
                    candidata.correo.contains(busqueda, ignoreCase = true)

            val coincideEstado = estado == null || candidata.estado == estado

            coincideTexto && coincideEstado
        }

        CandidatasUiState(
            candidatas = filtradas,
            busquedaTexto = busqueda,
            filtroEstado = estado,
            cargando = false,
            actualizando = actualizando,
            errorRed = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CandidatasUiState(cargando = true)
    )

    init {
        refrescar()
    }

    fun onBusquedaChange(nuevoTexto: String) {
        _busquedaTexto.value = nuevoTexto
    }

    fun onFiltroEstadoChange(nuevoEstado: EstadoCandidatura?) {
        _filtroEstado.value = nuevoEstado
    }

    /** READ remoto: se llama al abrir la pantalla y con el botón "Actualizar". */
    fun refrescar() {
        if (_actualizando.value) return
        viewModelScope.launch {
            _actualizando.value = true
            try {
                candidataRepository.refrescar()
                _errorRed.value = null
            } catch (e: Exception) {
                // Si el backend está apagado se sigue mostrando la última copia local
                _errorRed.value = e.mensajeAmigable()
            } finally {
                _actualizando.value = false
            }
        }
    }
}