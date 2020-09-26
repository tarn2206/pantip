package tarn.pantip.content;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * User: Tarn
 * Date: 8/14/13 9:29 PM
 */
public class ObjectStore
{
    private static final Map<String, Object> map = new HashMap<>();

    public static String put(Object object)
    {
        if (object == null) return null;
        String key = UUID.randomUUID().toString();
        map.put(key, object);
        return key;
    }

    public static Object get(String key)
    {
        return key == null ? null : map.remove(key);
    }
}