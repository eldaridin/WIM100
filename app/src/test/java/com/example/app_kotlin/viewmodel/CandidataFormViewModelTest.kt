package com.example.app_kotlin.viewmodel

import android.content.Context
import com.example.app_kotlin.data.repository.CandidataRepository
import com.example.app_kotlin.data.repository.RandomUserRepository
import com.example.app_kotlin.model.Candidata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CandidataFormViewModelTest {

    private val repo = mockk<CandidataRepository>()
    private val randomUser = mockk<RandomUserRepository>()
    private val context = mockk<Context>(relaxed = true)

    private val existente = Candidata(
        id = 1, nombre = "Ana Demo", correo = "ana@ejemplo.cl", telefono = "+56 9 1111 2222",
        area = "Seguridad ofensiva (Red Team)", annosExp = 5,
        resumenExp = "Resumen ficticio de al menos treinta caracteres.",
        linkedinUrl = "", cvNombre = "cv.pdf", registrador = "Evaluadora Demo"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { repo.obtenerPorId(1) } returns existente
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `modo edicion carga los datos de la candidata`() = runTest {
        val vm = CandidataFormViewModel(1L, repo, randomUser)

        assertTrue(vm.uiState.value.esEdicion)
        assertEquals("Ana Demo", vm.uiState.value.nombre.valor)
        assertEquals("5", vm.uiState.value.annosExp.valor)
    }

    @Test
    fun `un 409 del servidor marca el error en el correo`() = runTest {
        coEvery { repo.actualizarCandidata(any(), any()) } throws
            HttpException(Response.error<Any>(409, "".toResponseBody()))
        val vm = CandidataFormViewModel(1L, repo, randomUser)
        var exito = false

        vm.guardarCandidata(context) { exito = true }

        assertFalse(exito)
        assertEquals("Ya existe una candidatura con este correo", vm.uiState.value.correo.error)
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun `guardar correcto llama al repositorio y avisa el exito`() = runTest {
        coEvery { repo.actualizarCandidata(any(), any()) } returns Unit
        val vm = CandidataFormViewModel(1L, repo, randomUser)
        var exito = false

        vm.guardarCandidata(context) { exito = true }

        assertTrue(exito)
        assertTrue(vm.uiState.value.exitoGuardado)
        coVerify { repo.actualizarCandidata(match { it.id == 1L }, any()) }
    }

    @Test
    fun `guardar con un campo invalido no llama al servidor`() = runTest {
        val vm = CandidataFormViewModel(1L, repo, randomUser)
        vm.onCorreoChange("correo-malo")

        vm.guardarCandidata(context) { }

        assertNotNull(vm.uiState.value.correo.error)
        coVerify(exactly = 0) { repo.actualizarCandidata(any(), any()) }
    }

    @Test
    fun `generar nombre ficticio completa el campo nombre con la API externa`() = runTest {
        coEvery { randomUser.nombreFicticio() } returns "Lucía Gómez"
        val vm = CandidataFormViewModel(null, repo, randomUser)

        vm.generarNombreFicticio()

        assertEquals("Lucía Gómez", vm.uiState.value.nombre.valor)
    }

    @Test
    fun `si la API externa falla muestra un error general`() = runTest {
        coEvery { randomUser.nombreFicticio() } throws java.io.IOException("sin internet")
        val vm = CandidataFormViewModel(null, repo, randomUser)

        vm.generarNombreFicticio()

        assertNotNull(vm.uiState.value.errorGeneral)
    }
}
