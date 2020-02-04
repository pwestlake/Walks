package com.pwestlake.walks.utils

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.parcel.Parcelize
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.collections.ArrayList

fun formatAsGPX(points: Set<Trkpt>): String {
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    val documentBuilder = documentBuilderFactory.newDocumentBuilder()
    val document = documentBuilder.newDocument()

    val root = document.createElement("gpx")
    root.setAttribute("version", "1.1")
    root.setAttribute("creator", "Philip Westlake")
    root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
    root.setAttribute("xsi:schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd")
    document.appendChild(root)

    val trkElement = document.createElement("trk")
    root.appendChild(trkElement)

    val trksegElement = document.createElement("trkseg")
    trkElement.appendChild(trksegElement)

    val dfmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    for (trkpt in points) {
        val trkptElement = document.createElement("trkpt")
        trkptElement.setAttribute("lat", trkpt.lat.toString())
        trkptElement.setAttribute("lon", trkpt.lon.toString())

        val elevElement = document.createElement("elev")
        elevElement.textContent = trkpt.elevation.toString()
        trkElement.appendChild(elevElement)

        val dateElement = document.createElement("time")
        elevElement.textContent = dfmt.format(trkpt.time)
        trkElement.appendChild(dateElement)

        trksegElement.appendChild(trkptElement)

    }

    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    val domSource = DOMSource(document)
    val writer = StringWriter()
    val streamResult = StreamResult(writer)

    transformer.transform(domSource, streamResult)

    return writer.buffer.toString()

}

fun readGPXAsLatLng(gpx: String): List<LatLng> {
    val list = ArrayList<LatLng>()
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    val documentBuilder = documentBuilderFactory.newDocumentBuilder()
    val document = documentBuilder.parse(gpx.byteInputStream())

    val trk = document.getElementsByTagName("trkpt")
    for (i in 0..trk.length - 1) {
        val item = trk.item(i)
        val lat = item.attributes.getNamedItem("lat").nodeValue.toDouble()
        val lon = item.attributes.getNamedItem("lon").nodeValue.toDouble()
        list.add(LatLng(lat, lon))
    }

    return list
}

@Parcelize
data class Trkpt(val elevation: Double, val lat: Double, val lon: Double, val velocity: FloatArray): Parcelable {
    val time: Date = Date()
}