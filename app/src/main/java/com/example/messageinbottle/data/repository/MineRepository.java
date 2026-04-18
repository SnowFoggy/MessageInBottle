package com.example.messageinbottle.data.repository;

import android.content.Context;

import com.example.messageinbottle.data.model.MineTaskRecord;
import com.example.messageinbottle.data.model.PublishedTask;
import com.example.messageinbottle.data.model.Wallet;
import com.example.messageinbottle.data.remote.AcceptTaskRequest;
import com.example.messageinbottle.data.remote.ApiResponse;
import com.example.messageinbottle.data.remote.NetworkClient;
import com.example.messageinbottle.data.remote.PublishTaskRequest;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MineRepository {

    public static class ActionResult {
        private final boolean success;
        private final String message;

        public ActionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

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

    public ActionResult publishTask(long userId, String title, String description, double amount, String deadline,
                                    String category, java.io.File imageFile) {
        try {
            return request(() -> {
                String imageUrl = null;
                if (imageFile != null) {
                    java.util.Map<String, String> formData = new java.util.HashMap<>();
                    formData.put("userId", String.valueOf(userId));
                    String uploadJson = NetworkClient.getInstance().postMultipart(
                            "/api/tasks/publish/image",
                            formData,
                            "taskImage",
                            imageFile,
                            "image/jpeg"
                    );
                    java.lang.reflect.Type uploadType = new TypeToken<ApiResponse<ImageUploadResponse>>() { }.getType();
                    ApiResponse<ImageUploadResponse> uploadResponse = NetworkClient.getInstance().gson().fromJson(uploadJson, uploadType);
                    if (uploadResponse == null || !uploadResponse.isSuccess() || uploadResponse.getData() == null) {
                        return new ActionResult(false, uploadResponse == null ? "任务图片上传失败" : uploadResponse.getMessage());
                    }
                    imageUrl = uploadResponse.getData().getFileUrl();
                }

                String json = NetworkClient.getInstance().post(
                        "/api/tasks/publish",
                        new PublishTaskRequest(userId, title, category, description, amount, deadline, imageUrl)
                );
                java.lang.reflect.Type type = new TypeToken<ApiResponse<Object>>() { }.getType();
                ApiResponse<Object> response = NetworkClient.getInstance().gson().fromJson(json, type);
                if (response == null) {
                    return new ActionResult(false, "发布失败");
                }
                return new ActionResult(response.isSuccess(), response.getMessage());
            });
        } catch (Exception ignored) {
            return new ActionResult(false, "后端请求失败，请检查服务和地址配置");
        }
    }

    public ActionResult cancelTask(long taskId, long userId) {
        try {
            return request(() -> {
                String json = NetworkClient.getInstance().post("/api/tasks/" + taskId + "/cancel", new AcceptTaskRequest(userId));
                java.lang.reflect.Type type = new TypeToken<ApiResponse<PublishedTask>>() { }.getType();
                ApiResponse<PublishedTask> response = NetworkClient.getInstance().gson().fromJson(json, type);
                if (response == null) {
                    return new ActionResult(false, "取消失败");
                }
                return new ActionResult(response.isSuccess(), response.getMessage());
            });
        } catch (Exception ignored) {
            return new ActionResult(false, "后端请求失败，请检查服务和地址配置");
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
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getAmount(),
                    task.getDeadline(),
                    task.getProgress(),
                    task.getTaskImageUrl(),
                    task.getCompletionProofUrl()
            ));
        }
        return records;
    }

    private <T> T request(Callable<T> callable) throws Exception {
        return NetworkClient.getInstance().executor().submit(callable).get();
    }
}
