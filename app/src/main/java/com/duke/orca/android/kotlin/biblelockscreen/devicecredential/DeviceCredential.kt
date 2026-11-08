package com.duke.orca.android.kotlin.biblelockscreen.devicecredential

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.constants.BLANK
import timber.log.Timber

object DeviceCredential {
    fun requireUnlock(context: Context): Boolean {
        val keyguardManager = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        // 밀어서 잠금(보안 없는 잠금)도 함께 풀어야 시스템 잠금화면이 뒤에 남지 않는다.
        return keyguardManager.isKeyguardLocked
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun confirmDeviceCredential(activity: Activity, keyguardDismissCallback: KeyguardManager.KeyguardDismissCallback) {
        val keyguardManager = activity.getSystemService(KeyguardManager::class.java)

        keyguardManager.requestDismissKeyguard(activity, keyguardDismissCallback)
    }

    fun confirmDeviceCredential(activity: AppCompatActivity, activityResultLauncher: ActivityResultLauncher<Intent>?) {
        val keyguardManager = activity.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val title = if (lockPatternEnable(activity))
            activity.getString(R.string.draw_unlock_pattern)
        else
            activity.getString(R.string.enter_password_or_pin)

        @Suppress("DEPRECATION")
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(title, BLANK)

        activityResultLauncher?.launch(intent)
    }

    fun confirmDeviceCredential(fragment: Fragment, activityResultLauncher: ActivityResultLauncher<Intent>?) {
        val keyguardManager = fragment.requireActivity().getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val title = if (lockPatternEnable(fragment.requireContext()))
            fragment.getString(R.string.draw_unlock_pattern)
        else
            fragment.getString(R.string.enter_password_or_pin)

        @Suppress("DEPRECATION")
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(title, BLANK)

        activityResultLauncher?.launch(intent)
    }

    private fun lockPatternEnable(context: Context): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val lockPatternEnable: Int = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCK_PATTERN_ENABLED
            )

            lockPatternEnable == 1
        } catch (e: Settings.SettingNotFoundException) {
            Timber.e(e)
            false
        }
    }
}