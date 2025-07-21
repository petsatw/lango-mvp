package com.example.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class LearningItem(
    val id: String,
    val token: String,
    @SerialName("presentation_count") var presentationCount: Int = 0,
    @SerialName("usage_count") var usageCount: Int = 0,
    @SerialName("is_learned") var isLearned: Boolean = false
)