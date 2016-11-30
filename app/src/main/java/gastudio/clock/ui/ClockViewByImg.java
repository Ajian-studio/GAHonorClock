package gastudio.clock.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

public class ClockViewByImg extends View {

    private static final int DEFAULT_CLOCK_ANIMATION_DURATION = Constants.MINUTE;
    private static final int FULL_ANGLE = 360;
    private static final int SECOND_PER_MINUTE = 60;

    private static final int DEFAULT_TOTAL_CLOCK_SCALE_LINE_NUM = 120;
    private static final int ANGLE_PER_SECOND = FULL_ANGLE / SECOND_PER_MINUTE;
    private static final int ANGLE_PER_SCALE = FULL_ANGLE / DEFAULT_TOTAL_CLOCK_SCALE_LINE_NUM;

    private static final int DEFAULT_CLOCK_SCALE_LINE_COLOR = Color.WHITE;
    private static final int DEFAULT_CLOCK_SCALE_LINE_WIDTH = 2;
    private static final int DEFAULT_CLOCK_SCALE_LINE_HEIGHT = 25;
    private static final int ADJUST_CLOCK_SCALE_LINE_START_X = 2;

    private static final int DEFAULT_DIGITAL_TIME_TEXT_COLOR = Color.WHITE;
    private static final int DEFAULT_DIGITAL_TIME_TEXT_SIZE = 60;
    private static final String DEFAULT_DEFAULT_DIGITAL_TIME_TEXT = "00:00";

    private static final int DEFAULT_CLOCK_POINT_COLOR = Color.RED;
    private static final int DEFAULT_CLOCK_POINT_RADIUS = 6;

    private int mClockScaleLineWidth;
    private int mClockScaleLineHeight;
    private int mClockScaleLineColor;
    private int mAdjustClockScaleLineStartX;

    private int mClockViewCenterX;
    private int mClockViewCenterY;
    private RectF mClockViewRectF;
    private Bitmap mClockMaskBitmap;

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

    public ClockViewByImg(Context context) {
        this(context, null);
    }

    public ClockViewByImg(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockViewByImg(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ClockViewByImg(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
                + mClockScaleLineHeight + mClockPointRadius * 2);
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
        mClockMaskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.clock_mask);
        mXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
        mAdjustClockScaleLineStartX = UiUtils.dipToPx(getContext(), ADJUST_CLOCK_SCALE_LINE_START_X);
        mCalendar = Calendar.getInstance();
        mLastDigitalTimeStr = String.format("%02d:%02d", mCalendar.get(Calendar.HOUR),
                mCalendar.get(Calendar.MINUTE));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthSpecMode != MeasureSpec.EXACTLY) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mClockMaskBitmap.getWidth(), MeasureSpec.EXACTLY);
        }
        if (heightSpecMode != MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mClockMaskBitmap.getHeight(), MeasureSpec.EXACTLY);
        }
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Save a layer
        int layerCount = canvas.saveLayer(mClockViewRectF, mPaint, Canvas.ALL_SAVE_FLAG);

        // Draw the DEFAULT_TOTAL_CLOCK_SCALE_LINE_NUM clock scale lines
        mPaint.setColor(mClockScaleLineColor);

        // Because the picture is not perfect, we need mAdjustClockScaleLineStartX.
        float clockScaleLineStartY = mAdjustClockScaleLineStartX + mClockViewRectF.top;
        float clockScaleLineEndY = clockScaleLineStartY + mClockScaleLineHeight;
        for (int i = 0; i < DEFAULT_TOTAL_CLOCK_SCALE_LINE_NUM; i++) {
            canvas.drawLine(mClockViewCenterX, clockScaleLineStartY,
                    mClockViewCenterX, clockScaleLineEndY, mPaint);
            canvas.rotate(ANGLE_PER_SCALE, mClockViewCenterX, mClockViewCenterY);
        }
        mPaint.setXfermode(mXfermode);
        canvas.rotate(mNowClockAngle, mClockViewCenterX, mClockViewCenterY);
        canvas.drawBitmap(mClockMaskBitmap, null, mClockViewRectF, mPaint);
        mPaint.setXfermode(null);

        // Draw clock point
        mPaint.setColor(mClockPointColor);
        canvas.drawCircle(mClockPointCenterX, mClockPointCenterY, mClockPointRadius, mPaint);

        canvas.restoreToCount(layerCount);
        updateTimeText(canvas);
    }

    private void updateTimeText(Canvas canvas) {
        long currentTimeMillis = System.currentTimeMillis();
        // Use of abs() func is to prevent the user to adjust the time forward.
        if (Math.abs(currentTimeMillis - mLastTimeMillis) >= Constants.MINUTE) {
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
                mNowClockAngle = (float) animation.getAnimatedValue() + mInitClockAngle;
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
