package tarn.pantip.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * User: tarn
 * Date: 4/17/13 12:28 PM
 */
public class Size implements Serializable
{
    private static final long serialVersionUID = 1L;

    public int width;
    public int height;

    public Size()
    { }

    public Size(Size other)
    {
        this.width = other.width;
        this.height = other.height;
    }

    public Size(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    @NonNull
    @Override
    public String toString()
    {
        return width + "x" + height;
    }
}
