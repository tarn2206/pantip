package tarn.pantip.util;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import tarn.pantip.R;
import tarn.pantip.widget.SuggestionsAdapter;

/**
 * Created by Tarn on 29 August 2016
 */
public class Search
{
    public static MenuItem setup(AppCompatActivity activity, Menu menu)
    {
        MenuItem searchMenu = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchMenu.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);

        SearchManager searchManager = (SearchManager)activity.getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null)
        {
            SearchableInfo searchable = searchManager.getSearchableInfo(activity.getComponentName());
            if (searchable != null) searchView.setSearchableInfo(searchable);
            searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            searchView.setSuggestionsAdapter(new SuggestionsAdapter(activity, searchView, searchable));
        }

        searchView.setOnQueryTextListener(new QueryTextListener(searchMenu));
        searchView.setOnSuggestionListener(new SuggestionListener(searchMenu));

        return searchMenu;
    }

    private static class QueryTextListener implements SearchView.OnQueryTextListener
    {
        private final MenuItem searchMenu;

        private QueryTextListener(MenuItem searchMenu)
        {
            this.searchMenu = searchMenu;
        }

        @Override
        public boolean onQueryTextChange(String paramString)
        {
            return false;
        }

        @Override
        public boolean onQueryTextSubmit(String paramString)
        {
            searchMenu.collapseActionView();
            return false;
        }
    }

    private static class SuggestionListener implements SearchView.OnSuggestionListener
    {
        private final MenuItem searchMenu;

        private SuggestionListener(MenuItem searchMenu)
        {
            this.searchMenu = searchMenu;
        }

        @Override
        public boolean onSuggestionClick(int position)
        {
            searchMenu.collapseActionView();
            return false;
        }

        @Override
        public boolean onSuggestionSelect(int paramInt)
        {
            return false;
        }
    }
}