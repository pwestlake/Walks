package com.pwestlake.walks.service

import android.content.Context
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.pwestlake.walks.bo.WalkMetaData
import com.pwestlake.walks.utils.Trkpt
import com.pwestlake.walks.utils.formatAsGPX
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileServiceImpl: FileService {
    override fun createWalkFile(context: Context, data: WalkMetaData): String {
        val dir = context.filesDir

        val dfmt = SimpleDateFormat("yyyyMMddHHmmssS")
        val reference: String = dfmt.format(Date())
        data.id = reference

        val filename: String = "metadata.xml"

        val xmlMapper = XmlMapper()
        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        xmlMapper.setDateFormat(StdDateFormat());

        var file = File(dir.path + "/walks/" + reference)
        file.mkdirs()
        file = File(dir.path + "/walks/" + reference + "/metadata.xml")
        file.outputStream().use {
            it.write(xmlMapper.writeValueAsBytes(data))
        }

        return reference
    }

    override fun writeGPXFile(context: Context, item: String, points: Set<Trkpt>) {
        val dir = context.filesDir
        val data = formatAsGPX(points)

        val file = File(dir.path + "/walks/" + item + "/track.xml")
        file.outputStream().use {
            it.write(data.toByteArray())
        }
    }

    override fun getGPXFile(context: Context, item: String): String? {
        var result: String? = null
        val dir = context?.filesDir
        val file = File(dir?.path + "/walks/" + item + "/track.xml")

        if (file.exists()) {
            file.inputStream().use {
                result = it.readBytes().toString(Charsets.UTF_8)
            }
        }
        return result
    }

    override fun updateItem(context: Context, item: WalkMetaData) {
        val dir = context.filesDir

        val xmlMapper = XmlMapper()
        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        xmlMapper.setDateFormat(StdDateFormat());

        val file = File(dir.path + "/walks/" + item.id + "/metadata.xml")

        file.outputStream().use {
            it.write(xmlMapper.writeValueAsBytes(item))
        }
    }

    override fun getWalkItems(context: Context): MutableList<WalkMetaData> {
        val list = ArrayList<WalkMetaData>()

        val dir = context.filesDir
        val files = File(dir.path + "/walks").listFiles()

        val xmlMapper = XmlMapper()
        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        xmlMapper.setDateFormat(StdDateFormat());

        val type = object: TypeReference<WalkMetaData>() {}

        if (files != null) {
            for (file in files) {
                val file = File(file.path + "/metadata.xml")
                file.inputStream().use {
                    val walkMetaData: WalkMetaData = xmlMapper.readValue(it, type)
                    list.add(0, walkMetaData)
                }
            }
        }

        Collections.sort(list, {o1:WalkMetaData, o2:WalkMetaData -> o2.id.compareTo(o1.id)})
        return list
    }

    override fun deleteReferencedItems(context: Context, items: List<String>) {
        val dir = context.filesDir
        for (reference in items) {
            val file = File(dir.path + "/walks/" + reference)
            file.deleteRecursively()
        }
    }

    override fun deleteReferencedItem(context: Context?, item: String): Boolean {
        val dir = context?.filesDir
        val file = File(dir?.path + "/walks/" + item)

        return file.deleteRecursively()
    }

}