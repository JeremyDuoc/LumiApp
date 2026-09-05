package com.jeremy.lumi.ui.screens.partner

import androidx.compose.runtime.Composable
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.PartnerLink

/**
 * Envoltorio de compatibilidad para HomeScreen, que muestra esta vista en
 * dos contextos: el home completo del rol Acompañante y el sheet de vínculo
 * de las usuarias normales.
 *
 * La implementación real es PartnerStoryScreen (dashboard tipo historias).
 * [onUnlink] ya no se usa aquí: desvincular vive en el Hub del Modo Pareja,
 * con su diálogo de confirmación.
 */
@Composable
fun ObserverScreen(
    link: PartnerLink,
    currentUid: String,
    uiState: PartnerUiState,
    onSendCareAction: (CareAction) -> Unit,
    onUnlink: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    PartnerStoryScreen(
        link = link,
        myUid = currentUid,
        uiState = uiState,
        onSendCareAction = onSendCareAction,
        onClose = onBack
    )
}
