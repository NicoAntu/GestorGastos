package com.angel.gg.presentation.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

class RootComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<Config, Child>> = childStack(
        source               = navigation,
        serializer           = Config.serializer(),
        initialConfiguration = Config.Home,
        handleBackButton     = true,
        childFactory         = ::createChild
    )

    @Serializable
    sealed class Config {
        @Serializable data object CuotasMes : Config()
        @Serializable data object Home             : Config()
        @Serializable data class RegistrarGasto(val anio: Int, val mes: Int) : Config()
        @Serializable data object Buscador         : Config()
        @Serializable data object Estadisticas     : Config()
        @Serializable data object CategoriaDetalle : Config()
        @Serializable data object MisCategorias    : Config()
        @Serializable data object Ajustes          : Config()
        @Serializable data class  DetalleCuotas(val gastoId: String) : Config()
        @Serializable data class  EditarGasto(val gastoId: String)   : Config()
    }

    sealed class Child {
        data object CuotasMes : Child()
        data object Home             : Child()
        data class RegistrarGasto(val anio: Int, val mes: Int) : Child()
        data object Buscador         : Child()
        data object Estadisticas     : Child()
        data object CategoriaDetalle : Child()
        data object MisCategorias    : Child()
        data object Ajustes          : Child()
        data class  DetalleCuotas(val gastoId: String) : Child()
        data class  EditarGasto(val gastoId: String)   : Child()
    }

    private fun createChild(config: Config, context: ComponentContext): Child =
        when (config) {
            Config.CuotasMes -> Child.CuotasMes
            Config.Home             -> Child.Home
            is Config.RegistrarGasto -> Child.RegistrarGasto(config.anio, config.mes)
            Config.Buscador         -> Child.Buscador
            Config.Estadisticas     -> Child.Estadisticas
            Config.CategoriaDetalle -> Child.CategoriaDetalle
            Config.MisCategorias    -> Child.MisCategorias
            Config.Ajustes          -> Child.Ajustes
            is Config.DetalleCuotas -> Child.DetalleCuotas(config.gastoId)
            is Config.EditarGasto   -> Child.EditarGasto(config.gastoId)
        }

    fun navegarA(config: Config) = navigation.push(config)
    fun volver()                 = navigation.pop()
}