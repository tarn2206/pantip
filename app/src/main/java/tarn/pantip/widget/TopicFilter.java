package tarn.pantip.widget;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import tarn.pantip.R;
import tarn.pantip.app.MainActivity;
import tarn.pantip.app.RecommendActivity;
import tarn.pantip.model.TopicType;

import static tarn.pantip.model.TopicType.All;
import static tarn.pantip.model.TopicType.Chat;
import static tarn.pantip.model.TopicType.News;
import static tarn.pantip.model.TopicType.Poll;
import static tarn.pantip.model.TopicType.Question;
import static tarn.pantip.model.TopicType.Review;
import static tarn.pantip.model.TopicType.Shopping;

/**
 * Created by Tarn on 20 August 2017
 */

public class TopicFilter implements View.OnClickListener
{
    private final MainActivity activity;
    private String forum;
    private String tag;
    private final View itemView;
    private OnItemClickListener listener;
    private TopicType topicType = All;
    private final ImageView all;
    private ImageView lastTopicType;

    public TopicFilter(MainActivity activity, String forum, String tag)
    {
        this.activity = activity;

        itemView = activity.getLayoutInflater().inflate(R.layout.topic_filter, null);
        lastTopicType = all = itemView.findViewById(R.id.topic_type_all);
        lastTopicType.setSelected(true);

        all.setOnClickListener(this);
        itemView.findViewById(R.id.best_topic).setOnClickListener(this);
        itemView.findViewById(R.id.topic_type_chat).setOnClickListener(this);
        itemView.findViewById(R.id.topic_type_question).setOnClickListener(this);
        itemView.findViewById(R.id.topic_type_news).setOnClickListener(this);
        itemView.findViewById(R.id.topic_type_poll).setOnClickListener(this);
        itemView.findViewById(R.id.topic_type_review).setOnClickListener(this);
        itemView.findViewById(R.id.topic_type_shop).setOnClickListener(this);

        setForum(forum, tag);
    }

    public void setForum(String forum, String tag)
    {
        this.forum = forum;
        this.tag = tag;
    }

    public TopicType getType()
    {
        return topicType;
    }

    public View getView()
    {
        return itemView;
    }

    @Override
    public void onClick(View v)
    {
        TopicType newType = topicType;
        switch (v.getId())
        {
            case R.id.best_topic:
                Intent intent = new Intent(activity, RecommendActivity.class);
                intent.putExtra("forum", forum);
                intent.putExtra("tag", tag);
                activity.startActivity(intent);
                return;
            case R.id.topic_type_all:
                newType = All;
                break;
            case R.id.topic_type_question:
                newType = Question;
                break;
            case R.id.topic_type_chat:
                newType = Chat;
                break;
            case R.id.topic_type_poll:
                newType = Poll;
                break;
            case R.id.topic_type_review:
                newType = Review;
                break;
            case R.id.topic_type_news:
                newType = News;
                break;
            case R.id.topic_type_shop:
                newType = Shopping;
                break;
        }
        if (topicType == newType) return;

        topicType = newType;
        if (lastTopicType != null) lastTopicType.setSelected(false);
        lastTopicType = (ImageView)v;
        lastTopicType.setSelected(true);

        if (listener != null) listener.onItemClick(topicType);
        /*notifyChanged();
        removeFooter();
        startLoading(false);*/
    }

    public void reset()
    {
        if (topicType == All) return;

        if (lastTopicType != null) lastTopicType.setSelected(false);
        topicType = All;
        lastTopicType = all;
        lastTopicType.setSelected(true);
    }

    public void setOnItemClickListener(OnItemClickListener listener)
    {
        this.listener = listener;
    }

    public interface OnItemClickListener
    {
        void onItemClick(TopicType type);
    }
}
