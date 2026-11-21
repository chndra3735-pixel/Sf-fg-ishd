package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val handle: String = "",
  val avatarUrl: String = "",
  val bio: String = "",
  val gender: String = "male",
  val clubTag: String = "SWAR",
  val coins: Long = 1000L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class VoiceRoom(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val ownerName: String = "",
  val ownerAvatarUrl: String = "",
  val title: String = "",
  val roomCode: String = "",
  val category: String = "लोकप्रिय",
  val coverUrl: String = "",
  val backgroundTheme: String = "stadium",
  val clubTag: String = "SWAR",
  val welcomeNotice: String = "SwarRoom में आपका स्वागत है। कृपया एक दूसरे का सम्मान करें और सभ्य तरीके से चैट करें।",
  val seatCount: Long = 20L,
  val listenerCount: Long = 1L,
  @get:PropertyName("isLive") @field:PropertyName("isLive") val isLive: Boolean = true,
  @get:PropertyName("isPublic") @field:PropertyName("isPublic") val isPublic: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RoomSeat(
  @DocumentId val id: String = "",
  val roomId: String = "",
  val roomOwnerId: String = "",
  @get:PropertyName("isRoomPublic") @field:PropertyName("isRoomPublic") val isRoomPublic: Boolean = true,
  val seatIndex: Long = 0L,
  val occupantUid: String = "",
  val occupantName: String = "",
  val occupantAvatarUrl: String = "",
  @get:PropertyName("isLocked") @field:PropertyName("isLocked") val isLocked: Boolean = false,
  @get:PropertyName("isMuted") @field:PropertyName("isMuted") val isMuted: Boolean = false,
  @get:PropertyName("isSpeaking") @field:PropertyName("isSpeaking") val isSpeaking: Boolean = false,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RoomMessage(
  @DocumentId val id: String = "",
  val roomId: String = "",
  val roomOwnerId: String = "",
  @get:PropertyName("isRoomPublic") @field:PropertyName("isRoomPublic") val isRoomPublic: Boolean = true,
  val senderUid: String = "",
  val senderName: String = "",
  val senderAvatarUrl: String = "",
  val text: String = "",
  val messageType: String = "text",
  val badgeTag: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Conversation(
  @DocumentId val id: String = "",
  val participantUids: List<String> = emptyList(),
  val participantNames: Map<String, String> = emptyMap(),
  val participantAvatars: Map<String, String> = emptyMap(),
  val lastMessage: String = "",
  val lastSenderUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DirectMessage(
  @DocumentId val id: String = "",
  val conversationId: String = "",
  val participantUids: List<String> = emptyList(),
  val senderUid: String = "",
  val senderName: String = "",
  val text: String = "",
  val mediaUrl: String = "",
  val mediaType: String = "text",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MomentPost(
  @DocumentId val id: String = "",
  val authorUid: String = "",
  val authorName: String = "",
  val authorAvatarUrl: String = "",
  val caption: String = "",
  val mediaUrl: String = "",
  val mediaType: String = "image",
  @get:PropertyName("isPublic") @field:PropertyName("isPublic") val isPublic: Boolean = true,
  val likesCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class EmojiCatalogItem(
  @DocumentId val id: String = "",
  val createdByUid: String = "",
  val label: String = "",
  val category: String = "ट्रेंडिंग",
  val symbol: String = "",
  val imageUrl: String = "",
  @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class GiftCatalogItem(
  @DocumentId val id: String = "",
  val createdByUid: String = "",
  val name: String = "",
  val category: String = "हॉट",
  val emoji: String = "🎁",
  val thumbnailUrl: String = "",
  val mediaUrl: String = "",
  val mediaType: String = "emoji",
  val coinCost: Long = 10L,
  @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@androidx.annotation.Keep
@IgnoreExtraProperties
data class GiftEvent(
  @DocumentId val id: String = "",
  val roomId: String = "",
  val roomTitle: String = "",
  val senderUid: String = "",
  val senderName: String = "",
  val senderAvatarUrl: String = "",
  val recipientUid: String = "",
  val recipientName: String = "",
  val giftId: String = "",
  val giftName: String = "",
  val giftEmoji: String = "🎁",
  val giftMediaUrl: String = "",
  val giftMediaType: String = "emoji",
  val coinCost: Long = 0L,
  val quantity: Long = 1L,
  @get:PropertyName("isPublic") @field:PropertyName("isPublic") val isPublic: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@androidx.annotation.Keep
@IgnoreExtraProperties
data class LevelTierConfig(
  @DocumentId val id: String = "",
  val badgeLevel: Long = 1L,
  val entryVideoUrl: String = "",
  val avatarFrameUrl: String = "",
  val rewardCoins: Long = 0L,
  val profileBadgeUrl: String = "",
  val updatedByUid: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@androidx.annotation.Keep
@IgnoreExtraProperties
data class LevelRewardClaim(
  @DocumentId val id: String = "",
  val userId: String = "",
  val badgeLevel: Long = 1L,
  val coinsGranted: Long = 0L,
  @ServerTimestamp val claimedAt: Timestamp? = null,
)

@androidx.annotation.Keep
@IgnoreExtraProperties
data class RoomPresenceMember(
  @DocumentId val id: String = "",
  val userId: String = "",
  val roomId: String = "",
  val roomOwnerId: String = "",
  @get:PropertyName("isRoomPublic") @field:PropertyName("isRoomPublic") val isRoomPublic: Boolean = true,
  val displayName: String = "",
  val avatarUrl: String = "",
  val clubTag: String = "",
  @ServerTimestamp val joinedAt: Timestamp? = null,
  @ServerTimestamp val lastSeenAt: Timestamp? = null,
)

@androidx.annotation.Keep
@IgnoreExtraProperties
data class AppUpdateConfig(
  @DocumentId val id: String = "",
  val githubRepoUrl: String = "",
  val updatedByUid: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@androidx.annotation.Keep
@IgnoreExtraProperties
data class UserAuditProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val numericId: String = "",
  val userEmail: String = "",
  val displayName: String = "",
  val avatarUrl: String = "",
  val dobIso: String = "",
  val ageYears: Long = 0L,
  val locationLabel: String = "",
  val locationLat: Double = 0.0,
  val locationLng: Double = 0.0,
  val locationStatus: String = "pending",
  @get:PropertyName("termsAccepted") @field:PropertyName("termsAccepted") val termsAccepted: Boolean = false,
  @get:PropertyName("onboardingCompleted") @field:PropertyName("onboardingCompleted") val onboardingCompleted: Boolean = false,
  @get:PropertyName("isBlockedByAdmin") @field:PropertyName("isBlockedByAdmin") val isBlockedByAdmin: Boolean = false,
  @ServerTimestamp val firstLoginAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@androidx.annotation.Keep
@IgnoreExtraProperties
data class UserActivityLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val numericId: String = "",
  val userEmail: String = "",
  val userName: String = "",
  val userAvatarUrl: String = "",
  val activityType: String = "room_visit",
  val roomId: String = "",
  val roomTitle: String = "",
  val roomCode: String = "",
  val conversationId: String = "",
  val targetUid: String = "",
  val targetNumericId: String = "",
  val targetName: String = "",
  val textContent: String = "",
  val mediaUrl: String = "",
  val dateKey: String = "",
  val enteredAtMs: Long = 0L,
  val exitedAtMs: Long = 0L,
  val durationSeconds: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  val expiresAt: Timestamp? = null,
)

object SwarCatalogDefaults {
  val emojiCategories = listOf("ट्रेंडिंग", "मस्ती", "प्यार", "प्रतिक्रिया")
  val giftCategories = listOf("हॉट", "लग्ज़री", "वीडियो VIP", "लकी")

  val builtInEmojis = listOf(
    EmojiCatalogItem(id = "def_e1", label = "फायर", category = "ट्रेंडिंग", symbol = "🔥"),
    EmojiCatalogItem(id = "def_e2", label = "शानदार", category = "ट्रेंडिंग", symbol = "🤩"),
    EmojiCatalogItem(id = "def_e3", label = "रॉकस्टार", category = "ट्रेंडिंग", symbol = "🤘"),
    EmojiCatalogItem(id = "def_e4", label = "VIP", category = "ट्रेंडिंग", symbol = "💎"),
    EmojiCatalogItem(id = "def_e5", label = "हंसी", category = "मस्ती", symbol = "😂"),
    EmojiCatalogItem(id = "def_e6", label = "डांस", category = "मस्ती", symbol = "💃"),
    EmojiCatalogItem(id = "def_e7", label = "पार्टी", category = "मस्ती", symbol = "🥳"),
    EmojiCatalogItem(id = "def_e8", label = "गाना", category = "मस्ती", symbol = "🎵"),
    EmojiCatalogItem(id = "def_e9", label = "दिल से", category = "प्यार", symbol = "❤️"),
    EmojiCatalogItem(id = "def_e10", label = "गुलाब", category = "प्यार", symbol = "🌹"),
    EmojiCatalogItem(id = "def_e11", label = "प्यारा", category = "प्यार", symbol = "🥰"),
    EmojiCatalogItem(id = "def_e12", label = "चमक", category = "प्यार", symbol = "💖"),
    EmojiCatalogItem(id = "def_e13", label = "तालियां", category = "प्रतिक्रिया", symbol = "👏"),
    EmojiCatalogItem(id = "def_e14", label = "नमस्ते", category = "प्रतिक्रिया", symbol = "🙏"),
    EmojiCatalogItem(id = "def_e15", label = "सलाम", category = "प्रतिक्रिया", symbol = "🫡"),
    EmojiCatalogItem(id = "def_e16", label = "वाह", category = "प्रतिक्रिया", symbol = "😮"),
  )

  val builtInGifts: List<GiftCatalogItem> = emptyList()
}

