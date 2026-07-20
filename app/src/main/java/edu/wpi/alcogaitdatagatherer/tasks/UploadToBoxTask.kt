package edu.wpi.alcogaitdatagatherer.tasks

import android.os.AsyncTask
import android.util.Log
import com.box.androidsdk.content.BoxApiFile
import com.box.androidsdk.content.BoxConstants
import com.box.androidsdk.content.BoxException
import com.box.androidsdk.content.listeners.ProgressListener
import com.box.androidsdk.content.models.BoxEntity
import com.box.androidsdk.content.models.BoxError
import com.box.androidsdk.content.models.BoxFile
import edu.wpi.alcogaitdatagatherer.ui.views.CustomSurveyView
import java.io.File
import java.net.HttpURLConnection

/**
 * Created by Adonay on 1/4/2018.
 *
 * Background task that uploads a single recorded data file to Box (cloud
 * storage), reporting upload progress back to a [CustomSurveyView] and
 * showing a success/error state once the upload finishes.
 */
class UploadToBoxTask(
    private val customSurveyView: CustomSurveyView,
    private val uploadFile: File,
    private val mFileApi: BoxApiFile
) : AsyncTask<Void, Int, Void>() {

    companion object {
        private const val TAG = "UploadToBoxTask"
    }

    /**
     * Performs the upload on a background thread. Reports progress via
     * [publishProgress] as bytes are sent, and handles the special case
     * where Box reports a conflict (file already exists) by treating it as
     * a completed upload rather than a failure.
     */
    override fun doInBackground(vararg params: Void?): Void? {
        try {
            val request = mFileApi.getUploadRequest(uploadFile, BoxConstants.ROOT_FOLDER_ID)
            request.setProgressListener(object : ProgressListener {
                override fun onProgressChanged(numBytes: Long, totalBytes: Long) {
                    publishProgress((100 * (numBytes / totalBytes)).toInt())
                }
            })
            val uploadFileInfo: BoxFile = request.send()
            Log.d(TAG, "Uploaded " + uploadFileInfo.name)
            // loadRootFolder()
        } catch (e: BoxException) {
            val error: BoxError? = e.asBoxError
            if (error != null && error.status == HttpURLConnection.HTTP_CONFLICT) {
                val conflicts: ArrayList<BoxEntity>? = error.contextInfo.conflicts
                if (conflicts != null && conflicts.size == 1 && conflicts[0] is BoxFile) {
                    publishProgress(100)
                    return null
                }
            }
            Log.d(TAG, "Upload failed")
            e.printStackTrace()
        }
        return null
    }

    /** Forwards each progress update to the survey view's progress indicator. */
    override fun onProgressUpdate(vararg values: Int?) {
        customSurveyView.onProgressUpdate(values[0] ?: 0)
    }

    /**
     * Once the background task finishes, checks whether the progress
     * indicator actually reached 100% and shows the corresponding
     * success/error state on the survey view.
     */
    override fun onPostExecute(result: Void?) {
        if (customSurveyView.donutProgress.progress == 100) {
            customSurveyView.displayUploadComplete()
        } else {
            customSurveyView.displayUploadError()
        }
    }
}