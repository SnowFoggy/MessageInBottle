package com.example.messageinbottle.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "message_in_bottle_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NICKNAME = "nickname";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(long userId, String username, String nickname) {
        preferences.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_NICKNAME, nickname)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.contains(KEY_USER_ID);
    }

    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, -1L);
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, "");
    }

    public String getNickname() {
        return preferences.getString(KEY_NICKNAME, "");
    }

    public String getDisplayName() {
        String nickname = getNickname();
        return nickname == null || nickname.isEmpty() ? getUsername() : nickname;
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}

