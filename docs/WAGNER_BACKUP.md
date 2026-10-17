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

## Meta de ahorro (B3)

Presupuesto incluye una tarjeta reactiva de meta de ahorro y un diálogo para crear o editarla. Si todavía no hay una meta, la tarjeta permite configurarla sin mostrar importes ficticios. La meta se guarda mediante el repositorio existente; el caso de uso valida un nombre no vacío y un objetivo positivo, y reutiliza el ID de la meta activa al editar para que la fecha de creación se conserve.

El progreso no se almacena: se deriva de los ingresos y gastos acumulados de todos los tiempos, limitando el ahorro calculado a cero como mínimo. La barra visual se limita al 100 %, mientras que el importe ahorrado conserva su valor exacto y la meta se considera completa comparando centavos `Long`. Los borradores de meta y presupuesto permanecen independientes; una escritura activa bloquea envíos duplicados o el inicio de otra edición. Cancelar no escribe. Las notificaciones y alertas de transacciones siguen pendientes para B4 y B5.

## Alertas de presupuesto (B4 — dominio)

Al crear o editar un gasto, el caso de uso calcula los cruces de 80 % y 100 % usando el consumo del mes de la fecha del gasto. Un salto que cruce ambos umbrales produce solo la alerta del nivel más alto por presupuesto; una edición descuenta el gasto anterior únicamente cuando corresponde al mismo mes y ámbito. Los cálculos de umbral usan aritmética exacta para evitar desbordamientos.

La evaluación se prepara antes de guardar. Las alertas y el indicador de advertencia de evaluación se devuelven únicamente después de que la escritura financiera termina correctamente. Una falla ordinaria al leer presupuestos/consumos no bloquea el guardado; la cancelación sí se propaga. Los gastos sobre el límite siguen permitidos. Esta unidad cubre el dominio y los resultados del guardado; Snackbar, canal, permiso y notificación Android pertenecen a B5 y aún están pendientes.

## Pruebas y verificación

Las pruebas unitarias cubren balance frente a métricas mensuales, categorías, comparación mensual, límites de calendario, progreso de ahorro histórico, límites visuales, validación y preservación del ID/fecha al editar. Los tests del ViewModel cubren estados, reactividad, borradores independientes y bloqueo de escrituras duplicadas; las pruebas de pantalla cubren callbacks de presupuesto, meta y reintento.

Las pruebas/builds Gradle son responsabilidad del controlador principal y deben ejecutarse allí. Esta documentación no afirma verificación visual en dispositivo ni aprobación de las pruebas.
