package tarn.pantip.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * User: tarn
 * Date: 2/3/13 3:40 PM
 */
public class Emotion implements Serializable
{
    private static final long serialVersionUID = 1L;

    public int total;
    public final Info like = new Info();
    public final Info laugh = new Info();
    public final Info love = new Info();
    public final Info impress = new Info();
    public final Info scary = new Info();
    public final Info surprised = new Info();
    public final List<Latest> latest = new ArrayList<>();

    public Emotion()
    { }

    public int getSelected()
    {
        if (like.selected) return 0;
        if (laugh.selected) return 1;
        if (love.selected) return 2;
        if (impress.selected) return 3;
        if (scary.selected) return 4;
        if (surprised.selected) return 5;
        return -1;
    }

    public String getSelectedText()
    {
        if (like.selected) return "ถูกใจ";
        if (laugh.selected) return "ขำกลิ้ง";
        if (love.selected) return "หลงรัก";
        if (impress.selected) return "ซึ้ง";
        if (scary.selected) return "สยอง";
        if (surprised.selected) return "ทึ่ง";
        return "แสดงความรู้สึก";
    }

    public static class Info implements Serializable
    {
        public int count;
        public boolean selected;
    }

    public static class Latest implements Serializable
    {
        public String name;
        public String emotion;
    }
}