package gastudio.clock.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

import java.util.Calendar;

import gastudio.clock.utils.Constants;
import gastudio.clock.utils.UiUtils;
import gastudio.clock.R;

public class ClockViewByPath extends View {

    private static final int DEFAULT_CLOCK_ANIMATION_DURATION = Constants.MINUTE;
    private static final int FULL_ANGLE = 360;
    private static final int SECOND_PER_MINUTE = 60;

    private static final int DEFAULT_TOTAL_CLOCK_SCALE_LINE_NUM = 120;
    private static final int DEFAULT_CLOCK_VIEW_WIDTH = 260;
    private static final int ANGLE_PER_SECOND = FULL_ANGLE / SECOND_PER_MINUTE;
    private static final int ANGLE_PER_SCALE = FULL_ANGLE / DEFAULT_TOTAL_CLOCK_SCALE_LINE_NUM;

    private static final int DEFAULT_CLOCK_SCALE_LINE_COLOR = Color.WHITE;
    private static final int DEFAULT_CLOCK_SCALE_LINE_WIDTH = 2;
    private static final int DEFAULT_CLOCK_SCALE_LINE_HEIGHT = 14;
    private static final int ADJUST_CLOCK_SCALE_LINE_START_X = 1;

    private static final int DEFAULT_DIGITAL_TIME_TEXT_COLOR = Color.WHITE;
    private static final int DEFAULT_DIGITAL_TIME_TEXT_SIZE = 60;
    private static final String DEFAULT_DEFAULT_DIGITAL_TIME_TEXT = "00:00";

    private static final int DEFAULT_CLOCK_POINT_COLOR = Color.RED;
    private static final int DEFAULT_CLOCK_POINT_RADIUS = 6;

    private static final float[] CLOCK_SCALE_LINE_BASE_LEN_ARRAY = new float[]{
            1F, 1.1F, 1.21F, 1.32F, 1.452F,
            1.551F, 1.6827F, 1.75F, 1.6827F, 1.551F,
            1.452F, 1.32F, 1.21F, 1.1F, 1F};
    // This is max in CLOCK_SCALE_LINE_BASE_LEN_ARRAY
    private static final float RATIO_OF_MAX_HEIGTH_TO_NORMAL_HEIGHT = 1.75F;

    private int mClockScaleLineWidth;
    private int mClockScaleLineHeight;
    private int mClockScaleLineMaxHeight;
    private int mClockScaleLineColor;
    private int mAdjustClockScaleLineStartX;

    private int mClockViewCenterX;
    private int mClockViewCenterY;
    private int mClockMaskRadius;
    private RectF mClockViewRectF;
    private Path mClockMaskPath;
    private float mClockMaskAdjustAngle;

    private int mClockPointRadius;
    private int mClockPointColor;
    private int mClockPointCenterX;
    private int mClockPointCenterY;

    private int mDigitalTimeTextStartX;
    private int mDigitalTimeTextStartY;
    private int mDigitalTimeTextSize;
    private int mDigitalTimeTextColor;
    private Rect mDigitalTimeTextRect;
    private String mLastDigitalTimeStr;

    private Paint mPaint;
    private Xfermode mXfermode;
    private ValueAnimator mClockAnimator;

    private float mNowClockAngle;
    private float mInitClockAngle;
    private long mLastTimeMillis;
    private Calendar mCalendar;

    public ClockViewByPath(Context context) {
        this(context, null);
    }

    public ClockViewByPath(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockViewByPath(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ClockViewByPath(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ClockView);
        mDigitalTimeTextSize = a.getDimensionPixelSize(R.styleable.ClockView_timeTextSize,
                UiUtils.dipToPx(context, DEFAULT_DIGITAL_TIME_TEXT_SIZE));
        mDigitalTimeTextColor = a.getColor(R.styleable.ClockView_timeTextSize,
                DEFAULT_DIGITAL_TIME_TEXT_COLOR);
        mClockPointRadius = a.getDimensionPixelSize(R.styleable.ClockView_pointRadius,
                UiUtils.dipToPx(context, DEFAULT_CLOCK_POINT_RADIUS));
        mClockPointColor = a.getColor(R.styleable.ClockView_pointColor,
                DEFAULT_CLOCK_POINT_COLOR);
        mClockScaleLineWidth = a.getDimensionPixelSize(R.styleable.ClockView_timeScaleWidth,
                UiUtils.dipToPx(context, DEFAULT_CLOCK_SCALE_LINE_WIDTH));
        mClockScaleLineHeight = a.getDimensionPixelSize(R.styleable.ClockView_timeScaleHeight,
                UiUtils.dipToPx(context, DEFAULT_CLOCK_SCALE_LINE_HEIGHT));
        mClockScaleLineMaxHeight = (int) (RATIO_OF_MAX_HEIGTH_TO_NORMAL_HEIGHT * mClockScaleLineHeight);
        mClockScaleLineColor = a.getDimensionPixelSize(R.styleable.ClockView_timeScaleColor,
                UiUtils.dipToPx(context, DEFAULT_CLOCK_SCALE_LINE_COLOR));
        a.recycle();
        init();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int len = w > h ? h : w;
        mClockViewRectF.set(0, 0, len, len);
        mClockViewRectF.offset((w - len) / 2, (h - len) / 2);
        mClockViewCenterX = (int) mClockViewRectF.centerX();
        mClockViewCenterY = (int) mClockViewRectF.centerY();
        mDigitalTimeTextStartX = mClockViewCenterX - mDigitalTimeTextRect.left - mDigitalTimeTextRect.width() / 2;
        mDigitalTimeTextStartY = mClockViewCenterY - mDigitalTimeTextRect.top - mDigitalTimeTextRect.height() / 2;
        mClockPointCenterX = mClockViewCenterX;
        mClockPointCenterY = (int) (mClockViewRectF.top + mAdjustClockScaleLineStartX
                + mClockScaleLineMaxHeight + mClockPointRadius * 2);
        mClockMaskRadius = (int) (mClockViewRectF.width() / 2 - mClockScaleLineMaxHeight);
        generateMaskPath();
        mClockMaskAdjustAngle = (float) (CLOCK_SCALE_LINE_BASE_LEN_ARRAY.length + 1) / 2 * ANGLE_PER_SCALE;
    }

    private void generateMaskPath() {
        Point point = new Point(mClockViewCenterX, mClockViewCenterY - mClockMaskRadius - mClockScaleLineHeight);
        mClockMaskPath.moveTo(point.x, point.y);

        // Generate contour of the special clock scale lines
        int arrayLen = CLOCK_SCALE_LINE_BASE_LEN_ARRAY.length;
        for (int index = 0; index < arrayLen; index++) {
            calculateNextPoint(point, CLOCK_SCALE_LINE_BASE_LEN_ARRAY[index],
                    (float) Math.toRadians(ANGLE_PER_SCALE * (index + 1)));
            mClockMaskPath.lineTo(point.x, point.y);
        }

        // Generate contour of the normal clock scale lines
        int insertLen = mClockScaleLineMaxHeight - mClockScaleLineHeight;
        RectF cycleRectF = new RectF(mClockViewRectF);
        cycleRectF.inset(insertLen, insertLen);
        mClockMaskPath.arcTo(cycleRectF, arrayLen * ANGLE_PER_SCALE - 90,
                (DEFAULT_TOTAL_CLOCK_SCALE_LINE_NUM - arrayLen) * ANGLE_PER_SCALE);
    }

    private void calculateNextPoint(Point point, float scale, float angle) {
        float originLen = mClockMaskRadius + mClockScaleLineHeight;
        float adjustLen = mClockMaskRadius + mClockScaleLineHeight * scale;
        point.set(mClockViewCenterX + (int) (adjustLen * Math.sin(angle)),
                mClockViewCenterY - mClockMaskRadius - mClockScaleLineHeight
                        + (int) (-adjustLen * Math.cos(angle) + originLen));
    }

    private void init() {
        mClockViewRectF = new RectF();
        mDigitalTimeTextRect = new Rect();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mDigitalTimeTextSize);
        mPaint.getTextBounds(DEFAULT_DEFAULT_DIGITAL_TIME_TEXT, 0,
                DEFAULT_DEFAULT_DIGITAL_TIME_TEXT.length(), mDigitalTimeTextRect);
        mPaint.setStrokeWidth(mClockScaleLineWidth);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
        mAdjustClockScaleLineStartX = UiUtils.dipToPx(getContext(), ADJUST_CLOCK_SCALE_LINE_START_X);
        mClockMaskPath = new Path();
        mCalendar = Calendar.getInstance();
        mLastDigitalTimeStr = String.format("%02d:%02d", mCalendar.get(Calendar.HOUR),
                mCalendar.get(Calendar.MINUTE));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthSpecMode != MeasureSpec.EXACTLY) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    UiUtils.dipToPx(getContext(), DEFAULT_CLOCK_VIEW_WIDTH), MeasureSpec.EXACTLY);
        }
        if (heightSpecMode != MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    UiUtils.dipToPx(getContext(), DEFAULT_CLOCK_VIEW_WIDTH), MeasureSpec.EXACTLY);
        }
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Save layer
        int layerOne = canvas.saveLayer(mClockViewRectF, mPaint, Canvas.ALL_SAVE_FLAG);

        // Draw clock scale lines
        mPaint.setColor(mClockScaleLineColor);
        float clockScaleLineStartY = mAdjustClockScaleLineStartX + mClockViewRectF.top;
        float clockScaleLineEndY = clockScaleLineStartY + mClockScaleLineMaxHeight;
        for (int i = 0; i < DEFAULT_TOTAL_CLOCK_SCALE_LINE_NUM; i++) {
            canvas.drawLine(mClockViewCenterX, clockScaleLineStartY,
                    mClockViewCenterX, clockScaleLineEndY, mPaint);
            canvas.rotate(ANGLE_PER_SCALE, mClockViewCenterX, mClockViewCenterY);
        }

        mPaint.setXfermode(mXfermode);
        canvas.rotate(mNowClockAngle - mClockMaskAdjustAngle, mClockViewCenterX, mClockViewCenterY);
        // Generate a mask by path
        int layerTwo = canvas.saveLayer(mClockViewRectF, mPaint, Canvas.ALL_SAVE_FLAG);
        mPaint.setXfermode(null);
        canvas.drawOval(mClockViewRectF, mPaint);
        mPaint.setXfermode(mXfermode);
        canvas.drawPath(mClockMaskPath, mPaint);
        canvas.restoreToCount(layerTwo);

        mPaint.setXfermode(null);
        // Draw clock point
        mPaint.setColor(mClockPointColor);
        canvas.rotate(mClockMaskAdjustAngle, mClockViewCenterX, mClockViewCenterY);
        canvas.drawCircle(mClockPointCenterX, mClockPointCenterY, mClockPointRadius, mPaint);

        canvas.restoreToCount(layerOne);
        updateTimeText(canvas);
    }

    private void updateTimeText(Canvas canvas) {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mLastTimeMillis >= Constants.MINUTE) {
            mLastTimeMillis = currentTimeMillis;
            mCalendar.setTimeInMillis(currentTimeMillis);
            mLastDigitalTimeStr = String.format("%02d:%02d",
                    mCalendar.get(Calendar.HOUR), mCalendar.get(Calendar.MINUTE));
        }
        mPaint.setColor(mDigitalTimeTextColor);
        canvas.drawText(mLastDigitalTimeStr, mDigitalTimeTextStartX, mDigitalTimeTextStartY, mPaint);
    }

    public void performAnimation() {
        cancelAnimation();
        mClockAnimator = ValueAnimator.ofFloat(0, FULL_ANGLE);
        mClockAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mNowClockAngle = (float) animation.getAnimatedValue();
                mNowClockAngle += mInitClockAngle;
                invalidate();
            }
        });
        mClockAnimator.setDuration(DEFAULT_CLOCK_ANIMATION_DURATION);
        mClockAnimator.setInterpolator(new LinearInterpolator());
        mClockAnimator.setRepeatCount(Animation.INFINITE);
        mClockAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                long currentTimeMillis = System.currentTimeMillis();
                mCalendar.setTimeInMillis(currentTimeMillis);
                mLastDigitalTimeStr = String.format("%02d:%02d", mCalendar.get(Calendar.HOUR),
                        mCalendar.get(Calendar.MINUTE));
                mInitClockAngle = (mCalendar.get(Calendar.SECOND)
                        + (float) mCalendar.get(Calendar.MILLISECOND) / Constants.SECOND) * ANGLE_PER_SECOND;
                mLastTimeMillis = currentTimeMillis - mCalendar.get(Calendar.SECOND) * Constants.SECOND
                        - mCalendar.get(Calendar.MILLISECOND);
            }
        });
        mClockAnimator.start();
    }

    public void cancelAnimation() {
        if (mClockAnimator != null) {
            mClockAnimator.removeAllUpdateListeners();
            mClockAnimator.removeAllListeners();
            mClockAnimator.cancel();
            mClockAnimator = null;
        }
    }
}
