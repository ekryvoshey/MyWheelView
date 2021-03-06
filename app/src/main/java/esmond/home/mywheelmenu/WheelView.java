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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WheelView extends View {
    private final String ROTATION_TRACKER_DEBUG_TAG = "rotation is";
    private final GestureDetector mDetector;

    private WheelChangeListener wheelChangeListener;
    private RectF oval;
    private String label;

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
    private int selectedPosition;
    int selectedPositionIndex;

    private float size;
    private float startingPosition;
    private float innerCircleRadius;
    private float outerCircleRadius;
    private float fillerCircleRadius;
    private float startAngle;
    private float sweepAngle;
    private float activeRotation;
    private float newRotation;

    private double angleA;
    private double angleB;

    private boolean allowRotating = true;

    private String[] cars = new String[]{
            "Acura", "Alfa Romeo", "Audi", "Bentley", "BMW", "Bugatti", "Buick", "Cadillac",
            "Chevrolet", "Chrysler", "Citroen", "Dodge", "Ferrari", "Fiat", "Ford", "Honda",
            "Hyundai", "Infiniti", "Jaguar", "Jeep", "KIA", "Lamborghini", "Land Rover", "Lexus",
            "Lincoln", "Maserati", "Mercedes-Benz", "Mini", "Nissan", "Peugeot", "Porsche",
            "Renault", "Rolls-Royce", "Subaru", "Tesla", "Toyota", "Volkswagen", "Volvo"
    };
    public static List<String> menuItems;
    public static List<String> actualItems;


    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        startingPosition = 0.5f;
        sectorsQnt = 18;
        sweepAngle = 360 / sectorsQnt;
        mDetector = new GestureDetector(context, new MyGestureDetector());
        menuItems = new ArrayList<>();
        actualItems = Arrays.asList(cars);

        initLists();
        initPaint();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                angleA = getAngle(event.getX(), event.getY());
                allowRotating = false;
                break;
            case MotionEvent.ACTION_MOVE:
                angleB = getAngle(event.getX(), event.getY());
                activeRotation += (float) (angleA - angleB);
                changeListItem();
                if (wheelChangeListener != null) {
                    wheelChangeListener.onSelectionChange(selectedPosition);
                }
                angleA = angleB;
                break;
            case MotionEvent.ACTION_UP:
                checkRotation();
                allowRotating = true;
                break;
        }
        mDetector.onTouchEvent(event);
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        screenHeight = this.getHeight();
        screenWidth = this.getWidth();
        size = Math.min(screenHeight, screenWidth);
        screenCenterX = screenWidth / 2;
        screenCenterY = screenHeight / 2;

        outerCircleRadius = size / 2f;
        innerCircleRadius = size / 4f;
        fillerCircleRadius = size / 4f - 2;

        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();

        canvasCenterX = screenCenterX;
        canvasCenterY = screenCenterY;

        redrawCanvas(activeRotation, canvas);
        canvas.drawPath(setTriangle(35, 20, 0, 0), mainPaint);
        canvas.drawPath(setTriangle(35, 20, -2, 0), fillerPaint);
        changeListItem();
    }

    public void redrawCanvas(float rotation, Canvas canvas) {
        canvas.save();
        canvas.translate(canvasCenterX, canvasCenterY);
        canvas.scale(1f, 1f, canvasCenterX, canvasCenterY);
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
        canvas.rotate(sweepAngle / 2);
        for (int i = 0; i < sectorsQnt; i++) {
            canvas.rotate(-sweepAngle);
            label = menuItems.get(i);
            int length = label.length();
            if (length >= 15) {
                label = label.substring(0, 10) + " ...";
            }
            float pivotX = Math.round(innerCircleRadius * (1.5f + length * 0.005f));
            float pivotY = Math.round(sweepAngle / 2);
            canvas.drawText(label, pivotX, pivotY, textPaint);
        }
        canvas.restore();
    }

    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            WheelView.this.post(new FlingRunnable((velocityY)));
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
                activeRotation += velocity / 500;
                newRotation += velocity / 500;
                invalidate();
                velocity /= 1.0666F;
                changeListItem();
                checkRotation();
                if (wheelChangeListener != null) {
                    wheelChangeListener.onSelectionChange(selectedPosition);
                }
                WheelView.this.post(this);
            }
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
        textPaint.setTextSize(20);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private double getAngle(double xTouch, double yTouch) {
        double x = xTouch - (canvasWidth / 2d);
        double y = canvasHeight - yTouch - (canvasHeight / 2d);
        return Math.cos(y / Math.hypot(x, y)) * 180 / Math.PI;
    }

    public int getSelectedPosition() {

        return selectedPosition;
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

    public interface WheelChangeListener {
        public void onSelectionChange(int selectedPosition);
    }

    public void setWheelChangeListener(WheelChangeListener wheelChangeListener) {
        this.wheelChangeListener = wheelChangeListener;
    }

    private void checkRotation() {
        activeRotation = activeRotation % 360;
        if (activeRotation < 0) {
            activeRotation = 360 + activeRotation;
        }
        newRotation = newRotation % (actualItems.size() * sweepAngle);
    }

    private void initLists() {
        int k = sectorsQnt;

        menuItems = new ArrayList<>(k);
        actualItems = Arrays.asList(cars);

        for (int i = 0; i < k; i++){
            menuItems.add(i, "");
        }
        for (int i = 0; i < k/2; i++) {
            menuItems.set(i, actualItems.get(i));
        }
        for (int i = 0; i < k/2; i++){
            menuItems.set(k-i-1, actualItems.get(actualItems.size()-i-1));
        }
    }

    private void changeListItem() {
        int c;
        int i = selectedPosition;
        int k = menuItems.size();
        int j = actualItems.size();
        int p = sectorsQnt;


        if (i <= k / 2) {
            c = i + k / 2;
        } else if (i > k / 2) {
            c = i - k / 2;
        } else c = k / 2;

        int previousPosition = selectedPosition;
        selectedPosition = Math.round(activeRotation / sweepAngle)+1;
        if (selectedPosition == sectorsQnt + 1 || selectedPosition == 0) {
            selectedPosition = 1;
        }

        if(previousPosition != selectedPosition){
            int g = actualItems.indexOf(menuItems.get(this.getSelectedPosition()-1))+1;
//            if (g < 0){
//                g = g + sectorsQnt;
//            }
            if(previousPosition < selectedPosition) {
                Log.d(ROTATION_TRACKER_DEBUG_TAG, "previousPosition: "+previousPosition);
                Log.d(ROTATION_TRACKER_DEBUG_TAG, "selectedPosition: " + selectedPosition);
                Log.d(ROTATION_TRACKER_DEBUG_TAG, "g: " + g);
//                    menuItems.set(c, actualItems.get(g+p/2-1));
            } else {
                Log.d(ROTATION_TRACKER_DEBUG_TAG, "previousPosition: "+previousPosition);
                Log.d(ROTATION_TRACKER_DEBUG_TAG, "selectedPosition: " + selectedPosition);
                Log.d(ROTATION_TRACKER_DEBUG_TAG, "g: " + g);
                menuItems.set(c - 1, actualItems.get(j-p/2-(j-g)));
            }
        }
    }
}

