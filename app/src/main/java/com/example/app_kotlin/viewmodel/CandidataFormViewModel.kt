package com.example.app_kotlin.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_kotlin.data.remote.mensajeAmigable
import com.example.app_kotlin.data.repository.CandidataRepository
import com.example.app_kotlin.data.repository.RandomUserRepository
import com.example.app_kotlin.model.Candidata
import com.example.app_kotlin.model.EstadoCandidatura
import com.example.app_kotlin.model.Sesion
import com.example.app_kotlin.nativo.ArchivoHelper
import com.example.app_kotlin.nativo.InfoArchivo
import com.example.app_kotlin.validation.CandidataValidator
import com.example.app_kotlin.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class CandidataFormUiState(
    val nombre: CampoState = CampoState(),
    val correo: CampoState = CampoState(),
    val telefono: CampoState = CampoState(),
    val area: CampoState = CampoState(),
    val annosExp: CampoState = CampoState(),
    val resumenExp: CampoState = CampoState(),
    val linkedinUrl: CampoState = CampoState(),
    val infoArchivoPdf: InfoArchivo? = null,
    /** Nombre del CV que ya tiene la candidata (solo en modo edición). */
    val cvNombreActual: String? = null,
    val errorCvPdf: String? = null,
    val esEdicion: Boolean = false,
    val guardando: Boolean = false,
    val exitoGuardado: Boolean = false,
    val activadoSacudida: Boolean = false,
    val errorGeneral: String? = null
)

class CandidataFormViewModel(
    /** null = registrar una candidata nueva; con valor = editar esa candidata. */
    private val candidataId: Long?,
    private val candidataRepository: CandidataRepository,
    private val randomUserRepository: RandomUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CandidataFormUiState(esEdicion = candidataId != null))
    val uiState: StateFlow<CandidataFormUiState> = _uiState.asStateFlow()

    private var original: Candidata? = null

    init {
        candidataId?.let { cargarParaEdicion(it) }
    }

    private fun cargarParaEdicion(id: Long) {
        viewModelScope.launch {
            val c = candidataRepository.obtenerPorId(id)
            if (c == null) {
                _uiState.update { it.copy(errorGeneral = "La candidatura ya no existe") }
                return@launch
            }
            original = c
            _uiState.update {
                it.copy(
                    nombre = it.nombre.con(c.nombre, CandidataValidator.validarNombre(c.nombre)),
                    correo = it.correo.con(c.correo, CandidataValidator.validarCorreo(c.correo)),
                    telefono = it.telefono.con(c.telefono, CandidataValidator.validarTelefono(c.telefono)),
                    area = it.area.con(c.area, CandidataValidator.validarArea(c.area)),
                    annosExp = it.annosExp.con(c.annosExp.toString(), CandidataValidator.validarAnnosExp(c.annosExp.toString())),
                    resumenExp = it.resumenExp.con(c.resumenExp, CandidataValidator.validarResumenExp(c.resumenExp)),
                    linkedinUrl = it.linkedinUrl.con(c.linkedinUrl, CandidataValidator.validarLinkedinUrl(c.linkedinUrl)),
                    cvNombreActual = c.cvNombre
                )
            }
        }
    }

    fun onNombreChange(v: String) {
        _uiState.update { it.copy(nombre = it.nombre.con(v, CandidataValidator.validarNombre(v))) }
    }

    fun onCorreoChange(v: String) {
        _uiState.update { it.copy(correo = it.correo.con(v, CandidataValidator.validarCorreo(v))) }
    }

    fun onTelefonoChange(v: String) {
        _uiState.update { it.copy(telefono = it.telefono.con(v, CandidataValidator.validarTelefono(v))) }
    }

    fun onAreaChange(v: String) {
        _uiState.update { it.copy(area = it.area.con(v, CandidataValidator.validarArea(v))) }
    }

    fun onAnnosExpChange(v: String) {
        _uiState.update { it.copy(annosExp = it.annosExp.con(v, CandidataValidator.validarAnnosExp(v))) }
    }

    fun onResumenExpChange(v: String) {
        _uiState.update { it.copy(resumenExp = it.resumenExp.con(v, CandidataValidator.validarResumenExp(v))) }
    }

    fun onLinkedinUrlChange(v: String) {
        _uiState.update { it.copy(linkedinUrl = it.linkedinUrl.con(v, CandidataValidator.validarLinkedinUrl(v))) }
    }

    /** API EXTERNA: completa el nombre con uno ficticio de randomuser.me. */
    fun generarNombreFicticio() {
        viewModelScope.launch {
            try {
                onNombreChange(randomUserRepository.nombreFicticio())
                _uiState.update { it.copy(errorGeneral = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorGeneral = "API externa no disponible: ${e.mensajeAmigable()}") }
            }
        }
    }

    fun onArchivoPdfSeleccionado(context: Context, uri: Uri?) {
        if (uri == null) return
        val info = ArchivoHelper.obtenerInfoArchivo(context, uri)
        if (info == null) {
            _uiState.update { it.copy(errorCvPdf = "No se pudo leer el archivo seleccionado") }
            return
        }
        if (!info.nombre.lowercase().endsWith(".pdf")) {
            _uiState.update { it.copy(errorCvPdf = "El archivo debe ser un documento PDF") }
            return
        }
        _uiState.update { it.copy(infoArchivoPdf = info, errorCvPdf = null) }
    }

    fun guardarCandidata(context: Context, onExito: () -> Unit) {
        val st = _uiState.value
        if (st.guardando) return

        // Validar todos los campos antes de guardar
        val resNombre = CandidataValidator.validarNombre(st.nombre.valor)
        val resCorreo = CandidataValidator.validarCorreo(st.correo.valor)
        val resTelefono = CandidataValidator.validarTelefono(st.telefono.valor)
        val resArea = CandidataValidator.validarArea(st.area.valor)
        val resAnnos = CandidataValidator.validarAnnosExp(st.annosExp.valor)
        val resResumen = CandidataValidator.validarResumenExp(st.resumenExp.valor)
        val resLinkedin = CandidataValidator.validarLinkedinUrl(st.linkedinUrl.valor)
        // Al editar, el CV es opcional: si no se elige uno nuevo se conserva el que ya tenía
        val resCv = if (candidataId != null) {
            ValidationResult.Valido
        } else {
            CandidataValidator.validarCv(st.infoArchivoPdf?.uri?.toString())
        }

        val tieneErrores = !resNombre.esValido || !resCorreo.esValido || !resTelefono.esValido ||
                !resArea.esValido || !resAnnos.esValido || !resResumen.esValido ||
                !resLinkedin.esValido || !resCv.esValido

        if (tieneErrores) {
            _uiState.update {
                it.copy(
                    nombre = it.nombre.con(it.nombre.valor, resNombre),
                    correo = it.correo.con(it.correo.valor, resCorreo),
                    telefono = it.telefono.con(it.telefono.valor, resTelefono),
                    area = it.area.con(it.area.valor, resArea),
                    annosExp = it.annosExp.con(it.annosExp.valor, resAnnos),
                    resumenExp = it.resumenExp.con(it.resumenExp.valor, resResumen),
                    linkedinUrl = it.linkedinUrl.con(it.linkedinUrl.valor, resLinkedin),
                    errorCvPdf = resCv.mensajeError,
                    activadoSacudida = !it.activadoSacudida
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errorGeneral = null) }

            try {
                // Copiar el PDF elegido al almacenamiento interno (el CV no viaja al servidor)
                var cvUri = original?.cvUri
                var cvNombre = original?.cvNombre
                if (st.infoArchivoPdf != null) {
                    val nombreArchivo = "${System.currentTimeMillis()}_${st.infoArchivoPdf.nombre}"
                    cvUri = ArchivoHelper.copiarArchivoAAlmacenamientoInterno(
                        context,
                        st.infoArchivoPdf.uri,
                        nombreArchivo
                    )
                    cvNombre = st.infoArchivoPdf.nombre
                }

                val datos = Candidata(
                    id = original?.id ?: 0,
                    nombre = st.nombre.valor.trim().replace(Regex("\\s+"), " "),
                    correo = st.correo.valor.trim().lowercase(),
                    telefono = CandidataValidator.normalizarTelefono(st.telefono.valor),
                    area = st.area.valor,
                    annosExp = st.annosExp.valor.trim().toInt(),
                    resumenExp = st.resumenExp.valor.trim(),
                    linkedinUrl = st.linkedinUrl.valor.trim(),
                    cvUri = cvUri,
                    cvNombre = cvNombre,
                    estado = original?.estado ?: EstadoCandidatura.POSTULADA,
                    fechaRegistro = original?.fechaRegistro ?: System.currentTimeMillis(),
                    registrador = original?.registrador
                        ?: Sesion.usuarioActivo.ifBlank { "Evaluadora" }
                )

                if (original == null) {
                    candidataRepository.registrarCandidata(datos)
                } else {
                    candidataRepository.actualizarCandidata(datos, Sesion.usuarioActivo)
                }

                _uiState.update { it.copy(guardando = false, exitoGuardado = true) }
                onExito()
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    // El servidor valida el correo repetido y responde 409
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            correo = it.correo.copy(error = "Ya existe una candidatura con este correo"),
                            activadoSacudida = !it.activadoSacudida
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            errorGeneral = e.mensajeAmigable(),
                            activadoSacudida = !it.activadoSacudida
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        guardando = false,
                        errorGeneral = "No se pudo guardar: ${e.mensajeAmigable()}",
                        activadoSacudida = !it.activadoSacudida
                    )
                }
            }
        }
    }
}