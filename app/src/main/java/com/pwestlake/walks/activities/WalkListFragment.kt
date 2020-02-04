package com.pwestlake.walks.activities

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pwestlake.walks.R

import com.pwestlake.walks.bo.WalkMetaData
import com.pwestlake.walks.service.DaggerFileComponent
import com.pwestlake.walks.service.FileService
import java.util.stream.Collectors
import javax.inject.Inject

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [WalkListFragment.OnListFragmentInteractionListener] interface.
 */
class WalkListFragment : Fragment() {
    @Inject lateinit var fileService: FileService

    // TODO: Customize parameters
    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_walklist_list, container, false)

        DaggerFileComponent.create().inject(this)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = MyWalkListRecyclerViewAdapter(fileService.getWalkItems(context), listener)
            }
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun addItem(item: WalkMetaData): Unit {
        if (view is RecyclerView) {
            val recyclerViewAdapter = (view as RecyclerView).adapter

            if (recyclerViewAdapter is MyWalkListRecyclerViewAdapter) {
                recyclerViewAdapter.addItem(item)
            }

            (view as RecyclerView).scrollToPosition(0)
        }
    }

    fun updateItem(item: WalkMetaData): Unit {
        if (view is RecyclerView) {
            val recyclerViewAdapter = (view as RecyclerView).adapter

            if (recyclerViewAdapter is MyWalkListRecyclerViewAdapter) {
                recyclerViewAdapter.updateItem(item)
            }
        }
    }

    fun removeItem(item: WalkMetaData): Boolean {
        var status = false

        if (view is RecyclerView) {
            val recyclerViewAdapter = (view as RecyclerView).adapter

            if (recyclerViewAdapter is MyWalkListRecyclerViewAdapter) {
                status = recyclerViewAdapter.removeItem(item)
            }
        }

        return status
    }

    fun getSelectedItems(): List<WalkMetaData> {
        var list: List<WalkMetaData> = ArrayList<WalkMetaData>()

        if (view is RecyclerView) {
            val recyclerViewAdapter = (view as RecyclerView).adapter

            if (recyclerViewAdapter is MyWalkListRecyclerViewAdapter) {
                list = recyclerViewAdapter.getValues().stream()
                    .filter{it.checked()}
                    .collect(Collectors.toList())
            }
        }

        return list
    }

    fun deleteSelectedItems(): Unit {
        val list = getSelectedItems()

        for (item in list) {
            if (removeItem(item)) {
                fileService.deleteReferencedItem(context, item.id)
            }
        }

    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: WalkMetaData?)

        fun itemSelectionChanged(item: List<WalkMetaData>)
        fun onOneListItemChecked()
        fun onMoreThanOneListItemChecked()
        fun onListItemChecksCleared()
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            WalkListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}
