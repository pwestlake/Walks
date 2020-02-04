package com.pwestlake.walks.service

import android.content.Context
import com.pwestlake.walks.bo.WalkMetaData
import com.pwestlake.walks.utils.Trkpt

interface FileService {
    fun createWalkFile(context: Context, data: WalkMetaData): String

    fun writeGPXFile(context: Context, item: String, points: Set<Trkpt>)

    fun getGPXFile(context: Context, item: String): String?

    fun updateItem(context: Context, item: WalkMetaData): Unit

    fun getWalkItems(context: Context): MutableList<WalkMetaData>

    fun deleteReferencedItems(context: Context, items: List<String>)

    fun deleteReferencedItem(context: Context?, item: String): Boolean
}