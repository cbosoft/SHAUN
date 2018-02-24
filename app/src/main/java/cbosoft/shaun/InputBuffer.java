package cbosoft.shaun;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class InputBuffer extends EditText {

    int minSel = 0;

    public InputBuffer(Context context) {
        super(context);
    }

    public InputBuffer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InputBuffer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (selStart < minSel) {
            selStart = minSel;
            if (selEnd < minSel) selEnd = minSel;

            setSelection(selStart, selEnd);
        }
    }
}
