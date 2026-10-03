package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ClinicEntity
import com.example.localization.AppLanguage
import com.example.ui.components.Orb3DView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HealingGreen
import com.example.ui.theme.MedicalBlue

@Composable
fun Welcome3DScreen(
    clinic: ClinicEntity?,
    currentLanguage: AppLanguage,
    onEnterClientLogin: () -> Unit,
    onEnterOwnerLogin: () -> Unit,
    onBrowsePublicClinic: () -> Unit
) {
    var hasEntered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A192F))
    ) {
        // Ambient background image with soft medical gradient scrim
        Image(
            painter = painterResource(id = R.drawable.clinic_hero_banner),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.28f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x990A192F),
                            Color(0xCC0D2B24),
                            Color(0xFA0A192F)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Center Content: 3D Orb, Logo & Clinic Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Interactive 3D Medical Holographic Orb
                Orb3DView(
                    size = 130.dp,
                    primaryColor = Color(0xFF00E676),
                    secondaryColor = Color(0xFF00B0FF)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldPrimary.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, HealingGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "HOMEOPATHIC WELLNESS & AI",
                        color = HealingGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = clinic?.name ?: "HOMEo AI Healing Clinic",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your health, our care.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF80DEEA),
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Digital Clinic Management • Live Queue • Classical Healing",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB0BEC5)
                    )
                )
            }

            // Bottom Buttons Container: Initial "ENTER CLINIC" or Role Selection
            AnimatedContent(
                targetState = hasEntered,
                label = "welcome_action"
            ) { entered ->
                if (!entered) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { hasEntered = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text(
                                text = "ENTER CLINIC",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = null)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = onBrowsePublicClinic) {
                            Text(
                                text = "Browse Public Clinic Page (Guest)",
                                color = Color(0xFF80CBC4),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // CLIENT LOGIN Button
                        Button(
                            onClick = onEnterClientLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CLIENT / PATIENT LOGIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // OWNER / ADMIN LOGIN Button
                        Button(
                            onClick = onEnterOwnerLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "OWNER / ADMIN LOGIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        TextButton(
                            onClick = onBrowsePublicClinic,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Continue to Public Clinic Page",
                                color = Color(0xFFB0BEC5),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
