package edu.wpi.alcogaitdatagatherer.ui.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.wpi.alcogaitdatagatherer.R
import edu.wpi.alcogaitdatagatherer.models.TestSubject
import edu.wpi.alcogaitdatagatherer.ui.activities.DataGatheringActivity
import edu.wpi.alcogaitdatagatherer.ui.adapters.MyWalkReportRecyclerViewAdapter
import java.util.LinkedList

/** Dialog that lets the tester flag recorded walks and attach a free-text report */
class WalkReportFragment : DialogFragment() {

    private var columnCount = 1
    private var listener: ReportFragmentListener? = null
    private lateinit var testSubject: TestSubject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            columnCount = args.getInt(ARG_COLUMN_COUNT, 1)
            @Suppress("DEPRECATION")
            val subject = args.getSerializable(DataGatheringActivity.TB_FOR_WALK_REPORT) as? TestSubject
            testSubject = requireNotNull(subject) {
                "WalkReportFragment requires a TestSubject argument"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_walkreport_list, container, false)

        // The dialog uses a transparent background so the layout controls its visible shape
        dialog?.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewList)
        recyclerView.addItemDecoration(
            DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL),
        )
        recyclerView.layoutManager = if (columnCount <= 1) {
            LinearLayoutManager(recyclerView.context)
        } else {
            GridLayoutManager(recyclerView.context, columnCount)
        }
        recyclerView.adapter = MyWalkReportRecyclerViewAdapter(testSubject, listener)

        view.findViewById<AppCompatTextView>(R.id.reportSave).setOnClickListener {
            // The adapter owns the selection state and message input, so it submits the report
            (recyclerView.adapter as? MyWalkReportRecyclerViewAdapter)?.saveReport()
        }
        view.findViewById<AppCompatTextView>(R.id.reportCancel).setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ReportFragmentListener
            ?: throw RuntimeException("$context must implement ReportFragmentListener")
    }

    override fun onDetach() {
        super.onDetach()
        // Avoid retaining the activity after this dialog has been detached
        listener = null
    }

    /** Receives the selected walks and note when the user presses Save */
    interface ReportFragmentListener {
        fun submitReport(checkBoxStates: LinkedList<Boolean>, reportMessage: String)
    }

    companion object {
        private const val ARG_COLUMN_COUNT = "column-count"

        /** Creates the dialog with the requested number of walk columns */
        @JvmStatic
        fun newInstance(columnCount: Int): WalkReportFragment = WalkReportFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_COLUMN_COUNT, columnCount)
            }
        }
    }
}
