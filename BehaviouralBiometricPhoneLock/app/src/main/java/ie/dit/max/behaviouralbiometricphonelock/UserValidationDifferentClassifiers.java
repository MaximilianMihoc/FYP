package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.ml.RTrees;
import org.opencv.ml.KNearest;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.util.ArrayList;

public class UserValidationDifferentClassifiers extends AppCompatActivity
{
    Firebase ref;
    private static final String DEBUG_TAG = "Classifiers";
    Spinner spinner;
    ArrayList<Point> points = new ArrayList<>();
    ArrayList<Observation> trainScrollFlingObservations;
    ArrayList<Observation> scrollFlingObservations;
    ArrayList<Observation> tapOnlyObservations;
    ArrayList<Observation> trainTapOnlyObservations;

    private String userID;
    private SVM scrollFlingSVM;
    private SVM tapSVM;
    private KNearest scrollKNN;
    private KNearest tapKNN;
    private RTrees scrollRTree;
    private RTrees tapRTree;

    TextView scrollSVMTextView;
    TextView tapSVMTextView;
    TextView scrollKNNTextView;
    TextView tapKNNTextView;
    TextView scrollRTreeTextView;
    TextView tapRTreeTextView;

    ProgressBar progressBarScrollSVM;
    ProgressBar progressBarTapSVM;
    ProgressBar progressBarScrollKNN;
    ProgressBar progressBarTapKNN;
    ProgressBar progressBarScrollRTree;
    ProgressBar progressBarTapRTree;

    String[] userKeys;
    String[] userNames;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_validation_different_classifiers);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");

        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");

        scrollFlingObservations = new ArrayList<>();
        trainScrollFlingObservations = new ArrayList<>();
        tapOnlyObservations = new ArrayList<>();
        trainTapOnlyObservations = new ArrayList<>();
        points = new ArrayList<>();

        scrollSVMTextView = (TextView) findViewById(R.id.predictions);
        tapSVMTextView = (TextView) findViewById(R.id.predictions2);
        scrollKNNTextView = (TextView) findViewById(R.id.predictions3);
        tapKNNTextView = (TextView) findViewById(R.id.predictions4);
        scrollRTreeTextView = (TextView) findViewById(R.id.predictions5);
        tapRTreeTextView = (TextView) findViewById(R.id.predictions6);

        progressBarScrollSVM = (ProgressBar) findViewById(R.id.progressBar);
        progressBarTapSVM = (ProgressBar) findViewById(R.id.progressBar2);
        progressBarScrollKNN = (ProgressBar) findViewById(R.id.progressBar3);
        progressBarTapKNN = (ProgressBar) findViewById(R.id.progressBar4);
        progressBarScrollRTree = (ProgressBar) findViewById(R.id.progressBar5);
        progressBarTapRTree = (ProgressBar) findViewById(R.id.progressBar6);

        spinner = (Spinner)findViewById(R.id.spinner);
        populateSpinner();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                userID = userKeys[position];

                scrollFlingObservations = new ArrayList<>();
                trainScrollFlingObservations = new ArrayList<>();
                tapOnlyObservations = new ArrayList<>();
                trainTapOnlyObservations = new ArrayList<>();
                points = new ArrayList<>();

                scrollSVMTextView.setText("SVM Scroll/Fling");
                tapSVMTextView.setText("SVM Taps");

                scrollKNNTextView.setText("kNN Scroll/Fling");
                tapKNNTextView.setText("kNN Taps");

                scrollRTreeTextView.setText("rTree Scroll/Fling");
                tapRTreeTextView.setText("rTree Taps");

                /* Get user training data from Firebase */
                getTrainDataFromUsersFirebase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
    }

    private void populateSpinner()
    {
        Firebase userRef = new Firebase("https://fyp-max.firebaseio.com/users");

        userRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                userKeys = new String[(int)snapshot.getChildrenCount()];
                userNames = new String[(int)snapshot.getChildrenCount()];
                int i = 0;
                for (DataSnapshot usrSnapshot : snapshot.getChildren())
                {
                    User u = usrSnapshot.getValue(User.class);
                    userKeys[i] = u.getUserID();
                    userNames[i++] = u.getUserName();
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, userNames);
                spinner.setAdapter(adapter);

            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTrainDataFromUsersFirebase()
    {
        final Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println("No data available for this user. ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Train data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                } else
                {
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {
                        if (!usrSnapshot.getKey().equals(userID))
                        {
                            //System.out.println("usrSnapshot: " + usrSnapshot.child("scrollFling"));

                            int i = 0;
                            // Scroll/Fling:
                            DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                if(i < 10) trainScrollFlingObservations.add(obs);
                                i++;
                            }

                            i = 0;
                            // Taps:
                            DataSnapshot dpTap = usrSnapshot.child("tap");
                            for (DataSnapshot obsSnapshot : dpTap.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                //if(i < 10)
                                    trainTapOnlyObservations.add(obs);
                                i++;
                            }
                        }
                        else // Current user Data
                        {
                            DataSnapshot scrollSnapshot = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : scrollSnapshot.getChildren())
                            {
                                //System.out.println("data: " + obsSnapshot.toString());
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                trainScrollFlingObservations.add(obs);
                            }

                            // Tap Information
                            DataSnapshot tapSnapshot = usrSnapshot.child("tap");
                            for (DataSnapshot obsSnapshot : tapSnapshot.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                trainTapOnlyObservations.add(obs);
                            }
                        }
                    }

                    // Built the SVM model for Scroll/Fling Observations if training data exists.
                    if (trainScrollFlingObservations.size() > 0)
                    {
                        scrollFlingSVM = createAndTrainScrollFlingSVMClassifier(trainScrollFlingObservations);

                        scrollKNN = createAndTrainScrollFlingKNNClassifier(trainScrollFlingObservations);

                        scrollRTree = createAndTrainScrollFlingrTreeClassifier(trainScrollFlingObservations);

                    }else
                    {
                        System.out.println("No Scroll Fling data available. ");
                        // display a Toast letting the user know that there is no training data available.
                        Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    // Built the SVM model for Tap Observations if training data exists.
                    if (trainTapOnlyObservations.size() > 0)
                    {
                        tapSVM = createAndTrainTapSVMClassifier(trainTapOnlyObservations);

                        tapKNN = createAndTrainTapKNNClassifier(trainTapOnlyObservations);

                        tapRTree = createAndTraintapRTreeClassifier(trainTapOnlyObservations);
                    }else
                    {
                        System.out.println("No Tap data available. ");
                        // display a Toast letting the user know that there is no training data available.
                        Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided for Taps", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    /* Get test data from firebase and return predictions. */
                    getTestDataFromFirebaseAndTestSystem();

                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTestDataFromFirebaseAndTestSystem()
    {
        //get Scroll Fling Observations from Firebase
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/testData/" + userID);
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println("No data available for this User ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                } else
                {
                    // Scroll Fling Test Data
                    DataSnapshot dpScroll = snapshot.child("scrollFling");
                    for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                    {
                        //System.out.println("data: " + obsSnapshot.toString());
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        scrollFlingObservations.add(obs);
                    }

                    if(scrollFlingObservations.size() > 0)
                    {
                        //create train and test Martices
                        Mat testDataMat = buildTrainOrTestMatForScrollFling(scrollFlingObservations);
                        Mat resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);

                        // SVM
                        scrollFlingSVM.predict(testDataMat, resultMat, 0);
                        int counter = countOwnerResults(resultMat);
                        scrollSVMTextView.setText("SVM Scroll/Fling -> " + counter + " / " + scrollFlingObservations.size()
                                + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%");
                        progressBarScrollSVM.setMax(scrollFlingObservations.size());
                        progressBarScrollSVM.setProgress(counter);

                        System.out.println("Result Mat:");
                        displayMatrix(resultMat);
                        //System.out.println("TermCriteria: " + scrollFlingSVM.get);

                        // kNN
                        resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);
                        scrollKNN.predict(testDataMat, resultMat, 0);
                        counter = countOwnerResults(resultMat);
                        scrollKNNTextView.setText("kNN Scroll/Fling -> " + counter + " / " + scrollFlingObservations.size()
                                + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%");
                        progressBarScrollKNN.setMax(scrollFlingObservations.size());
                        progressBarScrollKNN.setProgress(counter);

                        // rTrees
                        resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);
                        scrollRTree.predict(testDataMat, resultMat, 0);
                        counter = countOwnerResults(resultMat);
                        scrollRTreeTextView.setText("rTrees Scroll/Fling -> " + counter + " / " + scrollFlingObservations.size()
                                + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%");
                        progressBarScrollRTree.setMax(scrollFlingObservations.size());
                        progressBarScrollRTree.setProgress(counter);

                    } else
                    {
                        System.out.println("No Scroll Fling data available. ");
                        Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided for Scroll/Fling", Toast.LENGTH_SHORT);
                        toast.show();
                    }


                    //Tap Test Data
                    DataSnapshot dpTap = snapshot.child("tap");
                    for (DataSnapshot obsSnapshot : dpTap.getChildren())
                    {
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        tapOnlyObservations.add(obs);
                    }

                    if(tapOnlyObservations.size() > 0)
                    {
                        Mat testTapDataMat = buildTrainOrTestMatForTaps(tapOnlyObservations);
                        Mat resultTapMat = new Mat(tapOnlyObservations.size(), 1, CvType.CV_32S);

                        // SVM
                        tapSVM.predict(testTapDataMat, resultTapMat, 0);
                        int counter = countOwnerResults(resultTapMat);
                        tapSVMTextView.setText("SVM Taps -> " + counter + " / " + tapOnlyObservations.size()
                                + " -> " + Math.round((counter * 100) / tapOnlyObservations.size()) + "%");
                        progressBarTapSVM.setMax(tapOnlyObservations.size());
                        progressBarTapSVM.setProgress(counter);

                        // kNN
                        tapKNN.predict(testTapDataMat, resultTapMat, 0);
                        counter = countOwnerResults(resultTapMat);
                        tapKNNTextView.setText("kNN Taps -> " + counter + " / " + tapOnlyObservations.size()
                                + " -> " + Math.round((counter * 100) / tapOnlyObservations.size()) + "%");
                        progressBarTapKNN.setMax(tapOnlyObservations.size());
                        progressBarTapKNN.setProgress(counter);

                        // rTrees
                        tapRTree.predict(testTapDataMat, resultTapMat, 0);
                        counter = countOwnerResults(resultTapMat);
                        tapRTreeTextView.setText("rTrees Taps -> " + counter + " / " + tapOnlyObservations.size()
                                + " -> " + Math.round((counter * 100) / tapOnlyObservations.size()) + "%");
                        progressBarTapRTree.setMax(tapOnlyObservations.size());
                        progressBarTapRTree.setProgress(counter);

                    }else
                    {
                        System.out.println("No Tap data available. ");
                        Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided for Taps", Toast.LENGTH_SHORT);
                        toast.show();
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

    private int countOwnerResults(Mat mat)
    {
        int counter = 0;
        for (int i = 0; i < mat.rows(); i++)
        {
            if (mat.get(i, 0)[0] == 1) counter++;
        }

        return counter;
    }

    private RTrees createAndTrainScrollFlingrTreeClassifier(ArrayList<Observation> arrayListObservations)
    {
        RTrees rTree = RTrees.create();
        Mat rTreeTrainMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat rTreeLabelsMat = buildLabelsMat(arrayListObservations);

        rTree.train(rTreeTrainMat, Ml.ROW_SAMPLE, rTreeLabelsMat);

        return rTree;
    }

    private RTrees createAndTraintapRTreeClassifier(ArrayList<Observation> arrayListObservations)
    {
        RTrees rTree = RTrees.create();
        Mat rTreeTrainMat = buildTrainOrTestMatForTaps(arrayListObservations);
        Mat rTreeLabelsMat = buildLabelsMat(arrayListObservations);

        rTree.train(rTreeTrainMat, Ml.ROW_SAMPLE, rTreeLabelsMat);

        return rTree;
    }

    private KNearest createAndTrainScrollFlingKNNClassifier(ArrayList<Observation> arrayListObservations)
    {
        KNearest kNN = KNearest.create();
        System.out.println("K is: " + kNN.getDefaultK());

        Mat kNNTrainMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat kNNLabelsMat = buildLabelsMat(arrayListObservations);

        kNN.train(kNNTrainMat, Ml.ROW_SAMPLE, kNNLabelsMat);

        return kNN;
    }

    private KNearest createAndTrainTapKNNClassifier(ArrayList<Observation> arrayListObservations)
    {
        KNearest kNN = KNearest.create();
        System.out.println("K is: " + kNN.getDefaultK());

        Mat kNNTrainMat = buildTrainOrTestMatForTaps(arrayListObservations);
        Mat kNNLabelsMat = buildLabelsMat(arrayListObservations);

        kNN.train(kNNTrainMat, Ml.ROW_SAMPLE, kNNLabelsMat);

        return kNN;
    }

    private SVM createAndTrainScrollFlingSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        SVM tempSVM = SVM.create();
        //initialise scrollFlingSVM
        tempSVM.setKernel(SVM.RBF);

        //tempSVM.setType(SVM.C_SVC);
        tempSVM.setType(SVM.NU_SVC);

        //tempSVM.setP(1);
        //tempSVM.setC(1/Math.pow(2,1));
        //tempSVM.setC(1/Math.pow(2,2));
        //tempSVM.setC(1/Math.pow(2,3));
        //tempSVM.setC(1/Math.pow(2,4));
        //tempSVM.setC(1/Math.pow(2,5));
        //tempSVM.setC(1/Math.pow(2,6));
        //tempSVM.setC(1/Math.pow(2,7));
        //tempSVM.setC(1/Math.pow(2,8));
        //tempSVM.setC(1/Math.pow(2,9));
        //tempSVM.setC(1/Math.pow(2,10));
        //tempSVM.setC(1/Math.pow(2,11));
        //tempSVM.setC(1/Math.pow(2,12));
        //tempSVM.setC(1/Math.pow(2,12.5));
        //tempSVM.setC(1/Math.pow(2,13));

        //tempSVM.setNu(0.99);
        //tempSVM.setNu(1/Math.pow(2,1.1));
        //tempSVM.setNu(1/Math.pow(2,1.5));
        //tempSVM.setNu(1/Math.pow(2,1.7));
        //tempSVM.setNu(1/Math.pow(2,1.9));
        //tempSVM.setNu(1/Math.pow(2,2));
        //tempSVM.setNu(1/Math.pow(2,5));
        //tempSVM.setNu(1/Math.pow(2,6));
        //tempSVM.setNu(1/Math.pow(2,7));
        //tempSVM.setNu(1/Math.pow(2,8));
        //tempSVM.setNu(1/Math.pow(2,9));
        //tempSVM.setNu(1/Math.pow(2,10));
        //tempSVM.setNu(1/Math.pow(2,11));
        //tempSVM.setNu(1/Math.pow(2,13));
        tempSVM.setNu(1/Math.pow(2,18.1));
        //tempSVM.setNu(1/Math.pow(2,18.5));
        //tempSVM.setNu(1/Math.pow(2,28));

        Mat trainScrollFlingMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat labelsScrollFlingMat = buildLabelsMat(arrayListObservations);

        //System.out.println("Train Matrix is:\n");
        //displayMatrix(trainScrollFlingMat);

        tempSVM.train(trainScrollFlingMat, Ml.ROW_SAMPLE, labelsScrollFlingMat);

        return tempSVM;
    }

    private SVM createAndTrainTapSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        SVM tempSVM = SVM.create();
        //initialise scrollFlingSVM
        tempSVM.setKernel(SVM.RBF);
        //tapSVM.setType(SVM.C_SVC);
        tempSVM.setType(SVM.NU_SVC);
        tempSVM.setC(1/Math.pow(2,13));
        tempSVM.setNu(1/Math.pow(2,11));

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
            tempMat.put(i, j++, scrollFlingObs.getMeanDirectionOfStroke());

            // Stop x
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().x);

            // Start x
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().x);

            // Stroke Duration
            tempMat.put(i, j++, scrollFlingObs.getScaledDuration());

            // Start y
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().y);

            // Stop y
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().y);
        }

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
            tempMat.put(i, j++, tapInteraction.getScaledDuration());

            // Start y
            tempMat.put(i, j++, tapInteraction.getScaledStartPoint().y);

            //Stop y
            tempMat.put(i, j++, tapInteraction.getScaledEndPoint().y);

        }

        return tempMat;
    }
}
