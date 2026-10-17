> **Aviso de alcance:** Esta rama contiene únicamente S5 (Presupuestos). El dashboard pertenece a la función separada S3 (`feature/s3-dashboard`); las secciones de esta guía corresponden a sus respectivas ramas.

# Respaldo local de Wagner — S3 y S5

Este documento describe las funcionalidades del respaldo local y sus límites de verificación. Los apartados indican a qué rama y unidad pertenece cada función. No es una publicación ni una atribución de trabajo a otra persona.

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

El progreso no se almacena: se deriva de los ingresos y gastos acumulados de todos los tiempos, limitando el ahorro calculado a cero como mínimo. La barra visual se limita al 100 %, mientras que el importe ahorrado conserva su valor exacto y la meta se considera completa comparando centavos `Long`. Los borradores de meta y presupuesto permanecen independientes; una escritura activa bloquea envíos duplicados o el inicio de otra edición. Cancelar no escribe. Las alertas y notificaciones de transacciones se describen en B4 y B5.

## Alertas de presupuesto (B4 — dominio)

Al crear o editar un gasto, el caso de uso calcula los cruces de 80 % y 100 % usando el consumo del mes de la fecha del gasto. Un salto que cruce ambos umbrales produce solo la alerta del nivel más alto por presupuesto; una edición descuenta el gasto anterior únicamente cuando corresponde al mismo mes y ámbito. Los cálculos de umbral usan aritmética exacta para evitar desbordamientos.

La evaluación se prepara antes de guardar. Las alertas y el indicador de advertencia de evaluación se devuelven únicamente después de que la escritura financiera termina correctamente. Una falla ordinaria al leer presupuestos/consumos no bloquea el guardado; la cancelación sí se propaga. Los gastos sobre el límite siguen permitidos.

## Avisos y notificaciones (B5)

Un guardado confirmado envía sus alertas y advertencia de evaluación en el evento de la ruta. La aplicación raíz —que permanece activa al cerrar el formulario— muestra un aviso descartable en la app para cualquier cruce o fallo de evaluación. Este aviso no depende del permiso Android ni espera el resultado del sistema; la navegación y el guardado no se bloquean por la entrega opcional.

La política de notificación está aislada en `BudgetNotificationCoordinator` y una interfaz de plataforma comprobable con fakes. En Android 13 o posterior, solo un cruce real consulta el permiso y puede abrir el diálogo normal de Activity Result; un fallo de evaluación sin cruce no solicita permiso. La app vuelve a comprobar el permiso y el estado de notificaciones antes de publicar. Versiones anteriores no usan permiso en tiempo de ejecución, pero respetan si las notificaciones están desactivadas. El adaptador crea el canal al publicar, usa un icono pequeño dedicado y un `PendingIntent` explícito e inmutable a `MainActivity`.

La denegación, desactivación o excepción de consulta/solicitud/publicación conserva el guardado y el aviso dentro de la app. No se reintenta la operación financiera ni se conceden permisos automáticamente. Los mensajes evitan atribuir el presupuesto a una categoría que no está disponible en los metadatos de alerta.

## Pruebas y verificación

Las pruebas unitarias cubren balance frente a métricas mensuales, categorías, comparación mensual, límites de calendario, progreso de ahorro histórico, límites visuales, validación y preservación del ID/fecha al editar. Los tests del ViewModel cubren estados, reactividad, borradores independientes y bloqueo de escrituras duplicadas; las pruebas de pantalla cubren callbacks de presupuesto, meta y reintento. B5 añade pruebas puras del coordinador para avisos, permiso, estado habilitado, callback de permiso y fallos de plataforma, además de comprobar el reenvío único de metadatos guardados.

En la rama S5, la suite unitaria completa y la compilación de los APK de la app y de pruebas finalizaron correctamente: 64 pruebas unitarias, sin fallos, errores ni omisiones. La revisión independiente de fuente no encontró bloqueos en B5.

Las pruebas instrumentadas solo se compilaron: no se ejecutaron. Quedan pendientes la verificación visual, la persistencia en un entorno Android aislado y el flujo real de permisos/notificaciones. Las pruebas puras y la revisión de fuente no sustituyen esa aceptación. No se instaló la app en el teléfono ni se concedieron permisos automáticamente.
