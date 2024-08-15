package com.example.kotlin_fundamentals

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AIFragment : Fragment() {

    private val client = OkHttpClient()
    lateinit var txtResponse: TextView
    lateinit var idTVQuestion: TextView
    lateinit var etQuestion: TextInputEditText

    // List of keywords related to food
    private val foodKeywords = listOf("food", "recipe", "meal", "cook", "ingredients", "dish", "cuisine", "restaurant", "eat", "diet")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai, container, false)
        txtResponse = view.findViewById(R.id.txtResponse)
        idTVQuestion = view.findViewById(R.id.idTVQuestion)
        etQuestion = view.findViewById(R.id.etQuestion)

        etQuestion.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val question = etQuestion.text.toString().trim()
                if (question.isNotEmpty()) {
                    if (isFoodRelated(question)) {
                        txtResponse.text = "Please wait..."
                        getResponse(question) { response ->
                            activity?.runOnUiThread { txtResponse.text = response }
                        }
                    } else {
                        activity?.runOnUiThread {
                            txtResponse.text = "This assistant only responds to food-related queries. Please ask something related to food."
                        }
                    }
                }
                true
            } else false
        }

        return view
    }

    private fun isFoodRelated(question: String): Boolean {
        return foodKeywords.any { keyword -> question.contains(keyword, ignoreCase = true) }
    }

    private fun getResponse(question: String, callback: (String) -> Unit) {
        idTVQuestion.text = question
        etQuestion.setText("")

        val requestBody = """
            {
                "model": "gpt-3.5-turbo",
                "messages": [
                    {"role": "user", "content": "$question"}
                ],
                "max_tokens": 500,
                "temperature": 0.7
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer sk-3VTd5gXJepAQK07N6a-rQX9gz7Nudkt8nxgfe7bCdYT3BlbkFJPvkHI0XPgUaThl36nc025xjUxeIay-HK8cUMV3aOoA")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    txtResponse.text = "Failed to connect: ${e.message}"
                }
                Log.e("API_ERROR", "Failed to connect", e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use { resp ->
                        val body = resp.body?.string()
                        Log.d("API_RESPONSE", "Response code: ${resp.code}, body: $body")
                        if (resp.isSuccessful && body != null) {
                            try {
                                val jsonObject = JSONObject(body)
                                val jsonArray = jsonObject.getJSONArray("choices")
                                val textResult = jsonArray.getJSONObject(0).getJSONObject("message").getString("content")
                                activity?.runOnUiThread {
                                    txtResponse.text = textResult
                                }
                            } catch (e: Exception) {
                                activity?.runOnUiThread {
                                    txtResponse.text = "Error parsing response: ${e.localizedMessage}"
                                }
                                Log.e("API_ERROR", "Response parsing failed", e)
                            }
                        } else {
                            activity?.runOnUiThread {
                                txtResponse.text = "Error: ${resp.message} - ${body ?: "No response body"}"
                            }
                            Log.e("API_ERROR", "Server responded with error: ${resp.message} - ${body ?: "No response body"}")
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        txtResponse.text = "An unexpected error occurred: ${e.localizedMessage}"
                    }
                    Log.e("API_ERROR", "Unexpected error", e)
                }
            }
        })
    }
}
