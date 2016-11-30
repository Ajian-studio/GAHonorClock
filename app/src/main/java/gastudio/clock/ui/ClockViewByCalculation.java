package gastudio.clock.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

import java.util.Calendar;

import gastudio.clock.utils.Constants;
import gastudio.clock.utils.UiUtils;
import gastudio.clock.R;

public class ClockViewByCalculation extends View {

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

    private int mClockScaleLineColor;
    private int mClockScaleLineWidth;
    private int mClockScaleLineHeight;
    private int mClockScaleLineMaxHeight;

    private int mClockViewCenterX;
    private int mClockViewCenterY;
    private Rect mClockViewRect;

    private int mClockPointColor;
    private int mClockPointRadius;
    private int mClockPointCenterX;
    private int mClockPointCenterY;

    private int mDigitalTimeTextColor;
    private int mDigitalTimeTextStartX;
    private int mDigitalTimeTextStartY;
    private int mDigitalTimeTextSize;
    private Rect mDigitalTimeTextRect;
    private String mDigitalTimeTextStr;

    private float mNowClockAngle;
    private float mInitClockAngle;
    private float mRemainderOfNowClockAngle;

    private Paint mPaint;
    private ValueAnimator mClockAnimator;
    private Calendar mCalendar;
    private long mLastRecodeTimeMillis;

    public ClockViewByCalculation(Context context) {
        this(context, null);
    }

    public ClockViewByCalculation(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockViewByCalculation(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ClockViewByCalculation(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
        mClockViewRect.set(0, 0, len, len);
        mClockViewRect.offset((w - len) / 2, (h - len) / 2);
        mClockViewCenterX = mClockViewRect.centerX();
        mClockViewCenterY = mClockViewRect.centerY();
        mDigitalTimeTextStartX = mClockViewCenterX - mDigitalTimeTextRect.left - mDigitalTimeTextRect.width() / 2;
        mDigitalTimeTextStartY = mClockViewCenterY - mDigitalTimeTextRect.top - mDigitalTimeTextRect.height() / 2;
        mClockPointCenterX = mClockViewCenterX;
        mClockPointCenterY = mClockViewRect.top
                + mClockScaleLineMaxHeight + mClockPointRadius * 2;
    }

    private void init() {
        mClockViewRect = new Rect();
        mDigitalTimeTextRect = new Rect();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mDigitalTimeTextSize);
        mPaint.getTextBounds(DEFAULT_DEFAULT_DIGITAL_TIME_TEXT, 0,
                DEFAULT_DEFAULT_DIGITAL_TIME_TEXT.length(), mDigitalTimeTextRect);
        mPaint.setStrokeWidth(mClockScaleLineWidth);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mCalendar = Calendar.getInstance();
        mDigitalTimeTextStr = String.format("%02d:%02d", mCalendar.get(Calendar.HOUR),
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
        // Normalization the angle
        float normalizedTimePeriod = mRemainderOfNowClockAngle / ANGLE_PER_SCALE;
        int clockScaleLineStartY = mClockViewRect.top + mClockScaleLineMaxHeight;

        canvas.save();

        // Rotate the canvas to now clock angle
        canvas.rotate(mNowClockAngle, mClockViewCenterX, mClockViewCenterY);

        // Draw the point
        mPaint.setColor(mClockPointColor);
        canvas.drawCircle(mClockPointCenterX, mClockPointCenterY, mClockPointRadius, mPaint);

        // The follow adjustArrayLen indicate the special clock scale num
        int adjustArrayLen = CLOCK_SCALE_LINE_BASE_LEN_ARRAY.length - 1;

        // Rotate the canvas to ensure that the longest scale line points to now scale line
        canvas.rotate(-mRemainderOfNowClockAngle - (adjustArrayLen - 2) / 2f * ANGLE_PER_SCALE,
                mClockViewCenterX, mClockViewCenterY);
        mPaint.setColor(mClockScaleLineColor);

        // Draw the special lines
        // First draw the rightmost clock scale line, so you need to start with index = adjustArrayLen - 1;
        for (int index = adjustArrayLen - 1; index >= 0; index--) {
            // The follow function is mean that Length 1 changes to length 2 within a certain period.
            // The formula can be expressed as follows, changeLen1 = (len2 - len1) * timeFactor + len1.
            float specialLineNowLen = (mClockScaleLineHeight * (CLOCK_SCALE_LINE_BASE_LEN_ARRAY[index]
                    + normalizedTimePeriod * (CLOCK_SCALE_LINE_BASE_LEN_ARRAY[index + 1]
                    - CLOCK_SCALE_LINE_BASE_LEN_ARRAY[index])));
            float specialClockEndY = clockScaleLineStartY - specialLineNowLen;
            canvas.drawLine(mClockViewCenterX, clockScaleLineStartY, mClockViewCenterX, specialClockEndY, mPaint);
            canvas.rotate(ANGLE_PER_SCALE, mClockViewCenterX, mClockViewCenterY);
        }

        // Draw the normal lines
        int clockScaleLineEndY = mClockScaleLineMaxHeight + mClockViewRect.top - mClockScaleLineHeight;
        for (int other = 0; other < (DEFAULT_TOTAL_CLOCK_SCALE_LINE_NUM - adjustArrayLen); other++) {
            canvas.drawLine(mClockViewCenterX, clockScaleLineStartY, mClockViewCenterX,
                    clockScaleLineEndY, mPaint);
            canvas.rotate(ANGLE_PER_SCALE, mClockViewCenterX, mClockViewCenterY);
        }

        canvas.restore();
        updateDigitalTimeText(canvas);
    }

    private void updateDigitalTimeText(Canvas canvas) {
        long currentTimeMillis = System.currentTimeMillis();
        // Use of abs() func is to prevent the user to adjust the time forward.
        if (Math.abs(currentTimeMillis - mLastRecodeTimeMillis) >= Constants.MINUTE) {
            mLastRecodeTimeMillis = currentTimeMillis;
            mCalendar.setTimeInMillis(currentTimeMillis);
            mDigitalTimeTextStr = String.format("%02d:%02d",
                    mCalendar.get(Calendar.HOUR), mCalendar.get(Calendar.MINUTE));
        }
        mPaint.setColor(mDigitalTimeTextColor);
        canvas.drawText(mDigitalTimeTextStr, mDigitalTimeTextStartX, mDigitalTimeTextStartY, mPaint);
    }

    public void performAnimation() {
        cancelAnimation();
        mClockAnimator = ValueAnimator.ofFloat(0, FULL_ANGLE);
        mClockAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mNowClockAngle = (float) animation.getAnimatedValue();
                mNowClockAngle += mInitClockAngle;
                mNowClockAngle %= FULL_ANGLE;
                mRemainderOfNowClockAngle = mNowClockAngle % ANGLE_PER_SCALE;
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
                mDigitalTimeTextStr = String.format("%02d:%02d", mCalendar.get(Calendar.HOUR),
                        mCalendar.get(Calendar.MINUTE));
                mInitClockAngle = (mCalendar.get(Calendar.SECOND)
                        + (float) mCalendar.get(Calendar.MILLISECOND) / Constants.SECOND) * ANGLE_PER_SECOND;
                mLastRecodeTimeMillis = currentTimeMillis - mCalendar.get(Calendar.SECOND) * Constants.SECOND
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
