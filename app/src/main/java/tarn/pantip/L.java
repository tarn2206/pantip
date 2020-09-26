package tarn.pantip;

import android.util.Log;

import com.android.volley.ServerError;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import tarn.pantip.content.HttpException;
import tarn.pantip.content.PantipException;
import tarn.pantip.model.DetailException;

public final class L
{
    public static final String TAG = "Pantip";
    private static final String THIS_CLASS_NAME = L.class.getName();

    public static void d(Object obj)
    {
        log(Log.DEBUG, obj == null ? "null" : obj.toString());
    }

    public static void d(String message, Object... args)
    {
        log(Log.DEBUG, message, args);
    }

    public static void i(Object obj)
    {
        log(Log.INFO, obj == null ? "null" : obj.toString());
    }

    public static void i(String message, Object... args)
    {
        log(Log.INFO, message, args);
    }

    public static void w(String message, Object... args)
    {
        log(Log.WARN, message, args);
    }

    public static void e(String message, Object... args)
    {
        log(Log.ERROR, message, args);
    }

    private static void log(int priority, String message, Object... args)
    {
        if (!BuildConfig.DEBUG) return;
        if (message != null && ArrayUtils.isNotEmpty(args)) message = String.format(message, args);

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        boolean foundLog = false;
        for (StackTraceElement e : stackTraceElements)
        {
            String className = e.getClassName();
            if (className.equals(THIS_CLASS_NAME))
            {
                foundLog = true;
                continue;
            }
            if (foundLog)
            {
                message = String.format("%s at %s", message, e);
                break;
            }
        }
        if (message != null)
        {
            Log.println(priority, TAG, message);
        }
    }

    public static void e(Throwable tr)
    {
        e(tr, null);
    }

    public static void e(Throwable tr, String msg, Object... args)
    {
        if (BuildConfig.DEBUG)
        {
            if (tr instanceof DetailException)
            {
                if (tr.getMessage() != null)
                {
                    Log.e(TAG, tr.getMessage());
                }
                if (tr.getCause() != null)
                {
                    tr = tr.getCause();
                }
            }
            if (StringUtils.isBlank(msg))
            {
                msg = tr.getMessage();
            }
            else
            {
                if (ArrayUtils.isNotEmpty(args)) msg = String.format(msg, args);
                msg += "\n" + tr.getMessage();
            }
            msg += "\n" + Log.getStackTraceString(tr);
            Log.e(TAG, msg);
        }
        else
        {
            try
            {
                if (!(tr instanceof PantipException))
                {
                    logException(tr);
                }
            }
            catch (Exception e)
            {/*ignored*/}
        }
    }

    private static boolean logException(Throwable tr)
    {
        String msg = tr.getMessage();
        if (msg == null) return true;
        if (tr instanceof ConnectException || msg.contains("No Internet Connection")) return false;
        if (tr instanceof UnknownHostException || msg.contains("No address associated with hostname")) return false;
        if (tr instanceof java.io.InterruptedIOException) return false;
        if (tr instanceof SocketException && msg.contains("timed out")) return false;
        if (tr instanceof javax.net.ssl.SSLException && msg.contains("closed")) return false;
        if (tr instanceof HttpException && msg.contains("java.net.ConnectException")) return false;

        if (tr instanceof ExecutionException)
        {
            if (msg.contains("com.android.volley.NoConnectionError")
                    || msg.contains("com.android.volley.TimeoutError")
                    || msg.contains("java.net.SocketException")
                    || msg.contains("java.net.UnknownHostException")) return false;
            if (msg.contains("com.android.volley.ServerError"))
            {
                Throwable cause = tr.getCause();
                if (cause instanceof ServerError) return logServerError((ServerError)cause);
            }
        }
        return true;
    }

    private static boolean logServerError(ServerError e)
    {
        if (e.networkResponse == null) return true;
        StringBuilder s = new StringBuilder();
        s.append("Status: ").append(e.networkResponse.statusCode);
        s.append("\nHeaders\n");
        if (e.networkResponse.headers != null)
        {
            for (String key : e.networkResponse.headers.keySet())
            {
                String value = e.networkResponse.headers.get(key);
                if (value == null) value = "";
                s.append(key).append(": ").append(value).append("\n");
            }
        }
        s.append("Data: ");
        if (e.networkResponse.data == null) s.append("null");
        else s.append(new String(e.networkResponse.data));
        return false;
    }
}