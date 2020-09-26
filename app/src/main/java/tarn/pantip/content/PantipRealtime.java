package tarn.pantip.content;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.util.RxUtils;

public class PantipRealtime
{
    public long ranking_time;
    public long previous_id;
    public String next_id;
    public HomeData[] data;

    public static Observable<PantipRealtime> load()
    {
        return load(null);
    }

    public static Observable<PantipRealtime> load(HomeObject prev)
    {
        return RxUtils.observe(() -> {
            String url = "https://pantip.com/api/forum-service/home/get_pantip_realtime?limit=20";
            if (prev != null)
            {
                url += "&next_id=" + prev.next_id + "&ranking_time=" + prev.ranking_time;
            }
            PantipRealtime data = Http.getAjax(url).execute(PantipRealtime.class);
            return data != null ? data : new PantipRealtime();
        });
    }
}
