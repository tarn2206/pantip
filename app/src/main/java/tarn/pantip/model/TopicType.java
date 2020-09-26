package tarn.pantip.model;

import androidx.annotation.DrawableRes;

import tarn.pantip.R;

/**
 * User: tarn
 * Date: 1/28/13 11:01 AM
 */
public enum TopicType
{
    All(0),
    Chat(1),
    Poll(2),
    Question(3),
    Review(4),
    News(5),
    Shopping(6);

    private final int value;

    TopicType(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    public static TopicType fromValue(int value)
    {
        switch (value)
        {
            case 1: return Chat;
            case 2: return Poll;
            case 3: return Question;
            case 4: return Review;
            case 5: return News;
            case 6: return Shopping;
            default: return All;
        }
    }

    public static TopicType fromValue2(int value)
    {
        switch (value)
        {
            case 1: return Chat;
            case 2: return Poll;
            case 3: return Question;
            case 4: return Review;
            case 5: return News;
            case 6: return Shopping;
            default: return All;
        }
    }

    public static TopicType parse(String value)
    {
        if (value.contains("-chat")) return Chat;
        if (value.contains("-poll")) return Poll;
        if (value.contains("-que")) return Question;
        if (value.contains("-review")) return Review;
        if (value.contains("-news")) return News;
        if (value.contains("-shop")) return Shopping;
        return All;
    }

    @DrawableRes
    public int getIcon()
    {
        switch (value)
        {
            case 1: return R.drawable.topic_chat;
            case 2: return R.drawable.topic_poll;
            case 3: return R.drawable.topic_question;
            case 4: return R.drawable.topic_review;
            case 5: return R.drawable.topic_news;
            case 6: return R.drawable.topic_trade;
            default: return 0;
        }
    }
}