package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import com.example.ui.components.AppShareAndScannerDialog
import com.example.ui.components.Orb3DView
import com.example.ui.components.QrCodeView
import com.example.ui.theme.EmeraldPrimary
import com.example.util.ContactUtils
import com.example.ui.theme.HealingGreen
import com.example.ui.theme.MedicalBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientPortalScreen(
    currentUser: UserEntity,
    clinic: ClinicEntity?,
    doctors: List<DoctorEntity>,
    services: List<ServiceEntity>,
    appointments: List<AppointmentEntity>, // Already filtered for this client!
    notifications: List<NotificationEntity>, // Already filtered for this client!
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    repository: ClinicRepository,
    onBookAppointmentClick: (DoctorEntity?) -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showShareAndScannerModal by remember { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    // 0: Home, 1: My Appointments, 2: Notifications, 3: Profile, 4: Contact Clinic

    val tabList = listOf(
        "Home" to Icons.Filled.Home,
        "My Appointments" to Icons.Filled.CalendarMonth,
        "Notifications" to Icons.Filled.Notifications,
        "Profile" to Icons.Filled.Person,
        "Contact" to Icons.Filled.ContactPhone
    )

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Client's appointments
    val upcoming = appointments.filter { it.status != AppointmentStatus.COMPLETED && it.status != AppointmentStatus.CANCELLED }
    val completed = appointments.filter { it.status == AppointmentStatus.COMPLETED }
    val cancelled = appointments.filter { it.status == AppointmentStatus.CANCELLED }
    val todayActive = upcoming.firstOrNull { it.appointmentDate == todayDate }

    var selectedQrAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }
    var selectedRescheduleAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }
    var selectedCancelAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

    // Intercept back button to return to Client Home or confirm logout
    BackHandler {
        if (selectedTabIndex != 0) {
            selectedTabIndex = 0
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
                            color = EmeraldPrimary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HOMEo AI",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Welcome, ${currentUser.fullName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
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

                    // App URL & QR Scanner
                    IconButton(onClick = { showShareAndScannerModal = true }) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "App URL & Scanner",
                            tint = EmeraldPrimary
                        )
                    }

                    // Logout Icon
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabList.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            if (label == "Notifications" && notifications.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text("${notifications.size}") } }) {
                                    Icon(icon, contentDescription = label)
                                }
                            } else {
                                Icon(icon, contentDescription = label)
                            }
                        },
                        label = { Text(label, fontSize = 11.sp, maxLines = 1) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { onBookAppointmentClick(doctors.firstOrNull()) },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(StringsLocalization.get("book_appointment", currentLanguage), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> ClientHomeScreen(
                    currentUser = currentUser,
                    clinic = clinic,
                    todayActiveAppointment = todayActive,
                    upcomingCount = upcoming.size,
                    currentLanguage = currentLanguage,
                    onBookClick = { onBookAppointmentClick(doctors.firstOrNull()) },
                    onOpenVoiceAssistant = onOpenVoiceAssistant,
                    onViewMyBookings = { selectedTabIndex = 1 },
                    onCheckIn = { apt ->
                        scope.launch {
                            val token = repository.checkInPatient(apt.id, todayDate)
                            Toast.makeText(context, "Checked in! Token #${token}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                1 -> ClientMyAppointmentsScreen(
                    appointments = appointments,
                    currentLanguage = currentLanguage,
                    onViewQr = { selectedQrAppointment = it },
                    onReschedule = { selectedRescheduleAppointment = it },
                    onCancel = { selectedCancelAppointment = it },
                    onBookNew = { onBookAppointmentClick(doctors.firstOrNull()) }
                )
                2 -> ClientNotificationsScreen(notifications = notifications)
                3 -> ClientProfileScreen(
                    currentUser = currentUser,
                    repository = repository
                )
                4 -> ClientContactScreen(
                    clinic = clinic,
                    currentUser = currentUser,
                    repository = repository
                )
            }
        }
    }

    // QR Code Modal
    selectedQrAppointment?.let { apt ->
        AlertDialog(
            onDismissRequest = { selectedQrAppointment = null },
            title = { Text("Reception Check-in QR") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    QrCodeView(data = apt.id, size = 180.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Booking #${apt.id}", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    Text(text = "${apt.doctorName} • ${apt.appointmentDate} at ${apt.timeSlot}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedQrAppointment = null }) { Text("Close") }
            }
        )
    }

    // Reschedule Modal
    selectedRescheduleAppointment?.let { apt ->
        RescheduleDialog(
            appointment = apt,
            doctors = doctors,
            repository = repository,
            currentLanguage = currentLanguage,
            onDismiss = { selectedRescheduleAppointment = null },
            onRescheduled = {
                selectedRescheduleAppointment = null
                Toast.makeText(context, "Reschedule request submitted successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Cancel Modal
    selectedCancelAppointment?.let { apt ->
        AlertDialog(
            onDismissRequest = { selectedCancelAppointment = null },
            title = { Text("Cancel Appointment?") },
            text = { Text("Are you sure you want to cancel booking #${apt.id}? This slot will be released back to the clinic.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.cancelAppointment(apt.id)
                            selectedCancelAppointment = null
                            Toast.makeText(context, "Appointment cancelled.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Booking")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCancelAppointment = null }) { Text("Keep") }
            }
        )
    }

    if (showShareAndScannerModal) {
        AppShareAndScannerDialog(
            repository = repository,
            initialTab = 0,
            onDismiss = { showShareAndScannerModal = false }
        )
    }
}

// -------------------------------------------------------------
// CLIENT TAB 0: HOME
// -------------------------------------------------------------
@Composable
fun ClientHomeScreen(
    currentUser: UserEntity,
    clinic: ClinicEntity?,
    todayActiveAppointment: AppointmentEntity?,
    upcomingCount: Int,
    currentLanguage: AppLanguage,
    onBookClick: () -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onViewMyBookings: () -> Unit,
    onCheckIn: (AppointmentEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Greeting Card with 3D Orb
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Orb3DView(size = 64.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${currentUser.fullName.substringBefore(" ")}!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Need homeopathic care? Schedule your visit with our senior doctors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Queue Card for Today (if scheduled today)
        if (todayActiveAppointment != null) {
            item {
                LiveQueueCard(
                    appointment = todayActiveAppointment,
                    currentLanguage = currentLanguage,
                    onCheckInClick = { onCheckIn(todayActiveAppointment) }
                )
            }
        }

        // Quick Booking Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBookClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = StringsLocalization.get("book_appointment", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Choose doctor, date, and live available slot",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }

        // 3D Voice Booking Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVoiceAssistant() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalBlue.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = MedicalBlue, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Voice Assistant Booking",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MedicalBlue
                        )
                        Text(
                            text = "\"Innaikku evening appointment book pannu\" (Tamil / Tanglish / EN)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Upcoming Consultations", fontWeight = FontWeight.Bold)
                        Text(text = "$upcomingCount active appointments scheduled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onViewMyBookings) {
                        Text("View All", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CLIENT TAB 1: MY APPOINTMENTS
// -------------------------------------------------------------
@Composable
fun ClientMyAppointmentsScreen(
    appointments: List<AppointmentEntity>,
    currentLanguage: AppLanguage,
    onViewQr: (AppointmentEntity) -> Unit,
    onReschedule: (AppointmentEntity) -> Unit,
    onCancel: (AppointmentEntity) -> Unit,
    onBookNew: () -> Unit
) {
    var filterTab by remember { mutableIntStateOf(0) } // 0: Upcoming, 1: Completed, 2: Cancelled

    val list = when (filterTab) {
        0 -> appointments.filter { it.status != AppointmentStatus.COMPLETED && it.status != AppointmentStatus.CANCELLED }
        1 -> appointments.filter { it.status == AppointmentStatus.COMPLETED }
        else -> appointments.filter { it.status == AppointmentStatus.CANCELLED }
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
            Text(text = "My Appointments", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onBookNew,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Book New", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filterTab == 0, onClick = { filterTab = 0 }, label = { Text("Upcoming") })
            FilterChip(selected = filterTab == 1, onClick = { filterTab = 1 }, label = { Text("Completed") })
            FilterChip(selected = filterTab == 2, onClick = { filterTab = 2 }, label = { Text("Cancelled") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (list.isEmpty()) {
            EmptyStateCard(
                message = "No appointments in this section.",
                actionLabel = "Book an Appointment",
                onActionClick = onBookNew
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list) { apt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                Text(text = "Appointment #${apt.id}", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                StatusChip(status = apt.status)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = apt.doctorName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Date: ${apt.appointmentDate} at ${apt.timeSlot}", fontWeight = FontWeight.Medium)
                            Text(text = "Type: ${if (apt.type.name == "ONLINE") "Online Video Room" else "In-Clinic Visit"}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Reason: ${apt.reasonForVisit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (apt.adminNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Doctor / Reception Note: ${apt.adminNotes}", style = MaterialTheme.typography.labelSmall, color = MedicalBlue)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onViewQr(apt) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Show QR", fontSize = 11.sp)
                                }

                                if (apt.status != AppointmentStatus.COMPLETED && apt.status != AppointmentStatus.CANCELLED) {
                                    OutlinedButton(
                                        onClick = { onReschedule(apt) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reschedule", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { onCancel(apt) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CLIENT TAB 2: NOTIFICATIONS
// -------------------------------------------------------------
@Composable
fun ClientNotificationsScreen(notifications: List<NotificationEntity>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Notifications & Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))

        if (notifications.isEmpty()) {
            EmptyStateCard(message = "No notifications yet.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = notif.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = notif.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CLIENT TAB 3: PROFILE
// -------------------------------------------------------------
@Composable
fun ClientProfileScreen(
    currentUser: UserEntity,
    repository: ClinicRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(currentUser.fullName) }
    var phone by remember { mutableStateOf(currentUser.phone) }
    var email by remember { mutableStateOf(currentUser.email) }
    var dob by remember { mutableStateOf(currentUser.dateOfBirth) }
    var address by remember { mutableStateOf(currentUser.address) }
    var emergencyContact by remember { mutableStateOf(currentUser.emergencyContact) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "My Personal Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(text = "Ensure your contact info is correct for medicine deliveries & reminders.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Phone") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Residential Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        OutlinedTextField(value = emergencyContact, onValueChange = { emergencyContact = it }, label = { Text("Emergency Contact") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                scope.launch {
                    val updated = currentUser.copy(
                        fullName = name,
                        phone = phone,
                        email = email,
                        dateOfBirth = dob,
                        address = address,
                        emergencyContact = emergencyContact
                    )
                    repository.updateUser(updated)
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Save Profile Changes", fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------
// CLIENT TAB 4: CONTACT CLINIC & ENQUIRIES
// -------------------------------------------------------------
@Composable
fun ClientContactScreen(
    clinic: ClinicEntity?,
    currentUser: UserEntity,
    repository: ClinicRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    val clinicPhone = clinic?.phone.orEmpty()
    val whatsAppNumber = clinic?.whatsAppNumber.orEmpty()
    val clinicAddress = clinic?.address.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Contact Clinic & Helpdesk", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    ContactUtils.callClinic(context, clinicPhone)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call Clinic")
            }

            Button(
                onClick = {
                    ContactUtils.openWhatsApp(context, whatsAppNumber.ifBlank { clinicPhone })
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("WhatsApp")
            }

            OutlinedButton(
                onClick = {
                    ContactUtils.openDirections(context, clinicAddress)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Directions")
            }
        }

        if (clinicPhone.isNotBlank()) {
            Surface(
                color = EmeraldPrimary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { ContactUtils.callClinic(context, clinicPhone) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clinic Phone: $clinicPhone",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Tap to call", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Send an Enquiry to the Doctor", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

        if (isSubmitted) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F5E9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "✓ Enquiry Submitted Successfully", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text(text = "Our clinic team has received your query and will reply shortly.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject / Concern") },
                placeholder = { Text("e.g. Diet restrictions with remedies, dosage query") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message / Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    if (subject.isNotBlank() && message.isNotBlank()) {
                        scope.launch {
                            repository.submitEnquiry(
                                clientId = currentUser.id,
                                name = currentUser.fullName,
                                phone = currentUser.phone,
                                email = currentUser.email,
                                subject = subject,
                                message = message
                            )
                            isSubmitted = true
                            Toast.makeText(context, "Enquiry dispatched to clinic admin.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Submit Message to Clinic")
            }
        }
    }
}
