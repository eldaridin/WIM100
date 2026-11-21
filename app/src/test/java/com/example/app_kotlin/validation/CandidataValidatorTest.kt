package com.example.app_kotlin.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidataValidatorTest {

    @Test
    fun `nombre exige nombre y apellido`() {
        assertTrue(CandidataValidator.validarNombre("Ana Demo").esValido)
        assertFalse(CandidataValidator.validarNombre("Ana").esValido)
        assertFalse(CandidataValidator.validarNombre("Ab").esValido)
        assertFalse(CandidataValidator.validarNombre("Ana 123").esValido)
    }

    @Test
    fun `correo solo acepta dominios ficticios permitidos`() {
        assertTrue(CandidataValidator.validarCorreo("ana@ejemplo.cl").esValido)
        assertTrue(CandidataValidator.validarCorreo("ana@example.com").esValido)
        assertFalse(CandidataValidator.validarCorreo("ana@gmail.com").esValido)
        assertFalse(CandidataValidator.validarCorreo("ana-sin-arroba").esValido)
        assertFalse(CandidataValidator.validarCorreo("").esValido)
    }

    @Test
    fun `telefono acepta formatos chilenos`() {
        assertTrue(CandidataValidator.validarTelefono("+56 9 1234 5678").esValido)
        assertTrue(CandidataValidator.validarTelefono("+56912345678").esValido)
        assertTrue(CandidataValidator.validarTelefono("912345678").esValido)
        assertFalse(CandidataValidator.validarTelefono("12345").esValido)
        assertFalse(CandidataValidator.validarTelefono("").esValido)
    }

    @Test
    fun `normalizarTelefono deja el formato +56 9 XXXX XXXX`() {
        assertEquals("+56 9 1234 5678", CandidataValidator.normalizarTelefono("912345678"))
        assertEquals("+56 9 1234 5678", CandidataValidator.normalizarTelefono("+56912345678"))
    }

    @Test
    fun `area debe estar en la lista oficial`() {
        assertTrue(CandidataValidator.validarArea("Seguridad ofensiva (Red Team)").esValido)
        assertFalse(CandidataValidator.validarArea("Cocina").esValido)
        assertFalse(CandidataValidator.validarArea("").esValido)
    }

    @Test
    fun `anios de experiencia entre 0 y 50`() {
        assertTrue(CandidataValidator.validarAnnosExp("0").esValido)
        assertTrue(CandidataValidator.validarAnnosExp("50").esValido)
        assertFalse(CandidataValidator.validarAnnosExp("51").esValido)
        assertFalse(CandidataValidator.validarAnnosExp("-1").esValido)
        assertFalse(CandidataValidator.validarAnnosExp("abc").esValido)
    }

    @Test
    fun `resumen entre 30 y 500 caracteres`() {
        assertFalse(CandidataValidator.validarResumenExp("corto").esValido)
        assertTrue(CandidataValidator.validarResumenExp("a".repeat(30)).esValido)
        assertFalse(CandidataValidator.validarResumenExp("a".repeat(501)).esValido)
    }

    @Test
    fun `linkedin es opcional pero si viene debe ser un perfil`() {
        assertTrue(CandidataValidator.validarLinkedinUrl("").esValido)
        assertTrue(CandidataValidator.validarLinkedinUrl("https://www.linkedin.com/in/ana-demo").esValido)
        assertFalse(CandidataValidator.validarLinkedinUrl("https://otra-red.com/ana").esValido)
    }

    @Test
    fun `cv es obligatorio`() {
        assertFalse(CandidataValidator.validarCv(null).esValido)
        assertFalse(CandidataValidator.validarCv("  ").esValido)
        assertTrue(CandidataValidator.validarCv("content://cv.pdf").esValido)
    }
}
