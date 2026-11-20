package com.example.app_kotlin.data.repository

import com.example.app_kotlin.data.local.dao.CandidataDao
import com.example.app_kotlin.data.local.dao.HistorialDao
import com.example.app_kotlin.data.remote.EstadoRequest
import com.example.app_kotlin.data.remote.WimApi
import com.example.app_kotlin.data.remote.aDto
import com.example.app_kotlin.data.remote.aEntidad
import com.example.app_kotlin.model.Acciones
import com.example.app_kotlin.model.Candidata
import com.example.app_kotlin.model.EstadoCandidatura
import com.example.app_kotlin.model.RegistroHistorial
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

/**
 * Postulantes: el microservicio (wim100-backend) es la fuente oficial.
 * Room guarda una copia local que observan las pantallas y que necesitan
 * las evaluaciones (llave foránea a la candidata).
 */
class CandidataRepository(
    private val candidataDao: CandidataDao,
    private val historialDao: HistorialDao,
    private val api: WimApi
) {
    val todasCandidatas: Flow<List<Candidata>> = candidataDao.obtenerTodas()

    fun obtenerPorIdFlow(id: Long): Flow<Candidata?> = candidataDao.obtenerPorIdFlow(id)

    suspend fun obtenerPorId(id: Long): Candidata? = candidataDao.obtenerPorId(id)

    /** READ: trae la lista del servidor y actualiza la copia local. */
    suspend fun refrescar() {
        val remotas = api.listar()
        // El cvUri es una ruta del teléfono: no viaja al servidor, se conserva el local
        val cvLocales = candidataDao.obtenerTodasLista().associate { it.id to it.cvUri }
        candidataDao.eliminarNoIncluidas(remotas.mapNotNull { it.id })
        candidataDao.upsertTodas(remotas.map { it.aEntidad(cvLocales[it.id]) })
    }

    /** CREATE: el servidor asigna el id; luego se guarda la copia local con ese id. */
    suspend fun registrarCandidata(candidata: Candidata): Long {
        val creada = api.crear(candidata.aDto())
        val id = checkNotNull(creada.id) { "El servidor no devolvió el id" }
        candidataDao.upsert(creada.aEntidad(candidata.cvUri))
        historialDao.insertar(
            RegistroHistorial(
                usuario = candidata.registrador,
                accion = Acciones.REGISTRO_CANDIDATA,
                detalle = "Candidata ${creada.nombre} registrada exitosamente en área ${creada.area}",
                candidataId = id
            )
        )
        return id
    }

    /** UPDATE: PUT al servidor y luego se actualiza la copia local. */
    suspend fun actualizarCandidata(candidata: Candidata, usuario: String) {
        val actualizada = api.actualizar(candidata.id, candidata.aDto())
        candidataDao.upsert(actualizada.aEntidad(candidata.cvUri))
        historialDao.insertar(
            RegistroHistorial(
                usuario = usuario,
                accion = Acciones.EDICION_CANDIDATA,
                detalle = "Se editaron los datos de ${actualizada.nombre}",
                candidataId = candidata.id
            )
        )
    }

    /** DELETE: si el servidor responde 404 ya no existe allá, igual se borra la copia local. */
    suspend fun eliminarCandidata(id: Long, usuario: String) {
        val nombre = candidataDao.obtenerPorId(id)?.nombre ?: "#$id"
        val respuesta = api.eliminar(id)
        if (!respuesta.isSuccessful && respuesta.code() != 404) throw HttpException(respuesta)
        candidataDao.eliminar(id)
        historialDao.insertar(
            RegistroHistorial(
                usuario = usuario,
                accion = Acciones.ELIMINACION_CANDIDATA,
                detalle = "Se eliminó a $nombre",
                candidataId = null
            )
        )
    }

    /** Envía al servidor el estado que calculó la app después de evaluar (PATCH /estado). */
    suspend fun sincronizarEstado(id: Long) {
        val local = candidataDao.obtenerPorId(id) ?: return
        api.cambiarEstado(id, EstadoRequest(local.estado.name))
    }

    /** Dirección cierra la candidatura como seleccionada o no seleccionada. */
    suspend fun resolverCandidatura(candidataId: Long, nuevoEstado: EstadoCandidatura, usuarioDireccion: String) {
        require(nuevoEstado.estaResuelta) { "Solo se puede resolver como Seleccionada o No seleccionada" }
        val candidata = candidataDao.obtenerPorId(candidataId) ?: return
        api.cambiarEstado(candidataId, EstadoRequest(nuevoEstado.name))
        candidataDao.actualizarEstado(candidataId, nuevoEstado)
        historialDao.insertar(
            RegistroHistorial(
                usuario = usuarioDireccion,
                accion = Acciones.CANDIDATURA_RESUELTA,
                detalle = "Postulación de ${candidata.nombre} resuelta como: ${nuevoEstado.titulo}",
                candidataId = candidataId
            )
        )
    }
}