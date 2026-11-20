package com.example.app_kotlin.data.local

import com.example.app_kotlin.di.AppContainer
import com.example.app_kotlin.model.Criterio

object DatosSemilla {

    suspend fun poblarSiVacia(container: AppContainer) {
        val db = container.database

        // La matriz de evaluación (criterios) sigue siendo local
        if (db.criterioDao().contarCriterios() == 0) {
            val criterios = listOf(
                Criterio(1, "Experiencia técnica en ciberseguridad", "Dominio de herramientas, metodologías y estándares de seguridad", 25, 1),
                Criterio(2, "Liderazgo y gestión de equipos", "Capacidad para dirigir proyectos y mentoría a otros profesionales", 20, 2),
                Criterio(3, "Innovación y proyectos destacados", "Contribuciones relevantes, desarrollo de herramientas o investigaciones", 20, 3),
                Criterio(4, "Impacto en la comunidad e industria", "Participación en conferencias, comunidades WIM o difusión técnica", 20, 4),
                Criterio(5, "Formación y certificaciones", "Grados académicos y certificaciones vigentes (CISSP, CISM, CEH, etc.)", 15, 5)
            )
            db.criterioDao().insertarCriterios(criterios)
        }

        // Los postulantes los crea el backend: se descargan al abrir la app.
        // Si el servidor está apagado no pasa nada, la lista se carga después.
        runCatching { container.candidataRepository.refrescar() }
    }
}