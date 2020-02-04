package com.pwestlake.walks.bo

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.util.*

@Parcelize
data class WalkMetaData(
    var id: String = "",
    var name: String = "default",
    val date: Date = Date(),
    var distance: Double = 0.0,
    var duration: Long = 0,
    var speed: Double = 0.0): Parcelable {

    private var checked: Boolean = false

    fun setChecked(value: Boolean) {
        this.checked = value
    }

    fun checked(): Boolean {
        return checked
    }

}