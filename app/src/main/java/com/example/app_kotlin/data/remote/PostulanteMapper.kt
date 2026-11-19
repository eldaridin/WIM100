package com.example.app_kotlin.data.remote

import com.example.app_kotlin.model.Candidata
import com.example.app_kotlin.model.EstadoCandidatura


/** DTO del servidor → entidad local. cvUri no viaja al servidor: se conserva el local. */
fun PostulanteDto.aEntidad(cvUriLocal: String? = null) = Candidata(
    id = id ?: 0,
    nombre = nombre,
    correo = correo,
    telefono = telefono,
    area = area,
    annosExp = aniosExperiencia,
    resumenExp = resumenExperiencia,
    linkedinUrl = linkedinUrl,
    cvUri = cvUriLocal,
    cvNombre = cvNombre,
    estado = estado
        ?.let { runCatching { EstadoCandidatura.valueOf(it) }.getOrNull() }
        ?: EstadoCandidatura.POSTULADA,
    fechaRegistro = fechaRegistro ?: System.currentTimeMillis(),
    registrador = registradaPor
)

/** Entidad local → DTO para enviar al servidor. */
fun Candidata.aDto() = PostulanteDto(
    id = id.takeIf { it > 0 },
    nombre = nombre,
    correo = correo,
    telefono = telefono,
    area = area,
    aniosExperiencia = annosExp,
    resumenExperiencia = resumenExp,
    linkedinUrl = linkedinUrl,
    cvNombre = cvNombre,
    estado = estado.name,
    fechaRegistro = fechaRegistro,
    registradaPor = registrador
)