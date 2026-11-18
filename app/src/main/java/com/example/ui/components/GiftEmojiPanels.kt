package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EmojiCatalogItem
import com.example.data.model.GiftCatalogItem
import com.example.data.model.RoomSeat
import com.example.data.model.SwarCatalogDefaults
import com.example.data.model.VoiceRoom
import com.example.data.repository.MediaCompressor
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EmojiPanelSheet(
  emojis: List<EmojiCatalogItem>,
  isAdminUser: Boolean,
  isUploading: Boolean,
  onSelectEmoji: (EmojiCatalogItem) -> Unit,
  onAddCustomEmoji: (label: String, category: String, symbol: String, imageUri: Uri?, onDone: () -> Unit) -> Unit,
  onUpdateCustomEmoji: (
    existing: EmojiCatalogItem,
    label: String,
    category: String,
    symbol: String,
    newImageUri: Uri?,
    removeExistingImage: Boolean,
    onDone: () -> Unit,
  ) -> Unit,
  onDeleteCustomEmoji: (item: EmojiCatalogItem, onDone: () -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  val categories = SwarCatalogDefaults.emojiCategories
  var selectedCategoryIdx by remember { mutableIntStateOf(0) }
  var showAdminAddDialog by remember { mutableStateOf(false) }
  var editingEmoji by remember { mutableStateOf<EmojiCatalogItem?>(null) }
  var deletingDefaultEmoji by remember { mutableStateOf<EmojiCatalogItem?>(null) }

  val currentCategory = categories[selectedCategoryIdx]
  val filteredEmojis = remember(emojis, currentCategory) {
    emojis.filter { it.category == currentCategory }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF1A1133),
    contentColor = Color.White,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "इमोजी और रिएक्शन पैनल",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = if (isAdminUser) {
              "किसी भी इमोजी को दबाकर रखें (Update / Delete)"
            } else {
              "लाइव रूम में तुरंत इमोजी भेजें"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isAdminUser) SwarCyan else Color(0xFFB5ACDA),
          )
        }

        // Admin-only '+' Icon (strictly visible only for chndra3735@gmail.com)
        if (isAdminUser) {
          IconButton(
            onClick = { showAdminAddDialog = true },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(SwarCyan, SwarViolet)))
              .testTag("admin_add_emoji_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "नया इमोजी जोड़ें",
              tint = Color.White,
            )
          }
        }
      }

      ScrollableTabRow(
        selectedTabIndex = selectedCategoryIdx,
        containerColor = Color.Transparent,
        contentColor = SwarCyan,
        edgePadding = 12.dp,
      ) {
        categories.forEachIndexed { idx, cat ->
          Tab(
            selected = selectedCategoryIdx == idx,
            onClick = { selectedCategoryIdx = idx },
            text = {
              Text(
                text = cat,
                fontWeight = if (selectedCategoryIdx == idx) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedCategoryIdx == idx) SwarCyan else Color(0xFFB5ACDA),
              )
            },
          )
        }
      }

      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
          .fillMaxWidth()
          .height(260.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filteredEmojis, key = { it.id.ifBlank { "${it.category}_${it.label}" } }) { item ->
          val isCustomEmoji = !item.id.startsWith("def_") && item.createdByUid.isNotBlank()
          Surface(
            color = Color(0xFF281C4C),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
              .combinedClickable(
                onClick = {
                  onSelectEmoji(item)
                  onDismiss()
                },
                onLongClick = {
                  if (isAdminUser) {
                    if (isCustomEmoji) editingEmoji = item else deletingDefaultEmoji = item
                  }
                },
              ),
          ) {
            Box(modifier = Modifier.fillMaxWidth()) {
              if (isAdminUser && isCustomEmoji) {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(SwarCyan.copy(alpha = 0.25f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "इमोजी संपादित करें",
                    tint = SwarCyan,
                    modifier = Modifier.size(10.dp),
                  )
                }
              }

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                if (item.imageUrl.isNotBlank()) {
                  AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.label,
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                  )
                } else {
                  Text(
                    text = item.symbol.ifBlank { "😊" },
                    fontSize = 32.sp,
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = item.label,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
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

  if (showAdminAddDialog && isAdminUser) {
    AddEmojiAdminDialog(
      defaultCategory = currentCategory,
      isUploading = isUploading,
      onDismiss = { showAdminAddDialog = false },
      onSubmit = { label, category, symbol, imageUri ->
        onAddCustomEmoji(label, category, symbol, imageUri) {
          showAdminAddDialog = false
        }
      },
    )
  }

  val targetEditEmoji = editingEmoji
  if (targetEditEmoji != null && isAdminUser) {
    EditEmojiAdminDialog(
      existing = targetEditEmoji,
      isBusy = isUploading,
      onDismiss = { editingEmoji = null },
      onUpdate = { label, category, symbol, newImageUri, removeExistingImage ->
        onUpdateCustomEmoji(targetEditEmoji, label, category, symbol, newImageUri, removeExistingImage) {
          editingEmoji = null
        }
      },
      onDelete = {
        onDeleteCustomEmoji(targetEditEmoji) {
          editingEmoji = null
        }
      },
    )
  }

  val targetDeleteDefaultEmoji = deletingDefaultEmoji
  if (targetDeleteDefaultEmoji != null && isAdminUser) {
    AlertDialog(
      onDismissRequest = { deletingDefaultEmoji = null },
      title = { Text("इमोजी डिलीट करें?") },
      text = { Text("क्या आप '${targetDeleteDefaultEmoji.label}' को इमोजी पैनल से हटाना चाहते हैं?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCustomEmoji(targetDeleteDefaultEmoji) {
              deletingDefaultEmoji = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), contentColor = Color.White),
        ) {
          Text("डिलीट करें")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingDefaultEmoji = null }) { Text("रद्द करें") }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GiftPanelSheet(
  room: VoiceRoom,
  seats: List<RoomSeat>,
  gifts: List<GiftCatalogItem>,
  userCoins: Long,
  isAdminUser: Boolean,
  isUploading: Boolean,
  onClaimBonusCoins: () -> Unit,
  onSendGift: (gift: GiftCatalogItem, recipientUid: String, recipientName: String, quantity: Int) -> Unit,
  onAddCustomGift: (
    name: String,
    category: String,
    emoji: String,
    coinCost: Long,
    thumbnailUri: Uri?,
    dpLinkUrl: String,
    videoLinkUrl: String,
    onDone: () -> Unit,
  ) -> Unit,
  onUpdateCustomGift: (
    existing: GiftCatalogItem,
    name: String,
    category: String,
    emoji: String,
    coinCost: Long,
    newThumbnailUri: Uri?,
    newDpLinkUrl: String,
    newVideoLinkUrl: String,
    removeExistingThumbnail: Boolean,
    removeExistingVideo: Boolean,
    onDone: () -> Unit,
  ) -> Unit,
  onDeleteCustomGift: (gift: GiftCatalogItem, onDone: () -> Unit) -> Unit,
  onDismiss: () -> Unit,
  giftUploadError: String? = null,
  onClearGiftUploadError: () -> Unit = {},
) {
  val categories = SwarCatalogDefaults.giftCategories
  var selectedCategoryIdx by remember { mutableIntStateOf(0) }
  val currentCategory = categories[selectedCategoryIdx]
  val filteredGifts = remember(gifts, currentCategory) {
    gifts.filter { it.category == currentCategory }
  }

  // Recipient options: Host + seated guests + All Members
  val recipients = remember(room.id, seats) {
    buildList {
      add(room.ownerId to "होस्ट • ${room.ownerName}")
      seats
        .filter { it.occupantUid.isNotBlank() && it.occupantUid != room.ownerId }
        .distinctBy { it.occupantUid }
        .forEach { seat ->
          add(seat.occupantUid to seat.occupantName)
        }
      add("all" to "सभी सदस्य")
    }
  }
  var selectedRecipient by remember(recipients) { mutableStateOf(recipients.first()) }
  var selectedGiftId by remember { mutableStateOf<String?>(null) }
  val selectedGift = remember(selectedGiftId, filteredGifts, gifts) {
    gifts.firstOrNull { it.id == selectedGiftId }
      ?: filteredGifts.firstOrNull()
      ?: gifts.firstOrNull()
  }
  var selectedQuantity by remember { mutableIntStateOf(1) }
  var showAdminAddGiftDialog by remember { mutableStateOf(false) }
  var editingGift by remember { mutableStateOf<GiftCatalogItem?>(null) }
  var deletingDefaultGift by remember { mutableStateOf<GiftCatalogItem?>(null) }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF170E2E),
    contentColor = Color.White,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
    ) {
      // Header + Coins + Admin-only '+' Button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "शाही गिफ्ट पैनल",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = if (isAdminUser) {
              "किसी भी गिफ्ट को दबाकर रखें (Update / Delete)"
            } else {
              "भेजा गया गिफ्ट रूम और ग्लोबल पट्टी में चलेगा"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isAdminUser) SwarCyan else Color(0xFFB5ACDA),
          )
        }

        CoinBalancePill(coins = userCoins, onAddCoinsClick = onClaimBonusCoins)

        // Admin-only '+' Icon (strictly visible only for chndra3735@gmail.com)
        if (isAdminUser) {
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = {
              onClearGiftUploadError()
              showAdminAddGiftDialog = true
            },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(SwarCoral, SwarViolet)))
              .testTag("admin_add_gift_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "नया गिफ्ट या वीडियो जोड़ें",
              tint = Color.White,
            )
          }
        }
      }

      // Recipient Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "किसे भेजें:",
          style = MaterialTheme.typography.labelMedium,
          color = Color(0xFFD7CFF6),
        )
        recipients.forEach { recipient ->
          val isSelected = selectedRecipient.first == recipient.first
          FilterChip(
            selected = isSelected,
            onClick = { selectedRecipient = recipient },
            label = { Text(recipient.second, maxLines = 1) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SwarCoral,
              selectedLabelColor = Color.White,
              labelColor = Color(0xFFD0C9EC),
            ),
          )
        }
      }

      // 4 Gift Categories
      ScrollableTabRow(
        selectedTabIndex = selectedCategoryIdx,
        containerColor = Color.Transparent,
        contentColor = SwarGold,
        edgePadding = 12.dp,
      ) {
        categories.forEachIndexed { idx, cat ->
          Tab(
            selected = selectedCategoryIdx == idx,
            onClick = { selectedCategoryIdx = idx },
            text = {
              Text(
                text = cat,
                fontWeight = if (selectedCategoryIdx == idx) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedCategoryIdx == idx) SwarGold else Color(0xFFB5ACDA),
              )
            },
          )
        }
      }

      // Gifts Grid (or empty state when admin cleared / before adding gifts)
      if (filteredGifts.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("empty_gift_catalog_state"),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            color = Color(0xFF231642),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(18.dp)),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(text = "🎁", fontSize = 32.sp)
              Text(
                text = if (gifts.isEmpty()) "गिफ्ट पैनल अभी खाली है" else "$currentCategory श्रेणी में कोई गिफ्ट नहीं है",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
              )
              Text(
                text = if (isAdminUser) {
                  "नया वीडियो या फोटो गिफ्ट लिंक + DP जोड़ने के लिए '+' बटन दबाएं।"
                } else {
                  "नए गिफ्ट एडमिन द्वारा लिंक के ज़रिए जोड़े जाएंगे।"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC8C0E8),
                textAlign = TextAlign.Center,
              )
              if (isAdminUser) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                  onClick = {
                    onClearGiftUploadError()
                    showAdminAddGiftDialog = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = SwarCoral, contentColor = Color.White),
                  shape = RoundedCornerShape(20.dp),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("empty_add_gift_button"),
                ) {
                  Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("नया गिफ्ट जोड़ें (+)", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(4),
          modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
          contentPadding = PaddingValues(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(filteredGifts, key = { it.id.ifBlank { "${it.category}_${it.name}" } }) { gift ->
          val isPicked = selectedGift?.id == gift.id
          val isCustomGift = !gift.id.startsWith("def_")
          val borderColor = if (isPicked) SwarGold else Color(0x33FFFFFF)
          Surface(
            color = if (isPicked) Color(0xFF352263) else Color(0xFF231642),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .border(if (isPicked) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
              .combinedClickable(
                onClick = { selectedGiftId = gift.id },
                onLongClick = {
                  if (isAdminUser) {
                    if (isCustomGift) editingGift = gift else deletingDefaultGift = gift
                  }
                },
              ),
          ) {
            Box(modifier = Modifier.fillMaxWidth()) {
              if (isAdminUser && isCustomGift) {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(SwarCyan.copy(alpha = 0.25f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "गिफ्ट एडिट या डिलीट करें",
                    tint = SwarCyan,
                    modifier = Modifier.size(10.dp),
                  )
                }
              }

              if (gift.mediaType == "video") {
                Surface(
                  color = SwarCoral,
                  shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 14.dp),
                  modifier = Modifier.align(Alignment.TopEnd),
                ) {
                  Text(
                    text = "VIDEO",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                  )
                }
              }

              val dpModel = gift.thumbnailUrl.ifBlank {
                if (gift.mediaType == "image") gift.mediaUrl else ""
              }
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                if (dpModel.isNotBlank()) {
                  AsyncImage(
                    model = dpModel,
                    contentDescription = gift.name,
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .border(
                        width = if (isPicked) 2.dp else 1.dp,
                        color = if (isPicked) SwarGold else Color(0x55FFFFFF),
                        shape = CircleShape,
                      ),
                    contentScale = ContentScale.Crop,
                  )
                } else {
                  Box(
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .background(Color(0x22FFFFFF)),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(text = gift.emoji.ifBlank { "🎁" }, fontSize = 26.sp)
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = gift.name,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  CoinEmblem(sizeDp = 12)
                  Text(
                    text = "${gift.coinCost}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SwarGold,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
        }
      }

      // Bottom Send Bar: Quantity x1/x5/x10 + Send Button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(1, 5, 10).forEach { qty ->
            val active = selectedQuantity == qty
            Surface(
              onClick = { selectedQuantity = qty },
              color = if (active) SwarGold else Color(0xFF2B1D52),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(
                text = "x$qty",
                style = MaterialTheme.typography.labelMedium,
                color = if (active) Color(0xFF3A2000) else Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              )
            }
          }
        }

        val activeGift = selectedGift
        val totalCost = (activeGift?.coinCost ?: 0L) * selectedQuantity
        Button(
          onClick = {
            if (activeGift != null) {
              onSendGift(activeGift, selectedRecipient.first, selectedRecipient.second, selectedQuantity)
              onDismiss()
            }
          },
          enabled = activeGift != null,
          colors = ButtonDefaults.buttonColors(
            containerColor = SwarCoral,
            contentColor = Color.White,
          ),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier
            .height(48.dp)
            .testTag("send_gift_button"),
        ) {
          CoinEmblem(sizeDp = 16)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "भेजें ($totalCost)",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }

  if (showAdminAddGiftDialog && isAdminUser) {
    AddGiftAdminDialog(
      defaultCategory = currentCategory,
      isUploading = isUploading,
      uploadError = giftUploadError,
      onDismiss = {
        onClearGiftUploadError()
        showAdminAddGiftDialog = false
      },
      onSubmit = { name, category, emoji, cost, thumbUri, dpLinkUrl, videoLinkUrl ->
        onClearGiftUploadError()
        onAddCustomGift(name, category, emoji, cost, thumbUri, dpLinkUrl, videoLinkUrl) {
          showAdminAddGiftDialog = false
        }
      },
    )
  }

  val targetEditGift = editingGift
  if (targetEditGift != null && isAdminUser) {
    EditGiftAdminDialog(
      existing = targetEditGift,
      isBusy = isUploading,
      uploadError = giftUploadError,
      onDismiss = {
        onClearGiftUploadError()
        editingGift = null
      },
      onUpdate = { name, category, emoji, cost, newThumbUri, newDpLinkUrl, newVideoLinkUrl, removeExistingThumb, removeExistingVideo ->
        onUpdateCustomGift(
          targetEditGift,
          name,
          category,
          emoji,
          cost,
          newThumbUri,
          newDpLinkUrl,
          newVideoLinkUrl,
          removeExistingThumb,
          removeExistingVideo,
        ) {
          editingGift = null
        }
      },
      onDelete = {
        onDeleteCustomGift(targetEditGift) {
          if (selectedGiftId == targetEditGift.id) {
            selectedGiftId = null
          }
          editingGift = null
        }
      },
    )
  }

  val targetDeleteDefaultGift = deletingDefaultGift
  if (targetDeleteDefaultGift != null && isAdminUser) {
    AlertDialog(
      onDismissRequest = { deletingDefaultGift = null },
      title = { Text("गिफ्ट डिलीट करें?") },
      text = { Text("क्या आप '${targetDeleteDefaultGift.name}' को गिफ्ट पैनल से हटाना चाहते हैं?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCustomGift(targetDeleteDefaultGift) {
              if (selectedGiftId == targetDeleteDefaultGift.id) {
                selectedGiftId = null
              }
              deletingDefaultGift = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), contentColor = Color.White),
          modifier = Modifier.testTag("confirm_delete_gift_button"),
        ) {
          Text("गिफ्ट पैनल से हटाएं")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingDefaultGift = null }) { Text("रद्द करें") }
      },
    )
  }
}

@Composable
private fun AddEmojiAdminDialog(
  defaultCategory: String,
  isUploading: Boolean,
  onDismiss: () -> Unit,
  onSubmit: (label: String, category: String, symbol: String, imageUri: Uri?) -> Unit,
) {
  val context = LocalContext.current
  var label by remember { mutableStateOf("") }
  var symbol by remember { mutableStateOf("✨") }
  var category by remember { mutableStateOf(defaultCategory) }
  var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

  val pickedFileName = remember(pickedImageUri) {
    pickedImageUri?.let { MediaCompressor.queryUriDisplayName(context, it) }.orEmpty()
  }
  val isPickedZip = pickedFileName.endsWith(".zip", ignoreCase = true)

  val documentPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
      if (uri != null) {
        pickedImageUri = uri
      }
    },
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("नया इमोजी / GIF / ZIP जोड़ें") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = label,
          onValueChange = { label = it.take(30) },
          label = { Text("इमोजी का नाम") },
          placeholder = { Text("उदा. रॉयल धमाका") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = symbol,
          onValueChange = { symbol = it.take(16) },
          label = { Text("इमोजी चिह्न (वैकल्पिक यदि GIF/ZIP चुनें)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("श्रेणी चुनें", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SwarCatalogDefaults.emojiCategories.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat) },
            )
          }
        }
        OutlinedButton(
          onClick = {
            documentPicker.launch(
              arrayOf(
                "image/*",
                "image/gif",
                "application/zip",
                "application/x-zip-compressed",
              )
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pick_emoji_media_button"),
        ) {
          Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (pickedImageUri == null) "GIF / ZIP / इमेज फ़ाइल चुनें"
            else "चयनित: ${pickedFileName.ifBlank { "मीडिया फ़ाइल" }} ✓",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (pickedImageUri != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            if (!isPickedZip) {
              AsyncImage(
                model = pickedImageUri,
                contentDescription = "चयनित इमोजी प्रीव्यू",
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .border(1.dp, SwarCyan, CircleShape),
                contentScale = ContentScale.Crop,
              )
            }
            Text(
              text = if (isPickedZip) {
                "ZIP पैक तैयार है — अंदर मौजूद सभी GIF/इमेज ऑटो अपलोड होंगे।"
              } else {
                "डीपी के गोल साइज में लाइव एनीमेशन प्रीव्यू तैयार है।"
              },
              style = MaterialTheme.typography.bodySmall,
              color = SwarMint,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubmit(
            label.ifBlank { pickedFileName.substringBeforeLast('.').take(30).ifBlank { "नया इमोजी" } },
            category,
            symbol,
            pickedImageUri,
          )
        },
        enabled = !isUploading && (symbol.isNotBlank() || pickedImageUri != null),
      ) {
        Text(if (isUploading) "अपलोड हो रहा है..." else "सेव करें")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("रद्द करें") }
    },
  )
}

@Composable
private fun AddGiftAdminDialog(
  defaultCategory: String,
  isUploading: Boolean,
  uploadError: String? = null,
  onDismiss: () -> Unit,
  onSubmit: (
    name: String,
    category: String,
    emoji: String,
    coinCost: Long,
    thumbnailUri: Uri?,
    dpLinkUrl: String,
    videoLinkUrl: String,
  ) -> Unit,
) {
  var videoLinkUrl by remember { mutableStateOf("") }
  var dpLinkUrl by remember { mutableStateOf("") }
  var thumbnailUri by remember { mutableStateOf<Uri?>(null) }
  var name by remember { mutableStateOf("") }
  var coinCostText by remember { mutableStateOf("500") }
  var category by remember { mutableStateOf(defaultCategory) }
  var emoji by remember { mutableStateOf("🎁") }

  val thumbPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        thumbnailUri = uri
      }
    },
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("नया गिफ्ट जोड़ें (लिंक सेटअप)") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!uploadError.isNullOrBlank()) {
          Surface(
            color = Color(0x33FF3B30),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(10.dp)),
          ) {
            Text(
              text = uploadError,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFFF8A80),
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        // 1. सबसे पहले: गिफ्ट वीडियो लिंक (URL)
        OutlinedTextField(
          value = videoLinkUrl,
          onValueChange = { videoLinkUrl = it.take(750) },
          leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = SwarCyan) },
          label = { Text("1. गिफ्ट वीडियो लिंक (URL)") },
          placeholder = { Text("https://... (.mp4 / Reel / pin.it)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("gift_video_link_input"),
        )

        // 2. उसके नीचे: गिफ्ट की DP (फोटो लिंक या गैलरी से फोटो अपलोड — पैनल में दिखने के लिए)
        OutlinedTextField(
          value = dpLinkUrl,
          onValueChange = { dpLinkUrl = it.take(750) },
          leadingIcon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = SwarGold) },
          label = { Text("2. गिफ्ट DP फोटो लिंक (URL - वैकल्पिक)") },
          placeholder = { Text("https://... (.jpg / .png / pin.it)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("gift_dp_link_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              thumbPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("gift_dp_picker_button"),
          ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (thumbnailUri == null) "या DP फोटो अपलोड करें" else "DP फोटो चयनित ✓",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          val dpPreviewModel: Any? = thumbnailUri ?: dpLinkUrl.trim().takeIf {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
          }
          if (dpPreviewModel != null) {
            AsyncImage(
              model = dpPreviewModel,
              contentDescription = "गिफ्ट DP प्रीव्यू",
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.5.dp, SwarGold, CircleShape),
              contentScale = ContentScale.Crop,
            )
          }
        }

        // 3. नाम और कॉइंस
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("गिफ्ट का नाम") },
            placeholder = { Text("शाही गिफ्ट") },
            singleLine = true,
            modifier = Modifier
              .weight(1.3f)
              .testTag("gift_name_input"),
          )

          OutlinedTextField(
            value = coinCostText,
            onValueChange = { coinCostText = it.filter { ch -> ch.isDigit() }.take(6) },
            label = { Text("कॉइंस") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
              .weight(0.9f)
              .testTag("gift_coins_input"),
          )
        }

        // 4. श्रेणी चयन
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("श्रेणी:", style = MaterialTheme.typography.labelMedium)
          SwarCatalogDefaults.giftCategories.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat) },
            )
          }
        }

        if (videoLinkUrl.isNotBlank()) {
          Surface(
            color = SwarMint.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = "लिंक और DP सेव होते ही गिफ्ट पैनल में DP दिखेगी व भेजने पर यही वीडियो प्ले होगा।",
              style = MaterialTheme.typography.bodySmall,
              color = SwarMint,
              modifier = Modifier.padding(8.dp),
            )
          }
        }

        Button(
          onClick = {
            val parsedCost = coinCostText.toLongOrNull()?.coerceIn(1L, 999999L) ?: 100L
            onSubmit(
              name.ifBlank { "स्पेशल गिफ्ट" },
              category,
              emoji.ifBlank { "🎁" },
              parsedCost,
              thumbnailUri,
              dpLinkUrl.trim(),
              videoLinkUrl.trim(),
            )
          },
          enabled = !isUploading &&
            (videoLinkUrl.isNotBlank() || dpLinkUrl.isNotBlank() || thumbnailUri != null),
          colors = ButtonDefaults.buttonColors(
            containerColor = SwarCoral,
            contentColor = Color.White,
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_gift_button"),
        ) {
          Text(
            text = if (isUploading) "सेव व लोड हो रहा है..." else "सबमिट करें",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("रद्द करें") }
    },
  )
}

@Composable
private fun EditEmojiAdminDialog(
  existing: EmojiCatalogItem,
  isBusy: Boolean,
  onDismiss: () -> Unit,
  onUpdate: (
    label: String,
    category: String,
    symbol: String,
    newImageUri: Uri?,
    removeExistingImage: Boolean,
  ) -> Unit,
  onDelete: () -> Unit,
) {
  var label by remember(existing.id) { mutableStateOf(existing.label) }
  var symbol by remember(existing.id) { mutableStateOf(existing.symbol) }
  var category by remember(existing.id) {
    mutableStateOf(
      if (existing.category in SwarCatalogDefaults.emojiCategories) existing.category
      else SwarCatalogDefaults.emojiCategories.first()
    )
  }
  var newImageUri by remember(existing.id) { mutableStateOf<Uri?>(null) }
  var removeExistingImage by remember(existing.id) { mutableStateOf(false) }
  var showDeleteConfirm by remember(existing.id) { mutableStateOf(false) }

  val imagePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
      if (uri != null) {
        newImageUri = uri
        removeExistingImage = false
      }
    },
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("इमोजी अपडेट / डिलीट (एडमिन)") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = label,
          onValueChange = { label = it.take(30) },
          label = { Text("इमोजी का नाम") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = symbol,
          onValueChange = { symbol = it.take(16) },
          label = { Text("इमोजी चिह्न") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("श्रेणी चुनें", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SwarCatalogDefaults.emojiCategories.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat) },
            )
          }
        }
        OutlinedButton(
          onClick = {
            imagePicker.launch(
              arrayOf("image/*", "image/gif", "application/zip", "application/x-zip-compressed")
            )
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            when {
              newImageUri != null -> "नया GIF / इमेज चयनित ✓"
              existing.imageUrl.isNotBlank() && !removeExistingImage -> "मौजूदा GIF / इमेज बदलें"
              else -> "नया GIF / ZIP / इमेज चुनें"
            }
          )
        }
        if (existing.imageUrl.isNotBlank() && !removeExistingImage && newImageUri == null) {
          TextButton(onClick = { removeExistingImage = true }) {
            Text("पुरानी इमेज हटाएं", color = SwarCoral)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = { showDeleteConfirm = true },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFB71C1C),
            contentColor = Color.White,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("delete_custom_emoji_button"),
        ) {
          Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("इमोजी पैनल से डिलीट करें")
        }
      }
    },
    confirmButton = {
      val hasAnyVisual = symbol.isNotBlank() || newImageUri != null ||
        (existing.imageUrl.isNotBlank() && !removeExistingImage)
      Button(
        onClick = {
          onUpdate(
            label.ifBlank { "कस्टम इमोजी" },
            category,
            symbol,
            newImageUri,
            removeExistingImage,
          )
        },
        enabled = !isBusy && hasAnyVisual,
      ) {
        Text(if (isBusy) "प्रक्रिया जारी..." else "अपडेट करें")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("रद्द करें") }
    },
  )

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("इमोजी डिलीट करें?") },
      text = { Text("क्या आप '${existing.label}' को इमोजी पैनल से हमेशा के लिए हटाना चाहते हैं?") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDelete()
          },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), contentColor = Color.White),
        ) {
          Text("हाँ, डिलीट करें")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("नहीं") }
      },
    )
  }
}

@Composable
private fun EditGiftAdminDialog(
  existing: GiftCatalogItem,
  isBusy: Boolean,
  uploadError: String? = null,
  onDismiss: () -> Unit,
  onUpdate: (
    name: String,
    category: String,
    emoji: String,
    coinCost: Long,
    newThumbnailUri: Uri?,
    newDpLinkUrl: String,
    newVideoLinkUrl: String,
    removeExistingThumbnail: Boolean,
    removeExistingVideo: Boolean,
  ) -> Unit,
  onDelete: () -> Unit,
) {
  var videoLinkUrl by remember(existing.id) {
    mutableStateOf(if (existing.mediaType == "video") existing.mediaUrl else "")
  }
  var dpLinkUrl by remember(existing.id) { mutableStateOf(existing.thumbnailUrl) }
  var newThumbUri by remember(existing.id) { mutableStateOf<Uri?>(null) }
  var name by remember(existing.id) { mutableStateOf(existing.name) }
  var emoji by remember(existing.id) { mutableStateOf(existing.emoji.ifBlank { "🎁" }) }
  var coinCostText by remember(existing.id) { mutableStateOf(existing.coinCost.toString()) }
  var category by remember(existing.id) {
    mutableStateOf(
      if (existing.category in SwarCatalogDefaults.giftCategories) existing.category
      else SwarCatalogDefaults.giftCategories.first()
    )
  }
  var removeExistingThumb by remember(existing.id) { mutableStateOf(false) }
  var removeExistingVideo by remember(existing.id) { mutableStateOf(false) }
  var showDeleteConfirm by remember(existing.id) { mutableStateOf(false) }

  val hasExistingVideo = existing.mediaType == "video" && existing.mediaUrl.isNotBlank()

  val thumbPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        newThumbUri = uri
        removeExistingThumb = false
      }
    },
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("गिफ्ट अपडेट / डिलीट (एडमिन)") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!uploadError.isNullOrBlank()) {
          Surface(
            color = Color(0x33FF3B30),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(10.dp)),
          ) {
            Text(
              text = uploadError,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFFF8A80),
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        // 1. गिफ्ट वीडियो लिंक (URL)
        OutlinedTextField(
          value = videoLinkUrl,
          onValueChange = {
            videoLinkUrl = it.take(750)
            removeExistingVideo = it.isBlank()
          },
          leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = SwarCyan) },
          label = { Text("1. गिफ्ट वीडियो लिंक (URL)") },
          placeholder = { Text("https://... (.mp4 / Reel / pin.it)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (hasExistingVideo && !removeExistingVideo) {
          TextButton(
            onClick = {
              videoLinkUrl = ""
              removeExistingVideo = true
            },
          ) {
            Text("मौजूदा वीडियो लिंक हटाएं", color = SwarCoral)
          }
        }

        // 2. गिफ्ट DP फोटो लिंक या गैलरी फोटो अपलोड
        OutlinedTextField(
          value = dpLinkUrl,
          onValueChange = {
            dpLinkUrl = it.take(750)
            if (it.isNotBlank()) removeExistingThumb = false
          },
          leadingIcon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = SwarGold) },
          label = { Text("2. गिफ्ट DP फोटो लिंक (URL)") },
          placeholder = { Text("https://... (.jpg / .png / pin.it)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              thumbPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier
              .weight(1f)
              .height(48.dp),
          ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (newThumbUri != null) "नई DP फोटो चयनित ✓" else "या गैलरी से DP बदलें",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          val dpPreviewModel: Any? = newThumbUri ?: dpLinkUrl.trim().takeIf {
            !removeExistingThumb && (it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true))
          }
          if (dpPreviewModel != null) {
            AsyncImage(
              model = dpPreviewModel,
              contentDescription = "गिफ्ट DP प्रीव्यू",
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.5.dp, SwarGold, CircleShape),
              contentScale = ContentScale.Crop,
            )
          }
        }
        if (existing.thumbnailUrl.isNotBlank() && !removeExistingThumb && newThumbUri == null) {
          TextButton(
            onClick = {
              dpLinkUrl = ""
              removeExistingThumb = true
            },
          ) {
            Text("मौजूदा DP फोटो हटाएं", color = SwarCoral)
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it.take(40) },
          label = { Text("गिफ्ट का नाम") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = emoji,
            onValueChange = { emoji = it.take(8) },
            label = { Text("आइकन") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = coinCostText,
            onValueChange = { coinCostText = it.filter { ch -> ch.isDigit() }.take(6) },
            label = { Text("कॉइंस कीमत") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1.4f),
          )
        }
        Text("गिफ्ट श्रेणी चुनें", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SwarCatalogDefaults.giftCategories.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat) },
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = { showDeleteConfirm = true },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFB71C1C),
            contentColor = Color.White,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("delete_custom_gift_button"),
        ) {
          Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("गिफ्ट और DP डिलीट करें")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedCost = coinCostText.toLongOrNull()?.coerceIn(1L, 999999L) ?: existing.coinCost
          onUpdate(
            name.ifBlank { "स्पेशल गिफ्ट" },
            category,
            emoji.ifBlank { "🎁" },
            parsedCost,
            newThumbUri,
            dpLinkUrl.trim(),
            videoLinkUrl.trim(),
            removeExistingThumb,
            removeExistingVideo,
          )
        },
        enabled = !isBusy,
      ) {
        Text(if (isBusy) "प्रक्रिया जारी..." else "अपडेट करें")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("रद्द करें") }
    },
  )

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("गिफ्ट डिलीट करें?") },
      text = { Text("क्या आप '${existing.name}' को गिफ्ट पैनल और स्टोरेज (DP व वीडियो) से पूरी तरह डिलीट करना चाहते हैं?") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDelete()
          },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), contentColor = Color.White),
          modifier = Modifier.testTag("confirm_delete_custom_gift_button"),
        ) {
          Text("हाँ, डिलीट करें")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("नहीं") }
      },
    )
  }
}
