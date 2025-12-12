package org.hwu.care.healthub // Or your appropriate package

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
//import androidx.compose.ui.semantics.error
// REMOVED: import androidx.compose.ui.semantics.error
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
//import androidx.wear.compose.material.placeholder
// REMOVED: import androidx.wear.compose.material.placeholder
import com.bumptech.glide.Glide
// Make sure R is imported from your app's package, e.g., import org.hwu.care.healthub.R
// Make sure HealthDevice is imported if it's in a different package or file

class DeviceGridAdapter(private val onItemClicked: (HealthDevice) -> Unit) :
    ListAdapter<HealthDevice, DeviceGridAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_device, parent, false) // Use your existing item layout
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = getItem(position)
        holder.bind(device)
        holder.itemView.setOnClickListener {
            onItemClicked(device)
        }
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // These IDs must match the IDs in your list_item_device.xml
        private val deviceImageView: ImageView = itemView.findViewById(R.id.deviceImageView)
        private val deviceNameTextView: TextView = itemView.findViewById(R.id.deviceNameTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val useDueTextView: TextView = itemView.findViewById(R.id.useDueTextView)

        fun bind(device: HealthDevice) {
            deviceNameTextView.text = device.label // Or device.name as you prefer
            statusTextView.text = "Status: ${device.status}" // Example formatting

            // --- Use Due Emphasis ---
            if (device.useDue.equals("YES", ignoreCase = true) || device.useDue.equals("ON", ignoreCase = true)) {
                useDueTextView.visibility = View.VISIBLE
                useDueTextView.text = "Time to Use" // Your chosen text

                // Set the icon
                val useDueIcon = ContextCompat.getDrawable(itemView.context, R.drawable.time_to_use) // Use your icon's name
                // Optional: Tint the icon to match the text color or another emphasis color
                useDueIcon?.setTintList(useDueTextView.textColors) // Tints icon to match text color
                // Or a specific color:
                // useDueIcon?.setTint(ContextCompat.getColor(itemView.context, R.color.use_due_yes))

                useDueTextView.setCompoundDrawablesWithIntrinsicBounds(useDueIcon, null, null, null) // Sets icon to the start (left)

                useDueTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.use_due_yes))
                useDueTextView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                useDueTextView.visibility = View.GONE
                useDueTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null) // Clear icon if view is reused
            }

            Glide.with(itemView.context)
                .load(device.pictureUrl)
                .placeholder(R.drawable.ic_placeholder_image) // This is correct for Views
                .error(R.drawable.ic_error_image)         // This is correct for Views
                .into(deviceImageView)
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<HealthDevice>() {
        override fun areItemsTheSame(oldItem: HealthDevice, newItem: HealthDevice): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: HealthDevice, newItem: HealthDevice): Boolean {
            return oldItem == newItem
        }
    }
}
