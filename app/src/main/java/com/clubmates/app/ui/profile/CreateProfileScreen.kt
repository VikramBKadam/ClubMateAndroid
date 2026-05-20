package com.clubmates.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubmates.app.domain.model.UserProfile
import com.clubmates.app.ui.auth.AuthViewModel
import com.clubmates.app.ui.components.ClubMatesTextField
import com.clubmates.app.ui.theme.*

@Composable
fun CreateProfileScreen(
    viewModel: AuthViewModel,
    onProfileCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isAuthenticated, uiState.needsProfile) {
        if (uiState.isAuthenticated && !uiState.needsProfile) onProfileCreated()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Create your\nprofile", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 40.sp)
        Text("Tell us about yourself", fontSize = 16.sp, color = TextSecondary)

        ClubMatesTextField(value = name, onValueChange = { name = it }, label = "Name")
        ClubMatesTextField(value = age, onValueChange = { age = it.filter { c -> c.isDigit() } }, label = "Age", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        ClubMatesTextField(value = bio, onValueChange = { bio = it }, label = "Bio", maxLines = 3, singleLine = false)
        ClubMatesTextField(value = jobTitle, onValueChange = { jobTitle = it }, label = "Job Title (optional)")

        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }

        Button(
            onClick = {
                val profile = UserProfile(
                    id = "", name = name, age = age.toIntOrNull() ?: 0,
                    bio = bio, jobTitle = jobTitle, company = "", height = "",
                    interests = emptyList(), prompts = emptyList(), photos = emptyList(), isVerified = false
                )
                viewModel.saveProfile(profile)
            },
            enabled = name.isNotBlank() && age.isNotBlank() && bio.isNotBlank() && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            } else {
                Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
