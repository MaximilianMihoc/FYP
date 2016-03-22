package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.util.ArrayList;

/**
 * Created by Maximilian on 04/02/2016.
 */
public class TestBehaviouralBiometrics extends Activity implements
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, SensorEventListener
{
    Firebase ref;

    private static final String DEBUG_TAG = "Test Activity";
    private final static double threshold = 70; //Max
    private int guestsObservationsNeeded = 14;
    private double high = 15; // Max Reword or Penalty
    private double low = 0;   // Min Reward or Penalty
    private double userTrust;

    private GestureDetectorCompat mDetector;

    //to be used for assigning listener to different views.
    public View.OnTouchListener gestureListener;

    Point startPoint, endPoint;
    ArrayList<Point> points = new ArrayList<>();
    ArrayList<Observation> trainScrollFlingObservations;
    ArrayList<Observation> scrollFlingObservations;
    ArrayList<Observation> tapOnlyObservations;
    ArrayList<Observation> trainTapOnlyObservations;

    private ArrayList<Float> linearAccelerations;
    private ArrayList<Float> angularVelocities;

    boolean isScroll = false;
    boolean isFling = false;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;

    private Float linearAcceleration;
    private Float angularVelocity;

    SharedPreferences sharedpreferences;
    private String userID;

    private static SVM scrollFlingSVM;
    private static SVM tapSVM;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);

        linearAcceleration = 0.0f;
        angularVelocity = 0.0f;
        userTrust = 100;

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Accelerometer declarations
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        // Gyroscope declarations
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        scrollFlingObservations = new ArrayList<>();
        trainScrollFlingObservations = new ArrayList<>();
        tapOnlyObservations = new ArrayList<>();
        trainTapOnlyObservations = new ArrayList<>();
        points = new ArrayList<>();
        linearAccelerations = new ArrayList<>();
        angularVelocities = new ArrayList<>();

        // get User details
        userID = sharedpreferences.getString("UserID", "");

        /* Get user training data from Firebase - Owner and Guest data */
        getTrainDataFromFirebase();

        gestureListener = new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                // need to call the gesture detector first so that the strokes can be differentiated from taps
                mDetector.onTouchEvent(event);

                //assignValuesToObservations(event);
                //add linear Acceleration and angular Velocity to list
                linearAccelerations.add(linearAcceleration);
                angularVelocities.add(angularVelocity);

                double duration;

                int action = event.getAction();
                switch (action)
                {
                    case (MotionEvent.ACTION_DOWN):
                    {
                        startPoint = new Point(event.getX(), event.getY());
                        return false;
                    }
                    case (MotionEvent.ACTION_MOVE):
                    {
                        Point newP = new Point(event.getX(), event.getY());
                        points.add(newP);
                        return false;
                    }
                    case (MotionEvent.ACTION_UP):
                    {
                        endPoint = new Point(event.getX(), event.getY());
                        duration = event.getEventTime() - event.getDownTime();
                        Observation tempObs = new Observation();

                        if(isFling || isScroll)
                        {
                            //touch = scrollFling
                            ScrollFling scrollFling = new ScrollFling();
                            scrollFling.setStartPoint(startPoint);
                            scrollFling.setEndPoint(endPoint);
                            scrollFling.initialisePoints(points);
                            scrollFling.setDuration(duration);

                            scrollFling.setMidStrokeAreaCovered(scrollFling.calculateMidStrokeAreaCovered());
                            scrollFling.setMeanDirectionOfStroke(scrollFling.calculateMeanDirectionOfStroke());
                            scrollFling.setDirectEndToEndDistance(scrollFling.calculateDirectEndToEndDistance());
                            scrollFling.setAngleBetweenStartAndEndVectorsInRad(scrollFling.calculateAngleBetweenStartAndEndVectorsInRad());

                            //Log.d(DEBUG_TAG, "ScrollFling: " + scrollFling.toString());
                            //tempObs.setScrollFling(scrollFling);
                            tempObs.setTouch(scrollFling);
                        } else
                        {
                            //touch = tap
                            Tap tap = new Tap();
                            tap.setStartPoint(startPoint);
                            tap.setEndPoint(endPoint);
                            tap.initialisePoints(points);
                            tap.setDuration(duration);

                            tap.setMidStrokeAreaCovered(tap.calculateMidStrokeAreaCovered());

                            //Log.d(DEBUG_TAG, "Tap: " + tap.toString());
                            //tempObs.setTap(tap);
                            tempObs.setTouch(tap);

                        }

                        //adding the lists of linearAccelerations and AngularVelocity to the Observation
                        tempObs.setAverageAngularVelocity(Observation.calculateAVGAngularVelocity(angularVelocities));
                        tempObs.setAverageLinearAcceleration(Observation.calculateAVGLinearAcc(linearAccelerations));

                        //In this section check each observation, one at a time and assign a judgement to it.
                        if(!isFling && !isScroll)
                        {
                            tapOnlyObservations.add(tempObs);

                            if(tapOnlyObservations.size() % 6 == 0)
                            {
                                Mat testDataMat = buildTrainOrTestMatForTaps(tapOnlyObservations);
                                // create the result Mat
                                Mat resultMat = new Mat(tapOnlyObservations.size(), 1, CvType.CV_32S);

                                if (tapSVM.isTrained())
                                {
                                    tapSVM.predict(testDataMat, resultMat, 0);
                                    int counter = countOwnerResults(resultMat);
                                    /*Toast toast = Toast.makeText(getApplicationContext(), "SVM Taps -> " + counter + " / " + tapOnlyObservations.size()
                                            + " -> " + Math.round((counter * 100) / tapOnlyObservations.size()) + "%", Toast.LENGTH_SHORT);
                                    toast.show();*/
                                }
                            }

                            // add test data in Firebase if the check box for adding data is selected.
                            if(OptionsScreen.saveData)
                            {
                                Firebase newUserRef = ref.child("testData").child(userID).child("tap");
                                newUserRef.push().setValue(tempObs);
                            }

                        }
                        else
                        {
                            // add all observations to a list.
                            //scrollFlingObservations.add(tempObs);

                            ArrayList<Observation> testOneObservstion = new ArrayList<>();
                            testOneObservstion.add(tempObs);
                            //if(scrollFlingObservations.size() % 6 == 0)
                            {
                                Mat testDataMat = buildTrainOrTestMatForScrollFling(testOneObservstion);

                                // create the result Mat
                                Mat resultMat = new Mat(testOneObservstion.size(), 1, CvType.CV_32S);

                                if (scrollFlingSVM.isTrained())
                                {
                                    scrollFlingSVM.predict(testDataMat, resultMat, 1);

                                    double observationConfidenceFromSVM =  resultMat.get(0, 0)[0];

                                    System.out.println("Confidence: " + observationConfidenceFromSVM);

                                    if(observationConfidenceFromSVM < 0)
                                    {
                                        //Owner Observation
                                        double newConf = normalizeOwnerConfidence(Math.abs(observationConfidenceFromSVM), 0, 50, high, low);
                                        if(userTrust + newConf > 100)
                                            userTrust = 100;
                                        else
                                            userTrust += newConf;
                                    }
                                    else
                                    {
                                        // guest Observation
                                        double newConf = normalizeOwnerConfidence(Math.abs(observationConfidenceFromSVM), 0, 50, high, low);
                                        if(userTrust - newConf < 0)
                                            userTrust = 0;
                                        else
                                            userTrust -= newConf;
                                    }
                                    Toast toast = Toast.makeText(getApplicationContext(), "" + userTrust, Toast.LENGTH_SHORT);
                                    toast.show();

                                    System.out.println("Confidence Normalized: " + normalizeOwnerConfidence(Math.abs(observationConfidenceFromSVM), 0, 50, high, low));

                                    if(userTrust < threshold)
                                    {
                                        //lock the phone
                                        Intent intent = new Intent(TestBehaviouralBiometrics.this, LogIn.class);
                                        startActivity(intent);

                                    }

                                    //int counter = countOwnerResults(resultMat);
                                    /*Toast toast = Toast.makeText(getApplicationContext(), "SVM Scrolls -> " + counter + " / " + scrollFlingObservations.size()
                                            + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%", Toast.LENGTH_SHORT);
                                    toast.show();*/
                                }
                            }

                            // uncomment the next 2 lines to add test data in Firebase.
                            if(OptionsScreen.saveData)
                            {
                                Firebase newUserRef = ref.child("testData").child(userID).child("scrollFling");
                                newUserRef.push().setValue(tempObs);
                            }
                        }

                        points.clear();
                        linearAccelerations.clear();
                        angularVelocities.clear();
                        isFling = false;
                        isScroll = false;
                        return false;
                    }
                }

                return mDetector.onTouchEvent(event);
            }
        };
    }

    /**
     * Normalize the confidentiality returned by the clasifier in order to be used
     * in the trust model.
     * Owner confidence should be between 10 and 0, 10 being the max reward and 0 min reward
     * Guest confidence between 10 and 0, 10 being the max penalty and 0 being the min penalty
     *
     * */
    private double normalizeOwnerConfidence(double value, double min, double max, double high, double low)
    {
        //normalization formula
        return ((value - min)/(max - min)) * (high - low) + low;
    }

    private int countOwnerResults(Mat mat)
    {
        int counter = 0;
        for (int i = 0; i < mat.rows(); i++)
        {
            if (mat.get(i, 0)[0] == 1) counter++;
        }

        return counter;
    }

    private void getTrainDataFromFirebase()
    {
        final Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println("No Scroll Fling data available. ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                } else
                {
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {
                        //System.out.println("usrSnapshot: " + usrSnapshot.child("scrollFling"));

                        if (!usrSnapshot.getKey().equals(userID))
                        {
                            // Scroll/Fling:
                            DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                            int countGuestObs = 0;
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                if(countGuestObs < guestsObservationsNeeded) trainScrollFlingObservations.add(obs);
                                countGuestObs++;
                            }

                            // Taps:
                            DataSnapshot dpTap = usrSnapshot.child("tap");
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                trainTapOnlyObservations.add(obs);
                            }
                        } else  // get data from the actual user
                        {
                            DataSnapshot scrollSnapshot = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : scrollSnapshot.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                trainScrollFlingObservations.add(obs);
                            }

                            // Built the SVM model for Scroll/Fling Observations if training data exists.
                            if (trainScrollFlingObservations.size() > 0)
                            {
                                scrollFlingSVM = createAndTrainScrollFlingSVMClassifier(trainScrollFlingObservations);

                            } else
                            {
                                System.out.println("No Scroll Fling data available. ");
                                // display a Toast letting the user know that there is no training data available.
                                Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                            //--------------------------------------------------------------------------------------------------------
                            // Tap Information
                            DataSnapshot tapSnapshot = usrSnapshot.child("tap");
                            for (DataSnapshot obsSnapshot : tapSnapshot.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                trainTapOnlyObservations.add(obs);
                            }

                            if (trainTapOnlyObservations.size() > 0)
                            {
                                tapSVM = createAndTrainTapSVMClassifier(trainTapOnlyObservations);

                            } else
                            {
                                System.out.println("No Tap data available. ");
                                // display a Toast letting the user know that there is no training data available.
                                Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided for Taps", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private SVM createAndTrainScrollFlingSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        //initialise scrollFlingSVM
        SVM tempSVM = SVM.create();
        tempSVM.setKernel(SVM.RBF);

        tempSVM.setType(SVM.C_SVC);
        //tempSVM.setType(SVM.NU_SVC);
        //tempSVM.setC(1/Math.pow(2,12));
        tempSVM.setNu(1/Math.pow(2,18.1));

        Mat trainScrollFlingMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat labelsScrollFlingMat = buildLabelsMat(arrayListObservations);

        //System.out.println("Train Matrix is:\n");
        //displayMatrix(trainScrollFlingMat);

        tempSVM.train(trainScrollFlingMat, Ml.ROW_SAMPLE, labelsScrollFlingMat);

        return tempSVM;
    }

    private SVM createAndTrainTapSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        //initialise scrollFlingSVM
        SVM tempSVM = SVM.create();
        tempSVM.setKernel(SVM.RBF);
        //tapSVM.setType(SVM.C_SVC);
        tempSVM.setType(SVM.NU_SVC);
        tempSVM.setC(1/Math.pow(2,13));
        tempSVM.setNu(1/Math.pow(2,18.1));

        Mat trainTapMat = buildTrainOrTestMatForTaps(arrayListObservations);
        Mat labelsTapMat = buildLabelsMat(arrayListObservations);

        //System.out.println("Train Matrix for Tap is:\n");
        //displayMatrix(trainTapMat);

        tempSVM.train(trainTapMat, Ml.ROW_SAMPLE, labelsTapMat);

        return tempSVM;
    }

    private Mat buildLabelsMat(ArrayList<Observation> listObservations)
    {
        Mat labelsTempMat = new Mat(listObservations.size(), 1, CvType.CV_32S);

        for(int i = 0; i < listObservations.size(); i++)
        {
            labelsTempMat.put(i, 0, listObservations.get(i).getJudgement());
        }

        return labelsTempMat;
    }

    private Mat buildTrainOrTestMatForScrollFling(ArrayList<Observation> listObservations)
    {
        Mat tempMat = new Mat(listObservations.size(), ScrollFling.numberOfFeatures, CvType.CV_32FC1);

        for(int i = 0; i < listObservations.size(); i++)
        {
            ScrollFling scrollFlingObs = new ScrollFling(listObservations.get(i).getTouch());
            int j = 0;

            // linear accelerations are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageLinearAcceleration());

            // angular Velocity are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageAngularVelocity());

            tempMat.put(i, j++, scrollFlingObs.getMidStrokeAreaCovered());

            // Angle between start and end vectors
            tempMat.put(i, j++, scrollFlingObs.getAngleBetweenStartAndEndVectorsInRad());

            tempMat.put(i, j++, scrollFlingObs.getDirectEndToEndDistance());

            // Mean Direction
            //tempMat.put(i, j++, scrollFlingObs.getMeanDirectionOfStroke());

            // Stop x
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().x);

            // Start x
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().x);

            // Stroke Duration
            tempMat.put(i, j++, scrollFlingObs.getScaledDuration()/10);

            // Start y
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().y);

            // Stop y
            tempMat.put(i, j, scrollFlingObs.getScaledEndPoint().y);
        }
        System.out.println("Normalise Mat: " );
        displayMatrix(tempMat);
        return tempMat;
    }

    private Mat buildTrainOrTestMatForTaps(ArrayList<Observation> listObservations)
    {
        Mat tempMat = new Mat(listObservations.size(), Tap.numberOfFeatures, CvType.CV_32FC1);

        for(int i = 0; i < listObservations.size(); i++)
        {
            Tap tapInteraction = new Tap(listObservations.get(i).getTouch());
            int j = 0;

            // linear accelerations are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageLinearAcceleration());

            // angular Velocity are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageAngularVelocity());


            tempMat.put(i, j++, tapInteraction.getMidStrokeAreaCovered());

            //Stop x
            tempMat.put(i, j++, tapInteraction.getScaledEndPoint().x);

            // Start x
            tempMat.put(i, j++, tapInteraction.getScaledStartPoint().x);

            //Duration
            tempMat.put(i, j++, tapInteraction.getScaledDuration()/10);

            // Start y
            tempMat.put(i, j++, tapInteraction.getScaledStartPoint().y);

            //Stop y
            tempMat.put(i, j, tapInteraction.getScaledEndPoint().y);

        }

        return tempMat;
    }

    private Mat normalizeMat(Mat toNormalize)
    {
        Mat tempMat = toNormalize.clone();

        for(int col = 0; col < toNormalize.cols(); col++)
        {
            // only normalize data from 2 features that are not properly normalised
            if(col == 5 || col == 8)
            {
                double min = getMinValueOFColumn(toNormalize, col);
                double max = getMaxValueOFColumn(toNormalize, col);

                for (int row = 0; row < toNormalize.rows(); row++)
                {
                    double[] element = toNormalize.get(row, col);
                    tempMat.put(row, col, (element[0] - min) / (max - min));
                }
            }
        }

        return tempMat;
    }

    private double getMinValueOFColumn(Mat mat, int col)
    {
        double min = Double.MAX_VALUE;
        for(int i = 0; i < mat.rows(); i++)
        {
            double [] temp = mat.get(i,col);
            if(temp[0] < min ) min = temp[0];
        }

        return min;
    }

    private double getMaxValueOFColumn(Mat mat, int col)
    {
        double max = Double.MIN_VALUE;
        for(int i = 0; i < mat.rows(); i++)
        {
            double [] temp = mat.get(i,col);
            if(temp[0] > max ) max = temp[0];
        }

        return max;
    }

    @Override
    public boolean onDown(MotionEvent e)
    {
        //add linear Acceleration and angular Velocity to list
        linearAccelerations.add(linearAcceleration);
        angularVelocities.add(angularVelocity);

        return false;
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
    public boolean onSingleTapConfirmed(MotionEvent e)
    {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e)
    {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e)
    {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e)
    {
        //Log.d(DEBUG_TAG, "onShowPress: " + e.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        //add linear Acceleration and angular Velocity to list
        linearAccelerations.add(linearAcceleration);
        angularVelocities.add(angularVelocity);

        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        isScroll = true;
        //add linear Acceleration and angular Velocity to list
        linearAccelerations.add(linearAcceleration);
        angularVelocities.add(angularVelocity);

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e)
    {
        //Log.d(DEBUG_TAG, "onLongPress: " + e.toString());
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        isFling = true;
        //add linear Acceleration and angular Velocity to list
        linearAccelerations.add(linearAcceleration);
        angularVelocities.add(angularVelocity);

        return false;
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

            angularVelocity = (float) Math.sqrt((double) (x*x + y*y + z*z));
        }

        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            //this sensor gives me the linear acceleration values for x, y, z.
            // linear acceleration is the acceleration - earth gravity.
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            linearAcceleration = (float) Math.sqrt((double) (x*x + y*y + z*z));
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
