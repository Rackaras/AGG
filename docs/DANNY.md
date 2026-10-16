# Trabajo de Danny — Nueva transacción e historial

La implementación usa la base de `feature/s1-data-layer`. El Sprint 2 agrega el formulario de transacciones; el Sprint 4 lo utiliza para editar desde el historial. No incluye Inicio, Presupuesto ni las pantallas de Más.

## Probar Nueva transacción (S2)

1. Abrir **+**, elegir Ingreso o Egreso, escribir el monto y seleccionar una categoría.
2. Guardar. La fecha predeterminada es hoy y la descripción es opcional (registro rápido).
3. Para otra fecha, tocar el botón de fecha y usar el calendario.
4. Para una categoría propia, tocar **Nueva categoría**, escribir el nombre y elegir ícono/color. La categoría pertenece al tipo de transacción seleccionado.
5. Cerrar y abrir la app y comprobar la persistencia cuando se disponga de un dispositivo.

Se admiten punto o coma decimal, con hasta dos decimales: `8.50` y `8,50` se guardan como `850` centavos. No se redondean importes con más decimales. El monto debe ser mayor que cero. Cambiar el tipo descarta una categoría incompatible. Una edición conserva su ID y su fecha salvo que se cambien expresamente los campos.

## Código para explicar en el pitch

- `domain/usecase/AddTransactionUseCase.kt`: valida monto, categoría/tipo y existencia del registro al editar antes de escribir por el repositorio.
- `ui/transaction/TransactionFormViewModel.kt`: estado único, categorías reactivas, protección contra doble envío y eventos de navegación por Channel.
- `ui/transaction/TransactionFormScreen.kt`: Route obtiene el ViewModel y observa el estado con lifecycle; Screen recibe estado y callbacks, sin acceso a Room.
- `ui/navigation/AggNavHost.kt`: integra el formulario con la ruta tipada existente y mantiene la animación original.

## Verificación

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

El segundo comando requiere un emulador o teléfono conectado. Compilar los tests instrumentados no equivale a ejecutarlos. Las pruebas unitarias usan repositorios falsos aislados; las pruebas instrumentadas existentes de Room prueban la capa de datos.
