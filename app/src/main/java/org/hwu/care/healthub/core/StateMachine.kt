package org.hwu.care.healthub.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.hwu.care.healthub.data.PiApi
import org.hwu.care.healthub.data.models.Reading

sealed class State {
    object Idle : State()
    object Welcome : State()
    data class NavigateToDevice(val deviceId: String) : State()
    data class ShowInstructions(val deviceId: String) : State()
    data class AwaitUse(val deviceId: String) : State()
    data class ReadingCapture(val deviceId: String) : State()
    data class ShowReading(val reading: Reading) : State()
    
    // BP Monitor specific states
    object NavigateToBP : State()
    object ShowBPInstructions : State()
    object AwaitBPUse : State()
    object BPReadingCapture : State()
    data class ShowBPResults(val systolic: Float, val diastolic: Float) : State()
    // Thermometer specific states
    object NavigateToThermometer : State()
    object ShowThermometerInstructions : State()
    object AwaitThermometerUse : State()
    data class ShowThermometerResults(val temperature: Float) : State()
    
    object Questionnaire : State()
    
    // Questionnaire states
    object AskPersonalDetails : State()
    object AskSmokingStatus : State()
    object AskAlcoholConsumption : State()
    object AskExerciseFrequency : State()
    object AskHeightWeight : State()
    object ReviewQuestionnaire : State()
    
    object ConfirmSession : State()
    data class ErrorRecover(val message: String) : State()
    object End : State()
}

sealed class Event {
    object Start : Event()
    object UserConfirm : Event()
    object DeviceArrived : Event()
    object ReadingReady : Event()
    object Timeout : Event()
    object Retry : Event()
    object Abort : Event()
    
    // BP Monitor events
    data class BPDataReceived(val systolic: Float, val diastolic: Float) : Event()
    object BPTimeout : Event()

    // Thermometer events
    data class ThermometerDataReceived(val temperature: Float) : Event()
    
    // Questionnaire events
    data class PersonalDetailsProvided(val firstName: String, val lastName: String, val dob: String) : Event()
    data class SmokingAnswered(val status: String, val perDay: Int? = null, val quitDate: String? = null) : Event()
    data class AlcoholAnswered(val unitsPerWeek: Int) : Event()
    data class ExerciseAnswered(val timesPerWeek: Int) : Event()
    data class HeightWeightProvided(val heightCm: Float, val weightKg: Float) : Event()
    object QuestionnaireComplete : Event()
}

class StateMachine(
    private val temi: TemiController,
    private val piApi: PiApi,
    private val sessionManager: SessionManager
) {
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    suspend fun handleEvent(event: Event) {
        val currentState = _state.value
        
        when (currentState) {
            is State.Idle -> {
                if (event is Event.Start) {
                    val firstDevice = "oximeter" // TODO: Get from config
                    temi.speak("Okay, starting screening. Moving to the $firstDevice station.")
                    temi.navigateTo(firstDevice)
                    _state.value = State.NavigateToDevice(firstDevice)
                }
            }
            is State.Welcome -> {
                if (event is Event.UserConfirm) {
                    val firstDevice = "oximeter" // TODO: Get from config
                    temi.navigateTo(firstDevice)
                    _state.value = State.NavigateToDevice(firstDevice)
                }
            }
            is State.NavigateToDevice -> {
                if (event is Event.DeviceArrived) {
                    temi.showInstructions(currentState.deviceId)
                    _state.value = State.ShowInstructions(currentState.deviceId)
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.ShowInstructions -> {
                if (event is Event.UserConfirm) {
                    // Tell Pi to focus/scan
                    piApi.setFocus(currentState.deviceId)
                    _state.value = State.AwaitUse(currentState.deviceId)
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.AwaitUse -> {
                if (event is Event.ReadingReady) {
                    _state.value = State.ReadingCapture(currentState.deviceId)
                    captureReading(currentState.deviceId)
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.ShowReading -> {
                if (event is Event.UserConfirm) {
                    // After oximeter, go directly to Thermometer instructions (skip navigation)
                    temi.speak("Great! Now let's check your temperature.")
                    _state.value = State.ShowThermometerInstructions
                } else if (event is Event.Retry) {
                    // Retry logic
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.NavigateToBP -> {
                if (event is Event.DeviceArrived) {
                    _state.value = State.ShowBPInstructions
                }
            }
            is State.ShowBPInstructions -> {
                if (event is Event.UserConfirm) {
                    _state.value = State.AwaitBPUse
                }
            }
            is State.AwaitBPUse -> {
                if (event is Event.BPDataReceived) {
                    val bpEvent = event as Event.BPDataReceived
                    _state.value = State.ShowBPResults(bpEvent.systolic, bpEvent.diastolic)
                } else if (event is Event.BPTimeout) {
                    _state.value = State.ErrorRecover("No BP data received. Please try again.")
                }
            }
            is State.ShowBPResults -> {
                if (event is Event.UserConfirm) {
                    // After BP, go to Questionnaire
                    temi.speak("Perfect. Now I have a few questions for you.")
                    _state.value = State.Questionnaire
                }
            }
            is State.ShowThermometerInstructions -> {
                if (event is Event.UserConfirm) {
                    // User clicked "Ready" - use same pattern as oximeter
                    piApi.setFocus("thermometer")
                    _state.value = State.AwaitUse("thermometer")
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.ShowThermometerResults -> {
                if (event is Event.UserConfirm) {
                    // After Thermometer, go DIRECTLY to Questionnaire (Skipping BP)
                    temi.speak("Perfect. Now I have a few questions for you.")
                    _state.value = State.Questionnaire
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.Questionnaire -> {
                // Auto-transition to first question
                _state.value = State.AskPersonalDetails
            }
            is State.AskPersonalDetails -> {
                if (event is Event.PersonalDetailsProvided) {
                    _state.value = State.AskSmokingStatus
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.AskSmokingStatus -> {
                if (event is Event.SmokingAnswered) {
                    _state.value = State.AskAlcoholConsumption
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.AskAlcoholConsumption -> {
                if (event is Event.AlcoholAnswered) {
                    _state.value = State.AskExerciseFrequency
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.AskExerciseFrequency -> {
                if (event is Event.ExerciseAnswered) {
                    _state.value = State.ReviewQuestionnaire
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            is State.AskHeightWeight -> {
                if (event is Event.HeightWeightProvided) {
                    _state.value = State.ReviewQuestionnaire
                }
            }
            is State.ReviewQuestionnaire -> {
                if (event is Event.QuestionnaireComplete) {
                    temi.speak("Thank you! Your health screening is complete.")
                    _state.value = State.Idle
                } else if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
            // ... handle other states
            else -> {
                // Handle global events like Abort
                if (event is Event.Abort) {
                    _state.value = State.Idle
                }
            }
        }
    }

    private suspend fun captureReading(deviceId: String) {
        try {
            val reading = piApi.getLatestReading(deviceId)
            if (reading != null) {
                sessionManager.addReading(reading)
                temi.showReading(reading)
                
                // Handle thermometer separately
                if (deviceId.lowercase().contains("thermometer")) {
                    // Extract temperature value from reading
                    val tempValue = reading.value.replace("°C", "").trim().toFloatOrNull() ?: 0f
                    _state.value = State.ShowThermometerResults(tempValue)
                } else {
                    _state.value = State.ShowReading(reading)
                }
            } else {
                _state.value = State.ErrorRecover("Could not fetch reading")
                temi.speak("I couldn't get the reading. Please try again.")
            }
        } catch (e: Exception) {
            _state.value = State.ErrorRecover(e.message ?: "Unknown error")
        }
    }
}
