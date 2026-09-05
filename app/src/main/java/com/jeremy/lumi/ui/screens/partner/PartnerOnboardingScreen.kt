package com.jeremy.lumi.ui.screens.partner

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.LinkType

/**
 * Onboarding del Modo Pareja: crear una invitación o unirse con un código
 * (también vía deep link lumi://pair/{code}).
 *
 * Flujo paso a paso con explicación clara de privacidad:
 *  CREAR → tipo de vínculo → privacidad → identidad (nombre + avatar) → código
 *  UNIRSE → código → identidad → éxito
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerOnboardingScreen(
    initialCode: String? = null,
    onNavigateBack: () -> Unit = {},
    onFinished: () -> Unit = {},
    viewModel: PartnerOnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialCode) {
        if (!initialCode.isNullOrBlank()) viewModel.startJoinWithCode(initialCode)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.pm_onboarding_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (!viewModel.goBack()) onNavigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.pm_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Barra de progreso del asistente
            val progress = viewModel.progressFraction()
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            AnimatedContent(
                targetState = uiState.step,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                },
                label = "pasos_onboarding"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (step) {
                        OnboardingStep.CHOOSER -> ChooserStep(viewModel)
                        OnboardingStep.ROLE -> RoleStep(viewModel, uiState)
                        OnboardingStep.PRIVACY -> PrivacyStep(viewModel, uiState)
                        OnboardingStep.IDENTITY -> IdentityStep(viewModel, uiState)
                        OnboardingStep.CODE_RESULT -> CodeResultStep(uiState, onFinished)
                        OnboardingStep.JOIN_CODE -> JoinCodeStep(viewModel, uiState)
                        OnboardingStep.JOIN_SUCCESS -> JoinSuccessStep(uiState, onFinished)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Paso 0: elegir camino
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChooserStep(viewModel: PartnerOnboardingViewModel) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.pm_chooser_title),
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.pm_chooser_subtitle),
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
    )
    Spacer(Modifier.height(28.dp))

    BigChoiceCard(
        icon = Icons.Rounded.PersonAdd,
        title = stringResource(R.string.pm_chooser_create_title),
        description = stringResource(R.string.pm_chooser_create_desc),
        gradient = listOf(Color(0xFF6C5CE7), Color(0xFFA18CD1)),
        onClick = { viewModel.startCreate() }
    )
    Spacer(Modifier.height(16.dp))
    BigChoiceCard(
        icon = Icons.Rounded.Key,
        title = stringResource(R.string.pm_chooser_join_title),
        description = stringResource(R.string.pm_chooser_join_desc),
        gradient = listOf(Color(0xFF2F80ED), Color(0xFF48C6EF)),
        onClick = { viewModel.startJoin() }
    )
}

@Composable
private fun BigChoiceCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(gradient))
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Paso: tipo de vínculo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RoleStep(viewModel: PartnerOnboardingViewModel, uiState: PartnerOnboardingUiState) {
    StepHeader(
        title = stringResource(R.string.pm_role_title),
        subtitle = stringResource(R.string.pm_role_subtitle)
    )

    RoleCard(
        icon = Icons.Rounded.Visibility,
        title = stringResource(R.string.pm_role_observer_title),
        description = stringResource(R.string.pm_role_observer_desc),
        selected = uiState.linkType == LinkType.OBSERVER,
        onClick = { viewModel.setLinkType(LinkType.OBSERVER) }
    )
    Spacer(Modifier.height(14.dp))
    RoleCard(
        icon = Icons.Rounded.Sync,
        title = stringResource(R.string.pm_role_sync_title),
        description = stringResource(R.string.pm_role_sync_desc),
        selected = uiState.linkType == LinkType.CYCLE_SYNC,
        onClick = { viewModel.setLinkType(LinkType.CYCLE_SYNC) }
    )

    Spacer(Modifier.height(28.dp))
    PrimaryButton(text = stringResource(R.string.pm_next), onClick = { viewModel.next() })
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Paso: privacidad granular
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrivacyStep(viewModel: PartnerOnboardingViewModel, uiState: PartnerOnboardingUiState) {
    StepHeader(
        title = stringResource(R.string.pm_privacy_title),
        subtitle = stringResource(R.string.pm_privacy_subtitle)
    )

    PrivacyToggleRow(
        icon = Icons.Rounded.DonutLarge,
        title = stringResource(R.string.pm_privacy_phase),
        description = stringResource(R.string.pm_privacy_phase_desc),
        checked = uiState.privacy.phase,
        onChange = { viewModel.setPrivacy(uiState.privacy.copy(phase = it)) }
    )
    PrivacyToggleRow(
        icon = Icons.Rounded.Mood,
        title = stringResource(R.string.pm_privacy_mood),
        description = stringResource(R.string.pm_privacy_mood_desc),
        checked = uiState.privacy.mood,
        onChange = { viewModel.setPrivacy(uiState.privacy.copy(mood = it)) }
    )
    PrivacyToggleRow(
        icon = Icons.Rounded.Healing,
        title = stringResource(R.string.pm_privacy_symptoms),
        description = stringResource(R.string.pm_privacy_symptoms_desc),
        checked = uiState.privacy.symptoms,
        onChange = { viewModel.setPrivacy(uiState.privacy.copy(symptoms = it)) }
    )
    PrivacyToggleRow(
        icon = Icons.Rounded.Insights,
        title = stringResource(R.string.pm_privacy_predictions),
        description = stringResource(R.string.pm_privacy_predictions_desc),
        checked = uiState.privacy.predictions,
        onChange = { viewModel.setPrivacy(uiState.privacy.copy(predictions = it)) }
    )

    Spacer(Modifier.height(18.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.pm_privacy_note),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    Spacer(Modifier.height(24.dp))
    PrimaryButton(text = stringResource(R.string.pm_next), onClick = { viewModel.next() })
}

@Composable
private fun PrivacyToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Paso: identidad (nombre + avatar)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdentityStep(viewModel: PartnerOnboardingViewModel, uiState: PartnerOnboardingUiState) {
    StepHeader(
        title = stringResource(R.string.pm_identity_title),
        subtitle = stringResource(R.string.pm_identity_subtitle)
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PartnerAvatarView(
            avatarId = uiState.avatarId,
            fallbackName = uiState.displayName,
            size = 84.dp
        )
    }
    Spacer(Modifier.height(20.dp))

    OutlinedTextField(
        value = uiState.displayName,
        onValueChange = viewModel::setDisplayName,
        label = { Text(stringResource(R.string.pm_identity_name_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    if (uiState.mode == OnboardingMode.CREATE) {
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = uiState.relationLabel,
            onValueChange = viewModel::setRelationLabel,
            label = { Text(stringResource(R.string.pm_identity_relation_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(22.dp))
    AvatarPickerGrid(
        selectedId = uiState.avatarId,
        onSelect = viewModel::setAvatar
    )

    Spacer(Modifier.height(26.dp))
    uiState.errorRes?.let { ErrorText(it) }
    PrimaryButton(
        text = stringResource(
            if (uiState.mode == OnboardingMode.CREATE) R.string.pm_create_link_cta else R.string.pm_join_cta
        ),
        loading = uiState.isWorking,
        onClick = { viewModel.submit() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Paso: resultado con código (camino CREAR)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CodeResultStep(uiState: PartnerOnboardingUiState, onFinished: () -> Unit) {
    val context = LocalContext.current
    val code = uiState.generatedCode.orEmpty()
    val shareText = stringResource(R.string.pm_share_link_text, "https://lumi.app/pair/$code")

    StepHeader(
        title = stringResource(R.string.pm_code_result_title),
        subtitle = stringResource(R.string.pm_code_result_subtitle)
    )

    // Código en cajitas, estilo PIN premium
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        code.forEach { digit ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = digit.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(26.dp))
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, null))
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.pm_share_link_button), fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(12.dp))
    androidx.compose.material3.TextButton(
        onClick = onFinished,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.pm_done))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Paso: introducir código (camino UNIRSE)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JoinCodeStep(viewModel: PartnerOnboardingViewModel, uiState: PartnerOnboardingUiState) {
    StepHeader(
        title = stringResource(R.string.pm_join_code_title),
        subtitle = stringResource(R.string.pm_join_code_subtitle)
    )

    OutlinedTextField(
        value = uiState.code,
        onValueChange = viewModel::setCode,
        label = { Text(stringResource(R.string.pm_join_code_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 8.sp,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(24.dp))
    uiState.errorRes?.let { ErrorText(it) }
    PrimaryButton(
        text = stringResource(R.string.pm_next),
        enabled = uiState.code.length == 6,
        onClick = { viewModel.next() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Paso: éxito al unirse
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JoinSuccessStep(uiState: PartnerOnboardingUiState, onFinished: () -> Unit) {
    Spacer(Modifier.height(28.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PartnerAvatarView(
            avatarId = uiState.avatarId,
            fallbackName = uiState.displayName,
            size = 96.dp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.pm_join_success_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.pm_join_success_subtitle),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(30.dp))
        PrimaryButton(text = stringResource(R.string.pm_go_to_hub), onClick = onFinished)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Piezas comunes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorText(resId: Int) {
    Text(
        text = stringResource(resId),
        color = MaterialTheme.colorScheme.error,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}
