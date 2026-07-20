package edu.wpi.alcogaitdatagatherer.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.box.androidsdk.content.BoxApiFile
import com.github.lzyzsd.circleprogress.DonutProgress
import edu.wpi.alcogaitdatagatherer.R
import edu.wpi.alcogaitdatagatherer.tasks.UploadToBoxTask
import edu.wpi.alcogaitdatagatherer.ui.activities.HomeActivity
import edu.wpi.alcogaitdatagatherer.ui.views.CustomSurveyView
import java.io.File
import java.text.SimpleDateFormat
import java.util.LinkedList
import java.util.concurrent.Executors

/**
 * Supplies saved survey files to the home screen's list and starts their Box uploads.
 */
class SurveyListAdapter(
    private val homeActivity: HomeActivity,
    files: LinkedList<File>,
    private val listView: ListView,
) : BaseAdapter() {

    private val inflater = LayoutInflater.from(homeActivity)

    init {
        Companion.files = files
    }

    /** Caches the child views so recycled list rows do not repeat view lookups */
    private class ViewHolder(
        val fileIdTextView: TextView,
        val dateModifiedTextView: TextView,
        val fileUploadProgress: DonutProgress,
        val customSurveyView: CustomSurveyView,
    )

    override fun getCount(): Int = files.size

    override fun getItem(position: Int): File = files[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val file = getItem(position)
        val (row, holder) = if (convertView == null) {
            val inflatedRow = inflater.inflate(R.layout.survey_list_view, parent, false)
            val surveyView = inflatedRow.findViewById<CustomSurveyView>(R.id.customSurveyView)
            val newHolder = ViewHolder(
                fileIdTextView = surveyView.fileIDTextView,
                dateModifiedTextView = surveyView.dateModifiedTextView,
                fileUploadProgress = surveyView.donutProgress,
                customSurveyView = surveyView,
            )
            inflatedRow.tag = newHolder
            inflatedRow to newHolder
        } else {
            convertView to (convertView.tag as ViewHolder)
        }

        val fileName = file.name.substring(HomeActivity.FILE_SHOULD_START_WITH.length)
        val lastModified = SimpleDateFormat("MM/dd/yyyy   hh:mm a").format(file.lastModified())

        holder.fileIdTextView.text = "Subject ID ${fileName.trim()}"
        holder.dateModifiedTextView.text = lastModified
        return row
    }

    /**
     * Returns an attached list row when available; otherwise creates a row for the requested
     * adapter position. This lets uploads update progress for off-screen items as before.
     */
    fun getViewByPosition(position: Int): View {
        val firstVisiblePosition = listView.firstVisiblePosition
        val lastVisiblePosition = firstVisiblePosition + listView.childCount - 1

        return if (position !in firstVisiblePosition..lastVisiblePosition) {
            getView(position, null, listView)
        } else {
            listView.getChildAt(position - firstVisiblePosition)
        }
    }

    /** Starts up to five concurrent Box uploads and displays each row's progress indicator. */
    fun syncWithBox(fileApi: BoxApiFile) {
        val executor = Executors.newFixedThreadPool(5)
        files.forEachIndexed { index, file ->
            val holder = getViewByPosition(index).tag as ViewHolder
            holder.fileUploadProgress.visibility = View.VISIBLE
            UploadToBoxTask(holder.customSurveyView, file, fileApi).executeOnExecutor(executor)
        }
        executor.shutdown()
    }

    /** Posts a short message safely when this adapter is called from a background thread. */
    @Suppress("unused")
    private fun showToast(text: String) {
        homeActivity.runOnUiThread {
            Toast.makeText(homeActivity, text, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private lateinit var files: LinkedList<File>

        /** Returns persisted subject identifiers without leading zeroes. */
        @JvmStatic
        fun getSavedIDs(): LinkedList<String> = LinkedList<String>().apply {
            files.forEach { file ->
                val idOnly = file.name.substring(HomeActivity.FILE_SHOULD_START_WITH.length)
                add(idOnly.replaceFirst(Regex("^0+(?!$)"), ""))
            }
        }
    }
}
