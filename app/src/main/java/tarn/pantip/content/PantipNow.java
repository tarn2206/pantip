package tarn.pantip.content;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.util.RxUtils;

/**
 * Created by Tarn on 20 December 2016
 */

public class PantipNow
{
    public HomeData[] data;
    public boolean has_next;
    public String next_id;

    public static Observable<PantipNow> load()
    {
        return load(null);
    }

    public static Observable<PantipNow> load(HomeObject prev)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/api/forum-service/home/get_pantip_now?limit=20";
            if (prev != null)
            {
                url += "&next_id=" + prev.next_id;
            }
            PantipNow data = Http.getAjax(url).execute(PantipNow.class);
            return data != null ? data : new PantipNow();
        });
    }
}
