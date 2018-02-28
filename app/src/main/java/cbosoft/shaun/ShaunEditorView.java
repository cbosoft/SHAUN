package cbosoft.shaun;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class ShaunEditorView extends EditText {

    public ShaunEditorView(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public ShaunEditorView(Context context){
        super(context);
        init();
    }

    public void init() {
        this.setHorizontallyScrolling(true);
    }

}
