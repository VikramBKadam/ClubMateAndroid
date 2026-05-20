package com.clubmates.app.ui.discover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.clubmates.app.domain.model.SwipeDirection
import com.clubmates.app.domain.model.UserProfile
import com.clubmates.app.domain.model.Venue
import com.clubmates.app.ui.components.AvatarImage
import com.clubmates.app.ui.components.GradientCard
import com.clubmates.app.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        if (!uiState.isCheckedIn) {
            VenuePickerScreen(
                venues = uiState.venues,
                onCheckIn = { viewModel.checkIn(it) },
                isLoading = uiState.isLoading
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(uiState.activeVenue?.name ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${uiState.roster.size} people here", fontSize = 14.sp, color = TextSecondary)
                    }
                    TextButton(onClick = { viewModel.checkOut() }) {
                        Text("Leave", color = TextSecondary)
                    }
                }

                if (uiState.venueEvent.isNotEmpty()) {
                    Text(
                        uiState.venueEvent,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Primary, fontSize = 14.sp
                    )
                }

                // Swipe card stack
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.profiles.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextHint, modifier = Modifier.size(64.dp))
                            Text("No more profiles here", color = TextSecondary, fontSize = 16.sp)
                            Text("Check back soon!", color = TextHint, fontSize = 14.sp)
                        }
                    } else {
                        // Back card
                        if (uiState.profiles.size > 1) {
                            ProfileCard(
                                profile = uiState.profiles[1],
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = 0.94f
                                        scaleY = 0.94f
                                        translationY = -20f
                                    }
                                    .zIndex(0f)
                            )
                        }
                        // Top card with drag
                        SwipeableCard(
                            profile = uiState.profiles[0],
                            onSwipe = { viewModel.swipe(uiState.profiles[0], it) },
                            modifier = Modifier.fillMaxWidth().zIndex(1f)
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (uiState.canUndo) viewModel.undo() },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Undo", tint = if (uiState.canUndo) TextSecondary else TextHint, modifier = Modifier.size(28.dp))
                    }
                    FloatingActionButton(
                        onClick = { if (uiState.profiles.isNotEmpty()) viewModel.swipe(uiState.profiles[0], SwipeDirection.NOPE) },
                        containerColor = SurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Nope", tint = Nope, modifier = Modifier.size(32.dp))
                    }
                    FloatingActionButton(
                        onClick = { if (uiState.profiles.isNotEmpty()) viewModel.swipe(uiState.profiles[0], SwipeDirection.SUPERLIKE) },
                        containerColor = SurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Super Like", tint = SuperLike, modifier = Modifier.size(26.dp))
                    }
                    FloatingActionButton(
                        onClick = { if (uiState.profiles.isNotEmpty()) viewModel.swipe(uiState.profiles[0], SwipeDirection.LIKE) },
                        containerColor = SurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Like", tint = Like, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        // Match alert
        uiState.matchResult?.let { result ->
            MatchAlertDialog(
                profile = result.profile,
                onDismiss = { viewModel.dismissMatch() },
                onSendMessage = { viewModel.dismissMatch() }
            )
        }
    }
}

@Composable
private fun SwipeableCard(
    profile: UserProfile,
    onSwipe: (SwipeDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = spring(dampingRatio = 0.7f), label = "offsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isDragging) offsetY else 0f,
        animationSpec = spring(dampingRatio = 0.7f), label = "offsetY"
    )

    val rotation = animatedOffsetX / 30f
    val likeOpacity = (animatedOffsetX / 150f).coerceIn(0f, 1f)
    val nopeOpacity = (-animatedOffsetX / 150f).coerceIn(0f, 1f)
    val superLikeOpacity = if (abs(animatedOffsetX) < 60) (-animatedOffsetY / 150f).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
            .rotate(rotation)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        when {
                            offsetX > 110 -> { offsetX = 0f; offsetY = 0f; onSwipe(SwipeDirection.LIKE) }
                            offsetX < -110 -> { offsetX = 0f; offsetY = 0f; onSwipe(SwipeDirection.NOPE) }
                            offsetY < -130 && abs(offsetX) < 60 -> { offsetX = 0f; offsetY = 0f; onSwipe(SwipeDirection.SUPERLIKE) }
                            else -> { offsetX = 0f; offsetY = 0f }
                        }
                    },
                    onDragCancel = { isDragging = false; offsetX = 0f; offsetY = 0f },
                    onDrag = { _, dragAmount -> offsetX += dragAmount.x; offsetY += dragAmount.y }
                )
            }
    ) {
        ProfileCard(profile = profile, modifier = Modifier.fillMaxWidth())

        // Like label
        if (likeOpacity > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(24.dp)
                    .background(Like.copy(alpha = likeOpacity * 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("LIKE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        }
        // Nope label
        if (nopeOpacity > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
                    .background(Nope.copy(alpha = nopeOpacity * 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("NOPE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        }
        // Super like label
        if (superLikeOpacity > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
                    .background(SuperLike.copy(alpha = superLikeOpacity * 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("SUPER LIKE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        }
    }
}

@Composable
fun ProfileCard(profile: UserProfile, modifier: Modifier = Modifier) {
    val photoUrl = profile.photos.firstOrNull()?.cdnUrl
    val gradientColors = listOf(Color(0xFF1A0A2E), Color(0xFF0D0D1A))

    Box(modifier = modifier.height(480.dp).background(CardBackground, RoundedCornerShape(20.dp))) {
        // Photo or gradient bg
        if (photoUrl != null) {
            coil.compose.AsyncImage(
                model = photoUrl,
                contentDescription = profile.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(CardBackground, RoundedCornerShape(20.dp))
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.linearGradient(gradientColors), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                AvatarImage(photoUrl = null, size = 100.dp)
            }
        }

        // Bottom info gradient overlay
        Box(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))), RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${profile.name}, ${profile.age}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (profile.isVerified) Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Primary, modifier = Modifier.size(20.dp))
                }
                if (profile.jobTitle.isNotBlank()) {
                    Text(profile.jobTitle, fontSize = 14.sp, color = TextSecondary)
                }
                if (profile.bio.isNotBlank()) {
                    Text(profile.bio, fontSize = 14.sp, color = TextSecondary, maxLines = 2)
                }
                if (profile.hasSuperLikedMe) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SuperLike, modifier = Modifier.size(16.dp))
                        Text("Super liked you", fontSize = 13.sp, color = SuperLike)
                    }
                }
            }
        }
    }
}
