package com.duke.orca.android.kotlin.biblelockscreen.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.duke.orca.android.kotlin.biblelockscreen.R
import com.duke.orca.android.kotlin.biblelockscreen.base.LinearLayoutManagerWrapper
import com.duke.orca.android.kotlin.biblelockscreen.base.views.PreferenceFragment
import com.duke.orca.android.kotlin.biblelockscreen.settings.adapters.PreferenceAdapter.AdapterItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// 상점. 지금은 광고 제거(한 번 구매)와 구매 복원만 둔다.
class StoreFragment : PreferenceFragment() {
    override val toolbar by lazy { viewBinding.toolbar }
    override val toolbarTitleResId: Int = R.string.store

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        bind()

        return viewBinding.root
    }

    private fun bind() {
        viewBinding.recyclerView.apply {
            adapter = preferenceAdapter
            layoutManager = LinearLayoutManagerWrapper(requireContext())
            setHasFixedSize(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(Billing.isAdFree, Billing.price, ::Pair).collect { (isAdFree, price) ->
                    preferenceAdapter.submitList(items(isAdFree, price))
                }
            }
        }
    }

    private fun items(isAdFree: Boolean, price: String?): List<AdapterItem> {
        val removeAdsSummary = when {
            isAdFree -> getString(R.string.purchased)
            price == null -> getString(R.string.remove_ads_summary)
            else -> "${getString(R.string.remove_ads_summary)} · $price"
        }

        return listOf(
            AdapterItem.Preference(
                id = Id.REMOVE_ADS,
                isClickable = isAdFree.not(),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.round_block_24),
                onClick = {
                    if (Billing.purchase(requireActivity()).not()) {
                        showToast(R.string.store_unavailable)
                    }
                },
                body = getString(R.string.remove_ads),
                summary = removeAdsSummary
            ),
            AdapterItem.Preference(
                id = Id.RESTORE_PURCHASES,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.round_refresh_24),
                onClick = {
                    viewLifecycleOwner.lifecycleScope.launch {
                        when (Billing.restore()) {
                            true -> showToast(R.string.purchases_restored)
                            false -> showToast(R.string.nothing_to_restore)
                            null -> showToast(R.string.store_unavailable)
                        }
                    }
                },
                body = getString(R.string.restore_purchases),
                summary = getString(R.string.restore_purchases_summary)
            )
        )
    }

    private fun showToast(@StringRes resId: Int) {
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
    }

    private object Id {
        const val REMOVE_ADS = 0L
        const val RESTORE_PURCHASES = 1L
    }
}
