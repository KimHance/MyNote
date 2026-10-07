package com.hancekim.mynote.brand

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hancekim.mynote.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class LauncherMaskTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `BR-1 마스크 렌더에서 아치 꼭대기가 잘리지 않는다`() {
        for (id in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
            val icon = context.getDrawable(id) as AdaptiveIconDrawable
            val name = context.resources.getResourceEntryName(id)
            val color = render(icon.background, icon.foreground)
            val colorArch = render(icon.foreground).archPixels()
            val tintedMark = icon.monochrome!!.mutate().apply { setTint(THEMED_MARK) }
            val mono = render(ColorDrawable(THEMED_BACKGROUND), tintedMark)
            val monoArch = render(icon.monochrome!!).archPixels()
            assertTrue(colorArch.isNotEmpty() && monoArch.isNotEmpty())
            assertColor(ACCENT, color.unitPixel(ARCH_TOP_X, ARCH_TOP_Y))

            val pictures = listOf(Triple("color", color, colorArch), Triple("mono", mono, monoArch))
            for ((maskName, mask) in masks(icon)) {
                for ((layer, picture, arch) in pictures) {
                    val masked = picture.masked(mask)
                    save(masked, "$name-$layer-$maskName.png")
                    val cut = arch.count { (x, y) -> Color.alpha(masked.getPixel(x, y)) != 255 }
                    assertEquals("$name $layer $maskName 에서 잘린 아치 픽셀", 0, cut)
                }
            }
        }
    }

    private fun render(vararg layers: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(VISIBLE_PX, VISIBLE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = (LAYER_UNITS - VISIBLE_UNITS) / 2 * PX_PER_UNIT
        for (layer in layers) {
            layer.setBounds(-inset, -inset, VISIBLE_PX + inset, VISIBLE_PX + inset)
            layer.draw(canvas)
        }
        return bitmap
    }

    private fun Bitmap.archPixels(): List<Pair<Int, Int>> {
        val bottom = ((ARCH_BOTTOM_Y - INSET_UNITS) * PX_PER_UNIT).toInt()
        return (0 until bottom).flatMap { y ->
            (0 until width).filter { x -> Color.alpha(getPixel(x, y)) == 255 }.map { x -> x to y }
        }
    }

    private fun Bitmap.unitPixel(x: Float, y: Float): Int =
        getPixel(((x - INSET_UNITS) * PX_PER_UNIT).toInt(), ((y - INSET_UNITS) * PX_PER_UNIT).toInt())

    private fun Bitmap.masked(mask: Path): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            clipPath(mask)
            drawBitmap(this@masked, 0f, 0f, null)
        }
        return out
    }

    private fun masks(icon: AdaptiveIconDrawable): List<Pair<String, Path>> {
        val size = VISIBLE_PX.toFloat()
        val circle = Path().apply { addCircle(size / 2, size / 2, size / 2, Path.Direction.CW) }
        val corner = size * ROUNDED_CORNER
        val rounded = Path().apply { addRoundRect(RectF(0f, 0f, size, size), corner, corner, Path.Direction.CW) }
        icon.setBounds(0, 0, VISIBLE_PX, VISIBLE_PX)
        return listOf("circle" to circle, "rounded" to rounded, "device" to Path(icon.iconMask))
    }

    private fun assertColor(
        expected: Int,
        actual: Int,
    ) {
        val close =
            abs(Color.red(expected) - Color.red(actual)) <= COLOR_TOLERANCE &&
                abs(Color.green(expected) - Color.green(actual)) <= COLOR_TOLERANCE &&
                abs(Color.blue(expected) - Color.blue(actual)) <= COLOR_TOLERANCE
        assertTrue("아치 꼭대기 색 ${Integer.toHexString(actual)}", close)
    }

    private fun save(
        bitmap: Bitmap,
        fileName: String,
    ) {
        val dir = File(context.externalCacheDir, "brand-mask").apply { mkdirs() }
        File(dir, fileName).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val PX_PER_UNIT = 4
        const val LAYER_UNITS = 108
        const val VISIBLE_UNITS = 72
        const val INSET_UNITS = (LAYER_UNITS - VISIBLE_UNITS) / 2f
        const val VISIBLE_PX = VISIBLE_UNITS * PX_PER_UNIT
        const val ARCH_BOTTOM_Y = 45.566f
        const val ARCH_TOP_X = 41.5635f
        const val ARCH_TOP_Y = 31.6f
        const val ROUNDED_CORNER = 0.22f
        const val COLOR_TOLERANCE = 8
        val ACCENT = Color.rgb(0x6E, 0xA8, 0xFE)
        val THEMED_BACKGROUND = Color.rgb(0x2B, 0x2F, 0x38)
        val THEMED_MARK = Color.rgb(0xC4, 0xD7, 0xFF)
    }
}
