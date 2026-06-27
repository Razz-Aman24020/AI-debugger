package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BugFixRepository
import com.example.data.BugFixSession
import com.example.network.BugFixResponse
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BugFixViewModel(private val repository: BugFixRepository) : ViewModel() {

    // Input States
    private val _buggyCode = MutableStateFlow("")
    val buggyCode = _buggyCode.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage = _errorMessage.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("Auto-detect")
    val selectedLanguage = _selectedLanguage.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-3.1-pro-preview") // Default to higher quality for coding tasks
    val selectedModel = _selectedModel.asStateFlow()

    // Processing & Error states
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError = _apiError.asStateFlow()

    // Selected / Active session for detail viewing
    private val _selectedSession = MutableStateFlow<BugFixSession?>(null)
    val selectedSession = _selectedSession.asStateFlow()

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filter History by language
    private val _filterLanguage = MutableStateFlow("All")
    val filterLanguage = _filterLanguage.asStateFlow()

    // History sessions
    val savedSessions: StateFlow<List<BugFixSession>> = combine(
        repository.allSessions,
        _searchQuery,
        _filterLanguage
    ) { sessions, query, lang ->
        sessions.filter { session ->
            val matchesSearch = query.isEmpty() ||
                    session.title.contains(query, ignoreCase = true) ||
                    session.buggyCode.contains(query, ignoreCase = true) ||
                    session.explanation.contains(query, ignoreCase = true)
            val matchesLang = lang == "All" || session.language.equals(lang, ignoreCase = true)
            matchesSearch && matchesLang
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateBuggyCode(code: String) {
        _buggyCode.value = code
    }

    fun updateErrorMessage(error: String) {
        _errorMessage.value = error
    }

    fun updateSelectedLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun updateSelectedModel(model: String) {
        _selectedModel.value = model
    }

    fun selectSession(session: BugFixSession?) {
        _selectedSession.value = session
        if (session != null) {
            // Load code inputs so the user can easily re-run or edit
            _buggyCode.value = session.buggyCode
            _errorMessage.value = session.errorMessage
            _selectedLanguage.value = session.language
        }
    }

    fun clearInputs() {
        _buggyCode.value = ""
        _errorMessage.value = ""
        _selectedSession.value = null
        _apiError.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterLanguage(lang: String) {
        _filterLanguage.value = lang
    }

    fun fixBug(apiKey: String) {
        val codeToFix = _buggyCode.value.trim()
        if (codeToFix.isEmpty()) {
            _apiError.value = "Please enter some code to fix."
            return
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            _apiError.value = "Gemini API Key is missing. Please configure your API key in the AI Studio Secrets panel."
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _apiError.value = null
            _selectedSession.value = null

            val language = _selectedLanguage.value
            val contextError = _errorMessage.value.trim()

            // Constructing Prompt
            val prompt = """
                Analyze and fix the following programming code.
                
                Language specified: $language
                
                Buggy Code:
                ```
                $codeToFix
                ```
                
                ${if (contextError.isNotEmpty()) "Context/Error Messages received:\n$contextError" else "No error messages provided."}
                
                You MUST respond STRICTLY in JSON format with exactly the following JSON structure:
                {
                  "title": "A short, descriptive title describing the bug found (e.g. 'Fixing Python Index Error')",
                  "fixedCode": "The complete, fully corrected code",
                  "explanation": "A detailed, structured explanation in markdown of the bug, why it happened, and how you fixed it."
                }
                
                Ensure the JSON keys are exactly as named above. Provide valid, properly escaped JSON. No surrounding markdown markers of other formats besides the JSON structure. Do not forget to escape quotes inside your code block properly.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.2,
                    responseMimeType = "application/json"
                ),
                systemInstruction = Content(
                    parts = listOf(
                        Part(
                            text = "You are an expert full-stack developer and AI code repair engine. You analyze, refactor, and fix code bugs across all programming languages, and output perfectly structured JSON."
                        )
                    )
                )
            )

            try {
                val response = RetrofitClient.service.generateContent(
                    model = _selectedModel.value,
                    apiKey = apiKey,
                    request = request
                )

                val rawResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawResult != null) {
                    try {
                        val parsed = parseGeminiResponse(rawResult)
                        
                        // Automatically save to local history database
                        val session = BugFixSession(
                            title = parsed.title,
                            language = if (language == "Auto-detect") detectLanguage(codeToFix) else language,
                            buggyCode = codeToFix,
                            errorMessage = contextError,
                            fixedCode = parsed.fixedCode,
                            explanation = parsed.explanation
                        )
                        
                        val newId = repository.insert(session)
                        _selectedSession.value = session.copy(id = newId.toInt())
                    } catch (parseEx: Exception) {
                        // Fallback parsing strategy: extract markdown blocks in case JSON is wrapped
                        val extractedResponse = attemptManualExtraction(rawResult, codeToFix, language, contextError)
                        val newId = repository.insert(extractedResponse)
                        _selectedSession.value = extractedResponse.copy(id = newId.toInt())
                    }
                } else {
                    _apiError.value = "No response received from the AI model. Please try again."
                }
            } catch (e: Exception) {
                _apiError.value = "Network/API Error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun parseGeminiResponse(rawText: String): BugFixResponse {
        var cleanText = rawText.trim()
        if (cleanText.startsWith("```")) {
            cleanText = cleanText.removePrefix("```json").removePrefix("```")
            if (cleanText.endsWith("```")) {
                cleanText = cleanText.removeSuffix("```")
            }
            cleanText = cleanText.trim()
        }
        val adapter = RetrofitClient.moshiParser.adapter(BugFixResponse::class.java)
        return adapter.fromJson(cleanText) ?: throw Exception("Failed to parse JSON")
    }

    private fun attemptManualExtraction(rawText: String, originalCode: String, language: String, contextError: String): BugFixSession {
        // Attempt to extract triple-backtick block for code
        val codeBlockRegex = """```[\w-]*\n([\s\S]*?)\n```""".toRegex()
        val matchResult = codeBlockRegex.find(rawText)
        val extractedCode = matchResult?.groups?.get(1)?.value ?: originalCode
        
        // Clean up explanation
        val explanationCleaned = rawText
            .replace(codeBlockRegex, "") // remove the code block from the explanation
            .trim()
            
        val detectedLang = if (language == "Auto-detect") detectLanguage(originalCode) else language

        return BugFixSession(
            title = "Fixed Code ($detectedLang)",
            language = detectedLang,
            buggyCode = originalCode,
            errorMessage = contextError,
            fixedCode = extractedCode,
            explanation = explanationCleaned.ifEmpty { "The AI corrected the bug. Review the fixed code panel." }
        )
    }

    private fun detectLanguage(code: String): String {
        val trimmed = code.trim()
        return when {
            trimmed.contains("def ") && trimmed.contains(":") -> "Python"
            trimmed.contains("import ") && (trimmed.contains("public class") || trimmed.contains("System.out")) -> "Java"
            trimmed.contains("package ") || trimmed.contains("fun ") || trimmed.contains("val ") -> "Kotlin"
            trimmed.contains("const ") || trimmed.contains("let ") || trimmed.contains("function ") || trimmed.contains("console.log") -> "JavaScript"
            trimmed.contains("#include") || trimmed.contains("std::") -> "C++"
            trimmed.contains("fn main()") || trimmed.contains("let mut") -> "Rust"
            trimmed.contains("import React") || trimmed.contains("interface ") -> "TypeScript"
            trimmed.contains("fmt.Println") || trimmed.contains("package main") -> "Go"
            trimmed.contains("<?php") -> "PHP"
            trimmed.contains("<html") || trimmed.contains("</div>") -> "HTML/CSS"
            trimmed.contains("SELECT ") && trimmed.contains(" FROM ") -> "SQL"
            else -> "Other"
        }
    }

    fun deleteSession(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_selectedSession.value?.id == id) {
                _selectedSession.value = null
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _selectedSession.value = null
        }
    }

    // Factory companion object
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BugFixViewModel::class.java)) {
                val db = AppDatabase.getDatabase(context)
                val repo = BugFixRepository(db.bugFixDao())
                @Suppress("UNCHECKED_CAST")
                return BugFixViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
