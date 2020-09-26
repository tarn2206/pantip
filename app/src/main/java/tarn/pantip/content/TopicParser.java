package tarn.pantip.content;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.model.Comment;
import tarn.pantip.model.Emotion;
import tarn.pantip.model.LinkSpec;
import tarn.pantip.model.Story;
import tarn.pantip.model.StoryType;
import tarn.pantip.model.Tag;
import tarn.pantip.model.TopicEx;
import tarn.pantip.model.TopicType;
import tarn.pantip.util.Utils;

class TopicParser
{
    static List<Comment> load(TopicEx topic) throws IOException, ParseException
    {
        String content = Http.get("https://pantip.com/topic/" + topic.id).execute();
        Document doc = Jsoup.parse(content);
        Element span = doc.select("span.icon-mini-posttype").first();
        if (span == null)
        {
            Element div = doc.select("div.callback-status").first();
            if (div == null) topic.error = doc.html();
            else topic.deleteMessage = Utils.fromHtml(div.html());
            return null;
        }

        if (topic.type == null || topic.type == TopicType.All)
        {
            String s = span.attr("class");
            topic.type = TopicType.parse(s);
            if (topic.type == TopicType.All) L.w(s);
        }
        if (topic.type == TopicType.Poll)
        {
            if (topic.questions != null) topic.questions.clear();
            topic.deadline = null;
            topic.pollRemark = null;
            topic.requiredAnswer = false;
            topic.closeVote = false;
            topic.voted = false;
        }

        Element e = doc.select(".adminpost").first();
        if (e != null)
        {
            e = e.select(".display-post-story").first();
            if (e != null) topic.notify = StringUtils.strip(Utils.fromHtml(e.html()), "\n");
        }

        topic.follow = isFollow(doc);
        e = doc.select("a.btn-bookmarks").first();
        if (e != null) topic.favorite = e.hasClass("icon-fav");

        e = doc.select("h2.display-post-title").first();
        if (e == null)
        {
            throw new PantipException("https://pantip.com/topic/" + topic.id + "\n" + doc.outerHtml());
        }
        topic.title = e.text();

        if (topic.tags == null || topic.tags.isEmpty())
        {
            topic.tags = parseTags(doc, topic.tags);
        }

        e = doc.select("div.display-post-story").first();
        parseStory(topic, e, true);
        removeDuplicateLink(topic.storyList);

        Element script = e.select("script").first();
        if (topic.type == TopicType.Poll && script != null)
        {
            topic.hasResult = Poll.parseResult(script, topic);
        }

        parseAuthor(doc, topic);
        parseStat(doc, topic);

        List<Comment> results = splitComment(new ArrayList<>(), topic);
        if (results.size() > 0)
        {
            results.get(results.size() - 1).hasNext = false;
        }
        return results;
    }

    private static boolean isFollow(Document doc)
    {
        Elements follow = doc.select("input[name=follow_topic]");
        if (follow != null)
        {
            for (Element e : follow)
            {
                if ("checked".equals(e.attr("checked")))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Tag> parseTags(Document doc, List<Tag> tags)
    {
        Element e = doc.select(".display-post-tag-wrapper").first();
        if (e == null) return tags;

        if (tags == null) tags = new ArrayList<>();
        Elements elements = e.children();
        for (Element a : elements)
        {
            String url = a.attr("href");
            if (url.startsWith("/tag/")) url = url.substring(5);
            tags.add(new Tag(a.text(), url));
        }
        return tags;
    }

    private static void parseAuthor(Document doc, TopicEx topic) throws ParseException
    {
        Element e = doc.select("a.owner").first();
        if (e != null)
        {
            topic.mid = Integer.parseInt(e.attr("id"));
            if (topic.author == null)
            {
                topic.author = StringEscapeUtils.unescapeHtml4(e.text());
            }
        }
        e = doc.select(".display-post-avatar img").first();
        if (e != null)
        {
            topic.avatar = e.attr("src");
            if (topic.avatar != null)
            {
                if (topic.avatar.endsWith("/unknown-avatar-38x38.png")) topic.avatar = null;
                else topic.avatar = topic.avatar.replace("_m.jpg", "_l.jpg");
            }
        }
        if (topic.time == null)
        {
            e = doc.select("abbr.timeago").first();
            if (e != null)
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
                topic.setTime(dateFormat.parse(e.attr("data-utime")));
            }
        }
    }

    private static void parseStat(Document doc, TopicEx topic)
    {
        Element e = doc.select("div.display-post-vote").first();
        int votes = Integer.parseInt(e.child(0).text());
        topic.liked = e.child(1).hasClass("i-vote");

        topic.setCommentStat(votes, parseEmotion(doc));
    }

    static void parseStory(Comment comment, Element element, boolean stripText) throws UnsupportedEncodingException
    {
        if (comment.storyList == null) comment.storyList = new ArrayList<>();
        else comment.storyList.clear();
        if (element == null) return;

        TopicEx topic = (comment instanceof TopicEx) ? (TopicEx)comment : null;
        StringBuilder s = new StringBuilder();
        List<LinkSpec> spans = new ArrayList<>();
        List<Node> children = element.childNodes();
        for (Node node : children)
        {
            if (node instanceof TextNode)
            {
                String text = getText(node, stripText);
                if (text.length() > 0) s.append(text);
                continue;
            }

            if (!(node instanceof Element)) continue;

            Element child = (Element)node;
            if (child.tagName().equals("div") || child.tagName().equals("span"))
            {
                if (topic != null)
                {
                    if ("review-section".equals(child.className()))
                    {
                        Element strong = child.select("strong").first();
                        topic.reviewProduct = strong.text();
                        Element input = child.select("input[checked]").first();
                        topic.reviewRating = input == null ? 0 : Float.parseFloat(input.attr("value"));
                        Element iframe = child.select("div.review-map > iframe").first();
                        if (iframe != null) topic.mapsUrl = iframe.attr("src");
                        continue;
                    }
                    if (topic.type == TopicType.Poll)
                    {
                        if (Poll.parse(child, topic)) continue;
                    }
                }
                if ("edit-history".equals(child.className()))
                {
                    addText(comment.storyList, s, spans);
                    Element abbr = child.select("abbr").first();
                    if (abbr == null) comment.edit = child.text();
                    else
                    {
                        try
                        {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
                            comment.setEditTime(dateFormat.parse(abbr.attr("data-utime")));
                            comment.edit = null;
                        }
                        catch (ParseException e)
                        {
                            String t = abbr.attr("title").replace("  ", " ");
                            comment.edit = child.text() + " " + (t.startsWith("0") ? t.substring(1) : t);
                        }
                    }
                    continue;
                }
            }

            if (!child.tagName().equals("script"))
            {
                parseElement(child, comment.storyList, s, spans, false, stripText);
            }
        }
        addText(comment.storyList, s, spans);
    }

    private static void parseElement(Element element, List<Story> list, StringBuilder s, List<LinkSpec> spans,
                                     boolean inSpoil, boolean stripText) throws UnsupportedEncodingException
    {
        if (element.tagName().equals("a"))
        {
            if ("spoil-btn".equals(element.className())) return;

            CharSequence text = element.text();
            if (text.length() > 0)
            {
                try
                {
                    text = Utils.trim(URLDecoder.decode(text.toString(), "UTF-8"));
                }
                catch (Exception e)
                {/*ignored*/}

                String href = Utils.trim(element.attr("href"));
                if (href.startsWith("http://") || href.startsWith("https://"))
                {
                    if (href.contains("youtube.com/watch") || href.contains("youtube.com/embed/") || href.contains("youtu.be/"))
                    {
                        addText(list, s, spans);
                        list.add(new Story(StoryType.YouTube, href));
                    }
                    else
                    {
                        LinkSpec si = new LinkSpec(LinkSpec.SpanType.Url, href, s.length());
                        s.append(text);
                        si.end = s.length();
                        spans.add(si);
                    }
                }
                else s.append(text);
            }
            for (Element child : element.children())
            {
                parseElement(child, list, s, spans, false, stripText);
            }
        }
        else if (element.tagName().equals("b") || element.tagName().equals("i") || element.tagName().equals("u"))
        {
            int start = s.length();
            for (Node node : element.childNodes())
            {
                if (node instanceof TextNode)
                {
                    String text = getText(node, stripText);
                    if (text.length() > 0) s.append(text);
                }
                else if (node instanceof Element)
                {
                    parseElement((Element)node, list, s, spans, false, stripText);
                }
            }
            if (start >= s.length()) return;
            LinkSpec.SpanType type;
            if (element.tagName().equals("b")) type = LinkSpec.SpanType.Bold;
            else if (element.tagName().equals("i")) type = LinkSpec.SpanType.Italic;
            else type = LinkSpec.SpanType.Underline;
            LinkSpec si = new LinkSpec(type, start);
            si.end = s.length();
            spans.add(si);
        }
        else if ("span".equals(element.tagName()) && "spoil-style".equals(element.className()))
        {
            if (inSpoil)
            {
                parseSpoil(element, list, s, spans);
            }
            else
            {
                addText(list, s, spans);
                List<Story> spoil = parseSpoil(element, new ArrayList<>(), new StringBuilder(), new ArrayList<>());
                list.add(new Story(spoil));
            }
        }
        else if (element.tagName().equals("img"))
        {
            String src = element.attr("src");
            if (Emoticons.isEmoticon(src))
            {
                String title = element.attr("title");
                if (title == null || title.length() == 0)
                {
                    title = element.attr("alt");
                    if (title == null || title.length() == 0)
                    {
                        int x = src.lastIndexOf('/') + 1;
                        int y = src.indexOf('.', x);
                        title = src.substring(x, y);
                    }
                }
                LinkSpec si = new LinkSpec(LinkSpec.SpanType.Emoticon, src, s.length());
                s.append("<").append(title).append(">");
                si.end = s.length();
                spans.add(si);
            }
            else
            {
                while (s.length() > 0 && s.charAt(s.length() - 1) == '\n') s.setLength(s.length() - 1);
                addText(list, s, spans);
                if (src.contains("img.youtube.com")) list.add(new Story(StoryType.YouTube, src));
                else list.add(new Story(StoryType.Image, src));
            }
        }
        else if (element.tagName().equals("iframe"))
        {
            String src = element.attr("src");
            if (src.contains("youtube.com/"))
            {
                addText(list, s, spans);
                list.add(new Story(StoryType.YouTube, src));
            }
            else
            {
                LinkSpec si = new LinkSpec(LinkSpec.SpanType.Url, src, s.length());
                s.append(src);
                si.end = s.length();
                spans.add(si);
            }
        }
        else if (element.tagName().equals("br")) s.append('\n');
        else if (element.childNodeSize() > 0)
        {
            for (Node node : element.childNodes())
            {
                if (node instanceof TextNode)
                {
                    String text = getText(node, stripText);
                    if (text.length() > 0) s.append(text);
                }
                else if (node instanceof Element) parseElement((Element)node, list, s, spans, false, stripText);
            }
        }
        else s.append(StringEscapeUtils.unescapeHtml4(element.text()));
    }

    private static List<Story> parseSpoil(Element element, List<Story> list, StringBuilder s, List<LinkSpec> spans)
            throws UnsupportedEncodingException
    {
        for (Node child : element.childNodes())
        {
            if (child instanceof TextNode)
            {
                String text = getText(child, false);
                try
                {
                    text = URLDecoder.decode(text, "UTF-8");
                }
                catch (Exception e)
                {/*ignored*/}
                s.append(text);
                continue;
            }

            if (child instanceof Element) parseElement((Element)child, list, s, spans, true, false);
        }
        addText(list, s, spans);
        return list;
    }

    private static Emotion parseEmotion(Document doc)
    {
        Emotion emotions = new Emotion();
        Element e = doc.select("span.emotion-score").first();
        emotions.total = e == null ? -1 : Integer.parseInt(e.text());
        e = doc.select("div.emotion-vote-user").first();
        if (e != null && Pantip.loggedOn)
        {
            for (int i = 0; i < e.childNodeSize() - 1; i++)
            {
                Node node = e.childNode(i);
                if (node.nodeName().equals("a"))
                {
                    Element a = (Element)node;
                    if (Pantip.currentUser != null && StringUtils.equals(a.text(), Pantip.currentUser.name))
                    {
                        Node c = e.childNode(i + 1);
                        if (c == null) break;
                        String s = c.toString();
                        if (s.contains("ถูกใจ")) emotions.like.selected = true;
                        else if (s.contains("ขำกลิ้ง")) emotions.laugh.selected = true;
                        else if (s.contains("หลงรัก")) emotions.love.selected = true;
                        else if (s.contains("ซึ้ง")) emotions.impress.selected = true;
                        else if (s.contains("สยอง")) emotions.scary.selected = true;
                        else if (s.contains("ทึ่ง")) emotions.surprised.selected = true;
                        break;
                    }
                }
            }
        }
        return emotions;
    }

    private static void addText(List<Story> list, StringBuilder s, List<LinkSpec> spans)
    {
        if (s.length() == 0) return;

        int shiftStart = 0;
        if (list.size() > 0 && list.get(list.size() - 1).type == StoryType.Image && s.charAt(0) == '\n')
        {
            s.deleteCharAt(0);
            shiftStart--;
            if (s.length() == 0) return;
        }
        if (s.length() == 0) return;
        if (s.charAt(s.length() - 1) == '\n')
        {
            s.deleteCharAt(s.length() - 1);
            if (s.length() == 0) return;
        }
        List<LinkSpec> a = null;
        if (spans.size() > 0)
        {
            for (LinkSpec si : spans)
            {
                si.start += shiftStart;
                if (si.start < 0) si.start = 0;
                si.end += shiftStart;
                if (si.end < si.start) si.end = si.start + 1;
                if (si.end > s.length()) si.end = s.length();
            }
            a = new ArrayList<>(spans);
            spans.clear();
        }
        list.add(new Story(StoryType.Text, s.toString(), a));
        s.setLength(0);
    }

    private static String getText(Node node, boolean stripText)
    {
        TextNode textNode = (TextNode)node;
        String text = textNode.getWholeText();
        if (stripText) text = strip(text);
        //else if (text.charAt(0) == ' ') text = text.substring(1);
        return text.length() == 0 ? text : StringEscapeUtils.unescapeHtml4(text);
    }

    private static String strip(String text)
    {
        int start = 0;
        if (text.charAt(0) == '\n') start = 1;
        if (text.charAt(0) == '\r' && text.charAt(1) == '\n') start = 2;
        while (start < text.length() && text.charAt(start) == '\t') start++;
        if (start < text.length())
        {
            int end = text.length();
            while (end > 0 && text.charAt(end - 1) == '\t') end--;
            if (end > start) return text.substring(start, end);
        }
        return "";
    }

    static void removeDuplicateLink(List<Story> storyList)
    {
        for (Story story1 : storyList)
        {
            if (story1.type == StoryType.YouTube && !story1.text.contains("/vi/"))
            {
                for (Story story2 : storyList)
                {
                    if (story2.type == StoryType.YouTube && story2 != story1
                            && StringUtils.equals(Utils.getYouTubeId(story1.text), Utils.getYouTubeId(story2.text)))
                    {
                        story1.type = StoryType.Text;
                        story1.spans = Collections.singletonList(createLinkSpec(story1.text));
                        break;
                    }
                }
            }
            else if (story1.type == StoryType.Maps && story1.text.contains("/@"))
            {
                for (Story story2 : storyList)
                {
                    if (story2.type == StoryType.Maps && story2 != story1
                            && StringUtils.equals(Utils.getMapLocation(story1.text), Utils.getMapLocation(story2.text)))
                    {
                        story1.type = StoryType.Text;
                        story1.spans = Collections.singletonList(createLinkSpec(story1.text));
                        break;
                    }
                }
            }
        }
    }

    private static LinkSpec createLinkSpec(String text)
    {
        LinkSpec info = new LinkSpec(LinkSpec.SpanType.Url, text, 0);
        info.end = text.length();
        return info;
    }

    static List<Comment> splitComment(List<Comment> results, Comment comment)
    {
        if (countImage(comment.storyList) <= 2)
        {
            results.add(comment);
            return results;
        }

        Comment newComment = null;
        boolean hasPrev = false;
        List<Story> newStory = new ArrayList<>();
        int imageCount = 0;
        for (Story story : comment.storyList)
        {
            newStory.add(story);
            if (story.type == StoryType.Image || story.type == StoryType.YouTube) imageCount++;
            if (imageCount == 2)
            {
                if (newComment != null) newComment.hasNext = true; // update in list
                newComment = comment.createCopy();
                newComment.hasPrev = hasPrev;
                newComment.storyList = newStory;
                results.add(newComment);

                imageCount = 0;
                hasPrev = true;
                newStory = new ArrayList<>();
            }
        }
        if (newStory.size() > 0)
        {
            if (newComment != null) newComment.hasNext = true; // update in list
            newComment = comment.createCopy();
            newComment.hasPrev = hasPrev;
            newComment.storyList = newStory;
            results.add(newComment);
        }
        return results;
    }

    private static int countImage(List<Story> stories)
    {
        if (stories == null) return 0;
        int imageCount = 0;
        for (Story story : stories)
        {
            if (story.type == StoryType.Image || story.type == StoryType.YouTube) imageCount++;
        }
        return imageCount;
    }
}
