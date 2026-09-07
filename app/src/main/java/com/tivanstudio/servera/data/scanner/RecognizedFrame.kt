package com.tivanstudio.servera.data.scanner

import android.graphics.Rect

/** One recognized line of text and where it sits in the frame. */
data class RecognizedLine(
    val text: String,
    /**
     * In the coordinates of the upright frame -- see [RecognizedFrame.uprightWidth].
     *
     * ML Kit is handed the rotation along with the image and reports boxes already turned the
     * right way up, so no rotation is applied to this rect anywhere downstream. If that ever
     * stops holding, the symptom is unmistakable on a portrait phone: the highlight lands at
     * right angles to the text, and the aimed-at area filters a vertical band instead of a
     * horizontal one.
     */
    val box: Rect
)

/**
 * What one camera frame contained.
 *
 * [imageWidth] and [imageHeight] are the camera buffer as delivered, before [rotationDegrees]
 * is applied. The buffer is landscape on essentially every phone, so on a portrait screen those
 * two are the wrong way round for anything the user sees; [uprightWidth] and [uprightHeight]
 * are the frame as it appears on screen, and are the space [RecognizedLine.box] is expressed in.
 */
data class RecognizedFrame(
    val lines: List<RecognizedLine>,
    val rotationDegrees: Int,
    val imageWidth: Int,
    val imageHeight: Int
) {
    private val isQuarterTurned: Boolean get() = rotationDegrees == 90 || rotationDegrees == 270

    /** Width of the frame the right way up: the buffer dimensions swap at 90 and 270 degrees. */
    val uprightWidth: Int get() = if (isQuarterTurned) imageHeight else imageWidth

    /** Height of the frame the right way up. */
    val uprightHeight: Int get() = if (isQuarterTurned) imageWidth else imageHeight
}
