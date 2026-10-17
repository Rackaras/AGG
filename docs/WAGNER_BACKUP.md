> **Aviso de alcance:** Esta rama contiene únicamente S5 (Presupuestos). El dashboard pertenece a la función separada S3 (`feature/s3-dashboard`); las secciones de esta guía corresponden a sus respectivas ramas.

# Respaldo local de Wagner — S3 y S5

Este documento registra las unidades implementadas del respaldo descrito en `odd/tasks/wagner-backup.md`. Los apartados indican su alcance; no implican que las unidades futuras estén terminadas. No es una publicación ni una atribución de trabajo a otra persona.

## Alcance

Inicio muestra datos reales observados de `TransactionRepository`: balance acumulado (ingresos de todos los tiempos menos gastos de todos los tiempos), ingresos y gastos del mes calendario actual, distribución de gastos por categoría y comparación con el gasto del mes anterior. Los importes y cálculos monetarios permanecen en centavos `Long`; solo la proporción visual del gráfico usa valores de punto flotante. Si el mes anterior no tiene gasto, la interfaz comunica que no hay referencia para comparar.

El `ObserveDashboardUseCase` calcula los límites de mes usando un `Clock` inyectado. Sus flujos son de solo lectura; no crea transacciones ni datos iniciales. `HomeViewModel` expone Loading, Success o Error mediante un único `StateFlow`; el botón de reintento vuelve a suscribirse a la lectura.

El acceso «Ver más» navega al destino existente Historial, que continúa siendo su placeholder. No se agregan funciones de historial, presupuestos, metas, alertas o notificaciones en B1.

## Presupuestos (B2)

La pantalla Presupuesto observa el límite general, los límites por categoría y el consumo del mes calendario actual. Permite configurar o editar un límite general y seleccionar categorías de egreso, incluidas las que todavía no tienen presupuesto. La interfaz muestra importes, disponible y progreso sin crear valores de ejemplo en la base de datos.

Al guardar, el monto se valida como centavos positivos con el parser existente. El caso de uso comprueba que la categoría exista y sea de egreso antes de llamar al setter correspondiente del repositorio; la persistencia conserva sus reglas de actualización e ID. Los rechazos no escriben. Durante el guardado se evita el doble envío y, si falla, se conserva el borrador para corregirlo o reintentar.

Las metas y alertas/notificaciones todavía no forman parte de B2; corresponden a B3–B5. Las pruebas usan repositorios falsos aislados. No se instala este respaldo en el teléfono ni se cambian sus datos sin autorización específica.

## Pruebas y verificación

Las pruebas unitarias cubren balance frente a métricas mensuales, categorías, comparación mensual, límites de calendario, actualizaciones reactivas, estados del ViewModel y ausencia de escrituras. Las pruebas de pantalla cubren métricas visibles y callbacks de «Ver más» y «Reintentar».

Las pruebas/builds Gradle son responsabilidad del controlador principal y deben ejecutarse allí. Esta documentación no afirma verificación visual en dispositivo ni aprobación de las pruebas.
