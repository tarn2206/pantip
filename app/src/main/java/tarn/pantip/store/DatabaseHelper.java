package tarn.pantip.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;

import tarn.pantip.L;
import tarn.pantip.Pantip;

/**
 * Created by Tarn on 29-Sep-14.
 */
class DatabaseHelper extends SQLiteOpenHelper
{
    private final Context context;

    DatabaseHelper(Context context)
    {
        super(context, "pantip.db", null, 6);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        createV1(db);
        createV2(db);
        createV4(db);
        upgradeRoom(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        try
        {
            switch (oldVersion)
            {
                case 1: createV2(db);
                case 3: createV4(db);
                default: upgradeRoom(db);
            }
        }
        catch (RuntimeException e)
        {
            wipeDatabase(db);
            onCreate(db);
        }
    }

    private void createV1(SQLiteDatabase db)
    {
        db.execSQL("create table if not exists forums(icon int not null,label text not null,url text not null,description text,hit_count int not null)");
        db.execSQL("create table if not exists tags(forum text not null,'no' int not null,label text not null,url text not null,sub_tag int not null,hit_count int not null)");
    }

    private void createV2(SQLiteDatabase db)
    {
        db.execSQL("create table if not exists read_topic(topic_id int not null,hit_count int not null,last_update text not null)");
    }

    private void createV4(SQLiteDatabase db)
    {
        db.execSQL("create table if not exists topics(topic_id int not null,data text not null,last_access int not null)");
        db.execSQL("create table if not exists comments(topic_id int not null,comment_no int not null,data text not null)");
    }

    private void upgradeRoom(SQLiteDatabase db)
    {
        List<String> forums;
        try
        {
            forums = IOUtils.readLines(context.getAssets().open("forum.txt"), "UTF-8");
        }
        catch (IOException e)
        {
            Pantip.handleException(e);
            return;
        }
        for (int i = 0; i < forums.size(); i++)
        {
            String s = forums.get(i);
            int a = s.indexOf(',');
            int b = s.indexOf(',', a + 1);
            String url = s.substring(0, a);
            String label = s.substring(a + 1, b);
            String desc = s.substring(b + 1);
            ContentValues values = new ContentValues();
            values.put("icon", i);
            values.put("url", url);
            values.put("label", label);
            values.put("description", desc);

            Cursor cursor = db.rawQuery("select count(*) from forums where label=?", new String[] { label });
            boolean found = cursor.moveToFirst() && cursor.getInt(0) > 0;
            cursor.close();
            if (found)
            {
                db.update("forums", values, "label=?", new String[] { label });
                continue;
            }
            cursor = db.rawQuery("select count(*) from forums where url=?", new String[] { url });
            found = cursor.moveToFirst() && cursor.getInt(0) > 0;
            cursor.close();
            if (found)
            {
                db.update("forums", values, "url=?", new String[] { url });
                continue;
            }

            values.put("hit_count", 0);
            db.insert("forums", null, values);
        }
    }

    private void wipeDatabase(SQLiteDatabase db)
    {
        try (Cursor cursor = db.query("sqlite_master", new String[] { "type", "name" }, null, null, null, null, null))
        {
            while (cursor.moveToNext())
            {
                String type = cursor.getString(0);
                String name = cursor.getString(1);
                if ("sqlite_sequence".equals(name)) continue;

                String sql = "DROP " + type + " IF EXISTS " + name;
                try
                {
                    db.execSQL(sql);
                }
                catch (SQLException e)
                {
                    L.e(e, "Error executing " + sql);
                }
            }
        }
    }
}