package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Workflow models
 */
data class Workflow(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("event_type") val eventType: String?,
    @SerializedName("actions") val actions: List<Action>,
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class Action(
    @SerializedName("action_id") val actionId: String,
    @SerializedName("action_name") val actionName: String,
    @SerializedName("params") val params: Map<String, Any>?
)

data class EventType(
    @SerializedName("event_type") val eventType: String,
    @SerializedName("event_name") val eventName: String,
    @SerializedName("event_description") val eventDescription: String?
)

data class WorkflowRunResult(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("result") val result: Any?
)
