package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;
import org.opencv.ml.StatModel;

import java.util.ArrayList;
import java.util.List;

public class TestActivity extends Activity implements
        GestureDetector.OnGestureListener,
        SensorEventListener
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

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;

    private float lastLinearAcceleration;
    private float linearAcceleration;

    private float lastAngularVelocity;
    private float angularVelocity;

    Point startPoint, endPoint;
    ArrayList<Point> points = new ArrayList<Point>();
    ArrayList<Observation> trainObservations;
    ArrayList<Observation> testObservations;

    boolean isScroll = false;
    boolean isFling = false;

    SVM svm;

    TextView outputdata;
    ProgressBar progressBar;
    int progressVal;
    String out = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        outputdata = (TextView) findViewById(R.id.predictions);

        linearAcceleration = 0.0f;
        lastLinearAcceleration = 0.0f;

        angularVelocity = 0.0f;
        lastAngularVelocity = 0.0f;

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        progressVal = 10;

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Accelerometer declarations
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        // Gyroscope declarations
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        //initialise svm.
        svm =  SVM.create();
        svm.setKernel(SVM.RBF);
        svm.setType(SVM.ONE_CLASS);
        //svm.setC(0.3);
        //svm.setP(1);
        svm.setGamma(0.001953125);
        svm.setNu(0.00390625);

        mDetector = new GestureDetectorCompat(this, this);
        testObservations = new ArrayList<>();

        Bundle bundle = getIntent().getExtras();
        trainObservations = (ArrayList<Observation>) bundle.getSerializable("trainObservations");

        Mat trainMat = buildTrainOrTestMatFromObservationList(trainObservations);

        Mat labelsMat = new Mat(trainObservations.size(), 1, CvType.CV_32S);

        for(int i = 0; i < trainObservations.size(); i++)
        {
            // all labels are 1 in training scenario. I presume that the owner is performing in the training area.
            labelsMat.put(i, 0, 1);
        }

        // Labels will have to be taken from Judgement Object which has to be created.
        // Remember to populate Judgement objects after predictions.
        // train the system

        System.out.println("Train Matrix is:\n");
        displayMatrix(trainMat);


        boolean isTrained = svm.train(trainMat, Ml.ROW_SAMPLE, labelsMat);

    }

    public Mat buildTrainOrTestMatFromObservationList(ArrayList<Observation> listObservations)
    {
        Mat tempMat = new Mat(listObservations.size(), Observation.numberOfFeatures, CvType.CV_32FC1);

        for(int i = 0; i < listObservations.size(); i++)
        {
            Touch touchGesture = listObservations.get(i).getGesture();
            int j = 0;
            // call scale data function
            touchGesture.scaleData();
            tempMat.put(i, j++, touchGesture.getScaledStartPoint().x);
            tempMat.put(i, j++, touchGesture.getScaledStartPoint().y);
            tempMat.put(i, j++, touchGesture.getScaledEndPoint().x);
            tempMat.put(i, j++, touchGesture.getScaledEndPoint().y);
            tempMat.put(i, j++, touchGesture.getScaledDuration());

            // linear accelerations are part of the observation
            tempMat.put(i, j++, trainObservations.get(i).getLastLinearAcceleration());
            tempMat.put(i, j++, trainObservations.get(i).getLinearAcceleration());

            // angular Velocity are part of the observation
            tempMat.put(i, j++, trainObservations.get(i).getLastAngularVelocity());
            tempMat.put(i, j++, trainObservations.get(i).getAngularVelocity());
        }

        return tempMat;
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

                // add linear accelerations to the Observation
                tempObs.setLinearAcceleration(linearAcceleration);
                tempObs.setLastLinearAcceleration(lastLinearAcceleration);
                Log.d(DEBUG_TAG, "Linear Accelerations on touch - lastLinearAcceleration: " + lastLinearAcceleration + " LinearAcceleration: " + linearAcceleration);

                // add angular velocity to the Observation on touch gesture
                tempObs.setAngularVelocity(angularVelocity);
                tempObs.setLastAngularVelocity(lastAngularVelocity);
                Log.d(DEBUG_TAG, "Angular Velocity on touch - lastAngularVelocity: " + lastAngularVelocity + " Angular Velocity: " + angularVelocity);

                // add observation to testList to remember what observations we tested so far - not used anywhere else
                testObservations.add(tempObs);

                //create a list containing only one Obs which s used to create the test Mat
                ArrayList<Observation> tempObsList = new ArrayList<Observation>();
                tempObsList.add(tempObs);

                Mat testDataMat = buildTrainOrTestMatFromObservationList(tempObsList);

                // create the result Mat
                Mat resultMat = new Mat(tempObsList.size(), 1, CvType.CV_32S);

                svm.predict(testDataMat, resultMat, 0);
                //svm.predict(testDataMat, resultMat, StatModel.RAW_OUTPUT);


                for (int i = 0; i < resultMat.rows(); i++)
                {
                    if((float)resultMat.get(i, 0)[0] == 0.0f)
                    {
                        progressBar.incrementProgressBy(progressVal);
                    }

                    out += "\tpredicted" + i + ": " + (float)resultMat.get(i, 0)[0];
                }

                /*
                * Printing things on console to help me understand the algorithm.
                *
                * */

                Mat supportVectors = svm.getSupportVectors();
                System.out.println("getSupportVectors:\n");
                displayMatrix(supportVectors);
                System.out.println("End get Support Vectors");


                Mat alpha = new Mat(supportVectors.rows(), supportVectors.cols(), CvType.CV_32S);
                double value = svm.getDecisionFunction(0, alpha, supportVectors);
                System.out.println("Decision Function value: " + value);

                System.out.println("Alpha Mat:\n");
                displayMatrix(alpha);
                System.out.println("End get AlphaMat");

                System.out.println("Decision Function Support Vectors:\n");
                displayMatrix(supportVectors);
                System.out.println("End get Support Vectors");

                outputdata.setText(out);

                points.clear();
                isFling = false;
                isScroll = false;
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    //function to display Mat on console
    public void displayMatrix(Mat matrix)
    {
        for(int i=0; i<matrix.rows(); i++)
        {
            for (int j = 0; j < matrix.cols(); j++)
            {
                System.out.print("\t" + (float)matrix.get(i, j)[0]);
            }
            System.out.println("\n");
        }
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
    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_GYROSCOPE)
        {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            lastAngularVelocity = angularVelocity;
            angularVelocity = (float) Math.sqrt((double) (x*x + y*y + z*z));

            //Log.d(DEBUG_TAG, "Sensor - lastAngularVelocity: " + lastAngularVelocity + " angularVelocity: " + angularVelocity);

            //Log.i("Gyroscope ", " x= " + x + " y= " + y + " z= " + z);
        }

        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            //this sensor gives me the linear acceleration values for x, y, z.
            // linear acceleration is the acceleration - earth gravity.

            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            lastLinearAcceleration = linearAcceleration;
            linearAcceleration = (float) Math.sqrt((double) (x*x + y*y + z*z));

            //Log.d(DEBUG_TAG, "Sensor - lastLinearAcceleration: " + lastLinearAcceleration + " LinearAcceleration: " + linearAcceleration);

            //Log.i("Accelerometer", " x= " + x + " y= " + y + " z= " + z);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }
}
