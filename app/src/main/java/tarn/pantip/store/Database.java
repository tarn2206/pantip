package tarn.pantip.store;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

class Database
{
    private final DatabaseHelper helper;

    Database(Context context)
    {
        helper = new DatabaseHelper(context);
    }

    SQLiteDatabase beginTransaction()
    {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        return db;
    }

    SQLiteDatabase getReadableDatabase()
    {
        return helper.getReadableDatabase();
    }
}