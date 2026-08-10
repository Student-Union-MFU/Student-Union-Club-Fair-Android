package com.su.clubfair.ui.scan

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Bridges CameraX frames to [QrDecoder], and reports the first code it reads.
 *
 * [onResult] fires at most once per analyzer: the screen wants a single scan,
 * not a stream of the same payload sixty times a second. Point it at a new booth
 * by rebuilding the analyzer.
 */
class QrAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {

    @Volatile
    private var done = false

    override fun analyze(image: ImageProxy) {
        // `use` matters more than usual here: an unclosed ImageProxy stalls the
        // whole pipeline at STRATEGY_KEEP_ONLY_LATEST, and the preview freezes
        // with no error anywhere.
        image.use {
            if (done) return@use
            val payload = try {
                QrDecoder.decode(packLuminance(it), it.width, it.height)
            } catch (e: RuntimeException) {
                // A malformed frame shouldn't take the camera down; drop it and
                // read the next one.
                Log.w(TAG, "frame skipped", e)
                null
            } ?: return@use

            done = true
            onResult(payload)
        }
    }

    private companion object {
        const val TAG = "QrAnalyzer"

        /**
         * Copies plane 0 (Y, i.e. brightness) into a tightly packed buffer.
         *
         * Camera rows are padded out to a hardware-friendly stride, so the plane
         * is usually *wider* than the image. Handing that to ZXing unpacked skews
         * every row by the padding and nothing ever decodes — the buffer looks
         * plausible, which is what makes it an awkward bug to spot.
         *
         * Rotation is ignored on purpose. A QR code carries its own orientation
         * markers, so ZXing reads one sideways as happily as upright, and rotating
         * every frame would be work spent for no gain.
         */
        fun packLuminance(image: ImageProxy): ByteArray {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val width = image.width
            val height = image.height
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            if (rowStride == width && pixelStride == 1) {
                return ByteArray(buffer.remaining()).also(buffer::get)
            }

            val out = ByteArray(width * height)
            val row = ByteArray(rowStride)
            for (y in 0 until height) {
                buffer.position(y * rowStride)
                val available = minOf(rowStride, buffer.remaining())
                buffer.get(row, 0, available)
                if (pixelStride == 1) {
                    row.copyInto(out, y * width, 0, width)
                } else {
                    for (x in 0 until width) out[y * width + x] = row[x * pixelStride]
                }
            }
            return out
        }
    }
}
