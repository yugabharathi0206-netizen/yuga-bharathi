package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.data.model.DoctorEntity
import com.example.data.repository.ClinicRepository
import com.example.localization.AppLanguage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HealingGreen
import com.example.ui.theme.MedicalBlue
import kotlinx.coroutines.launch

@Composable
fun AdminAppointmentDetailDialog(
    appointment: AppointmentEntity,
    doctors: List<DoctorEntity>,
    repository: ClinicRepository,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onStatusUpdated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var adminNotes by remember { mutableStateOf(appointment.adminNotes) }
    var isSaving by remember { mutableStateOf(false) }

    var showRescheduleDialog by remember { mutableStateOf(false) }
    var showCancelReasonDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Appointment #${appointment.id}",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "Booked on: ${appointment.appointmentDate} at ${appointment.timeSlot}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusChip(status = appointment.status)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Patient Info Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Patient Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Name: ${appointment.patientName} (${appointment.patientAge}y • ${appointment.patientGender})", fontWeight = FontWeight.Medium)
                            Text(text = "Phone: ${appointment.patientPhone}", style = MaterialTheme.typography.bodySmall)
                            if (appointment.patientEmail.isNotBlank()) {
                                Text(text = "Email: ${appointment.patientEmail}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${appointment.patientPhone}"))
                                    context.startActivity(dial)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call Patient", fontSize = 12.sp)
                            }
                        }
                    }

                    // Clinical Request Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Consultation Reason & Symptoms", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Reason: ${appointment.reasonForVisit}", fontWeight = FontWeight.SemiBold, color = EmeraldPrimary)
                            if (appointment.symptomsDescription.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Symptoms: ${appointment.symptomsDescription}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Type: ${if (appointment.type.name == "ONLINE") "Online Video Consultation" else "In-Clinic Visit"}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Doctor: ${appointment.doctorName}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Fee: ₹${appointment.consultationFee.toInt()} (${appointment.paymentStatus.name})", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Admin Notes Field
                    OutlinedTextField(
                        value = adminNotes,
                        onValueChange = { adminNotes = it },
                        label = { Text("Admin / Doctor Clinical Notes") },
                        placeholder = { Text("Add follow-up notes, reception alerts, or remedy recommendations...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Action Controls Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (appointment.status == AppointmentStatus.PENDING) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.confirmAppointmentByAdmin(appointment.id, adminNotes)
                                        onStatusUpdated()
                                        onDismiss()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HealingGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm Booking", fontSize = 12.sp)
                            }
                        }

                        if (appointment.status != AppointmentStatus.COMPLETED && appointment.status != AppointmentStatus.CANCELLED) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.markAppointmentCompleted(appointment.id, adminNotes)
                                        onStatusUpdated()
                                        onDismiss()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Mark Completed", fontSize = 12.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRescheduleDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reschedule", fontSize = 12.sp)
                        }

                        if (appointment.status != AppointmentStatus.CANCELLED) {
                            OutlinedButton(
                                onClick = { showCancelReasonDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject / Cancel", fontSize = 12.sp)
                            }
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(0.7f)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    if (showRescheduleDialog) {
        RescheduleDialog(
            appointment = appointment,
            doctors = doctors,
            repository = repository,
            currentLanguage = currentLanguage,
            onDismiss = { showRescheduleDialog = false },
            onRescheduled = {
                showRescheduleDialog = false
                onStatusUpdated()
                onDismiss()
            }
        )
    }

    if (showCancelReasonDialog) {
        AlertDialog(
            onDismissRequest = { showCancelReasonDialog = false },
            title = { Text("Reject / Cancel Booking") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide reason for cancellation (patient will receive this via notification):")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason") },
                        placeholder = { Text("e.g. Doctor emergency or slot timing conflict") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.cancelAppointmentByAdmin(appointment.id, cancelReason)
                            showCancelReasonDialog = false
                            onStatusUpdated()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelReasonDialog = false }) {
                    Text("Back")
                }
            }
        )
    }
}
