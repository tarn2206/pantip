package tarn.pantip.content;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser;

import java.io.FileInputStream;
import java.io.IOException;

import tarn.pantip.L;

public class FileProvider extends androidx.core.content.FileProvider
{
    @Override
    public String getType(@NonNull Uri uri)
    {
        try (ParcelFileDescriptor file = openFile(uri, "r"))
        {
            if (file == null) return super.getType(uri);
            try (FileInputStream in = new FileInputStream(file.getFileDescriptor()))
            {
                ImageHeaderParser.ImageType type = new DefaultImageHeaderParser().getType(in);
                String extension = "";
                switch (type)
                {
                    case GIF: extension = "gif"; break;
                    case JPEG: extension = "jpg"; break;
                    case PNG: extension = "png"; break;
                }
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (mime != null) return mime;
            }
        }
        catch (IOException e)
        {
            L.e(e);
        }
        return super.getType(uri);
    }
}
