package com.stevillis.opencvcamera;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    Mat imgRGBA, imgGray, imgCanny;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        javaCameraView = findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null) javaCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null) javaCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded succesfully");

            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        imgRGBA = new Mat(height, width, CvType.CV_8UC4);
        imgGray = new Mat(height, width, CvType.CV_8UC1);
        imgCanny = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        imgRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        imgRGBA = inputFrame.rgba();
        return detectPaper(imgRGBA);
    }

    private Mat detectPaper(Mat img) {
        Imgproc.cvtColor(imgRGBA, imgGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(imgGray, imgCanny, 75, 20);
        //return imgCanny;

        /* Book Code */
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(imgCanny, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.size() == 0) {
            Log.i(TAG, "Não encontrei contornos!");
            return imgRGBA;
        }

        // Encontra o contorno com maior área
        int index = 0;
        double maxim = Imgproc.contourArea(contours.get(0));
        for (int contourIdx = 1; contourIdx < contours.size(); contourIdx++) {
            double temp;
            temp = Imgproc.contourArea(contours.get(contourIdx));

            if (maxim < temp) {
                maxim = temp;
                index = contourIdx;
            }
        }

        double peri = 0.02 * Imgproc.arcLength(new MatOfPoint2f(contours.get(index).toArray()), true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(index).toArray()), approx, peri, true);

        if (approx.size().height == 4 && Imgproc.contourArea(contours.get(index)) > 30000) {
            Log.i(TAG, "Height: " + approx.size().height);
            Log.i(TAG, "Width: " + approx.size().width);
            Log.i(TAG, "Área: " + Imgproc.contourArea(contours.get(index)));
            Imgproc.drawContours(imgRGBA, contours, index, new Scalar(0, 255, 0), 5);
            return imgRGBA;
        }

        return imgRGBA;
    }
}