package com.example.app_kotlin.model

enum class Rol(val titulo: String, val descripcion: String) {
    EVALUADORA("Evaluadora", "Registra candidaturas y evalúa según la matriz WIM"),
    DIRECCION("Dirección", "Consulta resultados globales, ranking y resuelve postulaciones")
}

/** Usuario que devuelve el backend al iniciar sesión (POST /api/auth/login). */
data class UsuarioSesion(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: Rol
)

object Sesion {
    /** Nombre visible del usuario (ej. "Camila Rojas Demo"). Se usa como registrador/evaluadora. */
    var usuarioActivo: String = ""
    var rolActivo: Rol = Rol.EVALUADORA

    /** Datos que entrega el servidor. */
    var idUsuario: Long = 0
    var loginUsuario: String = ""

    val estaAutenticado: Boolean
        get() = usuarioActivo.isNotBlank()

    fun iniciar(u: UsuarioSesion) {
        usuarioActivo = u.nombre
        rolActivo = u.rol
        idUsuario = u.id
        loginUsuario = u.usuario
    }

    fun cerrar() {
        usuarioActivo = ""
        rolActivo = Rol.EVALUADORA
        idUsuario = 0
        loginUsuario = ""
    }
}