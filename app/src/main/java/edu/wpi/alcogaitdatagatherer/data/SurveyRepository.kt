package edu.wpi.alcogaitdatagatherer.data

import android.content.Context
import java.io.File

/** Provides access to surveys stored in the app-specific external-files directory. */
class SurveyRepository(private val context: Context) {
    companion object {
        const val FILE_SHOULD_START_WITH = "ID_"
        private const val SURVEY_DIRECTORY_NAME = "AlcoGaitDataGatherer"

        /**
         * Returns the shared survey directory used by both the form and recorder.
         * App-specific external storage needs no runtime storage permission on Android 11+.
         */
        fun getSurveyDirectory(context: Context): File {
            val appStorage = context.getExternalFilesDir(null) ?: context.filesDir
            return File(appStorage, SURVEY_DIRECTORY_NAME).apply {
                if (!exists() && !mkdirs()) {
                    throw IllegalStateException("Unable to create survey storage directory")
                }
            }
        }
    }

    fun getSurveyFiles(): List<File> {
        val allFilesFromDir = getSurveyDirectory(context).listFiles() ?: return emptyList()

        return allFilesFromDir
            .filter { file ->
                val fileName = file.name
                fileName.length == 6 && file.isDirectory() && fileName.startsWith(FILE_SHOULD_START_WITH)
            }
            .sortedByDescending { it.lastModified() }
    }

    fun getExistingIds(): List<String> {
        return getSurveyFiles().map { file ->
            file.name.removePrefix(FILE_SHOULD_START_WITH).replaceFirst("^0+(?!$)".toRegex(), "")
        }
    }
}
