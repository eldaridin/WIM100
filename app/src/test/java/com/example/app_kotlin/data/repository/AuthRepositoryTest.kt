package com.example.app_kotlin.data.repository

import com.example.app_kotlin.data.remote.LoginRequest
import com.example.app_kotlin.data.remote.LoginResponse
import com.example.app_kotlin.data.remote.WimApi
import com.example.app_kotlin.model.Rol
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AuthRepositoryTest {

    private val api = mockk<WimApi>()
    private val repo = AuthRepository(api)

    @Test
    fun `login convierte la respuesta del servidor en UsuarioSesion`() = runTest {
        coEvery { api.login(any()) } returns
            LoginResponse(id = 1, usuario = "evaluadora1", nombre = "Camila Rojas Demo", rol = "EVALUADORA")

        val usuario = repo.login("evaluadora1", "wim2026")

        assertEquals(1L, usuario.id)
        assertEquals("Camila Rojas Demo", usuario.nombre)
        assertEquals(Rol.EVALUADORA, usuario.rol)
    }

    @Test
    fun `login normaliza el usuario a minusculas y sin espacios`() = runTest {
        coEvery { api.login(any()) } returns
            LoginResponse(id = 3, usuario = "direccion", nombre = "Direccion Demo", rol = "DIRECCION")

        repo.login("  Direccion ", "wim2026")

        coVerify { api.login(LoginRequest("direccion", "wim2026")) }
    }

    @Test(expected = HttpException::class)
    fun `login propaga el 401 cuando las credenciales son incorrectas`() = runTest {
        coEvery { api.login(any()) } throws HttpException(Response.error<Any>(401, "".toResponseBody()))

        repo.login("evaluadora1", "malapass")
    }
}
