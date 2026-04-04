package com.example.messageinbottle.data.remote;

import com.example.messageinbottle.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkClient {

    private static volatile NetworkClient instance;
    private final ExecutorService executorService;
    private final OkHttpClient okHttpClient;
    private final Gson gson;

    private NetworkClient() {
        executorService = Executors.newSingleThreadExecutor();
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .build();
        gson = new GsonBuilder().create();
    }

    public static NetworkClient getInstance() {
        if (instance == null) {
            synchronized (NetworkClient.class) {
                if (instance == null) {
                    instance = new NetworkClient();
                }
            }
        }
        return instance;
    }

    public ExecutorService executor() {
        return executorService;
    }

    public Gson gson() {
        return gson;
    }

    public String get(String path) throws IOException {
        Request request = new Request.Builder()
                .url(BuildConfig.API_BASE_URL + path)
                .get()
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();
        }
    }

    public String post(String path, Object body) throws IOException {
        RequestBody requestBody = RequestBody.create(
                gson.toJson(body),
                MediaType.parse("application/json; charset=utf-8")
        );
        Request request = new Request.Builder()
                .url(BuildConfig.API_BASE_URL + path)
                .post(requestBody)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();
        }
    }
}
