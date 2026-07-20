package edu.wpi.alcogaitdatagatherer.interfaces

/**
 * Receives integer upload-progress updates from a Box upload task.
 *
 * Implementations update their UI with the supplied percentage.
 */
interface BoxUploadProgressListener {
    fun onProgressUpdate(progress: Int)
}
