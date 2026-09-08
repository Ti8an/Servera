package com.tivanstudio.servera.data.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Runs ML Kit text recognition over the camera preview and reports each frame line by line.
 *
 * Lines rather than one blob of text because a caller needs the coordinates: to keep only what
 * the camera is actually aimed at, and to point at the line a value was taken from.
 *
 * Nothing is parsed here -- what a line means is a domain question. Neither the text nor the
 * coordinates are logged at any level: they carry the user's server addresses and logins.
 */
class TextRecognitionAnalyzer(
    private val onFrameRecognized: (RecognizedFrame) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // Read off the proxy up front: the listeners below run after it has been closed, and
        // touching it there throws.
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val imageWidth = imageProxy.width
        val imageHeight = imageProxy.height

        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val lines = result.textBlocks
                    .flatMap { block -> block.lines }
                    // A line with no box cannot be placed on the screen or tested against the
                    // aimed-at area, so it is of no use to the caller.
                    .mapNotNull { line -> line.boundingBox?.let { RecognizedLine(line.text, it) } }
                // Reported even when empty: "nothing on this frame" is a state the caller shows.
                onFrameRecognized(
                    RecognizedFrame(lines, rotationDegrees, imageWidth, imageHeight)
                )
            }
            // Closed on completion rather than on success: a frame that stays open after a
            // failed recognition is never returned to the pipeline, and analysis stops for
            // good with no error anywhere.
            .addOnCompleteListener { imageProxy.close() }
    }

    /** Releases the ML Kit client. The scanner screen calls this when it is destroyed. */
    fun close() {
        recognizer.close()
    }
}
