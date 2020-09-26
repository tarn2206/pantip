package tarn.pantip.app;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import tarn.pantip.model.PollResult;
import tarn.pantip.widget.PollRanking;
import tarn.pantip.widget.PollScale;
import tarn.pantip.widget.PollSpinner;

/**
 * Created by Tarn on 10 February 2017
 */

public class PollValidator
{
    private final List<Holder> holders = new ArrayList<>();
    private RadioGroup.OnCheckedChangeListener onRadioChangeListener;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;
    private AdapterView.OnItemSelectedListener onItemSelectedListener;
    private Button submit;

    public void observe(RadioGroup radioGroup, TextView other, boolean require)
    {
        holders.add(new Holder(radioGroup, other, require));
        radioGroup.setOnCheckedChangeListener(getRadioCheckedChangeListener());
    }

    public void observe(List<CheckBox> checkBoxes, CompoundButton.OnCheckedChangeListener listener, TextView other, boolean require)
    {
        holders.add(new Holder(checkBoxes.toArray(new CheckBox[0]), other, require));
        for (CheckBox checkBox : checkBoxes)
        {
            checkBox.setOnCheckedChangeListener(getCheckedChangeListener());
        }
        if (listener != null) checkBoxes.get(checkBoxes.size() - 1).setOnCheckedChangeListener(wrap(listener));
    }

    public void observe(PollSpinner spinner, boolean require)
    {
        holders.add(new Holder(spinner, require));
        spinner.setOnItemSelectedListener(getItemSelectedListener());
    }

    public void observe(PollScale scale, boolean require)
    {
        holders.add(new Holder(scale, require));
        scale.setOnCheckedChangeListener(getRadioCheckedChangeListener());
    }

    public void observe(boolean require)
    {
        holders.add(new Holder(require));
    }

    public void observe(List<PollRanking> rankings, boolean require)
    {
        holders.add(new Holder(rankings.toArray(new PollRanking[0]), require));
        for (PollRanking ranking : rankings)
        {
            ranking.setOnItemSelectedListener(getItemSelectedListener());
        }
    }

    public void setSubmit(Button submit, boolean voted)
    {
        this.submit = submit;
        if (!voted) validate();
    }

    private void validate()
    {
        boolean enabled = true;
        int validCount = 0;
        for (Holder holder : holders)
        {
            boolean valid = holder.valid();
            if (holder.require & !valid) enabled = false;
            if (valid) validCount++;
        }
        submit.setEnabled(enabled && validCount > 0);
    }

    private RadioGroup.OnCheckedChangeListener getRadioCheckedChangeListener()
    {
        if (onRadioChangeListener == null)
        {
            onRadioChangeListener = (group, checkedId) -> validate();
        }
        return onRadioChangeListener;
    }

    private CompoundButton.OnCheckedChangeListener getCheckedChangeListener()
    {
        if (onCheckedChangeListener == null)
        {
            onCheckedChangeListener = (buttonView, isChecked) -> validate();
        }
        return onCheckedChangeListener;
    }

    private CompoundButton.OnCheckedChangeListener wrap(final CompoundButton.OnCheckedChangeListener listener)
    {
        return (buttonView, isChecked) -> {
            listener.onCheckedChanged(buttonView, isChecked);
            validate();
        };
    }

    private AdapterView.OnItemSelectedListener getItemSelectedListener()
    {
        if (onItemSelectedListener == null)
        {
            onItemSelectedListener = new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    validate();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {
                    validate();
                }
            };
        }
        return onItemSelectedListener;
    }

    public void setOnClickListener(View.OnClickListener listener)
    {
        submit.setOnClickListener(listener);
    }

    private static class Holder
    {
        private final int type;
        private final boolean require;
        private RadioGroup radioGroup;
        private CheckBox[] checkBoxes;
        private PollSpinner spinner;
        private PollScale scale;
        private PollRanking[] rankings;
        private TextView other;

        Holder(RadioGroup radioGroup, TextView other, boolean require)
        {
            type = 1;
            this.radioGroup = radioGroup;
            this.other = other;
            this.require = require;
        }

        Holder(CheckBox[] checkBoxes, TextView other, boolean require)
        {
            type = 2;
            this.checkBoxes = checkBoxes;
            this.other = other;
            this.require = require;
        }

        Holder(PollSpinner spinner, boolean require)
        {
            type = 3;
            this.spinner = spinner;
            this.require = require;
        }

        Holder(PollScale scale, boolean require)
        {
            type = 4;
            this.scale = scale;
            this.require = require;
        }

        Holder(boolean require)
        {
            type = 5;
            this.require = require;
        }

        Holder(PollRanking[] rankings, boolean require)
        {
            type = 6;
            this.rankings = rankings;
            this.require = require;
        }

        private boolean valid()
        {
            switch (type)
            {
                case 1:
                    return radioGroup.getCheckedRadioButtonId() != View.NO_ID;
                case 2:
                    for (CheckBox checkBox : checkBoxes)
                    {
                        if (checkBox.isChecked()) return true;
                    }
                    break;
                case 3:
                    return spinner.getSelectedItemPosition() > 0;
                case 4:
                    return scale.getSelectedId() != null;
                case 5:
                    return false;
                case 6:
                    for (PollRanking ranking : rankings)
                    {
                        if (ranking.getSelectedItemPosition() > 0) return true;
                    }
                    break;
            }
            return false;
        }

        PollResult getResult()
        {
            PollResult result = new PollResult();
            result.type = type;
            switch (type)
            {
                case 1:
                    int id = radioGroup.getCheckedRadioButtonId();
                    if (id != View.NO_ID)
                    {
                        result.values = new ArrayList<>();
                        result.values.add((String)radioGroup.findViewById(id).getTag());
                        if (other != null && other.isEnabled()) result.other = other.getText().toString();
                    }
                    break;
                case 2:
                    for (CheckBox checkBox : checkBoxes)
                    {
                        if (checkBox.isChecked())
                        {
                            if (result.values == null) result.values = new ArrayList<>();
                            result.values.add((String)checkBox.getTag());
                        }
                    }
                    if (other != null && other.isEnabled()) result.other = other.getText().toString();
                    break;
                case 3:
                    if (spinner.getSelectedItemPosition() > 0)
                    {
                        result.values = new ArrayList<>();
                        result.values.add(spinner.getSelectedId());
                    }
                    break;
                case 4:
                    if (scale.getCheckedRadioButtonId() != View.NO_ID)
                    {
                        result.values = new ArrayList<>();
                        result.values.add(scale.getSelectedId());
                    }
                    break;
                case 5:
                    result.values = new ArrayList<>();
                    /*for (PollRanking ranking : rankings)
                    {
                        result.values.add(ranking.getSelectedId());
                    }*/
                    break;
                case 6:
                    result.values = new ArrayList<>();
                    for (PollRanking ranking : rankings)
                    {
                        result.values.add(ranking.getSelectedId());
                    }
                    break;
            }
            return result;
        }
    }

    public List<PollResult> getResults()
    {
        List<PollResult> results = new ArrayList<>();
        for (Holder holder : holders)
        {
            results.add(holder.getResult());
        }
        return results;
    }
}