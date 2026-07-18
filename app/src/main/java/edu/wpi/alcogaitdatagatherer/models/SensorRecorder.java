package edu.wpi.alcogaitdatagatherer.models;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import edu.wpi.alcogaitdatagatherer.tasks.SaveWalkHolderToCSVTask;
import edu.wpi.alcogaitdatagatherer.ui.activities.DataGatheringActivity;
import edu.wpi.alcogaitdatagatherercommon.CommonCode;
import edu.wpi.alcogaitdatagatherercommon.WalkType;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;

public class SensorRecorder extends ChannelClient.ChannelCallback implements SensorEventListener {

    public interface SensorRecorderListener {
        void onWalkNumberUpdate(String info);
        void onWalkLogUpdate(String log);
        void onRecordingStatusChanged(boolean isRecording);
        void onStartButtonTextUpdate(String text);
    }

    private TestSubject testSubject;
    private Walk walk;

    private SensorRecorderListener listener;
    private DataGatheringActivity activity;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mMagnetometer;

    private float[] accelVal;
    private float[] gyroVal;
    private float[] magVal;
    private LinkedList<Walk> logQueue;

    private String rootFolderName;
    private String walkFolderName;
    private boolean isRecording;
    private int currentWalkNumber;
    private WalkType currentWalkType;
    private String TAG = "SensorRecorder";
    private static final float ALPHA = 0.15f;

    public SensorRecorder(DataGatheringActivity gatheringActivity, String rootFolderName, TestSubject testSubject, SensorRecorderListener listener) {
        this.testSubject = testSubject;
        this.activity = gatheringActivity;
        this.mSensorManager = (SensorManager) gatheringActivity.getSystemService(SENSOR_SERVICE);
        this.mAccelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        this.mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);
        this.mMagnetometer = mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);
        this.rootFolderName = rootFolderName;
        this.listener = listener;
        isRecording = false;

        logQueue = new LinkedList<Walk>();
        currentWalkNumber = 1;
        currentWalkType = WalkType.NORMAL;
        updateWalkNumberDisplay();
        testSubject.setCurrentWalkHolder(new WalkHolder(currentWalkNumber));
        testSubject.setWalkTypeAmount(gatheringActivity);
        prepareReportFile();
    }

    public void registerListeners() {
        mSensorManager.registerListener(this, mAccelerometer, CommonCode.DELAY_IN_MILLISECONDS * 1000);
        mSensorManager.registerListener(this, mGyroscope, CommonCode.DELAY_IN_MILLISECONDS * 1000);
        mSensorManager.registerListener(this, mMagnetometer, CommonCode.DELAY_IN_MILLISECONDS * 1000);
    }

    public void unregisterListeners() {
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        if (isRecording) {
            String sensorName = sensorEvent.sensor.getName();

            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelVal = lowPass(sensorEvent.values.clone(), accelVal);
                walk.addPhoneAccelerometerData(CommonCode.generatePrintableSensorData(sensorName, accelVal, sensorEvent.accuracy, sensorEvent.timestamp));
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                gyroVal = lowPass(sensorEvent.values.clone(), gyroVal);
                walk.addPhoneGyroscopeData(CommonCode.generatePrintableSensorData(sensorName, gyroVal, sensorEvent.accuracy, sensorEvent.timestamp));
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magVal = lowPass(sensorEvent.values.clone(), magVal);
            }
            if (accelVal != null && magVal != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, accelVal, magVal);
                if (success) {
                    float compassVal[] = new float[3];
                    SensorManager.getOrientation(R, compassVal);
                    walk.addCompassData(CommonCode.generatePrintableSensorData("Compass", compassVal, sensorEvent.accuracy, sensorEvent.timestamp));
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void startRecording(Double BAC) {
        isRecording = true;
        if (listener != null) listener.onRecordingStatusChanged(true);
        registerListeners();
        if (currentWalkType != null) {
            walk = new Walk(testSubject.getCurrentWalkHolder().getWalkNumber(), BAC, currentWalkType);
        }
    }

    public boolean stopRecording() {
        isRecording = false;
        if (listener != null) listener.onRecordingStatusChanged(false);
        unregisterListeners();

        testSubject.setCurrentWalkHolder(testSubject.getCurrentWalkHolder().addWalk(walk));

        currentWalkType = testSubject.getCurrentWalkHolder().getNextWalkType();
        updateWalkLogDisplay(true);
        if (currentWalkType != null) {
            updateWalkNumberDisplay();
            return true;
        } else {
            if (listener != null) listener.onStartButtonTextUpdate("SAVE WALK #" + String.valueOf(currentWalkNumber));
            return false;
        }
    }

    public void prepareWalkStorage() {
        walkFolderName = rootFolderName + File.separator + "walk_" + String.valueOf(testSubject.getCurrentWalkHolder().getWalkNumber());
        File f = new File(walkFolderName);
        f.mkdirs();
    }

    public void saveCurrentWalkNumberToCSV(EditText bacInput) {
        new SaveWalkHolderToCSVTask(this, walkFolderName, bacInput).execute();
    }

    private void updateWalkNumberDisplay() {
        if (listener != null) {
            listener.onWalkNumberUpdate("Walk Number " + (currentWalkNumber) + " : " + currentWalkType.toString());
        }
    }

    public void restartCurrentWalkNumber(final Context context, final Runnable onRestartConfirmed) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    restartWalkHolder();
                    if (onRestartConfirmed != null) onRestartConfirmed.run();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Restart");
        builder.setMessage("Do you want to remove all walks for the current walk number? (Walk Number " + testSubject.getCurrentWalkHolder().getWalkNumber() + ") (" + testSubject.getCurrentWalkHolder().getSampleSize() + " samples recorded)").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void restartWalkHolder() {
        isRecording = false;
        testSubject.replaceWalkHolder(new WalkHolder(currentWalkNumber));
        currentWalkNumber = testSubject.getCurrentWalkHolder().getWalkNumber();
        currentWalkType = testSubject.getCurrentWalkHolder().getNextWalkType();
        updateWalkNumberDisplay();
        clearWalkLog();
        testSubject.setWalkTypeAmount(activity);
        walk = testSubject.getCurrentWalkHolder().get(currentWalkType);
    }

    public void reDoWalk(final Context context, final Runnable onRedoConfirmed) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    testSubject.setCurrentWalkHolder(testSubject.getCurrentWalkHolder().removeWalk(walk.getWalkType()));
                    if (!testSubject.getCurrentWalkHolder().hasWalk(WalkType.NORMAL)) {
                        testSubject.setWalkTypeAmount(activity);
                    }
                    currentWalkType = walk.getWalkType();
                    updateWalkNumberDisplay();
                    logQueue.removeLast();
                    updateWalkLogDisplay(false);
                    if (currentWalkType != WalkType.NORMAL) {
                        walk = testSubject.getCurrentWalkHolder().get(testSubject.getCurrentWalkHolder().getPreviousWalkType(currentWalkType));
                    } else {
                        walk = null;
                    }
                    if (listener != null) {
                        listener.onStartButtonTextUpdate("START WALK");
                    }
                    if (onRedoConfirmed != null) {
                        onRedoConfirmed.run();
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Re-Do Walk");
        builder.setMessage("Do you want re-do the previous walk? (Walk Number " + testSubject.getCurrentWalkHolder().getWalkNumber() +
                " : " + testSubject.getCurrentWalkHolder().getPreviousWalkType(currentWalkType) + ")").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }


    void updateWalkLogDisplay(boolean addNewWalk) {
        int MAX_LOGS = 5;

        if (addNewWalk) {
            if (logQueue.size() >= MAX_LOGS) {
                logQueue.removeFirst();
            }

            logQueue.add(walk);
        }

        StringBuilder walkLog = new StringBuilder("Last " + MAX_LOGS + " Walks:");
        for (int i = logQueue.size() - 1; i >= 0; i--) {
            Walk aWalk = logQueue.get(i);
            String walkTypeString;
            if (aWalk.getWalkType() == WalkType.STANDING_ON_ONE_FOOT) {
                walkTypeString = "Stand 1 Foot";
            } else {
                walkTypeString = aWalk.getWalkType().toString();
            }
            walkLog.append("\nWalk Number ").append(aWalk.getWalkNumber()).append(" : BAC =").append(aWalk.getBAC()).append(", ").append(walkTypeString);
        }

        if (listener != null) listener.onWalkLogUpdate(walkLog.toString());
    }

    void clearWalkLog() {
        logQueue.clear();
        if (listener != null) listener.onWalkLogUpdate("");
    }

    public boolean isRecording() {
        return isRecording;
    }

    public TestSubject getTestSubject() {
        return testSubject;
    }

    public void setTestSubject(TestSubject testSubject) {
        this.testSubject = testSubject;
    }

    private float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public void incrementWalkNumber() {
        currentWalkNumber++;
        testSubject.addNewWalkHolder(new WalkHolder(currentWalkNumber));
        currentWalkType = testSubject.getCurrentWalkHolder().getNextWalkType();
        updateWalkNumberDisplay();
        testSubject.setWalkTypeAmount(activity);
        if (listener != null) listener.onStartButtonTextUpdate("START WALK");
    }

    public void prepareReportFile() {
        final File file = new File(rootFolderName, "report.txt");

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file, false);
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);

            bufferWriter.append(testSubject.printInfo());

            bufferWriter.close();

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void saveWalkReport() {
        final File file = new File(rootFolderName, "report.txt");

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);

            bufferWriter.append("\n\nReported Walk Numbers:\n");
            boolean hasReportedWalks = false;
            for (int i = 0; i < testSubject.getBooleanWalksList().size(); i++) {
                if (testSubject.getBooleanWalksList().get(i)) {
                    bufferWriter.append(String.valueOf(i + 1));
                    if (i != testSubject.getBooleanWalksList().size() - 1 && testSubject.getBooleanWalksList().size() > 1) {
                        bufferWriter.append(", ");
                    }
                    hasReportedWalks = true;
                }
            }
            if (!hasReportedWalks) {
                bufferWriter.append("None");
            }

            bufferWriter.append("\n\nReport Message:\n");
            bufferWriter.append(testSubject.getReportMessage() + "\n");

            bufferWriter.close();

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public WalkType getCurrentWalkType() {
        return currentWalkType;
    }

    @Override
    public void onChannelOpened(@NonNull ChannelClient.Channel channel) {
        if (channel.getPath().equals(CommonCode.WEAR_CSV_FILE_CHANNEL_PATH)) {
            activity.startProgressBar();
            activity.updateProgressBarMessage("Receiving Data From Watch");

            File file = new File(walkFolderName + File.separator + "watch.csv");

            try {
                file.createNewFile();
            } catch (IOException e) {
                //handle error
            }
            Wearable.getChannelClient(activity).receiveFile(channel, Uri.fromFile(file), false);
        }
    }

    @Override
    public void onChannelClosed(@NonNull ChannelClient.Channel var1, int var2, int var3) {
    }

    @Override
    public void onInputClosed(@NonNull ChannelClient.Channel channel, int i, int i1) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, "File received!", Toast.LENGTH_SHORT).show();
                activity.stopProgressBar();
            }
        });
    }

    public void setActivity(DataGatheringActivity activity) {
        this.activity = activity;
    }

    public double getPreviousBAC() {
        if (walk != null) return walk.getBAC();
        return 0.0;
    }
}
