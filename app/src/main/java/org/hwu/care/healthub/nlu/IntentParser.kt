package org.hwu.care.healthub.nlu

/**
 * Intent Parser - Maps user speech to FSM events
 * 
 * This provides fast, offline interpretation of common commands.
 * For complex inputs, it returns Intent.Unknown which triggers LLM processing.
 */
class IntentParser {
    
    companion object {
        // Pattern lists for different intents
        private val confirmPatterns = listOf(
            "yes", "yeah", "yep", "yup", "sure", "okay", "ok", "alright",
            "done", "finished", "ready", "go ahead", "proceed", "continue",
            "correct", "right", "affirmative", "absolutely"
        )
        
        private val cancelPatterns = listOf(
            "no", "nope", "nah", "stop", "cancel", "quit", "exit",
            "abort", "end", "finish", "close", "never mind"
        )
        
        private val retryPatterns = listOf(
            "again", "retry", "repeat", "one more time", "try again",
            "redo", "restart", "back", "previous"
        )
        
        private val helpPatterns = listOf(
            "help", "what", "how", "explain", "instructions",
            "i don't understand", "confused", "what do i do",
            "what should i do", "tell me more"
        )
        
        private val skipPatterns = listOf(
            "skip", "pass", "next", "move on", "later",
            "not now", "maybe later"
        )
    }
    
    /**
     * Parse user input text into an Intent
     * @param text User's speech or text input
     * @return Parsed Intent
     */
    fun parse(text: String): Intent {
        val normalized = text.lowercase().trim()
        
        // Check for empty input
        if (normalized.isBlank()) {
            return Intent.Unknown(text)
        }
        
        // Match against patterns
        return when {
            matchesAny(normalized, confirmPatterns) -> Intent.Confirm
            matchesAny(normalized, cancelPatterns) -> Intent.Cancel
            matchesAny(normalized, retryPatterns) -> Intent.Retry
            matchesAny(normalized, helpPatterns) -> Intent.Help
            matchesAny(normalized, skipPatterns) -> Intent.Skip
            else -> Intent.Unknown(text)
        }
    }
    
    /**
     * Check if text matches any pattern in the list
     */
    private fun matchesAny(text: String, patterns: List<String>): Boolean {
        return patterns.any { pattern ->
            // Exact match or contains as whole word
            text == pattern || text.contains(" $pattern ") || 
            text.startsWith("$pattern ") || text.endsWith(" $pattern")
        }
    }
    
    /**
     * Parse questionnaire answer (more specific than general intent)
     */
    fun parseAnswer(text: String): Answer {
        val normalized = text.lowercase().trim()
        
        return when {
            // Yes answers
            normalized in listOf("yes", "yeah", "yep", "yup", "sure", "okay", "ok") -> 
                Answer.Yes
            
            // No answers
            normalized in listOf("no", "nope", "nah", "not really", "negative") -> 
                Answer.No
            
            // Uncertain
            normalized in listOf("maybe", "sometimes", "occasionally", "i don't know", 
                                "not sure", "unsure") -> 
                Answer.Uncertain
            
            // Skip
            normalized in listOf("skip", "pass", "prefer not to say") -> 
                Answer.Skip
            
            // Complex answer - send to LLM
            else -> Answer.Complex(text)
        }
    }
}

/**
 * Intent types that map to FSM events
 */
sealed class Intent {
    object Confirm : Intent()
    object Cancel : Intent()
    object Retry : Intent()
    object Help : Intent()
    object Skip : Intent()
    data class Unknown(val text: String) : Intent()
}

/**
 * Questionnaire answer types
 */
sealed class Answer {
    object Yes : Answer()
    object No : Answer()
    object Uncertain : Answer()
    object Skip : Answer()
    data class Complex(val text: String) : Answer()
}
