package com.example.data

import com.example.domain.LearningItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStreamReader

class JsonAssetLoaderTest {

    @Test
    fun loadLearnedQueueFromJson_returnsParsedEntities() {
        // Arrange: Read JSON file
        val inputStream = checkNotNull(javaClass.classLoader?.getResourceAsStream("learned_queue.json")) {
            fail("JSON file not found")
        }
        val jsonContent = InputStreamReader(inputStream).use { it.readText() }

        val gson = Gson()
        val listType = object : TypeToken<List<LearningItem>>() {}.type
        val items: List<LearningItem> = gson.fromJson(jsonContent, listType)
        assertTrue("JSON array should not be empty", items.isNotEmpty())

        val firstItem = items.first()

        // Assert: Verify fields (adjust based on actual JSON)
        assertEquals("german_AA002", firstItem.id)
        assertEquals("sehr", firstItem.token)
        assertEquals("Adjectives/Adverbs", firstItem.category)
        assertEquals("adverb", firstItem.subcategory)
    }
}