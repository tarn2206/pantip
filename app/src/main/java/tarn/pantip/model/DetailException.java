package tarn.pantip.model;

/**
 * Created by Tarn on 12 March 2017
 */

public class DetailException extends RuntimeException
{
    private final String cause;

    public DetailException(Exception cause, String detail)
    {
        super(detail, cause);
        this.cause = cause.getMessage();
    }

    public String getCauseMessage()
    {
        return cause;
    }
}