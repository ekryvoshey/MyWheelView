package esmond.home.mywheelmenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class WheelView extends View {
    private final String ROTATION_TRACKER_DEBUG_TAG = "rotation is";
    private final String SELECTED_SEGMENT_DEBUG_TAG = "selected segment";
    private final static int INTERVAL = 1000;
    private final GestureDetector mDetector;

    private WheelChangeListener wheelChangeListener;
    private RectF oval;

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
    private int sectorsQnt;

    private float size;
    private float innerCircleRadius;
    private float outerCircleRadius;
    private float fillerCircleRadius;
    private float sweepAngle;
    private float activeRotation;
    private float startAngle;
    private float screen_density = getContext().getResources().getDisplayMetrics().density;

    private double angleA;
    private double angleB;

    private boolean allowRotating = true;
    private boolean[] mQuadrantTouched = new boolean[]{false, false, false, false, false};

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sectorsQnt = 24;
        sweepAngle = 360 / sectorsQnt;
        mDetector = new GestureDetector(context, new MyGestureDetector());
        this.setWheelChangeListener(new WheelChangeListener() {
            @Override
            public void onSelectionChange(int selectedPosition) {
                Log.d(SELECTED_SEGMENT_DEBUG_TAG, "" + selectedPosition);
            }
        });
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        for (int i = 0; i < mQuadrantTouched.length; i++) {
                            mQuadrantTouched[i] = false;
                        }
                        angleA = getAngle(event.getX(), event.getY());
                        allowRotating = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        angleB = getAngle(event.getX(), event.getY());
                        activeRotation += (float) (angleA - angleB);
                        angleA = angleB;
                        Log.d(ROTATION_TRACKER_DEBUG_TAG, "onTouch: "+activeRotation);
                        break;
                    case MotionEvent.ACTION_UP:
                        allowRotating = true;
                        break;
                }
                mDetector.onTouchEvent(event);
                invalidate();
                return true;
            }
        });
        initPaint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        screenHeight = this.getHeight();
        screenWidth = this.getWidth();
        size = Math.min(screenHeight, screenWidth);
        screenCenterX = screenWidth / 2;
        screenCenterY = screenHeight / 2;

        outerCircleRadius = size / 2;
        innerCircleRadius = size / 3.5f;
        fillerCircleRadius = size / 3.5f - 2;

        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();

        canvasCenterX = canvasWidth / 2;
        canvasCenterY = canvasHeight / 2;

        redrawCanvas(activeRotation, canvas);
        canvas.drawPath(setTriangle(35, 20, 0, 0), mainPaint);
        canvas.drawPath(setTriangle(35, 20, -2, 0), fillerPaint);
    }

    public void redrawCanvas(float rotation, Canvas canvas) {
        canvas.save();
        canvas.translate(screenCenterX, screenCenterY);
        canvas.scale(1f, 1f, screenCenterX, screenCenterY);
        canvas.rotate(rotation);

        // Set main circle
        oval = new RectF();
        startAngle = 0;
        canvas.rotate(sweepAngle / 2);
        for (int i = 0; i < sectorsQnt; i++) {
            oval.set(0 - outerCircleRadius, 0 - outerCircleRadius,
                    0 + outerCircleRadius, 0 + outerCircleRadius);
            canvas.drawArc(oval, startAngle, sweepAngle, true, mainPaint);
            startAngle -= sweepAngle;
        }
        // Set secondary inner circle to hide lines
        oval.set(0 - innerCircleRadius, 0 - innerCircleRadius,
                0 + innerCircleRadius, 0 + innerCircleRadius);
        canvas.drawArc(oval, 0, 360, true, mainPaint);

        // Set filler circle to show final color of inner part of the view
        oval.set(0 - fillerCircleRadius, 0 - fillerCircleRadius,
                0 + fillerCircleRadius, 0 + fillerCircleRadius);
        canvas.drawArc(oval, 0, 360, true, fillerPaint);

        // Set labels in segments
        canvas.rotate(-sweepAngle * 3 / 2);
        for (int i = 0; i < sectorsQnt; i++) {
            canvas.rotate(sweepAngle);
            String label = "Item " + (i + 1);
            float pivotX = Math.round(innerCircleRadius * 1.4f);
            float pivotY = Math.round(5);
            canvas.drawText(label, pivotX, pivotY, textPaint);
        }
        canvas.restore();
    }

    public interface WheelChangeListener {
        public void onSelectionChange(int selectedPosition);
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
                    || (q1 == 4 && q2 == 2 && mQuadrantTouched[3])) {
                WheelView.this.post(new FlingRunnable(-1 * (velocityY)));
            } else {

                // the normal rotation
                WheelView.this.post(new FlingRunnable(velocityY));
            }
            return true;
        }
    }

    private class FlingRunnable implements Runnable {
        private boolean stopped = false;
        private float velocity;

        public FlingRunnable(float velocity) {
            this.velocity = velocity;
        }
        @Override
        public void run() {
            setStopped(false);
            if (Math.abs(velocity) > 5 && allowRotating) {
                activeRotation += velocity / 500;
                invalidate();
                velocity /= 1.1666F;
                WheelView.this.post(this);
            }
        }

        public boolean isStopped(){
            return stopped;
        }

        public void setStopped(boolean stopped) {
            if (this.stopped != stopped)
            this.stopped = stopped;
        }

        public void stop(){
            setStopped(true);
        }
    }

    private void initPaint() {
        mainPaint = new Paint();
        mainPaint.setARGB(100, 0, 0, 0);
        mainPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeWidth(2);

        fillerPaint = new Paint();
        fillerPaint.setARGB(255, 250, 250, 250);
        fillerPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint();
        textPaint.setARGB(100, 0, 0, 0);
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private double getAngle(double xTouch, double yTouch) {
        double x = xTouch - (canvasWidth / 2d);
        double y = canvasHeight - yTouch - (canvasHeight / 2d);
        switch (getQuadrant(x, y)) {
            case 1:
                return Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            case 2:
                return 180 - Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            case 3:
                return 180 + (-1 * Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
            case 4:
                return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            default:
                return 0;
        }
    }

    private static int getQuadrant(double x, double y) {
        if (x >= 0) {
            return y >= 0 ? 1 : 4;
        } else {
            return y >= 0 ? 2 : 3;
        }
    }

    private Path setTriangle(int width, int height, int modX, int modY) {
        Point drawPoint1 = new Point();
        drawPoint1.set(canvasCenterX + Math.round(innerCircleRadius) + modX, canvasCenterY - height);
        Point drawPoint2 = new Point();
        drawPoint2.set(canvasCenterX + Math.round(innerCircleRadius + width + modX), canvasCenterY + modY);
        Point drawPoint3 = new Point();
        drawPoint3.set(canvasCenterX + Math.round(innerCircleRadius) + modX, canvasCenterY + height);

        Path path = new Path();
        path.moveTo(drawPoint1.x, drawPoint1.y);
        path.lineTo(drawPoint2.x, drawPoint2.y);
        path.lineTo(drawPoint3.x, drawPoint3.y);
        path.close();
        return path;
    }

    public void setWheelChangeListener(WheelChangeListener wheelChangeListener) {
        this.wheelChangeListener = wheelChangeListener;
    }
}
