# Trabajo de Danny — Nueva transacción e historial

La implementación usa la base de `feature/s1-data-layer`. El Sprint 2 agrega el formulario de transacciones; el Sprint 4 lo utiliza para editar desde el historial. No incluye Inicio, Presupuesto ni las pantallas de Más.

## Probar Nueva transacción (S2)

1. Abrir **+**, elegir **Ingreso** o **Gasto**, escribir el monto y seleccionar una categoría en el desplegable o en los accesos rápidos.
2. Guardar. La fecha predeterminada es hoy y la descripción es opcional (registro rápido).
3. Para otra fecha, tocar el botón de fecha y usar el calendario.
4. Para una categoría propia, tocar **Nueva categoría**, escribir el nombre y elegir ícono/color. La categoría pertenece al tipo de transacción seleccionado.
5. Cerrar y abrir la app y comprobar la persistencia cuando se disponga de un dispositivo.

Se admiten punto o coma decimal, con hasta dos decimales: `8.50` y `8,50` se guardan como `850` centavos. No se redondean importes con más decimales. El monto debe ser mayor que cero. Cambiar el tipo descarta una categoría incompatible. Una edición conserva su ID y su fecha salvo que se cambien expresamente los campos.

## Diseño del formulario

El formulario y el diálogo de categoría usan como referencia `images/02-nueva-transaccion.png` y `images/05-dialogo-nueva-categoria.png`: selección en píldoras, monto destacado, campos suaves, selector completo y cuadrícula de accesos rápidos, fecha localizada y guardado de ancho completo. El diálogo conserva todos los íconos disponibles, añade cierre superior y presenta la paleta de seis colores y las acciones inferiores del prototipo.

Estos cambios no alteran el almacenamiento en centavos, las validaciones ni el flujo de edición. La fidelidad visual final y el comportamiento en pantallas pequeñas deben comprobarse en un dispositivo; compilar no demuestra igualdad con la imagen.

La barra inferior conserva **Inicio · Historial · + · Presupuesto · Más**, con el botón central elevado, y también aparece durante la creación/edición como en la maqueta. Tocar **+** cuando el formulario ya está abierto no crea otra copia. Mientras se guarda una transacción o categoría se deshabilita la navegación para no abandonar una escritura pendiente; vuelve a habilitarse al terminar/salir del formulario. Las pantallas de Inicio y Presupuesto no se rediseñaron.

## Probar Historial (S4)

1. Abrir **Historial**: aparecen los registros agrupados por mes, del más reciente al más antiguo.
2. Usar **Todos / Ingresos / Egresos** y los filtros **Fecha** y **Categoría**. El rango incluye ambos días; **Limpiar filtros** vuelve a mostrar todo.
3. Abrir el menú de un registro y tocar **Editar**. Se reutiliza el formulario S2; al guardar y volver, la lista se actualiza desde Room conservando los filtros.
4. Tocar **Eliminar**: el registro se oculta temporalmente y aparece **Deshacer**. Deshacer lo vuelve a mostrar sin modificar la base de datos.
5. Cerrar el aviso o dejar que termine confirma la eliminación. Si falla, el registro reaparece y se informa el error. Mientras haya un aviso pendiente, no se permite iniciar otra eliminación.

La presentación sigue `images/03-historial.png`: pestañas en píldoras, filas compactas con separadores y círculos pastel para los íconos. La lupa abre la búsqueda por categoría existente y el botón de ajustes abre el rango de fechas; no se agregó búsqueda libre ni botones sin acción. La fecha de cada fila usa formato corto y distingue hoy.

Girar el teléfono, cambiar de pantalla o mandar la app al fondo no confirma el borrado: el aviso se ofrece de nuevo al volver si el ViewModel sigue vivo. Si el proceso termina antes de confirmar, el registro permanece guardado. No se intenta restaurar un registro borrado mediante la operación de edición.

## Código para explicar en el pitch

- `domain/usecase/AddTransactionUseCase.kt`: valida monto, categoría/tipo y existencia del registro al editar antes de escribir por el repositorio.
- `ui/transaction/TransactionFormViewModel.kt`: estado único, categorías reactivas, protección contra doble envío y eventos de navegación por Channel.
- `ui/transaction/TransactionFormScreen.kt`: Route obtiene el ViewModel y observa el estado con lifecycle; Screen recibe estado y callbacks, sin acceso a Room.
- `ui/navigation/AggNavHost.kt`: integra Historial y el formulario con las rutas tipadas existentes; mantiene las otras pantallas y la animación original.
- `ui/history/HistoryViewModel.kt`: combina registros, categorías, filtros y eliminación pendiente; expone un único estado y confirma el borrado a través del repositorio existente.
- `ui/history/HistoryRoute.kt`: observa el estado con lifecycle, maneja los diálogos y el aviso Deshacer, y entrega las acciones al ViewModel.
- `ui/history/HistoryScreen.kt`: lista agrupada por mes, filtros y menús; recibe datos/callbacks sin ViewModel ni DAO.

## Verificación

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

El segundo comando requiere un emulador o teléfono conectado. Compilar los tests instrumentados no equivale a ejecutarlos. Las pruebas unitarias usan repositorios falsos aislados; las pruebas instrumentadas existentes de Room prueban la capa de datos.
