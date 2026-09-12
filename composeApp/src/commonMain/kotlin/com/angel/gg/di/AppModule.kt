package com.angel.gg.di

import com.angel.gg.data.db.DatabaseDriverFactory
import com.angel.gg.data.repository.CategoriaRepositoryImpl
import com.angel.gg.data.repository.GastoRepositoryImpl
import com.angel.gg.data.repository.IngresoMensualRepositoryImpl
import com.angel.gg.data.settings.SettingsRepository
import com.angel.gg.data.settings.SettingsStore
import com.angel.gg.db.GastosDatabase
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.IngresoMensualRepository
import com.angel.gg.domain.usecase.BuscarGastosUseCase
import com.angel.gg.domain.usecase.EditarCuotasUseCase
import com.angel.gg.domain.usecase.EliminarCuotasUseCase
import com.angel.gg.domain.usecase.ExportarGastosUseCase
import com.angel.gg.domain.usecase.GenerarCuotasUseCase
import com.angel.gg.domain.usecase.GestionarCategoriaUseCase
import com.angel.gg.domain.usecase.ImportarGastosUseCase
import com.angel.gg.domain.usecase.GuardarIngresoMensualUseCase
import com.angel.gg.domain.usecase.ObtenerCronogramaCuotasUseCase
import com.angel.gg.domain.usecase.ObtenerResumenMesUseCase
import com.angel.gg.domain.usecase.RegistrarGastoUseCase
import com.angel.gg.export.ExportarGuardado
import com.angel.gg.presentation.i18n.Money
import com.angel.gg.presentation.i18n.Strings
import com.angel.gg.presentation.viewmodel.BuscadorViewModel
import com.angel.gg.presentation.viewmodel.CategoriaViewModel
import com.angel.gg.presentation.viewmodel.DashboardViewModel
import com.angel.gg.presentation.viewmodel.DetalleCuotasViewModel
import com.angel.gg.presentation.viewmodel.EditarGastoViewModel
import com.angel.gg.presentation.viewmodel.EstadisticasViewModel
import com.angel.gg.presentation.viewmodel.ExportarViewModel
import com.angel.gg.presentation.viewmodel.HomeViewModel
import com.angel.gg.presentation.viewmodel.ImportarViewModel
import com.angel.gg.presentation.viewmodel.RegistrarGastoViewModel
import org.koin.dsl.module

val dataModule = module {

    single<GastosDatabase> {
        GastosDatabase(driver = get<DatabaseDriverFactory>().createDriver())
    }

    single<CategoriaRepository> {
        CategoriaRepositoryImpl(database = get())
    }

    single<GastoRepository> {
        GastoRepositoryImpl(database = get())
    }

    single<IngresoMensualRepository> {
        IngresoMensualRepositoryImpl(database = get())
    }

    single<SettingsRepository> {
        SettingsRepository(get<SettingsStore>()).also {
            Strings.init(it)
            Money.init(it)
        }
    }
}

val domainModule = module {

    factory {
        RegistrarGastoUseCase(
            gastoRepository     = get(),
            categoriaRepository = get()
        )
    }

    factory {
        GenerarCuotasUseCase(
            gastoRepository     = get(),
            categoriaRepository = get()
        )
    }

    factory {
        EditarCuotasUseCase(
            gastoRepository     = get(),
            categoriaRepository = get()
        )
    }

    factory {
        EliminarCuotasUseCase(
            gastoRepository = get()
        )
    }

    factory {
        ObtenerCronogramaCuotasUseCase(
            gastoRepository = get()
        )
    }

    factory {
        ObtenerResumenMesUseCase(
            gastoRepository          = get(),
            ingresoMensualRepository = get()
        )
    }

    factory {
        GuardarIngresoMensualUseCase(
            ingresoMensualRepository = get()
        )
    }

    factory {
        BuscarGastosUseCase(
            gastoRepository = get()
        )
    }

    factory {
        GestionarCategoriaUseCase(
            categoriaRepository = get(),
            gastoRepository     = get()
        )
    }

    factory {
        ExportarGastosUseCase(
            gastoRepository          = get(),
            ingresoMensualRepository = get()
        )
    }

    factory {
        ImportarGastosUseCase(
            gastoRepository           = get(),
            categoriaRepository       = get(),
            ingresoMensualRepository  = get(),
            gestionarCategoriaUseCase = get()
        )
    }
}

val viewModelModule = module {

    single {
        HomeViewModel(
            gastoRepository          = get(),
            ingresoMensualRepository = get(),
            eliminarCuotasUseCase    = get(),
            guardarIngresoUseCase    = get()
        )
    }

    factory {
        EstadisticasViewModel(
            gastoRepository          = get(),
            ingresoMensualRepository = get(),
            obtenerResumenMesUseCase = get()
        )
    }

    factory {
        RegistrarGastoViewModel(
            registrarGastoUseCase = get(),
            generarCuotasUseCase  = get(),
            categoriaRepository   = get()
        )
    }

    factory {
        CategoriaViewModel(
            gestionarCategoriaUseCase = get(),
            categoriaRepository       = get()
        )
    }

    factory {
        DetalleCuotasViewModel(
            obtenerCronogramaUseCase = get(),
            editarCuotasUseCase      = get(),
            eliminarCuotasUseCase    = get()
        )
    }

    factory {
        DashboardViewModel(
            obtenerResumenMesUseCase = get()
        )
    }

    factory {
        BuscadorViewModel(
            buscarGastosUseCase = get()
        )
    }

    factory {
        EditarGastoViewModel(
            gastoRepository     = get(),
            categoriaRepository = get(),
            editarCuotasUseCase = get()
        )
    }

    factory {
        ExportarViewModel(
            exportarGastosUseCase = get(),
            exportarGuardado      = get()
        )
    }

    factory {
        ImportarViewModel(
            importarLectura       = get(),
            importarGastosUseCase = get()
        )
    }
}
