package edu.wpi.alcogaitdatagatherer.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.wpi.alcogaitdatagatherer.R
import edu.wpi.alcogaitdatagatherer.models.TestSubject
import edu.wpi.alcogaitdatagatherer.ui.fragments.WalkReportFragment
import java.util.LinkedList

/**
 * Displays one selectable row per recorded walk followed by a final row for the report message
 */
class MyWalkReportRecyclerViewAdapter(
    private val testSubject: TestSubject,
    private val listener: WalkReportFragment.ReportFragmentListener?,
) : RecyclerView.Adapter<MyWalkReportRecyclerViewAdapter.ViewHolder>(),
    CompoundButton.OnCheckedChangeListener {

    private val checkBoxStates: LinkedList<Boolean> = testSubject.booleanWalksList
    private var walkReportMessageInput: EditText? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == WALK_ROW_VIEW_TYPE) {
            R.layout.fragment_walkreport
        } else {
            R.layout.report_text_box
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view, viewType == WALK_ROW_VIEW_TYPE)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // The last adapter position is the message input, not a walk selection
        if (position == checkBoxStates.size) {
            walkReportMessageInput = holder.walkReportMessageInput
            return
        }

        val walkNumber = position + 1
        holder.walkNumber = walkNumber
        holder.walkNumberView!!.text = "Walk $walkNumber"
        holder.walkDetailsView!!.text = "${testSubject.sampleSizeMap[walkNumber]} data samples"

        holder.checkBox!!.apply {
            // Remove the previous listener before setting checked, which prevents recycled rows
            // from writing an old position back into the state list during binding
            setOnCheckedChangeListener(null)
            tag = position
            isChecked = checkBoxStates[position]
            setOnCheckedChangeListener(this@MyWalkReportRecyclerViewAdapter)
        }
    }

    override fun getItemCount(): Int = checkBoxStates.size + 1

    override fun getItemViewType(position: Int): Int =
        if (position == checkBoxStates.size) REPORT_TEXT_VIEW_TYPE else WALK_ROW_VIEW_TYPE

    fun isChecked(position: Int): Boolean = checkBoxStates[position]

    fun setChecked(position: Int, isChecked: Boolean) {
        checkBoxStates[position] = isChecked
    }

    fun toggle(position: Int) {
        setChecked(position, !isChecked(position))
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        checkBoxStates[buttonView.tag as Int] = isChecked
    }

    /** Submits the selected walks and the note currently entered in the final row. */
    fun saveReport() {
        listener?.submitReport(checkBoxStates, walkReportMessageInput?.text?.toString().orEmpty())
    }

    /**
     * A holder supports either layout. Walk controls are null only for the final message row.
     */
    class ViewHolder(view: View, isWalkRow: Boolean) : RecyclerView.ViewHolder(view) {
        val walkNumberView: TextView? = if (isWalkRow) {
            view.findViewById(R.id.fragmentListWalkNumber)
        } else {
            null
        }
        val walkDetailsView: TextView? = if (isWalkRow) {
            view.findViewById(R.id.fragmentListWalkDetails)
        } else {
            null
        }
        val checkBox: CheckBox? = if (isWalkRow) {
            view.findViewById(R.id.walkSelectionCheckBox)
        } else {
            null
        }
        val walkReportMessageInput: EditText? = if (isWalkRow) {
            null
        } else {
            view.findViewById(R.id.walkReportMessageInput)
        }
        var walkNumber: Int = 0

        override fun toString(): String = "${super.toString()} 'Walk $walkNumber'"
    }

    private companion object {
        // Resource IDs are generated at build time, so they cannot be Kotlin const values.
        val WALK_ROW_VIEW_TYPE = R.layout.fragment_walkreport
        val REPORT_TEXT_VIEW_TYPE = R.layout.report_text_box
    }
}
