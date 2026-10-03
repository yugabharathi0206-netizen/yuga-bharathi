package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.ClinicRepository
import com.example.localization.AppLanguage
import com.example.ui.components.Orb3DView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MedicalBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRoleScreen(
    repository: ClinicRepository,
    currentLanguage: AppLanguage,
    initialRoleTab: Int = 0,
    onLoginSuccess: (UserEntity) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedRoleTab by remember { mutableIntStateOf(initialRoleTab) } // 0: Client/Patient, 1: Owner/Admin

    // Check if Owner account has already been initialized
    var isCheckingOwner by remember { mutableStateOf(true) }
    var hasOwnerAccount by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasOwnerAccount = repository.hasOwner()
        isCheckingOwner = false
    }

    // Owner Setup Fields (First-time initialization)
    var setupOwnerName by remember { mutableStateOf("") }
    var setupOwnerEmail by remember { mutableStateOf("") }
    var setupOwnerPhone by remember { mutableStateOf("") }
    var setupOwnerPassword by remember { mutableStateOf("") }
    var setupOwnerConfirmPassword by remember { mutableStateOf("") }

    // Standard Login Fields
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Client Registration Modal
    var showRegisterModal by remember { mutableStateOf(false) }
    var regFullName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regDob by remember { mutableStateOf("") }
    var regGender by remember { mutableStateOf("Male") }

    // Forgot Password Modal
    var showForgotPasswordModal by remember { mutableStateOf(false) }
    var forgotEmailOrPhone by remember { mutableStateOf("") }

    BackHandler {
        onContinueAsGuest()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onContinueAsGuest) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back to Clinic")
                }
                TextButton(onClick = onContinueAsGuest) {
                    Text("Browse Clinic", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3D Visual Orb & Branding
            Orb3DView(size = 80.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "HOMEo AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldPrimary
            )
            Text(
                text = "Classical Homeopathy Clinic Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Role Switcher Tabs
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Client / Patient Tab
                    Surface(
                        onClick = {
                            selectedRoleTab = 0
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedRoleTab == 0) EmeraldPrimary else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (selectedRoleTab == 0) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Patient Portal",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRoleTab == 0) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Owner / Staff Tab
                    Surface(
                        onClick = {
                            selectedRoleTab = 1
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedRoleTab == 1) MedicalBlue else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (selectedRoleTab == 1) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Owner / Staff",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRoleTab == 1) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Authentication Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(
                    1.dp,
                    if (selectedRoleTab == 0) EmeraldPrimary.copy(alpha = 0.2f) else MedicalBlue.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    if (selectedRoleTab == 1 && !hasOwnerAccount) {
                        // -----------------------------------------------------------------
                        // FIRST-TIME OWNER SETUP FORM (Only when no Owner account exists!)
                        // -----------------------------------------------------------------
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MedicalBlue.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.VpnKey, contentDescription = null, tint = MedicalBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Set Up Clinic Owner Account", fontWeight = FontWeight.Bold, color = MedicalBlue)
                                    Text(
                                        "You are the master owner. Set up your private credentials to initialize the clinic system.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = setupOwnerName,
                            onValueChange = { setupOwnerName = it },
                            label = { Text("Your Full Name (Doctor / Owner) *") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = setupOwnerEmail,
                            onValueChange = { setupOwnerEmail = it },
                            label = { Text("Owner Email Address *") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = setupOwnerPhone,
                            onValueChange = { setupOwnerPhone = it },
                            label = { Text("Owner Mobile Number *") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = setupOwnerPassword,
                            onValueChange = { setupOwnerPassword = it },
                            label = { Text("Create Master Password *") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = setupOwnerConfirmPassword,
                            onValueChange = { setupOwnerConfirmPassword = it },
                            label = { Text("Confirm Master Password *") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (setupOwnerName.isBlank()) {
                                    errorMessage = "Please enter your full name."
                                    return@Button
                                }
                                if (setupOwnerEmail.isBlank() && setupOwnerPhone.isBlank()) {
                                    errorMessage = "Please enter an email or phone number."
                                    return@Button
                                }
                                if (setupOwnerPassword.length < 4) {
                                    errorMessage = "Password must be at least 4 characters long."
                                    return@Button
                                }
                                if (setupOwnerPassword != setupOwnerConfirmPassword) {
                                    errorMessage = "Passwords do not match."
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null
                                scope.launch {
                                    val result = repository.setupOwnerAccount(
                                        fullName = setupOwnerName,
                                        email = setupOwnerEmail,
                                        phone = setupOwnerPhone,
                                        password = setupOwnerPassword
                                    )
                                    isSubmitting = false
                                    result.onSuccess { ownerUser ->
                                        hasOwnerAccount = true
                                        Toast.makeText(context, "Owner account initialized successfully!", Toast.LENGTH_LONG).show()
                                        onLoginSuccess(ownerUser)
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Failed to initialize owner account."
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Clinic Owner Account", fontWeight = FontWeight.Bold)
                            }
                        }

                    } else {
                        // -----------------------------------------------------------------
                        // STANDARD SECURE LOGIN (Patient or Established Owner/Staff)
                        // -----------------------------------------------------------------
                        Text(
                            text = if (selectedRoleTab == 0) "Patient Account Login" else "Owner / Staff Login",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedRoleTab == 0) EmeraldPrimary else MedicalBlue
                        )
                        Text(
                            text = if (selectedRoleTab == 0)
                                "Access your appointments, prescriptions, and health records"
                            else
                                "Private access for Clinic Owner and authorized staff members only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Email / Mobile Field
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = { Text("Email or Mobile Number") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle password"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Forgot Password Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showForgotPasswordModal = true }) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Login Button
                        Button(
                            onClick = {
                                if (identifier.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter both credentials."
                                    return@Button
                                }
                                isSubmitting = true
                                errorMessage = null

                                scope.launch {
                                    val result = repository.login(identifier, password)
                                    isSubmitting = false
                                    result.onSuccess { user ->
                                        // Strict access control: verify role matches requested portal
                                        if (selectedRoleTab == 1 && user.role != UserRole.OWNER && user.role != UserRole.ADMIN) {
                                            errorMessage = "Access Denied: This is a Patient account. Please log in through the Patient Portal."
                                            repository.logout()
                                        } else if (selectedRoleTab == 0 && (user.role == UserRole.OWNER || user.role == UserRole.ADMIN)) {
                                            // Staff can use client side or switch to command center
                                            Toast.makeText(context, "Welcome, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(user)
                                        } else {
                                            Toast.makeText(context, "Welcome, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(user)
                                        }
                                    }.onFailure { error ->
                                        errorMessage = error.message ?: "Login failed. Check credentials."
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRoleTab == 0) EmeraldPrimary else MedicalBlue
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text(
                                    text = if (selectedRoleTab == 0) "Login to Patient Portal" else "Login to Command Center",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // Client Registration Button
                        if (selectedRoleTab == 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showRegisterModal = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Patient? Create Account", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Guest Browse Option
            TextButton(onClick = onContinueAsGuest) {
                Text(
                    text = "Continue as Guest (Browse Clinic & Services)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }

    // Client Registration Modal
    if (showRegisterModal) {
        AlertDialog(
            onDismissRequest = { showRegisterModal = false },
            title = {
                Text("New Patient Registration", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = { regPhone = it },
                        label = { Text("Mobile Number *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it },
                        label = { Text("Email (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Password *") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regDob,
                        onValueChange = { regDob = it },
                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                        placeholder = { Text("e.g. 1995-08-20") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = regGender == g,
                                onClick = { regGender = g },
                                label = { Text(g) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (regFullName.isBlank() || regPhone.isBlank() || regPassword.isBlank()) {
                            Toast.makeText(context, "Name, mobile, and password are required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            val result = repository.registerClient(
                                fullName = regFullName,
                                phone = regPhone,
                                email = regEmail,
                                passwordOrOtp = regPassword,
                                dob = regDob,
                                gender = regGender
                            )
                            result.onSuccess { user ->
                                Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                showRegisterModal = false
                                onLoginSuccess(user)
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Register & Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Forgot Password Modal
    if (showForgotPasswordModal) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordModal = false },
            title = { Text("Password Recovery", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the email or phone number registered with your clinic account to receive a reset link / OTP verification.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = forgotEmailOrPhone,
                        onValueChange = { forgotEmailOrPhone = it },
                        label = { Text("Registered Email or Mobile") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotEmailOrPhone.isNotBlank()) {
                            Toast.makeText(context, "Reset instructions sent to $forgotEmailOrPhone", Toast.LENGTH_LONG).show()
                            showForgotPasswordModal = false
                        }
                    }
                ) {
                    Text("Send Recovery Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
