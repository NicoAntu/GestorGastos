package com.angel.gg.presentation.i18n

object EsStrings {

    val map = mutableMapOf(
        "comun.volver"          to "Volver",
        "comun.guardar"         to "Guardar",
        "comun.cancelar"        to "Cancelar",
        "comun.eliminar"        to "Eliminar",
        "comun.editar"          to "Editar",
        "comun.confirmar"       to "Confirmar",

        // Mensajes de ViewModels
        "vm.errMontoValido"     to "Ingresá un monto válido",
        "vm.errCantCuotas"      to "Ingresá una cantidad de cuotas válida",
        "vm.errMontoMayorCero"  to "El monto debe ser mayor a cero",
        "vm.errDescripcion"     to "La descripción no puede estar vacía",
        "vm.errCategoria"       to "Seleccioná una categoría válida",
        "vm.errCronograma"      to "No se pudo cargar el cronograma",
        "vm.errNoEliminarCuota" to "No se pudo eliminar la cuota",
        "vm.errGastoNoEncontrado" to "No se encontró el gasto",
        "vm.errCuotaNoEncontrada" to "No se encontró la cuota",
        "vm.errNoEsCuota"       to "Este gasto no es una cuota",
        "vm.errEditar"          to "Error al editar: {0}",
        "vm.errResumen"         to "No se pudo cargar el resumen",
        "vm.errIngresoNegativo" to "El monto no puede ser negativo",
        "vm.ingresoGuardado"    to "Ingreso guardado correctamente",
        "vm.busquedaMinima"     to "La búsqueda requiere al menos 2 caracteres",
        "vm.catCreada"          to "Categoría creada correctamente",
        "vm.catActualizada"     to "Categoría actualizada",
        "vm.catEliminada"       to "Categoría eliminada",
        "vm.errNombreVacio"     to "El nombre no puede estar vacío",
        "vm.errColor"           to "El color debe ser un hexadecimal válido (#RRGGBB)",
        "vm.errCatInexistente"  to "La categoría no existe",
        "vm.errCatConGastos"    to "No se puede eliminar: la categoría tiene {0} gastos registrados. Editá o eliminá esos gastos para poder eliminarla.",
        "vm.editarSoloEsta"     to "Cuota editada correctamente",
        "vm.editarSiguientes"   to "Cuota editada y todas las siguientes",
        "vm.serieModificada"    to "Toda la serie fue modificada",
        "vm.eliminarSoloEsta"   to "Cuota eliminada correctamente",
        "vm.eliminarSiguientes" to "Cuota eliminada y todas las siguientes",
        "vm.serieEliminada"     to "Toda la serie fue eliminada",

        // Home
        "home.buscar"           to "Buscar",
        "home.nuevoGasto"       to "Nuevo Gasto",
        "home.ingresoDelMes"    to "Ingreso del mes",
        "home.monto"            to "Monto",
        "home.ejMonto"          to "Ej: 150000",
        "home.eliminarGasto"    to "Eliminar gasto",
        "home.confirmarEliminar" to "¿Eliminás \"{0}\"?",
        "home.balanceDisponible" to "Balance Disponible",
        "home.de"               to "de {0}",
        "home.porcentajeConsumido" to "{0}% consumido",
        "home.gastoTotal"       to "Gasto Total",
        "home.enMovimientos"    to "En {0} movimientos",
        "home.gastosDeCuotas"   to "Gastos de Cuotas",
        "home.cuotasActivas"    to "{0} cuotas activas",
        "home.verDetalles"      to "Ver detalles →",
        "home.gastosPorCategoria" to "Gastos por Categoría",
        "home.verTodas"         to "Ver todas",
        "home.sinCategoria"     to "Sin categoría",
        "home.ultimosMovimientos" to "Últimos Movimientos",
        "home.verTodos"         to "Ver todos",
        "home.todosLosMovimientos" to "Todos los movimientos de {0} {1}",
        "home.registros"        to "{0} registros",
        "home.sinMovimientos"   to "Sin movimientos este mes",
        "home.editarIngreso"    to "Editar ingreso",

        // Buscador
        "buscador.placeholder"  to "Buscar gastos...",
        "buscador.limpiar"      to "Limpiar",
        "buscador.empezar"      to "Escribí para buscar en todos tus gastos",
        "buscador.sinResultados" to "Sin resultados para \"{0}\"",

        // Estadísticas
        "est.gastosPorCategorias" to "Gastos por Categorías",
        "est.detallesDeCuotas"  to "Detalles de Cuotas",
        "est.verTodasCuotas"    to "Ver todas",
        "est.cuotaDe"           to "Cuota {0} de {1}",
        "est.restante"          to "Restante: {0}",
        "est.balanceGastado"    to "Balance Gastado",
        "est.delIngresoMensual" to "del ingreso mensual",
        "est.promedioGastoMensual" to "Promedio Gasto Mensual (Año)",
        "est.datosAnioActual"   to "Datos del año actual",

        // Cuotas
        "cuotas.todasLasCuotas" to "Todas las Cuotas",
        "cuotas.sinCuotas"      to "Sin cuotas activas este mes",
        "cuotas.detalle"        to "Detalle de cuotas",
        "cuotas.seleccionada"   to "Cuota seleccionada",
        "cuotas.cronograma"     to "Cronograma completo",
        "cuotas.queEditar"      to "¿Qué cuotas editar?",
        "cuotas.queEliminar"    to "¿Qué cuotas eliminar?",

        // Categorías
        "categorias.titulo"     to "Categorías",
        "categorias.totalGastado" to "Total gastado",
        "categorias.ingreso"    to "Ingreso",
        "categorias.porcentajeDelTotal" to "{0}% del total",

        // Registro
        "registrar.tituloGasto" to "Registrar gasto",
        "registrar.tituloCuotas" to "Registrar en cuotas",
        "registrar.sinCategorias" to "Sin categorías",
        "registrar.sinCategoriasDesc" to "Necesitás crear al menos una categoría antes de registrar un gasto.",
        "registrar.monto"       to "Monto",
        "registrar.descripcion" to "Descripción",
        "registrar.categoria"   to "Categoría",
        "registrar.fecha"       to "Fecha",
        "registrar.seleccionarFecha" to "Seleccionar fecha",
        "registrar.pagoEnCuotas" to "Pago en cuotas",
        "registrar.divideMonto" to "Divide el monto automáticamente",
        "registrar.cantCuotas"  to "Cantidad de cuotas",
        "registrar.vistaPrevia" to "Vista previa",
        "registrar.esteMesCuota" to "Este mes: Cuota 1/{0}",
        "registrar.porCuotaTotal" to "{0} por cuota  ·  Total: {1}",
        "registrar.generarCuotas" to "Generar {0} cuotas",
        "registrar.guardarGasto" to "Guardar gasto",

        // Edición
        "editar.actualizado"    to "Gasto actualizado",
        "editar.tituloGasto"    to "Editar gasto",
        "editar.tituloCuota"    to "Editar cuota",
        "editar.aplicarCambio"  to "Aplicar cambio a:",
        "editar.soloEsta"       to "Solo esta cuota",
        "editar.estaYLasSiguientes" to "Esta y las siguientes",
        "editar.todaLaSerie"    to "Toda la serie",
        "editar.guardarCambios" to "Guardar cambios",

        // Mis Categorías
        "micat.categorias"      to "{0} categorías",
        "micat.nueva"           to "Nueva categoría",
        "micat.editar"          to "Editar categoría",
        "micat.editarAccion"    to "Editar",
        "micat.nombre"          to "Nombre",
        "micat.descripcionOpcional" to "Descripción (opcional)",
        "micat.color"           to "Color",
        "micat.cambiar"         to "Cambiar →",

        // ColorPicker
        "colorpicker.titulo"    to "Elegí un color",
        "colorpicker.brillo"    to "Brillo",
        "colorpicker.seleccionar" to "Seleccionar",

        // Navegación
        "nav.dashboard"         to "Dashboard",
        "nav.gastos"            to "Gastos",
        "nav.estadisticas"      to "Estadísticas",

        // Ajustes
        "ajustes.titulo"        to "Ajustes",
        "ajustes.general"       to "General",
        "ajustes.datos"         to "Datos",
        "ajustes.misCategorias" to "Mis Categorías",
        "ajustes.misCatSub"     to "Crear, editar y eliminar categorías",
        "ajustes.idioma"        to "Idioma",
        "ajustes.idiomaSub"     to "Configurar el idioma de la app",
        "ajustes.moneda"        to "Denominación de moneda",
        "ajustes.monedaSub"     to "Elegir la moneda a mostrar",
        "ajustes.importar"      to "Importar archivo xls",
        "ajustes.importarSub"   to "Restaurar datos desde un archivo",
        "ajustes.exportar"      to "Exportar como xls",
        "ajustes.exportarSub"   to "Guardar los datos en un archivo",
        "ajustes.ir"            to "Ir",
        // Exportar
        "exportar.titulo"       to "Exportar a Excel",
        "exportar.mesActual"    to "Mes actual",
        "exportar.anioActual"   to "Año actual",
        "exportar.todo"         to "Todo",
        "exportar.personalizado" to "Personalizado",
        "exportar.desde"        to "Desde",
        "exportar.hasta"        to "Hasta",
        "exportar.boton"        to "Exportar",
        "exportar.exito"        to "Exportación completada",
        "exportar.error"        to "No se pudo exportar",
        "exportar.sinDatos"     to "No hay gastos en este período",
        "importar.titulo"          to "Importar gastos",
        "importar.boton"           to "Seleccionar archivo",
        "importar.exito"           to "Se importaron {0} gastos ({1} ya existían, {2} categorías creadas, {3} ingresos)",
        "importar.error"           to "No se pudo importar",
        "importar.archivoNoValido" to "El archivo no es un xlsx válido",
        "importar.sinGastos"       to "El archivo no contiene gastos"
    )

    val meses = listOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )
}