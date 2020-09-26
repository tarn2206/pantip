package tarn.pantip.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import tarn.pantip.L;

/**
 * User: Tarn
 * Date: 5/18/13 1:40 PM
 */
public class PersistentCookieStore implements CookieStore
{
    private static final String COOKIE_NAME_STORE = "names";
    private static final String COOKIE_NAME_PREFIX = "cookie_";
    private final SharedPreferences preferences;
    private final CookieStore store;

    public PersistentCookieStore(Context context)
    {
        preferences = context.getSharedPreferences("cookies.bin", Context.MODE_PRIVATE);
        store = new CookieManager().getCookieStore();
        loadCookies();
    }

    private void loadCookies()
    {
        String s = preferences.getString(COOKIE_NAME_STORE, null);
        if (s == null) return;

        boolean edit = false;
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder names = new StringBuilder();
        String[] nameArray = TextUtils.split(s, ",");
        for (String name : nameArray)
        {
            String value = preferences.getString(COOKIE_NAME_PREFIX + name, null);
            if (value == null) continue;

            HttpCookie cookie = decode(value);
            if (cookie == null) continue;

            if (cookie.hasExpired())
            {
                editor.remove(COOKIE_NAME_PREFIX + name);
                edit = true;
            }
            else
            {
                store.add(null, cookie);
                if (names.length() > 0) names.append(',');
                names.append(cookie.getName());
            }
        }
        if (edit)
        {
            editor.putString(COOKIE_NAME_STORE, names.toString());
            editor.apply();
        }
    }

    public void save()
    {
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder names = new StringBuilder();
        for (HttpCookie cookie : store.getCookies())
        {
            if (cookie.hasExpired()) continue;
            if (names.length() > 0) names.append(',');
            names.append(cookie.getName());

            String value = encode(cookie);
            editor.putString(COOKIE_NAME_PREFIX + cookie.getName(), value);
        }
        editor.putString(COOKIE_NAME_STORE, names.toString()).apply();
    }

    @Override
    public void add(URI uri, HttpCookie cookie)
    {
        store.add(uri, cookie);
    }

    @Override
    public List<HttpCookie> get(URI uri)
    {
        return store.get(uri);
    }

    public List<HttpCookie> getCookies()
    {
        return store.getCookies();
    }

    @Override
    public List<URI> getURIs()
    {
        return store.getURIs();
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie)
    {
        return store.remove(uri, cookie);
    }

    @Override
    public boolean removeAll()
    {
        return store.removeAll();
    }

    private String encode(HttpCookie cookie)
    {
        try
        {
            return cookie.getName() + "=" + URLEncoder.encode(cookie.getValue(), "UTF-8") + ";"
                   + URLEncoder.encode(cookie.getDomain(), "UTF-8") + ";"
                   + URLEncoder.encode(cookie.getPath(), "UTF-8") + ";"
                   + cookie.getMaxAge() + ";"
                   + cookie.getSecure() + ";"
                   + cookie.getVersion() + ";";
        }
        catch (UnsupportedEncodingException e)
        {
            return null;
        }
    }

    private HttpCookie decode(String s)
    {
        try
        {
            String[] a = s.split("=");
            String name = a[0];
            a = a[1].split(";");
            String value = URLDecoder.decode(a[0], "UTF-8");
            HttpCookie cookie = new HttpCookie(name, value);
            cookie.setDomain(URLDecoder.decode(a[1], "UTF-8"));
            cookie.setPath(URLDecoder.decode(a[2], "UTF-8"));
            cookie.setMaxAge(Long.parseLong(URLDecoder.decode(a[3], "UTF-8")));
            cookie.setSecure(Boolean.parseBoolean(URLDecoder.decode(a[4], "UTF-8")));
            cookie.setVersion(Integer.parseInt(URLDecoder.decode(a[5], "UTF-8")));
            return cookie;
        }
        catch (Exception e)
        {
            L.e(e);
            return null;
        }
    }
}