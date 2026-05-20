package com.clubmates.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubmates.app.ui.theme.*

@Composable
fun OtpScreen(
    phone: String,
    viewModel: AuthViewModel,
    onVerified: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var otp by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onVerified()
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Verify your\nnumber", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 40.sp)
            Text("Enter the 6-digit code sent to\n$phone", fontSize = 16.sp, color = TextSecondary, lineHeight = 24.sp)

            // OTP digit boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(SurfaceVariant, RoundedCornerShape(12.dp))
                            .border(
                                width = if (otp.length == index) 2.dp else 1.dp,
                                color = if (otp.length == index) Primary else SurfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = otp.getOrNull(index)?.toString() ?: "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Hidden input field capturing the real keyboard input
            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otp = it },
                modifier = Modifier.size(1.dp).focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Background, unfocusedBorderColor = Background)
            )

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Button(
                onClick = { viewModel.verifyOtp(phone, otp) },
                enabled = otp.length == 6 && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Text("Verify", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
