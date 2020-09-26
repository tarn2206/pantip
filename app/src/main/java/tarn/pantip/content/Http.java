package tarn.pantip.content;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import tarn.pantip.Pantip;
import tarn.pantip.model.DetailException;

/**
 * User: tarn
 * Date: 1/27/13 2:58 PM
 */
final public class Http
{
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final char[] MULTIPART_CHARS = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.80 Safari/537.36";
    private final String method;
    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, Object> forms = new HashMap<>();
    private final String data;

    private Http(String method, String url, String data)
    {
        this.method = method;
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        //headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip");
        this.data = data;
    }

    public static Http get(String url)
    {
        return new Http(GET, url, null);
    }

    public static Http post(String url)
    {
        return post(url, null);
    }

    public static Http post(String url, String data)
    {
        return new Http(POST, url, data);
    }

    public static Http getAjax(String url)
    {
        return get(url).ajax();
    }

    public static Http postAjax(String url)
    {
        return post(url).ajax();
    }

    public static Http postAjax(String url, String data)
    {
        return post(url, data).ajax();
    }

    public static Document getDocument(String url) throws IOException
    {
        String content = Http.get(url).execute();
        return Jsoup.parse(content);
    }

    private Http ajax()
    {
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Authorization", "Basic dGVzdGVyOnRlc3Rlcg==");
        headers.put("X-Requested-With", "XMLHttpRequest");
        return this;
    }

    public Http header(String name, String value)
    {
        headers.put(name, value);
        return this;
    }

    public Http form(String name, Object value)
    {
        forms.put(name, value);
        return this;
    }

    @NonNull
    public JsonObject executeJson() throws IOException
    {
        String json = execute();
        if (StringUtils.isBlank(json))
        {
            return new JsonObject();
        }
        try
        {
            if (!json.startsWith("{"))
            {
                int i = json.indexOf('{');
                if (i != -1) json = json.substring(i);
            }
            return json.equals("[]") ? new JsonObject()
                    : JsonParser.parseString(json).getAsJsonObject();
        }
        catch (Exception e)
        {
            throw new DetailException(e, url + "\n" + json);
        }
    }

    public <T> T execute(Class<T> classOfT) throws IOException
    {
        return Json.fromJson(execute(), classOfT);
    }

    @NonNull
    public String execute() throws IOException
    {
        if (noNetworkConnection())
        {
            throw new ConnectException("No Internet Connection");
        }

        Log.d("Http", method + " " + url);
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection)new URL(url).openConnection();
            for (String name : headers.keySet())
            {
                connection.setRequestProperty(name, headers.get(name));
            }
            connection.setRequestMethod(method);
            writeFormData(connection);

            int status = connection.getResponseCode();
            Log.d("Http", status + " " + connection.getHeaderField("Content-Type") + " " + connection.getContentLength());

            boolean success = status >= 200 && status < 400;
            String contentEncoding = connection.getHeaderField("Content-Encoding");
            String content;
            try (InputStream in = success ? connection.getInputStream() : connection.getErrorStream())
            {
                InputStream input = "gzip".equalsIgnoreCase(contentEncoding) ? new GZIPInputStream(in) : in;
                content = IOUtils.toString(input, StandardCharsets.UTF_8);
            }
            if (success) return content;

            throw new HttpException(method, url, status, connection.getResponseMessage(), connection.getHeaderFields(), content);
        }
        finally
        {
            if (connection != null) connection.disconnect();
        }
    }

    private void writeFormData(HttpURLConnection connection) throws IOException
    {
        String body = data;
        boolean multipart = isMultipart();
        if (StringUtils.isBlank(body) && !multipart)
        {
            StringBuilder s = new StringBuilder();
            for (String name : forms.keySet())
            {
                if (s.length() > 0) s.append('&');
                s.append(name).append('=');
                Object value = forms.get(name);
                if (value instanceof String) s.append(URLEncoder.encode((String)value, "UTF-8"));
                else s.append(value);
            }
            body = s.toString();
        }

        if (StringUtils.isNotBlank(body))
        {
            try (OutputStream out = doOutput(connection, "application/x-www-form-urlencoded; charset=utf-8"))
            {
                out.write(body.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
        else if (multipart)
        {
            writeMultipart(connection);
        }
    }

    private OutputStream doOutput(HttpURLConnection connection, String contentType) throws IOException
    {
        String method = connection.getRequestMethod();
        connection.setRequestProperty("Content-Type", contentType);
        connection.setDoOutput(true);
        connection.setRequestMethod(method);
        return connection.getOutputStream();
    }

    private boolean isMultipart()
    {
        for (String name : forms.keySet())
        {
            if (forms.get(name) instanceof FilePart)
            {
                return true;
            }
        }
        return false;
    }

    private void writeMultipart(HttpURLConnection connection) throws IOException
    {
        String boundary = generateBoundary();
        try (OutputStream out = doOutput(connection, "multipart/form-data; boundary=" + boundary);
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
        {
            for (String name : forms.keySet())
            {
                Object value = forms.get(name);
                writer.append("--").append(boundary).append("\r\n");
                if (value instanceof FilePart)
                {
                    FilePart filePart = (FilePart)value;
                    writer.append("Content-Disposition: form-data; name=\"").append(name)
                            .append("\"; filename=\"").append(filePart.file.getName()).append("\"\r\n");
                    writer.append("Content-Type: ").append(filePart.mimeType).append("\r\n");
                    writer.append("\r\n");
                    writer.flush();
                    try (FileInputStream in = new FileInputStream(filePart.file))
                    {
                        IOUtils.copy(in, out);
                    }
                    writer.append("\r\n");
                }
                else
                {
                    writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
                    writer.append("Content-Type: text/plain\r\n");
                    writer.append("\r\n").append(String.valueOf(value)).append("\r\n");
                }
            }

            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
            out.flush();
        }
    }

    private static String generateBoundary()
    {
        final StringBuilder builder = new StringBuilder("----WebKitFormBoundary");
        final Random rand = new Random();
        for (int i = 0; i < 16; i++)
        {
            builder.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        return builder.toString();
    }

    static void download(String uri, File file) throws IOException
    {
        if (!file.exists())
        {
            File dir = file.getParentFile();
            if (dir != null) FileUtils.forceMkdir(dir);
        }
        HttpURLConnection connection = (HttpURLConnection)new URL(uri).openConnection();
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        String encoding = connection.getHeaderField("Content-Encoding");
        try (InputStream in = "gzip".equalsIgnoreCase(encoding) ? new GZIPInputStream(connection.getInputStream()) : connection.getInputStream();
             FileOutputStream out = new FileOutputStream(file))
        {
            IOUtils.copy(in, out);
        }
        finally
        {
            connection.disconnect();
        }
    }

    private static boolean noNetworkConnection()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) Pantip.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return true;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) return true;
            return !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        else
        {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo == null || !networkInfo.isConnected();
        }
    }
}
