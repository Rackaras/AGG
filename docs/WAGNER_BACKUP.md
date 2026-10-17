# Respaldo local: Inicio (B1)

Este documento registra el respaldo local de la pantalla Inicio correspondiente a B1 (S3, S7 y S8 de `odd/tasks/wagner-backup.md`). Es una implementación de respaldo, no una publicación ni una atribución de trabajo a otra persona.

## Alcance

Inicio muestra datos reales observados de `TransactionRepository`: balance acumulado (ingresos de todos los tiempos menos gastos de todos los tiempos), ingresos y gastos del mes calendario actual, distribución de gastos por categoría y comparación con el gasto del mes anterior. Los importes y cálculos monetarios permanecen en centavos `Long`; solo la proporción visual del gráfico usa valores de punto flotante. Si el mes anterior no tiene gasto, la interfaz comunica que no hay referencia para comparar.

El `ObserveDashboardUseCase` calcula los límites de mes usando un `Clock` inyectado. Sus flujos son de solo lectura; no crea transacciones ni datos iniciales. `HomeViewModel` expone Loading, Success o Error mediante un único `StateFlow`; el botón de reintento vuelve a suscribirse a la lectura.

El acceso «Ver más» navega al destino existente Historial, que continúa siendo su placeholder. No se agregan funciones de historial, presupuestos, metas, alertas o notificaciones en B1.

## Pruebas y verificación

Las pruebas unitarias cubren balance frente a métricas mensuales, categorías, comparación mensual, límites de calendario, actualizaciones reactivas, estados del ViewModel y ausencia de escrituras. Las pruebas de pantalla cubren métricas visibles y callbacks de «Ver más» y «Reintentar».

Las pruebas/builds Gradle son responsabilidad del controlador principal y deben ejecutarse allí. Esta documentación no afirma verificación visual en dispositivo ni aprobación de las pruebas.
