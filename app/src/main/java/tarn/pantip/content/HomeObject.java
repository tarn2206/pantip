package tarn.pantip.content;

import java.io.File;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

public class HomeObject
{
    public HomeData[] items;
    public boolean has_next;
    public String next_id;
    public long ranking_time;
    public Boolean expired;

    public static Observable<HomeObject> load(String fileName)
    {
        return RxUtils.observe(() -> {
            HomeObject obj = null;
            File file = new File(Utils.getFileDir(), fileName);
            if (file.exists())
            {
                obj = Json.fromFile(file, HomeObject.class);
                if (obj != null)
                {
                    obj.expired = Utils.needUpdate(file.lastModified());
                }
            }
            return obj != null ? obj : new HomeObject();
        });
    }

    public static void save(HomeObject obj, List<HomeData> list, String fileName)
    {
        RxUtils.observe(emitter -> {
            obj.items = list.toArray(new HomeData[0]);

            File file = new File(Utils.getFileDir(), fileName);
            Json.toFile(obj, HomeObject.class, file);
            emitter.onComplete();
        }).subscribe();
    }
}
