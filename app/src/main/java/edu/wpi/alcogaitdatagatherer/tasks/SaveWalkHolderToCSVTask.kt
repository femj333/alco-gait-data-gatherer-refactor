package edu.wpi.alcogaitdatagatherer.tasks

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.AsyncTask
import android.widget.EditText
import com.opencsv.CSVWriter
import edu.wpi.alcogaitdatagatherer.models.SensorRecorder
import edu.wpi.alcogaitdatagatherer.models.TestSubject
import edu.wpi.alcogaitdatagatherercommon.WalkType
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Created by Adonay on 12/1/2017.
 *
 * Background task that writes the current walk number's recorded data
 * (every walk type in the current [TestSubject]'s current WalkHolder) out
 * to a "phone.csv" file, showing a progress dialog while it works.
 *
 * [bacInput] is nullable: the on-screen BAC field is now driven by a
 * ViewModel (see DataGatheringActivity), so this task no longer requires a
 * real EditText — it's kept only so this task can re-enable/clear it once
 * saving finishes, if one was actually passed in. Because of that, this
 * task can no longer pull a [android.content.Context] from
 * `bacInput.context` — it now gets one from [sensorRecorder] instead.
 */
class SaveWalkHolderToCSVTask(
    private val sensorRecorder: SensorRecorder,
    private val mFolderName: String?,
    private val bacInput: EditText?
) : AsyncTask<Void, Int, Boolean>() {

    private var savedSamples = 0
    private val dialog: ProgressDialog = ProgressDialog(sensorRecorder.getContext())
    private val testSubject: TestSubject = sensorRecorder.testSubject
    private val space = arrayOf("")

    /** Sets up and shows the progress dialog before background work begins. */
    override fun onPreExecute() {
        super.onPreExecute()
        dialog.setCancelable(false)
        dialog.setTitle("Saving to phone internal storage")
        dialog.setTitle("Writing data to $mFolderName")
        dialog.isIndeterminate = false
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog.progress = 0
        dialog.max = 100
        dialog.show()
    }

    /**
     * Writes every recorded walk type's data to phone.csv on a background
     * thread, publishing progress updates as it goes.
     *
     * @return true if the file was written successfully, false if an
     *         IOException occurred.
     */
    override fun doInBackground(vararg voids: Void?): Boolean {
        val fileName = mFolderName + File.separator + "phone.csv"
        val f = File(fileName)
        try {
            val mFileWriter: FileWriter = if (f.exists() && !f.isDirectory) {
                FileWriter(fileName, false)
            } else {
                FileWriter(fileName)
            }

            val writer = CSVWriter(mFileWriter)

            /*
            val testSubjectTitle = arrayOf("Subject ID", "Gender", "Age", "Weight", "Height(ft and inches)")
            val testSubjectInformation = arrayOf(
                testSubject.subjectID, testSubject.gender.toString(),
                testSubject.age.toString(), "${testSubject.weight} lbs",
                "${testSubject.heightFeet}' ${testSubject.heightInches}''"
            )
            writer.writeNext(testSubjectTitle)
            writer.writeNext(testSubjectInformation)
            writer.writeNext(space)
            */

            var percentProgress = 0
            val max = testSubject.currentWalkHolder.sampleSize
            for (walkType in WalkType.values()) {
                if (testSubject.currentWalkHolder.hasWalk(walkType)) {
                    writer.writeNext(arrayOf(walkType.toString()))
                    writer.writeNext(space)
                    for (data in testSubject.currentWalkHolder.get(walkType)!!.toCSVFormat()) {
                        writer.writeNext(data)
                        if ((++savedSamples / max) * 100 > percentProgress) {
                            percentProgress = (savedSamples / max) * 100
                            publishProgress(percentProgress)
                        }
                    }

                    writer.writeNext(space)
                    writer.writeNext(space)
                    writer.writeNext(space)
                    writer.writeNext(space)
                }
            }

            /*
            val messageTitle = arrayOf("Report Message")
            writer.writeNext(messageTitle)
            val reportMessage = arrayOf(testSubject.reportMessage)
            writer.writeNext(reportMessage)
            */

            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /** Updates the progress dialog's bar and message as samples are written. */
    override fun onProgressUpdate(vararg values: Int?) {
        dialog.progress = values[0] ?: 0
        dialog.setMessage("Saving Walk Number ${testSubject.currentWalkHolder.walkNumber}")
    }

    /**
     * Dismisses the progress dialog once saving finishes. On success,
     * advances to the next walk number and resets the BAC field (if one
     * was provided). On failure, offers to retry.
     */
    override fun onPostExecute(result: Boolean) {
        super.onPostExecute(result)
        dialog.dismiss()
        if (result) {
            sensorRecorder.incrementWalkNumber()
            bacInput?.isEnabled = true
            bacInput?.setText("")
        } else {
            // Show a file-save error dialog with the option to retry.
            val context = sensorRecorder.getContext()
            val alert = AlertDialog.Builder(context)
            alert.setTitle("Save Error")
            alert.setMessage("An error occurred while saving the data to file. Would you like to try saving again?")
            alert.setPositiveButton("YES") { _, _ ->
                SaveWalkHolderToCSVTask(sensorRecorder, mFolderName, bacInput).execute()
            }
            alert.setNegativeButton("NO", null)
            alert.show()
        }
    }
}