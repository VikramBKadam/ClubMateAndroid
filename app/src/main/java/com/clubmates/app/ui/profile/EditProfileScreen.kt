package com.clubmates.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubmates.app.domain.model.UserProfile
import com.clubmates.app.ui.components.ClubMatesTextField
import com.clubmates.app.ui.theme.*

@Composable
fun EditProfileScreen(
    profile: UserProfile,
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf(profile.age.toString()) }
    var bio by remember { mutableStateOf(profile.bio) }
    var jobTitle by remember { mutableStateOf(profile.jobTitle) }
    var company by remember { mutableStateOf(profile.company) }
    var height by remember { mutableStateOf(profile.height) }
    var prompts by remember { mutableStateOf(profile.prompts.toMutableList()) }
    var showInterestPicker by remember { mutableStateOf(false) }
    var interests by remember { mutableStateOf(profile.interests.toMutableList()) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            onBack()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Edit Profile", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            TextButton(
                onClick = {
                    val updated = profile.copy(
                        name = name, age = age.toIntOrNull() ?: profile.age,
                        bio = bio, jobTitle = jobTitle, company = company,
                        height = height, interests = interests.toList(),
                        prompts = prompts.toList()
                    )
                    viewModel.saveProfile(updated)
                },
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text("Save", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = SurfaceVariant)

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionLabel("Basic Info")
            ClubMatesTextField(value = name, onValueChange = { name = it }, label = "Name")
            ClubMatesTextField(value = age, onValueChange = { age = it.filter { c -> c.isDigit() }.take(2) }, label = "Age", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            ClubMatesTextField(value = bio, onValueChange = { bio = it.take(150) }, label = "Bio (${bio.length}/150)", singleLine = false, maxLines = 3)

            SectionLabel("Work")
            ClubMatesTextField(value = jobTitle, onValueChange = { jobTitle = it }, label = "Job Title")
            ClubMatesTextField(value = company, onValueChange = { company = it }, label = "Company")

            SectionLabel("Details")
            ClubMatesTextField(value = height, onValueChange = { height = it }, label = "Height (e.g. 5'10\")")

            // Interests
            SectionLabel("Interests")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${interests.size} selected", fontSize = 14.sp, color = TextSecondary)
                TextButton(onClick = { showInterestPicker = true }) { Text("Edit", color = Primary) }
            }

            // Prompts
            SectionLabel("Icebreakers (${prompts.size}/3)")
            prompts.forEachIndexed { index, prompt ->
                Column(
                    modifier = Modifier.fillMaxWidth().background(SurfaceVariant, RoundedCornerShape(14.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Prompt ${index + 1}", fontSize = 13.sp, color = TextHint)
                        IconButton(onClick = { prompts = prompts.toMutableList().also { it.removeAt(index) } }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Nope, modifier = Modifier.size(18.dp))
                        }
                    }
                    OutlinedTextField(
                        value = prompt.question,
                        onValueChange = { q -> prompts = prompts.toMutableList().also { it[index] = prompt.copy(question = q) } },
                        label = { Text("Question", color = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = SurfaceVariant.copy(alpha = 0.3f), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = prompt.answer,
                        onValueChange = { a -> prompts = prompts.toMutableList().also { it[index] = prompt.copy(answer = a) } },
                        label = { Text("Answer", color = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = SurfaceVariant.copy(alpha = 0.3f), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            }

            if (prompts.size < 3) {
                OutlinedButton(
                    onClick = { prompts = prompts.toMutableList().also { it.add(UserProfile.Prompt(question = "", answer = "")) } },
                    modifier = Modifier.fillMaxWidth(),
                    border = ButtonDefaults.outlinedButtonBorder.copy(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Prompt")
                }
            }

            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showInterestPicker) {
        InterestPickerSheet(
            selected = interests.toSet(),
            onDismiss = { showInterestPicker = false },
            onConfirm = { selected -> interests = selected.toMutableList(); showInterestPicker = false }
        )
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, fontSize = 13.sp, color = TextHint, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
}
