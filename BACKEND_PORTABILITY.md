# Backend Configuration Portability

## How HealthHub Works in Different Environments

The HealthHub architecture is **environment-agnostic** because all backend communication goes through a single configuration point.

## Current Architecture (Prototype)

```
Temi Robot → Android App → Backend API → Data Storage
```

**All backend calls go through ONE file:**
`app/src/main/java/org/hwu/care/healthub/data/PiApiImpl.kt`

## The Magic: One Configuration Change

### Current (OpenHAB Prototype)
```kotlin
// PiApiImpl.kt - Line 23
private const val BASE_URL = "http://192.168.2.150:8080/rest/"
```

### NHS Production
```kotlin
// PiApiImpl.kt - Line 23
private const val BASE_URL = "https://nhs-fhir-api.nhs.uk/api/"
```

### Any Other Environment
```kotlin
// PiApiImpl.kt - Line 23
private const val BASE_URL = "https://your-backend.com/api/"
```

**That's it!** Change one line, rebuild APK, deploy.

## What Stays Exactly the Same

- ✅ Temi robot hardware
- ✅ Android app code (99.9%)
- ✅ AI agent logic
- ✅ Voice interaction
- ✅ Questionnaire flow
- ✅ UI screens
- ✅ User experience

## What Changes (Configuration Only)

### 1. Backend URL (1 line)
```kotlin
private const val BASE_URL = "new-url-here"
```

### 2. Data Format Adapter (Optional)
If the new backend uses different data format (e.g., FHIR instead of OpenHAB items):

```kotlin
// Add a simple adapter
fun convertToFHIR(questionnaire: PatientQuestionnaire): FHIRResource {
    return FHIRResource(
        resourceType = "Observation",
        subject = questionnaire.firstName,
        // ... map fields
    )
}
```

### 3. Authentication (If Needed)
```kotlin
// Add auth header
private val authToken = "Bearer your-token"
```

## Example: Switching from OpenHAB to NHS FHIR

**Before (OpenHAB):**
```kotlin
interface PiApiService {
    @GET("items/Oximeter_SpO2")
    suspend fun getSpO2(): Response<OpenHabItemResponse>
}
```

**After (NHS FHIR):**
```kotlin
interface PiApiService {
    @GET("Observation?code=59408-5") // SpO2 LOINC code
    suspend fun getSpO2(): Response<FHIRBundle>
}
```

**Impact:** Change 2 files, rebuild APK. Everything else identical.

## Why This Works

The app uses **Repository Pattern** - all data access goes through `PiApiImpl.kt`:
- UI screens don't know about backend
- State machine doesn't know about backend
- AI agent doesn't know about backend

Only `PiApiImpl.kt` knows the backend details.

## Deployment Scenarios

### Scenario 1: NHS Hospital
```kotlin
BASE_URL = "https://hospital-trust.nhs.uk/fhir/"
AUTH = "NHS OAuth token"
```

### Scenario 2: Private Clinic
```kotlin
BASE_URL = "https://clinic-ehr.com/api/"
AUTH = "API key"
```

### Scenario 3: Research Lab
```kotlin
BASE_URL = "http://localhost:8080/" // Local server
AUTH = null
```

### Scenario 4: Cloud Deployment
```kotlin
BASE_URL = "https://healthub-cloud.azure.com/api/"
AUTH = "Azure AD token"
```

## Summary

**The architecture is portable because:**
1. Backend is abstracted behind one interface
2. Configuration is centralized in one file
3. No hardcoded dependencies on OpenHAB
4. Data models are generic (not OpenHAB-specific)

**To deploy in a new environment:**
1. Change `BASE_URL` in `PiApiImpl.kt`
2. Add auth if needed (few lines)
3. Add data format adapter if needed (one function)
4. Rebuild APK
5. Deploy to Temi

**No changes needed to:**
- Voice interaction
- AI prompts
- UI screens
- State machine
- User experience

This is why OpenHAB is just a **prototype backend** - it can be swapped for any other system with minimal effort.
