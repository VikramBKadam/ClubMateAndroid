package com.clubmates.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubmates.app.ui.theme.*

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    isLoggedIn: Boolean,
    onAlreadyLoggedIn: () -> Unit
) {
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onAlreadyLoggedIn()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2A0A4A), Background),
                    center = Offset(0.5f, 0.3f),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "ClubMates",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Meet people at the venues\nyou're at — right now.",
                fontSize = 18.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            Text(
                text = "By continuing you agree to our Terms & Privacy Policy",
                fontSize = 12.sp,
                color = TextHint,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
