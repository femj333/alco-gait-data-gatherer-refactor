package edu.wpi.alcogaitdatagatherer.models

import android.app.Dialog
import android.content.Context
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import edu.wpi.alcogaitdatagatherer.R
import java.io.Serializable
import java.util.LinkedList

/**
 * Created by Adonay on 9/27/2017.
 *
 * Represents the person being tested: their demographic info, plus the
 * bookkeeping needed to track walks recorded across the whole session
 * (which walk numbers were flagged/"reported", sample-size totals per walk
 * number, and the [WalkHolder] currently being recorded into).
 */
class TestSubject(
    val subjectID: String,
    val gender: Gender,
    val age: Int,
    val weight: Double,
    val heightFeet: Int,
    val heightInches: Int
) : Serializable {

    // Backing field for currentWalkHolder, nullable internally
    private var _currentWalkHolder: WalkHolder? = null

    // The WalkHolder currently being recorded into
    var currentWalkHolder: WalkHolder
        get() = _currentWalkHolder!!
        set(value) {
            _currentWalkHolder = value
        }

    // One boolean per walk number recorded so far, tracking whether the
    // tester flagged ("reported") that walk number as noteworthy.
    // NOTE: changed from a list of Walk objects to Booleans to reduce memory usage.
    var booleanWalksList: LinkedList<Boolean> = LinkedList()

    // Maps walk number -> total sample size recorded for that walk number,
    // kept so totals survive even after a WalkHolder is replaced/discarded.
    val sampleSizeMap: HashMap<Int, Int> = HashMap()

    // Free-text note the tester can attach to this subject's session
    var reportMessage: String = ""

    /**
     * Moves on to a new walk number: archives the sample size of the walk
     * holder we're leaving behind, swaps in the new [currentWalkHolder],
     * and adds a fresh "not yet reported" entry for it.
     */
    fun addNewWalkHolder(currentWalkHolder: WalkHolder) {
        addSamplesSize(this.currentWalkHolder!!.walkNumber, this.currentWalkHolder!!.sampleSize)
        this.currentWalkHolder = currentWalkHolder
        booleanWalksList.add(false)
    }

    /**
     * Shows a dialog asking the tester to choose how many walk types (2 or
     * 4) should be recorded for the current walk number, then applies that
     * choice to the current [WalkHolder].
     */
    fun setWalkTypeAmount(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.walk_type_amount_dialog)
        dialog.setCancelable(false)
        val title: TextView = dialog.findViewById(R.id.wtamt_title)
        val rd2: RadioButton = dialog.findViewById(R.id.rd_2)
        val rd4: RadioButton = dialog.findViewById(R.id.rd_4)
        val okButton: AppCompatTextView = dialog.findViewById(R.id.okButton)

        title.text = "Select Amount Of Walk Types To Record For Walk #${currentWalkHolder.walkNumber}"

        okButton.setOnClickListener {
            if (rd2.isChecked) {
                currentWalkHolder = currentWalkHolder.setAllowedWalkTypes(2)
                dialog.dismiss()
            }
            if (rd4.isChecked) {
                currentWalkHolder = currentWalkHolder.setAllowedWalkTypes(4)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    /**
     * Swaps in a brand-new WalkHolder for the same walk number (used when
     * restarting a walk number from scratch), discarding any previously
     * recorded sample-size total for that walk number.
     */
    fun replaceWalkHolder(currentWalkHolder: WalkHolder) {
        sampleSizeMap.remove(currentWalkHolder.walkNumber)
        this.currentWalkHolder = currentWalkHolder
    }

    /*
    fun getTotalSampleSize(): Int {
        var total = 0
        for (key in sampleSizeMap.keys) {
            total += sampleSizeMap[key]!!
        }
        return total
    }
    */

    /** Records the total sample size collected for a given walk number */
    fun addSamplesSize(walkNumber: Int, sampleSize: Int) {
        sampleSizeMap[walkNumber] = sampleSize
    }

    /** Builds a human-readable summary of this subject's info, used at the top of report.txt */
    fun printInfo(): String {
        return "Subject ID: $subjectID\nGender: ${gender}" +
                "\nAge: $age\nWeight: $weight" +
                "\nHeight(ft and inches): $heightFeet' " +
                "$heightInches''\n"
    }

    // HUGE DESIGN (DATA STRUCTURE) CHANGES TO DECREASE MEMORY USAGE
    /*
    fun addWalk(walk: Walk) {
        successfulWalks.add(walk)
    }

    fun getSuccessfulWalks(): LinkedList<Walk> = successfulWalks

    fun setSuccessfulWalks(successfulWalks: LinkedList<Walk>) {
        this.successfulWalks = successfulWalks
    }

    fun getReportedWalks(): LinkedList<Walk> = reportedWalks

    fun setReportedWalks(reportedWalks: LinkedList<Walk>) {
        this.reportedWalks = reportedWalks
    }

    fun clearWalkData() {
        successfulWalks.clear()
    }

    fun removeLastWalk(): Walk {
        return successfulWalks.removeLast()
    }
    */
}