package com.example.app_kotlin.di

import android.content.Context
import com.example.app_kotlin.data.local.WimDatabase
import com.example.app_kotlin.data.remote.RetrofitClient
import com.example.app_kotlin.data.repository.AuthRepository
import com.example.app_kotlin.data.repository.CandidataRepository
import com.example.app_kotlin.data.repository.EvaluacionRepository
import com.example.app_kotlin.data.repository.HistorialRepository
import com.example.app_kotlin.data.repository.RandomUserRepository
import com.example.app_kotlin.nativo.Notificador
import com.example.app_kotlin.nativo.NotificadorAndroid

class AppContainer(private val context: Context) {

    val database: WimDatabase by lazy {
        WimDatabase.obtener(context)
    }

    val candidataRepository: CandidataRepository by lazy {
        CandidataRepository(database.candidataDao(), database.historialDao(), RetrofitClient.wimApi)
    }

    val evaluacionRepository: EvaluacionRepository by lazy {
        EvaluacionRepository(database)
    }

    val historialRepository: HistorialRepository by lazy {
        HistorialRepository(database.historialDao())
    }

    /** Login contra el microservicio propio. */
    val authRepository: AuthRepository by lazy {
        AuthRepository(RetrofitClient.wimApi)
    }

    /** API externa (randomuser.me). */
    val randomUserRepository: RandomUserRepository by lazy {
        RandomUserRepository(RetrofitClient.randomUserApi)
    }

    val notificador: Notificador by lazy {
        NotificadorAndroid(context)
    }
}