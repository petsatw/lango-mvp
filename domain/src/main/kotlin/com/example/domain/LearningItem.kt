package com.example.domain

data class LearningItem(
    val id: String,
    val token: String,
    val category: String? = null,
    val subcategory: String? = null,
    var usageCount: Int = 0,
    var presentationCount: Int = 0,
    var isLearned: Boolean = false
)