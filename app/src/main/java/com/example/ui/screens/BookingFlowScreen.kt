package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.data.repository.SlotInfo
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
fun BookingFlowScreen(
    doctors: List<DoctorEntity>,
    preselectedDoctor: DoctorEntity?,
    preselectedDate: String? = null,
    preselectedSlot: String? = null,
    currentUserId: String? = null,
    currentLanguage: AppLanguage,
    repository: ClinicRepository,
    onBackClick: () -> Unit,
    onBookingConfirmed: (AppointmentEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(if (preselectedDoctor != null && preselectedDate != null && preselectedSlot != null) 4 else 1) }

    var selectedDoctor by remember { mutableStateOf(preselectedDoctor ?: doctors.firstOrNull()) }
    var selectedDate by remember { mutableStateOf(preselectedDate ?: getTodayDateString()) }
    var selectedSlot by remember { mutableStateOf(preselectedSlot ?: "") }

    var availableSlots by remember { mutableStateOf<List<SlotInfo>>(emptyList()) }
    var isLoadingSlots by remember { mutableStateOf(false) }

    // Patient Form State
    var patientName by remember { mutableStateOf("") }
    var patientPhone by remember { mutableStateOf("") }
    var patientAge by remember { mutableStateOf("") }
    var patientGender by remember { mutableStateOf("Male") }
    var reasonForVisit by remember { mutableStateOf("") }
    var consultationType by remember { mutableStateOf(AppointmentType.IN_CLINIC) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.PAY_AT_CLINIC) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Handle back button per step
    BackHandler {
        if (currentStep > 1) {
            currentStep -= 1
        } else {
            onBackClick()
        }
    }

    // Refresh slots whenever doctor or date changes
    LaunchedEffect(selectedDoctor, selectedDate) {
        val doc = selectedDoctor
        if (doc != null) {
            isLoadingSlots = true
            availableSlots = repository.getAvailableSlots(doc, selectedDate)
            isLoadingSlots = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = StringsLocalization.get("book_appointment", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step $currentStep of 5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep -= 1 else onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // 3D Progress Step Indicator
            ProgressHeader(currentStep = currentStep)

            errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    1 -> StepDoctorSelection(
                        doctors = doctors,
                        selectedDoctor = selectedDoctor,
                        currentLanguage = currentLanguage,
                        onDoctorSelected = {
                            selectedDoctor = it
                            currentStep = 2
                        }
                    )
                    2 -> StepDateSelection(
                        selectedDoctor = selectedDoctor,
                        selectedDate = selectedDate,
                        currentLanguage = currentLanguage,
                        onDateSelected = {
                            selectedDate = it
                            currentStep = 3
                        }
                    )
                    3 -> StepSlotSelection(
                        slots = availableSlots,
                        selectedSlot = selectedSlot,
                        isLoading = isLoadingSlots,
                        currentLanguage = currentLanguage,
                        onSlotSelected = {
                            selectedSlot = it
                            currentStep = 4
                        }
                    )
                    4 -> StepPatientDetails(
                        name = patientName,
                        onNameChange = { patientName = it },
                        phone = patientPhone,
                        onPhoneChange = { patientPhone = it },
                        age = patientAge,
                        onAgeChange = { patientAge = it },
                        gender = patientGender,
                        onGenderChange = { patientGender = it },
                        reason = reasonForVisit,
                        onReasonChange = { reasonForVisit = it },
                        type = consultationType,
                        onTypeChange = { consultationType = it },
                        currentLanguage = currentLanguage,
                        onNextClick = {
                            if (patientName.isBlank() || patientPhone.isBlank()) {
                                Toast.makeText(context, "Please enter your name and phone number", Toast.LENGTH_SHORT).show()
                            } else {
                                currentStep = 5
                            }
                        }
                    )
                    5 -> StepConfirmationAndPayment(
                        doctor = selectedDoctor,
                        date = selectedDate,
                        timeSlot = selectedSlot,
                        patientName = patientName,
                        patientPhone = patientPhone,
                        consultationType = consultationType,
                        paymentMethod = paymentMethod,
                        onPaymentMethodChange = { paymentMethod = it },
                        isSubmitting = isSubmitting,
                        currentLanguage = currentLanguage,
                        onConfirmBooking = {
                            val doc = selectedDoctor ?: return@StepConfirmationAndPayment
                            isSubmitting = true
                            errorMessage = null

                            scope.launch {
                                val result = repository.bookAppointment(
                                    clientId = currentUserId ?: "usr_client_guest",
                                    doctorId = doc.id,
                                    doctorName = doc.name,
                                    patientName = patientName,
                                    patientPhone = patientPhone,
                                    patientEmail = "",
                                    patientAge = patientAge.toIntOrNull() ?: 30,
                                    patientGender = patientGender,
                                    reason = reasonForVisit.ifBlank { "Homeopathic Consultation" },
                                    symptomsDescription = reasonForVisit,
                                    date = selectedDate,
                                    timeSlot = selectedSlot,
                                    type = consultationType,
                                    consultationFee = doc.consultationFee,
                                    paymentMethod = paymentMethod
                                )

                                isSubmitting = false
                                result.onSuccess { apt ->
                                    onBookingConfirmed(apt)
                                }.onFailure { error ->
                                    errorMessage = error.message ?: StringsLocalization.get("slot_taken_error", currentLanguage)
                                    // Take user back to slot step to choose another slot!
                                    currentStep = 3
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressHeader(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val steps = listOf("Doctor", "Date", "Slot", "Details", "Confirm")
        steps.forEachIndexed { index, title ->
            val stepNumber = index + 1
            val isActive = stepNumber == currentStep
            val isPassed = stepNumber < currentStep

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isPassed -> HealingGreen
                        isActive -> EmeraldPrimary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPassed) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                text = "$stepNumber",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StepDoctorSelection(
    doctors: List<DoctorEntity>,
    selectedDoctor: DoctorEntity?,
    currentLanguage: AppLanguage,
    onDoctorSelected: (DoctorEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = StringsLocalization.get("choose_doctor", currentLanguage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        doctors.forEach { doctor ->
            val isSelected = doctor.id == selectedDoctor?.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onDoctorSelected(doctor) },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.doctor_portrait),
                        contentDescription = doctor.name,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doctor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = doctor.qualification,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = doctor.specialization,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Fee: ₹${doctor.consultationFee.toInt()} • ${doctor.experienceYears} yrs exp",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onDoctorSelected(doctor) },
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                    )
                }
            }
        }
    }
}

@Composable
fun StepDateSelection(
    selectedDoctor: DoctorEntity?,
    selectedDate: String,
    currentLanguage: AppLanguage,
    onDateSelected: (String) -> Unit
) {
    val dateList = remember(selectedDoctor) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

        val list = mutableListOf<Triple<String, String, String>>() // (fullDate, dayOfWeek, displayDate)
        val cal = Calendar.getInstance()

        for (i in 0 until 10) {
            val d = cal.time
            val full = sdf.format(d)
            val day = dayFormat.format(d)
            val disp = displayFormat.format(d)
            list.add(Triple(full, day, disp))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = StringsLocalization.get("choose_date", currentLanguage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Showing next 10 days for ${selectedDoctor?.name ?: "Doctor"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        dateList.forEach { (fullDate, dayOfWeek, displayDate) ->
            val isSelected = fullDate == selectedDate
            val isWorkingDay = selectedDoctor?.availableDays?.contains(dayOfWeek, ignoreCase = true) != false

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(enabled = isWorkingDay) {
                        onDateSelected(fullDate)
                    },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !isWorkingDay -> Color(0xFFF1F5F9).copy(alpha = 0.5f)
                        isSelected -> EmeraldPrimary.copy(alpha = 0.08f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayOfWeek,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isWorkingDay) MaterialTheme.colorScheme.onSurface else Color.Gray
                            )
                            Text(
                                text = if (isWorkingDay) "Clinic Open (${selectedDoctor?.slotDurationMinutes ?: 30} min slots)" else "Doctor Not Available",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isWorkingDay) HealingGreen else Color.Gray
                            )
                        }
                    }

                    if (isWorkingDay) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (isSelected) EmeraldPrimary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepSlotSelection(
    slots: List<SlotInfo>,
    selectedSlot: String,
    isLoading: Boolean,
    currentLanguage: AppLanguage,
    onSlotSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = StringsLocalization.get("choose_time", currentLanguage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Real-time slot lock prevents double booking",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else if (slots.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = StringsLocalization.get("no_slots_available", currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(slots) { slot ->
                    val isSelected = slot.timeSlot == selectedSlot
                    val isAvail = slot.isAvailable

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable(enabled = isAvail) {
                                onSlotSelected(slot.timeSlot)
                            },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                !isAvail -> Color(0xFFCBD5E1)
                                isSelected -> EmeraldPrimary
                                else -> Color(0xFFE2E8F0)
                            }
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                !isAvail -> Color(0xFFF1F5F9)
                                isSelected -> EmeraldPrimary
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = slot.timeSlot,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = when {
                                        !isAvail -> Color.Gray
                                        isSelected -> Color.White
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = if (isAvail) "Available" else "Booked",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        !isAvail -> Color.Red.copy(alpha = 0.7f)
                                        isSelected -> Color.White.copy(alpha = 0.9f)
                                        else -> HealingGreen
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepPatientDetails(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    reason: String,
    onReasonChange: (String) -> Unit,
    type: AppointmentType,
    onTypeChange: (AppointmentType) -> Unit,
    currentLanguage: AppLanguage,
    onNextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = StringsLocalization.get("step_4_details", currentLanguage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(StringsLocalization.get("patient_name", currentLanguage)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text(StringsLocalization.get("patient_phone", currentLanguage)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text(StringsLocalization.get("patient_age", currentLanguage)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Column(modifier = Modifier.weight(1.5f)) {
                Text(text = StringsLocalization.get("patient_gender", currentLanguage), style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("Male", "Female").forEach { g ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onGenderChange(g) }
                        ) {
                            RadioButton(selected = gender == g, onClick = { onGenderChange(g) })
                            Text(text = g, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            label = { Text(StringsLocalization.get("reason_for_visit", currentLanguage)) },
            placeholder = { Text("e.g. Migraine, chronic allergies, knee pain, stress...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Consultation Type (In-Clinic vs Online)
        Text(
            text = StringsLocalization.get("consultation_type", currentLanguage),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeChoiceCard(
                title = StringsLocalization.get("in_clinic", currentLanguage),
                subtitle = "T. Nagar Clinic",
                icon = Icons.Filled.LocalHospital,
                isSelected = type == AppointmentType.IN_CLINIC,
                onClick = { onTypeChange(AppointmentType.IN_CLINIC) },
                modifier = Modifier.weight(1f)
            )

            TypeChoiceCard(
                title = StringsLocalization.get("online_tele", currentLanguage),
                subtitle = "HD Video Room",
                icon = Icons.Filled.Videocam,
                isSelected = type == AppointmentType.ONLINE,
                onClick = { onTypeChange(AppointmentType.ONLINE) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNextClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text("Proceed to Review", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TypeChoiceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) EmeraldPrimary else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StepConfirmationAndPayment(
    doctor: DoctorEntity?,
    date: String,
    timeSlot: String,
    patientName: String,
    patientPhone: String,
    consultationType: AppointmentType,
    paymentMethod: PaymentMethod,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    isSubmitting: Boolean,
    currentLanguage: AppLanguage,
    onConfirmBooking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = StringsLocalization.get("step_5_confirm", currentLanguage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Booking Summary Card with 3D Depth
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.doctor_portrait),
                        contentDescription = null,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = doctor?.name ?: "Doctor",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = doctor?.qualification ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                SummaryRow(label = StringsLocalization.get("date", currentLanguage), value = date)
                SummaryRow(label = StringsLocalization.get("time", currentLanguage), value = timeSlot)
                SummaryRow(label = StringsLocalization.get("patient_name", currentLanguage), value = patientName)
                SummaryRow(label = StringsLocalization.get("patient_phone", currentLanguage), value = patientPhone)
                SummaryRow(
                    label = "Consultation Type",
                    value = if (consultationType == AppointmentType.IN_CLINIC) "In-Clinic Visit" else "Online Teleconsultation"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = StringsLocalization.get("consultation_fee", currentLanguage),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "₹${doctor?.consultationFee?.toInt() ?: 500}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = EmeraldPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Option
        Text(
            text = StringsLocalization.get("payment_method", currentLanguage),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPaymentMethodChange(PaymentMethod.PAY_AT_CLINIC) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = if (paymentMethod == PaymentMethod.PAY_AT_CLINIC) 2.dp else 1.dp,
                color = if (paymentMethod == PaymentMethod.PAY_AT_CLINIC) EmeraldPrimary else Color(0xFFE2E8F0)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = paymentMethod == PaymentMethod.PAY_AT_CLINIC,
                    onClick = { onPaymentMethodChange(PaymentMethod.PAY_AT_CLINIC) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = StringsLocalization.get("pay_at_clinic", currentLanguage), fontWeight = FontWeight.Bold)
                    Text(text = "Pay cash or UPI at reception after consultation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPaymentMethodChange(PaymentMethod.ONLINE) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = if (paymentMethod == PaymentMethod.ONLINE) 2.dp else 1.dp,
                color = if (paymentMethod == PaymentMethod.ONLINE) EmeraldPrimary else Color(0xFFE2E8F0)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = paymentMethod == PaymentMethod.ONLINE,
                    onClick = { onPaymentMethodChange(PaymentMethod.ONLINE) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = StringsLocalization.get("pay_online", currentLanguage), fontWeight = FontWeight.Bold)
                    Text(text = "Razorpay / UPI Demo Sandbox (Auto-confirms PAID)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConfirmBooking,
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(StringsLocalization.get("booking_in_progress", currentLanguage))
            } else {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = StringsLocalization.get("confirm_booking_button", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}
