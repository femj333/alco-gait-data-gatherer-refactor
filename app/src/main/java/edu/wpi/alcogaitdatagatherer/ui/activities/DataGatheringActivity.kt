package edu.wpi.alcogaitdatagatherer.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import edu.wpi.alcogaitdatagatherer.R
import edu.wpi.alcogaitdatagatherer.models.SensorRecorder
import edu.wpi.alcogaitdatagatherer.models.TestSubject
import edu.wpi.alcogaitdatagatherer.data.SurveyRepository
import edu.wpi.alcogaitdatagatherer.ui.datagathering.DataGatheringScreen
import edu.wpi.alcogaitdatagatherer.ui.datagathering.DataGatheringViewModel
import edu.wpi.alcogaitdatagatherer.ui.fragments.WalkReportFragment
import edu.wpi.alcogaitdatagatherercommon.CommonCode
import edu.wpi.alcogaitdatagatherercommon.WalkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class DataGatheringActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener, WalkReportFragment.ReportFragmentListener {

    private val viewModel: DataGatheringViewModel by viewModels()
    private var sensorRecorder: SensorRecorder? = null
    private lateinit var testSubject: TestSubject
    private var mFolderName: String? = null
    private var countDownTimer: CountDownTimer? = null
    
    private var isReceivingFromWatch = false
    private var samplesReceivedFromWatch = 0
    private var totalSampleSizeInWearable = 0

    private val sensorRecorderListener = object : SensorRecorder.SensorRecorderListener {
        override fun onWalkNumberUpdate(info: String) {
            viewModel.updateWalkNumberInfo(info)
        }
        override fun onWalkLogUpdate(log: String) {
            viewModel.updateWalkLog(log)
        }
        override fun onRecordingStatusChanged(isRecording: Boolean) {
            viewModel.setRecording(isRecording)
        }
        override fun onStartButtonTextUpdate(text: String) {
            // Can be used to update button text if needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        testSubject = intent.getSerializableExtra("test_subject") as TestSubject

        prepareStoragePath()
        
        sensorRecorder = SensorRecorder(this, mFolderName, testSubject, sensorRecorderListener)
        setupTimer()

        setContent {
            DataGatheringScreen(
                viewModel = viewModel,
                onBackClick = { onBackPressed() },
                onRefreshWatchClick = {
                    if (isWearablePreferenceEnabled()) {
                        notifyWearableActivity(
                            CommonCode.WEAR_MESSAGE_PATH,
                            CommonCode.REFRESH_CONNECTION
                        )
                        checkWearableReachability()
                    }
                },
                onStartClick = { handleStartClick() },
                onStopClick = { handleStopClick() },
                onRedoSetClick = { handleRedoSet() },
                onRedoLastClick = { handleRedoLast() },
                onFinishClick = { handleFinishClick() },
                isWearableEnabled = isWearablePreferenceEnabled()
            )
        }
    }

    private fun handleStartClick() {
        val bacStr = viewModel.uiState.value.bacInput
        if (bacStr.trim().isEmpty()) {
            Toast.makeText(this, "Please enter BAC data.", Toast.LENGTH_SHORT).show()
            return
        }
        if (viewModel.uiState.value.bacError != null) return

        startRecording(bacStr.trim())
    }

    private fun handleStopClick() {
        stopRecording()
    }

    private fun handleRedoSet() {
        sensorRecorder?.restartCurrentWalkNumber(this) {
            viewModel.updateBacInput("")
            viewModel.updateCountdown(CommonCode.RECORD_TIME_IN_SECONDS)
            if (isWearablePreferenceEnabled()) {
                notifyWearableActivity(CommonCode.WEAR_MESSAGE_PATH, CommonCode.RESTART)
            }
        }
    }

    private fun handleRedoLast() {
        if (sensorRecorder?.testSubject?.currentWalkHolder?.hasWalk(WalkType.NORMAL) == true) {
            sensorRecorder?.reDoWalk(this) {
                viewModel.updateBacInput(sensorRecorder?.previousBAC?.toString() ?: "")
                if (isWearablePreferenceEnabled()) {
                    notifyWearableActivity(CommonCode.REDO_PREVIOUS_WALK_PATH, sensorRecorder?.currentWalkType?.toNoSpaceString() ?: "")
                }
            }
        } else {
            Toast.makeText(this, "You have not recorded any data for the current walk number.", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleFinishClick() {
        AlertDialog.Builder(this)
            .setTitle("Finish Survey")
            .setMessage("Are you sure you want to finish survey? Unfinished walk number data will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Report Walks")
                    .setMessage("Would you like to submit a report about any of the walks?")
                    .setPositiveButton("Yes") { _, _ ->
                        val walkReportFragment = WalkReportFragment()
                        val bundle = Bundle().apply {
                            putSerializable(TB_FOR_WALK_REPORT, sensorRecorder?.testSubject)
                        }
                        walkReportFragment.arguments = bundle
                        walkReportFragment.show(supportFragmentManager, "FRAGMENT")
                    }
                    .setNegativeButton("No") { _, _ ->
                        returnToHomeScreen()
                    }
                    .show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun prepareStoragePath() {
        val folderName = "ID_${testSubject.subjectID.trim()}"
        mFolderName = File(SurveyRepository.getSurveyDirectory(this), folderName)
            .apply {
                if (!exists() && !mkdirs()) {
                    throw IllegalStateException("Unable to create survey directory")
                }
            }
            .absolutePath
    }

    private fun setupTimer() {
        countDownTimer = object : CountDownTimer((CommonCode.RECORD_TIME_IN_SECONDS * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                viewModel.updateCountdown((millisUntilFinished / 1000).toInt())
            }

            override fun onFinish() {
                val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen1.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, 1000)
                val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(1000)
                stopRecording()
            }
        }
    }

    private fun startRecording(bacInputText: String) {
        if (sensorRecorder?.isRecording == false) {
            if (sensorRecorder?.testSubject?.currentWalkHolder?.nextWalkType == null) {
                sensorRecorder?.prepareWalkStorage()
                requestSave()
                if (isWearablePreferenceEnabled()) {
                    viewModel.setShowProgressBar(true, "Waiting For Watch Data")
                    sensorRecorder?.setActivity(this)
                    notifyWearableActivity(CommonCode.WEAR_MESSAGE_PATH, CommonCode.SAVE_WALKS)
                }
            } else {
                var formattedBac = bacInputText
                if (formattedBac.startsWith(".")) {
                    formattedBac = "0$formattedBac"
                }
                val bac = formattedBac.toDouble()
                viewModel.updateBacInput(bac.toString())
                
                samplesReceivedFromWatch = 0
                totalSampleSizeInWearable = 0

                sensorRecorder?.startRecording(bac)

                if (isWearablePreferenceEnabled()) {
                    notifyWearableActivity(CommonCode.START_RECORDING_PATH, sensorRecorder?.currentWalkType?.toNoSpaceString() ?: "")
                }

                countDownTimer?.start()
            }
        }
    }

    private fun stopRecording() {
        if (sensorRecorder?.isRecording == true) {
            if (isWearablePreferenceEnabled()) {
                if (isReceivingFromWatch) return
                notifyWearableActivity(CommonCode.WEAR_MESSAGE_PATH, CommonCode.STOP_RECORDING)
                viewModel.setShowProgressBar(true)
            }

            countDownTimer?.cancel()
            setupTimer()
            
            val finished = sensorRecorder?.stopRecording() ?: false
            if (isWearablePreferenceEnabled()) {
                viewModel.setShowProgressBar(false)
            }
        }
    }

    private fun requestSave() {
        // App-specific storage is already available; no legacy storage permission is needed.
        sensorRecorder?.saveCurrentWalkNumberToCSV(null)
    }

    fun notifyWearableActivity(path: String, text: String) {
        if (isWearablePreferenceEnabled()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val nodes = getNodes()
                for (nodeID in nodes) {
                    Wearable.getMessageClient(this@DataGatheringActivity).sendMessage(nodeID, path, text.toByteArray())
                }
            }
        }
    }

    private suspend fun getNodes(): Collection<String> = withContext(Dispatchers.IO) {
        val results = HashSet<String>()
        try {
            val nodes = Tasks.await(Wearable.getNodeClient(this@DataGatheringActivity).connectedNodes)
            for (node in nodes) {
                results.add(node.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        runOnUiThread {
            if (messageEvent.path.equals(CommonCode.WEAR_MESSAGE_PATH, ignoreCase = true)) {
                when (String(messageEvent.data)) {
                    CommonCode.WEARABLE_DISCONNECTED -> Toast.makeText(this, "WEARABLE DISCONNECTED", Toast.LENGTH_SHORT).show()
                    CommonCode.CHECK_IF_APP_OPEN -> {
                        notifyWearableActivity(CommonCode.WEAR_MESSAGE_PATH, CommonCode.APP_OPEN_ACK)
                        viewModel.setShowProgressBar(false)
                        Toast.makeText(this, "CONNECTION REQUEST RECEIVED", Toast.LENGTH_SHORT).show()
                    }
                    CommonCode.APP_OPEN_ACK -> {
                        viewModel.setShowProgressBar(false)
                        Toast.makeText(this, "CONNECTION CONFIRMED", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (messageEvent.path.equals(CommonCode.STOP_RECORDING_PATH, ignoreCase = true)) {
                totalSampleSizeInWearable = String(messageEvent.data).toInt()
                stopRecording()
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        runOnUiThread {
            if (capabilityInfo.nodes.size > 0) {
                Toast.makeText(this, "WATCH IS CONNECTED", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "WATCH IS DISCONNECTED", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorRecorder?.let {
            if (it.isRecording) it.registerListeners()
        }
        if (isWearablePreferenceEnabled()) {
            checkWearableReachability()
            Wearable.getCapabilityClient(this).addListener(this, CommonCode.WEAR_DISCOVERY_NAME)
            Wearable.getMessageClient(this).addListener(this)
            Wearable.getChannelClient(this).registerChannelCallback(sensorRecorder!!)
            notifyWearableActivity(CommonCode.WEAR_HOME_ACTIVITY_PATH, CommonCode.OPEN_APP)
        }
    }

    override fun onPause() {
        sensorRecorder?.let {
            if (it.isRecording) it.unregisterListeners()
        }
        if (isWearablePreferenceEnabled()) {
            notifyWearableActivity(CommonCode.WEAR_MESSAGE_PATH, CommonCode.WEARABLE_DISCONNECTED)
            Wearable.getCapabilityClient(this).removeListener(this, CommonCode.WEAR_DISCOVERY_NAME)
            Wearable.getMessageClient(this).removeListener(this)
            Wearable.getChannelClient(this).unregisterChannelCallback(sensorRecorder!!)
        }
        super.onPause()
    }

    private fun checkWearableReachability() {
        viewModel.setShowProgressBar(true, "Searching for watch...")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val capabilityInfo = Tasks.await(
                    Wearable.getCapabilityClient(this@DataGatheringActivity).getCapability(
                        CommonCode.WEAR_DISCOVERY_NAME, CapabilityClient.FILTER_REACHABLE
                    )
                )
                withContext(Dispatchers.Main) {
                    if (capabilityInfo.nodes.size > 0) {
                        viewModel.setShowProgressBar(true, "Awaiting Watch Response")
                        notifyWearableActivity(CommonCode.WEAR_MESSAGE_PATH, CommonCode.CHECK_IF_APP_OPEN)
                    } else {
                        viewModel.setShowProgressBar(false)
                        showWatchReachabilityError()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    viewModel.setShowProgressBar(false)
                    showWatchReachabilityError()
                }
            }
        }
    }

    private fun showWatchReachabilityError() {
        AlertDialog.Builder(this)
            .setTitle("Watch Is Not Reachable")
            .setMessage(R.string.watch_reachability_error_dialog)
            .setPositiveButton("OK", null)
            .show()
    }

    fun startProgressBar() {
        viewModel.setShowProgressBar(true)
    }

    fun stopProgressBar() {
        viewModel.setShowProgressBar(false)
    }

    fun updateProgressBarMessage(message: String) {
        viewModel.setShowProgressBar(true, message)
    }

    override fun submitReport(checkBoxStates: LinkedList<Boolean>, reportMessage: String) {
        sensorRecorder?.let {
            it.testSubject.booleanWalksList = checkBoxStates
            it.testSubject.reportMessage = reportMessage
            it.saveWalkReport()
        }
        returnToHomeScreen()
    }

    private fun returnToHomeScreen() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Return To Subject Information Form?")
            .setMessage("Are you sure you want to return to the form? Data for the latest walk number will be lost.")
            .setPositiveButton("Yes") { _, _ -> super.onBackPressed() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun isWearablePreferenceEnabled(): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        return sp.getBoolean(getString(R.string.wear_collection_preference), false)
    }

    companion object {
        const val TB_FOR_WALK_REPORT = "walk_report"
    }
}
