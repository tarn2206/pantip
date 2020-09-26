package tarn.pantip.util;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

class MyLinkMovementMethod extends LinkMovementMethod
{
    /*@Override
    public void initialize(TextView widget, Spannable text)
    {
        Selection.setSelection(text, text.length());
    }*/

    /*@Override
    public void onTakeFocus(TextView view, Spannable text, int dir)
    {
        if ((dir & (View.FOCUS_FORWARD | View.FOCUS_DOWN)) != 0)
        {
            if (view.getLayout() == null)
            {
                // This shouldn't be null, but do something sensible if it is.
                Selection.setSelection(text, text.length());
            }
        }
        else
        {
            Selection.setSelection(text, text.length());
        }
    }*/

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event)
    {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN)
        {
            int x = (int)event.getX();
            int y = (int)event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);

            if (links.length > 0)
            {
                ClickableSpan link = links[0];
                if (action == MotionEvent.ACTION_UP)
                {
                    link.onClick(widget);
                }
                else
                {
                    Selection.setSelection(buffer, buffer.getSpanStart(link), buffer.getSpanEnd(link));
                }
                return true;
            }
        }

        return Touch.onTouchEvent(widget, buffer, event);
    }
}
