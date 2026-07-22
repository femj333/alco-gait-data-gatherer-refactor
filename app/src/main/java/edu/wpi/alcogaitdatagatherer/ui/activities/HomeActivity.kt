package edu.wpi.alcogaitdatagatherer.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.preference.PreferenceManager
import com.box.androidsdk.content.BoxApiFile
import com.box.androidsdk.content.BoxApiFolder
import com.box.androidsdk.content.BoxConfig
import com.box.androidsdk.content.auth.BoxAuthentication
import com.box.androidsdk.content.models.BoxSession
import edu.wpi.alcogaitdatagatherer.R
import edu.wpi.alcogaitdatagatherer.ui.home.HomeScreen

class HomeActivity : ComponentActivity(), BoxAuthentication.AuthListener {

    private var boxSession: BoxSession? = null
    private var mFolderApi: BoxApiFolder? = null
    private var mFileApi: BoxApiFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isBoxPreferenceEnabled()) {
            configureBoxClient()
            initializeBoxSession()
        }

        setContent {
            HomeScreen(
                onAddClick = {
                    startActivity(Intent(this, SurveyFormActivity::class.java))
                },
                onSyncClick = {
                    mFileApi?.let {
                        Toast.makeText(this, "Syncing with Box...", Toast.LENGTH_SHORT).show()
                    }
                },
                onSettingsClick = {
                    startActivity(Intent(this, SettingsActivity::class.java))
                },
                isBoxEnabled = isBoxPreferenceEnabled()
            )
        }
    }

    private fun isBoxPreferenceEnabled(): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        return sp.getBoolean(getString(R.string.box_integration_preference), false)
    }

    private fun configureBoxClient() {
        BoxConfig.CLIENT_ID = CLIENT_ID
        BoxConfig.CLIENT_SECRET = CLIENT_SECRET
        BoxConfig.REDIRECT_URL = REDIRECT_URI
    }

    private fun initializeBoxSession() {
        boxSession = BoxSession(this)
        boxSession?.setSessionAuthListener(this)
        boxSession?.authenticate(this)
    }

    override fun onAuthCreated(info: BoxAuthentication.BoxAuthenticationInfo) {
        mFolderApi = BoxApiFolder(boxSession)
        mFileApi = BoxApiFile(boxSession)
    }

    override fun onAuthFailure(info: BoxAuthentication.BoxAuthenticationInfo?, ex: Exception?) {}

    override fun onLoggedOut(info: BoxAuthentication.BoxAuthenticationInfo?, ex: Exception?) {
        initializeBoxSession()
    }

    override fun onRefreshed(info: BoxAuthentication.BoxAuthenticationInfo) {}

    companion object {
        @JvmField
        val FILE_SHOULD_START_WITH = "ID_"

        private const val CLIENT_ID = "jqkqfexx2sdtk8fd145dwfexr851drh3"
        private const val CLIENT_SECRET = "NjaaG4NrOjCFFpvRn2gSFr5YtEuiReCl"
        private const val REDIRECT_URI = "https://localhost"
    }
}
