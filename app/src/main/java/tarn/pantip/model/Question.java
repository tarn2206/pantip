package tarn.pantip.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * User: tarn
 * Date: 4/7/13 12:38 AM
 */
public class Question implements Serializable, Iterable<Choice>
{
    private static final long serialVersionUID = 1L;

    public String title;
    public int type;
    public boolean require;
    public String minText;
    public String maxText;
    public ArrayList<Choice> choices;
    public int maxVote;
    public String[] legend;

    public Question()
    { }

    public Question(String title)
    {
        this.title = title;
    }

    public Choice addChoice(String id, String text)
    {
        return addChoice(id, text, null);
    }

    public Choice addChoice(String text)
    {
        return addChoice(null, text, null);
    }

    private Choice addChoice(String id, String text, String image)
    {
        Choice choice = new Choice(id, text, image);
        if (choices == null) choices = new ArrayList<>();
        choices.add(choice);
        return choice;
    }

    public Choice addChoice(int index, String text, int value)
    {
        Choice choice = new Choice(text);
        choice.value = value;
        if (choices == null) choices = new ArrayList<>();
        while (choices.size() <= index) choices.add(null);
        choices.set(index, choice);
        return choice;
    }

    public boolean hasImage()
    {
        if (choices == null) return false;
        for (Choice c : choices)
        {
            if (c.image != null) return true;
        }
        return false;
    }

    @NonNull
    @Override
    public Iterator<Choice> iterator()
    {
        return choices == null ? new Iterator<Choice>()
        {
            @Override
            public boolean hasNext()
            {
                return false;
            }

            @Override
            public Choice next()
            {
                return null;
            }
        } : choices.iterator();
    }
}