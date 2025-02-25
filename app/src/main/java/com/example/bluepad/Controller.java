package com.example.bluepad;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Controller extends AppCompatActivity {

    public static final String EXTRA_DEVICE = "com.example.bluepad.DEVICE";
    private static final UUID HC_06_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private TextView receivedTextView;
    private Handler handler;
    private StringBuilder messageBuilder = new StringBuilder();
    private String currentMessage = ""; // Variable to store the current message
    private boolean isSending = false;
    private Handler sendHandler;
    private Runnable sendRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        receivedTextView = findViewById(R.id.textView);
        handler = new Handler(Looper.getMainLooper());
        sendHandler = new Handler(Looper.getMainLooper());

        BluetoothDevice device = getIntent().getParcelableExtra(EXTRA_DEVICE);
        if (device != null) {
            connectToDevice(device);
        }

        setupButtonListeners();
    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(HC_06_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();
                runOnUiThread(() -> Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show());

                startListeningForData();
            } catch (IOException e) {
                Log.e("Controller", "Error connecting to device", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to connect to " + device.getName(), Toast.LENGTH_SHORT).show());
                try {
                    if (bluetoothSocket != null) {
                        bluetoothSocket.close();
                    }
                } catch (IOException ex) {
                    Log.e("Controller", "Error closing socket", ex);
                }
            }
        }).start();
    }

    private void startListeningForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String receivedMessage = new String(buffer, 0, bytes);
                    messageBuilder.append(receivedMessage);
                    int endOfLineIndex = messageBuilder.indexOf("\n");
                    if (endOfLineIndex != -1) {
                        String completeMessage = messageBuilder.substring(0, endOfLineIndex).trim();
                        messageBuilder.delete(0, endOfLineIndex + 1);
                        handler.post(() -> updateReceivedTextView(completeMessage));
                    }
                } catch (IOException e) {
                    Log.e("Controller", "Error reading from input stream", e);
                    break;
                }
            }
        }).start();
    }

    private void updateReceivedTextView(String message) {
        if (!message.equals(currentMessage) && !message.isEmpty()) {
            receivedTextView.setText(message);
            currentMessage = message; // Update the current message
        }
    }

    private void setupButtonListeners() {
        setButtonTouchListener(R.id.bUp, 'U');
        setButtonTouchListener(R.id.bForward, 'F');
        setButtonTouchListener(R.id.bBack, 'B');
        setButtonTouchListener(R.id.bDown, 'D');
        setButtonTouchListener(R.id.bLeft, 'L');
        setButtonTouchListener(R.id.bRight, 'R');
        setButtonTouchListener(R.id.bStartStop, 'S');
        setButtonTouchListener(R.id.bCW, 'C');
        setButtonTouchListener(R.id.bCCW, 'W');
        setButtonTouchListener(R.id.bOption, 'O');
    }

    private void setButtonTouchListener(int buttonId, final char character) {
        Button button = findViewById(buttonId);
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startSendingCommand(character);
                    return true;
                case MotionEvent.ACTION_UP:
                    stopSendingCommand();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }
            return false;
        });
    }

    private void startSendingCommand(char command) {
        isSending = true;
        sendRunnable = () -> {
            if (isSending) {
                sendCommand(String.valueOf(command));
                sendHandler.postDelayed(sendRunnable, 100); // Send command every 100ms
            }
        };
        sendHandler.post(sendRunnable);
    }

    private void stopSendingCommand() {
        isSending = false;
        sendHandler.removeCallbacks(sendRunnable);
    }

    private void sendCommand(String command) {
        if (outputStream != null) {
            try {
                outputStream.write((command).getBytes());
            } catch (IOException e) {
                Log.e("Controller", "Error sending command", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e("Controller", "Error closing socket", e);
            }
        }
    }
}