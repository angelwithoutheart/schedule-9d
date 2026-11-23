package ru.school9d.schedule.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCompressor {

    private const val MAX_SIDE = 1600
    private const val JPEG_QUALITY = 80

    suspend fun toDataUrl(resolver: ContentResolver, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(resolver, uri)
        try {
            val output = ByteArrayOutputStream()
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            if (!compressed) {
                throw IOException("Не удалось сжать изображение")
            }
            val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun decodeScaled(resolver: ContentResolver, uri: Uri): Bitmap {
        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeModern(resolver, uri)
        } else {
            decodeLegacy(resolver, uri)
        }
        return scaleDown(decoded, MAX_SIDE)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeModern(resolver: ContentResolver, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(resolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val maxDimension = max(info.size.width, info.size.height)
            if (maxDimension > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / maxDimension
                val width = max(1, (info.size.width * scale).roundToInt())
                val height = max(1, (info.size.height * scale).roundToInt())
                decoder.setTargetSize(width, height)
            }
        }
    }

    private fun decodeLegacy(resolver: ContentResolver, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Не удалось открыть изображение")

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Не удалось прочитать изображение")
        }

        var sampleSize = 1
        val maxDimension = max(bounds.outWidth, bounds.outHeight)
        while (maxDimension / (sampleSize * 2) >= MAX_SIDE) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IOException("Не удалось декодировать изображение")

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { rotated ->
                if (rotated !== bitmap) bitmap.recycle()
            }
    }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val maxDimension = max(bitmap.width, bitmap.height)
        if (maxDimension <= maxSide) return bitmap
        val scale = maxSide.toFloat() / maxDimension
        val width = max(1, (bitmap.width * scale).roundToInt())
        val height = max(1, (bitmap.height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }
}
