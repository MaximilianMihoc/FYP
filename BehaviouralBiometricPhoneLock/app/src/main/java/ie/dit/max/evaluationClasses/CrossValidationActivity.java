
package ie.dit.max.evaluationClasses;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.util.ArrayList;

import ie.dit.max.behaviouralbiometricphonelock.Classifier;
import ie.dit.max.behaviouralbiometricphonelock.Observation;
import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.User;

/**
 *  This activity has been used for Evaluation purposes and it was created before the UserValidationDifferentClassifiers activity.
 *  This activity is not used anywhere at the moment.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 */
public class CrossValidationActivity extends Activity
{
    private Spinner spinner;
    private ArrayList<Observation> trainScrollFlingObservations;
    private ArrayList<Observation> scrollFlingObservations;
    private ArrayList<Observation> tapOnlyObservations;
    private ArrayList<Observation> trainTapOnlyObservations;
    private String userID;
    private SVM scrollFlingSVM;
    private SVM tapSVM;
    private TextView outputData;
    private String out = "";
    private String[] userKeys;
    private String[] userNames;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Firebase.setAndroidContext(this);

        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");

        scrollFlingObservations = new ArrayList<>();
        trainScrollFlingObservations = new ArrayList<>();
        tapOnlyObservations = new ArrayList<>();
        trainTapOnlyObservations = new ArrayList<>();

        outputData = (TextView) findViewById(R.id.predictions);

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

                out = "";
                outputData.setText("Waiting for Data");

                /* Get user training data from Firebase */
                getTrainDataFromOtherUsersFirebase();
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
                    userNames[i++] = "User " + i; //u.getUserName();
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

    private void getTrainingDataFromFirebase()
    {
        //get Scroll Fling Observations from Firebase
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID);
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                //System.out.println("data: " + snapshot.toString());
                //System.out.println("There are " + snapshot.getChildrenCount() + " observations");

                DataSnapshot scrollSnapshot = snapshot.child("scrollFling");
                for (DataSnapshot obsSnapshot : scrollSnapshot.getChildren())
                {
                    //System.out.println("data: " + obsSnapshot.toString());
                    Observation obs = obsSnapshot.getValue(Observation.class);
                    trainScrollFlingObservations.add(obs);

                    //just some tests
                    /*Touch t = obs.getTouch();
                    ScrollFling sf = new ScrollFling(t);
                    System.out.println(sf.toString());*/
                }

                // Built the SVM model for Scroll/Fling Observations if training data exists.
                if (trainScrollFlingObservations.size() > 0)
                {
                    //initialise scrollFlingSVM
                    scrollFlingSVM = SVM.create();
                    scrollFlingSVM.setKernel(SVM.RBF);

                    //scrollFlingSVM.setType(SVM.C_SVC);
                    scrollFlingSVM.setType(SVM.NU_SVC);

                    //scrollFlingSVM.setC(2.55);
                    //scrollFlingSVM.setC(1/Math.pow(2,1));
                    //scrollFlingSVM.setC(1/Math.pow(2,2));
                    //scrollFlingSVM.setC(1/Math.pow(2,3));
                    //scrollFlingSVM.setC(1/Math.pow(2,4));
                    //scrollFlingSVM.setC(1/Math.pow(2,5));
                    //scrollFlingSVM.setC(1/Math.pow(2,6));
                    //scrollFlingSVM.setC(1/Math.pow(2,7));
                    //scrollFlingSVM.setC(1/Math.pow(2,8));
                    //scrollFlingSVM.setC(1/Math.pow(2,9));
                    //scrollFlingSVM.setC(1/Math.pow(2,10));
                    //scrollFlingSVM.setC(1/Math.pow(2,11));
                    scrollFlingSVM.setC(1/Math.pow(2,12));
                    //scrollFlingSVM.setC(1/Math.pow(2,12.5));
                    //scrollFlingSVM.setC(1/Math.pow(2,13));

                    //scrollFlingSVM.setP(1);
                    //scrollFlingSVM.setGamma(0.001953125);

                    //scrollFlingSVM.setNu(1/Math.pow(2,2));
                    //scrollFlingSVM.setNu(1/Math.pow(2,5));
                    //scrollFlingSVM.setNu(1/Math.pow(2,6));
                    //scrollFlingSVM.setNu(1/Math.pow(2,7));
                    //scrollFlingSVM.setNu(1/Math.pow(2,8));
                    //scrollFlingSVM.setNu(1/Math.pow(2,9));
                    //scrollFlingSVM.setNu(1/Math.pow(2,10));
                    //scrollFlingSVM.setNu(1/Math.pow(2,11));
                    scrollFlingSVM.setNu(1/Math.pow(2,13));

                    Mat trainScrollFlingMat = Classifier.buildTrainOrTestMatForScrollFling(trainScrollFlingObservations);
                    Mat labelsScrollFlingMat = Classifier.buildLabelsMat(trainScrollFlingObservations);

                    System.out.println("Train Matrix is:\n");
                    Classifier.displayMatrix(trainScrollFlingMat);

                    scrollFlingSVM.train(trainScrollFlingMat, Ml.ROW_SAMPLE, labelsScrollFlingMat);
                    // end training scrollFlingSNM
                }else
                {
                    System.out.println("No Scroll Fling data available. ");
                    // display a Toast letting the user know that there is no training data available.
                    Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }

                // Tap Information
                DataSnapshot tapSnapshot = snapshot.child("tap");
                for (DataSnapshot obsSnapshot : tapSnapshot.getChildren())
                {
                    Observation obs = obsSnapshot.getValue(Observation.class);
                    trainTapOnlyObservations.add(obs);
                }

                if (trainTapOnlyObservations.size() > 0)
                {
                    //initialise scrollFlingSVM
                    tapSVM = SVM.create();
                    tapSVM.setKernel(SVM.RBF);

                    //tapSVM.setType(SVM.C_SVC);
                    tapSVM.setType(SVM.NU_SVC);

                    //tapSVM.setC(1/Math.pow(2,1));
                    //tapSVM.setC(1/Math.pow(2,2));
                    //tapSVM.setC(1/Math.pow(2,3));
                    //tapSVM.setC(1/Math.pow(2,4));
                    //tapSVM.setC(1/Math.pow(2,5));
                    //tapSVM.setC(1/Math.pow(2,6));
                    //tapSVM.setC(1/Math.pow(2,7));
                    //tapSVM.setC(1/Math.pow(2,8));
                    //tapSVM.setC(1/Math.pow(2,9));
                    //tapSVM.setC(1/Math.pow(2,10));
                    //tapSVM.setC(1/Math.pow(2,11));
                    //tapSVM.setC(1/Math.pow(2,12));
                    //tapSVM.setC(1/Math.pow(2,12.5));
                    tapSVM.setC(1/Math.pow(2,13));

                    //tapSVM.setP(1);
                    //tapSVM.setGamma(0.001953125);

                    //tapSVM.setNu(1/Math.pow(2,5));
                    //tapSVM.setNu(1/Math.pow(2,6));
                    //tapSVM.setNu(1/Math.pow(2,7));
                    //tapSVM.setNu(1/Math.pow(2,8));
                    //tapSVM.setNu(1/Math.pow(2,9));
                    //tapSVM.setNu(1/Math.pow(2,10));
                    tapSVM.setNu(1/Math.pow(2,11));

                    Mat trainTapMat = Classifier.buildTrainOrTestMatForTaps(trainTapOnlyObservations);
                    Mat labelsTapMat = Classifier.buildLabelsMat(trainTapOnlyObservations);

                    //System.out.println("Train Matrix for Tap is:\n");
                    //displayMatrix(trainTapMat);

                    tapSVM.train(trainTapMat, Ml.ROW_SAMPLE, labelsTapMat);
                    // end training TapSVM
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

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTrainDataFromOtherUsersFirebase()
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
                                //if(i < 20)
                                    trainTapOnlyObservations.add(obs);
                                i++;
                            }
                        }
                    }

                    // train data for the user.
                    getTrainingDataFromFirebase();

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
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/testData/" + userID + "/scrollFling");
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
                    for (DataSnapshot obsSnapshot : snapshot.getChildren())
                    {
                        //System.out.println("data: " + obsSnapshot.toString());
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        scrollFlingObservations.add(obs);
                    }

                    Mat testDataMat = Classifier.buildTrainOrTestMatForScrollFling(scrollFlingObservations);

                    // create the result Mat
                    Mat resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);

                    scrollFlingSVM.predict(testDataMat, resultMat, 0);

                    int counter = 0;
                    out += "Scroll/Fling:\n";
                    for (int i = 0; i < resultMat.rows(); i++)
                    {
                        out += "\tpredicted" + i + ": " + resultMat.get(i, 0)[0];
                        if (resultMat.get(i, 0)[0] == 1) counter++;
                    }

                    out += "\nOwners: " + counter + " out of " + scrollFlingObservations.size();
                    outputData.setText(out);

                    //System.out.println("Scroll Fling Result Mat: ");
                    //displayMatrix(resultMat);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });

        //get Tap Observations from Firebase
        Firebase tapRef = new Firebase("https://fyp-max.firebaseio.com/testData/" + userID + "/tap");
        tapRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println("No Tap data available. ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided for Taps", Toast.LENGTH_SHORT);
                    toast.show();
                } else
                {
                    for (DataSnapshot obsSnapshot : snapshot.getChildren())
                    {
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        tapOnlyObservations.add(obs);
                    }

                    Mat testDataMat = Classifier.buildTrainOrTestMatForTaps(tapOnlyObservations);

                    // create the result Mat
                    Mat resultMat = new Mat(tapOnlyObservations.size(), 1, CvType.CV_32S);
                    tapSVM.predict(testDataMat, resultMat, 0);

                    int counter = 0;
                    out += "\n\nTaps:\n";
                    for (int i = 0; i < resultMat.rows(); i++)
                    {
                        out += "\tpredicted" + i + ": " + resultMat.get(i, 0)[0];
                        if (resultMat.get(i, 0)[0] == 1) counter++;
                    }

                    out += "\nOwners: " + counter + " out of " + tapOnlyObservations.size();
                    outputData.setText(out);

                    //System.out.println("Tap Result Mat: ");
                    //displayMatrix(resultMat);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

}
