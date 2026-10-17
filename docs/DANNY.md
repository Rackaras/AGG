# Trabajo de Danny — Transacciones e historial (S2 + S4)

Esta rama conserva el formulario de transacciones de S2 y lo integra con el Historial de S4. La sincronización también mantiene Inicio y Presupuesto/meta/avisos de main; esas pantallas y comportamientos no se redefinen aquí. Más (S6) permanece fuera de alcance.

## Nueva transacción y edición (S2)

1. Abrir **+**, elegir **Ingreso** o **Gasto**, escribir el monto y seleccionar una categoría en el desplegable o en los accesos rápidos.
2. Guardar. La fecha predeterminada es hoy y la descripción es opcional (registro rápido).
3. Para otra fecha, tocar el campo de fecha y usar el calendario.
4. Para una categoría propia, tocar **Nueva categoría**, escribir el nombre y elegir ícono/color. La categoría pertenece al tipo seleccionado.

Se admiten punto o coma decimal y hasta dos decimales: `8.50` y `8,50` se guardan como `850` centavos. No se redondean importes con más decimales. El monto debe ser mayor que cero. Cambiar el tipo descarta una categoría incompatible. Una edición conserva ID y fecha salvo cambios explícitos.

El formulario y el diálogo de categoría siguen las referencias `images/02-nueva-transaccion.png` y `images/05-dialogo-nueva-categoria.png`. La barra inferior **Inicio · Historial · + · Presupuesto · Más** permanece visible durante la edición. **+** no duplica un formulario abierto; mientras se guarda una transacción o categoría, la navegación queda deshabilitada. Al terminar o salir se restablece.

## Historial (S4)

1. Abrir **Historial**: los registros aparecen agrupados por mes, del más reciente al más antiguo.
2. Usar **Todos / Ingresos / Egresos** y los filtros **Fecha** y **Categoría**. El rango incluye ambos días; **Limpiar filtros** vuelve a mostrar todo.
3. Desde el menú de un registro, elegir **Editar**. Se abre el formulario existente; al guardar y volver, la lista se actualiza desde Room conservando los filtros.
4. **Eliminar** oculta temporalmente el registro y ofrece **Deshacer**. Deshacer lo vuelve a mostrar sin modificar la base de datos.
5. Cerrar el aviso o dejar que termine confirma la eliminación. Si falla, el registro reaparece y se informa el error. No se inicia otra eliminación mientras haya una pendiente.

La presentación sigue `images/03-historial.png`: pestañas en píldoras, filas compactas con separadores y círculos pastel para íconos. La lupa abre la búsqueda existente por categoría y ajustes abre el rango de fechas; no se agregó búsqueda libre ni botones sin acción. Las fechas usan formato corto y distinguen hoy. Cambiar de pantalla o mandar la app al fondo no confirma el borrado: el aviso se ofrece de nuevo al volver si el ViewModel sigue vivo. Si termina el proceso antes de confirmar, el registro permanece guardado.

## Integración y límites

`AggNavHost` conserva una única ruta consumidora de los eventos del formulario, conecta History y el formulario y retiene las rutas reales de Inicio y Presupuesto. El callback opcional de notificación mantiene su valor por defecto; una falla en la entrega de notificaciones no vuelve a guardar ni impide regresar tras un guardado confirmado. La raíz conserva la barra, el estado ocupado, el Snackbar de presupuesto y el launcher de permisos. No se rediseñan Inicio o Presupuesto y no se implementa S6.

Los fakes compartidos conservan filtros, errores de lectura/eliminación/deshacer, totales por categoría/mes y el hook previo a guardar.

## Verificación

En el contexto integrado S4 + main, la suite completa finalizó correctamente con 82 pruebas unitarias, sin fallos ni omisiones. También compilaron los APK de la app y de pruebas; la revisión independiente de fuente no encontró bloqueos en la conciliación.

Las pruebas instrumentadas solo se compilaron, no se ejecutaron. No se afirma una nueva aceptación visual, persistencia real ni prueba en teléfono de esta combinación.
