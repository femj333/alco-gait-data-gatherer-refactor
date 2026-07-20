package edu.wpi.alcogaitdatagatherer.models

import edu.wpi.alcogaitdatagatherercommon.WalkType
import java.io.Serializable
import java.util.LinkedList

/**
 * Created by Adonay on 9/29/2017.
 *
 * Represents a single recorded "walk" trial: the phone's accelerometer,
 * gyroscope, and derived compass readings captured while a test subject
 * performed one walk (of a given [WalkType]) at a given BAC level.
 *
 * Implements [Serializable] so it can be passed around (e.g. between
 * activities/services) or persisted as part of a [WalkHolder].
 */
class Walk(
    // Which walk number (trial set) this walk belongs to.
    val walkNumber: Int,
    // Blood alcohol content recorded for the subject during this walk.
    val BAC: Double,
    // The kind of walk performed
    val walkType: WalkType
) : Serializable {

    // Raw sensor readings collected during this walk, one String[] row per reading
    private val phoneAccelerometerDataList: LinkedList<Array<String>> = LinkedList()
    private val phoneGyroscopeDataList: LinkedList<Array<String>> = LinkedList()
    private val compassDataList: LinkedList<Array<String>> = LinkedList()

    // Number of samples received from the paired smartwatch for this walk (set separately,
    // since watch data arrives as a file transfer rather than row-by-row).
    private var watchSampleSize: Int = 0

    /** Appends one row of phone accelerometer data (e.g. [name, x, y, z, accuracy, timestamp]) */
    fun addPhoneAccelerometerData(sensorData: Array<String>) {
        this.phoneAccelerometerDataList.add(sensorData)
    }

    /** Appends one row of phone gyroscope data */
    fun addPhoneGyroscopeData(sensorData: Array<String>) {
        this.phoneGyroscopeDataList.add(sensorData)
    }

    /** Appends one row of derived compass/orientation data */
    fun addCompassData(compassData: Array<String>) {
        this.compassDataList.add(compassData)
    }

    /**
     * Total number of samples recorded for this walk across all phone
     * sensors plus whatever sample count was reported by the watch.
     */
    val sampleSize: Int
        get() = phoneAccelerometerDataList.size + phoneGyroscopeDataList.size +
                compassDataList.size + watchSampleSize

    /** Records how many samples the paired watch captured for this walk. */
    fun addWatchSampleSize(sampleSize: Int) {
        this.watchSampleSize = sampleSize
    }

    /**
     * Builds this walk's data as a list of CSV rows, ready to be written to
     * disk: a BAC header line, then each sensor's section (title row,
     * column-header row, and its data rows), separated by blank rows.
     */
    fun toCSVFormat(): LinkedList<Array<String>> {
        val SPACE = arrayOf("")
        val PHONE_ACCELEROMETER_TITLE = arrayOf("ACCELEROMETER DATA (PHONE)")
        val PHONE_GYROSCOPE_TITLE = arrayOf("GYROSCOPE DATA (PHONE)")
        val COMPASS_TITLE = arrayOf("COMPASS (PHONE)")

        val A_G_TABLE_HEADER = arrayOf("Sensor Name", "X", "Y", "Z", "Accuracy", "Timestamp")
        val COMPASS_TABLE_HEADER = arrayOf("Derived Data", "Azimuth", "Pitch", "Roll", "Accuracy", "Timestamp")

        val csvFormat = LinkedList<Array<String>>()

        val walkInformation = arrayOf("BAC = $BAC")

        csvFormat.add(walkInformation)
        csvFormat.add(SPACE)
        csvFormat.add(PHONE_ACCELEROMETER_TITLE)
        csvFormat.add(A_G_TABLE_HEADER)
        csvFormat.addAll(phoneAccelerometerDataList)
        csvFormat.add(SPACE)
        csvFormat.add(PHONE_GYROSCOPE_TITLE)
        csvFormat.add(A_G_TABLE_HEADER)
        csvFormat.addAll(phoneGyroscopeDataList)
        csvFormat.add(SPACE)
        csvFormat.add(COMPASS_TITLE)
        csvFormat.add(COMPASS_TABLE_HEADER)
        csvFormat.addAll(compassDataList)

        return csvFormat
    }
}