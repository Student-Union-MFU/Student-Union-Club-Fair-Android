package com.su.clubfair.ui.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * Reads a QR code out of a frame of camera luminance.
 *
 * Deliberately free of any Android or CameraX type: it takes a packed grey-scale
 * buffer and returns the payload. That's what lets `QrDecoderTest` drive the real
 * decode path on the JVM with a generated code, instead of the scanner only ever
 * being testable by pointing a phone at a poster.
 *
 * ZXing is already a dependency for rendering the student's pass, so the scanner
 * adds no second barcode library.
 */
object QrDecoder {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                // QR only. Left open, the reader also hunts for a dozen 1D formats
                // in every frame, which costs time and invites a stray barcode on
                // a drinks can to register as a checkpoint.
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }

    /**
     * Returns the decoded payload, or null if there's no readable code in [frame].
     *
     * [frame] must be tightly packed — `width * height` bytes, one per pixel, row
     * major. Camera planes usually carry padding, so [QrAnalyzer] compacts them
     * before calling in.
     *
     * Synchronised because [MultiFormatReader] holds decode state between calls
     * and is not safe to share. Analysis runs on one thread today, so this never
     * contends; it's here so it stays correct if that ever changes.
     */
    @Synchronized
    fun decode(frame: ByteArray, width: Int, height: Int): String? {
        require(frame.size >= width * height) {
            "frame is ${frame.size} bytes, need ${width * height} for ${width}x$height"
        }
        val source = PlanarYUVLuminanceSource(
            frame, width, height,
            0, 0, width, height,
            false,
        )
        // Try the frame as-is, then inverted. The pass screen learned the hard way
        // that ZXing will not read a light-on-dark code, and booth printouts are
        // not guaranteed to be dark-on-light — the second pass costs one binarise
        // on frames that were going to fail anyway.
        return read(source) ?: read(source.invert())
    }

    private fun read(source: LuminanceSource): String? = try {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))?.text
    } catch (_: NotFoundException) {
        // The overwhelmingly common case: this frame had no code in it.
        null
    } finally {
        reader.reset()
    }
}
