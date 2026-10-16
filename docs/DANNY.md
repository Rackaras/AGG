# Trabajo de Danny — Sprint 2: Nueva transacción

Esta rama contiene S1 y el formulario S2 corregido según la maqueta. No implementa Historial (S4), Inicio, Presupuesto ni Más: esos destinos conservan sus pantallas provisionales. El formulario permite editar por ID, pero el acceso desde la lista corresponde a S4.

## Probar Nueva transacción

1. Abrir **+**, elegir **Ingreso** o **Gasto**, escribir el monto y seleccionar una categoría en el desplegable o los accesos rápidos.
2. Guardar. La fecha predeterminada es hoy y la descripción es opcional (registro rápido).
3. Para otra fecha, tocar el campo de fecha y usar el calendario.
4. Para una categoría propia, tocar **Nueva categoría**, escribir el nombre y elegir ícono/color. La categoría pertenece al tipo seleccionado.
5. Comprobar la persistencia al cerrar y abrir la app cuando se disponga de un dispositivo desbloqueado.

Se admiten punto o coma decimal y hasta dos decimales: `8.50` y `8,50` se guardan como `850` centavos. No se redondean importes con más decimales. El monto debe ser mayor que cero. Cambiar el tipo descarta una categoría incompatible. Una edición conserva ID y fecha salvo cambios explícitos.

## Diseño y navegación

Referencias: `images/02-nueva-transaccion.png` y `images/05-dialogo-nueva-categoria.png`. El formulario usa píldoras, monto destacado, campos suaves, selector completo y accesos rápidos, fecha localizada y botón de ancho completo. El diálogo conserva todos los íconos, la paleta de seis colores, cierre y acciones inferiores.

La barra conserva **Inicio · Historial · + · Presupuesto · Más** y permanece visible en el formulario. Tocar **+** con un formulario abierto no abre otra copia. Durante el guardado de una transacción o categoría se deshabilita la navegación; al salir del formulario se restablece. No se importó la implementación de Historial al conectar el callback de ocupación.

## Código para explicar

- `domain/usecase/AddTransactionUseCase.kt`: valida monto, categoría/tipo y existencia del registro antes de editar.
- `ui/transaction/TransactionFormViewModel.kt`: estado único, categorías reactivas, protección contra doble envío y eventos por Channel.
- `ui/transaction/TransactionFormScreen.kt`: Route observa el ViewModel con lifecycle; Screen recibe estado y callbacks.
- `ui/components/PrototypeControls.kt`: presentación reutilizable sin dependencias de Historial.
- `ui/navigation/AggNavHost.kt`: conserva pantallas provisionales de otros sprints; conecta solamente el formulario y su callback de ocupación.

## Verificación

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

Las pruebas instrumentadas requieren teléfono/emulador desbloqueado y con pantalla activa. Compilarlas no equivale a ejecutarlas. Si aparece `No compose hierarchies found`, comprobar primero que el host esté visible, no detrás del bloqueo. No se da por cerrada la aceptación visual ni la persistencia real solo por compilar.

## Integración en orden

1. PR S1 (`feature/s1-data-layer`) hacia `main`, revisado por Wagner.
2. PR S2 corregido (`feature/s2-transaction-form`) sobre S1 mientras esté pendiente; tras integrar S1, cambiar su base a `main` y obtener revisión de Raúl.
3. Integrar S4 por separado después de S2. No usar la rama S4 para saltar esta separación.
