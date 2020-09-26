package tarn.pantip.content;

import java.io.IOException;

/**
 * User: Tarn
 * Date: 10/27/13 1:31 PM
 */
public class PantipException extends IOException
{
    public PantipException(String detailMessage)
    {
        super(detailMessage);
    }
}