package com.example.app_kotlin.viewmodel

import com.example.app_kotlin.data.repository.CandidataRepository
import com.example.app_kotlin.model.Candidata
import com.example.app_kotlin.model.EstadoCandidatura
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CandidatasViewModelTest {

    private val repo = mockk<CandidataRepository>()

    private fun candidata(id: Long, nombre: String, estado: EstadoCandidatura = EstadoCandidatura.POSTULADA) =
        Candidata(
            id = id, nombre = nombre, correo = "c$id@ejemplo.cl", telefono = "+56 9 1111 2222",
            area = "Seguridad ofensiva (Red Team)", annosExp = 3,
            resumenExp = "Resumen ficticio de al menos treinta caracteres.",
            linkedinUrl = "", estado = estado, registrador = "Evaluadora Demo"
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refrescar con error de red muestra el mensaje amigable`() = runTest {
        every { repo.todasCandidatas } returns flowOf(emptyList())
        coEvery { repo.refrescar() } throws IOException("sin conexion")

        val vm = CandidatasViewModel(repo)
        val estado = vm.uiState.first { it.errorRed != null }

        assertEquals("No se pudo conectar con el servidor. ¿Está corriendo el backend?", estado.errorRed)
        assertEquals(false, estado.actualizando)
    }

    @Test
    fun `refrescar correcto no deja error`() = runTest {
        every { repo.todasCandidatas } returns flowOf(listOf(candidata(1, "Ana Demo")))
        coEvery { repo.refrescar() } returns Unit

        val vm = CandidatasViewModel(repo)
        val estado = vm.uiState.first { !it.cargando }

        assertNull(estado.errorRed)
        assertEquals(1, estado.candidatas.size)
        coVerify(exactly = 1) { repo.refrescar() }
    }

    @Test
    fun `la busqueda filtra por nombre ignorando mayusculas`() = runTest {
        every { repo.todasCandidatas } returns
            flowOf(listOf(candidata(1, "Ana Demo"), candidata(2, "Bruno Prueba")))
        coEvery { repo.refrescar() } returns Unit

        val vm = CandidatasViewModel(repo)
        vm.onBusquedaChange("bruno")
        val estado = vm.uiState.first { it.busquedaTexto == "bruno" }

        assertEquals(listOf("Bruno Prueba"), estado.candidatas.map { it.nombre })
    }

    @Test
    fun `el filtro por estado deja solo las candidatas de ese estado`() = runTest {
        every { repo.todasCandidatas } returns flowOf(
            listOf(
                candidata(1, "Ana Demo", EstadoCandidatura.POSTULADA),
                candidata(2, "Bruno Prueba", EstadoCandidatura.EVALUADA)
            )
        )
        coEvery { repo.refrescar() } returns Unit

        val vm = CandidatasViewModel(repo)
        vm.onFiltroEstadoChange(EstadoCandidatura.EVALUADA)
        val estado = vm.uiState.first { it.filtroEstado == EstadoCandidatura.EVALUADA }

        assertEquals(listOf("Bruno Prueba"), estado.candidatas.map { it.nombre })
    }
}
