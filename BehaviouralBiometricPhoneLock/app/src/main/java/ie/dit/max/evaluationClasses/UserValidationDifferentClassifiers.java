package ie.dit.max.evaluationClasses;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import org.opencv.ml.RTrees;
import org.opencv.ml.KNearest;
import org.opencv.ml.SVM;

import java.util.ArrayList;

import ie.dit.max.behaviouralbiometricphonelock.Classifier;
import ie.dit.max.behaviouralbiometricphonelock.DBVar;
import ie.dit.max.behaviouralbiometricphonelock.Observation;
import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.User;

/**
 *  This activity has been used for Evaluation purposes and it has been left in the application for demonstration purpose.
 *
 *  In this activity, 2 spinners are populated with all users names. One spinner is used to select the name of the user
 *      for the train data and the other is used to select the name of the user for the test data.
 *
 *  Based on the information from spinners, train and test data are downloaded from the database and
 *      the system checks data with 3 different classifiers and display the predictions in progress bars.
 *
 *  Having this activity it was easy to observe the differences between classifiers.
 *
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 */
public class UserValidationDifferentClassifiers extends AppCompatActivity
{
    private static final String DEBUG_TAG = "Classifiers";

    private Spinner trainSpinner;
    private Spinner testSpinner;

    private ArrayList<Observation> trainScrollFlingObservations;
    private ArrayList<Observation> scrollFlingObservations;
    private ArrayList<Observation> tapOnlyObservations;
    private ArrayList<Observation> trainTapOnlyObservations;

    private String userID;
    private SVM scrollFlingSVM;
    private SVM tapSVM;
    private KNearest scrollKNN;
    private KNearest tapKNN;
    private RTrees scrollRTree;
    private RTrees tapRTree;

    private TextView scrollSVMTextView;
    private TextView tapSVMTextView;
    private TextView scrollKNNTextView;
    private TextView tapKNNTextView;
    private TextView scrollRTreeTextView;
    private TextView tapRTreeTextView;

    private ProgressBar progressBarScrollSVM;
    private ProgressBar progressBarTapSVM;
    private ProgressBar progressBarScrollKNN;
    private ProgressBar progressBarTapKNN;
    private ProgressBar progressBarScrollRTree;
    private ProgressBar progressBarTapRTree;

    private String[] userKeys;
    private String[] userNames;

    private SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_validation_different_classifiers);
        Firebase.setAndroidContext(this);

        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");

        scrollFlingObservations = new ArrayList<>();
        trainScrollFlingObservations = new ArrayList<>();
        tapOnlyObservations = new ArrayList<>();
        trainTapOnlyObservations = new ArrayList<>();

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

        Button goToAllUsersValidationScreen = (Button) findViewById(R.id.goToAllUsersValidationScreen);

        trainSpinner = (Spinner)findViewById(R.id.spinner);
        testSpinner = (Spinner)findViewById(R.id.spinner2);
        populateSpinners();

        trainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                userID = userKeys[position];
                testSpinner.setSelection(position);

                scrollFlingObservations = new ArrayList<>();
                trainScrollFlingObservations = new ArrayList<>();
                tapOnlyObservations = new ArrayList<>();
                trainTapOnlyObservations = new ArrayList<>();

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
                //do nothing
            }
        });

        testSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                String newUserID = userKeys[position];

                // if the same user is selected in the test spinner, Do nothing
                // if another user is selected, recalculate percentages
                if(!userID.equals(newUserID))
                {
                    scrollFlingObservations = new ArrayList<>();
                    tapOnlyObservations = new ArrayList<>();

                    scrollSVMTextView.setText("SVM Scroll/Fling");
                    tapSVMTextView.setText("SVM Taps");

                    scrollKNNTextView.setText("kNN Scroll/Fling");
                    tapKNNTextView.setText("kNN Taps");

                    scrollRTreeTextView.setText("rTree Scroll/Fling");
                    tapRTreeTextView.setText("rTree Taps");

                    /* Get user testing data from Firebase and compare it with other user train data*/
                    getTestDataFromFirebaseAndTestSystem(newUserID);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        goToAllUsersValidationScreen.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(UserValidationDifferentClassifiers.this, AllUsersValidation.class);

                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("ValidateDataForUserID", userKeys[trainSpinner.getSelectedItemPosition()]);
                editor.putString("ValidateDataForUserName", userNames[trainSpinner.getSelectedItemPosition()]);
                editor.apply();

                startActivity(intent);
            }
        });
    }

    private void populateSpinners()
    {
        Firebase userRef = new Firebase(DBVar.mainURL + "/users");

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
                    userNames[i++] = u.getUserName();//"User " + i ; //
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_item, userNames);
                trainSpinner.setAdapter(adapter);
                testSpinner.setAdapter(adapter);

            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTrainDataFromUsersFirebase()
    {
        final Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/trainData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "No Train data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else
                {
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {
                        if (!usrSnapshot.getKey().equals(userID))
                        {
                            int i = 0;
                            // Scroll/Fling
                            DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                if (i < 5) trainScrollFlingObservations.add(obs);
                                i++;
                            }

                            i = 0;
                            // Taps
                            DataSnapshot dpTap = usrSnapshot.child("tap");
                            for (DataSnapshot obsSnapshot : dpTap.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                trainTapOnlyObservations.add(obs);
                                i++;
                            }
                        }
                        else // Current user Data
                        {
                            DataSnapshot scrollSnapshot = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : scrollSnapshot.getChildren())
                            {
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

                    // Built the classifiers models for Scroll/Fling Observations if training data exists.
                    if (trainScrollFlingObservations.size() > 0)
                    {
                        scrollFlingSVM = Classifier.createAndTrainScrollFlingSVMClassifier(trainScrollFlingObservations);

                        scrollKNN = Classifier.createAndTrainScrollFlingKNNClassifier(trainScrollFlingObservations);

                        scrollRTree = Classifier.createAndTrainScrollFlingRTreeClassifier(trainScrollFlingObservations);
                    }
                    else
                    {
                        // display a Toast letting the user know that there is no training data available.
                        Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    // Built the classifiers models for Tap Observations if training data exists.
                    if (trainTapOnlyObservations.size() > 0)
                    {
                        tapSVM = Classifier.createAndTrainTapSVMClassifier(trainTapOnlyObservations);

                        tapKNN = Classifier.createAndTrainTapKNNClassifier(trainTapOnlyObservations);

                        tapRTree = Classifier.createAndTraintapRTreeClassifier(trainTapOnlyObservations);
                    }
                    else
                    {
                        // display a Toast letting the user know that there is no training data available.
                        Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided for Taps", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    /* Get test data from firebase and return predictions. */
                    getTestDataFromFirebaseAndTestSystem(userID);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTestDataFromFirebaseAndTestSystem(String userID)
    {
        //get Scroll Fling Observations from Firebase
        Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/testData/" + userID);
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else
                {
                    // Scroll Fling Test Data
                    DataSnapshot dpScroll = snapshot.child("scrollFling");
                    for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                    {
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        scrollFlingObservations.add(obs);
                    }

                    if (scrollFlingObservations.size() > 0)
                    {
                        //create train and test Martices
                        Mat testDataMat = Classifier.buildTrainOrTestMatForScrollFling(scrollFlingObservations);
                        Mat resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);

                        // SVM
                        scrollFlingSVM.predict(testDataMat, resultMat, 0);
                        int counter = Classifier.countOwnerResults(resultMat);
                        scrollSVMTextView.setText("SVM Scroll/Fling -> " + counter + " / " + scrollFlingObservations.size()
                                + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%");
                        progressBarScrollSVM.setMax(scrollFlingObservations.size());
                        progressBarScrollSVM.setProgress(counter);

                        // kNN
                        resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);
                        scrollKNN.predict(testDataMat, resultMat, 0);
                        counter = Classifier.countOwnerResults(resultMat);
                        scrollKNNTextView.setText("kNN Scroll/Fling -> " + counter + " / " + scrollFlingObservations.size()
                                + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%");
                        progressBarScrollKNN.setMax(scrollFlingObservations.size());
                        progressBarScrollKNN.setProgress(counter);

                        // rTrees
                        resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);
                        scrollRTree.predict(testDataMat, resultMat, 0);
                        counter = Classifier.countOwnerResults(resultMat);
                        scrollRTreeTextView.setText("rTrees Scroll/Fling -> " + counter + " / " + scrollFlingObservations.size()
                                + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%");
                        progressBarScrollRTree.setMax(scrollFlingObservations.size());
                        progressBarScrollRTree.setProgress(counter);

                    }
                    else
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

                    if (tapOnlyObservations.size() > 0)
                    {
                        Mat testTapDataMat = Classifier.buildTrainOrTestMatForTaps(tapOnlyObservations);
                        Mat resultTapMat = new Mat(tapOnlyObservations.size(), 1, CvType.CV_32S);

                        // SVM
                        tapSVM.predict(testTapDataMat, resultTapMat, 0);
                        int counter = Classifier.countOwnerResults(resultTapMat);
                        tapSVMTextView.setText("SVM Taps -> " + counter + " / " + tapOnlyObservations.size()
                                + " -> " + Math.round((counter * 100) / tapOnlyObservations.size()) + "%");
                        progressBarTapSVM.setMax(tapOnlyObservations.size());
                        progressBarTapSVM.setProgress(counter);

                        // kNN
                        tapKNN.predict(testTapDataMat, resultTapMat, 0);
                        counter = Classifier.countOwnerResults(resultTapMat);
                        tapKNNTextView.setText("kNN Taps -> " + counter + " / " + tapOnlyObservations.size()
                                + " -> " + Math.round((counter * 100) / tapOnlyObservations.size()) + "%");
                        progressBarTapKNN.setMax(tapOnlyObservations.size());
                        progressBarTapKNN.setProgress(counter);

                        // rTrees
                        tapRTree.predict(testTapDataMat, resultTapMat, 0);
                        counter = Classifier.countOwnerResults(resultTapMat);
                        tapRTreeTextView.setText("rTrees Taps -> " + counter + " / " + tapOnlyObservations.size()
                                + " -> " + Math.round((counter * 100) / tapOnlyObservations.size()) + "%");
                        progressBarTapRTree.setMax(tapOnlyObservations.size());
                        progressBarTapRTree.setProgress(counter);

                    }
                    else
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
                System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
            }
        });
    }
}
