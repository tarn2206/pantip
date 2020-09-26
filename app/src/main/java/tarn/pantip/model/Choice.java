package tarn.pantip.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * User: tarn
 * Date: 4/14/13 2:22 PM
 */
public class Choice implements Serializable
{
    private static final long serialVersionUID = 1L;

    public String id;
    public final String text;
    public String image;
    public String other;
    public boolean selected;
    public List<Choice> choices;
    public int value;
    public int[] values;

    Choice(String text)
    {
        this.text = text;
    }

    private Choice(String id, String text)
    {
        this(id, text, null);
    }

    Choice(String id, String text, String image)
    {
        this.id = id;
        this.text = text;
        this.image = image;
    }

    public Choice addChoice(String id, String text)
    {
        Choice choice = new Choice(id, text);
        if (choices == null) choices = new ArrayList<>();
        choices.add(choice);
        return choice;
    }

    @NonNull
    @Override
    public String toString()
    {
        return text;
    }
}