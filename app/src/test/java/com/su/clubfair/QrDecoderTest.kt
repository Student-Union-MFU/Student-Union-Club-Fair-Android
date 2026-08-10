package com.su.clubfair

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.su.clubfair.ui.scan.QrDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Drives the scanner's real decode path on the JVM.
 *
 * Pointing a phone at a poster is the only other way to find out whether the
 * scanner works, and it isn't repeatable. [QrDecoder] takes a packed luminance
 * buffer precisely so a generated code can stand in for a camera frame here.
 */
class QrDecoderTest {

    @Test
    fun `reads a booth code`() {
        val payload = "https://su.mfu.ac.th/fair/booth/07"
        assertEquals(payload, QrDecoder.decode(render(payload), SIZE, SIZE))
    }

    @Test
    fun `reads a code that is light on dark`() {
        // The pass screen found that ZXing will not read an inverted code, so the
        // decoder tries both polarities. This is the test for the second pass.
        assertEquals("BOOTH-12", QrDecoder.decode(render("BOOTH-12", invert = true), SIZE, SIZE))
    }

    @Test
    fun `returns null on a frame with no code`() {
        assertNull(QrDecoder.decode(ByteArray(SIZE * SIZE) { -1 }, SIZE, SIZE))
    }

    /** Encodes [text] into a packed grey-scale buffer, the shape a camera hands over. */
    private fun render(text: String, invert: Boolean = false): ByteArray {
        val matrix = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            SIZE,
            SIZE,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 2,
            ),
        )
        val dark = if (invert) LIGHT else DARK
        val light = if (invert) DARK else LIGHT
        return ByteArray(SIZE * SIZE) { i ->
            if (matrix.get(i % SIZE, i / SIZE)) dark else light
        }
    }

    private companion object {
        const val SIZE = 300
        const val DARK: Byte = 0
        const val LIGHT: Byte = -1 // 0xFF as a signed byte
    }
}
