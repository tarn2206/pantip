package tarn.pantip.app;

import android.content.ClipData;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.internal.functions.Functions;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.R;
import tarn.pantip.content.FilePart;
import tarn.pantip.content.Gallery;
import tarn.pantip.util.Utils;
import tarn.pantip.widget.ContentLoadingProgressBar;
import tarn.pantip.widget.MyGalleryAdapter;
import tarn.pantip.widget.MyGalleryView;

/**
 * Created by Tarn on 27 August 2017
 */

public class MyGalleryActivity extends BaseActivity implements MyGalleryView.OnSelectListener
{
    private final int REQUEST_IMAGE_PICKER = 2;
    private MenuItem menuDelete;
    private MyGalleryView gallery;
    private final List<Uri> uploadQueue = new ArrayList<>();
    private boolean uploading;
    private Snackbar snackbar;
    private final List<String> inserted = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_gallery);

        gallery = findViewById(R.id.gallery);
        gallery.setOnSelectListener(this);
        ContentLoadingProgressBar progressBar = findViewById(android.R.id.progress);
        progressBar.setVisibility(View.GONE);
        gallery.load(progressBar);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        gallery.calcItemHeight(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.my_gallery, menu);
        menuDelete = menu.findItem(R.id.action_delete);
        menuDelete.setIcon(getMenuIcon(R.drawable.ic_delete_black_24dp));
        menuDelete.setEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            setResult();
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.action_delete)
        {
            ConfirmDialog.delete(MyGalleryActivity.this, "ลบรูปภาพที่เลือก", (dialog, which) -> deletePicture());
        }
        return super.onOptionsItemSelected(item);
    }

    private void setResult()
    {
        if (inserted.size() == 0) return;

        Intent data = new Intent();
        data.putExtra("inserted", true);
        setResult(RESULT_OK, data);
    }

    @Override
    public void onBackPressed()
    {
        setResult();
        super.onBackPressed();
    }

    @Override
    public void onSelectChanged(int n)
    {
        menuDelete.setEnabled(n > 0);
    }

    public void pickImages(View v)
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMAGE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_PICKER && resultCode == RESULT_OK)
        {
            if (data.getData() != null)
            {
                uploadQueue.add(data.getData());
            }
            else if (data.getClipData() != null)
            {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++)
                {
                    uploadQueue.add(clipData.getItemAt(i).getUri());
                }
            }
            if (!uploading)
            {
                uploading = true;
                startUpload();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startUpload()
    {
        if (uploadQueue.size() == 0)
        {
            uploading = false;
            return;
        }

        Uri uri = uploadQueue.remove(0);
        Bitmap bm = null;
        try
        {
            bm = getBitmap(uri);
            FilePart filePart = reduceFileSize(bm, uri);
            uploadStatus(filePart);
            uploadPicture(filePart);
        }
        catch (Exception e)
        {
            L.e(e);
            Utils.showToast(MyGalleryActivity.this, e.getMessage(), Toast.LENGTH_LONG, false);
        }
        finally
        {
            if (bm != null) bm.recycle();
        }
    }

    private void uploadStatus(FilePart filePart)
    {
        if (snackbar != null) snackbar.dismiss();
        SpannableString s = new SpannableString("อัปโหลดไฟล์ " + filePart.file.getName());
        s.setSpan(new ForegroundColorSpan(0xFFFFFFFF), 0, s.length(), 0);
        snackbar = Snackbar.make(gallery, s, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void uploadPicture(FilePart filePart)
    {
        Gallery.uploadPicture(filePart)
                .subscribe(json -> uploadComplete(filePart, json), tr -> uploadFailed(filePart, tr));
    }

    public void uploadComplete(FilePart filePart, JsonObject json)
    {
        FileUtils.deleteQuietly(filePart.file);
        if (json.has("error"))
        {
            snackbar.dismiss();
            Utils.showToast(MyGalleryActivity.this, json.get("error").getAsString(), Toast.LENGTH_LONG, false);
        }
        else
        {
            try
            {
                File dir = Gallery.getDataFile();
                dir.setLastModified(dir.lastModified() - Pantip.REFRESH_TIME);
            }
            catch (IOException e)
            {
                L.e(e);
            }
            gallery.reload();
            snackbar.dismiss();
            inserted.add(getKey(json.get("path").getAsString()));
        }
        startUpload();
    }

    public void uploadFailed(FilePart filePart, Throwable tr)
    {
        FileUtils.deleteQuietly(filePart.file);
        snackbar.dismiss();
        L.e(tr);
        Utils.showToast(MyGalleryActivity.this, tr.getMessage());
        startUpload();
    }

    private Bitmap getBitmap(Uri uri) throws IOException
    {
        Bitmap bm;
        try (InputStream in = getContentResolver().openInputStream(uri))
        {
            bm = BitmapFactory.decodeStream(in);
        }
        int max = 1500;
        if (bm.getWidth() > max || bm.getHeight() > max)
        {
            int width, height;
            if (bm.getWidth() >= bm.getHeight())
            {
                width = max;
                height = (int)(width * bm.getHeight() / (float)bm.getWidth());
            }
            else
            {
                height = max;
                width = (int)(height * bm.getWidth() / (float)bm.getHeight());
            }
            Bitmap src = bm;
            bm = Bitmap.createScaledBitmap(src, width, height, false);
            src.recycle();
        }
        return bm;
    }

    private FilePart reduceFileSize(Bitmap bm, Uri uri) throws IOException
    {
        FilePart filePart = new FilePart();
        filePart.mimeType = getContentResolver().getType(uri);

        String fileName = getFilename(uri);
        filePart.file = new File(Utils.getTempDir(this), fileName);
        Bitmap.CompressFormat format = "image/png".equals(filePart.mimeType) ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
        int quality = 100;
        do
        {
            quality -= 10;
            try (FileOutputStream out = new FileOutputStream(filePart.file))
            {
                bm.compress(format, quality, out);
            }
        } while (filePart.file.length() > 716800 && quality > 40);
        return filePart;
    }

    private String getFilename(Uri uri)
    {
        String[] projection = { OpenableColumns.DISPLAY_NAME };
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null))
        {
            if (cursor != null)
            {
                if (cursor.moveToFirst())
                {
                    int col = cursor.getColumnIndex(projection[0]);
                    if (col >= 0) return cursor.getString(col);
                }
            }
        }
        String path = uri.toString();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String getKey(String url)
    {
        int i = url.lastIndexOf('/') + 1;
        int j = url.indexOf('-', i);
        return url.substring(i, j);
    }

    private void deletePicture()
    {
        menuDelete.setEnabled(false);
        final List<Gallery> selectedItems = gallery.getSelectedItems();
        while (selectedItems.size() > 0)
        {
            Gallery o = selectedItems.remove(0);
            Gallery.deletePicture(o.id, o.url)
                    .subscribe(json -> {
                        MyGalleryAdapter adapter = gallery.getAdapter();
                        if (adapter != null)
                        {
                            adapter.remove(o);
                            if (selectedItems.size() == 0)
                            {
                                adapter.save().subscribe(Functions.emptyConsumer(),
                                        tr -> deleteFailed(selectedItems, tr));
                            }
                        }
                        inserted.remove(getKey(o.url));
                    }, tr -> deleteFailed(selectedItems, tr));
        }
    }

    private void deleteFailed(List<Gallery> selectedItems, Throwable tr)
    {
        L.e(tr);
        Utils.showToast(MyGalleryActivity.this, tr.getMessage());
        MyGalleryAdapter adapter = gallery.getAdapter();
        if (adapter != null && selectedItems.size() == 0)
        {
            adapter.save().subscribe(Functions.emptyConsumer(),
                    e -> Utils.showToast(MyGalleryActivity.this, e.getMessage()));
        }
    }
}
