# AGG — Finanzas simples, grandes decisiones

AGG es una aplicación móvil Android que permite registrar, categorizar y visualizar ingresos y gastos diarios, ayudando a mantener el control del presupuesto personal de forma simple y visual. Funciona 100% offline.

Proyecto de la asignatura **Desarrollo de Aplicaciones Móviles** (Trabajo Autónomo, segunda etapa).

**Integrantes:** Danny Espinoza · Raúl Cedeño · Wagner Zambrano

📋 **[Plan de desarrollo, reparto de sprints y guía para clonar el repo](docs/PLAN.md)**

## Stack técnico

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3, Navigation Compose (rutas type-safe) |
| Presentación | MVVM, `ViewModel` + `StateFlow<UiState>`, `collectAsStateWithLifecycle()` |
| Dominio | Use cases (`operator fun invoke`) |
| Datos | Room (Single Source of Truth, offline-first), `Flow<T>` reactivo, `suspend` para escrituras |
| DI | Hilt |

## Arquitectura

```
ui (Route stateful → Screen stateless) → ViewModel → UseCase → Repository (interfaz)
                                                                  ↓
                                                        RepositoryImpl → DAO → Room
```

- Los composables hijos **nunca** reciben el ViewModel: solo estado inmutable y lambdas.
- Cada pantalla expone un único `StateFlow` modelado como `Loading / Success / Error`.
- Los repositorios exponen modelos de dominio; las entidades de Room no salen de la capa de datos.

## Requisitos funcionales

RF01–RF12 de la propuesta: registro de ingresos/egresos, balance, categorías (predefinidas y personalizadas), gráfico por categoría, presupuesto mensual (general y por categoría) con alertas, historial filtrable, edición/eliminación, resumen mensual comparativo, meta de ahorro y registro rápido.

## Cómo ejecutar

1. Abrir el proyecto en Android Studio.
2. Sincronizar Gradle.
3. Ejecutar la configuración `app` en un emulador o dispositivo (Android 8.0+ / API 26).

```bash
./gradlew assembleDebug          # compilar
./gradlew testDebugUnitTest      # pruebas unitarias
./gradlew connectedDebugAndroidTest  # pruebas instrumentadas (Room)
```

## Flujo de trabajo Git

- `main` siempre compila.
- Ramas por funcionalidad: `feature/<sprint>-<descripcion>` (ej. `feature/s2-transaction-form`).
- Commits con [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
- Integración mediante Pull Requests revisados por otro integrante.
