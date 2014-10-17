# BlurBackgroundDrawable

---


实时动态模糊效果的实现

其中getScreenshot中的SurfaceControl.screenshot方法需要权限android.permission.READ_FRAME_BUFFER,应用内使用可用view.getDrawingCache代替

使用方法:

BlurBackgroundDrawable drawable = new BlurBackgroundDrawable(context);
drawable.start();
imageView.setBackground(drawable);
