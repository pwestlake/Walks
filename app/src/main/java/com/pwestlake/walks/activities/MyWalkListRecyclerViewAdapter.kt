package com.pwestlake.walks.activities

import android.util.SparseBooleanArray
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.pwestlake.walks.R


import com.pwestlake.walks.activities.WalkListFragment.OnListFragmentInteractionListener
import com.pwestlake.walks.bo.WalkMetaData

import kotlinx.android.synthetic.main.fragment_walklist.view.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MyWalkListRecyclerViewAdapter(
    private val mValues: MutableList<WalkMetaData>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<MyWalkListRecyclerViewAdapter.ViewHolder>() {

    private val dfmt = SimpleDateFormat("dd MMM yyyy HH:mm")
    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as WalkMetaData
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_walklist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mIdView.text = item.name
        holder.mDistanceView.text = String.format("%.2f", (item.distance / 1609.34))
        holder.mContentView.text = dfmt.format(item.date)
        holder.checkBoxView.isChecked = item.checked()

        holder.checkBoxView.setOnClickListener(View.OnClickListener { v ->

            val clickedItem = holder.mView.tag as WalkMetaData
            clickedItem.setChecked(!clickedItem.checked())
            mListener?.itemSelectionChanged(mValues.stream().filter { it.checked() }.collect(Collectors.toList()))

            val checkedCount = mValues.stream().filter { it.checked() }.count()
            when (checkedCount) {
                1L -> mListener?.onOneListItemChecked()
                0L -> mListener?.onListItemChecksCleared()
                else -> mListener?.onMoreThanOneListItemChecked()
            }
        })

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    fun addItem(item: WalkMetaData): Unit {
        mValues.add(0,item)
        notifyItemInserted(0)
    }

    fun updateItem(item: WalkMetaData): Unit {
        val previous = mValues.stream().filter{it.id == item.id}.findFirst()
        if (previous.isPresent) {
            val index = mValues.indexOf(previous.get())

            if (index >= 0)  {
                mValues.set(index, item)
                notifyItemChanged(index)
            }
        }
    }

    fun removeItem(item: WalkMetaData): Boolean {
        val status = mValues.remove(item)
        notifyDataSetChanged()

        return status
    }

    fun getValues(): List<WalkMetaData> {
        return mValues
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.item_number
        val mContentView: TextView = mView.content
        val checkBoxView: CheckBox = mView.checkbox
        val mDistanceView: TextView = mView.item_distance

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
