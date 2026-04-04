package com.example.messageinbottle.data.repository;

import android.content.Context;

import com.example.messageinbottle.data.model.AcceptedTask;
import com.example.messageinbottle.data.model.HomeTask;
import com.example.messageinbottle.data.remote.AcceptTaskRequest;
import com.example.messageinbottle.data.remote.ApiResponse;
import com.example.messageinbottle.data.remote.NetworkClient;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class StageTwoRepository {

    public StageTwoRepository(Context context) {
    }

    public List<HomeTask> getHomeTasks() {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().get("/api/home/tasks");
                java.lang.reflect.Type type = new TypeToken<ApiResponse<List<HomeTask>>>() { }.getType();
                ApiResponse<List<HomeTask>> response = NetworkClient.getInstance().gson().fromJson(json, type);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    return response.getData();
                }
                return new ArrayList<>();
            });
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public List<HomeTask> searchHomeTasks(String keyword, String category) {
        List<HomeTask> source = getHomeTasks();
        List<HomeTask> result = new ArrayList<>();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedCategory = category == null ? "全部" : category;

        for (HomeTask task : source) {
            boolean keywordMatched = normalizedKeyword.isEmpty()
                    || task.getTitle().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                    || task.getDescription().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                    || task.getPublisher().toLowerCase(Locale.ROOT).contains(normalizedKeyword);

            boolean categoryMatched = "全部".equals(normalizedCategory)
                    || task.getCategory().equals(normalizedCategory);

            if (keywordMatched && categoryMatched) {
                result.add(task);
            }
        }
        return result;
    }

    public List<String> getHomeCategories() {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().get("/api/home/categories");
                java.lang.reflect.Type type = new TypeToken<ApiResponse<List<String>>>() { }.getType();
                ApiResponse<List<String>> response = NetworkClient.getInstance().gson().fromJson(json, type);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    return response.getData();
                }
                return List.of("全部", "校园", "跑腿", "学习", "生活");
            });
        } catch (Exception ignored) {
            return List.of("全部", "校园", "跑腿", "学习", "生活");
        }
    }

    public List<AcceptedTask> getAcceptedTasks(long userId) {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().get("/api/mine/accepted?userId=" + userId);
                java.lang.reflect.Type type = new TypeToken<ApiResponse<List<AcceptedTask>>>() { }.getType();
                ApiResponse<List<AcceptedTask>> response = NetworkClient.getInstance().gson().fromJson(json, type);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    return response.getData();
                }
                return new ArrayList<>();
            });
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public AcceptedTask acceptTask(long taskId, long userId) {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().post("/api/tasks/" + taskId + "/accept", new AcceptTaskRequest(userId));
                java.lang.reflect.Type type = new TypeToken<ApiResponse<AcceptedTask>>() { }.getType();
                ApiResponse<AcceptedTask> response = NetworkClient.getInstance().gson().fromJson(json, type);
                return response != null && response.isSuccess() ? response.getData() : null;
            });
        } catch (Exception ignored) {
            return null;
        }
    }

    public AcceptedTask completeTask(long taskId, long userId) {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().post("/api/tasks/" + taskId + "/complete", new AcceptTaskRequest(userId));
                java.lang.reflect.Type type = new TypeToken<ApiResponse<AcceptedTask>>() { }.getType();
                ApiResponse<AcceptedTask> response = NetworkClient.getInstance().gson().fromJson(json, type);
                return response != null && response.isSuccess() ? response.getData() : null;
            });
        } catch (Exception ignored) {
            return null;
        }
    }

    private <T> T request(Callable<T> callable) throws Exception {
        return NetworkClient.getInstance().executor().submit(callable).get();
    }
}
