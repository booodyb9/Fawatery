package com.example.export

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object InvoiceQr {
    fun bitmap(payload: String, pixels: Int = 512): Bitmap {
        require(payload.isNotBlank() && pixels >= 128)
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, pixels, pixels, mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 4,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        ))
        val image = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        val colors = IntArray(matrix.width * matrix.height) { index ->
            if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
        image.setPixels(colors, 0, matrix.width, 0, 0, matrix.width, matrix.height)
        return image
    }
}
