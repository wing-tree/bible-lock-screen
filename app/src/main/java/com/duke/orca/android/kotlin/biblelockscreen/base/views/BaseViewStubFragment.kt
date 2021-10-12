package com.duke.orca.android.kotlin.biblelockscreen.base.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.annotation.LayoutRes
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.application.fadeOut
import com.duke.orca.android.kotlin.biblelockscreen.databinding.FragmentViewStubBinding
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseViewStubFragment : BaseFragment<FragmentViewStubBinding>() {
    @get:LayoutRes
    abstract val layoutResource: Int
    abstract val showCircularProgressIndicator: Boolean

    private val duration = 200L

    private var viewStub: ViewStub? = null
    private var onResumed = AtomicBoolean(false)
    private var isInflated = AtomicBoolean(false)

    abstract fun onInflated(view: View)

    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentViewStubBinding {
        return FragmentViewStubBinding.inflate(inflater, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        if (showCircularProgressIndicator) {
            viewBinding.circularProgressIndicator.show()
        } else {
            viewBinding.circularProgressIndicator.hide()
        }

        viewStub = viewBinding.root.findViewById(R.id.view_stub)
        viewStub?.layoutResource = layoutResource
        inflate()

        return viewBinding.root
    }

    override fun onResume() {
        super.onResume()
        onResumed.set(true)
        inflate()
    }

    private fun inflate() {
        if (onResumed.get() && isInflated.get().not()) {
            viewStub?.inflate()?.let {
                if (showCircularProgressIndicator) {
                    viewBinding.circularProgressIndicator.fadeOut(duration) {
                        onInflated(it)
                        afterOnInflated()
                    }
                } else {
                    onInflated(it)
                    afterOnInflated()
                }
            }
        }
    }

    private fun afterOnInflated() {
        isInflated.set(true)
    }
}