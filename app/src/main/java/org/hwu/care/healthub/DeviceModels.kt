package org.hwu.care.healthub

import android.os.Parcelable // <-- 1. IMPORT THIS
import kotlinx.parcelize.Parcelize // This should already be there

// Represents a single OpenHAB item (point/property) from the JSON response
// This might be your original OpenhabItem, or you might rename it to OpenhabPointItem
// if it specifically represents the individual data points.
// Let's assume your existing OpenhabItem is suitable for representing these points.
@Parcelize
data class OpenhabItem( // Or OpenhabPointItem if you prefer to rename for clarity
    val name: String,
    val label: String?, // Make nullable if it can be missing
    val state: String?, // Make nullable
    val type: String?,  // Make nullable
    val link: String?,  // Add if present in JSON and needed
    val groupNames: List<String> = emptyList(),
    val tags: List<String> = emptyList() // Add if present in JSON and needed
    // Add other fields like 'stateDescription', 'metadata' if you need to parse them
    // val stateDescription: Map<String, Any>?,
    // val metadata: Map<String, Any>?
) : Parcelable

// NEW: Represents an Equipment Group like "PolarH10" which contains points
// This corresponds to each object in the top-level "members" array from your curl response
@Parcelize
data class OpenhabEquipmentItem(
    val name: String,
    val label: String?,
    val link: String?,
    val type: String?, // e.g., "Group"
    // Add any other fields that an equipment group itself might have (like its own state, tags, etc.)
    val members: List<OpenhabItem> // This list contains the actual points (like _LastUse, _Status)
    // Uses your existing OpenhabItem (or OpenhabPointItem)
) : Parcelable

// NEW: This is the top-level wrapper for the entire JSON response
// from /rest/items/gHealth?metadata=.*&recursive=true&parents=false
@Parcelize
data class OpenhabRootResponse(
    val members: List<OpenhabEquipmentItem>
    // If the gHealth item itself has properties at the root of the JSON object
    // (outside the main "members" array), you can add them here too. For example:
    // val link: String?,
    // val state: String?,
    // val editable: Boolean?,
    // val type: String?, // Should be "Group" for gHealth itself
    // val name: String?, // Should be "gHealth"
    // val label: String?, // Label for "gHealth"
    // val category: String?,
    // val tags: List<String>?,
    // val groupNames: List<String>?
) : Parcelable

// Represents a fully processed healthcare device for your UI
// (This is your existing HealthDevice class - no changes needed here unless
//  its 'properties' field needs adjustment based on the new parsing)
@Parcelize
data class HealthDevice(
    val name: String,
    val label: String,
    var status: String = "OFFLINE",
    var useDue: String = "NO",
    var lastUseTime: String = "Unknown",
    val pictureUrl: String = "",
    val instructionsUrl: String = "",
    // This 'properties' list will be populated from the dynamic properties
    // found within an OpenhabEquipmentItem's 'members' list.
    // Ensure OpenhabItem here has all the fields you want to display for these properties.
    var properties: List<OpenhabItem> = emptyList()
) : Parcelable

