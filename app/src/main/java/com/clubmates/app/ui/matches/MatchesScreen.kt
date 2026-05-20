package com.clubmates.app.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubmates.app.domain.model.Match
import com.clubmates.app.ui.components.AvatarImage
import com.clubmates.app.ui.theme.*

@Composable
fun MatchesScreen(viewModel: MatchesViewModel) {
    val matchesState by viewModel.matchesState.collectAsState()
    val chatState by viewModel.chatState.collectAsState()

    if (chatState.match != null) {
        ChatScreen(
            viewModel = viewModel,
            onBack = { viewModel.closeChat() }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Text(
            "Matches",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        if (matchesState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (matchesState.matches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("No matches yet", color = TextSecondary, fontSize = 18.sp)
                    Text("Check in to a venue and start swiping!", color = TextHint, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn {
                items(matchesState.matches) { match ->
                    MatchRow(match = match, onClick = { viewModel.openChat(match) })
                }
            }
        }
    }
}

@Composable
private fun MatchRow(match: Match, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box {
            AvatarImage(photoUrl = match.profile.photos.firstOrNull()?.cdnUrl, size = 56.dp)
            if (match.isNew) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Primary, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    match.profile.name,
                    fontSize = 16.sp,
                    fontWeight = if (match.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary
                )
                if (match.lastMessageAt.isNotBlank()) {
                    Text(match.lastMessageAt.take(10), fontSize = 12.sp, color = TextHint)
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    match.lastMessage.ifBlank { "Say hello! 👋" },
                    fontSize = 14.sp,
                    color = if (match.unreadCount > 0) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (match.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(match.unreadCount.toString(), fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    HorizontalDivider(color = SurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(start = 86.dp))
}
