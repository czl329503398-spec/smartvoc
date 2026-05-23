package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun lookUpWord(word: String): GeminiWordData? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is missing or default")
            return@withContext null
        }

        val prompt = """
            Provide the English dictionary definition, an illustrative example sentence, and a difficulty level ("Beginner", "Intermediate", or "Advanced") for the word: "$word".
            
            You MUST return ONLY a JSON object with these exact keys:
            - definition (string, 1 clear elegant dictionary definition)
            - example (string, 1 realistic and illustrative example sentence containing this word)
            - difficulty (string, either "Beginner", "Intermediate", or "Advanced")
            
            Do not wrap the response in markdown code blocks. Just return raw JSON.
        """.trimIndent()

        val jsonPayload = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": ${escapeJsonString(prompt)}
                    }
                  ]
                }
              ],
              "generationConfig": {
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()

        val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed: ${response.code} ${response.message}")
                    return@withContext null
                }

                val bodyString = response.body?.string() ?: return@withContext null
                
                val parentAdapter = moshi.adapter(Map::class.java)
                val responseMap = parentAdapter.fromJson(bodyString) ?: return@withContext null
                
                val candidates = responseMap["candidates"] as? List<*> ?: return@withContext null
                if (candidates.isEmpty()) return@withContext null
                
                val firstCandidate = candidates[0] as? Map<*, *> ?: return@withContext null
                val content = firstCandidate["content"] as? Map<*, *> ?: return@withContext null
                val parts = content["parts"] as? List<*> ?: return@withContext null
                if (parts.isEmpty()) return@withContext null
                
                val firstPart = parts[0] as? Map<*, *> ?: return@withContext null
                val textResponse = firstPart["text"] as? String ?: return@withContext null
                
                val wordDataAdapter = moshi.adapter(GeminiWordData::class.java)
                return@withContext wordDataAdapter.fromJson(textResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini lookup", e)
            return@withContext null
        }
    }

    private fun escapeJsonString(string: String): String {
        return moshi.adapter(String::class.java).toJson(string)
    }
}

data class GeminiWordData(
    val definition: String = "",
    val example: String = "",
    val difficulty: String = "Intermediate"
)
