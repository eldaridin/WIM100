package com.example.app_kotlin.data.remote

import com.example.app_kotlin.model.EstadoCandidatura
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ErroresYMapperTest {

    private fun http(codigo: Int) = HttpException(Response.error<Any>(codigo, "".toResponseBody()))

    @Test
    fun `mensajeAmigable traduce los codigos HTTP conocidos`() {
        assertEquals("Datos inválidos: revisa el formulario", http(400).mensajeAmigable())
        assertEquals("Usuario o contraseña incorrectos", http(401).mensajeAmigable())
        assertEquals("El registro ya no existe en el servidor", http(404).mensajeAmigable())
        assertEquals("Ya existe una candidatura con este correo", http(409).mensajeAmigable())
        assertEquals("Error del servidor (500)", http(500).mensajeAmigable())
    }

    @Test
    fun `mensajeAmigable avisa cuando no hay conexion`() {
        assertEquals(
            "No se pudo conectar con el servidor. ¿Está corriendo el backend?",
            IOException("timeout").mensajeAmigable()
        )
    }

    @Test
    fun `mensajeAmigable usa un texto por defecto para errores desconocidos`() {
        assertEquals("Error inesperado", RuntimeException().mensajeAmigable())
    }

    private fun dto(estado: String?) = PostulanteDto(
        id = 4, nombre = "Ana Demo", correo = "ana@ejemplo.cl", telefono = "+56 9 1111 2222",
        area = "Seguridad ofensiva (Red Team)", aniosExperiencia = 5,
        resumenExperiencia = "Resumen ficticio de al menos treinta caracteres.",
        linkedinUrl = "https://www.linkedin.com/in/ana-demo", estado = estado,
        fechaRegistro = 99L, registradaPor = "Evaluadora Demo"
    )

    @Test
    fun `aEntidad copia los datos y conserva el cvUri local`() {
        val c = dto("EVALUADA").aEntidad("content://cv")

        assertEquals(4L, c.id)
        assertEquals(5, c.annosExp)
        assertEquals("content://cv", c.cvUri)
        assertEquals(EstadoCandidatura.EVALUADA, c.estado)
        assertEquals("Evaluadora Demo", c.registrador)
    }

    @Test
    fun `aEntidad usa POSTULADA si el estado es nulo o desconocido`() {
        assertEquals(EstadoCandidatura.POSTULADA, dto(null).aEntidad().estado)
        assertEquals(EstadoCandidatura.POSTULADA, dto("INVENTADO").aEntidad().estado)
    }

    @Test
    fun `aDto envia id nulo cuando la candidata aun no existe en el servidor`() {
        val nueva = dto("POSTULADA").aEntidad().copy(id = 0)

        assertNull(nueva.aDto().id)
        assertEquals("POSTULADA", nueva.aDto().estado)
    }
}
