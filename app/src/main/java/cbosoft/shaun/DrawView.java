package cbosoft.shaun;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.concurrent.TimeUnit;

public class DrawView extends View {

    /*
    * Draws out a random shape or something in the background, sort of like a screensaver.
    * Could trace out like an animated icon?
    * Disabled for now.
     */


    Paint paint = new Paint();

    int pos = 0;
    int interval_ms = 10;

    private void init() {
        paint.setColor(getResources().getColor(R.color.colorAccent));
        paint.setStrokeWidth(2);
    }

    public DrawView(Context context) {
        super(context);
        init();
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void onDraw(Canvas canvas) {
        pos += 1;
        canvas.drawLine(0, 0, pos, pos, paint);
        canvas.drawLine(pos, 0, 0, pos, paint);
        try {
            TimeUnit.MILLISECONDS.sleep(interval_ms);
            this.invalidate();
        }
        catch (InterruptedException e){
            // interrupted, stop drawing
        }
    }
}
