package tarn.pantip.content;

/**
 * Created by Tarn on 30 August 2016
 */
public interface Callback<T>
{
    void complete(T result);
    void error(Throwable tr);
}