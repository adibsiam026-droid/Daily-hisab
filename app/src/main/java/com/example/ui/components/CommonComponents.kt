package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Formatters
import com.example.core.i18n.LocalAppStrings
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.example.ui.theme.CardStroke
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseUser

@Composable
fun LanguageToggleChip(
    currentLanguage: AppLanguage,
    onToggle: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = SurfaceMuted,
        border = BorderStroke(1.dp, CardStroke),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("language_toggle_chip")
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLanguage.entries.forEach { lang ->
                val selected = lang == currentLanguage
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) IncomeGreen else SurfaceMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onToggle(lang) }
                        .testTag("lang_option_${lang.code}")
                ) {
                    Text(
                        text = lang.toggleLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) PureWhite else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    record: TransactionRecord,
    language: AppLanguage,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isIncome = record.parsedType == TransactionType.INCOME
    val category = record.parsedCategory
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val badgeBg = if (isIncome) IncomeGreenSoft else ExpenseRedSoft
    val borderColor = if (isIncome) IncomeGreen.copy(alpha = 0.22f) else ExpenseRed.copy(alpha = 0.18f)

    val titleText = if (record.description.isNotBlank()) {
        record.description
    } else {
        category.localizedLabel(language)
    }
    val subtitleText = buildString {
        append(category.localizedLabel(language))
        append(" • ")
        append(Formatters.formatDate(record.transactionDateMillis, language, strings))
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("transaction_card_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.localizedLabel(language),
                    tint = if (isIncome) IncomeGreen else category.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = Formatters.formatTaka(
                            amount = if (isIncome) record.amount else -record.amount,
                            language = language,
                            includeSign = true
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (onDeleteClick != null) {
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_tx_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = strings.delete,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UserAvatarButton(
    user: FirebaseUser?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .background(IncomeGreenSoft)
            .border(1.5.dp, IncomeGreen.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onClick)
            .testTag("account_profile_button"),
        contentAlignment = Alignment.Center
    ) {
        val photoUrl = user?.photoUrl?.toString()
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = user.displayName ?: "Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = "Profile",
                tint = IncomeGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AccountProfileDialog(
    user: FirebaseUser?,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onSignOutConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showConfirmSignOut by remember { mutableStateOf(false) }

    if (showConfirmSignOut) {
        AlertDialog(
            onDismissRequest = { showConfirmSignOut = false },
            title = { Text(strings.signOutConfirmTitle) },
            text = { Text(strings.signOutConfirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSignOut = false
                        onDismiss()
                        onSignOutConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(strings.signOut)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSignOut = false }) {
                    Text(strings.cancel)
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = {
            Text(
                text = strings.accountTitle,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserAvatarButton(user = user, onClick = {})
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.displayName?.takeIf { it.isNotBlank() } ?: strings.appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!user?.email.isNullOrBlank()) {
                            Text(
                                text = user.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceMuted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                tint = IncomeGreen
                            )
                            Text(
                                text = if (language == AppLanguage.BN) "ভাষা" else "Language",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }
                        LanguageToggleChip(
                            currentLanguage = language,
                            onToggle = onLanguageChange
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showConfirmSignOut = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.signOut, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.confirm)
            }
        }
    )
}
