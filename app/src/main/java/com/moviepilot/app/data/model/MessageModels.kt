package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Message notification models
 */
data class Message(
    @SerializedName("id") val id: Int,
    @SerializedName("channel") val channel: String,
    @SerializedName("msg_type") val msgType: String,
    @SerializedName("title") val title: String,
    @SerializedName("text") val text: String,
    @SerializedName("image") val image: String?,
    @SerializedName("userid") val userId: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("action") val action: Map<String, Any>?,
    @SerializedName("send_time") val sendTime: String?
)

data class MessageConfig(
    @SerializedName("channels") val channels: List<String>,
    @SerializedName("switchs") val switchs: Map<String, Boolean>?
)

data class NotificationSwitch(
    @SerializedName("name") val name: String,
    @SerializedName("enabled") val enabled: Boolean
)
