package tarn.pantip.widget;

import android.content.DialogInterface;

import tarn.pantip.Pantip;
import tarn.pantip.app.MainActivity;
import tarn.pantip.store.DataStore;

/**
 * User: Tarn
 * Date: 8/24/13 1:56 PM
 */
public class ResetFavoriteClickListener implements DialogInterface.OnClickListener
{
    private final MainActivity context;
    private final String forum;
    private final String tag;
    private final String url;
    private final DataStore store;

    public ResetFavoriteClickListener(MainActivity context, String forum)
    {
        this(context, forum, null, null);
    }

    public ResetFavoriteClickListener(MainActivity context, String forum, String tag, String url)
    {
        this.context = context;
        this.forum = forum;
        this.tag = tag;
        this.url = url;
        store = Pantip.getDataStore();
    }

    @Override
    public void onClick(DialogInterface dialog, int which)
    {
        if (tag == null)
        {
            store.resetForumFavorite(forum);
            context.favoriteForumChanged();
        }
        else
        {
            store.updateTagFavorite(forum, tag, url, false);
            context.favoriteTagChanged();
        }
    }
}