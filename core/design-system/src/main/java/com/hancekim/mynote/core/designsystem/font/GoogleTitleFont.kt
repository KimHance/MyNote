package com.hancekim.mynote.core.designsystem.font

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.isAvailableOnDevice
import com.hancekim.mynote.core.designsystem.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * GMS 글꼴 제공자에서 제목 글꼴(700 하나)을 받는 곳.
 *
 * @param fontName 받을 글꼴 이름. null 이면 Gothic A1.
 */
fun googleTitleFontSource(
    context: Context,
    fontName: String? = null,
): TitleFontSource = GoogleTitleFontSource(context.applicationContext, fontName ?: GOTHIC_A1)

private class GoogleTitleFontSource(
    private val context: Context,
    fontName: String,
) : TitleFontSource {
    private val font = Font(GoogleFont(fontName), GmsFontProvider, FontWeight.W700) as AndroidFont

    override suspend fun isAvailable(): Boolean =
        withContext(Dispatchers.IO) {
            GmsFontProvider.isAvailableOnDevice(context)
        }

    // 제공자는 부른 스레드의 루퍼로 답한다. 첫 화면을 짓느라 바쁜 메인 대신 따로 둔 스레드로 받는다.
    override suspend fun load(): FontFamily =
        withContext(FontLooper) {
            val typeface = checkNotNull(font.typefaceLoader.awaitLoad(context, font)) { "글꼴이 비어 있습니다: $font" }
            FontFamily(typeface)
        }
}

private val FontLooper: CoroutineDispatcher by lazy {
    Handler(HandlerThread("title-font").apply { start() }.looper).asCoroutineDispatcher()
}

private val GmsFontProvider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.ds_com_google_android_gms_fonts_certs,
    )

private const val GOTHIC_A1 = "Gothic A1"
