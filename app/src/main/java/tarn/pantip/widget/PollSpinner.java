package tarn.pantip.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;

import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.model.Choice;

/**
 * Created by Tarn on 06 February 2017
 */

public class PollSpinner extends AppCompatSpinner
{
    public PollSpinner(Context context)
    {
        super(context);
    }

    public void setChoices(List<Choice> choices)
    {
        setAdapter(new ChoiceAdapter(getContext(), choices));
        for (int i = 0; i < choices.size(); i++)
        {
            Choice choice = choices.get(i);
            if (choice.selected) setSelection(i);
        }
    }

    public String getSelectedId()
    {
        return ((Choice)getSelectedItem()).id;
    }

    public static class ChoiceAdapter extends ArrayAdapter<Choice>
    {
        ChoiceAdapter(Context context, List<Choice> items)
        {
            super(context, R.layout.spinner_item, items);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent)
        {
            TextView view = (TextView)super.getView(position, convertView, parent);
            view.setTextSize(Pantip.textSize);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
        {
            TextView view = (TextView)super.getDropDownView(position, convertView, parent);
            view.setTextSize(Pantip.textSize);
            return view;
        }
    }
}