package com.clubmates.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubmates.app.ui.theme.*

@Composable
fun PhoneInputScreen(
    viewModel: AuthViewModel,
    onOtpSent: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.otpSent) {
        if (uiState.otpSent) {
            viewModel.resetOtpSent()
            onOtpSent(phone)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Enter your\nphone number", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 40.sp)
            Text("We'll send you a verification code.", fontSize = 16.sp, color = TextSecondary)

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = { Text("+91 98765 43210", color = TextHint) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (phone.isNotBlank()) viewModel.sendOtp(phone) }),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Primary
                )
            )

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Button(
                onClick = { viewModel.sendOtp(phone) },
                enabled = phone.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Text("Send Code", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
