package com.offpolice.webradiobot.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offpolice.webradiobot.ui.theme.ActiveCardBg
import com.offpolice.webradiobot.ui.theme.CardBg
import com.offpolice.webradiobot.ui.theme.PrimaryPink
import com.offpolice.webradiobot.ui.theme.SecondaryPink
import com.offpolice.webradiobot.ui.theme.TextMuted
import com.offpolice.webradiobot.ui.theme.TextPrimary
import com.offpolice.webradiobot.ui.theme.TextSecondary
import com.offpolice.webradiobot.updater.UpdateState
import java.util.Locale

private val DialogShape = RoundedCornerShape(24.dp)
private val ChipShape = RoundedCornerShape(12.dp)

@Composable
fun AppUpdateDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit
) {
    val context = LocalContext.current

    when (updateState) {
        is UpdateState.UpdateAvailable -> {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = DialogShape,
                    color = CardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, Color(0x33FFFFFF), DialogShape)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ActiveCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringLoc("new_version_available"),
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Version badge: v1.0.0 -> v1.0.1
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(CardBg, ChipShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "v${updateState.currentVersion.removePrefix("v")}",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "v${updateState.newVersion.removePrefix("v")}",
                                color = PrimaryPink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (updateState.apkAsset != null && updateState.apkAsset.size > 0L) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val sizeMb = String.format(Locale.US, "%.1f MB", updateState.apkAsset.size / (1024.0 * 1024.0))
                            Text(
                                text = "${stringLoc("update_size")}: $sizeMb",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Changelog block
                        val changelog = updateState.release.body?.trim().orEmpty()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBg, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringLoc("changelog_title"),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val scrollState = rememberScrollState()
                            Text(
                                text = if (changelog.isNotEmpty()) changelog else stringLoc("no_changelog"),
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier
                                    .heightIn(max = 120.dp)
                                    .verticalScroll(scrollState)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action buttons
                        if (updateState.apkAsset != null) {
                            Button(
                                onClick = onStartDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryPink,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("update_now_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringLoc("update_now"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        } else {
                            // If release doesn't have an APK attached yet, open in browser
                            Button(
                                onClick = {
                                    val url = updateState.release.htmlUrl ?: "https://github.com/OFFpolice/Radio-Apps/releases"
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryPink,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GitHub Release",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Text(
                                text = stringLoc("update_later"),
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        is UpdateState.Downloading -> {
            Dialog(
                onDismissRequest = {}, // Disallow clicking outside while downloading
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = DialogShape,
                    color = CardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, Color(0x33FFFFFF), DialogShape)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryPink,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringLoc("downloading_update"),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { updateState.percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryPink,
                            trackColor = CardBg
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val percentText = "${(updateState.percentage * 100).toInt()}%"
                            Text(
                                text = percentText,
                                color = PrimaryPink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            if (updateState.totalBytes > 0) {
                                val dlMb = updateState.downloadedBytes / (1024.0 * 1024.0)
                                val totMb = updateState.totalBytes / (1024.0 * 1024.0)
                                Text(
                                    text = String.format(Locale.US, "%.1f / %.1f MB", dlMb, totMb),
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = onCancelDownload,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = stringLoc("btn_cancel"),
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        is UpdateState.ReadyToInstall -> {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Surface(
                    shape = DialogShape,
                    color = CardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, Color(0x33FFFFFF), DialogShape)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ActiveCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringLoc("update_downloaded"),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "v${updateState.version.removePrefix("v")}",
                            color = PrimaryPink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onInstall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryPink,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("install_btn")
                        ) {
                            Text(
                                text = stringLoc("install_update"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Text(
                                text = stringLoc("btn_cancel"),
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        is UpdateState.UpToDate -> {
            if (updateState.isManualCheck) {
                Dialog(onDismissRequest = onDismiss) {
                    Surface(
                        shape = DialogShape,
                        color = CardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, Color(0x33FFFFFF), DialogShape)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringLoc("latest_version_installed"),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "v${updateState.currentVersion}",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("OK", color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        is UpdateState.Error -> {
            if (updateState.isManualCheck) {
                Dialog(onDismissRequest = onDismiss) {
                    Surface(
                        shape = DialogShape,
                        color = CardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, Color(0x33FFFFFF), DialogShape)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringLoc("update_error"),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = updateState.message,
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("OK", color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        else -> {}
    }
}

@Composable
fun SettingsUpdateSection(
    currentVersion: String,
    updateState: UpdateState,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isChecking = updateState is UpdateState.Checking

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ActiveCardBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = PrimaryPink,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringLoc("setting_updates"),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${stringLoc("current_version")}: v$currentVersion",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onCheckForUpdates,
                enabled = !isChecking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPink,
                    contentColor = Color.White,
                    disabledContainerColor = CardBg,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("check_updates_button")
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = PrimaryPink,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringLoc("check_updates"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringLoc("repo_label"),
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}
