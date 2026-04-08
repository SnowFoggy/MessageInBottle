package com.example.messageinbottle.data.repository;

import android.content.Context;

import com.example.messageinbottle.data.model.MessageItem;
import com.example.messageinbottle.data.remote.ApiResponse;
import com.example.messageinbottle.data.remote.NetworkClient;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MessageRepository {

    public MessageRepository(Context context) {
    }

    public List<MessageItem> getMessages(long userId) {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().get("/api/messages?userId=" + userId);
                java.lang.reflect.Type type = new TypeToken<ApiResponse<List<MessageItem>>>() { }.getType();
                ApiResponse<List<MessageItem>> response = NetworkClient.getInstance().gson().fromJson(json, type);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    return response.getData();
                }
                return new ArrayList<>();
            });
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private <T> T request(Callable<T> callable) throws Exception {
        return NetworkClient.getInstance().executor().submit(callable).get();
    }
}

