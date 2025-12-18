package org.hwu.care.healthub

import android.graphics.Color
import java.util.Date
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.hwu.care.healthub.databinding.ActivityDeviceDetailBinding
import kotlin.text.format

// Import SharedPreferencesManager if you still use it for other things, otherwise it can be removed
// import org.hwu.care.healthub.SharedPreferencesManager

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceDetailBinding
    private var currentDevice: HealthDevice? = null
    private var displayedDeviceName: String = ""

    // --- Hardcoded values for testing ---
    //LOCAL OPENHAB (on Healthub)
    private val HARDCODED_API_TOKEN = "oh.NHSTHT.QViW3MMVzsp56R8PNt3maoKrv9Z7iP7LNRymiPG25bYqlOXgV0BgggwQ8ZCbBbBdPTy6WxbBW0u0BBqCkiG9w" // Replace with your real token
    private val HARDCODED_OPENHAB_IP = "192.168.2.150" //e.g., "192.168.1.100" or "your-domain.com"

    //LARA OPENHAB
    //private val HARDCODED_API_TOKEN = "oh.MCP.tLQBOZFwd3UAnubBDKJZXoo7PV1tRXCm9fbLDoeRGJrJA1yIUCAyxCfcxdaixCuzrVg4c0khi1hef6XVu3yXQ" // Replace with your real token

    //private val HARDCODED_OPENHAB_IP = "openhabian"
    // --- End of hardcoded values ---

    private val MAX_VISIBLE_ENTRIES = 100
    private lateinit var lineChart: LineChart
    private var lineDataSet: LineDataSet? = null
    private val chartEntries = ArrayList<Entry>()
    private var startTimeMillis: Long = 0L // To calculate time for X-axis
    private var propertyNameToPlot: String? = null // Name of the property being plotted

    // Comment out or remove SharedPreferencesManager if ONLY used for token/IP in this Activity
    // private lateinit var sharedPreferencesManager: SharedPreferencesManager
    // private val apiToken by lazy { sharedPreferencesManager.getToken() ?: "" }
    // private val openhabIp by lazy { sharedPreferencesManager.getOpenhabIp() ?: "10.0.2.2" }

    companion object {
        const val EXTRA_DEVICE = "EXTRA_DEVICE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("DeviceDetailActivity", "onCreate started.")

        lineChart = binding.livePlotChart
        // sharedPreferencesManager = SharedPreferencesManager(applicationContext) // Comment out if not needed
        startTimeMillis = System.currentTimeMillis()

        currentDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DEVICE, HealthDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DEVICE)
        }

        if (currentDevice == null) {
            Log.e("DeviceDetailActivity", "HealthDevice data not found in Intent. Finishing.")
            finish()
            return
        }
        displayedDeviceName = currentDevice!!.name
        Log.i("DeviceDetailActivity", "Displaying details for: ${currentDevice!!.label} (Name: $displayedDeviceName)")

        populateUi()

        // ▼▼▼ CALL setupChart() HERE ▼▼▼
        setupChart() // Initialize chart appearance

        // Determine which property to plot (e.g., the first numerical one)
        determinePropertyToPlot() // This might depend on currentDevice, so call after device is loaded

    }

    override fun onStart() {
        super.onStart()
        Log.d("DeviceDetailActivity", "onStart - Starting SSE listening for device: $displayedDeviceName")
        startListeningForDeviceUpdates()
    }

    override fun onStop() {
        super.onStop()
        Log.d("DeviceDetailActivity", "onStop - Explicitly stopping SSE listening contribution.")
        SseRepository.stopExplicitly()
    }


    private fun populateUi() {
        currentDevice?.let { deviceData ->
            Log.d("DeviceDetailActivity", "Populating UI with initial data for ${deviceData.label}")

            Glide.with(this)
                .load(deviceData.pictureUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(com.google.android.material.R.drawable.mtrl_ic_error)
                .into(binding.detailDeviceImageView)

            binding.detailDeviceNameTextView.text = deviceData.label
            updateStatusInUi(deviceData.status)
            updateUseDueInUi(deviceData.useDue)
            updateLastUseTimeInUi(deviceData.lastUseTime)

            // ▼▼▼ CORRECTED REFERENCE HERE ▼▼▼
            binding.propertiesContainerTop.removeAllViews()
            val inflater = LayoutInflater.from(this)
            for (property in deviceData.properties) {
                // Pass the new container to addPropertyView if it adds views directly,
                // or ensure addPropertyView also uses propertiesContainerTop
                addPropertyView(inflater, property.name, property.label ?: property.name, property.state ?: "N/A")
            }
        }
    }


    private fun addPropertyView(inflater: LayoutInflater, itemName: String, itemLabel: String, itemState: String) {
        // Inflate the NEW horizontal item layout
        // Make sure your binding.propertiesContainerTop is the correct reference to the
        // LinearLayout inside your HorizontalScrollView
        val propertyView = inflater.inflate(
            R.layout.list_item_dynamic_property_horizontal, // Use the new item layout
            binding.propertiesContainerTop, // The parent ViewGroup to attach to during inflation (optional for addView)
            false // Attach to root: false, because we will add it manually
        )

        // Get references to the TextViews INSIDE the inflated propertyView
        val valueTextView = propertyView.findViewById<TextView>(R.id.propertyValueTextView)
        val labelTextView = propertyView.findViewById<TextView>(R.id.propertyLabelTextView)

        valueTextView.text = itemState // Set the large value text
        labelTextView.text = itemLabel // Set the smaller label text underneath

        propertyView.tag = itemName // Set the tag on the root of the inflated item view (the LinearLayout)
        // This is used to find this specific property's view later for updates.

        binding.propertiesContainerTop.addView(propertyView) // Add the new item view to your horizontal LinearLayout
        Log.d("DeviceDetail_UI", "Added property view for $itemLabel ($itemName) with state $itemState")
    }

    private fun updateStatusInUi(newStatus: String) {
        // ... (this function remains the same)
        binding.detailStatusTextView.text = "Status: $newStatus"
    }

    private fun updateUseDueInUi(newUseDue: String) {
        // ... (this function remains the same)
        if (newUseDue.equals("YES", ignoreCase = true)) {
            binding.detailUseDueTextView.visibility = View.VISIBLE
            binding.detailUseDueTextView.text = "Time to Use"
        } else {
            binding.detailUseDueTextView.visibility = View.GONE
        }
    }

    private fun updateLastUseTimeInUi(newLastUseTime: String) {
        // ... (this function remains the same)
        Log.d("DeviceDetailActivity", "Last Use Time updated in model (UI update depends on layout): $newLastUseTime")
    }


    private fun updatePropertyInUiByName(itemName: String, newState: String) {
        Log.d("DeviceDetailActivity_UI_Update", "Attempting to update UI for property name: $itemName to state: $newState")

        // Iterate through the children of propertiesContainerTop
        for (i in 0 until binding.propertiesContainerTop.childCount) {
            val propertyItemView = binding.propertiesContainerTop.getChildAt(i) // This is the root LinearLayout of your item
            if (propertyItemView.tag == itemName) {
                // Found the correct property item view (the LinearLayout for this specific property)
                // Now find the TextView responsible for displaying the VALUE inside this item view
                val valueTextView = propertyItemView.findViewById<TextView>(R.id.propertyValueTextView)
                if (valueTextView != null) {
                    valueTextView.text = newState
                    Log.i("DeviceDetailActivity_UI_Update", "UI updated for property: $itemName to $newState")
                } else {
                    Log.w("DeviceDetailActivity_UI_Update", "propertyValueTextView not found in item for $itemName")
                }
                return // Exit after updating
            }
        }
        Log.w("DeviceDetailActivity_UI_Update", "Could not find property view with tag: $itemName to update in propertiesContainerTop.")
    }

    // --- Chart Methods ---
    private fun determinePropertyToPlot() {
        propertyNameToPlot = currentDevice?.properties?.firstOrNull {
            // Attempt to parse the state as a Float to see if it's numerical
            // This is a basic check; you might need more robust parsing
            it.state?.toFloatOrNull() != null
        }?.name

        if (propertyNameToPlot != null) {
            val propertyLabel = currentDevice?.properties?.find { it.name == propertyNameToPlot }?.label ?: propertyNameToPlot
            Log.i("DeviceDetailActivity_Chart", "Will attempt to plot: $propertyLabel (Item: $propertyNameToPlot)")
            initializeLineDataSet(propertyLabel ?: "Data") // Initialize dataset with the label
        } else {
            Log.w("DeviceDetailActivity_Chart", "No numerical property found to plot.")
            // Optionally, disable the chart or show a message
            lineChart.setNoDataText("No numerical data available to plot.")
            lineChart.invalidate()
        }
    }

    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setDrawGridBackground(false)
        lineChart.setBackgroundColor(Color.TRANSPARENT) // Or your desired background

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f // Minimum interval between labels

        // Define 'format' here, accessible to the ValueFormatter
        // You could also define 'format' as a property of the DeviceDetailActivity class
        val simpleDateFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Convert value (seconds since start) back to a readable time
                val millis = startTimeMillis + TimeUnit.SECONDS.toMillis(value.toLong())
                // Now using 'simpleDateFormat' which is in scope
                return simpleDateFormat.format(java.util.Date(millis))
            }
        }

        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)
        // Set text color, etc. as needed for your theme
        // leftAxis.textColor = ContextCompat.getColor(this, R.color.your_axis_text_color)

        lineChart.axisRight.isEnabled = false // Disable right Y-axis

        // Initialize with an empty LineData object
        lineChart.data = LineData()
        lineChart.invalidate() // Refresh chart
    }


    private fun initializeLineDataSet(label: String) {
        lineDataSet = LineDataSet(chartEntries, label).apply {
            color = ContextCompat.getColor(this@DeviceDetailActivity, R.color.purple_500) // Example color
            valueTextColor = ContextCompat.getColor(this@DeviceDetailActivity, R.color.black) // Example color
            setCircleColor(ContextCompat.getColor(this@DeviceDetailActivity, R.color.purple_700))
            circleRadius = 3f
            setDrawCircleHole(false)
            lineWidth = 2f
            setDrawValues(false) // Don't draw exact values on each point for cleaner look
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smoother lines
        }
        val data = LineData(lineDataSet)
        lineChart.data = data
        lineChart.invalidate()
    }

    private fun addEntryToChart(value: Float) {
        // Capture the current state of lineDataSet into a local immutable variable
        val currentLineDataSet = this.lineDataSet

        if (currentLineDataSet == null) {
            Log.w("DeviceDetailActivity_Chart", "LineDataSet not initialized. Attempting to initialize.")
            // Attempt to determine and initialize again
            if (propertyNameToPlot == null) {
                determinePropertyToPlot() // This might initialize lineDataSet
            }
            // If determinePropertyToPlot calls initializeLineDataSet, 'this.lineDataSet' might be updated
            // So, we re-capture it.
            val potentiallyInitializedLineDataSet = this.lineDataSet
            if (potentiallyInitializedLineDataSet == null) {
                Log.e("DeviceDetailActivity_Chart", "Failed to initialize LineDataSet. Cannot add entry.")
                return // Still null, cannot proceed
            }
            // Use the newly (potentially) initialized dataset for this operation
            proceedWithAddingEntry(potentiallyInitializedLineDataSet, value)
        } else {
            // lineDataSet was already non-null, proceed
            proceedWithAddingEntry(currentLineDataSet, value)
        }
    }

    private fun proceedWithAddingEntry(activeLineDataSet: LineDataSet, value: Float) {
        Log.d("ChartUpdate", "proceedWithAddingEntry called with value: $value. ActiveDataSet entry count: ${activeLineDataSet.entryCount}")

        val data = lineChart.data
        if (data != null) {
            Log.d("ChartUpdate", "Current chart data object: $data. DataSetCount: ${data.dataSetCount}")

            if (data.dataSetCount == 0) {
                Log.e("ChartUpdate", "Chart data has NO datasets. Adding activeLineDataSet.")
                // This case should ideally be handled by initializeLineDataSet ensuring data is set.
                // If this happens, it means lineChart.data was perhaps set to an empty LineData()
                // without any datasets after initialization.
                data.addDataSet(activeLineDataSet) // Add it if it's missing
            } else if (data.getDataSetByIndex(0) != activeLineDataSet) {
                Log.w("ChartUpdate", "Mismatched dataset. Chart's current dataset: ${data.getDataSetByIndex(0)}, activeLineDataSet: $activeLineDataSet. Re-assigning data to chart.")
                val newLineData = LineData(activeLineDataSet)
                lineChart.data = newLineData
                // The 'data' variable now refers to the old LineData object.
                // We should use newLineData for the subsequent operations or re-fetch 'lineChart.data'.
                // For simplicity, let's assume if we re-assign, the next call will be correct.
                // Or, more robustly:
                // lineChart.data = LineData(activeLineDataSet) // Ensure chart uses the active one
                // val currentChartData = lineChart.data // Get the fresh data object
                // proceedWithAddingEntry will be called again or this path needs to be self-contained
                // This logic path can get complex. The primary goal is that the chart has the *correct* activeLineDataSet.
            }

            // It's safer to get the data object from the chart again in case it was replaced above.
            val currentChartData = lineChart.data
            if (currentChartData == null || currentChartData.dataSetCount == 0) {
                Log.e("ChartUpdate", "Chart data is null or empty after potential reassignment. Cannot add entry.")
                return
            }

            // Ensure we are working with the actual dataset in the chart's current data
            val targetDataSet = currentChartData.getDataSetByIndex(0) as? LineDataSet
            if (targetDataSet == null) {
                Log.e("ChartUpdate", "Target dataset at index 0 is null or not a LineDataSet.")
                return
            }
            // If 'activeLineDataSet' was re-assigned to the chart, targetDataSet should now be 'activeLineDataSet'.
            // We will add the entry to 'targetDataSet' which is confirmed to be in the chart.

            val timeInSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000f
            val entry = Entry(timeInSeconds, value)

            // Add entry to the dataset that's confirmed to be in the chart.
            currentChartData.addEntry(entry, 0) // Adds to the dataset at index 0 of currentChartData
            Log.d("ChartUpdate", "Entry added. New count in targetDataSet: ${targetDataSet.entryCount}")


            if (targetDataSet.entryCount > MAX_VISIBLE_ENTRIES) {
                val removed = targetDataSet.removeFirst() // removeFirst returns boolean
                Log.d("ChartUpdate", "Max entries reached. Oldest entry removed: $removed. New count: ${targetDataSet.entryCount}")
            }

            Log.d("ChartUpdate", "Notifying data changed and chart changed.")
            currentChartData.notifyDataChanged()
            lineChart.notifyDataSetChanged()

            Log.d("ChartUpdate", "Invalidating chart.")
            lineChart.invalidate() // Explicitly redraw

            lineChart.setVisibleXRangeMaximum(60f)
            lineChart.moveViewToX(currentChartData.entryCount.toFloat()) // Use currentChartData.entryCount for accuracy
            Log.d("DeviceDetailActivity_Chart", "Added entry: X=${timeInSeconds}, Y=${value}. Chart invalidated.")
        } else {
            Log.e("DeviceDetailActivity_Chart", "Chart data is null. Cannot add entry. Initializing LineData.")
            // If lineChart.data is null, it means it was never initialized or was set to null.
            // This is a more fundamental issue than just a mismatched dataset.
            // We should ensure initializeLineDataSet sets it up.
            // For robustness here, we could try to initialize it, but it indicates a flaw in setup.
            val dataSetToUse = this.lineDataSet // Get the class member
            if (dataSetToUse != null) {
                lineChart.data = LineData(dataSetToUse)
                Log.i("DeviceDetailActivity_Chart", "Initialized chart data with existing lineDataSet. Try adding entry again if this was the issue.")
                // Potentially recall proceedWithAddingEntry or just let the next event trigger it.
            } else {
                Log.e("DeviceDetailActivity_Chart", "lineDataSet class member is also null. Cannot initialize chart data.")
            }
        }
    }



    private fun startListeningForDeviceUpdates() {
        // ▼▼▼ Use hardcoded values ▼▼▼
        val tokenToUse = HARDCODED_API_TOKEN
        val ipToUse = HARDCODED_OPENHAB_IP
        // ▲▲▲ End of using hardcoded values ▲▲▲

        Log.d("DeviceDetailActivity_SSE", "[HARDCODED] Attempting to start SSE. Token: '$tokenToUse', IP: '$ipToUse'")

        if (tokenToUse.isBlank() || ipToUse.isBlank() || ipToUse == "0.0.0.0") { // Check the hardcoded values
            Log.w("DeviceDetailActivity_SSE", "[HARDCODED] Cannot start SSE: API Token or OpenHAB IP is invalid for device '$displayedDeviceName'.")
            return
        }
        if (displayedDeviceName.isBlank()) {
            Log.w("DeviceDetailActivity_SSE", "[HARDCODED] Cannot start SSE: No device name to filter for.")
            return
        }

        Log.d("DeviceDetailActivity_SSE", "[HARDCODED] Attempting SseRepository.startListening for device '$displayedDeviceName'. Token: ${tokenToUse.isNotBlank()}, IP: $ipToUse")
        SseRepository.startListening(tokenToUse, ipToUse) // Use hardcoded values

        lifecycleScope.launch {
            SseRepository.itemStateChanges.collectLatest { (itemName, newState) ->
                val isDeviceStatus = itemName == "${displayedDeviceName}_Status"
                val isDeviceLastUse = itemName == "${displayedDeviceName}_LastUse"
                val isDeviceUseDue = itemName == "${displayedDeviceName}_UseDue"
                val isDeviceProperty = currentDevice?.properties?.any { it.name == itemName } == true

                var deviceWasUpdated = false

                if (isDeviceStatus) {
                    Log.i("DeviceDetailActivity_SSE", "[HARDCODED] Relevant STATUS Update for '$displayedDeviceName': Item='$itemName', NewState='$newState'")
                    currentDevice = currentDevice?.copy(status = newState)
                    deviceWasUpdated = true
                    currentDevice?.let { updateStatusInUi(it.status) }
                } else if (isDeviceLastUse) {
                    Log.i("DeviceDetailActivity_SSE", "[HARDCODED] Relevant LAST USE Update for '$displayedDeviceName': Item='$itemName', NewState='$newState'")
                    currentDevice = currentDevice?.copy(lastUseTime = newState)
                    deviceWasUpdated = true
                    currentDevice?.let { updateLastUseTimeInUi(it.lastUseTime) }
                } else if (isDeviceUseDue) {
                    Log.i("DeviceDetailActivity_SSE", "[HARDCODED] Relevant USE DUE Update for '$displayedDeviceName': Item='$itemName', NewState='$newState'")
                    currentDevice = currentDevice?.copy(useDue = newState)
                    deviceWasUpdated = true
                    currentDevice?.let { updateUseDueInUi(it.useDue) }
                } else if (isDeviceProperty) {
                    Log.i("DeviceDetailActivity_SSE", "[HARDCODED] Relevant PROPERTY Update for '$displayedDeviceName': Item='$itemName', NewState='$newState'")
                    currentDevice = currentDevice?.copy(
                        properties = currentDevice!!.properties.map { prop ->
                            if (prop.name == itemName) prop.copy(state = newState) else prop
                        }
                    )
                    deviceWasUpdated = true
                    updatePropertyInUiByName(itemName, newState)

                    // --- Add to Chart if it's the property we're plotting ---
                    if (itemName == propertyNameToPlot) {
                        newState.toFloatOrNull()?.let { numericValue ->
                            addEntryToChart(numericValue)
                        } ?: Log.w("DeviceDetailActivity_Chart", "Received non-numeric state for $itemName: $newState")
                    }
                }

                if (deviceWasUpdated) {
                    Log.d("DeviceDetailActivity_SSE", "[HARDCODED] currentDevice object updated for item: $itemName. New status: ${currentDevice?.status}")
                }
            }
        }
    }
}
