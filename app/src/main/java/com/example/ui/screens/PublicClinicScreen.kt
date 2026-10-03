package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ClinicEntity
import com.example.data.model.DoctorEntity
import com.example.data.model.ServiceEntity
import com.example.data.repository.ClinicRepository
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import com.example.ui.components.AppShareAndScannerDialog
import com.example.ui.components.Orb3DView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HealingGreen
import com.example.ui.theme.MedicalBlue
import com.example.util.ContactUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicClinicScreen(
    clinic: ClinicEntity?,
    doctors: List<DoctorEntity>,
    services: List<ServiceEntity>,
    currentLanguage: AppLanguage,
    repository: ClinicRepository,
    onBookAppointmentClick: (DoctorEntity?) -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onOpenPatientPortal: () -> Unit,
    onOpenClinicStaffPortal: () -> Unit
) {
    val context = LocalContext.current
    var showContactDialog by remember { mutableStateOf(false) }
    var showShareAndScannerModal by remember { mutableStateOf(false) }
    var initialScannerTab by remember { mutableIntStateOf(0) }

    val clinicName = clinic?.name ?: "HOMEo AI Classical Clinic"
    val clinicAddress = clinic?.address.orEmpty()
    val clinicPhone = clinic?.phone.orEmpty()
    val whatsAppNumber = clinic?.whatsAppNumber.orEmpty()
    val showPhonePublicly = clinic?.showPhoneNumberOnPublicPage ?: true

    Scaffold(
        floatingActionButton = {
            // Floating 3D Voice AI Assistant button
            FloatingActionButton(
                onClick = onOpenVoiceAssistant,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .shadow(12.dp, CircleShape)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Orb3DView(size = 36.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Hero Clinic Banner with 3D Depth
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.clinic_hero_banner),
                        contentDescription = "Clinic Reception",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x66000000),
                                        Color(0xCC052011),
                                        Color(0xEE0A192F)
                                    )
                                )
                            )
                    )

                    // Top Floating Bar inside Hero: App URL & Scanner + Staff Portal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x66000000),
                            border = BorderStroke(1.dp, Color(0x44FFFFFF))
                        ) {
                            Text(
                                text = "HOMEo AI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldPrimary.copy(alpha = 0.9f),
                                modifier = Modifier.clickable {
                                    initialScannerTab = 0
                                    showShareAndScannerModal = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.QrCodeScanner,
                                        contentDescription = "App URL & Scanner",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "App URL & Scan",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x66000000),
                                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                                modifier = Modifier.clickable { onOpenClinicStaffPortal() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.AdminPanelSettings,
                                        contentDescription = "Staff Portal",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Staff",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Hero Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Surface(
                            color = HealingGreen.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, HealingGreen.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(HealingGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OPEN NOW • 9:00 AM - 8:00 PM",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = clinicName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = clinic?.tagline ?: "Smart Clinic. Better Care.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // 2. Primary Action Bar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Main Book Button
                        Button(
                            onClick = { onBookAppointmentClick(doctors.firstOrNull()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = StringsLocalization.get("book_appointment", currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick action icons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QuickActionChip(
                                icon = Icons.Filled.Phone,
                                label = StringsLocalization.get("call_clinic", currentLanguage),
                                onClick = {
                                    ContactUtils.callClinic(context, clinicPhone)
                                }
                            )

                            QuickActionChip(
                                icon = Icons.Filled.Chat,
                                label = "WhatsApp",
                                onClick = {
                                    ContactUtils.openWhatsApp(context, whatsAppNumber.ifBlank { clinicPhone })
                                }
                            )

                            QuickActionChip(
                                icon = Icons.Filled.Directions,
                                label = StringsLocalization.get("get_directions", currentLanguage),
                                onClick = {
                                    ContactUtils.openDirections(context, clinicAddress)
                                }
                            )

                            QuickActionChip(
                                icon = Icons.Filled.ContactMail,
                                label = StringsLocalization.get("contact_clinic", currentLanguage),
                                onClick = { showContactDialog = true }
                            )

                            QuickActionChip(
                                icon = Icons.Filled.Person,
                                label = StringsLocalization.get("patient_portal", currentLanguage),
                                onClick = onOpenPatientPortal
                            )
                        }

                        // Display phone number only if owner has enabled "Show phone number"
                        if (showPhonePublicly && clinicPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { ContactUtils.callClinic(context, clinicPhone) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Phone, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Clinic Phone: $clinicPhone",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Tap to call",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2.5 App URL & QR Scanner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            initialScannerTab = 0
                            showShareAndScannerModal = true
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App URL & QR Scanner",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Scan QR to open web app on mobile, or scan appointment token",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                initialScannerTab = 0
                                showShareAndScannerModal = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                contentColor = EmeraldPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Open", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 3. 3D AI Assistant Teaser Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onOpenVoiceAssistant() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Orb3DView(size = 72.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "3D AI Voice Booking",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"Innaikku evening appointment book pannu\" • English / தமிழ் / Tanglish",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Mic",
                            tint = MedicalBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // 4. Our Doctors Showcase
            item {
                SectionHeader(title = StringsLocalization.get("our_doctors", currentLanguage))
            }

            if (doctors.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Doctor Profile Setup in Progress",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "The Clinic Owner is setting up the official consulting profile and timetable. Please log in as Owner to create your doctor profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = onOpenClinicStaffPortal) {
                                Text("Owner Login / Setup Doctor Profile")
                            }
                        }
                    }
                }
            } else {
                items(doctors) { doctor ->
                    DoctorCard(
                        doctor = doctor,
                        clinic = clinic,
                        currentLanguage = currentLanguage,
                        onBookClick = { onBookAppointmentClick(doctor) }
                    )
                }
            }

            // 5. Specialized Healing Services
            item {
                SectionHeader(title = StringsLocalization.get("services_offered", currentLanguage))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(services) { service ->
                        ServiceCard(service = service, onBookClick = { onBookAppointmentClick(null) })
                    }
                }
            }

            // 6. Clinic Details & Hours & Amenities
            item {
                SectionHeader(title = StringsLocalization.get("working_hours", currentLanguage))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = clinic?.workingHoursSummary ?: "Mon - Sat: 9:00 AM - 1:00 PM & 4:00 PM - 8:00 PM",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MedicalBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = clinicAddress,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalPharmacy, contentDescription = null, tint = HealingGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pure German Homeopathic Potencies • Digital Health Records",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // 7. Staff & Doctor Portal Switcher Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = StringsLocalization.get("clinic_staff_portal", currentLanguage),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Queue manager, pharmacy inventory, case notes & billing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenClinicStaffPortal,
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
                        ) {
                            Text("Staff Login")
                        }
                    }
                }
            }
        }
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text(text = StringsLocalization.get("contact_clinic", currentLanguage), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = clinicName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    OutlinedButton(
                        onClick = {
                            ContactUtils.callClinic(context, clinicPhone)
                            showContactDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (clinicPhone.isNotBlank()) "Call: $clinicPhone" else "Call Clinic (Not configured)",
                            color = if (clinicPhone.isNotBlank()) EmeraldPrimary else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val wa = whatsAppNumber.ifBlank { clinicPhone }
                    OutlinedButton(
                        onClick = {
                            ContactUtils.openWhatsApp(context, wa)
                            showContactDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, tint = MedicalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (wa.isNotBlank()) "WhatsApp: $wa" else "WhatsApp (Not configured)",
                            color = if (wa.isNotBlank()) MedicalBlue else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (clinic?.email.orEmpty().isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                ContactUtils.sendEmail(context, clinic?.email)
                                showContactDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Email: ${clinic?.email}")
                        }
                    }

                    if (clinicAddress.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                ContactUtils.openDirections(context, clinicAddress)
                                showContactDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Directions: $clinicAddress", maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showShareAndScannerModal) {
        AppShareAndScannerDialog(
            repository = repository,
            initialTab = initialScannerTab,
            onDismiss = { showShareAndScannerModal = false }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun DoctorCard(
    doctor: DoctorEntity,
    clinic: ClinicEntity? = null,
    currentLanguage: AppLanguage,
    onBookClick: () -> Unit
) {
    val context = LocalContext.current
    val effectivePhone = doctor.phone.ifBlank { clinic?.phone.orEmpty() }
    val effectiveWhatsApp = doctor.whatsAppNumber.ifBlank { clinic?.whatsAppNumber ?: clinic?.phone.orEmpty() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.doctor_portrait),
                    contentDescription = doctor.name,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
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
                        text = "${doctor.experienceYears} ${StringsLocalization.get("experience", currentLanguage)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = doctor.specialization,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            // Direct Doctor Contact Options (Uses configured clinic/doctor number)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { ContactUtils.callClinic(context, effectivePhone) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { ContactUtils.openWhatsApp(context, effectiveWhatsApp, "Hello Dr. ${doctor.name}, I would like to consult with you.") },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = StringsLocalization.get("consultation_fee", currentLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${doctor.consultationFee.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MedicalBlue
                    )
                }

                Button(
                    onClick = onBookClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(
                        text = StringsLocalization.get("book_appointment", currentLanguage),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceCard(
    service: ServiceEntity,
    onBookClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(170.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${service.fee.toInt()}",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                TextButton(onClick = onBookClick, contentPadding = PaddingValues(0.dp)) {
                    Text("Select", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
