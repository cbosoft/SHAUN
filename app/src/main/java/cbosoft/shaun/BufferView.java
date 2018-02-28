package cbosoft.shaun;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;

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
        }
        out = sb.toString();
        Log.d(TAG, "onDraw: WRITING" + out);
        this.setText(out);
    }
}
