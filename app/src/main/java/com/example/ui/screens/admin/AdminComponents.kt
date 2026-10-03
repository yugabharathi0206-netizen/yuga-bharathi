package com.example.ui.screens.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.localization.AppLanguage
import com.example.ui.screens.EmptyStateCard
import com.example.ui.screens.StatusChip
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HealingGreen
import com.example.ui.theme.MedicalBlue
import com.example.util.ContactUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun KpiStatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun AdminAppointmentsManagement(
    appointments: List<AppointmentEntity>,
    todayDate: String,
    onOpenAppointment: (AppointmentEntity) -> Unit,
    onConfirm: (AppointmentEntity) -> Unit,
    onCancel: (AppointmentEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }

    val filtered = appointments.filter { apt ->
        val matchesQuery = searchQuery.isBlank() ||
                apt.patientName.contains(searchQuery, ignoreCase = true) ||
                apt.patientPhone.contains(searchQuery) ||
                apt.id.contains(searchQuery, ignoreCase = true) ||
                apt.appointmentDate.contains(searchQuery)

        val matchesStatus = when (statusFilter) {
            "TODAY" -> apt.appointmentDate == todayDate
            "PENDING" -> apt.status == AppointmentStatus.PENDING
            "CONFIRMED" -> apt.status == AppointmentStatus.CONFIRMED
            "COMPLETED" -> apt.status == AppointmentStatus.COMPLETED
            "CANCELLED" -> apt.status == AppointmentStatus.CANCELLED
            else -> true
        }
        matchesQuery && matchesStatus
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, phone, ID, date...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "TODAY", "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED").forEach { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { statusFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(message = "No appointments match your search or filter.")
                }
            } else {
                items(filtered) { apt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAppointment(apt) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "#${apt.id}", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "• ${apt.appointmentDate} at ${apt.timeSlot}", style = MaterialTheme.typography.bodySmall)
                                }
                                StatusChip(status = apt.status)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = apt.patientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Phone: ${apt.patientPhone} • Reason: ${apt.reasonForVisit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (apt.adminNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Notes: ${apt.adminNotes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MedicalBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (apt.status == AppointmentStatus.PENDING) {
                                    Button(
                                        onClick = { onConfirm(apt) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HealingGreen),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Confirm", fontSize = 11.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onOpenAppointment(apt) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("View Details", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPatientManagement(
    patients: List<PatientEntity>,
    appointments: List<AppointmentEntity>,
    onSelectPatient: (PatientEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = patients.filter { p ->
        searchQuery.isBlank() ||
                p.fullName.contains(searchQuery, ignoreCase = true) ||
                p.phone.contains(searchQuery) ||
                p.email.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search patients by name, phone...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Total Registered Patients (${filtered.size})",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered) { patient ->
                val patientApts = appointments.filter { it.patientPhone == patient.phone || it.patientId == patient.id }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPatient(patient) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = patient.fullName.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = patient.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Phone: ${patient.phone} • Age: ${patient.age}y (${patient.gender})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${patientApts.size} Consultations recorded", fontSize = 11.sp, color = MedicalBlue)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCalendarView(
    appointments: List<AppointmentEntity>,
    onOpenAppointment: (AppointmentEntity) -> Unit
) {
    var selectedCalendarMode by remember { mutableStateOf("TODAY") }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = remember { sdf.format(Date()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Clinic Calendar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("TODAY", "WEEK", "MONTH").forEach { mode ->
                    FilterChip(
                        selected = selectedCalendarMode == mode,
                        onClick = { selectedCalendarMode = mode },
                        label = { Text(mode, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val list = when (selectedCalendarMode) {
                "TODAY" -> appointments.filter { it.appointmentDate == today }
                else -> appointments
            }

            if (list.isEmpty()) {
                item {
                    EmptyStateCard(message = "No appointments scheduled for selected period.")
                }
            } else {
                items(list) { apt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAppointment(apt) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (apt.status) {
                                        AppointmentStatus.PENDING -> Color(0xFFFFF3E0)
                                        AppointmentStatus.CONFIRMED -> Color(0xFFE8F5E9)
                                        AppointmentStatus.COMPLETED -> Color(0xFFEDE7F6)
                                        else -> Color(0xFFFFEBEE)
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = when (apt.status) {
                                                AppointmentStatus.PENDING -> Color(0xFFE65100)
                                                AppointmentStatus.CONFIRMED -> Color(0xFF2E7D32)
                                                AppointmentStatus.COMPLETED -> Color(0xFF512DA8)
                                                else -> Color(0xFFC62828)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = "${apt.timeSlot} • ${apt.appointmentDate}", fontWeight = FontWeight.Bold)
                                    Text(text = "${apt.patientName} (${apt.id})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            StatusChip(status = apt.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMessagesView(messages: List<MessageEntity>) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Patient Enquiries (${messages.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))

        if (messages.isEmpty()) {
            EmptyStateCard(message = "No patient messages or enquiries received.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = msg.subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE3F2FD)) {
                                    Text(text = msg.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalBlue, modifier = Modifier.padding(4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = msg.message, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "From: ${msg.clientName} (${msg.clientPhone})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${msg.clientPhone}"))
                                    context.startActivity(dial)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Contact Client")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminNotificationsView(
    notifications: List<NotificationEntity>,
    appointments: List<AppointmentEntity>,
    onOpenAppointment: (AppointmentEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Admin Notifications Center", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))

        if (notifications.isEmpty()) {
            EmptyStateCard(message = "No admin notifications.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                notif.appointmentId?.let { aptId ->
                                    val apt = appointments.firstOrNull { it.id == aptId }
                                    if (apt != null) onOpenAppointment(apt)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = Color(0xFFFFF3E0), modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.NotificationImportant, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = notif.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(text = notif.message, style = MaterialTheme.typography.bodySmall)
                                if (notif.appointmentId != null) {
                                    Text(text = "Tap to open Booking #${notif.appointmentId}", color = MedicalBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReportsView(
    appointments: List<AppointmentEntity>,
    medicines: List<MedicineEntity>
) {
    val total = appointments.size
    val completed = appointments.count { it.status == AppointmentStatus.COMPLETED }
    val cancelled = appointments.count { it.status == AppointmentStatus.CANCELLED }
    val totalRev = appointments.filter { it.status != AppointmentStatus.CANCELLED }.sumOf { it.consultationFee }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Clinic Management Reports", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiStatCard("Total Revenue", "₹${totalRev.toInt()}", EmeraldPrimary, Modifier.weight(1f))
            KpiStatCard("Completion Rate", "${if (total > 0) (completed * 100 / total) else 0}%", HealingGreen, Modifier.weight(1f))
            KpiStatCard("Cancellation Rate", "${if (total > 0) (cancelled * 100 / total) else 0}%", Color.Red, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Remedy Inventory Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

        medicines.forEach { med ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "${med.remedyName} (${med.availablePotencies})", style = MaterialTheme.typography.bodySmall)
                Text(text = "${med.stockQuantity} ${med.unit}", fontWeight = FontWeight.Bold, color = if (med.stockQuantity <= med.reorderLevel) Color.Red else EmeraldPrimary)
            }
        }
    }
}

@Composable
fun AdminProfileSettingsView(
    clinic: ClinicEntity?,
    doctors: List<DoctorEntity>,
    repository: ClinicRepository,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentDoctor = doctors.firstOrNull()

    var clinicName by remember(clinic) { mutableStateOf(clinic?.name ?: "HOMEo AI Classical Clinic") }
    var doctorName by remember(clinic, currentDoctor) { mutableStateOf(clinic?.doctorName?.ifBlank { currentDoctor?.name.orEmpty() } ?: currentDoctor?.name.orEmpty()) }
    var phone by remember(clinic) { mutableStateOf(clinic?.phone.orEmpty()) }
    var whatsAppNumber by remember(clinic) { mutableStateOf(clinic?.whatsAppNumber.orEmpty()) }
    var email by remember(clinic) { mutableStateOf(clinic?.email.orEmpty()) }
    var address by remember(clinic) { mutableStateOf(clinic?.address.orEmpty()) }
    var openingTime by remember(clinic) { mutableStateOf(clinic?.openingTime ?: "09:00 AM") }
    var closingTime by remember(clinic) { mutableStateOf(clinic?.closingTime ?: "08:00 PM") }
    var showPhonePublicly by remember(clinic) { mutableStateOf(clinic?.showPhoneNumberOnPublicPage ?: true) }
    var workingHours by remember(clinic) { mutableStateOf(clinic?.workingHoursSummary ?: "Mon - Sat: 9:00 AM - 1:00 PM & 4:00 PM - 8:00 PM") }
    var fee by remember(clinic) { mutableStateOf("${clinic?.defaultConsultationFee?.toInt() ?: 500}") }
    var slotMins by remember { mutableIntStateOf(currentDoctor?.slotDurationMinutes ?: 30) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Central Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MedicalBlue.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MedicalBlue,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ContactPhone, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Central Clinic & Contact Profile",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MedicalBlue
                    )
                    Text(
                        text = "The Clinic Phone Number configured here is the ONLY source for all 'Call Clinic' buttons throughout the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "Clinic Information",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )

        OutlinedTextField(
            value = clinicName,
            onValueChange = { clinicName = it },
            label = { Text("Clinic Name *") },
            leadingIcon = { Icon(Icons.Filled.Store, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = doctorName,
            onValueChange = { doctorName = it },
            label = { Text("Doctor Name *") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Highlighted Central Clinic Phone Number
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.06f)),
            border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Clinic Phone Number (Central Call System)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = EmeraldPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Clinic Phone Number *") },
                    placeholder = { Text("e.g. +91 98401 00000") },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (phone.isBlank())
                        "⚠️ Not configured yet. App will inform patients: 'Clinic phone number not configured'."
                    else
                        "✓ Active central dialer source: All 'Call Clinic' buttons will dial $phone.",
                    fontSize = 11.sp,
                    color = if (phone.isBlank()) Color(0xFFC62828) else EmeraldPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // WhatsApp Number
        OutlinedTextField(
            value = whatsAppNumber,
            onValueChange = { whatsAppNumber = it },
            label = { Text("WhatsApp Number") },
            placeholder = { Text("e.g. +91 98401 00000") },
            leadingIcon = { Icon(Icons.Filled.Chat, contentDescription = null) },
            supportingText = { Text("Used for all WhatsApp enquiry links across the app") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Clinic Email") },
            placeholder = { Text("e.g. care@clinic.com") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Address
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Clinic Address") },
            placeholder = { Text("Full physical address for navigation") },
            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 2
        )

        Text(
            text = "Timings & Public Display",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = openingTime,
                onValueChange = { openingTime = it },
                label = { Text("Opening Time") },
                placeholder = { Text("09:00 AM") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = closingTime,
                onValueChange = { closingTime = it },
                label = { Text("Closing Time") },
                placeholder = { Text("08:00 PM") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = workingHours,
            onValueChange = { workingHours = it },
            label = { Text("Working Hours Summary") },
            placeholder = { Text("Mon - Sat: 9:00 AM - 1:00 PM & 4:00 PM - 8:00 PM") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Public visibility switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show phone number on public clinic page",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (showPhonePublicly)
                            "Visible: Patients see the clinic phone number on the home page."
                        else
                            "Hidden: Phone number is hidden from text display; 'Call Clinic' button still works.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showPhonePublicly,
                    onCheckedChange = { showPhonePublicly = it }
                )
            }
        }

        // Test Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { ContactUtils.callClinic(context, phone) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Call", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { ContactUtils.openWhatsApp(context, whatsAppNumber.ifBlank { phone }) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test WhatsApp", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Save Button
        Button(
            onClick = {
                scope.launch {
                    val base = clinic ?: ClinicEntity(id = "clinic_main", name = clinicName)
                    val updated = base.copy(
                        name = clinicName.trim(),
                        doctorName = doctorName.trim(),
                        phone = phone.trim(),
                        whatsAppNumber = whatsAppNumber.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        openingTime = openingTime.trim(),
                        closingTime = closingTime.trim(),
                        showPhoneNumberOnPublicPage = showPhonePublicly,
                        workingHoursSummary = workingHours.trim(),
                        defaultConsultationFee = fee.toDoubleOrNull() ?: 500.0
                    )
                    repository.saveClinic(updated)

                    // Sync to doctor profile if doctor exists
                    currentDoctor?.let { d ->
                        val updatedDoc = d.copy(
                            clinicName = clinicName.trim(),
                            clinicAddress = address.trim(),
                            phone = if (d.phone.isBlank()) phone.trim() else d.phone,
                            whatsAppNumber = if (d.whatsAppNumber.isBlank()) whatsAppNumber.trim() else d.whatsAppNumber,
                            slotDurationMinutes = slotMins
                        )
                        repository.saveDoctor(updatedDoc)
                    }

                    Toast.makeText(context, "Clinic Profile & Phone Number saved successfully!", Toast.LENGTH_LONG).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Clinic Profile", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminPatientProfileDialog(
    patient: PatientEntity,
    appointments: List<AppointmentEntity>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = patient.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Patient ID: ${patient.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Personal & Contact Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(text = "• Phone: ${patient.phone}")
                    if (patient.email.isNotBlank()) Text(text = "• Email: ${patient.email}")
                    Text(text = "• Age / Gender: ${patient.age} years • ${patient.gender}")
                    if (patient.dateOfBirth.isNotBlank()) Text(text = "• Date of Birth: ${patient.dateOfBirth}")
                    if (patient.address.isNotBlank()) Text(text = "• Address: ${patient.address}")
                    if (patient.emergencyContact.isNotBlank()) Text(text = "• Emergency Contact: ${patient.emergencyContact}")

                    if (patient.currentConcerns.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Current Health Concerns", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(text = patient.currentConcerns, style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Consultation & Booking History (${appointments.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    appointments.forEach { apt ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "#${apt.id} • ${apt.appointmentDate}", fontWeight = FontWeight.Bold)
                                    StatusChip(status = apt.status)
                                }
                                Text(text = "Reason: ${apt.reasonForVisit}", style = MaterialTheme.typography.bodySmall)
                                if (apt.adminNotes.isNotBlank()) {
                                    Text(text = "Notes: ${apt.adminNotes}", style = MaterialTheme.typography.labelSmall, color = MedicalBlue)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close Profile")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. OWNER PROFILE VIEW
// -------------------------------------------------------------
@Composable
fun OwnerProfileView(
    currentUser: UserEntity,
    clinic: ClinicEntity?,
    repository: ClinicRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var twoFactor by remember { mutableStateOf(currentUser.twoFactorEnabled) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Owner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MedicalBlue.copy(alpha = 0.08f)),
            border = BorderStroke(1.5.dp, MedicalBlue.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MedicalBlue, modifier = Modifier.size(54.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = currentUser.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Surface(shape = RoundedCornerShape(6.dp), color = MedicalBlue) {
                            Text(
                                text = "CLINIC OWNER (MASTER ACCESS)",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(text = "Contact & Authentication Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "• Email: ${currentUser.email.ifBlank { "Not configured" }}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• Mobile Phone: ${currentUser.phone.ifBlank { "Not configured" }}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• Account Status: Active & Secured", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Security Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Security & Master Control", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Two-Factor Verification (2FA)", fontWeight = FontWeight.SemiBold)
                        Text("Extra security layer for Owner command center", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = twoFactor,
                        onCheckedChange = { checked ->
                            twoFactor = checked
                            scope.launch {
                                repository.updateUser(currentUser.copy(twoFactorEnabled = checked))
                                Toast.makeText(context, "Two-factor setting updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                HorizontalDivider()

                Button(
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Filled.LockReset, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Master Password")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repository.deactivateAllSessions(currentUser.id)
                            Toast.makeText(context, "Terminated all other active device sessions.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.DevicesOther, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terminate Other Device Sessions")
                }
            }
        }
    }

    if (showPasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Update Master Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password (min 4 chars)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.length < 4) {
                            Toast.makeText(context, "Password too short", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPass != confirmPass) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            repository.updateUser(currentUser.copy(passwordHash = newPass))
                            Toast.makeText(context, "Master password changed successfully!", Toast.LENGTH_LONG).show()
                            showPasswordDialog = false
                        }
                    }
                ) {
                    Text("Save Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 2. DOCTOR MANAGEMENT VIEW (All 16 Profile Fields)
// -------------------------------------------------------------
@Composable
fun DoctorManagementView(
    doctors: List<DoctorEntity>,
    currentUser: UserEntity,
    clinic: ClinicEntity?,
    repository: ClinicRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val existingDoctor = doctors.firstOrNull()

    // 16 Real Doctor Profile Fields
    var doctorName by remember { mutableStateOf(existingDoctor?.name ?: currentUser.fullName) }
    var profilePhotoUrl by remember { mutableStateOf(existingDoctor?.photoUrl ?: "") }
    var qualification by remember { mutableStateOf(existingDoctor?.qualification ?: "BHMS, MD (Homeopathy)") }
    var specialization by remember { mutableStateOf(existingDoctor?.specialization ?: "Classical Constitutional Homeopathy") }
    var experienceYears by remember { mutableIntStateOf(existingDoctor?.experienceYears ?: 12) }
    var registrationNo by remember { mutableStateOf(existingDoctor?.registrationNumber ?: "") }
    var clinicName by remember { mutableStateOf(existingDoctor?.clinicName ?: (clinic?.name ?: "HOMEo AI Classical Clinic")) }
    var clinicAddress by remember { mutableStateOf(existingDoctor?.clinicAddress ?: (clinic?.address ?: "")) }
    var phone by remember(existingDoctor, clinic) { mutableStateOf(existingDoctor?.phone?.ifBlank { clinic?.phone.orEmpty() } ?: (clinic?.phone ?: currentUser.phone)) }
    var whatsAppNumber by remember(existingDoctor, clinic) { mutableStateOf(existingDoctor?.whatsAppNumber?.ifBlank { clinic?.whatsAppNumber.orEmpty() } ?: (clinic?.whatsAppNumber ?: currentUser.phone)) }
    var consultationFee by remember { mutableStateOf("${existingDoctor?.consultationFee?.toInt() ?: 500}") }
    var morningStart by remember { mutableStateOf(existingDoctor?.morningStart ?: "09:00") }
    var morningEnd by remember { mutableStateOf(existingDoctor?.morningEnd ?: "13:00") }
    var eveningStart by remember { mutableStateOf(existingDoctor?.eveningStart ?: "16:00") }
    var eveningEnd by remember { mutableStateOf(existingDoctor?.eveningEnd ?: "20:00") }
    var slotMins by remember { mutableIntStateOf(existingDoctor?.slotDurationMinutes ?: 30) }
    var availableDays by remember { mutableStateOf(existingDoctor?.availableDays ?: "Mon,Tue,Wed,Thu,Fri,Sat") }
    var aboutDoctor by remember { mutableStateOf(existingDoctor?.bio ?: "Dedicated to individualised constitutional treatment of chronic, autoimmune, and pediatric disorders.") }
    var languages by remember { mutableStateOf(existingDoctor?.languages ?: "Tamil, Tanglish, English") }
    var services by remember { mutableStateOf(existingDoctor?.services ?: "Constitutional Homeopathy, Chronic Disease Care, Pediatric Care") }
    var socialMediaLinks by remember { mutableStateOf(existingDoctor?.socialMediaLinks ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Doctor Profile Management", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (existingDoctor != null) {
                Surface(color = EmeraldPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(text = "Profile Active", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            } else {
                Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp)) {
                    Text(text = "Setup Required", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        Text(
            text = "Create and manage your official doctor profile. Patients will book appointments using these consultation timings, days, and fee structure.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(value = doctorName, onValueChange = { doctorName = it }, label = { Text("Doctor Name *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = qualification, onValueChange = { qualification = it }, label = { Text("Qualification (e.g. BHMS, MD Homeopathy) *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = specialization, onValueChange = { specialization = it }, label = { Text("Homeopathy Specialization *") }, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = "$experienceYears",
                onValueChange = { experienceYears = it.toIntOrNull() ?: experienceYears },
                label = { Text("Experience (Years)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = registrationNo,
                onValueChange = { registrationNo = it },
                label = { Text("Registration No.") },
                modifier = Modifier.weight(1.5f)
            )
        }

        OutlinedTextField(value = clinicName, onValueChange = { clinicName = it }, label = { Text("Clinic Name *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = clinicAddress, onValueChange = { clinicAddress = it }, label = { Text("Clinic Address *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number *") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = whatsAppNumber, onValueChange = { whatsAppNumber = it }, label = { Text("WhatsApp Number") }, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(value = consultationFee, onValueChange = { consultationFee = it }, label = { Text("Consultation Fee (₹) *") }, modifier = Modifier.fillMaxWidth())

        Text(text = "Consulting Timings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = morningStart, onValueChange = { morningStart = it }, label = { Text("Morn Start") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = morningEnd, onValueChange = { morningEnd = it }, label = { Text("Morn End") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = eveningStart, onValueChange = { eveningStart = it }, label = { Text("Eve Start") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = eveningEnd, onValueChange = { eveningEnd = it }, label = { Text("Eve End") }, modifier = Modifier.weight(1f))
        }

        Text(text = "Slot Duration: $slotMins minutes", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(15, 20, 30, 45, 60).forEach { mins ->
                FilterChip(selected = slotMins == mins, onClick = { slotMins = mins }, label = { Text("$mins min") })
            }
        }

        Text(text = "Available Days", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val currentDaysSet = availableDays.split(",").map { it.trim() }.toSet()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            daysList.forEach { day ->
                val isSelected = currentDaysSet.contains(day)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSet = if (isSelected) currentDaysSet - day else currentDaysSet + day
                        availableDays = daysList.filter { newSet.contains(it) }.joinToString(",")
                    },
                    label = { Text(day, fontSize = 11.sp) }
                )
            }
        }

        OutlinedTextField(value = aboutDoctor, onValueChange = { aboutDoctor = it }, label = { Text("About Doctor / Clinical Philosophy") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        OutlinedTextField(value = languages, onValueChange = { languages = it }, label = { Text("Consultation Languages (e.g. Tamil, Tanglish, English)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = services, onValueChange = { services = it }, label = { Text("Services & Treatments Offered") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = socialMediaLinks, onValueChange = { socialMediaLinks = it }, label = { Text("Website / Social Media Links") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (doctorName.isBlank()) {
                    Toast.makeText(context, "Doctor Name is required", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    val doctorEntity = DoctorEntity(
                        id = existingDoctor?.id ?: "doc_owner",
                        name = doctorName.trim(),
                        qualification = qualification.trim(),
                        specialization = specialization.trim(),
                        experienceYears = experienceYears,
                        registrationNumber = registrationNo.trim(),
                        bio = aboutDoctor.trim(),
                        consultationFee = consultationFee.toDoubleOrNull() ?: 500.0,
                        morningStart = morningStart.trim(),
                        morningEnd = morningEnd.trim(),
                        eveningStart = eveningStart.trim(),
                        eveningEnd = eveningEnd.trim(),
                        availableDays = availableDays,
                        slotDurationMinutes = slotMins,
                        phone = phone.trim(),
                        whatsAppNumber = whatsAppNumber.trim(),
                        clinicName = clinicName.trim(),
                        clinicAddress = clinicAddress.trim(),
                        languages = languages.trim(),
                        services = services.trim(),
                        socialMediaLinks = socialMediaLinks.trim(),
                        photoUrl = profilePhotoUrl.trim()
                    )
                    repository.saveOwnerDoctorProfile(doctorEntity, currentUser.id)
                    Toast.makeText(context, "Doctor profile saved successfully!", Toast.LENGTH_LONG).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Doctor Profile Changes", fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------
// 3. ADMIN MANAGEMENT VIEW (Only With Owner Permission)
// -------------------------------------------------------------
@Composable
fun AdminManagementView(
    repository: ClinicRepository,
    currentUser: UserEntity
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val admins by repository.allAdmins.collectAsState(initial = emptyList())

    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedAdminToEdit by remember { mutableStateOf<UserEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Staff & Admin Access Control", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = "Authorize and manage clinic admins with granular permissions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { showInviteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Admin", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (admins.isEmpty()) {
            EmptyStateCard(message = "No additional admins added yet. Tap 'Add Admin' to authorize a staff member.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(admins) { admin ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp,
                            when {
                                !admin.isApproved -> Color(0xFFF57C00)
                                !admin.isActive -> Color.Gray
                                else -> EmeraldPrimary.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = admin.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(text = "${admin.email.ifBlank { admin.phone }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = when {
                                        !admin.isApproved -> Color(0xFFFFF3E0)
                                        !admin.isActive -> Color(0xFFEEEEEE)
                                        else -> EmeraldPrimary.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            !admin.isApproved -> "PENDING APPROVAL"
                                            !admin.isActive -> "DISABLED"
                                            else -> "ACTIVE ADMIN"
                                        },
                                        color = when {
                                            !admin.isApproved -> Color(0xFFE65100)
                                            !admin.isActive -> Color.DarkGray
                                            else -> EmeraldPrimary
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // If PENDING approval: show prominent Permission Request Box
                            if (!admin.isApproved) {
                                Surface(
                                    color = Color(0xFFFFF8E1),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFB300)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = "⚠️ Admin Access Request", fontWeight = FontWeight.Bold, color = Color(0xFFB78103))
                                        Text(text = "This staff member requires your explicit approval before they can log in.", style = MaterialTheme.typography.bodySmall)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        repository.approveAdmin(admin.id, currentUser.id)
                                                        Toast.makeText(context, "Admin access approved for ${admin.fullName}!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Approve")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        repository.rejectAdmin(admin.id, currentUser.id)
                                                        Toast.makeText(context, "Admin access rejected.", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Reject")
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Active / Approved Admin controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (admin.isActive) "Account Enabled" else "Account Disabled",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Switch(
                                        checked = admin.isActive,
                                        onCheckedChange = { active ->
                                            scope.launch {
                                                repository.toggleAdminActive(admin.id, active, currentUser.id)
                                                Toast.makeText(context, "Account status updated", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { selectedAdminToEdit = admin },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Permissions", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteAdmin(admin.id, currentUser.id)
                                                Toast.makeText(context, "Admin removed", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Invite Admin Request Modal
    if (showInviteDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var tempPass by remember { mutableStateOf("") }
        var canAppts by remember { mutableStateOf(true) }
        var canPatients by remember { mutableStateOf(true) }
        var canQueue by remember { mutableStateOf(true) }
        var canMeds by remember { mutableStateOf(true) }
        var canBilling by remember { mutableStateOf(true) }
        var canReports by remember { mutableStateOf(true) }
        var canSettings by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Add Admin / Staff Member", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("The new admin will be created in PENDING status until you approve their access.", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Admin Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tempPass, onValueChange = { tempPass = it }, label = { Text("Initial Password *") }, modifier = Modifier.fillMaxWidth())

                    Text(text = "Assign Permissions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canAppts, onCheckedChange = { canAppts = it })
                        Text("Manage Appointments & Bookings", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canPatients, onCheckedChange = { canPatients = it })
                        Text("Manage Patients & History", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canQueue, onCheckedChange = { canQueue = it })
                        Text("Manage Live Queue & Tokens", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canMeds, onCheckedChange = { canMeds = it })
                        Text("Manage Medicine Inventory", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canBilling, onCheckedChange = { canBilling = it })
                        Text("Manage Billing & Payments", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canReports, onCheckedChange = { canReports = it })
                        Text("View Analytics & Reports", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canSettings, onCheckedChange = { canSettings = it })
                        Text("Manage Clinic Settings", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || (email.isBlank() && phone.isBlank()) || tempPass.isBlank()) {
                            Toast.makeText(context, "Name, contact and password are required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            val res = repository.addAdminRequest(
                                fullName = name,
                                email = email,
                                phone = phone,
                                password = tempPass,
                                canManageAppointments = canAppts,
                                canManagePatients = canPatients,
                                canManageQueue = canQueue,
                                canManageMedicines = canMeds,
                                canManageBilling = canBilling,
                                canViewReports = canReports,
                                canManageSettings = canSettings,
                                ownerId = currentUser.id
                            )
                            res.onSuccess {
                                Toast.makeText(context, "Admin access request created! Review and approve below.", Toast.LENGTH_LONG).show()
                                showInviteDialog = false
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
                ) {
                    Text("Create Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Admin Permissions Modal
    selectedAdminToEdit?.let { targetAdmin ->
        var canAppts by remember { mutableStateOf(targetAdmin.canManageAppointments) }
        var canPatients by remember { mutableStateOf(targetAdmin.canManagePatients) }
        var canQueue by remember { mutableStateOf(targetAdmin.canManageQueue) }
        var canMeds by remember { mutableStateOf(targetAdmin.canManageMedicines) }
        var canBilling by remember { mutableStateOf(targetAdmin.canManageBilling) }
        var canReports by remember { mutableStateOf(targetAdmin.canViewReports) }
        var canSettings by remember { mutableStateOf(targetAdmin.canManageSettings) }

        AlertDialog(
            onDismissRequest = { selectedAdminToEdit = null },
            title = { Text("Edit Permissions: ${targetAdmin.fullName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canAppts, onCheckedChange = { canAppts = it })
                        Text("Manage Appointments", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canPatients, onCheckedChange = { canPatients = it })
                        Text("Manage Patients", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canQueue, onCheckedChange = { canQueue = it })
                        Text("Manage Live Queue", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canMeds, onCheckedChange = { canMeds = it })
                        Text("Manage Pharmacy", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canBilling, onCheckedChange = { canBilling = it })
                        Text("Manage Billing", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canReports, onCheckedChange = { canReports = it })
                        Text("View Reports", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canSettings, onCheckedChange = { canSettings = it })
                        Text("Manage Clinic Settings", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.updateAdminPermissions(
                                adminId = targetAdmin.id,
                                canManageAppointments = canAppts,
                                canManagePatients = canPatients,
                                canManageQueue = canQueue,
                                canManageMedicines = canMeds,
                                canManageBilling = canBilling,
                                canViewReports = canReports,
                                canManageSettings = canSettings,
                                ownerId = currentUser.id
                            )
                            Toast.makeText(context, "Permissions updated for ${targetAdmin.fullName}", Toast.LENGTH_SHORT).show()
                            selectedAdminToEdit = null
                        }
                    }
                ) {
                    Text("Save Permissions")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAdminToEdit = null }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------
// 4. PERMISSIONS MATRIX & SECURITY AUDIT VIEW
// -------------------------------------------------------------
@Composable
fun PermissionsMatrixView(
    repository: ClinicRepository
) {
    val auditLogs by repository.allAuditLogs.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "Role Hierarchy & Permissions System", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "👑 OWNER (Clinic Owner)", fontWeight = FontWeight.Bold, color = MedicalBlue)
                Text(text = "• Unrestricted master access across all clinic modules\n• Manage doctor profile and clinic branding\n• Authorize, invite, and approve staff admins\n• Full control over appointments, patients, billing, and settings", fontSize = 12.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(text = "🛡️ ADMIN (Authorized Staff)", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                Text(text = "• Operates only with permissions explicitly approved by the Owner\n• Cannot create or remove Owner account\n• Cannot edit Owner permissions\n• Subject to immediate disable/enable control by Owner", fontSize = 12.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(text = "👤 CLIENT / PATIENT", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                Text(text = "• Patient portal access only\n• Can view doctor/clinic info and book appointments\n• Data strictly isolated to their own bookings\n• Completely blocked from Admin and Owner command centers", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Security Audit Logs (${auditLogs.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

        if (auditLogs.isEmpty()) {
            EmptyStateCard(message = "No security events recorded yet.")
        } else {
            auditLogs.take(20).forEach { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MedicalBlue)
                            Text(
                                text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(text = log.details, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

