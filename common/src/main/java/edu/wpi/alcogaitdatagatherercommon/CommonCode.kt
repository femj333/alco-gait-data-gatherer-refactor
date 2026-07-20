package edu.wpi.alcogaitdatagatherercommon

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared phone-to-wear protocol values and sensor-data formatting helpers.
 *
 * `const` values keep the original Java static-field API for both applications.
 */
object CommonCode {
    const val WEAR_DISCOVERY_NAME = "record_gait_data"
    const val DELAY_IN_MILLISECONDS = 5
    const val RECORD_TIME_IN_SECONDS = 60
    const val START_RECORDING_PATH = "/start_recording"
    const val STOP_RECORDING_PATH = "/stop_recording"
    const val WEAR_MESSAGE_PATH = "/message"
    const val WEAR_HOME_ACTIVITY_PATH = "/start/WearHomeActivity"
    const val REDO_PREVIOUS_WALK_PATH = "/redo_previous_walk"
    const val STOP_RECORDING = "stop_recording"
    const val SAVE_WALKS = "save_walks"
    const val SAVE_WALKS_ACK = "save_walks_acknowledgement"
    const val RESTART = "restart_survey"
    const val CHECK_IF_APP_OPEN = "check_if_app_open"
    const val APP_OPEN_ACK = "acknowledgement"
    const val RESTART_ACK = "restart_acknowledgement"
    const val REDO_PREVIOUS_WALK_ACK = "redo_walk_acknowledgement"
    const val WEARABLE_DISCONNECTED = "wearable_disconnected"
    const val WEAR_CSV_FILE_CHANNEL_PATH = "/channel_for_wear_csv_file"
    const val SENSOR_NAME = "sensor_name"
    const val ACCURACY = "accuracy"
    const val TIMESTAMP = "timestamp"
    const val VALUES = "values"
    const val SENSOR_PATH = "/sensors/"
    const val OPEN_APP = "open_app"
    const val REQUEST_WEARABLE_DATA_SAMPLE_SIZE = "request_sample_size"
    const val TRANSFER_FINISHED_LONG = 4145646541563468518L
    const val TRANSFER_FINISHED_STRING = "finished"
    const val REFRESH_CONNECTION = "refresh_connection"
    const val WALK_TYPE_INFO = "walk_type_info"

    private val timestampFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.US)

    /**
     * Converts a sensor event to the CSV-ready row shared by the phone and wearable.
     * The timestamp intentionally records current wall-clock time, matching the original format.
     */
    @JvmStatic
    fun generatePrintableSensorData(
        sensorName: String,
        values: FloatArray?,
        accuracy: Int,
        timestamp: Long,
    ): Array<String> {
        // Retain the Java platform-type call contract, while failing as the original code did
        // if an invalid null sensor array is supplied
        val sensorValues = requireNotNull(values) { "Sensor values cannot be null" }
        val result = Array(sensorValues.size + 3) { "" }
        result[0] = sensorName
        sensorValues.forEachIndexed { index, value -> result[index + 1] = value.toString() }
        result[sensorValues.size + 1] = accuracy.toString()

        // Keep the original wall-clock behavior; the sensor timestamp is not used here
        result[sensorValues.size + 2] = timestampFormat.format(Date(System.currentTimeMillis()))
        return result
    }
}
