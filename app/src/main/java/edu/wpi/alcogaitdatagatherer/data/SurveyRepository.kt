package edu.wpi.alcogaitdatagatherer.data

import android.os.Environment
import java.io.File

class SurveyRepository {
    companion object {
        const val FILE_SHOULD_START_WITH = "ID_"
    }

    fun getSurveyFiles(): List<File> {
        val baseDir = "${Environment.getExternalStorageDirectory().absolutePath}/AlcoGaitDataGatherer/"
        val alcoGaitDirectory = File(baseDir)
        
        if (!alcoGaitDirectory.exists()) {
            alcoGaitDirectory.mkdirs()
        }

        val allFilesFromDir = alcoGaitDirectory.listFiles() ?: return emptyList()

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
