import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStreamReader
import com.example.domain.LearningItem

class JsonAssetLoaderTest {

    @Test
    fun loadFirstLearningItemFromJson_printsContentsAndReturnsItem() {
        // Arrange: Read JSON file
        val inputStream = checkNotNull(javaClass.classLoader?.getResourceAsStream("core_blocks.json")) {
            fail("JSON file not found")
        }
        val jsonContent = InputStreamReader(inputStream).use { it.readText() }

        val gson = Gson()
        val listType = object : TypeToken<List<LearningItem>>() {}.type
        val items: List<LearningItem> = gson.fromJson(jsonContent, listType)
        assertTrue("JSON array should not be empty", items.isNotEmpty())

        val firstItem = items.first()

        // Assert: Verify fields (adjust based on actual JSON)
        assertEquals("german_CP001", firstItem.id)
        assertEquals("Entschuldigung", firstItem.token)
        assertEquals("Blocks", firstItem.category)
        assertEquals("fixed", firstItem.subcategory)
    }
}