package com.github.livingwithhippos.unchained.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * Response of `GET user/me`.
 *
 * @property plan 0=Free, 1=Essential, 2=Pro, 3=Standard
 * @property premiumExpiresAt jsonDate, null if never had/currently has no active plan
 */
@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class User(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "email") val email: String,
    @param:Json(name = "plan") val plan: Int,
    @param:Json(name = "is_subscribed") val isSubscribed: Boolean,
    @param:Json(name = "premium_expires_at") val premiumExpiresAt: String?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "cooldown_until") val cooldownUntil: String?,
    @param:Json(name = "total_bytes_downloaded") val totalBytesDownloaded: Long?,
    @param:Json(name = "torrents_downloaded") val torrentsDownloaded: Int?,
    @param:Json(name = "user_referral") val userReferral: String?,
) : Parcelable

object UserPlan {
    const val FREE = 0
    const val ESSENTIAL = 1
    const val PRO = 2
    const val STANDARD = 3
}
