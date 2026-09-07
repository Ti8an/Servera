package com.tivanstudio.servera.data.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Runs ML Kit text recognition over the camera preview and reports the whole recognized text.
 *
 * Nothing is parsed here: what a block of text means is a domain question, and this class only
 * says what was on the frame. The text is never logged at any level -- it carries the user's
 * server addresses and logins.
 */
class TextRecognitionAnalyzer(
    private val onTextRecognized: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                if (result.text.isNotBlank()) onTextRecognized(result.text)
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
