package com.thesis.adminthesis;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.messaging.FirebaseMessaging;
import com.thesis.adminthesis.model.Item;
import com.thesis.adminthesis.module.ItemAdapter;
import com.thesis.adminthesis.module.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private EditText serverIpEditText, itemEditText, serverPassword;
    private ItemAdapter itemAdapter;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private final String TAG = "NotificationApp";
    private String notificationToken = "";
    private Button startButton;
    private Button stopButton;
    private Button addButton;
    private Button deleteButton;

    private final NetworkUtil networkUtil = new NetworkUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            // Request permissions if not granted
            requestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET, Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
        } else {
            setContentView(R.layout.activity_main);

            serverIpEditText = findViewById(R.id.serverIpEditText);
            itemEditText = findViewById(R.id.itemEditText);
            serverPassword = findViewById(R.id.serverPassword);
            startButton = findViewById(R.id.startButton);
            stopButton = findViewById(R.id.stopButton);
            addButton = findViewById(R.id.addButton);
            deleteButton = findViewById(R.id.deleteButton);
            Button connectButton = findViewById(R.id.connect);
            connectButton.setOnClickListener(view -> {
                try {
                    runOnStopEditingServerIp();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });
            RecyclerView recyclerView = findViewById(R.id.recyclerView);

            // Initialize RecyclerView
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            recyclerView.setLayoutManager(gridLayoutManager);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            itemAdapter = new ItemAdapter(new ArrayList<>());
            recyclerView.setAdapter(itemAdapter);

            // Load the list from the server
            startButton.setOnClickListener(view -> sendCommandToServer("START"));
            stopButton.setOnClickListener(view -> sendCommandToServer("STOP"));

            // Button to add item
            addButton.setOnClickListener(view -> {
                String newItem = itemEditText.getText().toString();
                if (!newItem.isEmpty()) {
                    addItemToServer(newItem);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter an item.", Toast.LENGTH_SHORT).show();
                }
            });

            // Button to delete selected item
            deleteButton.setOnClickListener(view -> removeDevice(itemAdapter.getSelectedItemString()));
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }
                String token = task.getResult();
                notificationToken = token;
                Log.i(TAG, token);

            });
            disableFunctionButton();
        }
    }

    private void addItemsToList(String item) {
        runOnUiThread(() -> {
            itemAdapter.addItem(item);
            itemAdapter.notifyItemInserted(itemAdapter.getItemCount() - 1);
        });
    }

    private void enableFunctionButton() {
        if (!startButton.isEnabled()) {
            startButton.setEnabled(true);
            stopButton.setEnabled(true);
            addButton.setEnabled(true);
            deleteButton.setEnabled(true);
//            connectButton.setEnabled(true);
        }
    }

    private void disableFunctionButton() {
        if (startButton.isEnabled()) {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            addButton.setEnabled(false);
            deleteButton.setEnabled(false);
//            connectButton.setEnabled(false);
        }
    }

    private void runOnStopEditingServerIp() throws JSONException {
        String serverIpAddress = serverIpEditText.getText().toString();
        if (serverPassword.getText() != null && serverPassword.getText().length() != 0 && checkServerIpEmpty(serverIpAddress) && NetworkUtil.isValidIpAndPortUrl(serverIpAddress)) {
            JSONObject json = new JSONObject();
            json.put("password", serverPassword.getText());
            networkUtil.sendPostRequest(serverIpAddress + "/api/control/all", json.toString(), (items, statusCode) -> {
                // Handle the response (update UI or use the data)
                itemAdapter.removeAllItem();
                if (statusCode == 200) {
                    enableFunctionButton();
                    for (Item item : items) {
                        if (NetworkUtil.isValidIp(item.getDeviceConnection())) {
                            addItemsToList(NetworkUtil.getIpAndPort(item.getDeviceConnection()));
                        } else {
                            addItemsToList(item.getDeviceConnection());
                        }
                    }
                    try {
                        JSONObject json1 = new JSONObject();
                        json1.put("token", notificationToken);
                        String serverIp = serverIpEditText.getText().toString();
                        new NetworkUtil().sendPostRequest(serverIp + "/api/notify/resign", json1);
                    } catch (Exception e) {
                        Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                    }
                } else {
                    disableFunctionButton();
                    Toast.makeText(MainActivity.this, "Server can't handle", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            disableFunctionButton();
            itemAdapter.removeAllItem();
            Toast.makeText(MainActivity.this, "Please enter correct server IP.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED)) {
                // Permissions denied, show alert and exit
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this).setTitle("Permission Denied").setMessage("Network permissions are required to use this app. Please grant them in settings.").setCancelable(false).setPositiveButton("Exit", (dialog, which) -> finish()).show();
    }

    public void removeDevice(String deviceIp) {
        if (deviceIp != null) {
            JSONObject json = new JSONObject();
            try {
                if (NetworkUtil.isValidIp(deviceIp)) {
                    json.put("deviceIp", "rtsp://" + deviceIp);
                }
                else{
                    json.put("deviceIp",deviceIp);
                }
                json.put("password", serverPassword.getText());
            } catch (Exception e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
            String serverIp = serverIpEditText.getText().toString();
            if (checkServerIpEmpty(serverIp)) {
                new NetworkUtil().sendPostRequest(serverIp + "/api/control/delete", json.toString()
                        , (items, statusCode) -> {
                            if (statusCode != 200) {
                                Toast.makeText(MainActivity.this, "Remove device failed", Toast.LENGTH_SHORT).show();
                            } else {
                                itemAdapter.removeSelectedItem();
                            }
                        });
            }
        } else {
            Toast.makeText(MainActivity.this, "Please select device", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkServerIpEmpty(String serverIp) {
        if (!serverIp.isEmpty()) {
            return true;
        } else {
            Toast.makeText(MainActivity.this, "Please enter server IP.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    private void sendCommandToServer(String command) {
        new Thread(() -> {
            try {
                // Send an empty JSON object as per the command
                if (command.equals("START")) {
                    JSONObject json = new JSONObject();
                    String serverIp = serverIpEditText.getText().toString();
                    if (checkServerIpEmpty(serverIp)) {
                        new NetworkUtil().sendPostRequest(serverIp + "/api/control/startServies", json);
                    }
                } else if (command.equals("STOP")) {
                    JSONObject json = new JSONObject();
                    String serverIp = serverIpEditText.getText().toString();
                    if (checkServerIpEmpty(serverIp)) {
                        new NetworkUtil().sendPostRequest(serverIp + "/api/control/stopServies", json);
                    }
                }

            } catch (Exception e) {
                Log.e("MainActivity", "Error sending command to server", e);
            }
        }).start();
    }

    private void addItemToServer(String item) {
//        AtomicBoolean throwError = new AtomicBoolean(false);
//        new Thread(() -> {
//        if (NetworkUtil.isValidIp(item))
//        {
        try {
            JSONObject json = new JSONObject();
            JSONObject faJson = new JSONObject();
            try {
                faJson.put("password", serverPassword.getText());
                json.put("deviceConnection", "rtsp://" + item + ":1935");
                faJson.put("data", json);
            } catch (Exception e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
            String serverIp = serverIpEditText.getText().toString();
            if (checkServerIpEmpty(serverIp)) {
                new NetworkUtil().sendPostRequest(serverIp + "/api/control/save", faJson.toString()
                        , (items, statusCode) -> {
                            if (statusCode != 200) {
                                Toast.makeText(MainActivity.this, "add device failed", Toast.LENGTH_SHORT).show();
                            } else {
                                addItemsToList(item + ":1935");
                            }
                        });
            }

        } catch (Exception e) {
            Log.e("MainActivity", "Error adding item to server", e);
        }
//        }
//        else {
//            throwError.set(true);
//        }
//        }).start();
//        if (throwError.get()) {
//            Toast.makeText(MainActivity.this, "Please enter valid ip", Toast.LENGTH_SHORT).show();
//        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}