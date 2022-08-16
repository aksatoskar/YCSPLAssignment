package com.aksatoskar.ycsplassignment.util

import android.app.Activity
import android.graphics.Rect
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlin.math.abs

fun View.showSnackbar(msgId: Int, length: Int) {
    showSnackbar(context.getString(msgId), length)
}

fun View.showSnackbar(msg: String, length: Int) {
    showSnackbar(msg, length, null, {})
}

fun View.showSnackbar(msgId: Int, length: Int, actionMessageId: Int) {
    showSnackbar(msgId, length, actionMessageId) {}
}

fun View.showSnackbar(
    msgId: Int,
    length: Int,
    actionMessageId: Int,
    action: (View) -> Unit
) {
    showSnackbar(context.getString(msgId), length, context.getString(actionMessageId), action)
}

fun View.showSnackbar(
    msg: String,
    length: Int,
    actionMessage: CharSequence?,
    action: (View) -> Unit
) {
    val snackbar = Snackbar.make(this, msg, length)
    if (actionMessage != null) {
        snackbar.setAction(actionMessage) {
            action(this)
        }.show()
    }
}

fun View.hide() {
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun Activity.isKeyboardOpen(view: View): Boolean {
    val visibleBounds = Rect()
    view.getWindowVisibleDisplayFrame(visibleBounds)
    // if more than 100 pixels, its probably a keyboard
    return abs(view.rootView.height - (visibleBounds.bottom - visibleBounds.top)) > 250
}