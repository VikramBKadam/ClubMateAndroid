package com.clubmates.app.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubmates.app.domain.model.ChatMessage
import com.clubmates.app.ui.components.AvatarImage
import com.clubmates.app.ui.theme.*

@Composable
fun ChatScreen(
    viewModel: MatchesViewModel,
    onBack: () -> Unit
) {
    val chatState by viewModel.chatState.collectAsState()
    val match = chatState.match ?: return
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    // Typing indicator events
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) viewModel.sendTypingStart()
        else viewModel.sendTypingStop()
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            AvatarImage(photoUrl = match.profile.photos.firstOrNull()?.cdnUrl, size = 40.dp)
            Column {
                Text(match.profile.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (chatState.isTyping) {
                    Text("typing...", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        HorizontalDivider(color = SurfaceVariant)

        // Messages
        if (chatState.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(chatState.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
                if (chatState.isTyping) {
                    item {
                        TypingBubble()
                    }
                }
            }
        }

        // Input bar
        HorizontalDivider(color = SurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...", color = TextHint) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Primary
                )
            )
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                enabled = messageText.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (messageText.isNotBlank()) Primary else TextHint)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bgColor = if (message.isFromMe) Primary else SurfaceVariant
    val textColor = TextPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bgColor, RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (message.isFromMe) 18.dp else 4.dp,
                    bottomEnd = if (message.isFromMe) 4.dp else 18.dp
                ))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(message.body, fontSize = 15.sp, color = textColor)
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(
        modifier = Modifier
            .background(SurfaceVariant, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(modifier = Modifier.size(6.dp).background(TextSecondary, RoundedCornerShape(50)))
        }
    }
}
