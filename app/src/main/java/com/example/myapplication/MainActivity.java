package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements
        CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {
        private CameraBridgeViewBase cameraView;
        private CascadeClassifier classifier;
        private CascadeClassifier classifierupper;
        private Mat mGray;
        private Mat mRgba;
        private int mAbsoluteFaceSize = 0;
        private int mAbsoluteupperbody = 0;
        private boolean isFrontCamera;
        float mRelativeFaceSize = 0.1f;
        float mRelativeupperbody = 0.2f;

        // 手动装载openCV库文件，以保证手机无需安装OpenCV Manager
        static {
                System.loadLibrary("opencv_java3");
        }
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                initWindowSettings();
                setContentView(R.layout.activity_main);
                cameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
                cameraView.setCvCameraViewListener(this); // 设置相机监听
                initClassifier();
                upperinitClassifier();
                cameraView.enableView();
                Button switchCamera = (Button) findViewById(R.id.switch_camera);
                switchCamera.setOnClickListener(this); // 切换相机镜头，默认后置
        }
        @Override
        public void onClick(View v) {
                switch (v.getId()) {
                        case R.id.switch_camera:
                                cameraView.disableView();
                                if (isFrontCamera) {
                                        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                                        isFrontCamera = false;
                                } else {
                                        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                                        isFrontCamera = true;
                                }
                                cameraView.enableView();
                                break;
                        default:
                }
        }

        private void initWindowSettings() {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
        // 初始化人脸级联分类器，必须先初始化
        private void initClassifier() {
                try {
                        InputStream is = getResources()
                                .openRawResource(R.raw.lbpcascade_frontalface_improved);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface_improved.xml");
                        FileOutputStream os = new FileOutputStream(cascadeFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
        private void upperinitClassifier() {
                try {
                        InputStream is = getResources()
                                .openRawResource(R.raw.haarcascade_upperbody);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File cascadeFile = new File(cascadeDir, "haarcascade_upperbody.xml");
                        FileOutputStream os = new FileOutputStream(cascadeFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        classifierupper = new CascadeClassifier(cascadeFile.getAbsolutePath());
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
        @Override
        public void onCameraViewStarted(int width, int height) {
                mGray = new Mat();
                mRgba = new Mat();
        }
        @Override
        public void onCameraViewStopped() {
                mGray.release();
                mRgba.release();
        }
        int[] location={5,4,3,2,1,0,-1,-2,-3,-4,-5};
        int[] locationh={-5,-4,-3,-2,-1,0,1,2,3,4,5};


        @Override
        public Mat
                onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                        mRgba = inputFrame.rgba();
                        mGray = inputFrame.gray();

                        int[] placew=new int[11];
                int[] placeh=new int[11];
                        float screenwidth;
                        float screenhight;
                        int checkbody = 0;
                        int orientation = getResources().getConfiguration().orientation;
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                Mat rotImage = Imgproc.getRotationMatrix2D(new Point(mRgba.cols() /2,
                                        mRgba.rows()/2), 0, 1.5);
                                if (isFrontCamera) {
                                        Core.flip(mRgba, mRgba, 1);
                                        Core.flip(mGray, mGray, 1);
                                }
                                else {
                                        Core.flip(mRgba, mRgba, 1);
                                        Core.flip(mGray, mGray, 1);
                                }
                                screenwidth=mRgba.width();
                                screenhight=mRgba.height();
                                int wsize= (int) screenwidth;
                                int wdiv= (int) (screenwidth/11);
                                for(int i=0;i<11;i+=1){

                                        placew[i]= wsize-wdiv;
                                        wsize=wsize-wdiv;

                                }

                                int hsize= (int) screenhight;
                                int hdiv= (int) (screenhight/11);
                                for(int i=0;i<11;i+=1){

                                        placeh[i]= hsize-hdiv;
                                        hsize=hsize-hdiv;

                                }
                                if (mAbsoluteFaceSize == 0)
                                {
                                        int height = mGray.rows();
                                        if (Math.round(height * mRelativeFaceSize) > 0)
                                        {
                                                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                                        }
                                }
                                if (mAbsoluteupperbody == 0)
                                {
                                        int height = mGray.rows();
                                        if (Math.round(height * mRelativeupperbody) > 0)
                                        {
                                                mAbsoluteupperbody = Math.round(height * mRelativeupperbody);
                                        }
                                }

                                if (classifierupper != null){

                                        MatOfRect upperbody = new MatOfRect();
                                        classifierupper.detectMultiScale(mGray, upperbody, 1.1, 5, 2,
                                                new Size(mAbsoluteupperbody, mAbsoluteupperbody), new Size());
                                        Rect[] upperArray = upperbody.toArray();
                                        Scalar upperRectColor = new Scalar(0, 255, 0, 255);

                                        for (Rect faceRect : upperArray)
                                        {
                                                Imgproc.rectangle(mRgba, faceRect.tl(), faceRect.br(), upperRectColor, 9);
                                                int objectsizewidth
                                                        = (int) ((faceRect.br().x-faceRect.tl().x)/2)+(int)faceRect.tl().x;
                                                int objectsizehight
                                                        = (int) ((faceRect.br().y-faceRect.tl().y)/2)+(int)faceRect.tl().y;
                                                int lchi = 0;
                                                int lcwi=0;
                                                for(int i=0;i<11;i++){
                                                        if(i+1<11){
                                                                if(objectsizewidth<=placew[i]&&objectsizewidth>=placew[i+1]){
                                                                        lcwi=i;
                                                                        break;
                                                                }}
                                                }

                                                for(int i=0;i<11;i++){
                                                        if(i+1<11){
                                                                if(objectsizehight<=placeh[i]&&objectsizehight>=placeh[i+1]){
                                                                        lchi=i;
                                                                        break;
                                                                }
                                                        }

                                                }
                                                if(checkbody==0){
                                                        Imgproc.putText (mRgba,
                                                                " width== "+location[lcwi+1]+" hight=="+locationh[lchi+1]
                                                                , new Point(10, 50)
                                                                , Core.FONT_HERSHEY_SIMPLEX
                                                                , 1
                                                                , new Scalar(0, 255, 0, 255)
                                                                , 4);
                                                checkbody=1;
                                        }}
                                }
                                if (classifier != null)
                                {
                                        MatOfRect faces = new MatOfRect();
                                        classifier.detectMultiScale(mGray, faces, 1.1, 4, 2,
                                                new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
                                        Rect[] facesArray = faces.toArray();
                                        Scalar faceRectColor = new Scalar( 0, 0,  0,0);

                                        for (Rect faceRect : facesArray)
                                        {
                                                Imgproc.rectangle(mRgba, faceRect.tl(), faceRect.br(), faceRectColor, 10);
                                                int objectsizewidth
                                                        = (int) ((faceRect.br().x-faceRect.tl().x)/2)+(int)faceRect.tl().x;
                                                int objectsizehight
                                                        = (int) ((faceRect.br().y-faceRect.tl().y)/2)+(int)faceRect.tl().y;
                                                int lchi = 0;
                                                int lcwi=0;
                                                for(int i=0;i<11;i++){
                                                        if(i+1<11){
                                                                if(objectsizewidth<=placew[i]&&objectsizewidth>=placew[i+1]){
                                                                        lcwi=i;
                                                                        break;
                                                                }}
                                                }

                                                for(int i=0;i<11;i++){
                                                        if(i+1<11){
                                                                if(objectsizehight<=placeh[i]&&objectsizehight>=placeh[i+1]){
                                                                        lchi=i;
                                                                        break;
                                                                }
                                                        }

                                                }
                                                if(checkbody==0){
                                                        Imgproc.putText (mRgba,
                                                                " width== "+location[lcwi+1]+" hight=="+locationh[lchi+1]
                                                                , new Point(10, 50)
                                                                , Core.FONT_HERSHEY_SIMPLEX
                                                                , 1
                                                                , new Scalar(0, 255, 0, 255)
                                                                , 4);
                                        }
                                        }
                                }




                        }
                        else {

                                Mat rotImage = Imgproc.getRotationMatrix2D(new Point(mRgba.cols() / 2,
                                        mRgba.rows() / 2), 90, 1.5);
                                Imgproc.warpAffine(mRgba, mRgba, rotImage, mRgba.size());
                                Imgproc.warpAffine(mGray, mGray, rotImage, mRgba.size());
                                screenwidth = mRgba.width();
                                screenhight = mRgba.height();
                                int wsize = (int) screenwidth;
                                int wdiv = (int) (screenwidth / 11);
                                for (int i = 0; i < 11; i += 1) {

                                        placew[i] = wsize - wdiv;
                                        wsize = wsize - wdiv;

                                }

                                int hsize = (int) screenhight;
                                int hdiv = (int) (screenhight / 11);
                                for (int i = 0; i < 11; i += 1) {

                                        placeh[i] = hsize - hdiv;
                                        hsize = hsize - hdiv;

                                }
                                if (isFrontCamera) {
                                        Core.flip(mRgba, mRgba, 1);
                                        Core.flip(mGray, mGray, 1);
                                } else {
                                        Core.flip(mRgba, mRgba, 1);
                                        Core.flip(mGray, mGray, 1);
                                        Core.flip(mRgba, mRgba, -1);
                                        Core.flip(mGray, mGray, -1);
                                }
                                if (mAbsoluteFaceSize == 0) {
                                        int height = mGray.rows();
                                        if (Math.round(height * mRelativeFaceSize) > 0) {
                                                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                                        }
                                }
                                if (mAbsoluteupperbody == 0) {
                                        int height = mGray.rows();
                                        if (Math.round(height * mRelativeupperbody) > 0) {
                                                mAbsoluteupperbody = Math.round(height * mRelativeupperbody);
                                        }
                                }
                                if (classifierupper != null) {
                                        MatOfRect upperbody = new MatOfRect();
                                        classifierupper.detectMultiScale(mGray, upperbody, 1.1, 5, 2,
                                                new Size(mAbsoluteupperbody, mAbsoluteupperbody), new Size());
                                        Rect[] upperArray = upperbody.toArray();
                                        Scalar upperRectColor = new Scalar(0, 255, 0, 255);
                                        for (Rect faceRect : upperArray) {
                                                Imgproc.rectangle(mRgba, faceRect.tl(), faceRect.br(), upperRectColor, 9);
                                                int objectsizewidth
                                                        = (int) ((faceRect.br().x - faceRect.tl().x) / 2) + (int) faceRect.tl().x;
                                                int objectsizehight
                                                        = (int) ((faceRect.br().y - faceRect.tl().y) / 2) + (int) faceRect.tl().y;
                                                int lchi = 0;
                                                int lcwi = 0;
                                                for (int i = 0; i < 11; i++) {
                                                        if (i + 1 < 11) {
                                                                if (objectsizewidth <= placew[i] && objectsizewidth >= placew[i + 1]) {
                                                                        lcwi = i;
                                                                        break;
                                                                }
                                                        }
                                                }

                                                for (int i = 0; i < 11; i++) {
                                                        if (i + 1 < 11) {
                                                                if (objectsizehight <= placeh[i] && objectsizehight >= placeh[i + 1]) {
                                                                        lchi = i;
                                                                        break;
                                                                }
                                                        }

                                                }
                                                if (checkbody == 0) {
                                                        Imgproc.putText(mRgba,
                                                                " width== " + location[lcwi + 1] + " hight==" + locationh[lchi + 1]
                                                                , new Point(10, 50)
                                                                , Core.FONT_HERSHEY_SIMPLEX
                                                                , 1
                                                                , new Scalar(0, 255, 0, 255)
                                                                , 4);
                                                        checkbody = 1;
                                                }
                                        }}
                                        if (classifier != null) {
                                                MatOfRect faces = new MatOfRect();
                                                classifier.detectMultiScale(mGray, faces, 1.1, 4, 2,
                                                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
                                                Rect[] facesArray = faces.toArray();
                                                Scalar faceRectColor = new Scalar(0, 0, 0, 0);
                                                for (Rect faceRect : facesArray) {
                                                        Imgproc.rectangle(mRgba, faceRect.tl(), faceRect.br(), faceRectColor, 10);
                                                        int objectsizewidth
                                                                = (int) ((faceRect.br().x-faceRect.tl().x)/2)+(int)faceRect.tl().x;
                                                        int objectsizehight
                                                                = (int) ((faceRect.br().y-faceRect.tl().y)/2)+(int)faceRect.tl().y;
                                                        int lchi = 0;
                                                        int lcwi=0;
                                                        for(int i=0;i<11;i++){
                                                                if(i+1<11){
                                                                        if(objectsizewidth<=placew[i]&&objectsizewidth>=placew[i+1]){
                                                                                lcwi=i;
                                                                                break;
                                                                        }}
                                                        }

                                                        for(int i=0;i<11;i++){
                                                                if(i+1<11){
                                                                        if(objectsizehight<=placeh[i]&&objectsizehight>=placeh[i+1]){
                                                                                lchi=i;
                                                                                break;
                                                                        }
                                                                }

                                                        }
                                                        if(checkbody==0){
                                                                Imgproc.putText (mRgba,
                                                                        " width== "+location[lcwi+1]+" hight=="+locationh[lchi+1]
                                                                        , new Point(10, 50)
                                                                        , Core.FONT_HERSHEY_SIMPLEX
                                                                        , 1
                                                                        , new Scalar(0, 255, 0, 255)
                                                                        , 4);
                                                }
                                        }


                                }}


                        return mRgba;
        }


        @Override
        protected void onPause() {
                super.onPause();
                if (cameraView != null) {
                        cameraView.disableView();
                }
        }
        @Override
        protected void onDestroy() {
                super.onDestroy();
                cameraView.disableView();
        }
}