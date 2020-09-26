package tarn.pantip.app;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;

import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.util.Utils;

/**
 * User: tarn
 * Date: 2/13/13 6:23 PM
 */
public class WebViewActivity extends BaseActivity
{
    private WebView webView;
    private String url;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        setTitle("");
        final ProgressBar progress = findViewById(android.R.id.progress);
        setProgressDrawable(progress);

        url = savedInstanceState == null ? getIntent().getStringExtra("url") : savedInstanceState.getString("url");
        if (url != null && (url.startsWith("https://") || url.startsWith("http://")))
        {
            int i = url.indexOf("://") + 3;
            int j = url.indexOf('/', i + 1);
            if (j == -1) j = url.length();
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) actionBar.setSubtitle(url.substring(i, j));
        }

        webView = findViewById(R.id.web_view);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        WebSettings settings = webView.getSettings();
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        if (url.startsWith("file://"))
        {
            AppBarLayout.LayoutParams a = (AppBarLayout.LayoutParams)((View)toolbar.getParent()).getLayoutParams();
            a.setScrollFlags(0);
            //CoordinatorLayout.LayoutParams c = (CoordinatorLayout.LayoutParams)((View)webView.getParent()).getLayoutParams();
            //c.setBehavior(null);
        }
        else
        {
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
        }
        webView.setWebChromeClient(new WebChromeClient()
        {
            @Override
            public void onProgressChanged(WebView view, int newProgress)
            {
                progress.setProgress(newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title)
            {
                setTitle(title);
            }
        });
        webView.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                postDelayed(() -> progress.setVisibility(View.GONE), 500);
            }

            /*@Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }*/
        });
        if (savedInstanceState == null) webView.loadUrl(url);
        else webView.restoreState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("url", url);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed()
    {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (url.startsWith("file://")) return super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.web, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_open)
        {
            try
            {
                Utils.openBrowser(this, url);
                finish();
                return true;
            }
            catch (Exception e)
            {
                Pantip.handleException(this, e);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setProgressDrawable(ProgressBar progress)
    {
        int color = ContextCompat.getColor(this, R.color.accent_color_pantip);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0x3FFFFFFF);
        GradientDrawable secondaryProgress = new GradientDrawable();
        secondaryProgress.setColor(Pantip.textColorHint);
        GradientDrawable progressDrawable = new GradientDrawable();
        progressDrawable.setColor(color);
        Drawable[] layers = new Drawable[]
                {
                        background,
                        new ScaleDrawable(secondaryProgress, 3, 1, -1),
                        new ScaleDrawable(progressDrawable, 3, 1, -1),
                };
        LayerDrawable layer = new LayerDrawable(layers);
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.secondaryProgress);
        layer.setId(2, android.R.id.progress);
        progress.setProgressDrawable(layer);
        progress.getLayoutParams().height = Utils.toPixels(2);
    }
}