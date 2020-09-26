package tarn.pantip.content;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

public class Gallery
{
    public long id;
    public String url;
    public boolean selected;

    public Gallery()
    {}

    public Gallery(long id, String url)
    {
        this.id = id;
        this.url = url;
    }

    public static Observable<LocalObject<Gallery>> load()
    {
        return RxUtils.observe(() -> {
            File file = getDataFile();
            if (!file.exists()) return null;

            LocalObject<Gallery> result = new LocalObject<>();
            result.lastModified = file.lastModified();
            result.items = Json.fromFile(file, Gallery[].class);
            return result;
        });
    }

    public static File getDataFile() throws IOException
    {
        return new File(Utils.getFileDir(), "gallery.json");
    }

    public static Observable<Void> save(List<Gallery> items)
    {
        return RxUtils.observe(emitter -> {
            File file = getDataFile();
            Json.toFile(items.toArray(new Gallery[0]), Gallery[].class, file);
            emitter.onComplete();
        });
    }

    public static Observable<JsonObject> checkAvailable()
    {
        return RxUtils.observe(() -> Http.postAjax("https://pantip.com/image_gallery/lb_image").executeJson());
    }

    public static Observable<JsonObject> getPicture(int page)
    {
        return RxUtils.observe(() -> Http.getAjax("https://pantip.com/image_gallery/get_photos/" + page).executeJson());
    }

    public static Observable<JsonObject> uploadPicture(FilePart file)
    {
        return RxUtils.observe(() -> {
            JsonObject json = Http.postAjax("https://pantip.com/image_gallery/upload_image")
                    .form("id", 1)
                    .form("img_input_file", file)
                    .executeJson();
            if (json.has("error"))
                return json;

            JsonObject upload = json.getAsJsonObject("upload");
            return Http.postAjax("https://pantip.com/image_gallery/insert_image")
                    .form("o", upload.get("del_o").getAsString())
                    .form("m", upload.get("del_s").getAsString())
                    .form("title", upload.get("only_name").getAsString())
                    .form("name_img_o", upload.get("name_o").getAsString())
                    .form("name_img_m", upload.get("name_s").getAsString())
                    .executeJson();
        });
    }

    public static Observable<JsonObject> deletePicture(long id, String src)
    {
        return RxUtils.observe(() -> Http.postAjax("https://pantip.com/image_gallery/del_pic")
                .form("id", id)
                .form("src", src)
                .executeJson());
    }
}
