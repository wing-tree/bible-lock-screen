package com.duke.orca.android.kotlin.biblelockscreen.base.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.coroutineScope
import androidx.viewbinding.ViewBinding
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.bible.models.entries.Bible
import com.duke.orca.android.kotlin.biblelockscreen.persistence.database.Database
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.*
import timber.log.Timber

abstract class BaseFragment<VB: ViewBinding> : Fragment() {
    protected lateinit var viewBinding: VB

    open val toolbar: Toolbar? = null

    private val activityResultLauncherHashMap = hashMapOf<String, ActivityResultLauncher<Intent>>()
    private val behaviourSubjectHashMap = hashMapOf<String, BehaviorSubject<Any>>()
    private val publishSubjectHashMap = hashMapOf<String, PublishSubject<Any>>()

    protected val bible: Bible
        get() = Database.getInstance(requireContext()).bibleBookDao().get()

    protected val compositeDisposable = CompositeDisposable()

    protected fun putActivityResultLauncher(key: String, onActivityResult: (ActivityResult) -> Unit) {
        activityResultLauncherHashMap[key] = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onActivityResult(it)
        }
    }

    protected fun getActivityResultLauncher(key: String) = activityResultLauncherHashMap[key]

    protected fun putPublishSubject(key: String, publishSubject: PublishSubject<Any>) {
        publishSubjectHashMap[key] = publishSubject
    }

    protected fun getPublishSubject(key: String) = publishSubjectHashMap[key]

    protected fun putBehaviourSubject(key: String, behaviorSubject: BehaviorSubject<Any>) {
        behaviourSubjectHashMap[key] = behaviorSubject
    }

    protected fun getBehaviourSubject(key: String) = behaviourSubjectHashMap[key]

    abstract fun inflate(inflater: LayoutInflater, container: ViewGroup?): VB

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = inflate(inflater, container)

        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        return viewBinding.root
    }

    @CallSuper
    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }

    protected fun addFragment(
        @IdRes containerViewId: Int,
        fragmentManager: FragmentManager,
        fragment: Fragment
    ) {
        fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .setReorderingAllowed(true)
            .add(containerViewId, fragment, fragment.tag)
            .addToBackStack(fragment.tag)
            .commit()
    }

    protected fun delayOnLifecycle(
        timeMillis: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: () -> Unit
    ) {
        try {
            viewLifecycleOwner.lifecycle.coroutineScope.launch(dispatcher) {
                delay(timeMillis)
                block()
            }
        } catch (e: IllegalStateException) {
            Timber.e(e)
        }
    }

    protected fun finish() {
        requireActivity().finish()
    }

    @Suppress("DEPRECATION")
    protected fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {
        requireActivity().overridePendingTransition(enterAnim, exitAnim)
    }
}