package edu.wpi.alcogaitdatagatherer.ui.views

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.github.lzyzsd.circleprogress.DonutProgress
import edu.wpi.alcogaitdatagatherer.R
import edu.wpi.alcogaitdatagatherer.interfaces.BoxUploadProgressListener

/**
 * Created by Adonay on 10/24/2017.
 *
 * Custom list-row view showing a survey/file entry: its ID, last-modified
 * date, and an upload progress indicator (a "donut" ring) that animates as
 * uploads proceed, then gets swapped out for a success/error icon once the
 * upload finishes.
 *
 * Implements [BoxUploadProgressListener] so it can be handed directly to an
 * upload task (see [edu.wpi.alcogaitdatagatherer.tasks.UploadToBoxTask]) as
 * its progress callback.
 */
class CustomSurveyView : RelativeLayout, BoxUploadProgressListener {

    // Root of the inflated custom_survey_view layout
    lateinit var rootView: View
        private set
    lateinit var linearLayout: LinearLayout
        private set
    lateinit var fileIDTextView: TextView
        private set
    lateinit var dateModifiedTextView: TextView
        private set
    lateinit var donutProgress: DonutProgress
        private set

    /** Constructor used when creating this view purely in code */
    constructor(context: Context) : super(context) {
        init(context)
    }

    /** Constructor used when this view is inflated from XML */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    /** Inflates the layout and binds each child view */
    private fun init(context: Context) {
        rootView = inflate(context, R.layout.custom_survey_view, this)
        linearLayout = rootView.findViewById(R.id.fileInfoLayout)
        fileIDTextView = rootView.findViewById(R.id.fileIDTextView)
        dateModifiedTextView = rootView.findViewById(R.id.dateModifiedTextView)
        donutProgress = rootView.findViewById(R.id.fileUploadProgressBar)
    }

    /**
     * Called as an upload progresses. Animates the donut ring from its
     * current value to the new [progress] percentage over 100ms.
     */
    override fun onProgressUpdate(progress: Int) {
        donutProgress.visibility = View.VISIBLE
        val anim = ObjectAnimator.ofInt(donutProgress, "progress", donutProgress.progress, progress)
        donutProgress.progress = progress
        anim.interpolator = DecelerateInterpolator()
        anim.duration = 100
        anim.start()
    }

    /**
     * Replaces the donut progress indicator with a success checkmark icon,
     * keeping it in the same position in the layout.
     */
    fun displayUploadComplete() {
        val i = this.indexOfChild(donutProgress)
        this.removeView(donutProgress)
        val uploadCompletedLogo = ImageView(context)
        uploadCompletedLogo.setImageResource(R.drawable.ic_upload_success)
        val newParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        newParams.addRule(RelativeLayout.ALIGN_PARENT_END)
        uploadCompletedLogo.layoutParams = newParams
        this.addView(uploadCompletedLogo, i)
    }

    /**
     * Replaces the donut progress indicator with an error icon, keeping it
     * in the same position in the layout.
     */
    fun displayUploadError() {
        val i = this.indexOfChild(donutProgress)
        this.removeView(donutProgress)
        val uploadCompletedLogo = ImageView(context)
        uploadCompletedLogo.setImageResource(R.drawable.ic_upload_error)
        val newParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        newParams.addRule(RelativeLayout.ALIGN_PARENT_END)
        uploadCompletedLogo.layoutParams = newParams
        this.addView(uploadCompletedLogo, i)
    }
}