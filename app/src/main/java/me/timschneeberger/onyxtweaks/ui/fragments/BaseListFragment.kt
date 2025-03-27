package me.timschneeberger.onyxtweaks.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.timschneeberger.onyxtweaks.databinding.FragmentApplistSheetBinding
import me.timschneeberger.onyxtweaks.ui.adapters.BaseListAdapter


abstract class BaseListFragment<T,TAdapter> : BottomSheetDialogFragment() where TAdapter : BaseListAdapter<T> {

    private lateinit var binding: FragmentApplistSheetBinding
    private lateinit var watcher: TextWatcher
    private val behavior by lazy { (dialog as BottomSheetDialog).behavior }

    protected open lateinit var adapter: TAdapter
    abstract fun createAdapter(): TAdapter
    abstract suspend fun createList(): List<T>

    abstract var dataList: List<T>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentApplistSheetBinding.inflate(layoutInflater, container, false)

        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
        }
        binding.filter.addTextChangedListener(watcher)
        binding.filter.text?.clear()

        adapter = createAdapter()

        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManagerWrapper(requireContext())

        lifecycleScope.launch {
            binding.loader.isVisible = true
            binding.recyclerview.isVisible = false
            binding.filter.isEnabled = false

            val appsData = withContext(Dispatchers.IO) {
                createList()
            }

            dataList = appsData
            adapter.filter.filter(binding.filter.text)

            binding.loader.isVisible = false
            binding.recyclerview.isVisible = true
            binding.filter.isEnabled = true
        }
        return binding.root
    }

    override fun onDestroyView() {
        binding.filter.removeTextChangedListener(watcher)
        super.onDestroyView()
    }

    private class LinearLayoutManagerWrapper(context: Context?) : LinearLayoutManager(context) {
        override fun supportsPredictiveItemAnimations() = false
    }
}