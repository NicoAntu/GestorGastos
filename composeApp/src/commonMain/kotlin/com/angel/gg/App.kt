package com.angel.gg

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.angel.gg.data.settings.SettingsRepository
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.navigation.RootComponent
import com.angel.gg.presentation.screen.*
import com.angel.gg.presentation.viewmodel.HomeViewModel
import org.koin.compose.koinInject

private val DarkColorScheme = darkColorScheme(
    primary          = AppTheme.Indigo,
    onPrimary        = Color.White,
    secondary        = AppTheme.IndigoLight,
    background       = AppTheme.BgDark,
    surface          = AppTheme.CardDark,
    onBackground     = AppTheme.TextPrimary,
    onSurface        = AppTheme.TextPrimary,
    surfaceVariant   = AppTheme.CardMedium,
    onSurfaceVariant = AppTheme.TextMuted,
    error            = AppTheme.Red,
    outline          = AppTheme.BorderColor
)

@Composable
fun App(root: RootComponent) {
    MaterialTheme(colorScheme = DarkColorScheme) {
        koinInject<SettingsRepository>()
        val stack      by root.stack.subscribeAsState()
        val homeViewModel: HomeViewModel = koinInject()

        Children(
            stack     = stack,
            animation = stackAnimation(slide())
        ) { child ->
            when (val instance = child.instance) {

                is RootComponent.Child.Home ->
                    HomeScreen(
                        onNavegarRegistrar  = { root.navegarA(RootComponent.Config.RegistrarGasto(homeViewModel.state.value.anioActual, homeViewModel.state.value.mesActual))   },
                        onNavegarBuscador   = { root.navegarA(RootComponent.Config.Buscador)         },
                        onNavegarEstadisticas = { root.navegarA(RootComponent.Config.Estadisticas)   },
                        onNavegarCategoriaDetalle = { root.navegarA(RootComponent.Config.CategoriaDetalle) },
                        onNavegarAjustes = { root.navegarA(RootComponent.Config.Ajustes) },
                        onVerDetalleCuotas  = { root.navegarA(RootComponent.Config.DetalleCuotas(it)) },
                        onEditarGasto       = { root.navegarA(RootComponent.Config.EditarGasto(it))   }
                    )

                is RootComponent.Child.RegistrarGasto ->
                    RegistrarGastoScreen(
                        anio              = instance.anio,
                        mes               = instance.mes,
                        onVolver          = { root.volver() },
                        onGuardadoExitoso = {
                            homeViewModel.setTab(0)
                            root.volver()
                        }
                    )

                is RootComponent.Child.Buscador ->
                    BuscadorScreen(
                        onVolver           = { root.volver() },
                        onVerDetalleCuotas = { root.navegarA(RootComponent.Config.DetalleCuotas(it)) }
                    )

                is RootComponent.Child.Estadisticas ->
                    EstadisticasScreen(
                        onVolver          = { root.volver() },
                        onVerDetalleCuota = { root.navegarA(RootComponent.Config.DetalleCuotas(it)) },
                        onVerCategorias   = { root.navegarA(RootComponent.Config.CategoriaDetalle) },
                        onVerTodasCuotas  = { root.navegarA(RootComponent.Config.CuotasMes) }
                    )

                is RootComponent.Child.CuotasMes ->
                    CuotasMesScreen(
                        onVolver          = { root.volver() },
                        onVerDetalleCuota = { root.navegarA(RootComponent.Config.DetalleCuotas(it)) }
                    )

                is RootComponent.Child.CategoriaDetalle ->
                    CategoriaDetalleScreen(onVolver = { root.volver() })

                is RootComponent.Child.MisCategorias ->
                    MisCategoriasScreen(onVolver = { root.volver() })

                is RootComponent.Child.Ajustes ->
                    AjustesScreen(
                        onVolver          = { root.volver() },
                        onVerMisCategorias = { root.navegarA(RootComponent.Config.MisCategorias) }
                    )

                is RootComponent.Child.DetalleCuotas ->
                    DetalleCuotasScreen(
                        gastoId  = instance.gastoId,
                        onVolver = { root.volver() }
                    )

                is RootComponent.Child.EditarGasto ->
                    EditarGastoScreen(
                        gastoId  = instance.gastoId,
                        onVolver = { root.volver() }
                    )
            }
        }
    }
}