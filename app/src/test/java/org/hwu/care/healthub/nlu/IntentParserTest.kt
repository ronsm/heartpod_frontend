package org.hwu.care.healthub.nlu

import org.hwu.care.healthub.nlu.Answer
import org.hwu.care.healthub.nlu.Intent
import org.hwu.care.healthub.nlu.IntentParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IntentParserTest {
    
    private lateinit var parser: IntentParser
    
    @Before
    fun setup() {
        parser = IntentParser()
    }
    
    @Test
    fun `test confirm intent recognition`() {
        val confirmInputs = listOf(
            "yes",
            "yeah",
            "okay",
            "I'm done",
            "ready to go",
            "go ahead"
        )
        
        confirmInputs.forEach { input ->
            val intent = parser.parse(input)
            assertTrue("Failed for: $input", intent is Intent.Confirm)
        }
    }
    
    @Test
    fun `test cancel intent recognition`() {
        val cancelInputs = listOf(
            "no",
            "stop",
            "cancel",
            "quit",
            "never mind"
        )
        
        cancelInputs.forEach { input ->
            val intent = parser.parse(input)
            assertTrue("Failed for: $input", intent is Intent.Cancel)
        }
    }
    
    @Test
    fun `test retry intent recognition`() {
        val retryInputs = listOf(
            "again",
            "retry",
            "one more time",
            "try again"
        )
        
        retryInputs.forEach { input ->
            val intent = parser.parse(input)
            assertTrue("Failed for: $input", intent is Intent.Retry)
        }
    }
    
    @Test
    fun `test help intent recognition`() {
        val helpInputs = listOf(
            "help",
            "what do I do",
            "I don't understand",
            "explain"
        )
        
        helpInputs.forEach { input ->
            val intent = parser.parse(input)
            assertTrue("Failed for: $input", intent is Intent.Help)
        }
    }
    
    @Test
    fun `test unknown intent for complex input`() {
        val complexInputs = listOf(
            "I feel a bit dizzy",
            "Can we do blood pressure first",
            "I already measured my weight today"
        )
        
        complexInputs.forEach { input ->
            val intent = parser.parse(input)
            assertTrue("Failed for: $input", intent is Intent.Unknown)
            assertEquals(input, (intent as Intent.Unknown).text)
        }
    }
    
    @Test
    fun `test questionnaire answer parsing`() {
        assertEquals(Answer.Yes, parser.parseAnswer("yes"))
        assertEquals(Answer.No, parser.parseAnswer("no"))
        assertEquals(Answer.Uncertain, parser.parseAnswer("maybe"))
        assertEquals(Answer.Skip, parser.parseAnswer("skip"))
        
        val complexAnswer = parser.parseAnswer("only when I exercise")
        assertTrue(complexAnswer is Answer.Complex)
    }
    
    @Test
    fun `test case insensitivity`() {
        assertEquals(Intent.Confirm::class, parser.parse("YES").javaClass.kotlin)
        assertEquals(Intent.Confirm::class, parser.parse("YeS").javaClass.kotlin)
        assertEquals(Intent.Cancel::class, parser.parse("STOP").javaClass.kotlin)
    }
    
    @Test
    fun `test whitespace handling`() {
        assertEquals(Intent.Confirm::class, parser.parse("  yes  ").javaClass.kotlin)
        assertEquals(Intent.Cancel::class, parser.parse("stop   ").javaClass.kotlin)
    }
}
