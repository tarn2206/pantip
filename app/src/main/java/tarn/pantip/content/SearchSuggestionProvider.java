package tarn.pantip.content;

import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.net.Uri;
import android.provider.SearchRecentSuggestions;

/**
 * User: Tarn
 * Date: 5/2/13 1:10 PM
 */
public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider
{
    final private static String AUTHORITY = SearchSuggestionProvider.class.getName();
    final private static int MODE = DATABASE_MODE_QUERIES;// | DATABASE_MODE_2LINES;

    public SearchSuggestionProvider()
    {
        setupSuggestions(AUTHORITY, MODE);
    }

    public static void clearHistory(Context context)
    {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.clearHistory();
    }

    public static void save(Context context, String query)
    {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.saveRecentQuery(query, null);
    }

    public static void delete(Context context, String query)
    {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/suggestions");
        context.getContentResolver().delete(uri, "display1=?", new String[] { query });
    }
}