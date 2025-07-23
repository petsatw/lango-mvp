package com.example.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class PromptTemplateLoader(private val assetManager: AssetManager) : com.example.domain.PromptTemplateLoader {

    private val cache = ConcurrentHashMap<String, Map<String, String>>()
    private val cacheTimestamp = ConcurrentHashMap<String, Long>()
    private val CACHE_DURATION_MILLIS = 5 * 60 * 1000 // 5 minutes

    override suspend fun loadPromptTemplate(assetPath: String): Map<String, String> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        if (cache.containsKey(assetPath) && (currentTime - (cacheTimestamp[assetPath] ?: 0L) < CACHE_DURATION_MILLIS)) {
            return@withContext cache[assetPath]!!
        }

        val templateMap = mutableMapOf<String, String>()
        val inputStream = assetManager.open(assetPath)
        val reader = BufferedReader(InputStreamReader(inputStream))

        var currentSection: String? = null
        val sectionContent = StringBuilder()
        val sectionPattern = Pattern.compile("^##\\s*([A-Z]+)\\s*$")

        reader.forEachLine { line ->
            val matcher = sectionPattern.matcher(line)
            if (matcher.matches()) {
                if (currentSection != null) {
                    templateMap[currentSection!!] = sectionContent.toString().trim()
                    sectionContent.clear()
                }
                currentSection = matcher.group(1)
            }
            else {
                sectionContent.append(line).append("\n")
            }
        }
        if (currentSection != null) {
            val sectionToMap = currentSection // Local copy to enable smart cast
            templateMap[sectionToMap!!] = sectionContent.toString().trim()
            sectionContent.clear()
        }

        cache[assetPath] = templateMap
        cacheTimestamp[assetPath] = currentTime
        return@withContext templateMap
    }
}