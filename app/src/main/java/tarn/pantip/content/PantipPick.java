package tarn.pantip.content;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.util.RxUtils;

/**
 * Created by Tarn on 20 December 2016
 */

public class PantipPick
{
    public HomeData[] data;
    public boolean has_next;
    public String next_id;

    public static Observable<PantipPick> load()
    {
        return load(null);
    }

    public static Observable<PantipPick> load(HomeObject prev)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/api/forum-service/home/get_pantip_pick?limit=20";
            if (prev != null)
            {
                url += "&next_id=" + prev.next_id;
            }
            PantipPick data = Http.getAjax(url).execute(PantipPick.class);
            return data != null ? data : new PantipPick();
        });
    }
}
