package com.duke.orca.android.kotlin.biblelockscreen.application

import android.content.Context
import android.content.Intent
import com.duke.orca.android.kotlin.biblelockscreen.R

fun shareApplication(context: Context) {
    val intent = Intent(Intent.ACTION_SEND)
    val text = "https://play.google.com/store/apps/details?id=${context.packageName}"

    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, text)

    Intent.createChooser(intent, context.getString(R.string.share_the_app)).also {
        it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(it)
    }
}

fun Any?.isNull() = this == null
fun Any?.isNotNull() = isNull().not()

fun Int?.isZero() = this == 0
fun Int?.isNonZero() = isZero().not()

fun intRange(from: Int, to: Int) = from..to
fun IntRange.toStringArray() = map { it.toString() }.toTypedArray()

fun String.`is`(other: String) = this == other
fun String.not(other: String) = `is`(other).not()