package com.example.app_kotlin.data.repository

import com.example.app_kotlin.data.local.dao.CandidataDao
import com.example.app_kotlin.data.local.dao.HistorialDao
import com.example.app_kotlin.data.remote.EstadoRequest
import com.example.app_kotlin.data.remote.PostulanteDto
import com.example.app_kotlin.data.remote.WimApi
import com.example.app_kotlin.model.Candidata
import com.example.app_kotlin.model.EstadoCandidatura
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CandidataRepositoryTest {

    private val api = mockk<WimApi>()
    private val candidataDao = mockk<CandidataDao>(relaxed = true)
    private val historialDao = mockk<HistorialDao>(relaxed = true)
    private lateinit var repo: CandidataRepository

    private fun dto(id: Long) = PostulanteDto(
        id = id,
        nombre = "Ana Demo",
        correo = "ana@ejemplo.cl",
        telefono = "+56 9 1111 2222",
        area = "Seguridad ofensiva (Red Team)",
        aniosExperiencia = 5,
        resumenExperiencia = "Resumen ficticio de al menos treinta caracteres.",
        linkedinUrl = "https://www.linkedin.com/in/ana-demo",
        estado = "POSTULADA",
        fechaRegistro = 1L,
        registradaPor = "Evaluadora Demo"
    )

    private fun candidataLocal(id: Long = 0, cvUri: String? = null) = Candidata(
        id = id,
        nombre = "Ana Demo",
        correo = "ana@ejemplo.cl",
        telefono = "+56 9 1111 2222",
        area = "Seguridad ofensiva (Red Team)",
        annosExp = 5,
        resumenExp = "Resumen ficticio de al menos treinta caracteres.",
        linkedinUrl = "https://www.linkedin.com/in/ana-demo",
        cvUri = cvUri,
        cvNombre = "cv.pdf",
        registrador = "Evaluadora Demo"
    )

    @Before
    fun setUp() {
        repo = CandidataRepository(candidataDao, historialDao, api)
    }

    @Test
    fun `registrar guarda la copia local con el id que entrega el servidor`() = runTest {
        coEvery { api.crear(any()) } returns dto(10)

        val id = repo.registrarCandidata(candidataLocal(cvUri = "content://cv"))

        assertEquals(10L, id)
        coVerify { candidataDao.upsert(match { it.id == 10L && it.cvUri == "content://cv" }) }
        coVerify { historialDao.insertar(any()) }
    }

    @Test
    fun `refrescar conserva el cvUri local y borra las que ya no existen`() = runTest {
        coEvery { api.listar() } returns listOf(dto(1))
        coEvery { candidataDao.obtenerTodasLista() } returns
            listOf(candidataLocal(id = 1, cvUri = "content://local"))

        repo.refrescar()

        coVerify { candidataDao.eliminarNoIncluidas(listOf(1L)) }
        coVerify { candidataDao.upsertTodas(match { it.single().cvUri == "content://local" }) }
    }

    @Test
    fun `actualizar envia el PUT y guarda la copia local`() = runTest {
        coEvery { api.actualizar(3, any()) } returns dto(3)

        repo.actualizarCandidata(candidataLocal(id = 3), "Evaluadora Demo")

        coVerify { api.actualizar(3, any()) }
        coVerify { candidataDao.upsert(match { it.id == 3L }) }
    }

    @Test
    fun `eliminar borra la copia local aunque el servidor responda 404`() = runTest {
        coEvery { candidataDao.obtenerPorId(5) } returns null
        coEvery { api.eliminar(5) } returns Response.error<Unit>(404, "".toResponseBody())

        repo.eliminarCandidata(5, "Evaluadora Demo")

        coVerify { candidataDao.eliminar(5) }
    }

    @Test(expected = HttpException::class)
    fun `eliminar lanza error si el servidor responde 500`() = runTest {
        coEvery { candidataDao.obtenerPorId(5) } returns null
        coEvery { api.eliminar(5) } returns Response.error<Unit>(500, "".toResponseBody())

        repo.eliminarCandidata(5, "Evaluadora Demo")
    }

    @Test
    fun `sincronizar estado envia el estado local al servidor`() = runTest {
        coEvery { candidataDao.obtenerPorId(7) } returns
            candidataLocal(id = 7).copy(estado = EstadoCandidatura.EVALUADA)
        coEvery { api.cambiarEstado(7, any()) } returns dto(7)

        repo.sincronizarEstado(7)

        coVerify { api.cambiarEstado(7, EstadoRequest("EVALUADA")) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `resolver candidatura rechaza estados que no son finales`() = runTest {
        repo.resolverCandidatura(1, EstadoCandidatura.EN_EVALUACION, "Direccion Demo")
    }
}
