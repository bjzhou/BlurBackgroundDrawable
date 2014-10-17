package com.android.systemui.statusbar.phone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.WindowManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("NewApi")
public class BlurBackgroundDrawable extends Drawable implements Animatable, Runnable{

    public static final long DURATION = 40;
    private boolean mRunning = false;
    private Context mContext;
    private Rect mDstRect = new Rect();
    private Display mDisplay;
    private Paint mPaint = new Paint();

    private static RenderScript rs;
    private static Bitmap mScreenshot;
    private static int SCREEN_WIDTH;
    private static int SCREEN_HEIGHT;

    public BlurBackgroundDrawable(Context context) {
        mContext = context;
        rs = RenderScript.create(context);
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
        SCREEN_WIDTH = mDisplay.getWidth();
        SCREEN_HEIGHT = mDisplay.getHeight();
    }

    @Override
    public void start() {
        if (!isRunning()) {
            mRunning = true;
            run();
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            mRunning = false;
            unscheduleSelf(this);
        }
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mScreenshot != null) {
            Gravity.apply(Gravity.FILL, mScreenshot.getWidth(), mScreenshot.getHeight(), getBounds(), mDstRect);
            canvas.drawBitmap(mScreenshot, null, mDstRect, mPaint );
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public void run() {
        new MyAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    class MyAsyncTask extends AsyncTask<Void, Void, Void> {
        long duration;

        @Override
        protected Void doInBackground(Void... params) {
            duration = System.currentTimeMillis();
            Bitmap sceenshot = Bitmap.createBitmap(getScreenshot(mContext), 0, mDisplay.getRotation() == Surface.ROTATION_270 ? getNavigationBarHegiht(mContext) / 3 : 0, SCREEN_WIDTH / 3, SCREEN_HEIGHT / 3 - getNavigationBarHegiht(mContext) / 3);;
            sceenshot = fastblur(mContext, sceenshot);
            int rotate = 0;
            switch (mDisplay.getRotation()) {
                case Surface.ROTATION_90:
                    rotate = 270;
                    break;
                case Surface.ROTATION_270:
                    rotate = 90;
                    break;
                default:
                    rotate = 0;
                    break;
            }
            if (rotate != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotate);
                sceenshot = Bitmap.createBitmap(sceenshot,0,0,sceenshot.getWidth(),sceenshot.getHeight(),m,true);
            }
            mScreenshot = sceenshot;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            duration = System.currentTimeMillis() - duration;
            Log.d("bjzhou", "duration: " + duration + "ms");
            duration = 41 - duration;
            if (mRunning) {
                scheduleSelf(BlurBackgroundDrawable.this, SystemClock.uptimeMillis() + duration >= 0 ? duration : 0);
                invalidateSelf();
            }
        }}
    
    private static Bitmap getScreenshot(Context context) {


        Bitmap shotBitmap = null;
        try {
            final Class<?> SurfaceControl = Class.forName("android.view.SurfaceControl");
            if (SurfaceControl != null) {
                final Method screenshot = SurfaceControl.getDeclaredMethod("screenshot", int.class, int.class, int.class, int.class);
                shotBitmap = (Bitmap) screenshot.invoke(null, 
                        SCREEN_WIDTH / 3, 
                        SCREEN_HEIGHT / 3, 
                        20000, 
                        140000);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        
        return shotBitmap;
    }
    
    private int getNavigationBarHegiht(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    
    @SuppressLint("NewApi")
    public static Bitmap fastblur(Context context, Bitmap bitmapOriginal) {
          Bitmap outBitmap = bitmapOriginal.copy(bitmapOriginal.getConfig(), true);
          final Allocation input = Allocation.createFromBitmap(rs, bitmapOriginal); 
          final Allocation output = Allocation.createTyped(rs, input.getType());
          final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
          script.setRadius(12f);
          script.setInput(input);
          script.forEach(output);
          output.copyTo(outBitmap);
          return outBitmap;
      }

}
