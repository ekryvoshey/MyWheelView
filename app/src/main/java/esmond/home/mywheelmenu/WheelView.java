package esmond.home.mywheelmenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;

public class WheelView extends View {
    private final String ON_TOUCH_ACTION_DEBUG_TAG = "touch action";
    private final String VELOCITY_TRACKER_DEBUG_TAG = "action velocity";
    private final String ROTATION_TRACKER_DEBUG_TAG = "touch angle";
    private final GestureDetector mDetector;

    private VelocityTracker mVelocityTracker = null;
    private Context context;
    private RectF oval;
    private ArrayList<String> items;

    private Paint mainPaint;
    private Paint fillerPaint;
    private Paint textPaint;

    private int screenWidth;
    private int screenHeight;
    private int screenCenterX;
    private int screenCenterY;
    private int canvasWidth;
    private int canvasHeight;
    private int canvasCenterX;
    private int canvasCenterY;
    private int textColor;
    private int textSize;
    private int segmentsQnt;

    private float size;
    private float innerCircleRadius;
    private float outerCircleRadius;
    private float fillerCircleRadius;
    private float startAngle;
    private float sweepAngle;
    private float activeRotation;
    private float persistentRotation;
    private float refX;
    private float refY;

    private boolean allowRotating;
    private boolean[] mQuadrantTouched = new boolean[] { false, false, false, false, false };

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDetector = new GestureDetector(context, new MyGestureDetector());
        setTextSize(18);
        setTextColor(Color.rgb(250, 250, 250));
        items = new ArrayList<String>();
        for (int i = 0; i < segmentsQnt; i++){
            String label = "Label " + (i + 1);
            items.add(label);
        }

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int index = event.getActionIndex();
                int action = event.getActionMasked();
                int pointerId = event.getPointerId(index);

                if (action == MotionEvent.ACTION_DOWN){
                    refX = event.getX();
                    refY = event.getY();
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        mVelocityTracker.clear();
                    }
                    mVelocityTracker.addMovement(event);
                    for (int i = 0; i < mQuadrantTouched.length; i++) {
                        mQuadrantTouched[i] = false;
                    }
                    Log.d(ON_TOUCH_ACTION_DEBUG_TAG, "Action was DOWN");
                    allowRotating = false;
                } else if (action == MotionEvent.ACTION_MOVE){
                    float x = event.getX() - canvasCenterX;
                    float y = canvasCenterY - event.getY();
                    float velocity = (VelocityTrackerCompat.getYVelocity(mVelocityTracker, pointerId));
                    if ((x != 0) && (y != 0)) {
                        double angleB = computeAngle(x, y);
                        x = refX - canvasCenterX;
                        y = canvasCenterY - refY;
                        double angleA = computeAngle(x, y);
                        activeRotation += (float) ((angleA - angleB));
                        angleA = angleB;
                        Log.d(ROTATION_TRACKER_DEBUG_TAG, "Angle is " + activeRotation);
                    }
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    Log.d(VELOCITY_TRACKER_DEBUG_TAG, "X velocity is " +
                            VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId));
                    Log.d(VELOCITY_TRACKER_DEBUG_TAG, "Y velocity is " +
                            VelocityTrackerCompat.getYVelocity(mVelocityTracker, pointerId));
                    Log.d(ON_TOUCH_ACTION_DEBUG_TAG, "Action was MOVE");
                } else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)){
                    Log.d(ON_TOUCH_ACTION_DEBUG_TAG, "Action was UP");
                    persistentRotation += activeRotation;

                    while (persistentRotation > 360) {
                        persistentRotation -= 360;
                    }

                    while (persistentRotation < 0) {
                        persistentRotation += 360;
                    }
                    activeRotation += persistentRotation;
                    allowRotating = true;
                }
                mQuadrantTouched[getQuadrant(event.getX() - (canvasWidth / 2),
                       canvasHeight - event.getY() - (canvasHeight / 2))] = true;
                mDetector.onTouchEvent(event);
                return true;
            }
        });
        initPaint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        segmentsQnt = 24;
        startAngle = 0;
        sweepAngle = 360 / segmentsQnt;

        screenHeight = this.getHeight();
        screenWidth = this.getWidth();
        size = Math.min(screenHeight, screenWidth);
        screenCenterX = screenWidth / 2;
        screenCenterY = screenHeight / 2;

        outerCircleRadius = size / 2;
        innerCircleRadius = size / 4;
        fillerCircleRadius = size / 4 - 2;
        persistentRotation = 0;

        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();
        canvasCenterX = canvasWidth / 2;
        canvasCenterY = canvasHeight / 2;

        redrawCanvas(activeRotation, canvas);

        }

    private void initPaint() {
        mainPaint = new Paint();
        mainPaint.setColor(Color.BLACK);
        mainPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeWidth(2);
        mainPaint.setAlpha(25);

        fillerPaint = new Paint();
        fillerPaint.setAlpha(255);
        fillerPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        fillerPaint.setColor(Color.rgb(250, 250, 250));

        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(textSize);
        mainPaint.setAlpha(25);
    }

    double computeAngle(float x, float y) {
        final double RADS_TO_DEGREES = 360 / (java.lang.Math.PI * 2);
        double result = java.lang.Math.atan2(y, x) * RADS_TO_DEGREES;

        if (result < 0) {
            result = 360 + result;
        }

        return result;
    }

    public void redrawCanvas(float rotation, Canvas canvas) {
        canvas.save();
        canvas.translate(screenCenterX, screenCenterY);
        canvas.scale(1f, 1f, screenCenterX, screenCenterY);
        canvas.rotate(rotation);
        oval = new RectF();
        for (int i = 0; i < segmentsQnt; i++) {
            oval.set(0 - outerCircleRadius, 0 - outerCircleRadius,
                    0 + outerCircleRadius, 0 + outerCircleRadius);
            canvas.drawArc(oval, startAngle, sweepAngle, true, mainPaint);
            startAngle -= sweepAngle;
        }
        for (int i = 0; i < segmentsQnt; i++){
            canvas.rotate(sweepAngle);
            float pivot = Math.round(innerCircleRadius) + innerCircleRadius*0.5f;
            canvas.drawText(items.get(i), pivot, -innerCircleRadius/6, textPaint);
        }

        // Set secondary inner circle to hide lines
        oval.set(0 - innerCircleRadius, 0 - innerCircleRadius,
                0 + innerCircleRadius, 0 + innerCircleRadius);
        canvas.drawArc(oval, 0, 360, true, mainPaint);

        // Set filler circle to show final color of inner part of the view
        oval.set(0 - fillerCircleRadius, 0 - fillerCircleRadius,
                0 + fillerCircleRadius, 0 + fillerCircleRadius);
        canvas.drawArc(oval, 0, 360, true, fillerPaint);
        canvas.restore();
    }

    private static int getQuadrant(double x, double y) {
        if (x >= 0) {
            return y >= 0 ? 1 : 4;
        } else {
            return y >= 0 ? 2 : 3;
        }
    }

    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // get the quadrant of the start and the end of the fling
            int q1 = getQuadrant(e1.getX() - (canvasWidth / 2), canvasHeight - e1.getY() - (canvasHeight / 2));
            int q2 = getQuadrant(e2.getX() - (canvasWidth / 2), canvasHeight - e2.getY() - (canvasHeight / 2));
            // the inversed rotations
            if ((q1 == 2 && q2 == 2 && Math.abs(velocityX) < Math.abs(velocityY))
                    || (q1 == 3 && q2 == 3)
                    || (q1 == 1 && q2 == 3)
                    || (q1 == 4 && q2 == 4 && Math.abs(velocityX) > Math.abs(velocityY))
                    || ((q1 == 2 && q2 == 3) || (q1 == 3 && q2 == 2))
                    || ((q1 == 3 && q2 == 4) || (q1 == 4 && q2 == 3))
                    || (q1 == 2 && q2 == 4 && mQuadrantTouched[3])
                    || (q1 == 4 && q2 == 2 && mQuadrantTouched[3]) ) {
                WheelView.this.post(new FlingRunnable(-1 * (velocityX + velocityY)));
            } else {

                // the normal rotation
                WheelView.this.post(new FlingRunnable(velocityX + velocityY));
            }
            return true;
        }
    }

    private class FlingRunnable implements Runnable {
        private float velocity;
        public FlingRunnable(float velocity) {
            this.velocity = velocity;
        }
        @Override
        public void run() {
            if (Math.abs(velocity) > 5 && allowRotating) {
                activeRotation += velocity / 50;
                invalidate();
                velocity /= 1.0666F;

                WheelView.this.post(this);
            }
        }
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }
}
