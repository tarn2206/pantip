package tarn.pantip.widget;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.ResourceCursorAdapter;

import java.lang.reflect.Method;

import tarn.pantip.L;
import tarn.pantip.R;
import tarn.pantip.app.ConfirmDialog;
import tarn.pantip.content.SearchSuggestionProvider;

/**
 * Created by Tarn on 31/10/2015.
 */
public class SuggestionsAdapter extends ResourceCursorAdapter
{
    private static final int QUERY_LIMIT = 50;
    private final AppCompatActivity activity;
    private final SearchView searchView;
    private final SearchableInfo searchable;
    private final AutoCompleteTextView autoCompleteTextView;
    private final Method doBeforeTextChanged;
    private final Method doAfterTextChanged;
    private int columnText1 = -1;

    public SuggestionsAdapter(AppCompatActivity activity, SearchView searchView, SearchableInfo searchable)
    {
        super(activity, R.layout.suggestion_item, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        this.activity = activity;
        this.searchView = searchView;
        this.searchable = searchable;
        autoCompleteTextView = searchView.findViewById(com.google.android.material.R.id.search_src_text);
        doBeforeTextChanged = getAutoCompleteTextViewMethod("doBeforeTextChanged");
        doAfterTextChanged = getAutoCompleteTextViewMethod("doAfterTextChanged");
    }

    private static Method getAutoCompleteTextViewMethod(String name)
    {
        try
        {
            Method method = AutoCompleteTextView.class.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    private void invoke(View view, Method method)
    {
        if (method == null) return;
        try
        {
            method.invoke(view);
        }
        catch (Exception e)
        {/*ignored*/}
    }

    private void forceSuggestionQuery()
    {
        invoke(autoCompleteTextView, doBeforeTextChanged);
        invoke(autoCompleteTextView, doAfterTextChanged);
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint)
    {
        String query = (constraint == null) ? "" : constraint.toString();
        /*
         * for in app search we show the progress spinner until the cursor is returned with the results.
         */
        Cursor cursor;
        if (searchView.getVisibility() != View.VISIBLE || searchView.getWindowVisibility() != View.VISIBLE)
        {
            return null;
        }
        try
        {
            cursor = getSearchManagerSuggestions(searchable, query, QUERY_LIMIT);
            // trigger fill window so the spinner stays up until the results are copied over and
            // closer to being ready
            if (cursor != null)
            {
                cursor.getCount();
                return cursor;
            }
        }
        catch (RuntimeException e)
        {
            L.e(e, "Search suggestions query threw an exception.");
        }
        // If cursor is null or an exception was thrown, stop the spinner and return null.
        // changeCursor doesn't get called if cursor is null
        return null;
    }

    @Override
    public void changeCursor(Cursor cursor)
    {
        super.changeCursor(cursor);
        columnText1 = cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        final View view = super.newView(context, cursor, parent);
        view.setTag(new ViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor)
    {
        if (columnText1 == -1) columnText1 = cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1);
        final String query = cursor.getString(columnText1);
        final ViewHolder views = (ViewHolder)view.getTag();
        views.text.setText(query);
        views.text.setOnClickListener(v -> searchView.setQuery(query, true));
        views.text.setOnLongClickListener(v -> {
            SpannableString message = new SpannableString("ลบ " + query + " ออกจากประวัติการค้นหา?");
            message.setSpan(new StyleSpan(Typeface.BOLD), 3, 3 + query.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            ConfirmDialog.delete(activity, message, (dialog, which) -> {
                SearchSuggestionProvider.delete(activity, query);
                forceSuggestionQuery();
            }, (dialog, which) -> forceSuggestionQuery());
            return false;
        });
        views.refine.setOnClickListener(v -> searchView.setQuery(query, false));
    }

    private static class ViewHolder
    {
        final TextView text;
        final ImageView refine;

        ViewHolder(View view)
        {
            text = view.findViewById(android.R.id.text1);
            refine = view.findViewById(R.id.edit_query);
        }
    }

    private Cursor getSearchManagerSuggestions(SearchableInfo searchable, String query, int limit)
    {
        if (searchable == null) return null;

        String authority = searchable.getSuggestAuthority();
        if (authority == null) return null;

        Uri.Builder uriBuilder = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                                                  .authority(authority).query("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
                                                  .fragment("");  // TODO: Remove, workaround for a bug in Uri.writeToParcel()

        // if content path provided, insert it now
        final String contentPath = searchable.getSuggestPath();
        if (contentPath != null) uriBuilder.appendEncodedPath(contentPath);

        // append standard suggestion query path
        uriBuilder.appendPath(SearchManager.SUGGEST_URI_PATH_QUERY);

        // get the query selection, may be null
        String selection = searchable.getSuggestSelection();
        // inject query, either as selection args or inline
        String[] selArgs = null;
        if (selection != null)
        {    // use selection if provided
            selArgs = new String[] { query };
        }
        else
        {                    // no selection, use REST pattern
            uriBuilder.appendPath(query);
        }

        if (limit > 0)
        {
            uriBuilder.appendQueryParameter("limit", String.valueOf(limit));
        }

        Uri uri = uriBuilder.build();
        // finally, make the query
        return activity.getContentResolver().query(uri, null, selection, selArgs, null);
    }
}