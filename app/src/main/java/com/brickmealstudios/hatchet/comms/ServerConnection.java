package com.brickmealstudios.hatchet.comms;


import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.brickmealstudios.hatchet.types.Telemetry;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;


public class ServerConnection {
    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTED
    }

    public interface ServerListener {
        void onNewMessage(String message);
        void onStatusChange(ConnectionStatus status);
    }

    private WebSocket mWebSocket;
    private OkHttpClient mClient;
    private String mServerUrl;
    private Handler mMessageHandler;
    private Handler mStatusHandler;
    private ServerListener mListener;


    private class SocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Message m = mStatusHandler.obtainMessage(0, ConnectionStatus.CONNECTED);
            Log.d("ServerConnection", "Connection open");
            mStatusHandler.sendMessage(m);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Message m = mMessageHandler.obtainMessage(0, text);
            Log.d("ServerConnection", "Message received");
            mMessageHandler.sendMessage(m);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Message m = mStatusHandler.obtainMessage(0, ConnectionStatus.DISCONNECTED);
            Log.d("ServerConnection", "Connection closed");
            mStatusHandler.sendMessage(m);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.d("ServerConnection", "Connect failed");
            disconnect();
        }
    }

    public ServerConnection(String url) {
        mClient = new OkHttpClient.Builder()
                .readTimeout(3,  TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        mServerUrl = url;
    }

    public void connect(ServerListener listener) {
        Request request = new Request.Builder()
                .url(mServerUrl)
                .build();
        mWebSocket = mClient.newWebSocket(request, new SocketListener());
        mListener = listener;
        mMessageHandler = new Handler(msg -> {mListener.onNewMessage((String) msg.obj);
            return true;});
        mStatusHandler = new Handler(msg -> { mListener.onStatusChange((ConnectionStatus) msg.obj);
            return true;});
    }

    public void disconnect() {
        mWebSocket.cancel();
        mListener = null;
        mMessageHandler.removeCallbacksAndMessages(null);
        mStatusHandler.removeCallbacksAndMessages(null);
    }

    public void sendMessage(String message) {
        Log.d("ServerConnection", "Sending message");
        mWebSocket.send(message);
    }

    public void sendTelemetry(Location loc) {
        Gson gson = new Gson();
        Telemetry t = new Telemetry(loc);
        String message = gson.toJson(t);
        Log.d("ServerConnection", "Sending telemetry: " + message);
        mWebSocket.send(message);
    }
}
