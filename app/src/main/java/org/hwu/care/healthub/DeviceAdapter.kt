package org.hwu.care.healthub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DeviceAdapter(private val devices: List<HealthDevice>) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceImage: ImageView = view.findViewById(R.id.deviceImageView)
        val deviceName: TextView = view.findViewById(R.id.deviceNameTextView)
        val deviceStatus: TextView = view.findViewById(R.id.statusTextView)
        val useDue: TextView = view.findViewById(R.id.useDueTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]

        holder.deviceName.text = device.label
        holder.deviceStatus.text = "Status: ${device.status}"

        // Show "USE DUE" only if the state is ON or YES
        if (device.useDue.equals("ON", ignoreCase = true) || device.useDue.equals("YES", ignoreCase = true)) {
            holder.useDue.visibility = View.VISIBLE
            holder.useDue.text = "USE DUE"
        } else {
            holder.useDue.visibility = View.GONE
        }

        // Use Glide to load the picture from the URL
        Glide.with(holder.itemView.context)
            .load(device.pictureUrl)
            .placeholder(R.drawable.ic_launcher_background) // A default image
            .error(R.drawable.ic_launcher_background) // Image on error
            .into(holder.deviceImage)

        // TODO: Set an OnClickListener to open a detail activity
        // holder.itemView.setOnClickListener { ... }
    }

    override fun getItemCount() = devices.size
}