package com.clubmates.app.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubmates.app.domain.model.Venue
import com.clubmates.app.ui.components.GradientCard
import com.clubmates.app.ui.theme.*

@Composable
fun VenuePickerScreen(
    venues: List<Venue>,
    onCheckIn: (Venue) -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp)
    ) {
        Text("Pick a venue", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(vertical = 16.dp))
        Text("Check in to discover people around you", fontSize = 15.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 20.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (venues.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No venues available right now", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(venues) { venue ->
                    VenueCard(venue = venue, onClick = { onCheckIn(venue) })
                }
            }
        }
    }
}

@Composable
fun VenueCard(venue: Venue, onClick: () -> Unit) {
    val gradientColors = venue.gradientHex.mapNotNull { hex ->
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
    }.ifEmpty { listOf(Color(0xFF2A0A4A), Color(0xFF0A1A3A)) }

    GradientCard(
        gradientColors = gradientColors,
        modifier = Modifier.fillMaxWidth().height(110.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(venue.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(venue.neighborhood, fontSize = 14.sp, color = TextSecondary)
                if (venue.vibe.isNotBlank()) {
                    Text(venue.vibe, fontSize = 12.sp, color = TextHint)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text("${venue.occupancy}", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                }
                Text("here now", fontSize = 11.sp, color = TextHint)
            }
        }
    }
}
