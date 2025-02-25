package com.example.bluepad;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    private final List<BluetoothDevice> devices;
    private final OnDeviceSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private Context context;

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    public BluetoothDeviceAdapter(List<BluetoothDevice> devices, OnDeviceSelectedListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        holder.deviceName.setText(device.getName() != null ? device.getName() : device.getAddress());
        holder.itemView.setBackgroundColor(selectedPosition == position
                ? ContextCompat.getColor(context, R.color.selected_item)
                : ContextCompat.getColor(context, android.R.color.transparent));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView deviceName;

        ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            notifyItemChanged(selectedPosition);
            selectedPosition = getAdapterPosition();
            notifyItemChanged(selectedPosition);
            listener.onDeviceSelected(devices.get(selectedPosition));

            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(100);  // Vibrate for 100 milliseconds
            }
        }
    }
}