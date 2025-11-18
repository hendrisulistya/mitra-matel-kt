// app/src/main/kotlin/app/mitra/matel/network/models/AnnouncementModels.kt
package app.mitra.matel.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementResponse(
    @SerialName("id") val id: Int,
    val title: String,
    val severity: String,
    val type: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    val content: AnnouncementContent
)

@Serializable
data class AnnouncementContent(
    val sections: List<AnnouncementSection>? = null,
    val paragraphs: List<String>? = null
)

@Serializable
data class AnnouncementSection(
    val title: String,
    val entries: List<String>
)

@Serializable
data class AnnouncementLatestResponse(
    val promo: AnnouncementResponse? = null,
    val policy: AnnouncementResponse? = null,
    val update: AnnouncementResponse? = null
)