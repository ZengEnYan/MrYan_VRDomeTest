package com.mryan.test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.vr.sdk.widgets.common.VrWidgetView;
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener;
import com.google.vr.sdk.widgets.pano.VrPanoramaView;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private VrPanoramaView vrPanoramaView;
    private ImageLoaderTask imageLoaderTask;
    /**
     * 1.在项目开始时创建一个assets包用来放我们所需要的资源文件
     * 2.在清单文件下的Application节点中加入android:largeHeap="true"节点，设置为true
     *    内存警报线一般在192m 设置后会增加到512m
     * 3.导入VR所需要的依赖库，以导model的方式导入
     *
     * 4.这样还有错误，我们还需要在build文件里dependencies,添加：compile 'com.google.protobuf.nano:protobuf-javanano:3.0.0-alpha-7'
     *      如果不添加将会导致项目崩溃，报出异常；
     *
     *      位置会是 vrPanoramaView.setDisplayMode(VrWidgetView.DisplayMode.FULLSCREEN_STEREO);报错
     *      还有重新获取焦点时同样会报错
     *      protected void onResume() {
     *      //重新获取焦点，并渲染显示
     *      super.onResume();
     *      //这行报错
     *      vrPanoramaView.resumeRendering();
     *      }
     *
     *  5.在项目XML中布局VrPanoramaView
     * 6.由于VR文件较大，所以需要开启一个线程或者利用异步加载
     * 7.由于VR很占用内存，所以需要给VR设置暂停  继续  和销毁的状态
     * 8.设置对VR运行状态的监听,如果VR运行出现错误,可以及时的处理.
     * 9.播放VR效果,只需执行异步任务即可.
     * @param savedInstanceState
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化View
        //对VR控件进行初始化
        vrPanoramaView = (VrPanoramaView) findViewById(R.id.vrPanoramaView);
        //因按钮跳转到的是Google网站，不易加载，所以隐藏掉VR效果左下角的信息按钮显示
        vrPanoramaView.setInfoButtonEnabled(false);
        //因不需要全屏，所以隐藏了它
        vrPanoramaView.setFullscreenButtonEnabled(false);
        //切换VR的模式   参数: VrWidgetView.DisplayMode.FULLSCREEN_STEREO设备模式(手机横着放试试)
        //   VrWidgetView.DisplayMode.FULLSCREEN_MONO手机模式
        //这行报错
        vrPanoramaView.setDisplayMode(VrWidgetView.DisplayMode.FULLSCREEN_STEREO);
        //设置对VR运行状态的监听,如果VR运行出现错误,可以及时处理.
        vrPanoramaView.setEventListener(new MyVREventListener());
        //使用自定义的AsyncTask,播放VR效果
        imageLoaderTask = new ImageLoaderTask();
        imageLoaderTask.execute();
    }
    /**
     * B.自定义一个类继承AsyncTask,只使用我们需要的方法.
     * 由于VR资源数据量大,获取需要时间,故把加载图片放到子线程中进行,主线程来显示图片,故可以使用一个异步线程AsyncTask或EventBus来处理.
     */
    private class ImageLoaderTask extends AsyncTask<Void, Void , Bitmap>{
        //这个方法在子线程中，把我们本地的资源文件加载到内存中
        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                //从资源文件中拿到资源，返回的结果为字节流
                InputStream inputStream = getAssets().open("andes.jpg");
                //把字节流转换成Bitmap对象
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            //创建bVrPanoramaView.Options,去决定显示VR是普通效果,还是立体效果
            VrPanoramaView.Options options = new VrPanoramaView.Options();
            //TYPE_STEREO_OVER_UNDER立体效果:图片的上半部分放在左眼显示,下半部分放在右眼显示     TYPE_MONO:普通效果
            options.inputType=VrPanoramaView.Options.TYPE_STEREO_OVER_UNDER;
            //使用VR控件对象,显示效果  参数:1.Bitmap对象      2.VrPanoramaView.Options对象,决定显示的效果
            vrPanoramaView.loadImageFromBitmap(bitmap, options);
            super.onPostExecute(bitmap);
        }
    }
    //在onPause中暂停VR，失去焦点
    @Override
    protected void onPause() {
        vrPanoramaView.pauseRendering();
        super.onPause();
    }
    //在onResume中重新获取焦点
    @Override
    protected void onResume() {
        //重新获取焦点，并渲染显示
        super.onResume();
        //这行报错
        vrPanoramaView.resumeRendering();
    }
    //销毁Activity，销毁VR
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭渲染视图
        vrPanoramaView.shutdown();
        if(imageLoaderTask!=null){
            //在退出Activity时，如果异步任务没有取消的话，咱就取消下
            if(!imageLoaderTask.isCancelled()){
                //取消异步任务
                imageLoaderTask.cancel(true);
            }
        }
    }
    //VR运行状态监听类,自定义一个类继承VrPanoramaEventListener,复写里面的两个方法
    private class MyVREventListener extends VrPanoramaEventListener {
        //当VR视图加载成功的时候回调
        @Override
        public void onLoadSuccess() {
            super.onLoadSuccess();
            Toast.makeText(MainActivity.this, "加载成功", Toast.LENGTH_SHORT).show();
        }
        //当VR视图加载失败的时候回调
        @Override
        public void onLoadError(String errorMessage) {
            super.onLoadError(errorMessage);
            Toast.makeText(MainActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
        }
    }
}
