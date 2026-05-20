package com.clubmates.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.clubmates.app.ui.theme.*

private val ALL_INTERESTS = listOf(
    "Music", "Dancing", "Cocktails", "Wine", "Beer", "Coffee", "Food", "Travel",
    "Art", "Photography", "Fashion", "Fitness", "Yoga", "Running", "Hiking",
    "Tech", "Gaming", "Movies", "TV Shows", "Books", "Podcasts", "Comedy",
    "Sports", "Football", "Cricket", "Basketball", "Tennis", "Swimming",
    "Cooking", "Baking", "Meditation", "Astrology", "Board Games", "Karaoke"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InterestPickerSheet(
    selected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var current by remember { mutableStateOf(selected.toMutableSet()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Interests", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                TextButton(onClick = { onConfirm(current) }) { Text("Done", color = Primary, fontWeight = FontWeight.SemiBold) }
            }
            Text("Select up to 10", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ALL_INTERESTS.forEach { interest ->
                        val isSelected = interest in current
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Primary else SurfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = if (isSelected) 0.dp else 1.dp,
                                    color = if (isSelected) Primary else SurfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (isSelected) current = current.toMutableSet().also { it.remove(interest) }
                                    else if (current.size < 10) current = current.toMutableSet().also { it.add(interest) }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(interest, fontSize = 14.sp, color = if (isSelected) TextPrimary else TextSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
