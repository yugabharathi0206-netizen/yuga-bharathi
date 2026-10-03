package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.ClinicEntity
import com.example.data.repository.ClinicRepository
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import com.example.ui.components.QrCodeView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HealingGreen
import com.example.ui.theme.MedicalBlue
import com.example.util.ContactUtils
import kotlinx.coroutines.launch

@Composable
fun ConfirmationScreen(
    appointment: AppointmentEntity,
    clinic: ClinicEntity?,
    currentLanguage: AppLanguage,
    repository: ClinicRepository,
    onViewAppointmentClick: () -> Unit,
    onBackToHomeClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCancelDialog by remember { mutableStateOf(false) }

    BackHandler {
        onBackToHomeClick()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Success badge with 3D animation feel
        Surface(
            shape = CircleShape,
            color = HealingGreen.copy(alpha = 0.15f),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = HealingGreen,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = StringsLocalization.get("appointment_confirmed", currentLanguage),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = EmeraldPrimary
        )

        Text(
            text = "Your slot is locked in the clinic calendar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed Ticket Card with 3D QR Code
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QR Code
                QrCodeView(
                    data = appointment.id,
                    size = 150.dp
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = StringsLocalization.get("qr_scan_note", currentLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                SummaryRow(label = StringsLocalization.get("appointment_id", currentLanguage), value = appointment.id)
                SummaryRow(label = StringsLocalization.get("doctor", currentLanguage), value = appointment.doctorName)
                SummaryRow(label = StringsLocalization.get("clinic", currentLanguage), value = clinic?.name ?: "HOMEo AI Clinic")
                SummaryRow(label = StringsLocalization.get("date", currentLanguage), value = appointment.appointmentDate)
                SummaryRow(label = StringsLocalization.get("time", currentLanguage), value = appointment.timeSlot)
                SummaryRow(label = StringsLocalization.get("patient_name", currentLanguage), value = appointment.patientName)
                SummaryRow(
                    label = "Payment Status",
                    value = if (appointment.paymentStatus.name == "PAID") "PAID (Online)" else "Pay at Reception"
                )

                if (appointment.meetingLink.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Video Consultation Link:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = appointment.meetingLink,
                                color = MedicalBlue,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Row 1: Add to Calendar & Directions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val calIntent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, "Homeopathy Consultation - ${appointment.doctorName}")
                        putExtra(CalendarContract.Events.DESCRIPTION, "Appointment ID: ${appointment.id}\nClinic: ${clinic?.name ?: "HOMEo AI"}")
                        if (!clinic?.address.isNullOrBlank()) {
                            putExtra(CalendarContract.Events.EVENT_LOCATION, clinic?.address)
                        }
                    }
                    try {
                        context.startActivity(calIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Calendar application not found", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(StringsLocalization.get("add_to_calendar", currentLanguage), fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    ContactUtils.openDirections(context, clinic?.address)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(StringsLocalization.get("get_directions", currentLanguage), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row 2: Call Clinic & WhatsApp Helpdesk
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    ContactUtils.callClinic(context, clinic?.phone)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(StringsLocalization.get("call_clinic", currentLanguage), fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    ContactUtils.openWhatsApp(
                        context,
                        clinic?.whatsAppNumber ?: clinic?.phone,
                        "Hello, enquiry regarding my confirmed appointment #${appointment.id}"
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = MedicalBlue)
                Spacer(modifier = Modifier.width(6.dp))
                Text("WhatsApp", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Button: View Appointment Details
        Button(
            onClick = onViewAppointmentClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(Icons.Filled.Visibility, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(StringsLocalization.get("view_appointment", currentLanguage), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Return Home
        TextButton(
            onClick = onBackToHomeClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Clinic Home")
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Appointment?") },
            text = { Text("Are you sure you want to cancel this appointment? Your slot will be released back to the clinic availability.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.cancelAppointment(appointment.id)
                            showCancelDialog = false
                            onViewAppointmentClick()
                        }
                    }
                ) {
                    Text("Cancel Appointment", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Appointment")
                }
            }
        )
    }
}
