package com.example.app_kotlin.viewmodel

import com.example.app_kotlin.data.repository.AuthRepository
import com.example.app_kotlin.data.repository.HistorialRepository
import com.example.app_kotlin.model.Rol
import com.example.app_kotlin.model.Sesion
import com.example.app_kotlin.model.UsuarioSesion
import io.mockk.coEvery
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
class SesionViewModelTest {

    private val auth = mockk<AuthRepository>()
    private val historial = mockk<HistorialRepository>(relaxed = true)
    private lateinit var vm: SesionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        Sesion.cerrar()
        vm = SesionViewModel(auth, historial)
    }

    @After
    fun tearDown() {
        Sesion.cerrar()
        Dispatchers.resetMain()
    }

    @Test
    fun `validar falla con campos vacios y marca los errores`() {
        assertFalse(vm.validar())

        assertNotNull(vm.uiState.value.usuario.error)
        assertNotNull(vm.uiState.value.password.error)
    }

    @Test
    fun `login correcto crea la sesion con el rol que entrega el servidor`() = runTest {
        coEvery { auth.login("evaluadora1", "wim2026") } returns
            UsuarioSesion(id = 1, usuario = "evaluadora1", nombre = "Camila Rojas Demo", rol = Rol.EVALUADORA)
        vm.onUsuarioChange("evaluadora1")
        vm.onPasswordChange("wim2026")
        var navego = false

        vm.iniciarSesion("test") { navego = true }

        assertTrue(navego)
        assertTrue(vm.uiState.value.estaAutenticado)
        assertEquals(Rol.EVALUADORA, Sesion.rolActivo)
        assertEquals("Camila Rojas Demo", Sesion.usuarioActivo)
    }

    @Test
    fun `login con 401 muestra el error y no crea sesion`() = runTest {
        coEvery { auth.login(any(), any()) } throws
            HttpException(Response.error<Any>(401, "".toResponseBody()))
        vm.onUsuarioChange("evaluadora1")
        vm.onPasswordChange("malapass")
        var navego = false

        vm.iniciarSesion("test") { navego = true }

        assertFalse(navego)
        assertFalse(Sesion.estaAutenticado)
        assertEquals("Usuario o contraseña incorrectos", vm.uiState.value.mensajeErrorGeneral)
        assertFalse(vm.uiState.value.cargando)
    }
}
