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

    /** Respaldo privado cifrado en el Google Drive de la usuaria. */
    const val BACKUP = "backup_settings"

    // ── Módulo BONDS ──────────────────────────────────────────────────────────
    /** Pantalla principal de Bonds (Home). */
    const val BONDS_HOME = "bonds_home"

    /** Vinculación y Privacidad (QR, código y toggles de consentimiento). */
    const val BONDS_PAIRING = "bonds_pairing"

    /** Experiencia inmersiva de envío de detalles/gestos afectivos. */
    const val BONDS_SEND_GESTURE = "bonds_send_gesture/{action}?linkId={linkId}"

    /** Calendario Compartido de Bonds con vista multivínculo. */
    const val BONDS_SHARED_CALENDAR = "bonds_shared_calendar"

    /** Detalle individual de una persona conectada en el círculo. */
    const val BONDS_MEMBER_DETAIL = "bonds_member_detail/{linkId}"
}
