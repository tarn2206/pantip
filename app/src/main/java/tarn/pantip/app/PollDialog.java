package tarn.pantip.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.AppCompatEditText;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.ArrayList;
import java.util.List;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Callback;
import tarn.pantip.content.Poll;
import tarn.pantip.model.Choice;
import tarn.pantip.model.Question;
import tarn.pantip.model.TopicEx;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.BarChart;
import tarn.pantip.widget.PollRanking;
import tarn.pantip.widget.PollScale;
import tarn.pantip.widget.PollSpinner;
import tarn.pantip.widget.StackedBarChart;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.RelativeLayout.ALIGN_BASELINE;
import static android.widget.RelativeLayout.RIGHT_OF;

/**
 * Created by Tarn on 09 February 2017
 */

public class PollDialog extends AppCompatDialog
{
    private final View progress;
    private final View overlay;
    private final TopicEx topic;
    private final Callback<String> callback;

    private PollDialog(Context context, TopicEx topic, Callback<String> callback)
    {
        super(context);
        setCancelable(false);
        this.topic = topic.createCopy();
        this.topic.questions.clear();
        this.callback = callback;

        setContentView(R.layout.dialog_poll);
        progress = findViewById(android.R.id.progress);
        overlay = findViewById(R.id.overlay);
        overlay.setBackgroundColor(Pantip.overlayColor);
    }

    public static void edit(final Context context, final TopicEx topic, final Callback<String> callback)
    {
        final PollDialog dialog = new PollDialog(context, topic, callback);
        dialog.show();
        SpoilDialog.setWidth(dialog, Pantip.displayWidth);

        Poll.edit(topic.id).subscribe(dialog::complete, tr -> Utils.alert(context, tr));
    }

    public void complete()
    {
        setCancelable(true);
        progress.setVisibility(View.GONE);
        final LinearLayout container = findViewById(R.id.container);
        final PollValidator validator = render(topic, container, container, true);
        if (validator == null) return;
        validator.setOnClickListener(v -> {
            overlay.setVisibility(View.VISIBLE);
            progress.setVisibility(View.VISIBLE);

            Poll.submit(topic.id, validator.getResults())
                    .subscribe(result -> {
                        dismiss();
                        callback.complete(result);
                    }, tr -> {
                        progress.setVisibility(View.GONE);
                        overlay.setVisibility(View.GONE);
                        callback.error(tr);
                    });
        });
    }

    private void complete(String result)
    {
        Document doc = Jsoup.parse(result);
        Element div = doc.select("div.display-post-story").first();
        if (div != null)
        {
            List<Node> children = div.childNodes();
            for (Node node : children)
            {
                if (!(node instanceof Element)) continue;
                Element e = (Element)node;
                if (e.tagName().equals("div") || e.tagName().equals("span")) Poll.parse(e, topic);
            }
        }
        setCancelable(true);
        progress.setVisibility(View.GONE);
        final LinearLayout container = findViewById(R.id.container);
        final PollValidator validator = render(topic, container, container, true);
        if (validator == null) return;
        validator.setOnClickListener(v -> {
            overlay.setVisibility(View.VISIBLE);
            progress.setVisibility(View.VISIBLE);
            Poll.submit(topic.id, validator.getResults())
                    .subscribe(r -> {
                        dismiss();
                        callback.complete(r);
                    }, tr -> {
                        progress.setVisibility(View.GONE);
                        overlay.setVisibility(View.GONE);
                        callback.error(tr);
                    });
        });
    }

    public static PollValidator render(final TopicEx topic, LinearLayout pollContainer, LinearLayout container, boolean edit)
    {
        if (topic.requiredAnswer) wrapText(pollContainer, "* จำเป็นต้องตอบ", Pantip.textSize - 2, Pantip.dangerColor);
        PollValidator validator = new PollValidator();
        int leftMargin = 0;
        int topMargin = 0;
        for (int i = 0; i < topic.questions.size(); i++)
        {
            Question q = topic.questions.get(i);
            CharSequence title;
            if (q.require)
            {
                SpannableString ss = new SpannableString(q.title + " *");
                ForegroundColorSpan span = new ForegroundColorSpan(Pantip.dangerColor);
                ss.setSpan(span, q.title.length() + 1, q.title.length() + 2, 0);
                title = ss;
            }
            else title = q.title;

            TextView questView = wrapText(pollContainer, title, Pantip.textSize, Pantip.textColor);
            ((LinearLayout.LayoutParams)questView.getLayoutParams()).bottomMargin = Utils.toPixels(10);
            if (i == 0)
            {
                //leftMargin = (int)questView.getPaint().measureText("0. ", 0, 3);
                Rect bounds = Utils.getTextBounds(questView, "Z");
                topMargin = bounds.height();
            }

            if (topic.questions.size() == 1 && title.equals("1."))
            {
                pollContainer.removeView(questView);
                questView = null;
            }

            if (!edit && (topic.closeVote || topic.hasResult))
            {
                if (questView != null)
                {
                    int m = (i == 0) ? topMargin / 2 : topMargin * 2;
                    LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams)questView.getLayoutParams();
                    layout.topMargin = m;
                }
                renderPollResult(pollContainer, q, leftMargin, topMargin);
            }
            else
            {
                if ((i > 0 || topic.requiredAnswer) && questView != null) ((LinearLayout.LayoutParams)questView.getLayoutParams()).topMargin = topMargin;
                switch (q.type)
                {
                    case 1:
                        renderPoll1(pollContainer, q, leftMargin, topMargin, validator);
                        break;
                    case 2:
                        renderPoll2(pollContainer, q, leftMargin, topMargin, validator);
                        break;
                    case 3:
                        renderPoll3(pollContainer, q, leftMargin, topMargin, validator);
                        break;
                    case 4:
                        renderPoll4(pollContainer, q, leftMargin, topMargin, validator);
                        break;
                    case 5:
                        renderPoll5(pollContainer, q, leftMargin, topMargin, validator);
                        break;
                    case 6:
                        renderPoll6(pollContainer, q, leftMargin, topMargin, validator);
                        break;
                    default:
                        renderUnknownPoll(pollContainer, q, leftMargin, topMargin, validator);
                        break;
                }
            }
        }

        if (topic.closeVote) return null;

        Button submit = new Button(pollContainer.getContext());
        submit.setId(View.generateViewId());
        submit.setText(topic.voted ? "แก้ไขโพล" : "ตอบโพล");
        submit.setTextSize(Pantip.textSize - 1);

        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.topMargin = topMargin / 2;
        layout.bottomMargin = topMargin / 2;
        layout.gravity = Gravity.CENTER_HORIZONTAL;
        container.addView(submit, layout);

        validator.setSubmit(submit, topic.voted || topic.hasResult);
        return validator;
    }

    @SuppressLint("ClickableViewAccessibility")
    private static void renderPoll1(LinearLayout container, final Question q, int leftMargin, final int verticalMargin, PollValidator validation)
    {
        final RelativeLayout relativeLayout = new RelativeLayout(container.getContext());
        container.addView(relativeLayout);
        relativeLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        final RadioGroup radioGroup = new RadioGroup(container.getContext());
        relativeLayout.addView(radioGroup);
        radioGroup.setLayoutParams(new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        TextView otherText = null;
        int n = 0;
        for (Choice choice : q)
        {
            //if (choice.other == null) choice.image = "http://www.panayiotisgeorgiou.net/wp-content/uploads/2017/01/android.jpg";
            final RadioButton radioButton = new RadioButton(container.getContext());
            radioButton.setId(View.generateViewId());
            radioButton.setTag(choice.id);
            radioButton.setText(choice.text);
            radioButton.setTextSize(Pantip.textSize);
            radioButton.setTextColor(Pantip.textColor);
            radioGroup.addView(radioButton);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.leftMargin = leftMargin;
            params.topMargin = n++ == 0 ? 0 : verticalMargin;
            radioButton.setLayoutParams(params); // set layout before addView not work on some device
            if (choice.selected) radioButton.setChecked(true); // add view to container before set checked
            if (!q.require)
            {
                radioButton.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        if (radioButton.isChecked())
                        {
                            radioGroup.clearCheck();
                            return true;
                        }
                    }
                    return false;
                });
            }
            if (choice.other != null)
            {
                final AppCompatEditText other = createOther(container.getContext(), choice.other);
                otherText = other;
                other.setEnabled(choice.selected);
                relativeLayout.addView(other);
                Utils.observeGlobalLayout(radioButton, view -> {
                    RelativeLayout.LayoutParams otherParams = (RelativeLayout.LayoutParams)other.getLayoutParams();
                    otherParams.leftMargin = radioButton.getLeft() + radioButton.getWidth() + verticalMargin;
                    otherParams.topMargin = radioButton.getTop() + radioButton.getBaseline() - other.getBaseline();
                    otherParams.width = MATCH_PARENT;
                    relativeLayout.requestLayout();
                });
                radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    other.setEnabled(isChecked);
                    if (isChecked) other.requestFocus();
                });
            }
            if (choice.image != null)
            {
                View view = loadImage(radioGroup, choice.image, verticalMargin);
                view.setOnClickListener(v -> {
                    if (radioButton.isChecked()) radioGroup.clearCheck();
                    else radioButton.setChecked(true);
                });
            }
        }
        validation.observe(radioGroup, otherText, q.require);
    }

    private static void renderPoll2(LinearLayout container, final Question q, int leftMargin, final int verticalMargin, PollValidator validation)
    {
        List<CheckBox> list = new ArrayList<>();
        CompoundButton.OnCheckedChangeListener listener = null;
        TextView otherText = null;
        int n = 0;
        for (Choice choice : q)
        {
            //if (choice.other == null) choice.image = "http://www.panayiotisgeorgiou.net/wp-content/uploads/2017/01/android.jpg";
            final CheckBox checkBox = new CheckBox(container.getContext());
            checkBox.setId(View.generateViewId());
            checkBox.setTag(choice.id);
            checkBox.setText(choice.text);
            checkBox.setTextSize(Pantip.textSize);
            checkBox.setTextColor(Pantip.textColor);
            if (choice.selected) checkBox.setChecked(true);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.leftMargin = leftMargin;
            params.topMargin = n++ == 0 ? 0 : verticalMargin;
            if (choice.other == null)
            {
                container.addView(checkBox);
                checkBox.setLayoutParams(params);
            }
            else
            {
                RelativeLayout layout = new RelativeLayout(container.getContext());
                container.addView(layout);
                layout.setLayoutParams(params);

                layout.addView(checkBox);
                final TextView other = createOther(container.getContext(), choice.other);
                otherText = other;
                other.setEnabled(choice.selected);
                layout.addView(other);
                RelativeLayout.LayoutParams otherParams = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                otherParams.leftMargin = verticalMargin;
                otherParams.addRule(RIGHT_OF, checkBox.getId());
                otherParams.addRule(ALIGN_BASELINE, checkBox.getId());
                other.setLayoutParams(otherParams);
                checkBox.setOnCheckedChangeListener(listener = (buttonView, isChecked) -> {
                    other.setEnabled(isChecked);
                    if (isChecked) other.requestFocus();
                });
            }
            if (choice.image != null)
            {
                View view = loadImage(container, choice.image, verticalMargin);
                view.setOnClickListener(v -> checkBox.setChecked(!checkBox.isChecked()));
            }
            list.add(checkBox);
        }
        validation.observe(list, listener, otherText, q.require);
    }

    private static void renderPoll3(LinearLayout container, final Question q, int leftMargin, final int verticalMargin, PollValidator validation)
    {
        PollSpinner spinner = new PollSpinner(container.getContext());
        spinner.setChoices(q.choices);
        container.addView(spinner);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.leftMargin = leftMargin;
        //params.topMargin = verticalMargin;
        spinner.setLayoutParams(params);
        validation.observe(spinner, q.require);
    }

    private static void renderPoll4(LinearLayout container, final Question q, int leftMargin, final int verticalMargin, PollValidator validation)
    {
        PollScale scale = new PollScale(container.getContext());
        container.addView(scale);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.leftMargin = leftMargin;
        //params.topMargin = verticalMargin;
        params.bottomMargin = verticalMargin;
        scale.setLayoutParams(params);

        scale.setChoices(q.minText, q.maxText, q.choices, q.require);
        validation.observe(scale, q.require);
    }

    private static void renderPoll5(LinearLayout container, final Question q, int leftMargin, final int verticalMargin, PollValidator validation)
    {
        renderUnknownPoll(container, q, leftMargin, verticalMargin, validation);
    }

    private static void renderPoll6(LinearLayout container, final Question q, int leftMargin, final int verticalMargin, PollValidator validation)
    {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.leftMargin = leftMargin;
        List<PollRanking> list = new ArrayList<>();
        for (Choice choice : q)
        {
            //choice.image = "http://www.panayiotisgeorgiou.net/wp-content/uploads/2017/01/android.jpg";
            PollRanking ranking = new PollRanking(container.getContext());
            container.addView(ranking);
            ranking.setLayoutParams(params);
            ranking.setChoice(choice);
            list.add(ranking);
        }
        validation.observe(list, q.require);
    }

    private static void renderUnknownPoll(LinearLayout container, final Question q, int leftMargin, final int verticalMargin, PollValidator validation)
    {
        validation.observe(q.require);
        View view = wrapText(container, "ไม่สามารถแสดงโพลได้", Pantip.textSize, Pantip.textColorTertiary);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)view.getLayoutParams();
        params.leftMargin = leftMargin;
        //params.topMargin = verticalMargin;
    }

    private static void renderPollResult(LinearLayout container, Question q, int leftMargin, int verticalMargin)
    {
        if (q.type <= 4)
        {
            boolean hasImage = q.hasImage();
            int n = 0;
            for (Choice choice : q)
            {
                //if (q.type < 3 && !choice.title.equals("อื่นๆ")) choice.image = "http://www.panayiotisgeorgiou.net/wp-content/uploads/2017/01/android.jpg";
                BarChart chart = new BarChart(container.getContext());
                container.addView(chart);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                params.topMargin = n == 0 ? 0 : verticalMargin / 2;

                chart.setLayoutParams(params);
                chart.setData(n++, choice.text, q.maxVote, choice.value, hasImage && choice.image == null ? "" : choice.image);
            }
        }
        else if (q.type == 5 || q.type == 6)
        {
            StackedBarChart chart = new StackedBarChart(container.getContext());
            chart.setBottomMargin(verticalMargin);
            container.addView(chart);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            //params.topMargin = verticalMargin / 2;
            chart.setLayoutParams(params);
            chart.setData(q.choices, q.maxVote, q.legend, verticalMargin);
        }
        else
        {
            View view = wrapText(container, "ไม่สามารถแสดงโพลได้", Pantip.textSize, Pantip.textColorTertiary);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)view.getLayoutParams();
            params.leftMargin = leftMargin;
            //params.topMargin = verticalMargin / 2;
        }
    }

    private static AppCompatEditText createOther(Context context, String text)
    {
        AppCompatEditText other = new AppCompatEditText(context);
        other.setHint("โปรดระบุ");
        other.setText(text);
        other.setTextSize(Pantip.textSize);
        other.setTextColor(Pantip.textColor);
        other.setMaxLines(1);
        other.setImeOptions(EditorInfo.IME_ACTION_DONE);
        other.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
        other.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        other.setFilters(new InputFilter[] { new InputFilter.LengthFilter(50) });
        return other;
    }

    private static ImageView loadImage(LinearLayout container, String url, int verticalMargin)
    {
        ImageView imageView = new ImageView(container.getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Utils.getDisplaySize().x / 2, WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = verticalMargin;
        container.addView(imageView);
        imageView.setLayoutParams(params);
        GlideApp.with(container.getContext()).load(url).into(imageView);
        return imageView;
    }

    private static TextView wrapText(LinearLayout container, CharSequence text, int size, int foreColor)
    {
        TextView textView = new TextView(container.getContext());
        textView.setText(text);
        textView.setTextColor(foreColor);
        textView.setTextSize(size);
        textView.setTextIsSelectable(true);
        container.addView(textView);
        return textView;
    }
}