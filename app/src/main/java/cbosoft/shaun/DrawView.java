package cbosoft.shaun;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import static android.content.ContentValues.TAG;
import static android.content.Context.SENSOR_SERVICE;

public class DrawView extends View implements SensorEventListener {

    /*
    * Draws out a random shape or something in the background, sort of like a screensaver.
    * Could trace out like an animated icon?
    * Disabled for now.
     */
    Paint paint = new Paint();
    int scrH = 0, scrW = 0, interval_ms = 100;
    Random rand = new Random();

    class point {
        public int x, y;
        public point(int x, int y) {
            this.y = y;
            this.x = x;
        }
    }

    point[] points = new point[50];
    SensorManager mSensorManager;
    Sensor s;
    int sVal = 0;

    private void init() {
        paint.setColor(getResources().getColor(R.color.colorAccent));
        paint.setStrokeWidth(2);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        scrH = displayMetrics.heightPixels;
        scrW = displayMetrics.widthPixels;

        for (int i = 0; i < points.length; i++) {
            int xp = scrW - (i * (scrW / points.length));
            points[i] = new point(xp, ((int)(scrH / 2.0)));
        }

        mSensorManager = (SensorManager)this.getContext().getSystemService(SENSOR_SERVICE);
        s = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        mSensorManager.registerListener(this, s, mSensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent se) {
        if (se.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] vals = se.values;

            sVal = (int)(vals[0] * vals[0] + vals[1] * vals[1] + vals[2] * vals[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor s, int a) {

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

    void updateGraph() {
        for (int i = 0; i < points.length - 1; i++) {
            points[i].y = points[i + 1].y;
        }

        points[points.length - 1].y = sVal;//rand.nextInt(100) - 50;
    }

    @Override
    public void onDraw(Canvas canvas) {

        updateGraph();
        //setGraph();
        for (int i = 1; i < points.length; i ++){
            canvas.drawLine(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y, paint);
        }

        try {
            TimeUnit.MILLISECONDS.sleep(interval_ms);
            this.invalidate();
        }
        catch (InterruptedException e){
            // interrupted, stop drawing
        }
    }
}
