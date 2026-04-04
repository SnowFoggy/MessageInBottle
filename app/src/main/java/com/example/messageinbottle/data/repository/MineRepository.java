package com.example.messageinbottle.data.repository;

import android.content.Context;

import com.example.messageinbottle.data.model.MineTaskRecord;
import com.example.messageinbottle.data.model.PublishedTask;
import com.example.messageinbottle.data.model.Wallet;
import com.example.messageinbottle.data.remote.ApiResponse;
import com.example.messageinbottle.data.remote.NetworkClient;
import com.example.messageinbottle.data.remote.PublishTaskRequest;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MineRepository {

    public MineRepository(Context context) {
    }

    public Wallet getWallet(long userId) {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().get("/api/wallet?userId=" + userId);
                java.lang.reflect.Type type = new TypeToken<ApiResponse<Wallet>>() { }.getType();
                ApiResponse<Wallet> response = NetworkClient.getInstance().gson().fromJson(json, type);
                return response != null && response.isSuccess() ? response.getData() : null;
            });
        } catch (Exception ignored) {
            return null;
        }
    }

    public void ensureWalletSeed(long userId) {
    }

    public boolean publishTask(long userId, String title, String description, double amount, String deadline) {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().post(
                        "/api/tasks/publish",
                        new PublishTaskRequest(userId, title, "校园", description, amount, deadline)
                );
                java.lang.reflect.Type type = new TypeToken<ApiResponse<Object>>() { }.getType();
                ApiResponse<Object> response = NetworkClient.getInstance().gson().fromJson(json, type);
                return response != null && response.isSuccess();
            });
        } catch (Exception ignored) {
            return false;
        }
    }

    public List<MineTaskRecord> getPublishedTasks(long userId) {
        return mapRecords(fetchPublishedTasks("/api/mine/published?userId=" + userId));
    }

    public List<MineTaskRecord> getCompletedTasks(long userId) {
        return mapRecords(fetchPublishedTasks("/api/mine/completed?userId=" + userId));
    }

    private List<PublishedTask> fetchPublishedTasks(String path) {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().get(path);
                java.lang.reflect.Type type = new TypeToken<ApiResponse<List<PublishedTask>>>() { }.getType();
                ApiResponse<List<PublishedTask>> response = NetworkClient.getInstance().gson().fromJson(json, type);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    return response.getData();
                }
                return new ArrayList<>();
            });
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<MineTaskRecord> mapRecords(List<PublishedTask> tasks) {
        List<MineTaskRecord> records = new ArrayList<>();
        for (PublishedTask task : tasks) {
            records.add(new MineTaskRecord(
                    task.getTitle(),
                    task.getDescription(),
                    task.getAmount(),
                    task.getDeadline(),
                    task.getProgress()
            ));
        }
        return records;
    }

    private <T> T request(Callable<T> callable) throws Exception {
        return NetworkClient.getInstance().executor().submit(callable).get();
    }
}
