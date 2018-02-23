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
    }

    public BufferView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public void addToBuffer(String toAdd) {
        this.buffer.add(toAdd);
        String out;// = String.join("\n", this.buffer);

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
