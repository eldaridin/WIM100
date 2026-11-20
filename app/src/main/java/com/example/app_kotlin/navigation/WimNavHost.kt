package com.example.app_kotlin.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.app_kotlin.ui.screens.CandidataFormScreen
import com.example.app_kotlin.ui.screens.CandidatasScreen
import com.example.app_kotlin.ui.screens.DetalleCandidataScreen
import com.example.app_kotlin.ui.screens.EvaluacionScreen
import com.example.app_kotlin.ui.screens.HistorialScreen
import com.example.app_kotlin.ui.screens.LoginScreen
import com.example.app_kotlin.ui.screens.PerfilScreen
import com.example.app_kotlin.ui.screens.ResultadosScreen
import com.example.app_kotlin.viewmodel.AppViewModelProvider
import com.example.app_kotlin.viewmodel.CandidataFormViewModel
import com.example.app_kotlin.viewmodel.CandidatasViewModel
import com.example.app_kotlin.viewmodel.DetalleCandidataViewModel
import com.example.app_kotlin.viewmodel.EvaluacionViewModel
import com.example.app_kotlin.viewmodel.HistorialViewModel
import com.example.app_kotlin.viewmodel.ResultadosViewModel
import com.example.app_kotlin.viewmodel.SesionViewModel

@Composable
fun WimNavHost(
    navController: NavHostController,
    sesionViewModel: SesionViewModel,
    modifier: Modifier = Modifier
) {
    val sesionState by sesionViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(320)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) },
        exitTransition = { fadeOut(animationSpec = tween(320)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) },
        popEnterTransition = { fadeIn(animationSpec = tween(320)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(320)) },
        popExitTransition = { fadeOut(animationSpec = tween(320)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(320)) }
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                state = sesionState,
                onUsuarioChange = sesionViewModel::onUsuarioChange,
                onPasswordChange = sesionViewModel::onPasswordChange,
                onValidar = sesionViewModel::validar,
                onLoginClick = { metodo ->
                    // Navega solo cuando el servidor acepta las credenciales
                    sesionViewModel.iniciarSesion(metodo) {
                        navController.navigate(Rutas.CANDIDATAS) {
                            popUpTo(Rutas.LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Rutas.CANDIDATAS) {
            val viewModel: CandidatasViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by viewModel.uiState.collectAsState()

            CandidatasScreen(
                state = state,
                onBusquedaChange = viewModel::onBusquedaChange,
                onFiltroEstadoChange = viewModel::onFiltroEstadoChange,
                onCandidataClick = { candidataId ->
                    navController.navigate(Rutas.detalleCandidata(candidataId))
                },
                onNuevaCandidataClick = {
                    navController.navigate(Rutas.NUEVA_CANDIDATA)
                },
                onRefrescarClick = viewModel::refrescar
            )
        }

        composable(Rutas.NUEVA_CANDIDATA) {
            val viewModel: CandidataFormViewModel = viewModel(
                factory = AppViewModelProvider.formViewModelFactory(null)
            )
            FormularioCandidata(viewModel = viewModel, onVolver = { navController.popBackStack() })
        }

        // Editar reutiliza la misma pantalla del formulario
        composable(
            route = Rutas.EDITAR_CANDIDATA,
            arguments = listOf(navArgument("candidataId") { type = NavType.LongType })
        ) { backStackEntry ->
            val candidataId = backStackEntry.arguments?.getLong("candidataId") ?: 0L
            val viewModel: CandidataFormViewModel = viewModel(
                factory = AppViewModelProvider.formViewModelFactory(candidataId)
            )
            FormularioCandidata(viewModel = viewModel, onVolver = { navController.popBackStack() })
        }

        composable(
            route = Rutas.DETALLE_CANDIDATA,
            arguments = listOf(navArgument("candidataId") { type = NavType.LongType })
        ) { backStackEntry ->
            val candidataId = backStackEntry.arguments?.getLong("candidataId") ?: 0L
            val viewModel: DetalleCandidataViewModel = viewModel(
                factory = AppViewModelProvider.detalleViewModelFactory(candidataId)
            )
            val state by viewModel.uiState.collectAsState()

            DetalleCandidataScreen(
                state = state,
                onEvaluarClick = {
                    navController.navigate(Rutas.evaluar(candidataId))
                },
                onResolverClick = viewModel::resolverCandidatura,
                onEditarClick = { navController.navigate(Rutas.editarCandidata(candidataId)) },
                onEliminarClick = viewModel::eliminarCandidata,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Rutas.EVALUAR,
            arguments = listOf(navArgument("candidataId") { type = NavType.LongType })
        ) { backStackEntry ->
            val candidataId = backStackEntry.arguments?.getLong("candidataId") ?: 0L
            val viewModel: EvaluacionViewModel = viewModel(
                factory = AppViewModelProvider.evaluacionViewModelFactory(candidataId)
            )
            val state by viewModel.uiState.collectAsState()

            EvaluacionScreen(
                state = state,
                onPuntajeChange = viewModel::onPuntajeChange,
                onObservacionCriterioChange = viewModel::onObservacionCriterioChange,
                onObservacionGeneralChange = viewModel::onObservacionGeneralChange,
                onGuardarClick = {
                    viewModel.guardarEvaluacion {
                        navController.popBackStack()
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Rutas.RESULTADOS) {
            val viewModel: ResultadosViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by viewModel.uiState.collectAsState()

            ResultadosScreen(
                state = state,
                onCandidataClick = { candidataId ->
                    navController.navigate(Rutas.detalleCandidata(candidataId))
                }
            )
        }

        composable(Rutas.HISTORIAL) {
            val viewModel: HistorialViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val state by viewModel.uiState.collectAsState()

            HistorialScreen(
                state = state,
                onBusquedaChange = viewModel::onBusquedaChange
            )
        }

        composable(Rutas.PERFIL) {
            PerfilScreen(
                onCerrarSesionClick = {
                    sesionViewModel.cerrarSesion {
                        navController.navigate(Rutas.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FormularioCandidata(
    viewModel: CandidataFormViewModel,
    onVolver: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    CandidataFormScreen(
        state = state,
        onNombreChange = viewModel::onNombreChange,
        onCorreoChange = viewModel::onCorreoChange,
        onTelefonoChange = viewModel::onTelefonoChange,
        onAreaChange = viewModel::onAreaChange,
        onAnnosExpChange = viewModel::onAnnosExpChange,
        onResumenExpChange = viewModel::onResumenExpChange,
        onLinkedinUrlChange = viewModel::onLinkedinUrlChange,
        onArchivoPdfSeleccionado = { uri -> viewModel.onArchivoPdfSeleccionado(context, uri) },
        onGenerarNombreClick = viewModel::generarNombreFicticio,
        onGuardarClick = { viewModel.guardarCandidata(context) { onVolver() } },
        onBackClick = onVolver
    )
}