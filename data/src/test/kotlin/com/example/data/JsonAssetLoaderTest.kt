package com.example.data

import com.example.domain.LearningItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStreamReader

class JsonAssetLoaderTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun loadLearnedQueueFromJson_returnsParsedEntities() {
        // Arrange: Read JSON file
        val inputStream = checkNotNull(javaClass.classLoader?.getResourceAsStream("learned_queue.json")) {
            fail("JSON file not found")
        }
        val jsonContent = InputStreamReader(inputStream).use { it.readText() }

        val items: List<LearningItem> = json.decodeFromString(jsonContent)
        assertTrue("JSON array should not be empty", items.isNotEmpty())
        assertEquals(70, items.size)

        val firstItem = items.first()

        // Assert: Verify fields (adjust based on actual JSON)
        assertEquals("german_AA002", firstItem.id)
        assertEquals("sehr", firstItem.token)
        assertEquals("Adjectives/Adverbs", firstItem.category)
        assertEquals("adverb", firstItem.subcategory)
    }

    @Test
    fun loadCoreBlocksFromJson_returnsParsedEntities() {
        // Arrange: Read JSON file
        val inputStream = checkNotNull(javaClass.classLoader?.getResourceAsStream("core_blocks.json")) {
            fail("JSON file not found")
        }
        val jsonContent = InputStreamReader(inputStream).use { it.readText() }

        val items: List<LearningItem> = json.decodeFromString(jsonContent)
        assertTrue("JSON array should not be empty", items.isNotEmpty())
        assertEquals(3, items.size)

        val firstItem = items.first()

        // Assert: Verify fields (adjust based on actual JSON)
        assertEquals("german_CP001", firstItem.id)
        assertEquals("Entschuldigung", firstItem.token)
        assertEquals("Blocks", firstItem.category)
        assertEquals("fixed", firstItem.subcategory)
    }
}