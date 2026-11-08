package com.duke.orca.android.kotlin.biblelockscreen.bible

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.datamodels.Content
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Bible
import java.io.File

// 구절을 앱 아이콘과 같은 남색 카드 이미지로 만들어 공유한다.
private const val WIDTH = 1080
private const val HEIGHT = 1350
private const val PADDING = 120f
private const val MAX_TEXT_SIZE = 64f
private const val MIN_TEXT_SIZE = 30f

private const val TOP_COLOR = 0xFF10284A.toInt()
private const val BOTTOM_COLOR = 0xFF225692.toInt()
private const val GOLD = 0xFFE3C77A.toInt()

fun shareAsImage(context: Context, bible: Bible, content: Content) {
    val file = File(context.cacheDir, "shared").run {
        mkdirs()
        File(this, "verse.png")
    }

    file.outputStream().use {
        drawVerse(context, bible, content).compress(Bitmap.CompressFormat.PNG, 100, it)
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    Intent.createChooser(intent, context.getString(R.string.share_as_image)).apply {
        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(this)
    }
}

private fun drawVerse(context: Context, bible: Bible, content: Content): Bitmap {
    val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val typeface = ResourcesCompat.getFont(context, R.font.nanum_myeongjo_regular)
    val boldTypeface = ResourcesCompat.getFont(context, R.font.nanum_myeongjo_bold)

    Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), TOP_COLOR, BOTTOM_COLOR, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), this)
    }

    val textWidth = (WIDTH - PADDING * 2).toInt()
    val footerTop = HEIGHT - PADDING
    val maxTextHeight = footerTop - PADDING * 2 - 260

    // 구절이 길면 글자를 줄여 카드 안에 맞춘다.
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        this.typeface = typeface
    }
    var textSize = MAX_TEXT_SIZE
    var layout: StaticLayout

    while (true) {
        textPaint.textSize = textSize
        layout = staticLayout(content.word, textPaint, textWidth, 1.5f)

        if (layout.height <= maxTextHeight || textSize <= MIN_TEXT_SIZE) break

        textSize -= 2f
    }

    val referencePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GOLD
        this.typeface = boldTypeface
        this.textSize = 40f
    }
    val reference = staticLayout(
        "${bible.name(content.book)}  ${content.chapter} : ${content.verse}",
        referencePaint,
        textWidth,
        1f
    )

    val ornament = 72f
    val gap = 48f
    val blockHeight = ornament + gap + layout.height + gap + reference.height
    var top = PADDING + (footerTop - PADDING - blockHeight) / 2

    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GOLD
        strokeWidth = 3f
        canvas.drawLine(WIDTH / 2f, top, WIDTH / 2f, top + ornament, this)
        canvas.drawLine(WIDTH / 2f - 22f, top + 22f, WIDTH / 2f + 22f, top + 22f, this)
    }

    top += ornament + gap
    canvas.save()
    canvas.translate(PADDING, top)
    layout.draw(canvas)
    canvas.restore()

    top += layout.height + gap
    canvas.save()
    canvas.translate(PADDING, top)
    reference.draw(canvas)
    canvas.restore()

    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99FFFFFF.toInt()
        this.typeface = typeface
        this.textSize = 30f
        textAlign = Paint.Align.CENTER
        canvas.drawText(context.getString(R.string.app_name), WIDTH / 2f, footerTop, this)
    }

    return bitmap
}

private fun staticLayout(text: CharSequence, paint: TextPaint, width: Int, spacing: Float): StaticLayout {
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(0f, spacing)
        .build()
}
