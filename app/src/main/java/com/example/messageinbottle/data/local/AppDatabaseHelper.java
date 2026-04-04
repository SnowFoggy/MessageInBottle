package com.example.messageinbottle.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "message_in_bottle.db";
    private static final int DATABASE_VERSION = 1;

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(buildUserTableSql());
        db.execSQL(buildTaskTableSql());
        db.execSQL(buildWalletTableSql());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.WalletTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.TaskTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.UserTable.TABLE_NAME);
        onCreate(db);
    }

    private String buildUserTableSql() {
        return "CREATE TABLE " + DbContract.UserTable.TABLE_NAME + " ("
                + DbContract.UserTable.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DbContract.UserTable.COLUMN_USERNAME + " TEXT UNIQUE NOT NULL,"
                + DbContract.UserTable.COLUMN_NICKNAME + " TEXT NOT NULL,"
                + DbContract.UserTable.COLUMN_PASSWORD + " TEXT NOT NULL,"
                + DbContract.UserTable.COLUMN_CREATED_AT + " INTEGER NOT NULL"
                + ")";
    }

    private String buildTaskTableSql() {
        return "CREATE TABLE " + DbContract.TaskTable.TABLE_NAME + " ("
                + DbContract.TaskTable.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DbContract.TaskTable.COLUMN_PUBLISHER_ID + " INTEGER NOT NULL,"
                + DbContract.TaskTable.COLUMN_TITLE + " TEXT NOT NULL,"
                + DbContract.TaskTable.COLUMN_DESCRIPTION + " TEXT NOT NULL,"
                + DbContract.TaskTable.COLUMN_CATEGORY + " TEXT,"
                + DbContract.TaskTable.COLUMN_REWARD + " REAL NOT NULL DEFAULT 0,"
                + DbContract.TaskTable.COLUMN_DEADLINE + " INTEGER NOT NULL,"
                + DbContract.TaskTable.COLUMN_STATUS + " TEXT NOT NULL,"
                + DbContract.TaskTable.COLUMN_CREATED_AT + " INTEGER NOT NULL"
                + ")";
    }

    private String buildWalletTableSql() {
        return "CREATE TABLE " + DbContract.WalletTable.TABLE_NAME + " ("
                + DbContract.WalletTable.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DbContract.WalletTable.COLUMN_USER_ID + " INTEGER UNIQUE NOT NULL,"
                + DbContract.WalletTable.COLUMN_BALANCE + " REAL NOT NULL DEFAULT 0,"
                + DbContract.WalletTable.COLUMN_UPDATED_AT + " INTEGER NOT NULL"
                + ")";
    }
}



