package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.data.repository.LiveQueueStatus
import com.example.data.repository.SlotInfo
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import com.example.ui.components.QrCodeView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HealingGreen
import com.example.ui.theme.MedicalBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(
    patientPhone: String = "9876543210",
    appointments: List<AppointmentEntity>,
    doctors: List<DoctorEntity>,
    currentLanguage: AppLanguage,
    repository: ClinicRepository,
    onBookNewClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Upcoming, 1: Completed, 2: Cancelled, 3: Profile
    val tabTitles = listOf(
        StringsLocalization.get("upcoming", currentLanguage),
        StringsLocalization.get("completed", currentLanguage),
        StringsLocalization.get("cancelled", currentLanguage),
        StringsLocalization.get("profile", currentLanguage)
    )

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var selectedAppointmentForQr by remember { mutableStateOf<AppointmentEntity?>(null) }
    var selectedAppointmentToReschedule by remember { mutableStateOf<AppointmentEntity?>(null) }
    var selectedAppointmentToCancel by remember { mutableStateOf<AppointmentEntity?>(null) }

    // Filter appointments for patient
    val myAppointments = appointments.filter { it.patientPhone == patientPhone || patientPhone.isEmpty() }
    val upcomingAppointments = myAppointments.filter { it.status != AppointmentStatus.CANCELLED && it.status != AppointmentStatus.COMPLETED && it.status != AppointmentStatus.RESCHEDULED }
    val completedAppointments = myAppointments.filter { it.status == AppointmentStatus.COMPLETED }
    val cancelledAppointments = myAppointments.filter { it.status == AppointmentStatus.CANCELLED || it.status == AppointmentStatus.RESCHEDULED }

    // Find active checked-in or waiting appointment for today
    val todayActiveAppointment = upcomingAppointments.firstOrNull { it.appointmentDate == todayDate }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = StringsLocalization.get("my_appointments", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onBookNewClick) {
                        Icon(Icons.Filled.Add, contentDescription = "Book New", tint = EmeraldPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Upcoming List + Live Queue Card
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Live Queue Tracker for Today
                        if (todayActiveAppointment != null) {
                            item {
                                LiveQueueCard(
                                    appointment = todayActiveAppointment,
                                    currentLanguage = currentLanguage,
                                    onCheckInClick = {
                                        scope.launch {
                                            val token = repository.checkInPatient(todayActiveAppointment.id, todayDate)
                                            if (token > 0) {
                                                Toast.makeText(context, StringsLocalization.get("check_in_success", currentLanguage), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        if (upcomingAppointments.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    message = "No upcoming appointments.",
                                    actionLabel = StringsLocalization.get("book_appointment", currentLanguage),
                                    onActionClick = onBookNewClick
                                )
                            }
                        } else {
                            items(upcomingAppointments) { apt ->
                                AppointmentCard(
                                    appointment = apt,
                                    currentLanguage = currentLanguage,
                                    onViewQrClick = { selectedAppointmentForQr = apt },
                                    onRescheduleClick = { selectedAppointmentToReschedule = apt },
                                    onCancelClick = { selectedAppointmentToCancel = apt },
                                    onDirectionsClick = {
                                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=T+Nagar+Chennai+Homeopathy+Clinic"))
                                        context.startActivity(mapIntent)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Completed List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (completedAppointments.isEmpty()) {
                            item {
                                EmptyStateCard(message = "No completed consultations yet.")
                            }
                        } else {
                            items(completedAppointments) { apt ->
                                AppointmentCard(
                                    appointment = apt,
                                    currentLanguage = currentLanguage,
                                    onViewQrClick = { selectedAppointmentForQr = apt },
                                    onRescheduleClick = null,
                                    onCancelClick = null,
                                    onDirectionsClick = null
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Cancelled List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (cancelledAppointments.isEmpty()) {
                            item {
                                EmptyStateCard(message = "No cancelled appointments.")
                            }
                        } else {
                            items(cancelledAppointments) { apt ->
                                AppointmentCard(
                                    appointment = apt,
                                    currentLanguage = currentLanguage,
                                    onViewQrClick = null,
                                    onRescheduleClick = null,
                                    onCancelClick = null,
                                    onDirectionsClick = null
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // Patient Profile Edit
                    PatientProfileSection(
                        patientPhone = patientPhone,
                        repository = repository
                    )
                }
            }
        }
    }

    // QR Code Dialog
    selectedAppointmentForQr?.let { apt ->
        AlertDialog(
            onDismissRequest = { selectedAppointmentForQr = null },
            title = { Text(text = "Reception Check-in QR") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QrCodeView(data = apt.id, size = 180.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "ID: ${apt.id}", fontWeight = FontWeight.Bold)
                    Text(text = "${apt.doctorName} • ${apt.appointmentDate} at ${apt.timeSlot}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAppointmentForQr = null }) {
                    Text("Done")
                }
            }
        )
    }

    // Reschedule Dialog
    selectedAppointmentToReschedule?.let { apt ->
        RescheduleDialog(
            appointment = apt,
            doctors = doctors,
            repository = repository,
            currentLanguage = currentLanguage,
            onDismiss = { selectedAppointmentToReschedule = null },
            onRescheduled = {
                selectedAppointmentToReschedule = null
                Toast.makeText(context, "Appointment rescheduled successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Cancel Dialog
    selectedAppointmentToCancel?.let { apt ->
        AlertDialog(
            onDismissRequest = { selectedAppointmentToCancel = null },
            title = { Text("Cancel Appointment?") },
            text = { Text("Are you sure you want to cancel appointment ${apt.id}? This will release the slot back to the clinic.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.cancelAppointment(apt.id)
                            selectedAppointmentToCancel = null
                            Toast.makeText(context, "Appointment cancelled.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppointmentToCancel = null }) {
                    Text("Keep")
                }
            }
        )
    }
}

@Composable
fun LiveQueueCard(
    appointment: AppointmentEntity,
    currentLanguage: AppLanguage,
    onCheckInClick: () -> Unit
) {
    val isCheckedIn = appointment.tokenNumber != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MedicalBlue.copy(alpha = 0.08f)),
        border = BorderStroke(1.5.dp, MedicalBlue.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = HealingGreen, modifier = Modifier.size(10.dp)) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = StringsLocalization.get("live_queue", currentLanguage),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MedicalBlue
                    )
                }
                Text(
                    text = "Today's Clinic",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isCheckedIn) {
                Text(
                    text = "You are scheduled for today at ${appointment.timeSlot}. Please check in when you arrive at reception or tap below to self check-in.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onCheckInClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Filled.HowToReg, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(StringsLocalization.get("check_in", currentLanguage), fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QueueStatBox(
                        label = StringsLocalization.get("your_token", currentLanguage),
                        value = "#${appointment.tokenNumber}",
                        valueColor = EmeraldPrimary
                    )
                    QueueStatBox(
                        label = StringsLocalization.get("now_serving", currentLanguage),
                        value = "#${(appointment.tokenNumber ?: 1) - 2}".let { if (it.contains("-") || it == "#0") "#1" else it },
                        valueColor = MedicalBlue
                    )
                    QueueStatBox(
                        label = StringsLocalization.get("patients_ahead", currentLanguage),
                        value = "2",
                        valueColor = Color(0xFFF57C00)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• ${StringsLocalization.get("estimated_wait", currentLanguage)}: ~30 minutes (based on 15 mins/consultation)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QueueStatBox(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = valueColor)
    }
}

@Composable
fun AppointmentCard(
    appointment: AppointmentEntity,
    currentLanguage: AppLanguage,
    onViewQrClick: (() -> Unit)?,
    onRescheduleClick: (() -> Unit)?,
    onCancelClick: (() -> Unit)?,
    onDirectionsClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appointment.id,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary
                )
                StatusChip(status = appointment.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appointment.doctorName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${appointment.appointmentDate} • ${appointment.timeSlot}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Reason: ${appointment.reasonForVisit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (appointment.tokenNumber != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assigned Token: #${appointment.tokenNumber}",
                    fontWeight = FontWeight.Bold,
                    color = MedicalBlue,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onViewQrClick != null) {
                    OutlinedButton(
                        onClick = onViewQrClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QR Code", fontSize = 11.sp)
                    }
                }

                if (onRescheduleClick != null) {
                    OutlinedButton(
                        onClick = onRescheduleClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(StringsLocalization.get("reschedule", currentLanguage), fontSize = 11.sp)
                    }
                }

                if (onCancelClick != null) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(StringsLocalization.get("cancel", currentLanguage), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: AppointmentStatus) {
    val (bg, fg) = when (status) {
        AppointmentStatus.CONFIRMED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        AppointmentStatus.WAITING -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        AppointmentStatus.IN_CONSULTATION -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        AppointmentStatus.COMPLETED -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        AppointmentStatus.CANCELLED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        AppointmentStatus.RESCHEDULED -> Color(0xFFECEFF1) to Color(0xFF455A64)
        else -> Color(0xFFF1F5F9) to Color.DarkGray
    }

    Surface(shape = RoundedCornerShape(12.dp), color = bg) {
        Text(
            text = status.name,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.EventNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onActionClick, colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                    Text(text = actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PatientProfileSection(
    patientPhone: String,
    repository: ClinicRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("Karthik Subramanian") }
    var phone by remember { mutableStateOf(patientPhone.ifEmpty { "9876543210" }) }
    var email by remember { mutableStateOf("karthik.sub@gmail.com") }
    var dob by remember { mutableStateOf("1992-06-15") }
    var gender by remember { mutableStateOf("Male") }
    var address by remember { mutableStateOf("Flat 4B, Emerald Residency, Anna Nagar, Chennai") }
    var emergencyContact by remember { mutableStateOf("Sangeetha (Spouse): 9876543211") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Patient Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = "Keep your contact information updated for prescriptions and SMS reminders.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Phone") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (Optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Home Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = emergencyContact, onValueChange = { emergencyContact = it }, label = { Text("Emergency Contact") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    val p = PatientEntity(
                        id = "pat_demo_01",
                        fullName = name,
                        phone = phone,
                        email = email,
                        dateOfBirth = dob,
                        gender = gender,
                        address = address,
                        emergencyContact = emergencyContact
                    )
                    repository.savePatient(p)
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text("Save Profile Changes", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RescheduleDialog(
    appointment: AppointmentEntity,
    doctors: List<DoctorEntity>,
    repository: ClinicRepository,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onRescheduled: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val doctor = doctors.firstOrNull { it.id == appointment.doctorId } ?: doctors.firstOrNull()

    var newDate by remember { mutableStateOf(appointment.appointmentDate) }
    var newSlot by remember { mutableStateOf("") }
    var slots by remember { mutableStateOf<List<SlotInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(doctor, newDate) {
        if (doctor != null) {
            isLoading = true
            slots = repository.getAvailableSlots(doctor, newDate)
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Appointment") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select new date and available slot:")
                Spacer(modifier = Modifier.height(8.dp))

                // Simple date choice chips (Today, Tomorrow, +2d)
                val cal = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dates = (0..3).map {
                    val d = sdf.format(cal.time)
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    d
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dates.forEach { d ->
                        FilterChip(
                            selected = newDate == d,
                            onClick = { newDate = d },
                            label = { Text(d.substring(5)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    LazyColumn(modifier = Modifier.height(140.dp)) {
                        items(slots.filter { it.isAvailable }) { slot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { newSlot = slot.timeSlot }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = newSlot == slot.timeSlot, onClick = { newSlot = slot.timeSlot })
                                Text(slot.timeSlot)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = newSlot.isNotEmpty() && doctor != null,
                onClick = {
                    if (doctor != null && newSlot.isNotEmpty()) {
                        scope.launch {
                            val res = repository.rescheduleAppointment(
                                oldAppointmentId = appointment.id,
                                newDoctorId = doctor.id,
                                newDate = newDate,
                                newSlot = newSlot
                            )
                            if (res.isSuccess) {
                                onRescheduled()
                            } else {
                                Toast.makeText(context, "Slot unavailable, choose another.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Confirm Reschedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
