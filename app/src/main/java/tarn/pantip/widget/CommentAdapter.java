package tarn.pantip.widget;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.flexbox.FlexboxLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.app.EmotionDialog;
import tarn.pantip.app.MainActivity;
import tarn.pantip.app.PollDialog;
import tarn.pantip.app.PollValidator;
import tarn.pantip.app.ProfileActivity;
import tarn.pantip.app.ReplyActivity;
import tarn.pantip.app.SpoilDialog;
import tarn.pantip.app.TopicActivity;
import tarn.pantip.content.Callback;
import tarn.pantip.content.ObjectStore;
import tarn.pantip.content.Poll;
import tarn.pantip.content.TargetType;
import tarn.pantip.content.Vote;
import tarn.pantip.model.Comment;
import tarn.pantip.model.Emotion;
import tarn.pantip.model.Story;
import tarn.pantip.model.StoryType;
import tarn.pantip.model.Tag;
import tarn.pantip.model.Topic;
import tarn.pantip.model.TopicEx;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.GlideApp;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 12 August 2017
 */

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder>
{
    private final TopicActivity activity;
    private final LayoutInflater inflater;
    private final TopicEx topic;
    private final List<Comment> list = new ArrayList<>();
    private final List<Story> imageList = new ArrayList<>();
    public static final int padding = Utils.toPixels(12);
    private boolean processFeedback;
    private final int avatarSize;

    public CommentAdapter(TopicActivity activity, TopicEx topic)
    {
        this.activity = activity;
        inflater = activity.getLayoutInflater();
        this.topic = topic;
        avatarSize = (int)(2.2 * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, Pantip.textSize, activity.getResources().getDisplayMetrics()));
    }

    public void append(Comment[] a)
    {
        int start = list.size();
        list.addAll(Arrays.asList(a));
        notifyItemRangeInserted(start, a.length);
        for (Comment item : a)
        {
            if (item.storyList == null) continue;
            for (Story story : item.storyList)
            {
                if (story.type == StoryType.Image) imageList.add(story);
            }
        }
    }

    public void update(Comment[] a)
    {
        RecyclerUtil.update(this, list, a, false, new RecyclerUtil.Callback<Comment>()
        {
            @Override
            protected boolean areItemsTheSame(Comment oldItem, Comment newItem)
            {
                return oldItem.id == newItem.id;
            }

            @Override
            protected boolean areContentsTheSame(Comment oldItem, Comment newItem)
            {
                return (oldItem.replyCount == newItem.replyCount && StringUtils.equals(oldItem.edit, newItem.edit))
                        || !(oldItem instanceof TopicEx) || ((TopicEx)oldItem).type != TopicType.Poll;
            }
        });
    }

    public Comment[] getItems()
    {
        return list.toArray(new Comment[0]);
    }

    @Override
    public int getItemCount()
    {
        return list.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        return position == 0 ? -1 : list.get(position).replyNo;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        TopicEx topic = null;
        View.OnClickListener tagListener = null;
        if (viewType == -1)
        {
            topic = (TopicEx)list.get(0);
            tagListener = v -> {
                Tag tag = (Tag)v.getTag();
                Intent intent = new Intent(activity, MainActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra("view", MainActivity.TAG)
                        .putExtra("label", tag.label)
                        .putExtra("value", tag.url);
                activity.startActivity(intent);
                Pantip.getDataStore().updateTagFavorite(null, tag.label, tag.url, true);
            };
        }
        return new ViewHolder(inflater, parent, viewType, topic, tagListener);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        int mid;
        String avatarUrl;
        final View commentView;
        final View authorGroup;
        final ImageView avatar;
        final TextView author;
        final TextView time;
        final TextView no;
        final View editButton;
        final View replyFooter;

        private final LinearLayout storyGroup;

        final View statGroup;
        final TextView stat;
        final TextView reply;

        final View feedbackGroup;
        final View feedbackDivider;
        final FeedbackButton feedbackVoteButton;
        final FeedbackButton feedbackEmotionButton;
        final FeedbackButton feedbackReplyButton;

        final View footerPadding;

        ViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType, TopicEx topic, View.OnClickListener tagListener)
        {
            super(inflater.inflate(viewType <= 0 ? R.layout.comment_item : R.layout.comment_reply, parent, false));
            itemView.setClickable(false);
            itemView.setLongClickable(false);
            itemView.setFocusable(false);

            if (viewType == -1)
            {
                LinearLayout linearLayout = (LinearLayout)itemView;
                View header = createTitleView(inflater, linearLayout, topic, tagListener);
                linearLayout.addView(header, 0);
            }

            commentView = itemView.findViewById(R.id.comment_item);
            replyFooter = itemView.findViewById(R.id.last_reply);

            authorGroup = itemView.findViewById(R.id.author_group);
            avatar = itemView.findViewById(R.id.avatar);
            author = itemView.findViewById(R.id.story_author);
            author.setTextSize(Pantip.textSize);
            if (Pantip.isNightMode)
                author.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textColor));
            View.OnClickListener authorClickListener = v -> {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("mid", mid);
                intent.putExtra("name", author.getText());
                intent.putExtra("avatar", avatarUrl);
                v.getContext().startActivity(intent);
            };
            avatar.setOnClickListener(authorClickListener);
            author.setOnClickListener(authorClickListener);

            time = itemView.findViewById(R.id.time);
            time.setTextSize(Pantip.textSize - 3);
            no = itemView.findViewById(R.id.comment_no);
            no.setTextSize(Pantip.textSize - 4);
            editButton = itemView.findViewById(R.id.edit);

            storyGroup = itemView.findViewById(R.id.story_group);

            statGroup = itemView.findViewById(R.id.stat_group);
            stat = itemView.findViewById(R.id.stat);
            stat.setTextColor(Pantip.textColorSecondary);
            stat.setTextSize(Utils.fixTextSize(Pantip.textSize - 3));
            reply = itemView.findViewById(R.id.reply);
            reply.setTextColor(Pantip.textColorSecondary);
            reply.setTextSize(Utils.fixTextSize(Pantip.textSize - 3));

            feedbackGroup = itemView.findViewById(R.id.feedback_group);
            feedbackDivider = itemView.findViewById(R.id.feedback_divider);
            feedbackVoteButton = itemView.findViewById(R.id.feedback_vote_button);
            feedbackEmotionButton = itemView.findViewById(R.id.feedback_emotion_button);
            feedbackReplyButton = itemView.findViewById(R.id.feedback_reply_button);

            footerPadding = itemView.findViewById(R.id.footer_padding);
        }

    }

    private static View createTitleView(LayoutInflater inflater, ViewGroup parent, TopicEx topic, View.OnClickListener tagListener)
    {
        View header = inflater.inflate(R.layout.topic_title, parent, false);

        TextView titleView = header.findViewById(R.id.title);
        titleView.setBackgroundResource(Pantip.topicBackground[Pantip.xLarge ? 0 : 1]);
        titleView.setPadding(Pantip.spacer, (int)(Pantip.spacer / 1.2f), Pantip.spacer, (int)(Pantip.spacer / 3f));
        titleView.setTextSize(Pantip.textSize + 1);
        titleView.setTextIsSelectable(true);
        titleView.setTransformationMethod(null);
        TopicAdapter.setTitle(titleView, topic.type, topic.title, 0);

        if (topic.notify != null)
        {
            TextView textView = header.findViewById(R.id.notify);
            textView.setText(topic.notify);
            textView.setTextSize(Pantip.textSize - 1);
            textView.setVisibility(View.VISIBLE);

            titleView.setBackgroundResource(Pantip.topicBackground[0]);
            titleView.setPadding(Utils.toPixels(10), Utils.toPixels(8), Utils.toPixels(10), 0);
        }

        FlexboxLayout tagsView = header.findViewById(R.id.topic_tags);
        if (topic.tags == null || topic.tags.size() == 0) Utils.setVisible(tagsView, false);
        else
        {
            tagsView.setBackgroundResource(Pantip.topicBackground[1]);
            tagsView.setPadding(Pantip.spacer, 0, Pantip.spacer, 0);
            for (Tag tag : topic.tags)
            {
                String label = " " + tag.label + " ";
                TextView tagView = (TextView)inflater.inflate(R.layout.tag, tagsView, false);
                tagView.setTag(tag);
                tagView.setText(label);
                tagView.setOnClickListener(tagListener);
                tagsView.addView(tagView);
            }
        }

        return header;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Comment comment = list.get(position);
        try
        {
            int[] backgrounds;
            if (comment.no == 0) backgrounds = Pantip.topicBackground;
            else if (comment.replyNo == 0) backgrounds = Pantip.commentBackground;
            else
            {
                backgrounds = Pantip.replyBackground;
                holder.commentView.setBackgroundResource(Pantip.commentBackground[1]);
                holder.commentView.setPadding(padding, 0, padding, 0);
                boolean nextIsReply = position < getItemCount() - 1 && list.get(position + 1).replyNo > 0;
                holder.replyFooter.setVisibility(nextIsReply ? View.GONE : View.VISIBLE);
            }

            bindAuthor(holder, backgrounds, comment);

            boolean hasNext = position < getItemCount() - 1 && list.get(position + 1).hasPrev;
            bindStory(holder.storyGroup, backgrounds, comment, hasNext);

            bindStat(holder, backgrounds, comment);
            boolean deleted;
            if (comment.replyNo == 0) deleted = comment.deleteMessage != null;
            else
            {
                deleted = comment.deleted;
                if (!deleted)
                {
                    int i = position;
                    while (i > 0 && list.get(i).replyNo > 0) i--;
                    deleted = list.get(i).deleteMessage != null;
                }
            }
            bindFeedback(holder, backgrounds, comment, deleted);

            //holder.itemView.setBackgroundColor(position % 2 == 0 ? 0xFFFFCC00 : 0xFF9900FF);
        }
        catch (Exception e)
        {
            Pantip.handleException(activity, e);
        }
    }

    private void bindAuthor(ViewHolder holder, int[] backgrounds, final Comment comment)
    {
        holder.authorGroup.setVisibility(comment.hasPrev ? View.GONE : View.VISIBLE);
        if (comment.hasPrev) return;

        holder.authorGroup.setBackgroundResource(backgrounds[comment.no == 0 ? 1 : 0]);
        holder.authorGroup.setPadding(0, Utils.toPixels(6), 0, Utils.toPixels(16));
        ((ViewGroup.MarginLayoutParams)holder.authorGroup.getLayoutParams()).topMargin = comment.replyNo == 0 ? 0 : (comment.replyNo == 1 ? Utils.toPixels(10) : Utils.toPixels(6));

        if (comment.no == 0) holder.no.setVisibility(View.GONE);
        else
        {
            String no = String.valueOf(comment.no);
            if (comment.replyNo != 0) no += "-" + comment.replyNo;
            holder.no.setText(no);
            holder.no.setVisibility(View.VISIBLE);
        }

        holder.mid = comment.mid;
        holder.avatar.getLayoutParams().width = avatarSize;
        holder.avatar.getLayoutParams().height = avatarSize;
        if (comment.avatar == null || comment.avatar.startsWith("/images/unknown-avatar"))
            holder.avatar.setImageResource(Pantip.currentTheme == R.style.AppTheme ? R.drawable.avatar_pantip : R.drawable.avatar_light);
        else
        {
            String url = comment.avatar;
            if (url.startsWith("http://")) url = "https://" + url.substring(7);
            GlideApp.with(activity).load(url).transform(new CircleCrop()).into(holder.avatar);
            holder.avatarUrl = url;
        }

        String author = comment.author + " ";
        holder.author.setText(author);
        holder.time.setText(Utils.getRelativeTime(comment.time));

        boolean myComment = comment.author != null && Pantip.currentUser != null && StringUtils.equals(comment.author, Pantip.currentUser.name) && !comment.isTopic() && comment.deleteMessage == null;
        if (!myComment)
        {
            holder.editButton.setVisibility(View.GONE);
            return;
        }

        holder.editButton.setVisibility(View.VISIBLE);
        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ReplyActivity.class);
            intent.putExtra("edit", true);
            intent.putExtra("topic", ObjectStore.put(topic));
            intent.putExtra("comment", ObjectStore.put(comment));
            activity.startActivityForResult(intent, TopicActivity.RC_EDIT);
        });
    }

    private void bindStat(ViewHolder holder, int[] backgrounds, Comment comment)
    {
        CharSequence stat = comment.getStatText();
        String reply = comment.replyCount == 0 ? null : Topic.nFormat.format(comment.replyCount) + " ความเห็นย่อย";
        boolean visible = !comment.hasNext && (StringUtils.isNotBlank(stat) || reply != null);

        holder.statGroup.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) return;

        holder.statGroup.setBackgroundResource(backgrounds[1]);
        holder.statGroup.setPadding(Pantip.spacer, 0, Pantip.spacer, 0);

        holder.stat.setText(stat);
        Utils.setVisible(holder.stat, StringUtils.isNotBlank(stat));
        holder.reply.setText(reply);
        Utils.setVisible(holder.reply, reply != null);
    }

    private void bindFeedback(ViewHolder holder, int[] backgrounds, Comment comment, boolean deleted)
    {
        boolean feedbackVisible = !deleted && !comment.hasNext && Pantip.loggedOn && !comment.isNew;
        holder.feedbackGroup.setVisibility(feedbackVisible ? View.VISIBLE : View.GONE);
        boolean footerVisible = !feedbackVisible && !comment.hasNext && comment.replyCount == 0;
        holder.footerPadding.setVisibility(footerVisible ? View.VISIBLE : View.GONE);
        boolean hasReply = comment.replyCount > 0;
        if (footerVisible)
        {
            holder.footerPadding.setBackgroundResource(backgrounds[hasReply ? 1 : 2]);
            ((ViewGroup.MarginLayoutParams)holder.footerPadding.getLayoutParams()).bottomMargin = comment.replyCount > 0 ? 0 : (comment.replyNo == 0 ? Utils.getDimension(R.dimen.last_padding) : Utils.toPixels(10));
        }
        if (!feedbackVisible) return;

        holder.feedbackGroup.setBackgroundResource(backgrounds[hasReply ? 1 : 2]);
        //holder.feedbackGroup.setPadding(holder.feedbackGroup.getPaddingLeft(), padding, holder.feedbackGroup.getPaddingRight(), Util);
        ((ViewGroup.MarginLayoutParams)holder.feedbackGroup.getLayoutParams()).bottomMargin = comment.replyCount > 0 ? 0 : (comment.replyNo == 0 ? Utils.getDimension(R.dimen.last_padding) : Utils.toPixels(10));

        holder.feedbackDivider.setBackgroundColor(comment.replyNo == 0 ? Pantip.feedbackDividerColor : Pantip.feedbackDividerReplyColor);
        holder.feedbackVoteButton.setSelected(comment.liked);
        holder.feedbackVoteButton.setOnClickListener(new FeedbackClickListener(comment, "vote", holder));
        setEmoticon(holder.feedbackEmotionButton, comment);
        holder.feedbackEmotionButton.setSelected(comment.emotions != null && comment.emotions.getSelected() >= 0);
        holder.feedbackEmotionButton.setOnClickListener(new FeedbackClickListener(comment, "emotion", holder));
        holder.feedbackReplyButton.setOnClickListener(new FeedbackClickListener(comment, "reply", holder));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindStory(LinearLayout storyGroup, int[] backgrounds, final Comment comment, boolean hasNext)
    {
        storyGroup.removeAllViews();
        storyGroup.setBackgroundResource(backgrounds[1]);

        final TopicEx topicEx = (comment instanceof TopicEx) ? (TopicEx)comment : null;
        if (topicEx != null && topicEx.type == TopicType.Review && !topicEx.hasPrev)
        {
            createReviewHeader(storyGroup, topicEx);
        }

        if (comment.deleteMessage != null)
        {
            TextView textView = wrapText(storyGroup, comment.deleteMessage, Pantip.textSize, Pantip.textColorTertiary, Typeface.NORMAL, false);
            textView.setTag("spoil");
            textView.setLongClickable(true);
            textView.setOnLongClickListener(v -> {
                SpoilDialog.show(activity, topic.id, comment.storyList);
                return true;
            });
            ((LinearLayout.LayoutParams)textView.getLayoutParams()).bottomMargin = Utils.toPixels(8);
        }
        else if (comment.storyList != null && comment.storyList.size() > 0)
        {
            for (int i = 0; i < comment.storyList.size(); i++)
            {
                Story story = comment.storyList.get(i);
                PhotoLayout image = null;
                if (story.type == StoryType.Text)
                {
                    if (StringUtils.isBlank(story.text) && (i > 0 && comment.storyList.get(i - 1).type != StoryType.Text) && (i < comment.storyList
                            .size() - 1 && comment.storyList.get(i + 1).type != StoryType.Text)) continue;
                    TextView textView = wrapText(storyGroup, "", Pantip.textSize, Pantip.textColor, Typeface.NORMAL, true);
                    textView.setText(story.getSpannable(activity, textView));
                    Utils.addLinkMovementMethod(textView);
                    if (i == comment.storyList.size() - 1) ((LinearLayout.LayoutParams)textView.getLayoutParams()).bottomMargin = Utils.toPixels(12);
                }
                else if (story.type == StoryType.Image)
                {
                    int index = -1;
                    for (int n = 0; n < imageList.size(); n++)
                    {
                        if (StringUtils.equals(imageList.get(n).text, story.text))
                        {
                            index = n;
                            break;
                        }
                    }
                    image = new PhotoLayout(activity, story, comment.replyNo > 0, topic.id, imageList, index);
                }
                else if (story.type == StoryType.YouTube)
                {
                    image = new PhotoLayout(activity, story, comment.replyNo > 0);
                }
                else if (story.type == StoryType.Maps)
                {
                    image = new PhotoLayout(activity, story, comment.replyNo > 0);
                }
                else if (story.type == StoryType.Spoil)
                {
                    String text = "[Spoil] คลิกเพื่อดูข้อความที่ซ่อนไว้ ";
                    SpannableString s = SpannableString.valueOf(text);
                    s.setSpan(new UnderlineSpan(), 0, text.length() - 1, 0);
                    TextView textView = wrapText(storyGroup, s, Pantip.textSize, Pantip.linkColor, Typeface.NORMAL, false);
                    textView.setOnClickListener(new SpoilListener(story.spoil));
                    textView.setTag("spoil");
                    if (i == comment.storyList.size() - 1) ((LinearLayout.LayoutParams)textView.getLayoutParams()).bottomMargin = Utils.toPixels(8);
                }
                if (image != null)
                {
                    int width = Utils.getDisplaySize().x * 2 / 3;
                    int height = (int)(width * 0.75f);
                    LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(width, height);
                    layout.gravity = Gravity.CENTER_HORIZONTAL;
                    storyGroup.addView(image, layout);
                    /*if (Pantip.xLarge)
                    {
                        image.setPadding(4, image.getPaddingTop(), 4, image.getPaddingBottom());
                    }*/
                    image.loadImage();
                }
            }
        }

        if (topicEx != null && topicEx.deleteMessage == null && topicEx.type == TopicType.Poll && !hasNext)
            createPoll(storyGroup);
        else if (!comment.hasNext && (comment.edit != null || comment.editTime != null))
            createStoryFooter(storyGroup, comment);
    }

    private void createStoryFooter(LinearLayout storyGroup, Comment comment)
    {
        if (comment.edit == null)
        {
            String time = Utils.getRelativeTime(comment.editTime) + " ";
            if (time.startsWith("เมื่อ")) comment.edit = "แก้ไขข้อความ" + time;
            else comment.edit = "แก้ไขข้อความเมื่อ " + time;
        }

        int topSpacer = 0;
        if (comment.storyList.size() > 0)
        {
            StoryType lastType = comment.storyList.get(comment.storyList.size() - 1).type;
            if (lastType == StoryType.Image || lastType == StoryType.YouTube || lastType == StoryType.Maps)
                topSpacer = Utils.toPixels(2);
        }
        TextView textView = wrapText(storyGroup, comment.edit, Pantip.textSize - 4, Pantip.textColorTertiary, Typeface.ITALIC, false);
        textView.setPadding(0, topSpacer, 0, Utils.toPixels(6));
        textView.setTag("edit");
    }

    private void createReviewHeader(LinearLayout storyGroup, TopicEx topic)
    {
        TextView textView = wrapText(storyGroup, topic.reviewProduct, Pantip.textSize + 4, Pantip.topicTitleColor, Typeface.NORMAL, true);
        textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        RatingBar ratingBar = new RatingBar(activity);
        ratingBar.setIsIndicator(true);
        ratingBar.setRating(topic.reviewRating);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setMargins(Pantip.spacer, padding / 2, 0, topic.mapsUrl == null ? padding : 0);
        storyGroup.addView(ratingBar, layout);

        if (topic.mapsUrl != null)
        {
            String url;
            try
            {
                String location = Utils.getMapLocation(topic.mapsUrl);
                String place = Utils.formatLocation(location);
                url = "https://www.google.com/maps/place/" + place + "/@" + location + ",15z";
            }
            catch (Exception e)
            {
                L.e(e, topic.mapsUrl);
                url = topic.mapsUrl;
            }
            SpannableString sp = new SpannableString(url);
            URLSpan span = new URLSpan(url);
            sp.setSpan(span, 0, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView = wrapText(storyGroup, sp, Pantip.textSize, Pantip.textColor, Typeface.NORMAL, true);
            Utils.addLinkMovementMethod(textView);
        }
    }

    private void createPoll(LinearLayout storyGroup)
    {
        boolean marginAdded = false;
        if (topic.pollRemark != null)
        {
            View view = wrapText(storyGroup, topic.pollRemark, Pantip.textSize - 1, Pantip.dangerColor, Typeface.NORMAL, true);
            LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams)view.getLayoutParams();
            layout.topMargin = Utils.toPixels(16);
            marginAdded = true;
        }
        if (topic.deadline != null)
        {
            int color = topic.closeVote ? Pantip.dangerColor : Pantip.colorAccent;
            View view = wrapText(storyGroup, topic.deadline, Pantip.textSize - 1, color, Typeface.NORMAL, true);
            LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams)view.getLayoutParams();
            layout.topMargin = Utils.toPixels(marginAdded ? 8 : 16);
            marginAdded = true;
        }

        LinearLayout pollHeader = new LinearLayout(activity);
        pollHeader.setId(View.generateViewId());
        pollHeader.setBackgroundResource(Pantip.isNightMode ? Pantip.commentBackground[0] : Pantip.replyBackground[0]);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toPixels(8));
        layout.leftMargin = layout.rightMargin = padding;
        if (!marginAdded) layout.topMargin = Utils.toPixels(padding);
        storyGroup.addView(pollHeader, layout);

        LinearLayout pollContainer = new LinearLayout(activity);
        pollContainer.setBackgroundResource(Pantip.isNightMode ? Pantip.commentBackground[1] : Pantip.replyBackground[1]);
        pollContainer.setOrientation(LinearLayout.VERTICAL);
        pollContainer.setPadding(Pantip.spacer, padding, Pantip.spacer, padding);
        layout = new LinearLayout.LayoutParams(Pantip.displayWidth - 2 * padding, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.gravity = Gravity.CENTER_HORIZONTAL;
        storyGroup.addView(pollContainer, layout);

        LinearLayout pollFooter = new LinearLayout(activity);
        pollFooter.setBackgroundResource(Pantip.isNightMode ? Pantip.commentBackground[2] : Pantip.replyBackground[2]);
        layout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, padding);
        layout.leftMargin = layout.rightMargin = padding;
        storyGroup.addView(pollFooter, layout);

        final PollValidator validator = PollDialog.render(topic, pollContainer, storyGroup, false);
        if (validator != null)
        {
            validator.setOnClickListener(v -> {
                if (topic.voted && topic.hasResult)
                {
                    PollDialog.edit(activity, topic, new Callback<String>()
                    {
                        @Override
                        public void complete(String result)
                        {
                            activity.onRefresh(true);
                        }

                        @Override
                        public void error(Throwable tr)
                        {
                            Utils.alert(activity, tr);
                        }
                    });
                }
                else
                {
                    v.setEnabled(false);
                    Poll.submit(topic.id, validator.getResults())
                            .subscribe(result -> activity.onRefresh(true),
                                    tr -> {
                                        v.setEnabled(true);
                                        Utils.alert(activity, tr);
                                    });
                }
            });
        }
    }

    private TextView wrapText(LinearLayout container, CharSequence text, int size, int foreColor, int style, boolean selectable)
    {
        TextView textView = new TextView(activity);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setMargins(Pantip.spacer, 0, Pantip.spacer, 0);
        textView.setLayoutParams(layout);
        textView.setText(text);
        textView.setTextColor(foreColor);
        textView.setLinkTextColor(Pantip.linkColor);
        textView.setTextSize(size);
        textView.setTypeface(null, style);
        textView.setTextIsSelectable(selectable);
        container.addView(textView);
        return textView;
    }

    private class SpoilListener implements View.OnClickListener
    {
        private final List<Story> spoil;

        SpoilListener(List<Story> spoil)
        {
            this.spoil = spoil;
        }

        @Override
        public void onClick(View v)
        {
            SpoilDialog.show(activity, topic.id, spoil);
        }
    }

    class FeedbackClickListener implements View.OnClickListener
    {
        private final TargetType type;
        private final Comment comment;
        private final String action;
        private final ViewHolder holder;

        private FeedbackClickListener(Comment comment, String action, ViewHolder holder)
        {
            type = comment.no == 0 ? TargetType.Topic : (comment.replyId == 0 ? TargetType.Comment : TargetType.Reply);
            this.comment = comment;
            this.action = action;
            this.holder = holder;
        }

        @Override
        public void onClick(View v)
        {
            if (action.equals("vote"))
            {
                if (processFeedback) return;
                processFeedback = true;
                Utils.playSound(activity, R.raw.pop);
                holder.feedbackVoteButton.setTag(holder.feedbackVoteButton.getText());
                holder.feedbackVoteButton.setText("...");
                Vote.post(type, comment.liked, topic.id, comment.id, comment.no, comment.replyId, comment.replyNo)
                        .subscribe(this::voteSuccess, this::voteFailed);
            }
            else if (action.equals("emotion"))
            {
                if (processFeedback) return;
                EmotionDialog.show(activity, type, topic.id, comment.id, comment.no, comment.replyId, comment.replyNo, new EmoticonCallback(holder, comment));
            }
            else if (action.equals("reply"))
            {
                Intent intent = new Intent(activity, ReplyActivity.class);
                intent.putExtra("topic", ObjectStore.put(topic));
                intent.putExtra("comment", ObjectStore.put(comment));
                activity.startActivityForResult(intent, TopicActivity.RC_REPLY);
            }
        }

        private void voteSuccess(Vote result)
        {
            processFeedback = false;
            holder.feedbackVoteButton.setText((String)holder.feedbackVoteButton.getTag());
            if (result.success)
            {
                comment.toggleVote();
                activity.saveComments();
                if (comment.liked) Utils.showToast(activity, result.message, Toast.LENGTH_LONG, false);
                CommentAdapter.this.notifyItemChanged(holder.getAdapterPosition());
            }
            else Utils.showToast(activity, result.message, Toast.LENGTH_LONG, false);
        }

        private void voteFailed(Throwable tr)
        {
            processFeedback = false;
            holder.feedbackVoteButton.setText((String)holder.feedbackVoteButton.getTag());
            Utils.showToast(activity, tr.getMessage(), Toast.LENGTH_LONG, false);
            L.e(tr);
        }
    }

    private class EmoticonCallback implements Callback<Emotion>
    {
        private final ViewHolder holder;
        private final Comment comment;

        EmoticonCallback(ViewHolder holder, Comment comment)
        {
            this.holder = holder;
            this.comment = comment;
        }

        @Override
        public void complete(Emotion result)
        {
            processFeedback = false;
            if (result != null)
            {
                comment.update(result);
                activity.saveComments();
                CommentAdapter.this.notifyItemChanged(holder.getAdapterPosition());
            }
            setEmoticon(holder.feedbackEmotionButton, comment);
        }

        @Override
        public void error(Throwable tr)
        {
            processFeedback = false;
            holder.feedbackEmotionButton.setText((String)holder.feedbackEmotionButton.getTag());
            Pantip.handleException(activity, tr);
        }
    }

    private void setEmoticon(FeedbackButton button, Comment comment)
    {
        int resId = 0;
        if (comment.emotions != null)
        {
            if (comment.emotions.like.selected) resId = R.drawable.reaction_like;
            else if (comment.emotions.laugh.selected) resId = R.drawable.reaction_haha;
            else if (comment.emotions.love.selected) resId = R.drawable.reaction_love;
            else if (comment.emotions.impress.selected) resId = R.drawable.reaction_impress;
            else if (comment.emotions.scary.selected) resId = R.drawable.reaction_scary;
            else if (comment.emotions.surprised.selected) resId = R.drawable.reaction_wow;
        }
        if (resId > 0) button.setIconResource(resId, false);
        else button.setIconResource(R.drawable.ic_emoticon_black_24dp, true);

        button.setText(comment.emotions != null ? comment.emotions.getSelectedText() : "แสดงความรู้สึก");
    }

    public int getPosition(int cNo, int rNo)
    {
        for (int i = 0; i < list.size(); i++)
        {
            Comment c = list.get(i);
            if (c.no == cNo && c.replyNo == rNo) return i;
        }
        return -1;
    }
}
