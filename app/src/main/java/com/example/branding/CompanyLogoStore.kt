package com.example.branding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.R
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

/** App-owned immutable files keep issued invoice logos independent of gallery access and profile edits. */
object CompanyLogoStore {
    private const val MAX_INPUT_BYTES = 10 * 1024 * 1024
    private const val LOGO_PIXELS = 256

    fun isValidFileName(name: String): Boolean = Regex("logo_[0-9a-f]{64}\\.png").matches(name)

    fun importFromUri(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var count = input.read(buffer)
            while (count != -1) {
                require(output.size() + count <= MAX_INPUT_BYTES) { "اختر صورة أصغر من 10 ميجابايت" }
                output.write(buffer, 0, count)
                count = input.read(buffer)
            }
            output.toByteArray()
        } ?: error("تعذر فتح صورة الشعار")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "اختر صورة صالحة مثل PNG أو JPEG" }
        bounds.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        bounds.inJustDecodeBounds = false
        var bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)) { "تعذر قراءة صورة الشعار" }
        try {
            val orientation = runCatching {
                ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val transform = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                }
            }
            if (!transform.isIdentity) {
                val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true)
                if (oriented !== bitmap) { bitmap.recycle(); bitmap = oriented }
            }
            val scaled = fit(bitmap)
            if (scaled !== bitmap) { bitmap.recycle(); bitmap = scaled }
            val normalized = ByteArrayOutputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "تعذر تجهيز الشعار" }
                out.toByteArray()
            }
            val hash = MessageDigest.getInstance("SHA-256").digest(normalized)
                .joinToString("") { String.format(Locale.US, "%02x", it.toInt() and 255) }
            val name = "logo_${hash}.png"
            val directory = File(context.filesDir, "company_logos")
            check(directory.isDirectory || directory.mkdirs()) { "تعذر حفظ الشعار" }
            val target = File(directory, name)
            if (!target.isFile) {
                val temporary = File.createTempFile("logo-", ".tmp", directory)
                try {
                    FileOutputStream(temporary).use { out ->
                        out.write(normalized); out.flush(); out.fd.sync()
                    }
                    check(temporary.renameTo(target) || target.isFile) { "تعذر حفظ الشعار" }
                } finally { temporary.delete() }
            }
            return name
        } finally { bitmap.recycle() }
    }

    /** Caller owns the bitmap. Never substitute a different logo for an unavailable custom file. */
    fun load(context: Context, fileName: String?): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        val file = fileName?.let {
            require(isValidFileName(it)) { "اسم ملف الشعار غير صالح" }
            File(File(context.filesDir, "company_logos"), it).also { file ->
                check(file.isFile) { "ملف الشعار غير متاح؛ اختر الشعار مجددًا من إعدادات المنشأة" }
            }
        }
        if (file == null) BitmapFactory.decodeResource(context.resources, R.drawable.fawraty_logo, bounds)
        else BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "تعذر قراءة صورة الشعار" }
        bounds.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        bounds.inJustDecodeBounds = false
        val original = requireNotNull(if (file == null)
            BitmapFactory.decodeResource(context.resources, R.drawable.fawraty_logo, bounds)
            else BitmapFactory.decodeFile(file.path, bounds)) { "تعذر قراءة صورة الشعار" }
        val scaled = fit(original)
        if (scaled !== original) original.recycle()
        return scaled
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > 512) sample *= 2
        return sample
    }

    private fun fit(bitmap: Bitmap): Bitmap {
        val scale = minOf(1f, LOGO_PIXELS.toFloat() / maxOf(bitmap.width, bitmap.height))
        if (scale == 1f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, maxOf(1, (bitmap.width * scale).toInt()),
            maxOf(1, (bitmap.height * scale).toInt()), true)
    }
}
