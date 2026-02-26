package com.github.kr328.clash.design.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.databinding.DialogTextFieldBinding
import com.github.kr328.clash.design.databinding.DialogZivpnServerProfileBinding
import com.github.kr328.clash.design.util.*
import com.github.kr328.clash.service.model.ZivpnServerProfile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun Context.requestZivpnServerProfileInput(
    initial: ZivpnServerProfile?,
    title: CharSequence,
): ZivpnServerProfile? {
    return suspendCancellableCoroutine {
        val binding = DialogZivpnServerProfileBinding
            .inflate(layoutInflater, this.root, false)

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(binding.root)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { _, _ ->
                val name = binding.nameView.text?.toString() ?: ""
                val address = binding.hostView.text?.toString() ?: ""

                val separatorIndex = address.lastIndexOf('@')
                val (host, pass) = if (separatorIndex != -1) {
                    address.substring(0, separatorIndex) to address.substring(separatorIndex + 1)
                } else {
                    address to ""
                }

                it.resume(ZivpnServerProfile(name, host, pass))
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setOnDismissListener { _ ->
                if (!it.isCompleted)
                    it.resume(initial)
            }

        val dialog = builder.create()

        it.invokeOnCancellation {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            binding.nameView.setText(initial?.name)

            if (initial != null) {
                binding.hostView.setText("${initial.host}@${initial.pass}")
            }
        }

        dialog.show()
    }
}

suspend fun Context.requestModelTextInput(
    initial: String,
    title: CharSequence,
    hint: CharSequence? = null,
    error: CharSequence? = null,
    validator: Validator = ValidatorAcceptAll,
): String {
    return this.requestModelTextInput(initial, title, null, hint, error, validator)!!
}

suspend fun Context.requestModelTextInput(
    initial: String?,
    title: CharSequence,
    reset: CharSequence?,
    hint: CharSequence? = null,
    error: CharSequence? = null,
    validator: Validator = ValidatorAcceptAll,
): String? {
    return suspendCancellableCoroutine {
        val binding = DialogTextFieldBinding
            .inflate(layoutInflater, this.root, false)

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(binding.root)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { _, _ ->
                val text = binding.textField.text?.toString() ?: ""

                if (validator(text))
                    it.resume(text)
                else
                    it.resume(initial)
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setOnDismissListener { _ ->
                if (!it.isCompleted)
                    it.resume(initial)
            }

        if (reset != null) {
            builder.setNeutralButton(reset) { _, _ ->
                it.resume(null)
            }
        }

        val dialog = builder.create()

        it.invokeOnCancellation {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            if (hint != null)
                binding.textLayout.hint = hint

            binding.textField.apply {
                binding.textLayout.isErrorEnabled = error != null

                doOnTextChanged { text, _, _, _ ->
                    if (!validator(text?.toString() ?: "")) {
                        if (error != null)
                            binding.textLayout.error = error

                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    } else {
                        if (error != null)
                            binding.textLayout.error = null

                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    }
                }

                setText(initial)

                setSelection(0, initial?.length ?: 0)

                requestTextInput()
            }
        }

        dialog.show()
    }
}