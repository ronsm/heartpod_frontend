# Temi Robot Navigation Implementation Guide

## Overview
This guide explains how to implement physical navigation for the Temi robot to:
1. Move to the door to greet users
2. Guide users to the measurement table
3. Navigate between device stations

## Temi SDK Navigation Basics

### 1. Saving Locations

**Using Temi Settings App:**
1. Open Temi Settings on the robot
2. Go to "Locations"
3. Drive the robot to the desired position
4. Click "Save Location"
5. Name it (e.g., "entrance_door", "measurement_table", "oximeter_station")

**Programmatically (if needed):**
```kotlin
// Save current location
val robot = Robot.getInstance()
robot.saveLocation("entrance_door")
```

### 2. Navigating to Saved Locations

```kotlin
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener

class MainActivity : AppCompatActivity(), OnGoToLocationStatusChangedListener {
    private lateinit var robot: Robot
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        robot = Robot.getInstance()
        robot.addOnGoToLocationStatusChangedListener(this)
    }
    
    // Navigate to a location
    fun navigateToLocation(locationName: String) {
        robot.goTo(locationName)
    }
    
    // Listen for navigation status
    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        when (status) {
            OnGoToLocationStatusChangedListener.COMPLETE -> {
                // Navigation completed successfully
                Log.d("Navigation", "Arrived at $location")
                handleArrival(location)
            }
            OnGoToLocationStatusChangedListener.GOING -> {
                Log.d("Navigation", "Moving to $location")
            }
            OnGoToLocationStatusChangedListener.ABORT -> {
                Log.e("Navigation", "Navigation aborted")
            }
        }
    }
    
    fun handleArrival(location: String) {
        when (location) {
            "entrance_door" -> greetUser()
            "measurement_table" -> startScreening()
            "oximeter_station" -> showOximeterInstructions()
        }
    }
}
```

## Implementation for Your Health Screening Flow

### Step 1: Define Locations

Save these locations on your Temi robot:
- `entrance_door` - Where robot waits for users
- `measurement_table` - Where all devices are placed
- `home_base` - Charging/idle position

### Step 2: Update TemiApiImpl.kt

```kotlin
// app/src/main/java/org/hwu/care/healthub/temi/TemiApiImpl.kt

class TemiApiImpl(private val context: Context) : TemiApi {
    private val robot = Robot.getInstance()
    
    override fun navigateTo(location: String) {
        Log.d("TemiApi", "Navigating to: $location")
        robot.goTo(location)
    }
    
    override fun speak(text: String) {
        robot.speak(TtsRequest.create(text, false))
    }
    
    override fun stopMovement() {
        robot.stopMovement()
    }
    
    override fun getLocations(): List<String> {
        return robot.locations
    }
}
```

### Step 3: Update StateMachine for Door Greeting

```kotlin
// Add new states for door greeting
sealed class State {
    object WaitingAtDoor : State()
    object GreetingUser : State()
    object NavigatingToTable : State()
    // ... existing states
}

// Add new events
sealed class Event {
    object UserDetected : Event()
    object UserFollowing : Event()
    // ... existing events
}

// Update handleEvent
suspend fun handleEvent(event: Event) {
    when (currentState) {
        is State.Idle -> {
            // On app start, go to door
            temi.speak("Moving to entrance")
            temi.navigateTo("entrance_door")
            _state.value = State.WaitingAtDoor
        }
        
        is State.WaitingAtDoor -> {
            if (event is Event.UserDetected) {
                temi.speak("Hello! Welcome to the health screening station. Please follow me.")
                temi.navigateTo("measurement_table")
                _state.value = State.NavigatingToTable
            }
        }
        
        is State.NavigatingToTable -> {
            if (event is Event.DeviceArrived) {
                temi.speak("We've arrived at the measurement station. Let's begin your health screening.")
                _state.value = State.ShowInstructions("oximeter")
            }
        }
        
        // ... rest of your existing states
    }
}
```

### Step 4: User Detection

**Option A: Manual Button**
Add a "User Arrived" button on the waiting screen:

```kotlin
@Composable
private fun WaitingAtDoorScreen(onUserArrived: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Waiting for user at entrance...", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onUserArrived) {
            Text("User Arrived - Start Screening", fontSize = 20.sp)
        }
    }
}
```

**Option B: Automatic Detection (Advanced)**
Use Temi's detection features:
```kotlin
robot.addDetectionStateChangedListener { state ->
    if (state == DetectionData.DETECTED) {
        sendEvent(Event.UserDetected)
    }
}
```

### Step 5: Simplified Flow (All Devices at One Table)

Since all devices are at one table, you don't need navigation between devices:

```kotlin
sealed class State {
    object Idle : State()
    object WaitingAtDoor : State()
    object NavigatingToTable : State()
    object AtMeasurementTable : State()
    // Then proceed with: ShowInstructions, AwaitUse, ShowReading, etc.
}

// In handleEvent:
is State.NavigatingToTable -> {
    if (event is Event.DeviceArrived) {
        temi.speak("We're at the measurement station. Let's start with the oximeter.")
        _state.value = State.ShowInstructions("oximeter")
    }
}
```

## Quick Setup Checklist

1. **Save Locations on Temi:**
   - [ ] Open Temi Settings → Locations
   - [ ] Drive to door → Save as "entrance_door"
   - [ ] Drive to table → Save as "measurement_table"
   - [ ] Drive to home → Save as "home_base"

2. **Update Code:**
   - [ ] Add `OnGoToLocationStatusChangedListener` to MainActivity
   - [ ] Add `WaitingAtDoor` and `NavigatingToTable` states
   - [ ] Add navigation logic to StateMachine
   - [ ] Add UI for waiting at door

3. **Test Navigation:**
   - [ ] Test `robot.goTo("entrance_door")`
   - [ ] Test `robot.goTo("measurement_table")`
   - [ ] Verify arrival callbacks work

## Example Complete Flow

```
1. App starts → Robot says "Moving to entrance" → Navigates to door
2. Robot waits at door → Shows "Waiting for user" screen
3. User presses "Start" or robot detects user
4. Robot says "Hello! Please follow me" → Navigates to table
5. Robot arrives → Says "Let's begin" → Shows oximeter instructions
6. User completes all measurements at the table
7. Robot says "Thank you!" → Navigates back to door or home
```

## Important Notes

- **Battery**: Ensure robot is charged before navigation
- **Obstacles**: Temi will avoid obstacles automatically
- **Abort**: Users can stop navigation anytime with the exit button
- **Testing**: Test navigation paths when room is empty first
- **Locations**: Re-save locations if furniture is moved

## Next Steps

1. Save the 3 locations on your Temi robot
2. Test basic navigation with `robot.goTo("location_name")`
3. Implement the state machine changes
4. Test the complete flow end-to-end
