package tarn.pantip.content;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tarn.pantip.L;
import tarn.pantip.model.DetailException;

/**
 * Created by Tarn on 23/10/2015.
 */
public class Json
{
    private static final Gson gson = new Gson();

    public static <T> T fromJson(String json, Class<T> classOfT)
    {
        if (StringUtils.isBlank(json)) return null;
        try
        {
            return gson.fromJson(json, classOfT);
        }
        catch (RuntimeException e)
        {
            L.e(e, json);
            return null;
        }
    }

    public static <T> T[] toArray(String json, Class<T[]> clazz)
    {
        return gson.fromJson(json, clazz);
    }

    public static <T> List<T> toList(String json, Class<T[]> clazz)
    {
        try
        {
            T[] items = gson.fromJson(json, clazz);
            List<T> list = new ArrayList<>(items == null ? 0 : items.length);
            if (items != null)
            {
                Collections.addAll(list, items);
            }
            return list;
        }
        catch (RuntimeException e)
        {
            L.e(e);
            return null;
        }
    }

    static <T> List<T> toList(File file, Class<T[]> clazz)
    {
        try (FileReader reader = new FileReader(file))
        {
            T[] items = gson.fromJson(new BufferedReader(reader), clazz);
            List<T> list = new ArrayList<>(items == null ? 0 : items.length);
            if (items != null)
            {
                Collections.addAll(list, items);
            }
            return list;
        }
        catch (Exception e)
        {
            L.e(e);
            return null;
        }
    }

    public static String toJson(Object object)
    {
        try
        {
            return gson.toJson(object);
        }
        catch (RuntimeException e)
        {
            L.e(e);
            return null;
        }
    }

    public static <T> T fromFile(File file, Class<T> clazz)
    {
        if (!file.exists()) return null;
        try (FileReader reader = new FileReader(file))
        {
            //L.d("read %s %d", file.getAbsolutePath(), file.length());
            return gson.fromJson(new BufferedReader(reader), clazz);
            //return objectMapper.readValue(new BufferedReader(reader), clazz);
        }
        catch (Exception e)
        {
            try
            {
                L.e(new DetailException(e, FileUtils.readFileToString(file, "utf-8")));
            }
            catch (IOException e1)
            {
                L.e(e1);
            }
            return null;
        }
    }

    public synchronized static void toFile(Object object, Type type, File file) throws IOException
    {
        File parent = file.getParentFile();
        if (parent != null) FileUtils.forceMkdir(parent);
        try (FileWriter fw = new FileWriter(file))
        {
            BufferedWriter bw = new BufferedWriter(fw);
            gson.toJson(object, type, new JsonWriter(bw));
            bw.flush();
        }
    }

    private static JsonElement get(JsonObject o, String names)
    {
        String[] a = names.split("\\.");
        JsonElement e = o;
        for (int i = 0; i < a.length; i++)
        {
            e = ((JsonObject)e).get(a[i]);
            if (e == null) return null;
            if (i == a.length - 1) return e;
            if (!e.isJsonObject()) return null;
        }
        return null;
    }

    public static long getAsLong(JsonObject o, String names)
    {
        JsonElement e = get(o, names);
        return e == null ? 0 : e.getAsLong();
    }

    public static String getAsString(JsonObject o, String names)
    {
        JsonElement e = get(o, names);
        return e == null ? null : e.getAsString();
    }
}