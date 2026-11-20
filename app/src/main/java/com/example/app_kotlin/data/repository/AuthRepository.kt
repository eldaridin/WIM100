package com.example.app_kotlin.data.repository

import com.example.app_kotlin.data.remote.LoginRequest
import com.example.app_kotlin.data.remote.WimApi
import com.example.app_kotlin.model.Rol
import com.example.app_kotlin.model.UsuarioSesion

class AuthRepository(private val api: WimApi) {

    /** Lanza HttpException(401) si las credenciales son incorrectas. */
    suspend fun login(usuario: String, password: String): UsuarioSesion {
        val r = api.login(LoginRequest(usuario.trim().lowercase(), password))
        return UsuarioSesion(id = r.id, usuario = r.usuario, nombre = r.nombre, rol = Rol.valueOf(r.rol))
    }
}