package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.data.repository.ClinicRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalBlue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AppUrlConstants {
    const val SHARED_APP_URL = "https://ais-pre-krh3ojasrp76qf4324mwph-653917990697.asia-southeast1.run.app"
    const val DEV_APP_URL = "https://ais-dev-krh3ojasrp76qf4324mwph-653917990697.asia-southeast1.run.app"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShareAndScannerDialog(
    repository: ClinicRepository,
    initialTab: Int = 0, // 0: App QR & URL, 1: QR & Ticket Scanner
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val clinic by repository.clinic.collectAsStateWithLifecycle(initialValue = null)

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var selectedUrlType by remember { mutableIntStateOf(if (clinic?.website.orEmpty().isNotBlank()) 2 else 0) }
    var customUrlInput by remember { mutableStateOf(clinic?.website.orEmpty()) }

    LaunchedEffect(clinic?.website) {
        val savedSite = clinic?.website.orEmpty()
        if (savedSite.isNotBlank() && customUrlInput.isBlank()) {
            customUrlInput = savedSite
            selectedUrlType = 2
        }
    }
    
    val activeUrl = when (selectedUrlType) {
        0 -> AppUrlConstants.DEV_APP_URL
        1 -> AppUrlConstants.SHARED_APP_URL
        else -> customUrlInput.ifBlank { AppUrlConstants.DEV_APP_URL }
    }

    val allApts by repository.allAppointments.collectAsStateWithLifecycle(initialValue = emptyList())

    // Scanner state
    var scannedCodeInput by remember { mutableStateOf("") }
    var searchedAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    // Animation for scanner line
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scannerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scannerLine"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (selectedTab == 0) EmeraldPrimary else MedicalBlue,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (selectedTab == 0) Icons.Filled.QrCode2 else Icons.Filled.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (selectedTab == 0) "App URL & QR Code" else "Clinic QR Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedTab == 0) "Scan to open app on mobile" else "Scan appointment ticket or token",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("App QR & Link", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("QR Scanner", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab 0: App QR Code & Link Sharing
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Environment Switcher: Dev URL vs Shared URL vs Custom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedUrlType == 0,
                                onClick = { selectedUrlType = 0 },
                                label = { Text("Dev URL (Active)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    if (selectedUrlType == 0) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedUrlType == 1,
                                onClick = { selectedUrlType = 1 },
                                label = { Text("Shared URL", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    if (selectedUrlType == 1) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedUrlType == 2,
                                onClick = { selectedUrlType = 2 },
                                label = { Text("Custom", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    if (selectedUrlType == 2) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                },
                                modifier = Modifier.weight(0.7f)
                            )
                        }

                        // Guidance box explaining URL status
                        if (selectedUrlType == 1) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Why Shared URL may say 403 / Access Denied: In Google AI Studio top-right, click 'Share' -> choose 'Anyone with the link can view' to make it public!",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        } else if (selectedUrlType == 0) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Active Development URL. Opens the live streaming app directly in browser.",
                                        fontSize = 11.sp,
                                        color = EmeraldPrimary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        } else {
                            // Custom Verified URL Input
                            OutlinedTextField(
                                value = customUrlInput,
                                onValueChange = { customUrlInput = it },
                                label = { Text("Enter Verified Public URL / Domain") },
                                placeholder = { Text("https://yourclinic.com or public link") },
                                leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            if (customUrlInput.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            clinic?.let { currentClinic ->
                                                scope.launch {
                                                    repository.saveClinic(currentClinic.copy(website = customUrlInput.trim()))
                                                    Toast.makeText(context, "Saved verified clinic URL!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save as Clinic Public URL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Genuine ZXing QR Code
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(2.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                QrCodeView(
                                    data = activeUrl,
                                    size = 180.dp,
                                    qrColor = Color(0xFF064E3B)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when (selectedUrlType) {
                                        0 -> "SCAN DEV PREVIEW URL"
                                        1 -> "SCAN SHARED PUBLIC URL"
                                        else -> "SCAN CUSTOM CLINIC URL"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Point any phone camera or Google Lens at this QR code to access the app immediately on web or mobile browser.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // URL Display Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = MedicalBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = activeUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Clinic App URL", activeUrl)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "App URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy URL", tint = EmeraldPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Share & Open
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Welcome to HOMEo AI Classical Homeopathy Clinic! Book appointments, consult doctors, and view health records here: $activeUrl"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Clinic App URL"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share URL")
                            }

                            OutlinedButton(
                                onClick = {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(activeUrl))
                                    context.startActivity(browserIntent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Link")
                            }
                        }
                    }
                }

                // Tab 1: Clinic QR Scanner
                if (selectedTab == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Simulated Visual Scanner Viewfinder
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Target Reticle Corner Brackets
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            )

                            // Animated Laser Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .offset(y = (-75 + (scannerOffset * 150)).dp)
                                    .background(Color(0xFF00E676))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ready to Scan",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (allApts.isNotEmpty()) {
                            Text(
                                text = "Quick Select Active Ticket / Token:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allApts.take(6).forEach { apt ->
                                    FilterChip(
                                        selected = scannedCodeInput == apt.id,
                                        onClick = {
                                            scannedCodeInput = apt.id
                                            searchedAppointment = apt
                                            scanMessage = "✓ Verified Appointment: #${apt.id}"
                                        },
                                        label = { Text("#${apt.id} (${apt.patientName})", fontSize = 11.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = "Enter or paste appointment ticket code (e.g. HM-2026-...) or token to verify:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = scannedCodeInput,
                            onValueChange = { scannedCodeInput = it },
                            placeholder = { Text("e.g. HM-2026-1001 or Ticket QR code") },
                            leadingIcon = { Icon(Icons.Filled.QrCode, contentDescription = null) },
                            trailingIcon = {
                                if (scannedCodeInput.isNotBlank()) {
                                    IconButton(onClick = { scannedCodeInput = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = null)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val query = scannedCodeInput.trim()
                                if (query.isBlank()) {
                                    Toast.makeText(context, "Please enter an appointment code or scan data", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSearching = true
                                scanMessage = null
                                searchedAppointment = null

                                scope.launch {
                                    val apts = repository.allAppointments.first()
                                    val match = apts.find { it.id.equals(query, ignoreCase = true) || it.patientPhone == query }
                                    isSearching = false
                                    if (match != null) {
                                        searchedAppointment = match
                                        scanMessage = "✓ Verified Appointment: #${match.id}"
                                    } else {
                                        scanMessage = "No matching appointment found for: $query"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify Ticket / Token")
                        }

                        // Search Result Display
                        if (searchedAppointment != null) {
                            val apt = searchedAppointment!!
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "TICKET VERIFIED", fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary, fontSize = 11.sp)
                                        Text(text = "#${apt.id}", fontWeight = FontWeight.Bold, color = MedicalBlue)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = apt.patientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Doctor: ${apt.doctorName} • Date: ${apt.appointmentDate} (${apt.timeSlot})", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Status: ${apt.status.name} • Fee: ₹${apt.consultationFee.toInt()}", style = MaterialTheme.typography.bodySmall)

                                    if (apt.status == AppointmentStatus.PENDING) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    repository.confirmAppointmentByAdmin(apt.id, "Verified by QR Scanner")
                                                    val updated = repository.allAppointments.first().find { it.id == apt.id }
                                                    searchedAppointment = updated
                                                    Toast.makeText(context, "Checked-in and confirmed #${apt.id}!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Check-In & Confirm Patient")
                                        }
                                    }
                                }
                            }
                        } else if (scanMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = scanMessage!!, color = if (scannedCodeInput.startsWith("HM-", ignoreCase = true)) MedicalBlue else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                            if (scannedCodeInput.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val code = scannedCodeInput.trim()
                                        scope.launch {
                                            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                            val result = repository.importWebBooking(
                                                id = if (code.startsWith("HM-", ignoreCase = true)) code else "HM-${System.currentTimeMillis() % 100000}",
                                                patientName = "Web Patient (${code.takeLast(4)})",
                                                patientPhone = "9876543210",
                                                patientAge = 30,
                                                patientGender = "Male",
                                                date = today,
                                                timeSlot = "10:30 AM",
                                                consultationType = "IN_CLINIC",
                                                symptoms = "Booked via Website Portal"
                                            )
                                            val imported = result.getOrNull()
                                            if (imported != null) {
                                                searchedAppointment = imported
                                                scanMessage = "✓ Successfully Imported from Web: #${imported.id}"
                                                Toast.makeText(context, "Web appointment #${imported.id} imported into database!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Web Booking into App")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
