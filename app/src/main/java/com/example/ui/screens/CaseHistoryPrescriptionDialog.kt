package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.CaseHistoryEntity
import com.example.data.model.PrescriptionEntity
import com.example.data.model.PrescriptionItemEntity
import com.example.data.repository.ClinicRepository
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CaseHistoryPrescriptionDialog(
    appointment: AppointmentEntity,
    repository: ClinicRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var diagnosis by remember { mutableStateOf("Chronic Allergic Rhinitis with Constitutional Psora") }
    var symptoms by remember { mutableStateOf("Sneezing bouts on waking, watery coryza, worse in cold breeze, better in warm room") }
    var clinicalNotes by remember { mutableStateOf("Patient responds well to Arsenicum and Allium Cepa indications. Administer constitutional remedy.") }

    var remedyName by remember { mutableStateOf("Arnica Montana") }
    var potency by remember { mutableStateOf("200C") }
    var dosage by remember { mutableStateOf("4 globules twice daily") }
    var durationDays by remember { mutableStateOf("14") }

    var dietAdvice by remember { mutableStateOf("Avoid raw garlic, coffee, and camphorated oils 30 minutes before/after pills.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Consultation & Prescription", fontWeight = FontWeight.Bold)
                Text(text = "Patient: ${appointment.patientName} (${appointment.id})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("Clinical Diagnosis") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = symptoms,
                    onValueChange = { symptoms = it },
                    label = { Text("Totality of Symptoms / Case Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text(text = "Prescribed Homeopathic Remedy", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = remedyName,
                        onValueChange = { remedyName = it },
                        label = { Text("Remedy") },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = potency,
                        onValueChange = { potency = it },
                        label = { Text("Potency") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage Instruction") },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { durationDays = it },
                        label = { Text("Days") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = dietAdvice,
                    onValueChange = { dietAdvice = it },
                    label = { Text("Diet & Regimen Advice") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        // Save Case History
                        val ch = CaseHistoryEntity(
                            appointmentId = appointment.id,
                            patientId = appointment.patientId,
                            doctorId = appointment.doctorId,
                            visitDate = today,
                            chiefComplaints = symptoms,
                            totalityOfSymptoms = symptoms,
                            clinicalNotes = clinicalNotes
                        )
                        repository.saveCaseHistory(ch)

                        // Save Prescription
                        val rxId = "RX-" + (10000..99999).random()
                        val rx = PrescriptionEntity(
                            id = rxId,
                            appointmentId = appointment.id,
                            patientId = appointment.patientId,
                            doctorId = appointment.doctorId,
                            date = today,
                            diagnosis = diagnosis,
                            dietAndRegimenAdvice = dietAdvice,
                            followUpInDays = durationDays.toIntOrNull() ?: 14
                        )

                        val item = PrescriptionItemEntity(
                            prescriptionId = rxId,
                            medicineName = remedyName,
                            potency = potency,
                            dosageForm = "Globules",
                            dosageInstruction = dosage,
                            durationDays = durationDays.toIntOrNull() ?: 14
                        )

                        repository.savePrescription(rx, listOf(item))
                        onSaved()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Complete & Save RX")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
