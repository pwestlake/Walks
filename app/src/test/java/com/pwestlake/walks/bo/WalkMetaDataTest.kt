package com.pwestlake.walks.bo

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class WalkMetaDataTest {
    @Test
    fun testXml() {
        val xmlMapper = XmlMapper()
        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        xmlMapper.setDateFormat(StdDateFormat());

        val dfmt = SimpleDateFormat("yyyyMMdd")
        val exampleData = WalkMetaData("", "TestWalk", dfmt.parse("20200114"))

        val xml = xmlMapper.writeValueAsString(exampleData)
        assertEquals("<WalkMetaData><id></id><name>TestWalk</name><date>2020-01-14T00:00:00.000+0000</date><distance>0.0</distance><duration>0</duration><speed>0.0</speed></WalkMetaData>",
            xml)
    }

    @Test
    fun testParseXml() {
        val xmlMapper = XmlMapper()
        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        xmlMapper.setDateFormat(StdDateFormat());

        val type = object: TypeReference<WalkMetaData>() {}
        val obj = xmlMapper.readValue<WalkMetaData>(
            "<WalkMetaData><id></id><name>TestWalk</name><date>2020-01-14T00:00:00.000+0000</date><distance>0.0</distance><duration>0</duration><speed>0.0</speed></WalkMetaData>",type)

        val dfmt = SimpleDateFormat("yyyyMMdd")
        val expectedDate = dfmt.parse("20200114")
        val expected = WalkMetaData("", "TestWalk", expectedDate)

        assertEquals(expected, obj)
    }
}