package edu.wpi.alcogaitdatagatherer.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import edu.wpi.alcogaitdatagatherer.models.TestSubject

class SurveyFormActivity : ComponentActivity() {

    private val viewModel: SurveyFormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SurveyFormScreen(
                viewModel = viewModel,
                onBackClick = { finish() },
                onSubmitClick = {
                    val testSubject = viewModel.createTestSubject()
                    if (testSubject != null) {
                        val intent = Intent(this, DataGatheringActivity::class.java).apply {
                            putExtra("test_subject", testSubject)
                        }
                        startActivity(intent)
                    }
                }
            )
        }
    }
}
