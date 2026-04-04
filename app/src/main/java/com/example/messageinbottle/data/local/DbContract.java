package com.example.messageinbottle.data.local;

public final class DbContract {

    private DbContract() {
    }

    public static final class UserTable {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_USERNAME = "username";
        public static final String COLUMN_NICKNAME = "nickname";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_CREATED_AT = "created_at";
    }

    public static final class TaskTable {
        public static final String TABLE_NAME = "tasks";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_PUBLISHER_ID = "publisher_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_REWARD = "reward";
        public static final String COLUMN_DEADLINE = "deadline";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_CREATED_AT = "created_at";
    }

    public static final class WalletTable {
        public static final String TABLE_NAME = "wallets";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_BALANCE = "balance";
        public static final String COLUMN_UPDATED_AT = "updated_at";
    }
}
