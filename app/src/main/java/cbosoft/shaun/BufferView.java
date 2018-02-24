package cbosoft.shaun;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.ActionBarOverlayLayout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

public class BufferView extends TextView {

    ArrayList<String> buffer = new ArrayList<>(0);

    public BufferView(Context context){
        super(context);
        init();
    }

    public BufferView(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    void init() {
    }

    public void addToBuffer(String toAdd) {
        this.buffer.add(toAdd);
        refreshFromBuffer();
    }

    public void refreshFromBuffer() {
        String out;
        StringBuilder sb = new StringBuilder();
        for (String s: buffer) {
            sb.append(s);
            sb.append("\n");
        }
        out = sb.toString();
        Log.d(TAG, "onDraw: WRITING" + out);
        this.setText(out);
    }
}
