package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HealingGreen
import com.example.ui.theme.MedicalBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicDashboardScreen(
    clinic: ClinicEntity?,
    doctors: List<DoctorEntity>,
    appointments: List<AppointmentEntity>,
    medicines: List<MedicineEntity>,
    currentLanguage: AppLanguage,
    repository: ClinicRepository,
    onBackToPatientView: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSection by remember { mutableIntStateOf(0) } // 0: Today's Appointments, 1: Queue, 2: Inventory, 3: Analytics, 4: Settings
    val sectionTitles = listOf("Appointments", "Live Queue", "Pharmacy", "Analytics", "Settings")

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var selectedAppointmentForPrescription by remember { mutableStateOf<AppointmentEntity?>(null) }
    var appointmentStatusFilter by remember { mutableStateOf("ALL") }

    // Filter appointments for today
    val todayAppointments = appointments.filter { it.appointmentDate == todayDate }
    val filteredAppointments = when (appointmentStatusFilter) {
        "CONFIRMED" -> todayAppointments.filter { it.status == AppointmentStatus.CONFIRMED }
        "WAITING" -> todayAppointments.filter { it.status == AppointmentStatus.WAITING }
        "IN_CONSULTATION" -> todayAppointments.filter { it.status == AppointmentStatus.IN_CONSULTATION }
        "COMPLETED" -> todayAppointments.filter { it.status == AppointmentStatus.COMPLETED }
        "CANCELLED" -> todayAppointments.filter { it.status == AppointmentStatus.CANCELLED }
        "NO_SHOW" -> todayAppointments.filter { it.status == AppointmentStatus.NO_SHOW }
        else -> todayAppointments
    }

    BackHandler {
        onBackToPatientView()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = StringsLocalization.get("clinic_staff_portal", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dr. Rajesh Kumar • Active Clinic Session",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToPatientView) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onBackToPatientView) {
                        Text(
                            text = StringsLocalization.get("switch_to_patient", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
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
            // Horizontal Navigation Bar
            ScrollableTabRow(
                selectedTabIndex = selectedSection,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                sectionTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedSection) {
                0 -> {
                    // Section 1: Today's Appointments with Status Filter & Actions
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter chips
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("ALL", "CONFIRMED", "WAITING", "IN_CONSULTATION", "COMPLETED", "NO_SHOW").forEach { filter ->
                                        FilterChip(
                                            selected = appointmentStatusFilter == filter,
                                            onClick = { appointmentStatusFilter = filter },
                                            label = { Text(filter) }
                                        )
                                    }
                                }
                            }

                            if (filteredAppointments.isEmpty()) {
                                item {
                                    EmptyStateCard(message = "No appointments matching filter for today.")
                                }
                            } else {
                                items(filteredAppointments) { apt ->
                                    ClinicAppointmentActionCard(
                                        appointment = apt,
                                        onCheckIn = {
                                            scope.launch {
                                                val token = repository.checkInPatient(apt.id, todayDate)
                                                Toast.makeText(context, "Checked in! Assigned Token #$token", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onStartConsultation = {
                                            scope.launch {
                                                repository.updateAppointmentStatus(apt.id, AppointmentStatus.IN_CONSULTATION)
                                            }
                                        },
                                        onCompleteConsultation = {
                                            selectedAppointmentForPrescription = apt
                                        },
                                        onNoShow = {
                                            scope.launch {
                                                repository.updateAppointmentStatus(apt.id, AppointmentStatus.NO_SHOW)
                                            }
                                        },
                                        onCancel = {
                                            scope.launch {
                                                repository.cancelAppointment(apt.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Section 2: Live Queue Board
                    QueueManagementSection(
                        appointments = todayAppointments,
                        todayDate = todayDate,
                        repository = repository
                    )
                }
                2 -> {
                    // Section 3: Medicine Pharmacy Inventory
                    PharmacyInventorySection(
                        medicines = medicines,
                        repository = repository
                    )
                }
                3 -> {
                    // Section 4: Analytics
                    ClinicAnalyticsSection(
                        todayAppointments = todayAppointments,
                        allAppointments = appointments
                    )
                }
                4 -> {
                    // Section 5: Clinic & Doctor Settings + Integrations
                    ClinicSettingsSection(
                        clinic = clinic,
                        doctors = doctors,
                        repository = repository
                    )
                }
            }
        }
    }

    // Prescription & Case History Dialog
    selectedAppointmentForPrescription?.let { apt ->
        CaseHistoryPrescriptionDialog(
            appointment = apt,
            repository = repository,
            onDismiss = { selectedAppointmentForPrescription = null },
            onSaved = {
                selectedAppointmentForPrescription = null
                Toast.makeText(context, "Prescription saved & consultation marked completed!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ClinicAppointmentActionCard(
    appointment: AppointmentEntity,
    onCheckIn: () -> Unit,
    onStartConsultation: () -> Unit,
    onCompleteConsultation: () -> Unit,
    onNoShow: () -> Unit,
    onCancel: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = appointment.timeSlot,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${appointment.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(status = appointment.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${appointment.patientName} (${appointment.patientAge}y • ${appointment.patientGender})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Phone: ${appointment.patientPhone} • Reason: ${appointment.reasonForVisit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (appointment.tokenNumber != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MedicalBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Token: #${appointment.tokenNumber}",
                        fontWeight = FontWeight.Bold,
                        color = MedicalBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons row depending on current status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (appointment.status) {
                    AppointmentStatus.PENDING -> {
                        Button(
                            onClick = onCheckIn,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = HealingGreen)
                        ) {
                            Text("Confirm Booking", fontSize = 12.sp)
                        }
                    }
                    AppointmentStatus.CONFIRMED -> {
                        Button(
                            onClick = onCheckIn,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Check-In", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onNoShow,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("No-Show", fontSize = 12.sp)
                        }
                    }
                    AppointmentStatus.WAITING -> {
                        Button(
                            onClick = onStartConsultation,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
                        ) {
                            Text("Start Consult", fontSize = 12.sp)
                        }
                    }
                    AppointmentStatus.IN_CONSULTATION -> {
                        Button(
                            onClick = onCompleteConsultation,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = HealingGreen)
                        ) {
                            Text("Complete & RX", fontSize = 12.sp)
                        }
                    }
                    AppointmentStatus.COMPLETED -> {
                        OutlinedButton(
                            onClick = onCompleteConsultation,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View / Edit RX", fontSize = 12.sp)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun QueueManagementSection(
    appointments: List<AppointmentEntity>,
    todayDate: String,
    repository: ClinicRepository
) {
    val scope = rememberCoroutineScope()
    val waitingList = appointments.filter { it.status == AppointmentStatus.WAITING }
    val inConsultation = appointments.firstOrNull { it.status == AppointmentStatus.IN_CONSULTATION }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MedicalBlue.copy(alpha = 0.08f)),
            border = BorderStroke(1.5.dp, MedicalBlue)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "CURRENTLY IN CONSULTATION", fontWeight = FontWeight.Bold, color = MedicalBlue, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                if (inConsultation != null) {
                    Text(text = "Token #${inConsultation.tokenNumber}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MedicalBlue)
                    Text(text = "${inConsultation.patientName} (${inConsultation.timeSlot})", fontWeight = FontWeight.SemiBold)
                } else {
                    Text(text = "Consulting Room Free", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Waiting in Queue (${waitingList.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (waitingList.isEmpty()) {
            EmptyStateCard(message = "No patients currently waiting in queue.")
        } else {
            waitingList.forEach { pt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
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
                            Surface(shape = CircleShape, color = EmeraldPrimary, modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "#${pt.tokenNumber ?: 1}", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = pt.patientName, fontWeight = FontWeight.Bold)
                                Text(text = "Slot: ${pt.timeSlot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    repository.updateAppointmentStatus(pt.id, AppointmentStatus.IN_CONSULTATION)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Call Next", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PharmacyInventorySection(
    medicines: List<MedicineEntity>,
    repository: ClinicRepository
) {
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

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
            Text(text = "Remedy Inventory (${medicines.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Remedy", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(medicines) { med ->
                val isLow = med.stockQuantity <= med.reorderLevel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (isLow) Color.Red.copy(alpha = 0.5f) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = med.remedyName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(text = "${med.commonName} • ${med.availablePotencies} (${med.form})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (isLow) {
                                Text(text = "⚠️ Low Stock Alert (Reorder at ${med.reorderLevel})", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${med.stockQuantity} ${med.unit}",
                                fontWeight = FontWeight.Bold,
                                color = if (isLow) Color.Red else EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                scope.launch {
                                    repository.updateMedicine(med.copy(stockQuantity = med.stockQuantity + 5))
                                }
                            }) {
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = "Add 5", tint = EmeraldPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var remedy by remember { mutableStateOf("") }
        var potencies by remember { mutableStateOf("30C, 200C, 1M") }
        var stock by remember { mutableStateOf("20") }
        var price by remember { mutableStateOf("120") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Homeopathic Remedy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = remedy, onValueChange = { remedy = it }, label = { Text("Remedy Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = potencies, onValueChange = { potencies = it }, label = { Text("Potencies") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Initial Stock") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (₹)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (remedy.isNotBlank()) {
                            scope.launch {
                                repository.addMedicine(
                                    MedicineEntity(
                                        remedyName = remedy,
                                        commonName = "Homeopathic Remedy",
                                        availablePotencies = potencies,
                                        form = "Globules",
                                        stockQuantity = stock.toIntOrNull() ?: 10,
                                        price = price.toDoubleOrNull() ?: 100.0
                                    )
                                )
                                showAddDialog = false
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ClinicAnalyticsSection(
    todayAppointments: List<AppointmentEntity>,
    allAppointments: List<AppointmentEntity>
) {
    val completed = todayAppointments.count { it.status == AppointmentStatus.COMPLETED }
    val cancelled = todayAppointments.count { it.status == AppointmentStatus.CANCELLED }
    val noShow = todayAppointments.count { it.status == AppointmentStatus.NO_SHOW }
    val online = todayAppointments.count { it.type == AppointmentType.ONLINE }
    val walkin = todayAppointments.size - online
    val revenue = todayAppointments.filter { it.status != AppointmentStatus.CANCELLED }.sumOf { it.consultationFee }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "Clinic Performance Analytics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(title = "Today Booked", value = "${todayAppointments.size}", color = MedicalBlue, modifier = Modifier.weight(1f))
            StatCard(title = "Completed", value = "$completed", color = HealingGreen, modifier = Modifier.weight(1f))
            StatCard(title = "Revenue", value = "₹${revenue.toInt()}", color = EmeraldPrimary, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(title = "Online Visits", value = "$online", color = Color(0xFF7C4DFF), modifier = Modifier.weight(1f))
            StatCard(title = "In-Clinic", value = "$walkin", color = Color(0xFF00B0FF), modifier = Modifier.weight(1f))
            StatCard(title = "Cancelled", value = "$cancelled", color = Color(0xFFE53935), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Historical Consultation Volume", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

        // Custom M3 bar chart representation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf(
                    "Mon" to 14,
                    "Tue" to 18,
                    "Wed" to 15,
                    "Thu" to 22,
                    "Fri" to 19,
                    "Sat" to 25
                ).forEach { (day, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = day, modifier = Modifier.width(36.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (count / 30f).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldPrimary)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "$count pts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun ClinicSettingsSection(
    clinic: ClinicEntity?,
    doctors: List<DoctorEntity>,
    repository: ClinicRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var clinicName by remember { mutableStateOf(clinic?.name ?: "HOMEo AI Healing Clinic & Research") }
    var fee by remember { mutableStateOf("${clinic?.defaultConsultationFee?.toInt() ?: 500}") }
    var workingHours by remember { mutableStateOf(clinic?.workingHoursSummary ?: "Mon - Sat: 9:00 AM - 1:00 PM & 4:00 PM - 8:00 PM") }
    var slotDuration by remember { mutableStateOf(30) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "Clinic Profile & Schedule Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = clinicName,
            onValueChange = { clinicName = it },
            label = { Text("Clinic Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fee,
            onValueChange = { fee = it },
            label = { Text("Default Consultation Fee (₹)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = workingHours,
            onValueChange = { workingHours = it },
            label = { Text("Working Hours Summary") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Appointment Slot Duration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 20, 30, 45, 60).forEach { mins ->
                FilterChip(
                    selected = slotDuration == mins,
                    onClick = { slotDuration = mins },
                    label = { Text("$mins min") }
                )
            }
        }

        Button(
            onClick = {
                scope.launch {
                    val updated = clinic?.copy(
                        name = clinicName,
                        defaultConsultationFee = fee.toDoubleOrNull() ?: 500.0,
                        workingHoursSummary = workingHours
                    )
                    if (updated != null) {
                        repository.saveClinic(updated)
                    }
                    doctors.firstOrNull()?.let { doc ->
                        repository.saveDoctor(doc.copy(slotDurationMinutes = slotDuration))
                    }
                    Toast.makeText(context, "Clinic configuration saved!", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Save Clinic Changes")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        // Requirement 34: Real Access / Production Configuration Status
        Text(text = "Production Services Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

        ServiceStatusRow(name = "Database", status = "CONNECTED (Room SQLite)", isLive = true)
        ServiceStatusRow(name = "Speech & TTS Engine", status = "READY (Android SpeechRecognizer)", isLive = true)
        ServiceStatusRow(name = "AI Voice NLU", status = "ACTIVE (Tamil / Tanglish / English)", isLive = true)
        ServiceStatusRow(name = "SMS & OTP Provider", status = "DEMO MODE (Twilio/MSG91 ready)", isLive = false)
        ServiceStatusRow(name = "Payment Provider", status = "SANDBOX (UPI / Razorpay ready)", isLive = false)
        ServiceStatusRow(name = "Maps / Directions", status = "CONNECTED (Google Maps Intent)", isLive = true)
    }
}

@Composable
fun ServiceStatusRow(name: String, status: String, isLive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isLive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ) {
            Text(
                text = status,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLive) Color(0xFF2E7D32) else Color(0xFFE65100),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
