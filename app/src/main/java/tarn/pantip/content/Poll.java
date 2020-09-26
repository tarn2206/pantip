package tarn.pantip.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.model.Choice;
import tarn.pantip.model.PollResult;
import tarn.pantip.model.Question;
import tarn.pantip.model.TopicEx;
import tarn.pantip.util.RxUtils;

/**
 * Created by Tarn on 9 February 2017
 */

public class Poll
{
    // 36084801 all
    // 35921534 open end
    // 35875084 closed with image
    // 35313475 table
    // 35752619, 35744188, 35744186 ranking
    public static boolean parse(Element element, TopicEx topic)
    {
        String className = element.className();
        if (className == null) return false;

        if (className.contains("can-vote"))
        {
            String s = element.text();
            if (s.contains("ปิดโหวต"))
            {
                if (s.contains("วันที่ 0")) s = s.replace("วันที่ 0", "วันที่ ");
                if (s.startsWith("*** ")) topic.deadline = s.substring(4);
                else topic.deadline = s;
            }
            else topic.pollRemark = s;
            return true;
        }
        if (className.contains("required-answer-poll"))
        {
            topic.requiredAnswer = true;
            return true;
        }
        if (className.contains("q-poll"))
        {
            Element titleElement = element.select(".que-item-title").first();
            Question q = topic.addQuestion(new Question(titleElement == null ? "" : titleElement.text()));

            int index = className.indexOf("qtype_");
            if (index != -1) q.type = Integer.parseInt(className.substring(index + 6, index + 7));

            Elements requiredMark = element.select(".required-mark");
            q.require = requiredMark.size() > 0;
            if (q.require && q.title.endsWith(" *")) q.title = q.title.substring(0, q.title.length() - 2);

            if (q.type == 1 || q.type == 2 || q.type == 6)
            {
                Element table = element.select(".post-que-table").first();
                if (table != null)
                {
                    Elements rows = table.select("tr");
                    for (Element tr : rows)
                    {
                        Choice c = q.addChoice(StringEscapeUtils.unescapeHtml4(tr.child(1).text()));
                        if (q.type == 1 || q.type == 2)
                        {
                            Element input = tr.child(0).select("input").first();
                            if (input != null)
                            {
                                c.id = input.val();
                                c.selected = input.hasAttr("checked");
                            }
                        }
                        else
                        {
                            int i = 0;
                            Elements choices = tr.child(0).select("option");
                            for (Element e : choices)
                            {
                                String text = StringEscapeUtils.unescapeHtml4(e.text());
                                if (text.length() > 9) text = String.valueOf(i);
                                Choice o = c.addChoice(e.val(), text);
                                if (e.hasAttr("selected")) o.selected = true;
                                i++;
                            }
                        }

                        Element img = tr.child(1).select("img").first();
                        if (img == null) img = tr.child(1).select("iframe").first();
                        if (img != null) c.image = img.attr("src");

                        Element other = tr.child(1).select("input").first();
                        if (other != null) c.other = other.val();
                    }
                }
            }
            else if (q.type == 3)
            {
                Elements choices = element.select("option");
                for (Element e : choices)
                {
                    Choice choice = q.addChoice(e.val(), StringEscapeUtils.unescapeHtml4(e.text()));
                    if (e.hasAttr("selected")) choice.selected = true;
                }
            }
            else if (q.type == 4)
            {
                Element table = element.select("table").first();
                if (table != null)
                {
                    Elements rows = table.select("tr");
                    Elements cols = rows.get(0).select("td");
                    int n = Integer.parseInt(cols.get(1).text());
                    cols = rows.get(1).select("td");
                    q.minText = StringEscapeUtils.unescapeHtml4(cols.get(0).text());
                    q.maxText = StringEscapeUtils.unescapeHtml4(cols.get(cols.size() - 1).text());
                    Elements inputs = rows.get(1).select("input");
                    for (Element e : inputs)
                    {
                        Choice c = q.addChoice(e.val(), String.valueOf(n++));
                        if (e.hasAttr("checked")) c.selected = true;
                    }
                }
            }
            /*else if (q.type == 5)
            {
                // TODO:
            }*/
            return true;
        }
        if (className.contains("button-container"))
        {
            topic.closeVote = element.children().size() == 0;
            topic.voted = element.text().equals("แก้ไขโพล");
            return true;
        }
        return className.equals("callback-status");
    }

    public static Observable<String> submit(long topicId, List<PollResult> results)
    {
        final StringBuilder s = new StringBuilder();
        s.append("result[topic_id]=").append(topicId);
        int qid = 0;
        for (PollResult result : results)
        {
            s.append("&result[vote][").append(qid).append("][question_id]=").append(qid);
            s.append("&result[vote][").append(qid).append("][poll_type]=").append(result.type);
            if (result.values != null)
            {
                int count = 0;
                for (String id : result.values)
                {
                    s.append("&result[vote][").append(qid).append("][result][]=").append(id);
                    if (StringUtils.isNotBlank(id)) count++;
                }
                if (result.type <= 4) count = 1;
                if (count > 0) s.append("&result[vote][").append(qid).append("][cnt]=").append(count);
            }
            if (result.other != null) s.append("&result[vote][").append(qid).append("][other]=").append(result.other);
            qid++;
        }
        return RxUtils.observe(() -> {
            Http.postAjax("https://pantip.com/forum/topic/check_permission_vote_poll", "id=" + topicId).execute();
            return Http.postAjax("https://pantip.com/forum/topic/add_polls_result", s.toString()).execute();
        });
    }

    public static Observable<String> getResult(long topicId)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/topic/" + topicId + "/result";
            return Http.getAjax(url).execute();
        });
    }

    static boolean parseResult(Element element, TopicEx topic)
    {
        String script = element.data();
        String key = "$.resultPoll.render(";
        int start = script.indexOf(key);
        if (start == -1) return false;
        int end = script.indexOf(");", start);
        script = script.substring(start + key.length(), end);
        if (script.equals("\"\"")) return false;

        JsonObject json = JsonParser.parseString(script).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet())
        {
            key = entry.getKey();
            int x = Integer.parseInt(key.substring("chart".length()));
            Question question = topic.questions.get(x);

            JsonObject chart = entry.getValue().getAsJsonObject();
            question.maxVote = chart.get("max_vote").getAsInt();
            JsonArray data = chart.getAsJsonArray("data");
            String type = chart.get("type").getAsString();
            if (type.equals("bar"))
            {
                for (JsonElement e : data)
                {
                    JsonArray a = e.getAsJsonArray();
                    int value = a.get(0).getAsInt();

                    String html = a.get(1).getAsString();
                    Element p = Jsoup.parseBodyFragment(html).body().child(0);
                    int i = Integer.parseInt(p.removeClass("pantip-plot-axis-item").className());
                    String text = p.text();
                    if (text.contains("...")) text = p.attr("title");
                    Choice choice = question.addChoice(i, text, value);

                    Element img = p.select("img").first();
                    if (img != null) choice.image = img.attr("src");
                }
            }
            else if (type.equals("stack"))
            {
                int[][] values = null;
                for (int i = 0; i < data.size(); i++)
                {
                    JsonArray a = data.get(i).getAsJsonArray();
                    if (values == null) values = new int[a.size()][data.size()];
                    for (int j = 0; j < a.size(); j++)
                    {
                        values[j][i] = a.get(j).getAsInt();
                    }
                }
                JsonArray name = chart.getAsJsonArray("name");
                for (int i = 0; i < name.size(); i++)
                {
                    Element p = Jsoup.parse(name.get(i).getAsString()).select("p").first();
                    Choice choice = question.addChoice(p.attr("title"));
                    if (values != null && i < values.length)
                    {
                        choice.values = values[i];
                    }
                    Element img = p.select("img").first();
                    if (img != null) choice.image = img.attr("src");
                }

                JsonArray legend = chart.getAsJsonArray("legend");
                question.legend = new String[legend.size()];
                for (int i = 0; i < legend.size(); i++)
                {
                    String s = legend.get(i).getAsString();
                    if (s.startsWith("<span "))
                    {
                        Element span = Jsoup.parse(s).select("span").first();
                        question.legend[i] = span.attr("title");
                    }
                    else question.legend[i] = s;
                }
            }
        }
        return true;
    }

    public static Observable<String> edit(long topicId)
    {
        return RxUtils.observe(() -> Http.getAjax("https://pantip.com/topic/" + topicId + "/edit").execute());
    }
}