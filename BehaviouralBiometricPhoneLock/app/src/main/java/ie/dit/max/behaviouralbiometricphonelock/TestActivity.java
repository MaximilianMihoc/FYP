package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.util.ArrayList;
import java.util.List;

public class TestActivity extends Activity implements
        GestureDetector.OnGestureListener
{
    //Test if OpenCV initialisation was successful or it failed
    static {
        if(!OpenCVLoader.initDebug()){
            Log.i("OpenCV", "OpenCV initialization failed");
        } else {
            Log.i("OpenCV", "OpenCV initialization successful");
        }
    }

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    Point startPoint, endPoint;
    ArrayList<Point> points = new ArrayList<Point>();
    ArrayList<Observation> trainObservations;
    ArrayList<Observation> testObservations;

    boolean isScroll = false;
    boolean isFling = false;

    SVM svm;

    TextView outputdata;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        outputdata = (TextView) findViewById(R.id.predictions);

        //initialise svm.
        svm =  SVM.create();
        svm.setKernel(SVM.INTER);
        svm.setType(SVM.ONE_CLASS);
        svm.setC(10);
        //svm.setP(1);
        //svm.setGamma(0.5);
        svm.setNu(0.5);

        mDetector = new GestureDetectorCompat(this, this);
        testObservations = new ArrayList<>();

        Bundle bundle = getIntent().getExtras();
        trainObservations = (ArrayList<Observation>) bundle.getSerializable("trainObservations");

        Mat trainMat = new Mat(trainObservations.size(), 6, CvType.CV_32FC1);
        Mat labelsMat = new Mat(trainObservations.size(), 1, CvType.CV_32S);

        for(int i = 0; i < trainObservations.size(); i++)
        {
            Touch touchGesture = trainObservations.get(i).getGesture();
            trainMat.put(i, 0, touchGesture.getStartPoint().x);
            trainMat.put(i, 1, touchGesture.getStartPoint().y);
            trainMat.put(i, 2, touchGesture.getEndPoint().x);
            trainMat.put(i, 3, touchGesture.getEndPoint().y);
            trainMat.put(i, 4, touchGesture.getDuration());
            trainMat.put(i, 5, touchGesture.getPressure());

            // all labels are 1 in training scenario. I presume that the owner is performing in the training area.
            labelsMat.put(i, 0, 1);
        }

        // Labels will have to be taken from Judgement Object which has to be created.
        // Remember to populate Judgement objects after predictions.
        // train the system

        boolean isTrained = svm.train(trainMat, Ml.ROW_SAMPLE, labelsMat);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        this.mDetector.onTouchEvent(event);

        Log.d(DEBUG_TAG, "onTouchEvent: " + event.toString());

        double duration;

        int action = MotionEventCompat.getActionMasked(event);
        switch(action)
        {
            case (MotionEvent.ACTION_DOWN):
            {
                startPoint = new Point(event.getX(), event.getY());

                return true;
            }
            case (MotionEvent.ACTION_MOVE):
            {
                Point newP = new Point(event.getX(), event.getY());
                points.add(newP);

                return true;
            }
            case (MotionEvent.ACTION_UP):
            {
                endPoint = new Point(event.getX(), event.getY());
                duration = event.getEventTime() - event.getDownTime();
                Observation tempObs = new Observation();

                if(isFling)
                {
                    //touch = fling
                    Fling fling = new Fling();
                    fling.setStartPoint(startPoint);
                    fling.setEndPoint(endPoint);
                    fling.setPoints(points);
                    fling.setDuration(duration);
                    fling.setPressure(event.getPressure());

                    Log.d(DEBUG_TAG, "Fling: " + fling.toString());
                    tempObs.setGesture(fling);
                }
                else if(isScroll)
                {
                    //touch = scroll
                    Scroll scroll = new Scroll();
                    scroll.setStartPoint(startPoint);
                    scroll.setEndPoint(endPoint);
                    scroll.setPoints(points);
                    scroll.setDuration(duration);
                    scroll.setPressure(event.getPressure());

                    Log.d(DEBUG_TAG, "Scroll: " + scroll.toString());
                    tempObs.setGesture(scroll);

                }
                else
                {
                    //touch = tap
                    Tap tap = new Tap();
                    tap.setStartPoint(startPoint);
                    tap.setEndPoint(endPoint);
                    tap.setPoints(points);
                    tap.setDuration(duration);
                    tap.setPressure(event.getPressure());

                    Log.d(DEBUG_TAG, "Tap: " + tap.toString());
                    tempObs.setGesture(tap);
                }

                // add observation to testList to remember what observations we tested so far
                testObservations.add(tempObs);

                Mat testDataMat = new Mat(testObservations.size(), 6, CvType.CV_32FC1);
                //write code to predict one Observation at a time. This can be later written in a separate function
                for(int i = 0; i < testObservations.size(); i++)
                {
                    Touch touchGesture = testObservations.get(i).getGesture();
                    testDataMat.put(i, 0, touchGesture.getStartPoint().x);
                    testDataMat.put(i, 1, touchGesture.getStartPoint().y);
                    testDataMat.put(i, 2, touchGesture.getEndPoint().x);
                    testDataMat.put(i, 3, touchGesture.getEndPoint().y);
                    testDataMat.put(i, 4, touchGesture.getDuration());
                    testDataMat.put(i, 5, touchGesture.getPressure());

                }
                // create the result Mat
                Mat resultMat = new Mat(testObservations.size(), 1, CvType.CV_32S);

                svm.predict(testDataMat, resultMat, 0);

                String out = "";
                for (int i = 0; i < resultMat.rows(); i++)
                {
                    out += "\tpredicted" + i + ": " + (float)resultMat.get(i, 0)[0];
                }

                outputdata.setText(out);

                points.clear();
                isFling = false;
                isScroll = false;
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e)
    {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e)
    {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        isScroll = true;
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e)
    {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        isFling = true;
        return true;
    }
}
