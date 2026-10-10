package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import com.example.ui.screens.*
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.voice.VoiceAssistantManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.widget.Toast

enum class AppScreen {
    WELCOME_3D,
    PUBLIC_CLINIC,
    LOGIN,
    CLIENT_PORTAL,
    ADMIN_PORTAL,
    BOOKING_FLOW,
    CONFIRMATION
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: ClinicRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        repository = ClinicRepository(database.clinicDao(), applicationContext)

        // Ensure database initial data is seeded
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.populateInitialData(database.clinicDao())
        }

        // Start Real-Time Cloud Sync (Firebase Firestore)
        repository.startFirestoreRealtimeSync(this, CoroutineScope(Dispatchers.IO))

        // Handle incoming web booking deep link
        handleWebBookingIntent(intent)

        setContent {
            MyApplicationTheme {
                HomeoApp(repository = repository)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWebBookingIntent(intent)
    }

    private fun handleWebBookingIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "homeoclinic" && uri.host == "appointment") {
            val id = uri.getQueryParameter("id") ?: "HM-${System.currentTimeMillis() % 100000}"
            val name = uri.getQueryParameter("name") ?: "Web Patient"
            val phone = uri.getQueryParameter("phone") ?: "9876543210"
            val age = uri.getQueryParameter("age")?.toIntOrNull() ?: 30
            val gender = uri.getQueryParameter("gender") ?: "Not Specified"
            val date = uri.getQueryParameter("date") ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val time = uri.getQueryParameter("time") ?: "10:00 AM"
            val mode = uri.getQueryParameter("mode") ?: "IN_CLINIC"
            val symptoms = uri.getQueryParameter("symptoms") ?: "Web Booking"

            CoroutineScope(Dispatchers.IO).launch {
                repository.importWebBooking(
                    id = id,
                    patientName = name,
                    patientPhone = phone,
                    patientAge = age,
                    patientGender = gender,
                    date = date,
                    timeSlot = time,
                    consultationType = mode,
                    symptoms = symptoms
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "✓ Web Appointment Received: $name on $date at $time",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeoApp(repository: ClinicRepository) {
    val scope = rememberCoroutineScope()

    // Authenticated User Session
    val currentUser by repository.currentUser.collectAsStateWithLifecycle(initialValue = null)

    // Reactive Shared Data from Room Database
    val clinic by repository.clinic.collectAsStateWithLifecycle(initialValue = null)
    val doctors by repository.allDoctors.collectAsStateWithLifecycle(initialValue = emptyList())
    val services by repository.allServices.collectAsStateWithLifecycle(initialValue = emptyList())
    val allPatients by repository.allPatients.collectAsStateWithLifecycle(initialValue = emptyList())
    val medicines by repository.allMedicines.collectAsStateWithLifecycle(initialValue = emptyList())
    val allMessages by repository.allMessages.collectAsStateWithLifecycle(initialValue = emptyList())

    // Admin-Only Reactive Flows
    val allAppointments by repository.allAppointments.collectAsStateWithLifecycle(initialValue = emptyList())
    val adminNotifications by repository.adminNotifications.collectAsStateWithLifecycle(initialValue = emptyList())

    // Client-Specific Filtered Flows (Strict access control)
    val clientAppointments by repository.getClientAppointments(
        currentUser?.id ?: "guest",
        currentUser?.phone ?: ""
    ).collectAsStateWithLifecycle(initialValue = emptyList())

    val clientNotifications by repository.getClientNotifications(
        currentUser?.id ?: "guest",
        currentUser?.phone ?: ""
    ).collectAsStateWithLifecycle(initialValue = emptyList())

    // Language State
    var currentLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }

    // Screen Navigation State
    var currentScreen by remember { mutableStateOf(AppScreen.WELCOME_3D) }
    var loginInitialTab by remember { mutableIntStateOf(0) }

    // Booking Flow Intermediate State
    var activeDoctorForBooking by remember { mutableStateOf<DoctorEntity?>(null) }
    var activeDateForBooking by remember { mutableStateOf<String?>(null) }
    var activeSlotForBooking by remember { mutableStateOf<String?>(null) }
    var latestConfirmedAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

    // Voice Assistant Dialog State
    var showVoiceAssistant by remember { mutableStateOf(false) }
    val voiceManager = remember { VoiceAssistantManager(repository, scope) }

    // Automatic Role-Based Routing & Route Protection
    LaunchedEffect(currentUser) {
        val user = currentUser
        if (user != null) {
            when (user.role) {
                UserRole.OWNER, UserRole.ADMIN -> currentScreen = AppScreen.ADMIN_PORTAL
                UserRole.CLIENT -> currentScreen = AppScreen.CLIENT_PORTAL
            }
        }
    }

    // Strict Access Control Guard: Never allow Client or Guest on Admin Screen
    if (currentScreen == AppScreen.ADMIN_PORTAL && currentUser?.role != UserRole.OWNER && currentUser?.role != UserRole.ADMIN) {
        currentScreen = if (currentUser?.role == UserRole.CLIENT) AppScreen.CLIENT_PORTAL else AppScreen.LOGIN
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.WELCOME_3D -> {
                Welcome3DScreen(
                    clinic = clinic,
                    currentLanguage = currentLanguage,
                    onEnterClientLogin = {
                        loginInitialTab = 0
                        currentScreen = AppScreen.LOGIN
                    },
                    onEnterOwnerLogin = {
                        loginInitialTab = 1
                        currentScreen = AppScreen.LOGIN
                    },
                    onBrowsePublicClinic = {
                        currentScreen = AppScreen.PUBLIC_CLINIC
                    }
                )
            }

            AppScreen.PUBLIC_CLINIC -> {
                PublicClinicScreen(
                    clinic = clinic,
                    doctors = doctors,
                    services = services,
                    currentLanguage = currentLanguage,
                    repository = repository,
                    onBookAppointmentClick = { doc ->
                        activeDoctorForBooking = doc
                        activeDateForBooking = null
                        activeSlotForBooking = null
                        currentScreen = AppScreen.BOOKING_FLOW
                    },
                    onOpenVoiceAssistant = { showVoiceAssistant = true },
                    onOpenPatientPortal = {
                        if (currentUser != null && currentUser?.role == UserRole.CLIENT) {
                            currentScreen = AppScreen.CLIENT_PORTAL
                        } else {
                            loginInitialTab = 0
                            currentScreen = AppScreen.LOGIN
                        }
                    },
                    onOpenClinicStaffPortal = {
                        if (currentUser != null && (currentUser?.role == UserRole.OWNER || currentUser?.role == UserRole.ADMIN)) {
                            currentScreen = AppScreen.ADMIN_PORTAL
                        } else {
                            loginInitialTab = 1
                            currentScreen = AppScreen.LOGIN
                        }
                    }
                )
            }

            AppScreen.LOGIN -> {
                LoginRoleScreen(
                    repository = repository,
                    currentLanguage = currentLanguage,
                    initialRoleTab = loginInitialTab,
                    onLoginSuccess = { user ->
                        when (user.role) {
                            UserRole.OWNER, UserRole.ADMIN -> currentScreen = AppScreen.ADMIN_PORTAL
                            UserRole.CLIENT -> currentScreen = AppScreen.CLIENT_PORTAL
                        }
                    },
                    onContinueAsGuest = {
                        currentScreen = AppScreen.PUBLIC_CLINIC
                    }
                )
            }

            AppScreen.CLIENT_PORTAL -> {
                currentUser?.let { clientUser ->
                    ClientPortalScreen(
                        currentUser = clientUser,
                        clinic = clinic,
                        doctors = doctors,
                        services = services,
                        appointments = clientAppointments,
                        notifications = clientNotifications,
                        currentLanguage = currentLanguage,
                        onLanguageChange = { currentLanguage = it },
                        repository = repository,
                        onBookAppointmentClick = { doc ->
                            activeDoctorForBooking = doc
                            activeDateForBooking = null
                            activeSlotForBooking = null
                            currentScreen = AppScreen.BOOKING_FLOW
                        },
                        onOpenVoiceAssistant = { showVoiceAssistant = true },
                        onLogout = {
                            repository.logout()
                            currentScreen = AppScreen.WELCOME_3D
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.LOGIN
                }
            }

            AppScreen.ADMIN_PORTAL -> {
                currentUser?.let { adminUser ->
                    AdminDashboardScreen(
                        currentUser = adminUser,
                        clinic = clinic,
                        doctors = doctors,
                        appointments = allAppointments,
                        patients = allPatients,
                        medicines = medicines,
                        notifications = adminNotifications,
                        messages = allMessages,
                        currentLanguage = currentLanguage,
                        onLanguageChange = { currentLanguage = it },
                        repository = repository,
                        onLogout = {
                            repository.logout()
                            currentScreen = AppScreen.WELCOME_3D
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.LOGIN
                }
            }

            AppScreen.BOOKING_FLOW -> {
                BookingFlowScreen(
                    doctors = doctors,
                    preselectedDoctor = activeDoctorForBooking,
                    preselectedDate = activeDateForBooking,
                    preselectedSlot = activeSlotForBooking,
                    currentUserId = currentUser?.id,
                    currentLanguage = currentLanguage,
                    repository = repository,
                    onBackClick = {
                        currentScreen = if (currentUser?.role == UserRole.CLIENT) AppScreen.CLIENT_PORTAL else AppScreen.PUBLIC_CLINIC
                    },
                    onBookingConfirmed = { apt ->
                        latestConfirmedAppointment = apt
                        currentScreen = AppScreen.CONFIRMATION
                    }
                )
            }

            AppScreen.CONFIRMATION -> {
                latestConfirmedAppointment?.let { apt ->
                    ConfirmationScreen(
                        appointment = apt,
                        clinic = clinic,
                        currentLanguage = currentLanguage,
                        repository = repository,
                        onViewAppointmentClick = {
                            currentScreen = if (currentUser?.role == UserRole.CLIENT) AppScreen.CLIENT_PORTAL else AppScreen.PUBLIC_CLINIC
                        },
                        onBackToHomeClick = {
                            currentScreen = if (currentUser?.role == UserRole.CLIENT) AppScreen.CLIENT_PORTAL else AppScreen.WELCOME_3D
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.PUBLIC_CLINIC
                }
            }
        }

        // Voice Assistant Floating Dialog (Accessible from anywhere)
        if (showVoiceAssistant) {
            VoiceAssistantDialog(
                voiceManager = voiceManager,
                currentLanguage = currentLanguage,
                onDismiss = { showVoiceAssistant = false },
                onNavigateToConfirmation = { doc, date, slot ->
                    activeDoctorForBooking = doc
                    activeDateForBooking = date
                    activeSlotForBooking = slot
                    currentScreen = AppScreen.BOOKING_FLOW
                }
            )
        }
    }
}
