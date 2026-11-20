package com.example.app_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_kotlin.data.remote.mensajeAmigable
import com.example.app_kotlin.data.repository.AuthRepository
import com.example.app_kotlin.data.repository.HistorialRepository
import com.example.app_kotlin.model.Acciones
import com.example.app_kotlin.model.RegistroHistorial
import com.example.app_kotlin.model.Sesion
import com.example.app_kotlin.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SesionUiState(
    val usuario: CampoState = CampoState(),
    val password: CampoState = CampoState(),
    val cargando: Boolean = false,
    val estaAutenticado: Boolean = false,
    val mensajeErrorGeneral: String? = null
)

class SesionViewModel(
    private val authRepository: AuthRepository,
    private val historialRepository: HistorialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SesionUiState())
    val uiState: StateFlow<SesionUiState> = _uiState.asStateFlow()

    fun onUsuarioChange(v: String) {
        _uiState.update {
            it.copy(usuario = it.usuario.con(v, validarUsuario(v)), mensajeErrorGeneral = null)
        }
    }

    fun onPasswordChange(v: String) {
        _uiState.update {
            it.copy(password = it.password.con(v, validarPassword(v)), mensajeErrorGeneral = null)
        }
    }

    /** Valida los campos antes de pedir la verificación del dispositivo y llamar al servidor. */
    fun validar(): Boolean {
        val s = _uiState.value
        val u = validarUsuario(s.usuario.valor)
        val p = validarPassword(s.password.valor)
        _uiState.update {
            it.copy(
                usuario = it.usuario.con(s.usuario.valor, u),
                password = it.password.con(s.password.valor, p)
            )
        }
        return u.esValido && p.esValido
    }

    /** Llama al backend. Si responde 200 crea la sesión y avisa a la pantalla. */
    fun iniciarSesion(metodo: String, onExito: () -> Unit) {
        val s = _uiState.value
        if (s.cargando) return
        _uiState.update { it.copy(cargando = true, mensajeErrorGeneral = null) }

        viewModelScope.launch {
            val usuario = try {
                authRepository.login(s.usuario.valor, s.password.valor)
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, mensajeErrorGeneral = e.mensajeAmigable()) }
                return@launch
            }

            // El rol lo entrega el servidor, ya no se elige en pantalla
            Sesion.iniciar(usuario)
            _uiState.value = SesionUiState(estaAutenticado = true)

            historialRepository.registrarAccion(
                RegistroHistorial(
                    usuario = usuario.nombre,
                    accion = Acciones.INICIO_SESION,
                    detalle = "Sesión iniciada con rol de ${usuario.rol.titulo} ($metodo)"
                )
            )
            onExito()
        }
    }

    fun cerrarSesion(onSesionCerrada: () -> Unit) {
        val usuarioAnterior = Sesion.usuarioActivo
        viewModelScope.launch {
            if (usuarioAnterior.isNotBlank()) {
                historialRepository.registrarAccion(
                    RegistroHistorial(
                        usuario = usuarioAnterior,
                        accion = Acciones.CIERRE_SESION,
                        detalle = "Sesión cerrada correctamente"
                    )
                )
            }
            Sesion.cerrar()
            _uiState.value = SesionUiState()
            onSesionCerrada()
        }
    }

    companion object {
        fun validarUsuario(v: String): ValidationResult =
            if (v.isBlank()) ValidationResult.Error("Ingresa tu usuario") else ValidationResult.Valido

        fun validarPassword(v: String): ValidationResult = when {
            v.isBlank() -> ValidationResult.Error("Ingresa tu contraseña")
            v.length < 6 -> ValidationResult.Error("Mínimo 6 caracteres")
            else -> ValidationResult.Valido
        }
    }
}