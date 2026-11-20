package com.example.app_kotlin.data.repository

import com.example.app_kotlin.data.remote.RandomUserApi

class RandomUserRepository(private val api: RandomUserApi) {

    /** Devuelve un nombre y apellido ficticios, ej. "Lucía Gómez". */
    suspend fun nombreFicticio(): String {
        val nombre = api.generar().results.first().name
        return "${nombre.first} ${nombre.last}"
    }
}