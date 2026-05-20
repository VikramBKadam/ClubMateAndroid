package com.clubmates.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clubmates.app.domain.model.UserProfile
import com.clubmates.app.ui.components.AvatarImage
import com.clubmates.app.ui.theme.*

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEdit by remember { mutableStateOf(false) }

    if (showEdit && uiState.profile != null) {
        EditProfileScreen(
            profile = uiState.profile!!,
            viewModel = viewModel,
            onBack = { showEdit = false }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row {
                IconButton(onClick = { showEdit = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = TextSecondary)
                }
            }
        }

        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            uiState.profile == null -> Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("Could not load profile", color = TextSecondary)
            }
            else -> ProfileContent(profile = uiState.profile!!)
        }
    }
}

@Composable
private fun ProfileContent(profile: UserProfile) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Avatar + name
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AvatarImage(photoUrl = profile.photos.firstOrNull()?.cdnUrl, size = 80.dp)
            Column {
                Text("${profile.name}, ${profile.age}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (profile.jobTitle.isNotBlank()) Text(profile.jobTitle, fontSize = 15.sp, color = TextSecondary)
            }
        }

        if (profile.bio.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About", fontSize = 14.sp, color = TextHint, fontWeight = FontWeight.Medium)
                Text(profile.bio, fontSize = 15.sp, color = TextPrimary)
            }
        }

        if (profile.interests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Interests", fontSize = 14.sp, color = TextHint, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.interests.forEach { interest ->
                        Box(
                            modifier = Modifier.background(SurfaceVariant, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text(interest, fontSize = 13.sp, color = TextPrimary) }
                    }
                }
            }
        }

        if (profile.prompts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Prompts", fontSize = 14.sp, color = TextHint, fontWeight = FontWeight.Medium)
                profile.prompts.forEach { prompt ->
                    Column(
                        modifier = Modifier.fillMaxWidth().background(SurfaceVariant, RoundedCornerShape(14.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(prompt.question, fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium)
                        Text(prompt.answer, fontSize = 15.sp, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
