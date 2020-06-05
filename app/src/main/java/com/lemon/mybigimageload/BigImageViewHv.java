package com.lemon.mybigimageload;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.Nullable;

/**
 * author : xu
 * date : 2020/6/4 18:47
 * description :  不缩放 左右滑动 展示大图
 */

public class BigImageViewHv extends View implements GestureDetector.OnGestureListener, View.OnTouchListener {
    // 不缩放的话   展示的区域 一直都是view的 区域
    private Rect mRect;
    // 内存复用
    private BitmapFactory.Options options;
    // 手势识别
    private GestureDetector gestureDetector;
    // 滑动
    private Scroller scroller;
    private int imageWidth, imageHeight;
    // 区域解码器
    private BitmapRegionDecoder bitmapRegionDecoder;
    private int viewWidth, viewHeight;
    private float mScale;
    private Bitmap bitmap;

    public BigImageViewHv(Context context) {
        this(context, null);
    }

    public BigImageViewHv(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigImageViewHv(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 1只加载图片的一片区域
        mRect = new Rect();
        options = new BitmapFactory.Options();
        gestureDetector = new GestureDetector(context, this);
        scroller = new Scroller(context, new AccelerateInterpolator());
        setOnTouchListener(this);
    }

    /**
     * 2设置大图片
     * 得到图片的信息
     * 由于只加载一部分
     */
    public void setBigImage(InputStream inputStream) {
        // 要获取图片宽高   并且 不把整个图片加载到内存
        // 如果inJustDecoedBounds设置为true的话，解码bitmap时可以只返回其高、宽和Mime类型，而不必为其申请内存，从而节省了内存空间。
        // 获取图片的宽高
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        imageWidth = options.outWidth;
        imageHeight = options.outHeight;
        // 开启内存复用
        options.inMutable = true;
        // 表示图片解码时  使用的颜色模式
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = false;
        Log.e("TAG", "setBigImage  imageWidth:" + imageWidth + "---imageHeight:" + imageHeight);

        // 创建 区域解码器   因为要分区域加载
        try {
            bitmapRegionDecoder = BitmapRegionDecoder.newInstance(inputStream, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestLayout();
    }

    /**
     * 3测量 图片的缩放因子     将大图的宽度 缩放成与屏幕的宽度一样   等比缩放 图片的高度
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = getMeasuredWidth();
        viewHeight = getMeasuredHeight();
        mRect.left = 0;
        mRect.top = 0;
        mRect.right = viewWidth;
        mRect.bottom = viewHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 4区域解码器为空的 话  不执行
        if (bitmapRegionDecoder == null) {
            return;
        }
        // 绘制时  复用内存  [inBitmap 使用之前 用过的bitmap的内存]
        // 复用的bitmap  只能与 解码的bitmap 尺寸一样
        options.inBitmap = bitmap;
        // 指定解码区域 bitmap
        bitmap = bitmapRegionDecoder.decodeRegion(mRect, options);
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    // 手指按下    手势由于惯性一直向上走的时候     这时暂停
    @Override
    public boolean onDown(MotionEvent motionEvent) {
        if (!scroller.isFinished()) {
            scroller.forceFinished(true);
        }
        // 继续接受后续事件
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    /**
     * 滑动
     *
     * @param motionEvent  手指按下去的事件    比如按下时
     * @param motionEvent1 获取当前事件坐标  比如 滑动时
     * @param X            x 轴   相当于移动前    移动的距离
     * @param Y            y 轴    相当于移动前    移动的距离
     * @return
     */
    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float X, float Y) {
        // 上下移动  改变显示区域  通过mRect
        mRect.offset((int) X, (int) Y);
        // 找到 顶部 底部 滑动的临界点
        if (mRect.bottom > imageHeight) {
            // 滑到底部
            mRect.bottom = imageHeight;
            // 区域顶部 = 图片高度 - 区域高度
            mRect.top = imageHeight - viewHeight;
        } else if (mRect.top < 0) {
            // 滑到顶部
            mRect.top = 0;
            // 区域底部  =  根据缩放比例 计算的
            mRect.bottom = viewHeight;
        }
        // 左右边距处理
        if (mRect.left < 0) {
            // 滑到顶部
            mRect.left = 0;
            // 区域底部  =  根据缩放比例 计算的
            mRect.right = viewWidth;
        } else if (mRect.right > imageWidth) {
            mRect.right = imageWidth;
            mRect.left = imageWidth - viewWidth;
        }
        invalidate();

        Log.e("TAG", "---------------------onScroll X:" + X + "--Y:" + Y);
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    /**
     * 惯性滑动
     *
     * @param motionEvent
     * @param motionEvent1
     * @param x            x轴移动的距离
     * @param y            y轴移动的距离
     * @return
     */
    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float x, float y) {
        // 手势交给  scroller去计算
        // startX ： 移动前的初始x 值    startY: 移动前初始的y
        // velocityX :  x 速度 单位像素  velocityY: y方向速度  单位像素

        // 以下 4个参数 都是相对初始值来说
        // minX : x  相对于参数1来说   即向左 最大滑动距离   [用于界定 左侧边界 ]   maxX  : 相对于参数1来说   向右最大滑动距离  [右侧滑动边界]      0 左右不可滑动
        // minY : Y 相对于参数2 来说  向上最大滑动距离      maxY ：相对于参数2 来说 向下最大滑动距离
        scroller.fling( mRect.left, mRect.top, (int) -x, (int) -y, 0, imageWidth - viewWidth, 0, imageHeight - viewHeight);
        return false;
    }

    /**
     * 处理计算结果
     */
    @Override
    public void computeScroll() {
        if (scroller.isFinished()) {
            return;
        }
        // 当这个返回为true的 时候 意味着 滑动还没结束   应该一直绘制
        if (scroller.computeScrollOffset()) {
            mRect.top = scroller.getCurrY();
            mRect.bottom = mRect.top + viewHeight;
            mRect.left = scroller.getCurrX();
            mRect.right = mRect.left + viewWidth;
        }
        invalidate();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        // 直接交给手势事件处理
        return gestureDetector.onTouchEvent(motionEvent);
    }
}


