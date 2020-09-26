package tarn.pantip.content;

import com.google.gson.JsonObject;

import io.reactivex.rxjava3.core.Observable;
import tarn.pantip.L;
import tarn.pantip.Pantip;
import tarn.pantip.model.User;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

public class Account
{
    private Account()
    {}

    public static Observable<User> login(String userName, String password)
    {
        Pantip.clearCookies();
        return RxUtils.observe(() -> {
            final String uri = "https://pantip.com/login/authentication";
            final String data = "member[email]=" + userName + "&member[crypted_password]=" + password + "&persistent[remember]=1&action=login&redirect=Lw==";

            String html = Http.post(uri, data).execute();
            if (html.indexOf("รหัสผ่านไม่ถูกต้อง") > 0) return error("บัญชีผู้ใช้หรือรหัสผ่านไม่ถูกต้อง กรุณาตรวจสอบใหม่อีกครั้ง");
            if (html.indexOf("อักษรภาพ") > 0) return error("คุณกรอกรหัสผ่านผิดมากเกินไป กรุณาปลดล๊อกอักษรภาพที่หน้าเว็บไซต์ก่อน");
            if (!Pantip.hasPantipSession()) return error("บัญชีผู้ใช้หรือรหัสผ่านไม่ถูกต้อง กรุณาตรวจสอบใหม่อีกครั้ง");

            Pantip.saveCookies();

            User user = new User();
            String name;
            String avatar = null;
            try
            {
                JsonObject json = Http.getAjax("https://pantip.com/forum/topic/get_reply").executeJson();
                user.id = json.get("mid").getAsInt();
                name = json.get("name").getAsString();
                JsonObject o = json.get("avatar").getAsJsonObject();
                try
                {
                    avatar = o.get("large").getAsString();
                    if (avatar.startsWith("/")) avatar = "https://ptcdn.info" + avatar;
                    Http.download(avatar, Utils.getAvatarFile(user.id));
                }
                catch (Exception e)
                {
                    L.e(e);
                    user.error = e.getMessage();
                }
            }
            catch (Exception e)
            {
                Pantip.handleException(e);
                int i = html.indexOf("https://pantip.com/profile/") + 26;
                int j = html.indexOf("\"", i);
                try
                {
                    user.id = Integer.parseInt(html.substring(i, j));
                }
                catch (Exception e1)
                {
                    Pantip.handleException(new Exception(html, e1));
                }
                name = userName;
            }

            user.name = name;
            user.avatar = avatar;
            return user;
        });
    }

    private static User error(String error)
    {
        User user = new User();
        user.error = error;
        return user;
    }

    public static Observable<Void> logout()
    {
        return RxUtils.observe(emitter -> {
            Http.get("https://pantip.com/logout").execute();
            emitter.onComplete();
        });
    }
}
