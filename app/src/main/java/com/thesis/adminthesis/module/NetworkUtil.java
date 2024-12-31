package com.thesis.adminthesis.module;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.thesis.adminthesis.model.CommonRes;
import com.thesis.adminthesis.model.Item;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.*;

public class NetworkUtil {
    // Define JSON type
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    // Method to send POST request

    private final String TAG = "NotificationApp";
    private static final String TAGstatic="NotificationApp";
    public void sendPostRequest(String url, JSONObject json) {
        // Create the request body
        RequestBody body = RequestBody.create(json.toString(), JSON);

        // Build the request
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // Send the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle failure
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Handle the response
                    String responseData = response.body().string();
                    Log.i(TAG,"Response: " + responseData);
                } else {
                    Log.e(TAG,"Request failed: " + response.code());
                }
            }
        });
    }

    // Interface to handle the response
    public interface ResponseCallback {
        void onResponse(List<Item> items, int statusCode);
    }// Sends the POST request and returns the raw response as a string

    private static CommonRes sendRequest(String urlString, String jsonBody) {
        CommonRes response = new CommonRes();

        OkHttpClient client = new OkHttpClient();

        // Create the request body
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        // Create the POST request
        Request request = new Request.Builder()
                .url(urlString)
                .post(body)
                .build();

        try {
            // Send the request and get the response
            Response responseObj = client.newCall(request).execute();
            response.setStatusCode(responseObj.code());
            if (responseObj.isSuccessful()) {
                // Get the response body as a string
                response.setResponseBody(responseObj.body().string());
            }
        } catch (IOException e) {
            Log.e(TAGstatic, Objects.requireNonNull(e.getMessage()));
        }

        return response;
    }

    // Method to send POST request with JSON body and get the JSON array response
    // Parse the JSON response and return a list of items
    private static List<Item> parseJsonResponse(String response) {
        List<Item> itemList = new ArrayList<>();

        if (response != null && !response.isEmpty()) {
            try {
                // Parse the JSON response (Assume it's a JSON array)
                JSONArray jsonArray = new JSONArray(response);

                // Iterate through the array and extract the data
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject itemJson = jsonArray.getJSONObject(i);

                    // Example of extracting data, assuming each item has a "name" and "description"
                    String deviceConnection = itemJson.getString("sourcePlace");

                    // Create an Item object and add it to the list
                    itemList.add(new Item(deviceConnection));
                }
            } catch (JSONException e) {
                Log.e(TAGstatic, Objects.requireNonNull(e.getMessage()));
            }
        }

        return itemList;
    }

    public  void sendPostRequest(String urlString, String jsonBody, ResponseCallback callback) {
        // Run the network request on a background thread
        new Thread(() -> {
            CommonRes response = sendRequest(urlString, jsonBody);
            if (callback != null) {
                // Parse the JSON response on the background thread
                List<Item> itemList = parseJsonResponse(response.getResponseBody());

                // After parsing, send the result back to the main thread (UI thread)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(itemList,response.getStatusCode());
                    }
                });
            }
        }).start();
    }

    public static String getIpAndPort(String url) {
        Pattern pattern = Pattern.compile("^(rtsp)://([0-9a-zA-Z\\.:]+):(\\d+)$");
        Matcher matcher = pattern.matcher(url);
        matcher.matches();
        String ip = matcher.group(2);   // IP part
        String portStr = matcher.group(3); // Port part
        return ip + ":" + portStr;
    }

    public static boolean isValidIpAndPortUrl(String url) {
        // Updated regex to allow both http and https
        Pattern pattern = Pattern.compile("^(http|https)://([0-9a-zA-Z\\.:]+):(\\d+)$");
        Matcher matcher = pattern.matcher(url);

        if (matcher.matches()) {
            String ip = matcher.group(2);   // IP part
            String portStr = matcher.group(3); // Port part

            // Validate IP and port
            return isValidIp(ip) && isValidPort(portStr);
        }
        return false; // URL format is not valid
    }// Validate if the IP is valid (either IPv4 or IPv6)

    public static boolean isValidIp(String ip) {// Regular expression to validate IPv4 and IPv6 addresses
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        String ipv6Pattern = "([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}"; // Basic validation for IPv6

        Pattern ipv4Regex = Pattern.compile(ipv4Pattern);
        Pattern ipv6Regex = Pattern.compile(ipv6Pattern);

        return ipv4Regex.matcher(ip).matches() || ipv6Regex.matcher(ip).matches();
    }

    // Validate if the port is valid (must be between 0 and 65535)
    private static boolean isValidPort(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            return port >= 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
