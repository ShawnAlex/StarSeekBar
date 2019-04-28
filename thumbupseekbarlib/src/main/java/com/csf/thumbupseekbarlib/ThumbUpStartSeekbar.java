package com.csf.thumbupseekbarlib;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

/**
 * @author chenxiao
 */
@SuppressLint({"DrawAllocation", "NewApi"})
@TargetApi(Build.VERSION_CODES.FROYO)
public class ThumbUpStartSeekbar extends View {

    private static final String TAG = "=RangeSeekbar=";

    private static final int DEFAULT_DURATION = 100;

    private enum DIRECTION {
        LEFT, RIGHT;
    }

    private int mDuration;

    /**
     * Scrollers for current cursor
     */
    private Scroller mScroller;

    /**
     * cursor width and hight
     */
    private int cursorWidth;
    private int cursorHeight;

    /**
     * Length of every part. As we divide some parts according to marks.
     */
    private int spaceBetweenCursor;

    /**
     * count of Cursor.
     */
    private int cursorCount;

    private Rect mCurrentCursorRect;

    private float cursorIndex;
    private int mCursorCorrectIndex;
    private float oldCursorIndex;


    private Paint mPaint;

    private int mPointerLastX;

    private int mPointerID;

    private boolean isCursorSelected;

    private OnCursorChangeListener mListener;

    private Rect[] mClickRectArray;
    private Rect[] mDrawableRectArray;
    private Rect[] mSeekMaskRectArray;
    private int clickIndex;

    private Drawable selectedDrawable;
    private Drawable defaultDrawable;
    private Drawable maskDrawable;

    private int selectedDrawableId;
    private int defaultDrawableId;
    private int maskDrawableId;


    public ThumbUpStartSeekbar(Context context) {
        this(context, null, 0);
    }

    public ThumbUpStartSeekbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThumbUpStartSeekbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyConfig(context, attrs);

        mCurrentCursorRect = new Rect();

        if (cursorCount != -1) {
            mClickRectArray = new Rect[cursorCount];
            mDrawableRectArray = new Rect[cursorCount];
            mSeekMaskRectArray = new Rect[cursorCount - 1];
        }

        mScroller = new Scroller(context, new DecelerateInterpolator());
        mPointerID = -1;

        initPaint();

        setWillNotDraw(false);
        setFocusable(true);
        setClickable(true);

        selectedDrawable = ContextCompat.getDrawable(context, selectedDrawableId);
        defaultDrawable = ContextCompat.getDrawable(context, defaultDrawableId);
        maskDrawable = ContextCompat.getDrawable(context, maskDrawableId);

    }

    private void applyConfig(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ThumbUpStartSeekbar);

        mDuration = a.getInteger(R.styleable.ThumbUpStartSeekbar_autoMoveDuration, DEFAULT_DURATION);

        cursorWidth = (int) a.getDimension(R.styleable.ThumbUpStartSeekbar_cursorWidth, 50);
        cursorHeight = (int) a.getDimension(R.styleable.ThumbUpStartSeekbar_cursorHight, 50);
        //设置控件内边距，若左右内边距小于光标1/2宽度，设置为光标1/2宽度
        if (getPaddingLeft() < cursorWidth / 2) {
            setPadding(cursorWidth / 2, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }
        if (getPaddingRight() < cursorWidth / 2) {
            setPadding(getPaddingLeft(), getPaddingTop(), cursorWidth / 2, getPaddingBottom());
        }

        cursorCount = a.getInt(R.styleable.ThumbUpStartSeekbar_startCount, 1);

        spaceBetweenCursor = (int) a.getDimension(R.styleable.ThumbUpStartSeekbar_spaceBetweenStart, 0);

        cursorIndex = a.getInt(R.styleable.ThumbUpStartSeekbar_defaultSelectionIndex, -1);
        mCursorCorrectIndex = (int) cursorIndex;
        clickIndex = (int) cursorIndex;

        selectedDrawableId = a.getResourceId(R.styleable.ThumbUpStartSeekbar_selectedDrawable, R.drawable.thumb_up_start_selected);
        defaultDrawableId = a.getResourceId(R.styleable.ThumbUpStartSeekbar_defaultDrawable, R.drawable.thumb_up_start_default);
        maskDrawableId = a.getResourceId(R.styleable.ThumbUpStartSeekbar_maskDrawable, R.drawable.thumb_up_start_mask);
        a.recycle();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.FILL);
    }

    /**
     * 设置控件内边距，若左右内边距小于光标1/2宽度，设置为光标1/2宽度
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        if (getPaddingLeft() < cursorWidth / 2) {
            setPadding(cursorWidth / 2, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }
        if (getPaddingRight() < cursorWidth / 2) {
            setPadding(getPaddingLeft(), getPaddingTop(), cursorWidth / 2, getPaddingBottom());
        }
    }

    private int viewWidthSize;
    private int viewHeightSize;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightNeeded = cursorHeight + getPaddingTop() + getPaddingBottom();
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (heightMode == MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize < heightNeeded ? heightSize : heightNeeded, MeasureSpec.EXACTLY);
        } else {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightNeeded, MeasureSpec.EXACTLY);
        }

        viewWidthSize = MeasureSpec.getSize(widthMeasureSpec);
        viewHeightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (spaceBetweenCursor < cursorWidth * 2) {
            spaceBetweenCursor = cursorWidth * 2 + 1;
        }

        /*set onclick bound*/
        for (int i = 0; i < cursorCount; i++) {
            Rect rect = mClickRectArray[i];
            if (rect == null) {
                rect = new Rect();
                rect.top = getPaddingTop();
                rect.bottom = rect.top + cursorHeight;
                rect.left = getPaddingLeft() + spaceBetweenCursor * i;
                rect.right = rect.left + cursorWidth;
                mClickRectArray[i] = rect;
            }

            //set default start rect size
            Rect defaultStartRect = mDrawableRectArray[i];
            if (defaultStartRect == null) {
                defaultStartRect = new Rect();
                defaultStartRect.left = (int) Math.floor(getPaddingLeft() + spaceBetweenCursor * i);
                defaultStartRect.top = (int) Math.floor(getPaddingTop());
                defaultStartRect.right = (int) Math.ceil(getPaddingLeft() + spaceBetweenCursor * i + cursorWidth);
                defaultStartRect.bottom = (int) Math.ceil(defaultStartRect.top + cursorHeight);
                mDrawableRectArray[i] = defaultStartRect;
            }

            //set seek mask rect
            if (i > 0) {
                Rect seekMaskRect = mSeekMaskRectArray[i - 1];
                if (seekMaskRect == null) {
                    seekMaskRect = new Rect();
                    seekMaskRect.left = mDrawableRectArray[i - 1].right;
                    seekMaskRect.top = mDrawableRectArray[i].top;
                    seekMaskRect.right = mDrawableRectArray[i].left;
                    seekMaskRect.bottom = mDrawableRectArray[i].bottom;
                    mSeekMaskRectArray[i - 1] = seekMaskRect;
                }
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @SuppressLint({"DrawAllocation", "NewApi"})
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /*** Draw back ground***/
        mPaint.setARGB(255, 255, 255, 255);
        Rect backGroundRect = new Rect(getPaddingLeft(), getPaddingTop(), viewWidthSize - getPaddingRight(), viewHeightSize);
        canvas.drawRect(backGroundRect, mPaint);

        //Cache data
        mCurrentCursorRect.left = 0;
        mCurrentCursorRect.top = getPaddingTop();
        mCurrentCursorRect.right = (int) Math.ceil(getPaddingLeft() + cursorIndex * spaceBetweenCursor + cursorWidth);
        mCurrentCursorRect.bottom = mCurrentCursorRect.top + cursorHeight;

        drawSelectedStart(canvas, (int) Math.ceil(cursorIndex));
        drawDefaultDrawable(canvas, cursorCount - (int) cursorIndex);

        //draw filler
        Bitmap selectedCursorBit = BitmapFactory.decodeResource(getResources(), selectedDrawableId);
        mPaint.setColor(selectedCursorBit.getPixel(selectedCursorBit.getWidth() / 2, selectedCursorBit.getHeight() / 2));
        canvas.drawRect(mCurrentCursorRect, mPaint);

        drawMask(canvas);
        drawMaskStart(canvas);
    }


    /**
     * draw mask
     */

    private void drawMask(Canvas canvas) {
        mPaint.setARGB(0xff, 0xff, 0xff, 0xff);
        for (int i = 0; i < cursorCount - 1; i++) {
            canvas.drawRect(mSeekMaskRectArray[i], mPaint);
        }
        mPaint.setARGB(255, 255, 255, 255);
        Rect backGroundRect = new Rect(0, 0, getPaddingLeft(), viewHeightSize);
        canvas.drawRect(backGroundRect, mPaint);
        backGroundRect = new Rect(getPaddingLeft() + cursorWidth + spaceBetweenCursor * (cursorCount - 1), 0, viewWidthSize, viewHeightSize);
        canvas.drawRect(backGroundRect, mPaint);
    }

    /**
     * draw mask start
     *
     * @params
     */
    private void drawMaskStart(Canvas canvas) {
        for (int i = 0; i < cursorCount; i++) {
            maskDrawable.setBounds(mDrawableRectArray[i].left, mDrawableRectArray[i].top,
                    mDrawableRectArray[i].right, mDrawableRectArray[i].bottom);
            maskDrawable.draw(canvas);
        }
    }

    /**
     * draw default start
     *
     * @params invertedCount 从右向左要画几颗星星
     */
    private void drawDefaultDrawable(Canvas canvas, int invertedCount) {
        for (int i = cursorCount - 1; i > cursorCount - invertedCount; i--) {
            defaultDrawable.setBounds(mDrawableRectArray[i].left, mDrawableRectArray[i].top,
                    mDrawableRectArray[i].right, mDrawableRectArray[i].bottom);
            defaultDrawable.draw(canvas);
        }
    }

    /**
     * draw selected start
     *
     * @params positiveCount 从左向右画几颗星星
     */
    private void drawSelectedStart(Canvas canvas, int positiveCount) {
        for (int i = 0; i < positiveCount; i++) {
            selectedDrawable.setBounds(mDrawableRectArray[i].left, mDrawableRectArray[i].top,
                    mDrawableRectArray[i].right, mDrawableRectArray[i].bottom);
            selectedDrawable.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(event);
                break;

            case MotionEvent.ACTION_MOVE:
                handleTouchMove(event);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleTouchUp(event);
                break;
        }

        return super.onTouchEvent(event);
    }


    private void handleTouchDown(MotionEvent event) {

        isCursorSelected = true;

        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int downX = (int) event.getX(actionIndex);
        final int downY = (int) event.getY(actionIndex);


        //判断是否选中可点击的光标区域
        //点击上下内边距区域不算选中事件
        final int clickBoundaryTop = mClickRectArray[0].top;
        final int clickBoundaryBottom = mClickRectArray[0].bottom;
        if (downY < clickBoundaryTop || downY > clickBoundaryTop + cursorHeight + clickBoundaryBottom) {
            return;
        }

        final float partIndex = (downX - getPaddingLeft() - cursorWidth / 2) / spaceBetweenCursor;
        final float partDelta = (downX - getPaddingLeft() - cursorWidth / 2) % spaceBetweenCursor;
        if (partIndex > cursorCount - 1 || partDelta < -cursorWidth) {
            //超出最光标可点击区域的事件，视为无效事件
            return;
        }
        if (Math.abs(partDelta) <= cursorWidth / 2) {
            clickIndex = (int) partIndex;
        } else if (spaceBetweenCursor - partDelta <= cursorWidth / 2) {
            clickIndex = (int) (partIndex + 1);
        } else {
            //点击非光标数组的区域，不执行任何操作
            return;
        }

        cursorIndex = partIndex + partDelta / spaceBetweenCursor;
        mPointerLastX = downX;
        mPointerID = event.getPointerId(actionIndex);
        invalidate();


    }

    /**
     * @param event
     */
    private void handleTouchMove(MotionEvent event) {
        int x = 0;
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        x = (int) event.getX(actionIndex);
        if (mPointerID != -1) {
            float deltaX = x - mPointerLastX;
            mPointerLastX = (int) x;
            float moveX = deltaX / spaceBetweenCursor;
            cursorIndex += moveX;

            if (deltaX == 0) {
                return;
            }

            //判断滑动方向
            DIRECTION direction = (deltaX < 0 ? DIRECTION.LEFT : DIRECTION.RIGHT);

            final float minIndex = -cursorWidth * 1f / spaceBetweenCursor;
            final float maxMarkIndex = cursorCount - 1 + cursorWidth * 1f / spaceBetweenCursor;

            if (direction == DIRECTION.RIGHT && cursorIndex > maxMarkIndex) {
                cursorIndex = maxMarkIndex;
                invalidate();
            } else if (direction == DIRECTION.LEFT && cursorIndex < minIndex) {
                cursorIndex = minIndex;
                invalidate();
            } else {
                invalidate();
            }
            isCursorSelected = false;
        }
    }


    private void handleTouchUp(MotionEvent event) {
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int actionID = event.getPointerId(actionIndex);
        if (actionID != mPointerID) {
            return;
        }

        final int lower = (int) Math.floor(cursorIndex);
        final int higher = (int) Math.ceil(cursorIndex);

        if (!isCursorSelected) {
            //滑动事件
            final float offset = cursorIndex % 1;
            if (offset >=1 - cursorWidth*1f / 2/spaceBetweenCursor) {
                mCursorCorrectIndex = higher;
            } else {
                mCursorCorrectIndex = lower;
            }

            if (!mScroller.computeScrollOffset()) {
                cursorIndex = mCursorCorrectIndex;
                oldCursorIndex = mCursorCorrectIndex;
                final int fromX = (int) (cursorIndex * spaceBetweenCursor) + getPaddingLeft() + cursorWidth / 2;
                mScroller.startScroll(fromX, 0, mCursorCorrectIndex * spaceBetweenCursor - fromX, 0, mDuration);
                triggleCallback(mCursorCorrectIndex);
                invalidate();
            }
        } else {
            //点击事件
            if (oldCursorIndex == clickIndex) {
                clickIndex -= 1;
                oldCursorIndex = clickIndex;
                cursorIndex = clickIndex;
            } else {
                oldCursorIndex = clickIndex;
                cursorIndex = clickIndex;

            }
            triggleCallback(clickIndex);
            invalidate();
        }

        mPointerLastX = 0;
        mPointerID = -1;

    }


    @Override
    public void computeScroll() {

        if (mScroller.computeScrollOffset()) {
            final int deltaX = mScroller.getCurrX();

            cursorIndex = (float) deltaX / spaceBetweenCursor;

            invalidate();
        }
    }

    private void triggleCallback(int location) {

        if (mListener == null) {
            return;
        }

        mListener.onCursorChanged(location);

    }

    /**
     * 使用代码设置RangeSeekbar属性
     */

    public void setCursorSelection(int partIndex) {
        if (partIndex > cursorCount - 1 || partIndex < -1) {
            throw new IllegalArgumentException("Index should from 1 to size of text array minus 1!");
        }

        if (partIndex != cursorIndex) {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            cursorIndex = partIndex;
            mCursorCorrectIndex = partIndex;
            clickIndex = partIndex;
            final int rightFromX = (int) (spaceBetweenCursor * cursorIndex);
            mScroller.startScroll(rightFromX, 0, mCursorCorrectIndex * spaceBetweenCursor - rightFromX, 0, mDuration);
            triggleCallback(mCursorCorrectIndex);
            invalidate();
        }
    }

    public int getCurrentCursorIndex() {
        return (int) cursorIndex;
    }

    private void setDefaultStartDrawable(int resId) {
        defaultDrawableId = resId;
        invalidate();
    }

    private void setMaskStartDrawable(int resId) {
        maskDrawableId = resId;
        invalidate();
    }

    private void setSelectedStartDrawable(int resId) {
        maskDrawableId = resId;
        invalidate();
    }

    public void setOnCursorChangeListener(OnCursorChangeListener cursorChangeListener) {
        mListener = cursorChangeListener;
    }

    public interface OnCursorChangeListener {

        void onCursorChanged(int location);

    }

}
