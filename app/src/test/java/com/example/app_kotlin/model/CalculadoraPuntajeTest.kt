package com.example.app_kotlin.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculadoraPuntajeTest {

    private fun criterio(peso: Int) =
        Criterio(nombre = "Criterio", descripcion = "Descripcion", peso = peso, orden = 1)

    @Test
    fun `sin criterios el puntaje es cero`() {
        assertEquals(0.0, CalculadoraPuntaje.calcularPuntajeEscala100(emptyList()), 0.0)
    }

    @Test
    fun `todo en 5 da 100`() {
        val datos = listOf(5 to criterio(30), 5 to criterio(70))
        assertEquals(100.0, CalculadoraPuntaje.calcularPuntajeEscala100(datos), 0.0)
    }

    @Test
    fun `pondera los puntajes segun el peso del criterio`() {
        // (4*50 + 2*50) / (100*5) * 100 = 60.0
        val datos = listOf(4 to criterio(50), 2 to criterio(50))
        assertEquals(60.0, CalculadoraPuntaje.calcularPuntajeEscala100(datos), 0.0)
    }

    @Test
    fun `categorias segun el puntaje`() {
        assertEquals("Sobresaliente", CalculadoraPuntaje.obtenerCategoria(85.0))
        assertEquals("Sólida", CalculadoraPuntaje.obtenerCategoria(70.0))
        assertEquals("En desarrollo", CalculadoraPuntaje.obtenerCategoria(50.0))
        assertEquals("No cumple perfil", CalculadoraPuntaje.obtenerCategoria(49.9))
    }

    @Test
    fun `normalizar clave de evaluadora`() {
        assertEquals("camila_rojas_demo", CalculadoraPuntaje.normalizarClaveEvaluadora("  Camila  Rojas Demo "))
    }
}
