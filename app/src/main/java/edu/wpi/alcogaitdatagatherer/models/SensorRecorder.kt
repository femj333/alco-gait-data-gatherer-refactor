package edu.wpi.alcogaitdatagatherer.models

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.Sensor.TYPE_GYROSCOPE
import android.hardware.Sensor.TYPE_MAGNETIC_FIELD
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import edu.wpi.alcogaitdatagatherer.tasks.SaveWalkHolderToCSVTask
import edu.wpi.alcogaitdatagatherer.ui.activities.DataGatheringActivity
import edu.wpi.alcogaitdatagatherercommon.CommonCode
import edu.wpi.alcogaitdatagatherercommon.WalkType
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.LinkedList

/**
 * Records phone-sensor data (accelerometer, gyroscope, magnetometer) for a
 * "walk" performed by a [TestSubject], and also receives the matching
 * sensor CSV streamed over from a paired smartwatch via the Wearable Data
 * Layer API.
 *
 * Extends [ChannelClient.ChannelCallback] to receive file-transfer events
 * from the watch, and implements [SensorEventListener] to receive live
 * phone sensor readings.
 *
 * @param gatheringActivity the activity that owns this recorder (used for UI callbacks
 *                           and to look up the SensorManager)
 * @param rootFolderName    root directory on disk where walk data / reports are written
 * @param testSubject       the subject currently being recorded
 * @param listener          callback used to push UI updates (walk number, logs, etc.)
 */
class SensorRecorder(
    private var activity: DataGatheringActivity,
    private val rootFolderName: String,
    private var testSubject: TestSubject,
    private val listener: SensorRecorderListener?
) : ChannelClient.ChannelCallback(), SensorEventListener {

    /**
     * Callback interface used to notify the UI layer of recording state
     * changes without SensorRecorder needing to know about Views directly.
     */
    interface SensorRecorderListener {
        fun onWalkNumberUpdate(info: String)
        fun onWalkLogUpdate(log: String)
        fun onRecordingStatusChanged(isRecording: Boolean)
        fun onStartButtonTextUpdate(text: String)
    }

    // The walk currently being recorded (or just finished)
    private var walk: Walk? = null

    // Android sensors
    private val mSensorManager: SensorManager =
        activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val mAccelerometer: Sensor? = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER)
    private val mGyroscope: Sensor? = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE)
    private val mMagnetometer: Sensor? = mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD)

    // Latest low-pass-filtered readings from each sensor, null until first reading arrives
    private var accelVal: FloatArray? = null
    private var gyroVal: FloatArray? = null
    private var magVal: FloatArray? = null

    // Rolling log (most recent MAX_LOGS walks) shown in the UI
    private val logQueue: LinkedList<Walk> = LinkedList()

    // Path of the folder currently used to store the active walk's data
    private var walkFolderName: String? = null

    private var isRecording: Boolean = false
    private var currentWalkNumber: Int = 1
    private var currentWalkType: WalkType? = WalkType.NORMAL

    private val TAG = "SensorRecorder"

    companion object {
        // Smoothing factor for the exponential low-pass filter applied to raw sensor data
        private const val ALPHA = 0.15f
    }

    init {
        updateWalkNumberDisplay()
        testSubject.setCurrentWalkHolder(WalkHolder(currentWalkNumber))
        testSubject.setWalkTypeAmount(activity)
        prepareReportFile()
    }

    /** Subscribes this recorder to accelerometer/gyroscope/magnetometer updates */
    fun registerListeners() {
        mSensorManager.registerListener(this, mAccelerometer, CommonCode.DELAY_IN_MILLISECONDS * 1000)
        mSensorManager.registerListener(this, mGyroscope, CommonCode.DELAY_IN_MILLISECONDS * 1000)
        mSensorManager.registerListener(this, mMagnetometer, CommonCode.DELAY_IN_MILLISECONDS * 1000)
    }

    /** Unsubscribes from all sensor updates (call when recording stops / activity pauses) */
    fun unregisterListeners() {
        mSensorManager.unregisterListener(this)
    }

    /**
     * Fired on every new reading from any registered sensor. Filters and stores the data
     * derives device orientation ("compass") data when accelerometer and magnetometer data
     * are available.
     *
     * @param sensorEvent the new sensor reading
     */
    override fun onSensorChanged(sensorEvent: SensorEvent) {
        // values returned by this sensor cannot be trusted
        if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return
        }

        // sensor is recording
        if (isRecording) {
            val sensorName = sensorEvent.sensor.name

            // accelerometer data
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // filter raw value
                accelVal = lowPass(sensorEvent.values.clone(), accelVal)
                // add to current walk
                walk?.addPhoneAccelerometerData(
                    CommonCode.generatePrintableSensorData(sensorName, accelVal, sensorEvent.accuracy, sensorEvent.timestamp)
                )
            }

            // gyroscope data
            if (sensorEvent.sensor.type == Sensor.TYPE_GYROSCOPE) {
                // filter raw value
                gyroVal = lowPass(sensorEvent.values.clone(), gyroVal)
                // add to current walk
                walk?.addPhoneGyroscopeData(
                    CommonCode.generatePrintableSensorData(sensorName, gyroVal, sensorEvent.accuracy, sensorEvent.timestamp)
                )
            }

            // magnetometer data
            if (sensorEvent.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                // filter raw value
                magVal = lowPass(sensorEvent.values.clone(), magVal)
            }

            // Once we have both accel and mag data we can compute device orientation
            if (accelVal != null && magVal != null) {
                val r = FloatArray(9)
                val i = FloatArray(9)
                val success = SensorManager.getRotationMatrix(r, i, accelVal, magVal)
                if (success) {
                    val compassVal = FloatArray(3)
                    SensorManager.getOrientation(r, compassVal)
                    walk?.addCompassData(
                        CommonCode.generatePrintableSensorData("Compass", compassVal, sensorEvent.accuracy, sensorEvent.timestamp)
                    )
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {
        // No-op: accuracy changes aren't currently handled.
    }

    /**
     * Begins a new recording for the given BAC
     *
     * @param BAC the given BAC
     */
    fun startRecording(BAC: Double) {
        // register recording
        isRecording = true
        listener?.onRecordingStatusChanged(true)
        // register sensor listeners
        registerListeners()
        // create new walk with given BAC, current walk type
        currentWalkType?.let { walkType ->
            walk = Walk(testSubject.getCurrentWalkHolder().walkNumber, BAC, walkType)
        }
    }

    /**
     * Ends the current recording
     *
     * @return true if there is another walk type left to record for this
     *         walk number, false if this walk number is now complete.
     */
    fun stopRecording(): Boolean {
        // end current reading
        isRecording = false
        listener?.onRecordingStatusChanged(false)
        // unregister sensors
        unregisterListeners()

        // save walk to subject's current walk holder
        testSubject.setCurrentWalkHolder(testSubject.getCurrentWalkHolder().addWalk(walk))

        // advance to next required walk type
        currentWalkType = testSubject.getCurrentWalkHolder().nextWalkType
        updateWalkLogDisplay(true)

        return if (currentWalkType != null) { // there is another walk type left to record for this walk number
            updateWalkNumberDisplay()
            true
        } else { // this walk number is now complete
            listener?.onStartButtonTextUpdate("SAVE WALK #$currentWalkNumber")
            false
        }
    }

    /** Creates the on-disk folder ("walk_N") that this walk's data will be saved into. */
    fun prepareWalkStorage() {
        walkFolderName = rootFolderName + File.separator + "walk_" + testSubject.getCurrentWalkHolder().walkNumber
        val f = File(walkFolderName!!)
        f.mkdirs()
    }

    /** Kicks off an async task that writes the current WalkHolder's data out to CSV. */
    fun saveCurrentWalkNumberToCSV(bacInput: EditText) {
        SaveWalkHolderToCSVTask(this, walkFolderName, bacInput).execute()
    }

    /** Pushes "Walk Number X : TYPE" text to the UI listener. */
    private fun updateWalkNumberDisplay() {
        listener?.onWalkNumberUpdate("Walk Number $currentWalkNumber : ${currentWalkType.toString()}")
    }

    /**
     * Shows a confirmation dialog; if confirmed, wipes all walks recorded
     * for the current walk number and starts that walk number over.
     */
    fun restartCurrentWalkNumber(context: Context, onRestartConfirmed: Runnable?) {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    restartWalkHolder()
                    onRestartConfirmed?.run()
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    // User cancelled; nothing to do.
                }
            }
        }
        AlertDialog.Builder(context)
            .setTitle("Restart")
            .setMessage(
                "Do you want to remove all walks for the current walk number? (Walk Number " +
                        testSubject.getCurrentWalkHolder().walkNumber + ") (" +
                        testSubject.getCurrentWalkHolder().sampleSize + " samples recorded)"
            )
            .setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener)
            .show()
    }

    /** Replaces the current WalkHolder with a fresh, empty one for the same walk number. */
    private fun restartWalkHolder() {
        isRecording = false
        testSubject.replaceWalkHolder(WalkHolder(currentWalkNumber))
        currentWalkNumber = testSubject.getCurrentWalkHolder().walkNumber
        currentWalkType = testSubject.getCurrentWalkHolder().nextWalkType
        updateWalkNumberDisplay()
        clearWalkLog()
        testSubject.setWalkTypeAmount(activity)
        walk = testSubject.getCurrentWalkHolder().get(currentWalkType)
    }

    /**
     * Shows a confirmation dialog; if confirmed, discards just the most
     * recently recorded walk (letting the subject redo one specific walk
     * type) rather than the whole walk number.
     */
    fun reDoWalk(context: Context, onRedoConfirmed: Runnable?) {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val walkTypeToRemove = walk?.walkType
                    if (walkTypeToRemove != null) {
                        testSubject.setCurrentWalkHolder(testSubject.getCurrentWalkHolder().removeWalk(walkTypeToRemove))
                    }
                    if (!testSubject.getCurrentWalkHolder().hasWalk(WalkType.NORMAL)) {
                        testSubject.setWalkTypeAmount(activity)
                    }
                    currentWalkType = walk?.walkType
                    updateWalkNumberDisplay()
                    logQueue.removeLast()
                    updateWalkLogDisplay(false)

                    walk = if (currentWalkType != WalkType.NORMAL) {
                        testSubject.getCurrentWalkHolder()
                            .get(testSubject.getCurrentWalkHolder().getPreviousWalkType(currentWalkType))
                    } else {
                        null
                    }

                    listener?.onStartButtonTextUpdate("START WALK")
                    onRedoConfirmed?.run()
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    // User cancelled; nothing to do.
                }
            }
        }
        AlertDialog.Builder(context)
            .setTitle("Re-Do Walk")
            .setMessage(
                "Do you want re-do the previous walk? (Walk Number " +
                        testSubject.getCurrentWalkHolder().walkNumber + " : " +
                        testSubject.getCurrentWalkHolder().getPreviousWalkType(currentWalkType) + ")"
            )
            .setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener)
            .show()
    }

    /**
     * Rebuilds the "last N walks" log shown in the UI.
     *
     * @param addNewWalk if true, appends [walk] to the queue (trimming the
     *                    oldest entry if already at capacity) before rendering.
     */
    fun updateWalkLogDisplay(addNewWalk: Boolean) {
        val MAX_LOGS = 5

        if (addNewWalk) {
            if (logQueue.size >= MAX_LOGS) {
                logQueue.removeFirst()
            }
            walk?.let { logQueue.add(it) }
        }

        val walkLog = StringBuilder("Last $MAX_LOGS Walks:")
        for (i in logQueue.size - 1 downTo 0) {
            val aWalk = logQueue[i]
            val walkTypeString = if (aWalk.walkType == WalkType.STANDING_ON_ONE_FOOT) {
                "Stand 1 Foot"
            } else {
                aWalk.walkType.toString()
            }
            walkLog.append("\nWalk Number ").append(aWalk.walkNumber)
                .append(" : BAC =").append(aWalk.BAC).append(", ").append(walkTypeString)
        }

        listener?.onWalkLogUpdate(walkLog.toString())
    }

    /** Clears the rolling walk log, both internally and in the UI. */
    fun clearWalkLog() {
        logQueue.clear()
        listener?.onWalkLogUpdate("")
    }

    fun getTestSubject(): TestSubject = testSubject

    /**
     * Simple exponential low-pass filter used to smooth noisy raw sensor
     * readings: output = output + ALPHA * (input - output).
     *
     * On the very first call (no previous output yet) the raw input is
     * returned as-is so we have something to filter against next time.
     */
    private fun lowPass(input: FloatArray, output: FloatArray?): FloatArray {
        if (output == null) return input

        for (i in input.indices) {
            output[i] = output[i] + ALPHA * (input[i] - output[i])
        }
        return output
    }

    /**
     * Advances to a brand-new walk number: creates a fresh WalkHolder,
     * resets the walk-type sequence, and resets the start button label.
     */
    fun incrementWalkNumber() {
        currentWalkNumber++
        testSubject.addNewWalkHolder(WalkHolder(currentWalkNumber))
        currentWalkType = testSubject.getCurrentWalkHolder().nextWalkType
        updateWalkNumberDisplay()
        testSubject.setWalkTypeAmount(activity)
        listener?.onStartButtonTextUpdate("START WALK")
    }

    /**
     * Creates (or overwrites) report.txt in the root folder and writes the
     * test subject's basic info to it. Called once when the recorder is
     * first constructed.
     */
    fun prepareReportFile() {
        val file = File(rootFolderName, "report.txt")

        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            val fileWriter = FileWriter(file, false)
            val bufferWriter = BufferedWriter(fileWriter)

            bufferWriter.append(testSubject.printInfo())

            bufferWriter.close()

            fileWriter.flush()
            fileWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    /**
     * Appends a summary to report.txt: which walk numbers were flagged
     * ("reported") by the tester, followed by any free-text report message.
     */
    fun saveWalkReport() {
        val file = File(rootFolderName, "report.txt")

        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            val fileWriter = FileWriter(file, true)
            val bufferWriter = BufferedWriter(fileWriter)

            bufferWriter.append("\n\nReported Walk Numbers:\n")
            var hasReportedWalks = false
            val booleanWalksList = testSubject.getBooleanWalksList()
            for (i in booleanWalksList.indices) {
                if (booleanWalksList[i]) {
                    bufferWriter.append((i + 1).toString())
                    if (i != booleanWalksList.size - 1 && booleanWalksList.size > 1) {
                        bufferWriter.append(", ")
                    }
                    hasReportedWalks = true
                }
            }
            if (!hasReportedWalks) {
                bufferWriter.append("None")
            }

            bufferWriter.append("\n\nReport Message:\n")
            bufferWriter.append(testSubject.getReportMessage() + "\n")

            bufferWriter.close()

            fileWriter.flush()
            fileWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    /**
     * Fired when the smartwatch opens a data channel to send its recorded
     * sensor CSV over. If the channel matches the expected CSV path, starts
     * receiving the incoming file into walk_N/watch.csv.
     */
    override fun onChannelOpened(channel: ChannelClient.Channel) {
        if (channel.path == CommonCode.WEAR_CSV_FILE_CHANNEL_PATH) {
            activity.startProgressBar()
            activity.updateProgressBarMessage("Receiving Data From Watch")

            val file = File(walkFolderName + File.separator + "watch.csv")

            try {
                file.createNewFile()
            } catch (e: IOException) {
                // handle error
            }
            Wearable.getChannelClient(activity).receiveFile(channel, Uri.fromFile(file), false)
        }
    }

    override fun onChannelClosed(channel: ChannelClient.Channel, closeReason: Int, appErrorCode: Int) {
        // No-op: channel-closed events aren't currently handled.
    }

    /** Fired once the watch's file transfer finishes; notifies the user and hides the progress bar. */
    override fun onInputClosed(channel: ChannelClient.Channel, i: Int, i1: Int) {
        activity.runOnUiThread {
            Toast.makeText(activity, "File received!", Toast.LENGTH_SHORT).show()
            activity.stopProgressBar()
        }
    }

    fun setActivity(activity: DataGatheringActivity) {
        this.activity = activity
    }

    /** Returns the BAC of the most recently recorded walk, or 0.0 if none exists yet. */
    fun getPreviousBAC(): Double {
        return walk?.BAC ?: 0.0
    }
}