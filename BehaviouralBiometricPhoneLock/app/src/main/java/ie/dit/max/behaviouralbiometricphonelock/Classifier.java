package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ml.KNearest;
import org.opencv.ml.Ml;
import org.opencv.ml.RTrees;
import org.opencv.ml.SVM;

import java.util.ArrayList;

/**
 * This class contains the classifiers initialisations and common methods used by each classifier
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 04th April 2016
 */
public class Classifier
{
    /**
     * Method countOwnerResults()
     * This method is used to count the results returned by the classifier in the Result Mat
     * 1 representing Owner data
     * 0 representing Guest data
     *
     * @param mat ResultMat
     * @return int
     */
    public static int countOwnerResults(Mat mat)
    {
        int counter = 0;
        for (int i = 0; i < mat.rows(); i++)
        {
            if (mat.get(i, 0)[0] == 1) counter++;
        }

        return counter;
    }

    /**
     * Method changeJudgements
     * This method is used to change the judgements of observations
     *
     * @param obsList ArrayList of Observations
     * @param judgementValue value of the new judgement for all observations
     * @return ArrayList
     */
    public static ArrayList<Observation> changeJudgements(ArrayList<Observation> obsList, int judgementValue)
    {
        for(Observation obs : obsList)
        {
            obs.setJudgement(judgementValue);
        }

        return obsList;
    }

    /**
     * Method createAndTrainScrollFlingSVMClassifier
     * This method is used to create and train the SVM classifier for Scroll/Fling actions
     * given an array of Observations for the train data.
     *
     * @param arrayListObservations ArrayList of Observations
     * @return SVM
     */
    public static SVM createAndTrainScrollFlingSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        //initialise scrollFlingSVM
        SVM tempSVM = SVM.create();
        tempSVM.setKernel(SVM.CHI2);

        tempSVM.setType(SVM.C_SVC);
        tempSVM.setC(10.55);
        tempSVM.setGamma(0.15);

        Mat trainScrollFlingMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat labelsScrollFlingMat = buildLabelsMat(arrayListObservations);

        tempSVM.train(trainScrollFlingMat, Ml.ROW_SAMPLE, labelsScrollFlingMat);

        return tempSVM;
    }

    /**
     * Method createAndTrainTapSVMClassifier
     * This method is used to create and train the SVM classifier for Tap actions
     * given an array of Observations for the train data.
     *
     * @param arrayListObservations ArrayList of Observations
     * @return SVM
     */
    public static SVM createAndTrainTapSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        SVM tempSVM = SVM.create();

        tempSVM.setKernel(SVM.CHI2);
        tempSVM.setType(SVM.C_SVC);
        tempSVM.setC(10.55);
        tempSVM.setNu(0.15);

        Mat trainTapMat = buildTrainOrTestMatForTaps(arrayListObservations);
        Mat labelsTapMat = buildLabelsMat(arrayListObservations);

        tempSVM.train(trainTapMat, Ml.ROW_SAMPLE, labelsTapMat);

        return tempSVM;
    }

    /**
     * Method createAndTrainScrollFlingRTreeClassifier
     * This method is used to create and train the RTrees classifier for Scroll/Fling actions
     * given an array of Observations for the train data.
     *
     * @param arrayListObservations ArrayList of Observations
     * @return RTrees
     */
    public static RTrees createAndTrainScrollFlingRTreeClassifier(ArrayList<Observation> arrayListObservations)
    {
        RTrees rTree = RTrees.create();
        Mat rTreeTrainMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat rTreeLabelsMat = buildLabelsMat(arrayListObservations);

        rTree.train(rTreeTrainMat, Ml.ROW_SAMPLE, rTreeLabelsMat);

        return rTree;
    }

    /**
     * Method createAndTraintapRTreeClassifier
     * This method is used to create and train the RTrees classifier for Tap actions
     * given an array of Observations for the train data.
     *
     * @param arrayListObservations ArrayList of Observations
     * @return RTrees
     */
    public static RTrees createAndTraintapRTreeClassifier(ArrayList<Observation> arrayListObservations)
    {
        RTrees rTree = RTrees.create();
        Mat rTreeTrainMat = buildTrainOrTestMatForTaps(arrayListObservations);
        Mat rTreeLabelsMat = buildLabelsMat(arrayListObservations);

        rTree.train(rTreeTrainMat, Ml.ROW_SAMPLE, rTreeLabelsMat);

        return rTree;
    }

    /**
     * Method createAndTrainScrollFlingKNNClassifier
     * This method is used to create and train the KNearest classifier for Scroll/Fling actions
     * given an array of Observations for the train data.
     *
     * @param arrayListObservations ArrayList of Observations
     * @return KNearest
     */
    public static KNearest createAndTrainScrollFlingKNNClassifier(ArrayList<Observation> arrayListObservations)
    {
        KNearest kNN = KNearest.create();

        Mat kNNTrainMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat kNNLabelsMat = buildLabelsMat(arrayListObservations);

        kNN.train(kNNTrainMat, Ml.ROW_SAMPLE, kNNLabelsMat);

        return kNN;
    }

    /**
     * Method createAndTrainTapKNNClassifier
     * This method is used to create and train the KNearest classifier for Tap actions
     * given an array of Observations for the train data.
     *
     * @param arrayListObservations ArrayList of Observations
     * @return KNearest
     */
    public static KNearest createAndTrainTapKNNClassifier(ArrayList<Observation> arrayListObservations)
    {
        KNearest kNN = KNearest.create();

        Mat kNNTrainMat = buildTrainOrTestMatForTaps(arrayListObservations);
        Mat kNNLabelsMat = buildLabelsMat(arrayListObservations);

        kNN.train(kNNTrainMat, Ml.ROW_SAMPLE, kNNLabelsMat);

        return kNN;
    }

    /**
     * Method buildLabelsMat
     * This method is used to build the Labels Matrix for a classifier
     * Given a list of Observations, the method returns the judgements of each observation
     * The returned Mat contains only one column and many rows. Each row contain the judgement of one Observation     *
     *
     * @param listObservations ArrayList of Observations
     * @return Mat
     */
    public static Mat buildLabelsMat(ArrayList<Observation> listObservations)
    {
        Mat labelsTempMat = new Mat(listObservations.size(), 1, CvType.CV_32S);

        for(int i = 0; i < listObservations.size(); i++)
        {
            labelsTempMat.put(i, 0, listObservations.get(i).getJudgement());
        }

        return labelsTempMat;
    }

    /**
     * Method buildTrainOrTestMatForScrollFling
     * Given a list of observations, this method returns the feature matrix used by classifiers to train or test the Scroll/Fling model
     * Each row of the matrix contains all the features of one Scroll/Fling observation
     *
     * @param listObservations ArrayList of Observations
     * @return Mat
     */
    public static Mat buildTrainOrTestMatForScrollFling(ArrayList<Observation> listObservations)
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
        return tempMat;

    }

    /**
     * Method buildTrainOrTestMatForTaps
     * Given a list of observations, this method returns the feature matrix used by classifiers to train or test the Tap model
     * Each row of the matrix contains all the features of one Tap observation
     *
     * @param listObservations ArrayList of Observations
     * @return Mat
     */
    public static Mat buildTrainOrTestMatForTaps(ArrayList<Observation> listObservations)
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

    /**
     * Method displayMatrix
     * This method is used to display the content of ant matrix on console
     *
     * @param matrix Mat
     */
    public static void displayMatrix(Mat matrix)
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

    /**
     * Method normalizeMat
     * This method was used to normalise feature values of a matrix between 0 and 1
     * Not used anymore as normalisation on each feature was better for predictions
     *
     * @param toNormalize Mat to normalize
     * @return Mat
     */
    public static Mat normalizeMat(Mat toNormalize)
    {
        Mat tempMat = toNormalize.clone();

        for(int col = 0; col < toNormalize.cols(); col++)
        {
            double min = getMinValueOFColumn(toNormalize, col);
            double max = getMaxValueOFColumn(toNormalize, col);

            for (int row = 0; row < toNormalize.rows(); row++)
            {
                double[] element = toNormalize.get(row, col);
                tempMat.put(row, col, (element[0] - min) / (max - min));
            }
        }

        return tempMat;
    }

    /**
     * Method getMinValueOFColumn
     * This method was used to get the minimum value of a column from a matrix
     * Used for normalization
     *
     * @param mat Mat to use
     * @param col column number
     * @return double
     */
    private static double getMinValueOFColumn(Mat mat, int col)
    {
        double min = Double.MAX_VALUE;
        for(int i = 0; i < mat.rows(); i++)
        {
            double [] temp = mat.get(i,col);
            if(temp[0] < min ) min = temp[0];
        }

        return min;
    }

    /**
     * Method getMinValueOFColumn
     * This method was used to get the maximum value of a column from a matrix
     * Used for normalization
     *
     * @param mat Mat to use
     * @param col column number
     * @return double
     */
    private static double getMaxValueOFColumn(Mat mat, int col)
    {
        double max = Double.MIN_VALUE;
        for(int i = 0; i < mat.rows(); i++)
        {
            double [] temp = mat.get(i,col);
            if(temp[0] > max ) max = temp[0];
        }

        return max;
    }
}
