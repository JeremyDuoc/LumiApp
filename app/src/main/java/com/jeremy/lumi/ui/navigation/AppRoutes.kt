package com.jeremy.lumi.ui.navigation

/**
 * Objeto que centraliza todas las rutas de navegación de la app.
 *
 * Usar constantes tipadas en lugar de literales dispersos evita typos y
 * facilita el refactor.
 */
object AppRoutes {
    /** Pantalla de bienvenida animada. Es el `startDestination` del grafo. */
    const val SPLASH = "splash"

    /** Contenedor principal con la barra inferior y las pestañas. */
    const val MAIN = "main"

    /** Pantalla de gráficos e insights del ciclo. */
    const val INSIGHTS = "insights"

    /** Flujo de primera vez — se muestra solo si onboarding_completed == false. */
    const val ONBOARDING = "onboarding"

    /** Pantalla de vinculación con deep link. */
    const val PARTNER = "partner?code={code}"

    /** Hub central de conexiones (Modo Pareja rediseñado). */
    const val PARTNER_HUB = "partner_hub"
}
