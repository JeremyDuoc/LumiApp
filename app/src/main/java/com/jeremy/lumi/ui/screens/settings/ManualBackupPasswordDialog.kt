package com.jeremy.lumi.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jeremy.lumi.R

/**
 * Password is ephemeral Compose state, never rememberSaveable, persisted, or logged.
 * The caller must clear() the returned CharArray when the storage operation ends.
 */
@Composable
internal fun ManualBackupPasswordDialog(
    exporting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit
) {
    var password by remember(exporting) { mutableStateOf("") }
    var repeated by remember(exporting) { mutableStateOf("") }
    val valid = password.isNotBlank() && (!exporting || (password.length >= 12 && password == repeated))
    val dismiss: () -> Unit = {
        password = ""
        repeated = ""
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Text(stringResource(if (exporting) R.string.manual_backup_export_title else R.string.manual_backup_restore_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (exporting) R.string.manual_backup_export_explain else R.string.manual_backup_restore_explain))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.manual_backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (exporting) {
                    OutlinedTextField(
                        value = repeated,
                        onValueChange = { repeated = it },
                        label = { Text(stringResource(R.string.manual_backup_confirm_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Text(stringResource(R.string.manual_backup_password_guidance))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val chars = password.toCharArray()
                    password = ""
                    repeated = ""
                    onConfirm(chars)
                }
            ) {
                Text(stringResource(if (exporting) R.string.manual_backup_export_action else R.string.manual_backup_restore_action))
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) { Text(stringResource(R.string.backup_action_cancel)) }
        }
    )
}
