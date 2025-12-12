package org.hwu.care.healthub.core

import kotlinx.coroutines.test.runTest
import org.hwu.care.healthub.data.PiApi
import org.hwu.care.healthub.data.models.Reading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsmIntegrationTest {

    // Mock Dependencies
    private val mockTemi = object : TemiController {
        var lastSpeech = ""
        var lastLocation = ""
        override fun speak(text: String) { lastSpeech = text }
        override fun navigateTo(location: String) { lastLocation = location }
        override fun showInstructions(deviceId: String) {}
        override fun showReading(reading: Reading) {}
    }

    private val mockPiApi = object : PiApi {
        override suspend fun getLatestReading(deviceId: String): Reading? {
            return Reading(deviceId, "98", "%", System.currentTimeMillis())
        }
        override suspend fun setFocus(deviceId: String) {}
    }

    private val sessionManager = SessionManager()

    @Test
    fun `test full session flow`() {
        runTest {
            val fsm = StateMachine(mockTemi, mockPiApi, sessionManager)

            // 1. Initial State
            assertTrue(fsm.state.value is State.Idle)

            // 2. Start Event -> Welcome
            fsm.handleEvent(Event.Start)
            assertTrue(fsm.state.value is State.Welcome)
            assertEquals("Welcome to HealthHub. Shall we start?", mockTemi.lastSpeech)

            // 3. User Confirm -> Navigate
            fsm.handleEvent(Event.UserConfirm)
            assertTrue(fsm.state.value is State.NavigateToDevice)
            assertEquals("oximeter", (fsm.state.value as State.NavigateToDevice).deviceId)

            // 4. Device Arrived -> Show Instructions
            fsm.handleEvent(Event.DeviceArrived)
            assertTrue(fsm.state.value is State.ShowInstructions)

            // 5. User Ready -> Await Use
            fsm.handleEvent(Event.UserConfirm)
            assertTrue(fsm.state.value is State.AwaitUse)

            // 6. Reading Ready -> Show Reading
            fsm.handleEvent(Event.ReadingReady)
            assertTrue(fsm.state.value is State.ShowReading)
            val readingState = fsm.state.value as State.ShowReading
            assertEquals("98", readingState.reading.value)

            // 7. Confirm -> ConfirmSession (End of flow for now)
            fsm.handleEvent(Event.UserConfirm)
            assertTrue(fsm.state.value is State.ConfirmSession)
            
            println("Test Passed: Full flow verified successfully!")
        }
    }
}
