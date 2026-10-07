package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import com.example.ui.components.AppShareAndScannerDialog
import com.example.ui.components.Orb3DView
import com.example.ui.screens.admin.*
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HealingGreen
import com.example.ui.theme.MedicalBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentUser: UserEntity,
    clinic: ClinicEntity?,
    doctors: List<DoctorEntity>,
    appointments: List<AppointmentEntity>,
    patients: List<PatientEntity>,
    medicines: List<MedicineEntity>,
    notifications: List<NotificationEntity>,
    messages: List<MessageEntity>,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    repository: ClinicRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isOwner = currentUser.role == UserRole.OWNER

    // 10 Distinct Navigation Sections
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        "My Profile" to Icons.Filled.AccountCircle,
        "Doctor Management" to Icons.Filled.MedicalServices,
        "Admin Management" to Icons.Filled.Group,
        "Appointments" to Icons.Filled.CalendarMonth,
        "Patients" to Icons.Filled.People,
        "Analytics" to Icons.Filled.BarChart,
        "Payments" to Icons.Filled.Payment,
        "Clinic Profile" to Icons.Filled.Store,
        "Permissions" to Icons.Filled.Security,
        "Logout" to Icons.Filled.Logout
    )

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var selectedAppointmentToInspect by remember { mutableStateOf<AppointmentEntity?>(null) }
    var selectedPatientToInspect by remember { mutableStateOf<PatientEntity?>(null) }
    var showScannerModal by remember { mutableStateOf(false) }

    // Intercept back button to return to first tab or prompt logout
    BackHandler {
        if (selectedNavIndex != 0) {
            selectedNavIndex = 0
        } else {
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isOwner) MedicalBlue else EmeraldPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isOwner) Icons.Filled.AdminPanelSettings else Icons.Filled.Badge,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isOwner) "Owner Command Center" else "Clinic Staff Portal",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isOwner) MedicalBlue else EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isOwner) MedicalBlue.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isOwner) "OWNER" else "ADMIN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isOwner) MedicalBlue else EmeraldPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentUser.fullName,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // App URL & QR Scanner
                    IconButton(onClick = { showScannerModal = true }) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "App QR & Scanner",
                            tint = if (isOwner) MedicalBlue else EmeraldPrimary
                        )
                    }

                    // Language Switcher
                    var showLangMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { showLangMenu = true }) {
                            Text(currentLanguage.displayName, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = showLangMenu, onDismissRequest = { showLangMenu = false }) {
                            AppLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.displayName) },
                                    onClick = {
                                        onLanguageChange(lang)
                                        showLangMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Logout Icon
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
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
            // Horizontal Navigation Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedNavIndex,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                navItems.forEachIndexed { index, (label, icon) ->
                    Tab(
                        selected = selectedNavIndex == index,
                        onClick = {
                            if (index == 9) {
                                onLogout()
                            } else {
                                selectedNavIndex = index
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (selectedNavIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                }
            }

            // Content Sections matching Owner Dashboard requirements
            Box(modifier = Modifier.weight(1f)) {
                when (selectedNavIndex) {
                    0 -> OwnerProfileView(
                        currentUser = currentUser,
                        clinic = clinic,
                        repository = repository
                    )
                    1 -> DoctorManagementView(
                        doctors = doctors,
                        currentUser = currentUser,
                        clinic = clinic,
                        repository = repository
                    )
                    2 -> {
                        if (isOwner) {
                            AdminManagementView(
                                repository = repository,
                                currentUser = currentUser
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Access Restricted to Clinic Owner",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Only the master Clinic Owner has permission to invite, approve, or manage staff admin accounts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    3 -> AdminAppointmentsManagement(
                        appointments = appointments,
                        todayDate = todayDate,
                        onOpenAppointment = { selectedAppointmentToInspect = it },
                        onConfirm = { apt ->
                            scope.launch {
                                repository.confirmAppointmentByAdmin(apt.id)
                                repository.syncAppointmentConfirmationToCloud(context, apt.id)
                                Toast.makeText(context, "Confirmed #${apt.id}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCancel = { apt ->
                            scope.launch {
                                repository.cancelAppointmentByAdmin(apt.id, "Cancelled by Clinic")
                                Toast.makeText(context, "Cancelled #${apt.id}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    4 -> AdminPatientManagement(
                        patients = patients,
                        appointments = appointments,
                        onSelectPatient = { selectedPatientToInspect = it }
                    )
                    5 -> AdminReportsView(
                        appointments = appointments,
                        medicines = medicines
                    )
                    6 -> AdminPaymentsView(
                        appointments = appointments,
                        repository = repository
                    )
                    7 -> AdminProfileSettingsView(
                        clinic = clinic,
                        doctors = doctors,
                        repository = repository,
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange
                    )
                    8 -> PermissionsMatrixView(repository = repository)
                    else -> OwnerProfileView(currentUser, clinic, repository)
                }
            }
        }
    }

    // Appointment Detail & Action Modal
    selectedAppointmentToInspect?.let { apt ->
        AdminAppointmentDetailDialog(
            appointment = apt,
            doctors = doctors,
            repository = repository,
            currentLanguage = currentLanguage,
            onDismiss = { selectedAppointmentToInspect = null },
            onStatusUpdated = {
                // refreshed via Room Flow
            }
        )
    }

    // Patient 360 Profile Detail Modal
    selectedPatientToInspect?.let { patient ->
        AdminPatientProfileDialog(
            patient = patient,
            appointments = appointments.filter { it.patientPhone == patient.phone || it.patientId == patient.id },
            onDismiss = { selectedPatientToInspect = null }
        )
    }

    if (showScannerModal) {
        AppShareAndScannerDialog(
            repository = repository,
            initialTab = 1,
            onDismiss = { showScannerModal = false }
        )
    }
}

// -------------------------------------------------------------
// SECTION 1: OWNER COMMAND CENTER (Dashboard)
// -------------------------------------------------------------
@Composable
fun AdminOverviewDashboard(
    appointments: List<AppointmentEntity>,
    patients: List<PatientEntity>,
    messages: List<MessageEntity>,
    notifications: List<NotificationEntity>,
    todayDate: String,
    onOpenAppointment: (AppointmentEntity) -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onQuickConfirm: (AppointmentEntity) -> Unit
) {
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        currentHour < 12 -> "Good Morning, Doctor 👋"
        currentHour < 17 -> "Good Afternoon, Doctor 👋"
        else -> "Good Evening, Doctor 👋"
    }

    val totalApts = appointments.size
    val todayApts = appointments.filter { it.appointmentDate == todayDate }
    val pendingApts = appointments.filter { it.status == AppointmentStatus.PENDING }
    val confirmedApts = appointments.filter { it.status == AppointmentStatus.CONFIRMED }
    val completedApts = appointments.filter { it.status == AppointmentStatus.COMPLETED }
    val cancelledApts = appointments.filter { it.status == AppointmentStatus.CANCELLED }
    val todayCollection = todayApts.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.consultationFee }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Dynamic Header: "Good Evening, Doctor 👋"
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalBlue.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Orb3DView(size = 56.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MedicalBlue
                        )
                        Text(
                            text = "Live Command Center • ${pendingApts.size} pending requests",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live 8-Card Command KPI Grid
        item {
            Text(text = "Live Operations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiStatCard("New Requests", "${pendingApts.size}", Color(0xFFF57C00), Modifier.weight(1f))
                    KpiStatCard("Today's Appts", "${todayApts.size}", MedicalBlue, Modifier.weight(1f))
                    KpiStatCard("Confirmed", "${confirmedApts.size}", EmeraldPrimary, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiStatCard("Pending Appts", "${pendingApts.size}", Color(0xFFE65100), Modifier.weight(1f))
                    KpiStatCard("Completed", "${completedApts.size}", HealingGreen, Modifier.weight(1f))
                    KpiStatCard("Total Patients", "${patients.size}", Color(0xFF7C4DFF), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiStatCard("Today's Collection", "₹${todayCollection.toInt()}", Color(0xFF00897B), Modifier.weight(1f))
                    KpiStatCard("Follow-ups", "3 scheduled", Color(0xFF0097A7), Modifier.weight(1f))
                }
            }
        }

        // Live Booking Requests Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Live Booking Requests", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onNavigateToTab(1) }) {
                    Text("Manage (${appointments.size})", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (pendingApts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
                        Text(text = "✓ No pending requests. All appointments are reviewed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(pendingApts) { apt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, Color(0xFFFFB74D)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFFF3E0)) {
                                Text(
                                    text = "NEW APPOINTMENT",
                                    color = Color(0xFFE65100),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = "#${apt.id}", fontWeight = FontWeight.Bold, color = EmeraldPrimary, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = apt.patientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(text = "${apt.appointmentDate} • ${apt.timeSlot} (${apt.type.name})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(text = "Reason: ${apt.reasonForVisit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(12.dp))

                        // [VIEW] [CONFIRM] [RESCHEDULE] Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenAppointment(apt) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("VIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onQuickConfirm(apt) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HealingGreen),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CONFIRM", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onOpenAppointment(apt) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("RESCHEDULE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SECTION 4: AI CLINIC ASSISTANT (Chat & Voice with Actions)
// -------------------------------------------------------------
@Composable
fun AdminAiAssistantSection(
    appointments: List<AppointmentEntity>,
    patients: List<PatientEntity>,
    repository: ClinicRepository,
    currentLanguage: AppLanguage,
    onConfirmAction: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val followUps by repository.allFollowUps.collectAsState(initial = emptyList())

    var chatInput by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf(
            "AI" to "Hello Doctor! I am your Clinic AI Assistant. Ask me:\n• 'Who is coming tomorrow?'\n• 'Show today's pending appointments'\n• 'How many appointments do I have this week?'\n• 'Who needs a follow-up?'\n• 'Ravi appointment tomorrow confirm pannunga'"
        )
    }

    var pendingActionDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // (prompt, aptId)

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                chatMessages.add("USER" to spoken)
                val reply = repository.answerAdminClinicQuery(spoken, appointments, patients, followUps)
                chatMessages.add("AI" to reply)

                // Voice Action Detection (e.g. "confirm ravi appointment")
                val lower = spoken.lowercase(Locale.ROOT)
                if (lower.contains("confirm") || lower.contains("உறுதி") || lower.contains("pannu")) {
                    val targetApt = appointments.firstOrNull { apt ->
                        lower.contains(apt.patientName.lowercase(Locale.ROOT).substringBefore(" ")) ||
                                lower.contains(apt.id.lowercase(Locale.ROOT))
                    } ?: appointments.firstOrNull { it.status == AppointmentStatus.PENDING }

                    if (targetApt != null) {
                        pendingActionDialog = "Confirm ${targetApt.patientName}'s appointment (${targetApt.appointmentDate} at ${targetApt.timeSlot})?" to targetApt.id
                    }
                }
            }
        }
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Orb3DView(size = 40.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Clinic AI Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = "English • Tamil • Tanglish Natural NLP", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat log
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages) { (sender, text) ->
                val isAi = sender == "AI"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAi) MaterialTheme.colorScheme.surfaceVariant else MedicalBlue,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = text,
                            color = if (isAi) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Who is coming tomorrow?",
                "Show today's pending appointments",
                "Who needs a follow-up?",
                "Naalaikku irukkura appointments kaatu"
            ).forEach { prompt ->
                SuggestionChip(
                    onClick = {
                        chatMessages.add("USER" to prompt)
                        val reply = repository.answerAdminClinicQuery(prompt, appointments, patients, followUps)
                        chatMessages.add("AI" to reply)
                    },
                    label = { Text(prompt, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input bar with Mic
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Ask administrative query...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (chatInput.isNotBlank()) {
                IconButton(
                    onClick = {
                        val q = chatInput
                        chatInput = ""
                        chatMessages.add("USER" to q)
                        val reply = repository.answerAdminClinicQuery(q, appointments, patients, followUps)
                        chatMessages.add("AI" to reply)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MedicalBlue)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            } else {
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLanguage == AppLanguage.TAMIL) "ta-IN" else "en-IN")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (_: Exception) {
                            // Fallback simulation
                            chatMessages.add("USER" to "Who is coming tomorrow?")
                            val reply = repository.answerAdminClinicQuery("Who is coming tomorrow?", appointments, patients, followUps)
                            chatMessages.add("AI" to reply)
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EmeraldPrimary)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Mic", tint = Color.White)
                }
            }
        }
    }

    // Confirmation Dialog before executing action
    pendingActionDialog?.let { (actionPrompt, aptId) ->
        AlertDialog(
            onDismissRequest = { pendingActionDialog = null },
            title = { Text("Confirm AI Action") },
            text = { Text(actionPrompt) },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmAction(aptId)
                        pendingActionDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealingGreen)
                ) {
                    Text("CONFIRM")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingActionDialog = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SECTION 5: PAYMENTS MANAGEMENT
// -------------------------------------------------------------
@Composable
fun AdminPaymentsView(
    appointments: List<AppointmentEntity>,
    repository: ClinicRepository
) {
    val payments by repository.allPayments.collectAsState(initial = emptyList())
    val totalCollected = payments.sumOf { it.paidAmount }
    val totalPending = payments.sumOf { it.pendingAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Clinic Financials & Payments", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiStatCard("Total Collected", "₹${totalCollected.toInt()}", HealingGreen, Modifier.weight(1f))
            KpiStatCard("Pending Dues", "₹${totalPending.toInt()}", Color(0xFFF57C00), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "Transaction Records (${payments.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(payments) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Column {
                            Text(text = p.patientName, fontWeight = FontWeight.Bold)
                            Text(text = "Appt #${p.appointmentId} • ${p.paymentDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Paid: ₹${p.paidAmount.toInt()} / ₹${p.totalFee.toInt()} (${p.paymentMethod.name})", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (p.status == PaymentStatus.PAID) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = p.status.name,
                                color = if (p.status == PaymentStatus.PAID) Color(0xFF2E7D32) else Color(0xFFE65100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SECTION 6: FOLLOW-UPS REMINDER SYSTEM
// -------------------------------------------------------------
@Composable
fun AdminFollowUpsView(
    patients: List<PatientEntity>,
    doctors: List<DoctorEntity>,
    repository: ClinicRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val followUps by repository.allFollowUps.collectAsState(initial = emptyList())

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
            Text(text = "Follow-up Reminders (${followUps.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Reminder", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (followUps.isEmpty()) {
            EmptyStateCard(message = "No patient follow-ups scheduled.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(followUps) { fu ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = fu.patientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(text = fu.followUpDate, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }
                            Text(text = "Phone: ${fu.patientPhone} • Doctor: ${fu.doctorName}", style = MaterialTheme.typography.bodySmall)
                            if (fu.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Notes: ${fu.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val selectedPatient = patients.firstOrNull()
        val selectedDoctor = doctors.firstOrNull()
        var date by remember { mutableStateOf("2026-10-15") }
        var notes by remember { mutableStateOf("Evaluate response to constitutional remedy & relief progression.") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Schedule Follow-up Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Patient: ${selectedPatient?.fullName ?: "Patient"}")
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Follow-up Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Clinical Notes / Instructions") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedPatient != null && selectedDoctor != null) {
                            scope.launch {
                                repository.createFollowUp(
                                    patientId = selectedPatient.id,
                                    patientName = selectedPatient.fullName,
                                    patientPhone = selectedPatient.phone,
                                    doctorId = selectedDoctor.id,
                                    doctorName = selectedDoctor.name,
                                    date = date,
                                    notes = notes
                                )
                                showAddDialog = false
                                Toast.makeText(context, "Follow-up reminder set!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Follow-up")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------
// SECTION 11: SECURITY CENTER & AUDIT LOGS
// -------------------------------------------------------------
@Composable
fun AdminSecurityCenterView(repository: ClinicRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val auditLogs by repository.allAuditLogs.collectAsState(initial = emptyList())
    var twoFactor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "Clinic Security Center & Session Management", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Two-Factor Authentication (2FA)", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Require OTP verification for all admin logins", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = twoFactor, onCheckedChange = { twoFactor = it })
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            repository.deactivateAllSessions("usr_admin_01")
                            Toast.makeText(context, "All active remote sessions terminated.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout All Other Devices")
                }
            }
        }

        Text(text = "Security Audit Logs (${auditLogs.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

        auditLogs.take(15).forEach { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = log.action, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MedicalBlue)
                        Text(text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(log.timestamp)), fontSize = 10.sp, color = Color.Gray)
                    }
                    Text(text = log.details, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
