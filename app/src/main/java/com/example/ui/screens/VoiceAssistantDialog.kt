package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DoctorEntity
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import com.example.ui.components.Orb3DView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalBlue
import com.example.ui.voice.VoiceAssistantManager
import java.util.Locale

@Composable
fun VoiceAssistantDialog(
    voiceManager: VoiceAssistantManager,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onNavigateToConfirmation: (DoctorEntity, String, String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                voiceManager.processVoiceInput(
                    input = spokenText,
                    lang = currentLanguage,
                    onOpenConfirmation = { doc, date, slot ->
                        onDismiss()
                        onNavigateToConfirmation(doc, date, slot)
                    }
                )
            }
        }
        voiceManager.isListening = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header with 3D Orb
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Orb3DView(size = 46.dp, isListening = voiceManager.isListening)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HOMEo AI Voice Booking",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tamil • Tanglish • English",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Chat Messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(voiceManager.conversationHistory) { msg ->
                        val isAi = msg.sender == "AI"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isAi) 4.dp else 16.dp,
                                    bottomEnd = if (isAi) 16.dp else 4.dp
                                ),
                                color = if (isAi) MaterialTheme.colorScheme.primaryContainer else EmeraldPrimary,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (isAi) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Sample query chips
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val samples = listOf(
                        "Innaikku evening appointment venum",
                        "Dr. Rajesh Kumar",
                        "05:00 PM book pannu",
                        "Yes confirm pannu",
                        "நாளை மாலை டாக்டர் ராஜேஷ்",
                        "Book tomorrow 10:00 AM"
                    )
                    items(samples) { sample ->
                        SuggestionChip(
                            onClick = {
                                voiceManager.processVoiceInput(
                                    input = sample,
                                    lang = currentLanguage,
                                    onOpenConfirmation = { doc, date, slot ->
                                        onDismiss()
                                        onNavigateToConfirmation(doc, date, slot)
                                    }
                                )
                            },
                            label = { Text(sample, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Bar with Mic button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Speak or type in Tanglish/Tamil/EN...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = textInput
                                textInput = ""
                                voiceManager.processVoiceInput(
                                    input = text,
                                    lang = currentLanguage,
                                    onOpenConfirmation = { doc, date, slot ->
                                        onDismiss()
                                        onNavigateToConfirmation(doc, date, slot)
                                    }
                                )
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                voiceManager.isListening = true
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLanguage == AppLanguage.TAMIL) "ta-IN" else "en-IN")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your appointment request...")
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (_: Exception) {
                                    voiceManager.isListening = false
                                    // Fallback sample
                                    voiceManager.processVoiceInput(
                                        input = "Innaikku evening doctor appointment venum",
                                        lang = currentLanguage,
                                        onOpenConfirmation = { doc, date, slot ->
                                            onDismiss()
                                            onNavigateToConfirmation(doc, date, slot)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (voiceManager.isListening) Color.Red else EmeraldPrimary)
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = "Mic", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
