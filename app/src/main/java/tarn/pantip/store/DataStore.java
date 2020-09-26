package tarn.pantip.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.model.ForumMenuItem;
import tarn.pantip.model.MenuItemType;
import tarn.pantip.model.TagMenuItem;
import tarn.pantip.util.RxUtils;

/**
 * Created by Tarn on 29-Sep-14.
 */
public class DataStore
{
    private final Database database;

    public DataStore(Context context)
    {
        database = new Database(context);
    }

    public Observable<List<ForumMenuItem>> listForums()
    {
        return RxUtils.observe(() -> {
            SQLiteDatabase db = database.getReadableDatabase();
            try (Cursor cursor = db.rawQuery("select icon,label,url,description,hit_count from forums order by hit_count desc,icon", null))
            {
                List<ForumMenuItem> list = new ArrayList<>();

                if (!cursor.moveToFirst()) return list;
                do
                {
                    ForumMenuItem item = new ForumMenuItem(MenuItemType.Item, cursor.getString(1));
                    item.iconId = cursor.getInt(0);
                    item.url = cursor.getString(2);
                    item.description = cursor.getString(3);
                    item.hitCount = cursor.getInt(4);
                    list.add(item);
                } while (cursor.moveToNext());
                return list;
            }
        });
    }

    public String getForumLabel(String value)
    {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("select label from forums where url=?", new String[] { value }))
        {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    public String getTagLabel(String url)
    {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("select label from tags where url=?", new String[] { url }))
        {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    public List<TagMenuItem> loadFavoriteTags()
    {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("select forum,label,url,[no],sub_tag,hit_count from tags where hit_count>0 order by hit_count desc,label", null))
        {
            List<TagMenuItem> list = new ArrayList<>();
            if (!cursor.moveToFirst()) return list;
            do
            {
                TagMenuItem item = new TagMenuItem(cursor.getString(0), cursor.getString(1), cursor.getString(2));
                item.no = cursor.getInt(3);
                item.isSubTag = false;//cursor.getInt(4) == 1;
                item.hitCount = cursor.getInt(5);
                list.add(item);
            } while (cursor.moveToNext());
            return list;
        }
    }

    public void increaseForumFavorite(String label)
    {
        SQLiteDatabase db = database.beginTransaction();
        try
        {
            db.execSQL("update forums set hit_count=hit_count+1 where label=?", new String[] { label });
            db.setTransactionSuccessful();
        }
        finally
        {
            db.endTransaction();
        }
    }

    public void resetForumFavorite(String label)
    {
        SQLiteDatabase db = database.beginTransaction();
        try
        {
            db.execSQL("update forums set hit_count=0 where label=?", new String[] { label });
            db.setTransactionSuccessful();
        }
        finally
        {
            db.endTransaction();
        }
    }

    public void updateTagFavorite(String forum, String label, String url, boolean increase)
    {
        SQLiteDatabase db = database.beginTransaction();
        try (Cursor cursor = db.rawQuery("select hit_count from tags where url=?", new String[] { url }))
        {
            if (cursor.moveToFirst())
            {
                String value = increase ? "hit_count+1" : "0";
                db.execSQL("update tags set hit_count=" + value + ",label=? where url=?", new String[] { label, url });
                if (StringUtils.isNotBlank(forum))
                {
                    db.execSQL("update tags set forum=? where url=? and forum is null", new String[] { forum, url });
                }
            }
            else
            {
                if (forum == null) forum = "";
                ContentValues values = new ContentValues();
                values.put("forum", forum);
                values.put("label", label);
                values.put("url", url);
                values.put("no", 0);
                values.put("sub_tag", 0);
                values.put("hit_count", 1);
                db.insert("tags", null, values);
            }
            db.setTransactionSuccessful();
        }
        finally
        {
            db.endTransaction();
        }
        //printTable("tags", "hit_count>0");
    }

    private void printTable(String name, String where)
    {
        SQLiteDatabase db = database.getReadableDatabase();
        String sql = "select * from " + name;
        if (where != null && where.length() > 0) sql += " where " + where;

        try (Cursor cursor = db.rawQuery(sql, new String[0]))
        {
            if (cursor.moveToFirst())
            {
                int count = cursor.getColumnCount();
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < count; i++)
                {
                    if (i > 0) s.append(", ");
                    s.append(cursor.getColumnName(i));
                }
                L.d(s);
                int n = 1;
                do
                {
                    s = new StringBuilder();
                    for (int i = 0; i < count; i++)
                    {
                        if (i > 0) s.append(", ");
                        s.append(cursor.getString(i));
                    }
                    L.d("Row %d : %s", n++, s.toString());
                } while (cursor.moveToNext());
            }
            else L.d("%s no record", name);
        }
    }

    public void markAsRead(long topicId)
    {
        int count = readCount(topicId);

        SQLiteDatabase db = database.beginTransaction();
        try
        {
            ContentValues values = new ContentValues();
            values.put("hit_count", count + 1);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            String date = format.format(new Date());
            values.put("last_update", date);
            int rowsAffected = db.update("read_topic", values, "topic_id=?", new String[] { String.valueOf(topicId) });
            if (rowsAffected == 0)
            {
                values.put("topic_id", topicId);
                db.insert("read_topic", null, values);
            }
            db.setTransactionSuccessful();
        }
        finally
        {
            db.endTransaction();
        }
    }

    public int readCount(long topicId)
    {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("select hit_count from read_topic where topic_id=?", new String[] { String.valueOf(topicId) }))
        {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }
}