package com.example.messageinbottle.data.repository;

import android.content.Context;

import com.example.messageinbottle.R;
import com.example.messageinbottle.data.local.SessionManager;
import com.example.messageinbottle.data.remote.ApiResponse;
import com.example.messageinbottle.data.remote.AuthPayload;
import com.example.messageinbottle.data.remote.LoginRequest;
import com.example.messageinbottle.data.remote.NetworkClient;
import com.example.messageinbottle.data.remote.RegisterRequest;
import com.google.gson.reflect.TypeToken;

public class AuthRepository {

    public interface AuthResultCallback {
        void onSuccess(String message);

        void onError(String message);
    }

    private final Context appContext;
    private final SessionManager sessionManager;

    public AuthRepository(Context context) {
        appContext = context.getApplicationContext();
        sessionManager = new SessionManager(appContext);
    }

    public void register(String username, String nickname, String password, AuthResultCallback callback) {
        NetworkClient.getInstance().executor().execute(() -> {
            try {
                String json = NetworkClient.getInstance().post(
                        "/api/auth/register",
                        new RegisterRequest(username, nickname, password)
                );
                java.lang.reflect.Type type = new TypeToken<ApiResponse<AuthPayload>>() { }.getType();
                ApiResponse<AuthPayload> response = NetworkClient.getInstance().gson().fromJson(json, type);
                if (response != null && response.isSuccess()) {
                    callback.onSuccess(appContext.getString(R.string.register_success));
                } else {
                    callback.onError(response == null || response.getMessage() == null
                            ? appContext.getString(R.string.error_network)
                            : response.getMessage());
                }
            } catch (Exception exception) {
                callback.onError(exception.getClass().getSimpleName() + ": " + (exception.getMessage() == null ? appContext.getString(R.string.error_network) : exception.getMessage()));
            }
        });
    }

    public void login(String username, String password, AuthResultCallback callback) {
        NetworkClient.getInstance().executor().execute(() -> {
            try {
                String json = NetworkClient.getInstance().post(
                        "/api/auth/login",
                        new LoginRequest(username, password)
                );
                java.lang.reflect.Type type = new TypeToken<ApiResponse<AuthPayload>>() { }.getType();
                ApiResponse<AuthPayload> response = NetworkClient.getInstance().gson().fromJson(json, type);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    AuthPayload payload = response.getData();
                    sessionManager.saveSession(payload.getId(), payload.getUsername(), payload.getNickname());
                    callback.onSuccess(appContext.getString(R.string.login_success));
                } else {
                    callback.onError(response == null || response.getMessage() == null
                            ? appContext.getString(R.string.error_user_not_found)
                            : response.getMessage());
                }
            } catch (Exception exception) {
                callback.onError(exception.getClass().getSimpleName() + ": " + (exception.getMessage() == null ? appContext.getString(R.string.error_network) : exception.getMessage()));
            }
        });
    }

    public void logout() {
        sessionManager.clearSession();
    }
}
