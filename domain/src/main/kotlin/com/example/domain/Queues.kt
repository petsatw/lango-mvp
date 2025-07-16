package com.example.domain

data class Queues(
    val newQueue: MutableList<LearningItem>,
    val learnedPool: MutableList<LearningItem>
)
