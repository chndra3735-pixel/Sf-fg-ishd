package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.viewmodel.UiState

@Composable
fun MessagesScreen(
  conversationsState: UiState<List<Conversation>>,
  activeConversationId: String?,
  directMessages: List<DirectMessage>,
  currentUserId: String,
  onOpenConversation: (String) -> Unit,
  onCloseConversation: () -> Unit,
  onSendDirectMessage: (Conversation, String) -> Unit,
  isUploadingMedia: Boolean = false,
  onSendDirectImage: (Conversation, Uri) -> Unit = { _, _ -> },
) {
  val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
  val activeConversation = conversations.firstOrNull { it.id == activeConversationId }

  if (activeConversation != null) {
    DirectChatThreadView(
      conversation = activeConversation,
      messages = directMessages,
      currentUserId = currentUserId,
      isUploadingMedia = isUploadingMedia,
      onBack = onCloseConversation,
      onSend = { text -> onSendDirectMessage(activeConversation, text) },
      onSendImage = { uri -> onSendDirectImage(activeConversation, uri) },
    )
    return
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
      Text(
        text = "रीयल-टाइम संदेश",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "वॉइस रूम दोस्तों के साथ सीधे टेक्स्ट चैट करें",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    when (conversationsState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = SwarCyan)
        }
      }
      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(conversationsState.message, color = MaterialTheme.colorScheme.error)
        }
      }
      is UiState.Success -> {
        if (conversations.isEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Chat,
              contentDescription = null,
              tint = SwarCyan,
              modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "कोई निजी बातचीत नहीं है",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "किसी भी वॉइस रूम में होस्ट या सीट के सदस्य पर टैप करके तुरंत रीयल-टाइम चैट शुरू करें।",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(conversations, key = { it.id }) { convo ->
              val otherUid = convo.participantUids.firstOrNull { it != currentUserId }.orEmpty()
              val otherName = convo.participantNames[otherUid].orEmpty().ifBlank { "Swar Friend" }
              val otherAvatar = convo.participantAvatars[otherUid].orEmpty()

              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onOpenConversation(convo.id) },
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  UserAvatar(
                    url = otherAvatar,
                    name = otherName,
                    size = 48.dp,
                    borderColor = SwarCyan,
                    borderWidth = 1.5.dp,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = otherName,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = convo.lastMessage,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DirectChatThreadView(
  conversation: Conversation,
  messages: List<DirectMessage>,
  currentUserId: String,
  isUploadingMedia: Boolean,
  onBack: () -> Unit,
  onSend: (String) -> Unit,
  onSendImage: (Uri) -> Unit,
) {
  BackHandler(onBack = onBack)

  val otherUid = conversation.participantUids.firstOrNull { it != currentUserId }.orEmpty()
  val otherName = conversation.participantNames[otherUid].orEmpty().ifBlank { "Swar Friend" }
  val otherAvatar = conversation.participantAvatars[otherUid].orEmpty()
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri -> uri?.let(onSendImage) },
  )

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .imePadding(),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "वापस जाएं")
        }
        UserAvatar(url = otherAvatar, name = otherName, size = 38.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(text = otherName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            text = "रीयल-टाइम चैट",
            style = MaterialTheme.typography.labelSmall,
            color = SwarCyan,
          )
        }
      }
    }

    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(messages, key = { it.id.ifBlank { "${it.senderUid}_${it.text.hashCode()}" } }) { msg ->
        val isMine = msg.senderUid == currentUserId
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        ) {
          Surface(
            color = if (isMine) SwarCyan else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
              topStart = 18.dp,
              topEnd = 18.dp,
              bottomStart = if (isMine) 18.dp else 4.dp,
              bottomEnd = if (isMine) 4.dp else 18.dp,
            ),
            modifier = Modifier.widthIn(max = 280.dp),
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
              if (msg.mediaUrl.isNotBlank()) {
                AsyncImage(
                  model = msg.mediaUrl,
                  contentDescription = "इनबॉक्स फ़ोटो / DP",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(modifier = Modifier.height(6.dp))
              }
              Text(
                text = msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMine) SwarDeepNight else MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = {
          photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        enabled = !isUploadingMedia,
        modifier = Modifier
          .size(48.dp)
          .testTag("dm_pick_image_button"),
      ) {
        if (isUploadingMedia) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = SwarCyan)
        } else {
          Icon(
            imageVector = Icons.Filled.PhotoLibrary,
            contentDescription = "DP / फ़ोटो भेजें",
            tint = Color(0xFF006CB5),
          )
        }
      }
      OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it.take(1000) },
        placeholder = { Text("संदेश लिखें...") },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("dm_input"),
      )
      Spacer(modifier = Modifier.width(8.dp))
      IconButton(
        onClick = {
          if (inputText.isNotBlank()) {
            onSend(inputText)
            inputText = ""
          }
        },
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(SwarCyan)
          .testTag("dm_send_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "भेजें", tint = SwarDeepNight)
      }
    }
  }
}
