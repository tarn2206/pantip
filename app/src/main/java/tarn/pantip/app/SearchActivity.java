package tarn.pantip.app;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import androidx.annotation.NonNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.Http;
import tarn.pantip.content.Json;
import tarn.pantip.content.Preferences;
import tarn.pantip.content.SearchSuggestionProvider;
import tarn.pantip.model.SearchResult;
import tarn.pantip.model.SearchResultItem;
import tarn.pantip.model.SpanInfo;
import tarn.pantip.model.SpanText;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Search;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.GoogleSearchAdapter;

/**
 * User: Tarn
 * Date: 5/1/13 8:53 PM
 */
public class SearchActivity extends RecyclerActivity<SearchResult>
{
    private static final int NUM_RESULT = 20;
    private String query;
    private GoogleSearchAdapter adapter;
    private SearchResult result;
    private int start;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Preferences preferences = new Preferences(this, savedInstanceState);
        query = preferences.getString("query");
        if (query == null)
        {
            onNewIntent(getIntent());
            return;
        }
        setTitle(query);
        String json = preferences.getString("result");
        if (json == null)
        {
            onNewIntent(getIntent());
            return;
        }
        try
        {
            result = Json.fromJson(json, SearchResult.class);
        }
        catch (Exception e)
        {/*ignored*/}
        finally
        {
            if (result == null) onNewIntent(getIntent());
            else complete(result);
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        query = intent.getStringExtra(SearchManager.QUERY);
        SearchSuggestionProvider.save(this, query);
        setTitle(query);
        if (adapter != null)
        {
            adapter.clear();
            adapter.notifyDataSetChanged();
        }
        recycler.emptyText = "การค้นหาของคุณ - " + query + " - ไม่ตรงกับกระทู้ใดๆ";
        recycler.showProgress();
        search(query, 0).subscribe(this::complete, this::error);

        super.onNewIntent(intent);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        saveInstanceState(new Preferences(outState));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        saveInstanceState(new Preferences(this));
    }

    private void saveInstanceState(Preferences preferences)
    {
        preferences.putString("query", query);
        if (result != null)
        {
            result.items = adapter.getItems();
            preferences.putString("result", Json.toJson(result));
        }
    }

    @Override
    protected boolean onLoad(Bundle savedInstanceState)
    {
        if (result != null) complete(result);
        return result != null;
    }

    @Override
    public void onRefresh()
    {
        search(query, start = 0).subscribe(this::complete, this::error);
    }

    @Override
    public void onLoadMore()
    {
        search(query, start).subscribe(this::complete, this::error);
    }

    @Override
    protected boolean onFinish(boolean isRefreshing, SearchResult result)
    {
        if (result == null) return false;
        if (adapter == null) setAdapter(adapter = new GoogleSearchAdapter(this));
        else if (isRefreshing) adapter.clear();
        this.result = result;

        boolean hasMore = false;
        if (result.items != null)
        {
            start += NUM_RESULT;
            hasMore = result.items.size() >= NUM_RESULT - 1;
            adapter.appendItems(result.items, hasMore);
            adapter.notifyDataSetChanged();
        }
        return hasMore;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.search, menu);
        Search.setup(this, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private Observable<SearchResult> search(final String q, int start)
    {
        return RxUtils.observe(() -> {
            if (StringUtils.isBlank(q))
            {
                return new SearchResult();
            }

            String query;
            try
            {
                query = URLEncoder.encode(q, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                query = q;
            }
            String url = "https://www.google.co.th/search?q=" + query + "+site:pantip.com&start=" + start + "&num=" + NUM_RESULT;
            String html = Http.get(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.8,th;q=0.6")
                    .header("referer", "https://www.google.co.th/")
                    .execute();
            return parseSearchResult(html, q);
        });
    }

    @NonNull
    private SearchResult parseSearchResult(String html, String q)
    {
        SearchResult result = new SearchResult();
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select("div.srg");
        if (elements.size() == 0)
        {
            logHtml(q, html);
            return result;
        }
        elements = elements.get(0).select("div.g");
        if (elements.size() == 0)
        {
            logHtml(q, html);
            return result;
        }

        result.items = new ArrayList<>();
        for (Element e : elements)
        {
            Element a = e.select("a").first();
            if (a == null)
            {
                logHtml(q, html);
                break;
            }
            SearchResultItem item = new SearchResultItem(a.attr("href"));
            Element title = e.select("h3").first();
            if (title != null) item.title = spanTitle(title.text(), q);
            else
            {
                logHtml(q, html);
                break;
            }

            Element st = e.select("span.st").first();
            if (st == null)
            {
                logHtml(q, html);
                break;
            }
            String content = Jsoup.clean(st.html(), "", Whitelist.none().addTags("em"));
            item.content = span(content);
            result.items.add(item);
        }
        return result;
    }

    private void logHtml(String q, String html)
    {
        try
        {
            String fileName = "search-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(new Date());
            File file = new File(Utils.getFileDir(this), fileName + ".html");
            FileUtils.write(file, html, StandardCharsets.UTF_8);
            file = new File(Utils.getFileDir(this), fileName + ".txt");
            FileUtils.write(file, q, StandardCharsets.UTF_8);
            L.e("parse error " + file.getAbsolutePath());
        }
        catch (Exception e)
        {
            L.e(e);
        }
    }

    private SpanText spanTitle(String text, String q)
    {
        text = StringEscapeUtils.unescapeHtml4(StringEscapeUtils.unescapeHtml4(text));
        List<SpanInfo> list = new ArrayList<>();
        int start = 0;
        while ((start = StringUtils.indexOfIgnoreCase(text, q, start)) != -1)
        {
            int end = start + q.length();
            list.add(new SpanInfo(start, end, Pantip.colorAccent));
            start = end;
        }
        SpanText result = new SpanText();
        result.text = text;
        result.spans = list;
        return result;
    }

    private static SpanText span(String text)
    {
        StringBuilder s = new StringBuilder(StringEscapeUtils.unescapeHtml4(StringEscapeUtils.unescapeHtml4(text)));
        List<SpanInfo> list = new ArrayList<>();
        int start;
        while ((start = s.indexOf("<em>")) != -1)
        {
            s.delete(start, start + "<em>".length());
            int end = s.indexOf("</em>");
            if (end == -1) continue;
            s.delete(end, end + "</em>".length());
            list.add(new SpanInfo(start, end, Pantip.colorAccent));
        }
        SpanText result = new SpanText();
        result.text = s.toString();
        result.spans = list;
        return result;
    }
}
